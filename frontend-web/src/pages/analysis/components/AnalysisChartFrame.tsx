import { type ReactElement, useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { AssetDefinition, DrawTool, TimeRange } from '../types'

const rangeButtons: TimeRange[] = ['1h', '6h', '24h', '7d', '30d', '90d', '1y', '5y']
const COMPARE_SLOT_COUNT = 3

type AnalysisChartFrameProps = {
  timeRange: TimeRange
  onRangeChange: (range: TimeRange) => void
  showNewsOnChart: boolean
  showMA20: boolean
  showMA50: boolean
  showRsi: boolean
  showVolume: boolean
  onToggleNews: () => void
  onToggleMA20: () => void
  onToggleMA50: () => void
  onToggleRsi: () => void
  onToggleVolume: () => void
  drawTool: DrawTool
  onDrawToolChange: (tool: DrawTool) => void
  children: React.ReactNode
  compareSlots: (AssetDefinition | null)[]
  compareCandidates: AssetDefinition[]
  mainAssetId: string | null
  onCompareSlotSet: (slotIndex: number, assetId: string | null) => void
}

function DrawIconNone() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M6 3l2.5 12.5L11 12l4 8 2-1-4.5-8.5L17 9 6 3z"
        fill="currentColor"
        stroke="currentColor"
        strokeWidth="0.5"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function DrawIconTrend() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M4 18L10 10l4 4 6-8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function DrawIconPoint() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <circle cx="12" cy="11" r="3" />
      <path d="M12 14v5M9 21h6" strokeLinecap="round" />
    </svg>
  )
}

function DrawIconHline() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M4 12h16" strokeLinecap="round" />
      <path d="M8 7h8M8 17h8" strokeOpacity="0.35" strokeLinecap="round" />
    </svg>
  )
}

const drawTools: { id: DrawTool; Icon: () => ReactElement }[] = [
  { id: 'none', Icon: DrawIconNone },
  { id: 'trendline', Icon: DrawIconTrend },
  { id: 'point', Icon: DrawIconPoint },
  { id: 'hline', Icon: DrawIconHline },
]

function padCompareSlots(slots: (AssetDefinition | null)[]): (AssetDefinition | null)[] {
  const out = slots.slice(0, COMPARE_SLOT_COUNT)
  while (out.length < COMPARE_SLOT_COUNT) {
    out.push(null)
  }
  return out
}

export function AnalysisChartFrame({
  timeRange,
  onRangeChange,
  showNewsOnChart,
  showMA20,
  showMA50,
  showRsi,
  showVolume,
  onToggleNews,
  onToggleMA20,
  onToggleMA50,
  onToggleRsi,
  onToggleVolume,
  drawTool,
  onDrawToolChange,
  children,
  compareSlots,
  compareCandidates,
  mainAssetId,
  onCompareSlotSet,
}: AnalysisChartFrameProps) {
  const { t } = useTranslation('analysis')
  const [pickerSlot, setPickerSlot] = useState<number | null>(null)
  const topRightRef = useRef<HTMLDivElement>(null)
  const pickerId = useId()

  const slots = padCompareSlots(compareSlots)

  const pickerCandidates = compareCandidates.filter((asset) => {
    if (mainAssetId != null && asset.id === mainAssetId) return false
    return !slots.some((s, i) => i !== pickerSlot && s?.id === asset.id)
  })

  useEffect(() => {
    if (pickerSlot == null) return undefined
    const onPointerDown = (ev: PointerEvent) => {
      const root = topRightRef.current
      if (root && !root.contains(ev.target as Node)) {
        setPickerSlot(null)
      }
    }
    const onKeyDown = (ev: KeyboardEvent) => {
      if (ev.key === 'Escape') setPickerSlot(null)
    }
    document.addEventListener('pointerdown', onPointerDown, true)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown, true)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [pickerSlot])

  const layerStrip = [
    { key: 'news', text: t('chartFrame.layerStripNews'), aria: t('controls.showNews'), on: showNewsOnChart, toggle: onToggleNews },
    { key: 'ma20', text: t('chartFrame.layerStripMA20'), aria: t('controls.showMA20'), on: showMA20, toggle: onToggleMA20 },
    { key: 'ma50', text: t('chartFrame.layerStripMA50'), aria: t('controls.showMA50'), on: showMA50, toggle: onToggleMA50 },
    { key: 'rsi', text: t('chartFrame.layerStripRSI'), aria: t('controls.showRSI'), on: showRsi, toggle: onToggleRsi },
    { key: 'vol', text: t('chartFrame.layerStripVolume'), aria: t('deck.showVolume'), on: showVolume, toggle: onToggleVolume },
  ]

  return (
    <div className="fi-analysis-chart-workbench">
      <header className="fi-chart-frame-top">
        <div className="fi-chart-frame-layers-strip" role="toolbar" aria-label={t('deck.analysisLayers')}>
          {layerStrip.map((layer) => (
            <button
              key={layer.key}
              type="button"
              role="switch"
              aria-checked={layer.on}
              title={layer.aria}
              aria-label={layer.aria}
              className={`fi-chart-layer-strip-btn${layer.on ? ' fi-chart-layer-strip-btn--on' : ''}`}
              onClick={layer.toggle}
            >
              <span className="fi-chart-layer-strip-label">{layer.text}</span>
            </button>
          ))}
        </div>

        <div className="fi-chart-frame-top-right" ref={topRightRef}>
          <div className="fi-chart-frame-ranges-scroll" role="toolbar" aria-label={t('chartFrame.intervalsAria')}>
            <div className="fi-interval-group fi-interval-group--scroll">
              {rangeButtons.map((range) => (
                <button
                  key={range}
                  type="button"
                  className={`fi-interval-btn${timeRange === range ? ' fi-interval-btn--active' : ''}`}
                  onClick={() => onRangeChange(range)}
                >
                  {range}
                </button>
              ))}
            </div>
          </div>

          <div className="fi-chart-compare-slots" role="group" aria-label={t('chartFrame.compareSlotsAria')}>
            {slots.map((asset, slotIndex) => (
              <div key={slotIndex} className="fi-chart-compare-slot-wrap">
                <button
                  type="button"
                  className={`fi-chart-compare-slot${asset ? ' fi-chart-compare-slot--filled' : ' fi-chart-compare-slot--empty'}`}
                  aria-expanded={pickerSlot === slotIndex}
                  aria-controls={pickerSlot === slotIndex ? `${pickerId}-${slotIndex}` : undefined}
                  title={asset ? t('chartFrame.compareSlotChange', { symbol: asset.symbol }) : t('chartFrame.compareSlotAdd')}
                  aria-label={asset ? t('chartFrame.compareSlotChange', { symbol: asset.symbol }) : t('chartFrame.compareSlotAdd')}
                  onClick={() => setPickerSlot((cur) => (cur === slotIndex ? null : slotIndex))}
                >
                  {asset ? <span className="fi-chart-compare-slot-symbol">{asset.symbol}</span> : <span className="fi-chart-compare-slot-plus">+</span>}
                </button>
                {asset ? (
                  <button
                    type="button"
                    className="fi-chart-compare-slot-remove"
                    aria-label={t('chartFrame.compareSlotRemove', { symbol: asset.symbol })}
                    onClick={(ev) => {
                      ev.stopPropagation()
                      onCompareSlotSet(slotIndex, null)
                      setPickerSlot(null)
                    }}
                  >
                    ×
                  </button>
                ) : null}
                {pickerSlot === slotIndex ? (
                  <div id={`${pickerId}-${slotIndex}`} className="fi-chart-compare-slot-picker" role="dialog" aria-modal="true" aria-label={t('chartFrame.comparePickerTitle')}>
                    <div className="fi-chart-compare-slot-picker-head">
                      <span>{t('chartFrame.comparePickerTitle')}</span>
                      <button type="button" className="fi-chart-compare-slot-picker-close" onClick={() => setPickerSlot(null)} aria-label={t('chartFrame.closeCompareAdd')}>
                        ×
                      </button>
                    </div>
                    <div className="fi-chart-compare-slot-picker-list" role="listbox">
                      {pickerCandidates.length === 0 ? (
                        <p className="fi-chart-compare-slot-picker-empty">{t('chartFrame.comparePickerEmpty')}</p>
                      ) : (
                        pickerCandidates.map((cand) => (
                          <button
                            key={cand.id}
                            type="button"
                            role="option"
                            className="fi-chart-compare-slot-picker-row"
                            onClick={() => {
                              onCompareSlotSet(slotIndex, cand.id)
                              setPickerSlot(null)
                            }}
                          >
                            <span className="fi-chart-compare-slot-picker-symbol">{cand.symbol}</span>
                            <span className="fi-chart-compare-slot-picker-name">{cand.name}</span>
                          </button>
                        ))
                      )}
                    </div>
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        </div>
      </header>

      <div className="fi-chart-frame-body">
        <aside className="fi-chart-frame-draw-rail" aria-label={t('chartFrame.drawToolsAria')}>
          {drawTools.map(({ id, Icon }) => (
            <button
              key={id}
              type="button"
              className={`fi-chart-draw-tool-btn${drawTool === id ? ' fi-chart-draw-tool-btn--active' : ''}`}
              onClick={() => onDrawToolChange(id)}
              title={
                id === 'none'
                  ? t('chartFrame.drawPointer')
                  : id === 'trendline'
                    ? t('chartFrame.drawTrend')
                    : id === 'point'
                      ? t('chartFrame.drawPoint')
                      : t('chartFrame.drawHline')
              }
              aria-pressed={drawTool === id}
            >
              <Icon />
            </button>
          ))}
        </aside>
        <div className="fi-chart-frame-plot">{children}</div>
      </div>
    </div>
  )
}
