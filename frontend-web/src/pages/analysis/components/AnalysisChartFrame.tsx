import { useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { AssetDefinition, ChartDisplayType, DrawTool, TimeRange } from '../types'
import type { DrawableTool } from '../chart/drawing/drawColors'
import { CHART_DRAW_TOOLS, DrawIconHistory, DrawIconPlus, DrawIconTrash, IconChartMeasure } from './chartDrawIcons'
import { DrawToolColorPicker } from './DrawToolColorPicker'

const rangeButtons: TimeRange[] = ['1h', '6h', '24h', '7d', '30d', '90d', '1y', '5y']
const COMPARE_SLOT_COUNT = 3

type AnalysisChartFrameProps = {
  timeRange: TimeRange
  onRangeChange: (range: TimeRange) => void
  showNewsOnChart: boolean
  chartNewsLoading?: boolean
  chartNewsFavoritesOnly?: boolean
  showChartNewsFavoriteStar?: boolean
  onToggleChartNewsFavorites?: () => void
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
  drawColorsByTool: Record<DrawableTool, string>
  onDrawColorChange: (tool: DrawableTool, color: string) => void
  selectedDrawingId: string | null
  hasDrawings: boolean
  showDrawingLibrary: boolean
  canSaveDrawings: boolean
  onSaveDrawingsClick: () => void
  onDrawingHistoryClick: () => void
  onDeleteSelectedDrawing: () => void
  children: React.ReactNode
  compareSlots: (AssetDefinition | null)[]
  compareCandidates: AssetDefinition[]
  mainAssetId: string | null
  onCompareSlotSet: (slotIndex: number, assetId: string | null) => void
  chartType: ChartDisplayType
  onChartTypeChange: (type: ChartDisplayType) => void
  measureToolActive: boolean
  onMeasureToolToggle: () => void
}

function IconCandles() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <line x1="7" y1="5" x2="7" y2="19" strokeLinecap="round" />
      <rect x="5.25" y="9" width="3.5" height="6" rx="0.5" fill="currentColor" stroke="none" />
      <line x1="13" y1="3" x2="13" y2="19" strokeLinecap="round" />
      <rect x="11.25" y="7" width="3.5" height="7" rx="0.5" fill="currentColor" stroke="none" />
      <line x1="19" y1="7" x2="19" y2="19" strokeLinecap="round" />
      <rect x="17.25" y="10" width="3.5" height="5" rx="0.5" fill="currentColor" stroke="none" />
    </svg>
  )
}

function IconLineChart() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M4 17 L9 11 L14 14 L20 7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function drawToolTitle(tool: DrawTool, t: (key: string) => string): string {
  const keyMap: Record<DrawTool, string> = {
    none: 'chartFrame.drawPointer',
    trendline: 'chartFrame.drawTrend',
    ray: 'chartFrame.drawRay',
    hline: 'chartFrame.drawHline',
    vline: 'chartFrame.drawVline',
    rect: 'chartFrame.drawRect',
    fib: 'chartFrame.drawFib',
    point: 'chartFrame.drawPoint',
  }
  return t(keyMap[tool])
}

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
  chartNewsLoading = false,
  chartNewsFavoritesOnly = false,
  showChartNewsFavoriteStar = false,
  onToggleChartNewsFavorites,
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
  drawColorsByTool,
  onDrawColorChange,
  selectedDrawingId,
  hasDrawings,
  showDrawingLibrary,
  canSaveDrawings,
  onSaveDrawingsClick,
  onDrawingHistoryClick,
  onDeleteSelectedDrawing,
  children,
  compareSlots,
  compareCandidates,
  mainAssetId,
  onCompareSlotSet,
  chartType,
  onChartTypeChange,
  measureToolActive,
  onMeasureToolToggle,
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
    {
      key: 'news',
      text: chartNewsLoading ? `${t('chartFrame.layerStripNews')}…` : t('chartFrame.layerStripNews'),
      aria: chartNewsFavoritesOnly ? t('chartFrame.showFavoriteNews') : t('controls.showNews'),
      on: showNewsOnChart,
      toggle: onToggleNews,
      favoriteStar: showChartNewsFavoriteStar,
    },
    { key: 'ma20', text: t('chartFrame.layerStripMA20'), aria: t('controls.showMA20'), on: showMA20, toggle: onToggleMA20 },
    { key: 'ma50', text: t('chartFrame.layerStripMA50'), aria: t('controls.showMA50'), on: showMA50, toggle: onToggleMA50 },
    { key: 'rsi', text: t('chartFrame.layerStripRSI'), aria: t('controls.showRSI'), on: showRsi, toggle: onToggleRsi },
    { key: 'vol', text: t('chartFrame.layerStripVolume'), aria: t('deck.showVolume'), on: showVolume, toggle: onToggleVolume },
  ]

  return (
    <div className="fi-analysis-chart-workbench">
      <header className="fi-chart-frame-top">
        <div className="fi-chart-frame-layers-strip" role="toolbar" aria-label={t('deck.analysisLayers')}>
          {layerStrip.map((layer) =>
            layer.key === 'news' && layer.favoriteStar ? (
              <div
                key={layer.key}
                className={`fi-chart-layer-news-combo${layer.on ? ' fi-chart-layer-news-combo--on' : ''}${
                  chartNewsFavoritesOnly ? ' fi-chart-layer-news-combo--favorites' : ''
                }`}
              >
                <button
                  type="button"
                  role="switch"
                  aria-checked={layer.on}
                  title={layer.aria}
                  aria-label={layer.aria}
                  className="fi-chart-layer-news-combo-main"
                  onClick={layer.toggle}
                >
                  <span className="fi-chart-layer-strip-label">{layer.text}</span>
                </button>
                <span className="fi-chart-layer-news-combo-divider" aria-hidden="true" />
                <button
                  type="button"
                  aria-label={chartNewsFavoritesOnly ? t('chartFrame.unfavoriteNews') : t('chartFrame.favoriteNews')}
                  aria-pressed={chartNewsFavoritesOnly}
                  title={chartNewsFavoritesOnly ? t('chartFrame.unfavoriteNews') : t('chartFrame.favoriteNews')}
                  className={`fi-chart-layer-news-combo-star${chartNewsFavoritesOnly ? ' fi-chart-layer-news-combo-star--on' : ''}`}
                  onClick={(event) => {
                    event.stopPropagation()
                    onToggleChartNewsFavorites?.()
                  }}
                >
                  {chartNewsFavoritesOnly ? '★' : '☆'}
                </button>
              </div>
            ) : (
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
            ),
          )}
        </div>

        <div className="fi-chart-frame-center-tools">
          <button
            type="button"
            className={`fi-chart-measure-btn${measureToolActive ? ' fi-chart-measure-btn--active' : ''}`}
            aria-pressed={measureToolActive}
            title={t('chartFrame.measureTool')}
            aria-label={t('chartFrame.measureTool')}
            onClick={onMeasureToolToggle}
          >
            <IconChartMeasure />
            <span className="fi-chart-type-btn-label">{t('chartFrame.measureToolShort')}</span>
          </button>
          <span className="fi-chart-frame-tools-divider" aria-hidden="true" />
          <div className="fi-chart-frame-chart-type" role="group" aria-label={t('chartFrame.chartTypeAria')}>
          <button
            type="button"
            className={`fi-chart-type-btn${chartType === 'candle' ? ' fi-chart-type-btn--active' : ''}`}
            aria-pressed={chartType === 'candle'}
            title={t('chartFrame.chartTypeCandle')}
            onClick={() => onChartTypeChange('candle')}
          >
            <IconCandles />
            <span className="fi-chart-type-btn-label">{t('chartFrame.chartTypeCandleShort')}</span>
          </button>
          <button
            type="button"
            className={`fi-chart-type-btn${chartType === 'line' ? ' fi-chart-type-btn--active' : ''}`}
            aria-pressed={chartType === 'line'}
            title={t('chartFrame.chartTypeLine')}
            onClick={() => onChartTypeChange('line')}
          >
            <IconLineChart />
            <span className="fi-chart-type-btn-label">{t('chartFrame.chartTypeLineShort')}</span>
          </button>
          </div>
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
          <div className="fi-chart-draw-tool-list" role="toolbar">
            {CHART_DRAW_TOOLS.map(({ id, Icon }) => (
              <div key={id} className="fi-chart-draw-tool-cell">
                <button
                  type="button"
                  className={`fi-chart-draw-tool-btn${drawTool === id ? ' fi-chart-draw-tool-btn--active' : ''}`}
                  onClick={() => onDrawToolChange(id)}
                  title={drawToolTitle(id, t)}
                  aria-label={drawToolTitle(id, t)}
                  aria-pressed={drawTool === id}
                >
                  <Icon />
                </button>
                {id !== 'none' && drawTool === id ? (
                  <DrawToolColorPicker
                    tool={id}
                    color={drawColorsByTool[id]}
                    onColorChange={onDrawColorChange}
                  />
                ) : null}
              </div>
            ))}
          </div>
          <div className="fi-chart-draw-tool-divider" aria-hidden="true" />
          {showDrawingLibrary ? (
            <div className="fi-chart-draw-tool-library" role="group" aria-label={t('chartDrawings.libraryAria')}>
              <button
                type="button"
                className={`fi-chart-draw-tool-btn fi-chart-draw-tool-btn--save${canSaveDrawings ? '' : ' fi-chart-draw-tool-btn--disabled'}`}
                onPointerDown={(event) => event.stopPropagation()}
                onClick={(event) => {
                  event.preventDefault()
                  event.stopPropagation()
                  onSaveDrawingsClick()
                }}
                title={t('chartDrawings.save')}
                aria-label={t('chartDrawings.save')}
              >
                <DrawIconPlus />
              </button>
              <button
                type="button"
                className="fi-chart-draw-tool-btn fi-chart-draw-tool-btn--history"
                onPointerDown={(event) => event.stopPropagation()}
                onClick={(event) => {
                  event.preventDefault()
                  event.stopPropagation()
                  onDrawingHistoryClick()
                }}
                title={t('chartDrawings.history')}
                aria-label={t('chartDrawings.history')}
              >
                <DrawIconHistory />
              </button>
            </div>
          ) : null}
          <div className="fi-chart-draw-tool-divider" aria-hidden="true" />
          <button
            type="button"
            className={`fi-chart-draw-tool-btn fi-chart-draw-tool-btn--delete${selectedDrawingId || hasDrawings ? '' : ' fi-chart-draw-tool-btn--disabled'}`}
            onPointerDown={(event) => event.stopPropagation()}
            onClick={(event) => {
              event.preventDefault()
              event.stopPropagation()
              onDeleteSelectedDrawing()
            }}
            title={t('chartFrame.drawDelete')}
            aria-label={t('chartFrame.drawDelete')}
          >
            <DrawIconTrash />
          </button>
        </aside>
        <div className="fi-chart-frame-plot">{children}</div>
      </div>
    </div>
  )
}
