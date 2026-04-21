import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { CandlestickSeries, HistogramSeries, type IChartApi, type ISeriesApi } from 'lightweight-charts'
import type { CandlePoint } from '../../types'
import { toCandlestickData, toVolumeHistogramData } from '../utils/candleMappers'

export type CandleVolumeSeries = {
  candle: ISeriesApi<'Candlestick'>
  volume: ISeriesApi<'Histogram'>
}

function safeRemoveSeries(chart: IChartApi, series: ISeriesApi<'Candlestick' | 'Histogram'>) {
  try {
    chart.removeSeries(series)
  } catch {
    /* chart may be disposed */
  }
}

export function useCandleVolumeSeries(chart: IChartApi | null): CandleVolumeSeries | null {
  const [bundle, setBundle] = useState<CandleVolumeSeries | null>(null)

  useLayoutEffect(() => {
    if (!chart) return

    const candle = chart.addSeries(CandlestickSeries, {
      upColor: '#22c55e',
      downColor: '#ef4444',
      wickUpColor: '#22c55e',
      wickDownColor: '#ef4444',
      borderVisible: false,
    })

    const volume = chart.addSeries(HistogramSeries, {
      priceFormat: { type: 'volume' },
      priceScaleId: '',
    })
    volume.priceScale().applyOptions({
      scaleMargins: { top: 0.86, bottom: 0 },
    })

    // eslint-disable-next-line react-hooks/set-state-in-effect -- bridge chart series handles into React for downstream hooks
    setBundle({ candle, volume })

    return () => {
      setBundle(null)
      safeRemoveSeries(chart, candle)
      safeRemoveSeries(chart, volume)
    }
  }, [chart])

  return bundle
}

export function useCandleVolumeData(
  chart: IChartApi | null,
  series: CandleVolumeSeries | null,
  candles: CandlePoint[],
  fitContentKey: string,
) {
  const prevFitKeyRef = useRef<string | null>(null)
  const prevChartRef = useRef<IChartApi | null>(null)

  useEffect(() => {
    if (chart !== prevChartRef.current) {
      prevFitKeyRef.current = null
      prevChartRef.current = chart
    }

    if (!chart || !series) return

    series.candle.setData(candles.map(toCandlestickData))
    series.volume.setData(candles.map(toVolumeHistogramData))

    const shouldFit = prevFitKeyRef.current === null || prevFitKeyRef.current !== fitContentKey
    prevFitKeyRef.current = fitContentKey
    if (shouldFit) {
      chart.timeScale().fitContent()
    }
  }, [chart, series, candles, fitContentKey])
}
