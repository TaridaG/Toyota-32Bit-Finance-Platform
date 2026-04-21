import { useEffect, useRef } from 'react'
import { LineSeries, type IChartApi, type ISeriesApi, type LineData, type Time } from 'lightweight-charts'

export type ComparisonLine = {
  id: string
  color: string
  data: LineData<Time>[]
}

function safeRemove(chart: IChartApi, series: ISeriesApi<'Line'>) {
  try {
    chart.removeSeries(series)
  } catch {
    /* disposed */
  }
}

export function useCompareSeries(chart: IChartApi | null, lines: ComparisonLine[], enabled: boolean) {
  const lineRefs = useRef<ISeriesApi<'Line'>[]>([])

  useEffect(() => {
    if (!chart) return

    lineRefs.current.forEach((s) => safeRemove(chart, s))
    lineRefs.current = []

    if (!enabled) return

    lines.forEach((line) => {
      const series = chart.addSeries(LineSeries, {
        color: line.color,
        lineWidth: 2,
        priceLineVisible: false,
        lastValueVisible: false,
        priceScaleId: 'compare',
      })
      series.setData(line.data)
      lineRefs.current.push(series)
    })

    if (lineRefs.current.length > 0) {
      chart.priceScale('compare').applyOptions({
        scaleMargins: { top: 0.05, bottom: 0.05 },
      })
    }

    return () => {
      lineRefs.current.forEach((s) => safeRemove(chart, s))
      lineRefs.current = []
    }
  }, [chart, enabled, lines])
}
