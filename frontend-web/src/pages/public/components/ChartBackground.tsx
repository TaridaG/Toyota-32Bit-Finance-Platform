import { useLayoutEffect, useRef } from 'react'
import { CandlestickSeries, createChart, type CandlestickData, type UTCTimestamp } from 'lightweight-charts'

type ChartBackgroundProps = {
  isReducedMotion: boolean
  theme: 'light' | 'dark'
}

function randomBetween(min: number, max: number) {
  return min + Math.random() * (max - min)
}

export function ChartBackground({ isReducedMotion, theme }: ChartBackgroundProps) {
  const hostRef = useRef<HTMLDivElement | null>(null)

  useLayoutEffect(() => {
    const host = hostRef.current
    if (!host) return

    let timerId: number | null = null
    let isActive = true

    const isDark = theme === 'dark'
    const palette = {
      grid: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.05)',
      bull: isDark ? '#22c55e' : '#16a34a',
      bear: isDark ? '#ef4444' : '#dc2626',
      bg: isDark ? '#0b1220' : '#f8fafc',
    }

    const chartInstance = createChart(host, {
      width: Math.max(host.clientWidth || 0, 320),
      height: Math.max(host.clientHeight || 0, 180),
      handleScroll: false,
      handleScale: false,
      layout: {
        background: { color: palette.bg },
        textColor: 'rgba(148,163,184,0.01)',
      },
      grid: {
        vertLines: { color: palette.grid },
        horzLines: { color: palette.grid },
      },
      rightPriceScale: { visible: false, borderVisible: false },
      leftPriceScale: { visible: false, borderVisible: false },
      timeScale: { visible: false, borderVisible: false, secondsVisible: false, timeVisible: false },
      crosshair: {
        mode: 0,
        vertLine: { visible: false, labelVisible: false },
        horzLine: { visible: false, labelVisible: false },
      },
      localization: {
        locale: 'en-US',
      },
    })

    const seriesInstance = chartInstance.addSeries(CandlestickSeries, {
      upColor: `${palette.bull}88`,
      downColor: `${palette.bear}88`,
      borderVisible: false,
      wickUpColor: `${palette.bull}66`,
      wickDownColor: `${palette.bear}66`,
      priceLineVisible: false,
      lastValueVisible: false,
    })

    const nowSec = Math.floor(Date.now() / 1000)
    const initialCount = 120
    let lastTime = nowSec - initialCount
    let lastClose = 100

    const initialData: CandlestickData[] = []
    for (let i = 0; i < initialCount; i += 1) {
      const open = lastClose
      const close = open + randomBetween(-1, 1)
      const high = Math.max(open, close) + randomBetween(0, 0.5)
      const low = Math.min(open, close) - randomBetween(0, 0.5)
      const candle: CandlestickData = {
        time: (lastTime + i) as UTCTimestamp,
        open,
        high,
        low,
        close,
      }
      initialData.push(candle)
      lastClose = close
    }
    lastTime = nowSec
    seriesInstance.setData(initialData)
    chartInstance.timeScale().fitContent()

    const tick = () => {
      if (!isActive) return
      const open = lastClose
      const close = open + randomBetween(-1, 1)
      const high = Math.max(open, close) + randomBetween(0, 0.5)
      const low = Math.min(open, close) - randomBetween(0, 0.5)
      lastTime += 1
      const candle: CandlestickData = {
        time: lastTime as UTCTimestamp,
        open,
        high,
        low,
        close,
      }
      seriesInstance.update(candle)
      lastClose = close
      const nextDelay = isReducedMotion ? randomBetween(700, 950) : randomBetween(500, 800)
      timerId = window.setTimeout(tick, nextDelay)
    }

    tick()

    const resizeObserver = new ResizeObserver(() => {
      chartInstance.applyOptions({
        width: Math.max(host.clientWidth || 0, 320),
        height: Math.max(host.clientHeight || 0, 180),
      })
    })
    resizeObserver.observe(host)

    const onVisibility = () => {
      if (document.hidden) {
        isActive = false
        if (timerId != null) {
          window.clearTimeout(timerId)
          timerId = null
        }
        return
      }
      if (!isActive) {
        isActive = true
        tick()
      }
    }
    document.addEventListener('visibilitychange', onVisibility)

    return () => {
      isActive = false
      if (timerId != null) {
        window.clearTimeout(timerId)
      }
      resizeObserver.disconnect()
      document.removeEventListener('visibilitychange', onVisibility)
      chartInstance.remove()
    }
  }, [isReducedMotion, theme])

  return (
    <div className="landing-chart-host" aria-hidden="true">
      <div ref={hostRef} className="landing-chart-surface" />
      <div className="landing-chart-overlay" />
    </div>
  )
}
