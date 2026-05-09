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
      priceLineVisible: true,
      lastValueVisible: true,
      priceLineColor: '#38bdf8',
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
  const prevCandlesRef = useRef<CandlePoint[]>([])

  useEffect(() => {
    if (chart !== prevChartRef.current) {
      prevFitKeyRef.current = null
      prevChartRef.current = chart
    }

    if (!chart || !series) return
    try {
      const prev = prevCandlesRef.current
      const canIncremental =
        prev.length > 0 &&
        candles.length >= prev.length &&
        prev.every((point, index) => candles[index]?.time === point.time)

      if (canIncremental) {
        for (let i = prev.length - 1; i < candles.length; i += 1) {
          if (i < 0) continue
          series.candle.update(toCandlestickData(candles[i]))
          series.volume.update(toVolumeHistogramData(candles[i]))
        }
        if (candles.length > prev.length) {
          chart.timeScale().scrollToRealTime()
        }
      } else {
        series.candle.setData(candles.map(toCandlestickData))
        series.volume.setData(candles.map(toVolumeHistogramData))
      }
      prevCandlesRef.current = candles

      const shouldFit = prevFitKeyRef.current === null || prevFitKeyRef.current !== fitContentKey
      prevFitKeyRef.current = fitContentKey
      if (shouldFit) {
        chart.timeScale().fitContent()
      }
    } catch {
      /* unmount / chart.remove racing with data update */
    }
  }, [chart, series, candles, fitContentKey])
}
