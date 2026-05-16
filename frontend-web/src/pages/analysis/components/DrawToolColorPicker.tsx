import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useTranslation } from 'react-i18next'
import {
  hexFromSliderPosition,
  normalizeDrawColor,
  sliderPositionFromHex,
} from '../chart/drawing/drawColors'
import type { DrawableTool } from '../chart/drawing/drawColors'

type DrawToolColorPickerProps = {
  tool: DrawableTool
  color: string
  onColorChange: (tool: DrawableTool, color: string) => void
}

const POPOVER_WIDTH = 228

type ColorSpectrumSliderProps = {
  color: string
  ariaLabel: string
  onPreview: (color: string) => void
  onCommit: (color: string) => void
}

function ColorSpectrumSlider({ color, ariaLabel, onPreview, onCommit }: ColorSpectrumSliderProps) {
  const trackRef = useRef<HTMLDivElement>(null)
  const [position, setPosition] = useState(() => sliderPositionFromHex(color))
  const [dragging, setDragging] = useState(false)
  const draggingRef = useRef(false)

  useEffect(() => {
    if (!draggingRef.current) {
      setPosition(sliderPositionFromHex(color))
    }
  }, [color])

  const applyPosition = useCallback(
    (nextPosition: number, commit: boolean) => {
      const clamped = Math.min(1, Math.max(0, nextPosition))
      const nextColor = hexFromSliderPosition(clamped)
      setPosition(clamped)
      onPreview(nextColor)
      if (commit) onCommit(nextColor)
    },
    [onCommit, onPreview],
  )

  const updateFromClientX = useCallback(
    (clientX: number, commit: boolean) => {
      const track = trackRef.current
      if (!track) return
      const rect = track.getBoundingClientRect()
      if (rect.width <= 0) return
      const ratio = (clientX - rect.left) / rect.width
      applyPosition(ratio, commit)
    },
    [applyPosition],
  )

  const handlePointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    event.preventDefault()
    event.stopPropagation()
    draggingRef.current = true
    setDragging(true)
    event.currentTarget.setPointerCapture(event.pointerId)
    updateFromClientX(event.clientX, false)
  }

  const handlePointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!draggingRef.current) return
    event.preventDefault()
    updateFromClientX(event.clientX, false)
  }

  const handlePointerUp = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!draggingRef.current) return
    event.preventDefault()
    draggingRef.current = false
    setDragging(false)
    updateFromClientX(event.clientX, true)
    try {
      event.currentTarget.releasePointerCapture(event.pointerId)
    } catch {
      /* already released */
    }
  }

  const thumbPercent = `${position * 100}%`

  return (
    <div
      ref={trackRef}
      className={`fi-chart-draw-color-spectrum${dragging ? ' fi-chart-draw-color-spectrum--dragging' : ''}`}
      role="slider"
      aria-label={ariaLabel}
      aria-valuemin={0}
      aria-valuemax={360}
      aria-valuenow={Math.round(position * 360)}
      aria-valuetext={normalizeDrawColor(color).toUpperCase()}
      tabIndex={0}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerUp}
      onKeyDown={(event) => {
        const step = event.shiftKey ? 0.05 : 0.01
        if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
          event.preventDefault()
          applyPosition(position - step, true)
        } else if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
          event.preventDefault()
          applyPosition(position + step, true)
        }
      }}
    >
      <div className="fi-chart-draw-color-spectrum-track" aria-hidden="true" />
      <div
        className="fi-chart-draw-color-spectrum-thumb"
        style={{ left: thumbPercent, backgroundColor: normalizeDrawColor(color) }}
        aria-hidden="true"
      />
    </div>
  )
}

export function DrawToolColorPicker({ tool, color, onColorChange }: DrawToolColorPickerProps) {
  const { t } = useTranslation('analysis')
  const [open, setOpen] = useState(false)
  const [previewColor, setPreviewColor] = useState(() => normalizeDrawColor(color))
  const [popoverStyle, setPopoverStyle] = useState<React.CSSProperties | null>(null)
  const swatchRef = useRef<HTMLButtonElement>(null)
  const popoverRef = useRef<HTMLDivElement>(null)
  const pickerId = useId()
  const swatchColor = normalizeDrawColor(color)

  useEffect(() => {
    if (!open) {
      setPreviewColor(swatchColor)
    }
  }, [open, swatchColor])

  const syncPopoverPosition = useCallback(() => {
    const swatch = swatchRef.current
    if (!swatch) return
    const rect = swatch.getBoundingClientRect()
    const gap = 10
    const fitsRight = rect.right + gap + POPOVER_WIDTH <= window.innerWidth - 8
    setPopoverStyle({
      position: 'fixed',
      left: fitsRight ? rect.right + gap : rect.left - gap - POPOVER_WIDTH,
      top: rect.top + rect.height / 2,
      transform: 'translateY(-50%)',
      zIndex: 10050,
    })
  }, [])

  useEffect(() => {
    if (!open) return undefined
    syncPopoverPosition()
    const onLayout = () => syncPopoverPosition()
    window.addEventListener('resize', onLayout)
    window.addEventListener('scroll', onLayout, true)
    return () => {
      window.removeEventListener('resize', onLayout)
      window.removeEventListener('scroll', onLayout, true)
    }
  }, [open, syncPopoverPosition])

  useEffect(() => {
    if (!open) return undefined
    const onPointerDown = (event: PointerEvent) => {
      const target = event.target as Node
      if (swatchRef.current?.contains(target)) return
      if (popoverRef.current?.contains(target)) return
      setOpen(false)
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown, true)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown, true)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  const toggleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.preventDefault()
    event.stopPropagation()
    if (!open) {
      setPreviewColor(swatchColor)
      syncPopoverPosition()
    }
    setOpen((value) => !value)
  }

  const displayColor = open ? previewColor : swatchColor

  const popover =
    open && popoverStyle
      ? createPortal(
          <div
            ref={popoverRef}
            id={pickerId}
            className="fi-chart-draw-color-popover fi-chart-draw-color-popover--portal"
            style={popoverStyle}
            role="dialog"
            aria-label={t('chartDrawings.colorPickerTitle')}
            onPointerDown={(event) => event.stopPropagation()}
          >
            <div className="fi-chart-draw-color-popover-head">
              <span className="fi-chart-draw-color-popover-title">{t('chartDrawings.colorPickerTitle')}</span>
              <span className="fi-chart-draw-color-popover-hex">{displayColor.toUpperCase()}</span>
            </div>
            <div
              className="fi-chart-draw-color-preview"
              style={{
                background: `linear-gradient(135deg, ${displayColor} 0%, ${displayColor}dd 100%)`,
              }}
              aria-hidden="true"
            />
            <ColorSpectrumSlider
              color={displayColor}
              ariaLabel={t('chartDrawings.colorSpectrumAria')}
              onPreview={(nextColor) => {
                setPreviewColor(nextColor)
                onColorChange(tool, nextColor)
              }}
              onCommit={(nextColor) => {
                setPreviewColor(nextColor)
                onColorChange(tool, nextColor)
              }}
            />
          </div>,
          document.body,
        )
      : null

  return (
    <div className="fi-chart-draw-color-wrap">
      <button
        ref={swatchRef}
        type="button"
        className="fi-chart-draw-color-swatch"
        aria-expanded={open}
        aria-controls={pickerId}
        aria-label={t('chartDrawings.colorPickerAria', { tool })}
        title={t('chartDrawings.colorPickerTitle')}
        onPointerDown={(event) => event.stopPropagation()}
        onClick={toggleOpen}
      >
        <span className="fi-chart-draw-color-swatch-dot" style={{ backgroundColor: displayColor }} />
      </button>
      {popover}
    </div>
  )
}
