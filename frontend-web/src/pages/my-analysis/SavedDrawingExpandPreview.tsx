import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import {
  fetchChartDrawingSave,
  hydrateSavedDrawings,
  type ChartDrawingSaveSummary,
  type SavedDrawingPayload,
} from '../../features/analysis/api/chartDrawingService'
import { useCandles } from '../../features/analysis/hooks/useCandles'
import { AnalysisChart } from '../analysis/components/AnalysisChart'
import type { DrawingItem } from '../analysis/types'
import {
  formatSavedAt,
  inferWireCategory,
  resolveAssetType,
  resolveChartRangeForSave,
} from './savedDrawingChartUtils'

type SavedDrawingExpandPreviewProps = {
  summary: ChartDrawingSaveSummary
}

export function SavedDrawingExpandPreview({ summary }: SavedDrawingExpandPreviewProps) {
  const { t, i18n } = useTranslation(['myAnalysisPage', 'analysis', 'common'])
  const chartRange = resolveChartRangeForSave(summary)
  const wireCategory = inferWireCategory(summary.assetType)
  const assetType = resolveAssetType(summary.assetType)

  const { candles, loading: candlesLoading, error: candlesError, refetch } = useCandles(
    summary.assetSymbol,
    chartRange,
    { wireCategory },
  )

  const [drawings, setDrawings] = useState<DrawingItem[]>([])
  const [detailLoading, setDetailLoading] = useState(true)
  const [detailError, setDetailError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setDetailLoading(true)
    setDetailError(null)
    void fetchChartDrawingSave(summary.id)
      .then((detail) => {
        if (cancelled) return
        const payloads = detail.drawings as SavedDrawingPayload[]
        setDrawings(hydrateSavedDrawings(payloads))
      })
      .catch(() => {
        if (!cancelled) setDetailError(t('myAnalysisPage:expandLoadError'))
      })
      .finally(() => {
        if (!cancelled) setDetailLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [summary.id, t])

  const loading = detailLoading || (candlesLoading && candles.length === 0)
  const analysisSymbol = summary.assetSymbol.replace(/[^A-Za-z0-9]/g, '').toUpperCase()

  return (
    <div className="fi-my-analysis-expand" role="region" aria-label={t('myAnalysisPage:expandAria', { name: summary.name })}>
      <div className="fi-my-analysis-expand-meta">
        <span>
          {t('myAnalysisPage:expandSymbol', { symbol: summary.assetSymbol })}
        </span>
        <span>{t('myAnalysisPage:expandRange', { range: chartRange.toUpperCase() })}</span>
        <span>{formatSavedAt(summary.createdAt, i18n.language)}</span>
        <Link
          to={`/app/analysis?symbol=${encodeURIComponent(analysisSymbol)}`}
          className="my-portfolio-card-link fi-my-analysis-open-analysis"
        >
          {t('myAnalysisPage:openInAnalysis')}
        </Link>
      </div>

      {loading ? <p className="fi-empty">{t('common:loading')}</p> : null}
      {detailError ? <p className="fi-chart-drawing-save-error">{detailError}</p> : null}
      {candlesError && !loading ? (
        <div className="markets-error-wrap">
          <span>{t('analysis:chartLoadError')}</span>
          <button type="button" className="markets-filter" onClick={() => void refetch()}>
            {t('common:retry')}
          </button>
        </div>
      ) : null}

      {!loading && !detailError && !candlesError ? (
        <div className="fi-my-analysis-chart-wrap">
          <AnalysisChart
            embedded
            candles={candles}
            chartType="candle"
            fitContentKey={`save-${summary.id}-${chartRange}`}
            comparisonLines={[]}
            showCompare={false}
            showMA20={false}
            showMA50={false}
            showRSI={false}
            movingAverageData={{ ma20: [], ma50: [] }}
            rsiData={[]}
            showEventMarkers={false}
            showVolume={false}
            newsItems={[]}
            tradeEvents={[]}
            selectedNewsId={null}
            onSelectNews={() => {}}
            drawTool="none"
            activeDrawColor={null}
            drawings={drawings}
            selectedDrawingId={null}
            onAddDrawing={() => {}}
            onSelectDrawing={() => {}}
            locale={i18n.language}
            currency="USD"
            assetType={assetType}
          />
        </div>
      ) : null}
    </div>
  )
}
