import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { flushSync } from 'react-dom'

export type ThemeMode = 'light' | 'dark'

type ThemeOrigin = {
  x: number
  y: number
}

type ThemeContextValue = {
  theme: ThemeMode
  setTheme: (theme: ThemeMode, origin?: ThemeOrigin) => void
  toggleTheme: (origin?: ThemeOrigin) => void
}

type ViewTransitionLike = {
  finished: Promise<void>
}

type DocumentWithViewTransition = Document & {
  startViewTransition?: (callback: () => void) => ViewTransitionLike
}

const THEME_STORAGE_KEY = 'finance.theme'

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined)

function resolveSystemTheme(): ThemeMode {
  if (typeof window === 'undefined') {
    return 'light'
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function resolveInitialTheme(): ThemeMode {
  if (typeof window === 'undefined') {
    return 'light'
  }

  const saved = window.localStorage.getItem(THEME_STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') {
    return saved
  }

  return resolveSystemTheme()
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemeMode>(resolveInitialTheme)

  useEffect(() => {
    const root = document.documentElement
    root.dataset.theme = theme
    root.style.colorScheme = theme
    window.localStorage.setItem(THEME_STORAGE_KEY, theme)
  }, [theme])

  const applyThemeTransition = useCallback(
    (nextTheme: ThemeMode, origin?: ThemeOrigin) => {
      if (nextTheme === theme) {
        return
      }

      if (typeof window === 'undefined' || typeof document === 'undefined') {
        setThemeState(nextTheme)
        return
      }

      const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      if (reduceMotion) {
        setThemeState(nextTheme)
        return
      }

      const x = origin?.x ?? window.innerWidth / 2
      const y = origin?.y ?? window.innerHeight / 2
      const maxX = Math.max(x, window.innerWidth - x)
      const maxY = Math.max(y, window.innerHeight - y)
      const radius = Math.hypot(maxX, maxY)
      const root = document.documentElement
      const transitionDocument = document as DocumentWithViewTransition

      root.style.setProperty('--theme-wave-x', `${x}px`)
      root.style.setProperty('--theme-wave-y', `${y}px`)
      root.style.setProperty('--theme-wave-radius', `${radius}px`)

      if (typeof transitionDocument.startViewTransition !== 'function') {
        setThemeState(nextTheme)
        return
      }

      root.classList.add('theme-wave-transition')

      const transition = transitionDocument.startViewTransition(() => {
        flushSync(() => {
          setThemeState(nextTheme)
        })
      })

      transition.finished.finally(() => {
        root.classList.remove('theme-wave-transition')
      })
    },
    [theme],
  )

  const setTheme = useCallback(
    (nextTheme: ThemeMode, origin?: ThemeOrigin) => {
      applyThemeTransition(nextTheme, origin)
    },
    [applyThemeTransition],
  )

  const toggleTheme = useCallback(
    (origin?: ThemeOrigin) => {
      const nextTheme = theme === 'light' ? 'dark' : 'light'
      applyThemeTransition(nextTheme, origin)
    },
    [applyThemeTransition, theme],
  )

  const value = useMemo(
    () => ({
      theme,
      setTheme,
      toggleTheme,
    }),
    [setTheme, theme, toggleTheme],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

// Hook colocated with provider; split file would duplicate context wiring.
// eslint-disable-next-line react-refresh/only-export-components -- useTheme is a hook, not a component
export function useTheme() {
  const context = useContext(ThemeContext)

  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider')
  }

  return context
}

