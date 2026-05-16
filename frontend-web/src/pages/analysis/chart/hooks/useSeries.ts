import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import {
  CandlestickSeries,
  HistogramSeries,
  LineSeries,
  type IChartApi,
  type ISeriesApi,
} from 'lightweight-charts'
import type { CandlePoint, ChartDisplayType } from '../../types'
import { toCandlestickData, toLineData, toVolumeHistogramData } from '../utils/candleMappers'

export type MainPriceSeries = {
  chartType: ChartDisplayType
  volume: ISeriesApi<'Histogram'>
  main: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'>
}

/** @deprecated Use MainPriceSeries */
export type CandleVolumeSeries = MainPriceSeries & {
  candle: ISeriesApi<'Candlestick'>
}

function safeRemoveSeries(
  chart: IChartApi,
  series: ISeriesApi<'Candlestick' | 'Line' | 'Histogram'>,
) {
  try {
    chart.removeSeries(series)
  } catch {
    /* chart may be disposed */
  }
}

export function useMainPriceSeries(
  chart: IChartApi | null,
  chartType: ChartDisplayType,
): MainPriceSeries | null {
  const [bundle, setBundle] = useState<MainPriceSeries | null>(null)

  useLayoutEffect(() => {
    if (!chart) return

    const volume = chart.addSeries(HistogramSeries, {
      priceFormat: { type: 'volume' },
      priceScaleId: '',
    })
    volume.priceScale().applyOptions({
      scaleMargins: { top: 0.86, bottom: 0 },
    })

    if (chartType === 'candle') {
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
      setBundle({ chartType: 'candle', volume, main: candle })
      return () => {
        setBundle(null)
        safeRemoveSeries(chart, candle)
        safeRemoveSeries(chart, volume)
      }
    }

    const line = chart.addSeries(LineSeries, {
      color: '#38bdf8',
      lineWidth: 2,
      priceLineVisible: true,
      lastValueVisible: true,
      crosshairMarkerVisible: true,
      crosshairMarkerRadius: 4,
    })
    setBundle({ chartType: 'line', volume, main: line })
    return () => {
      setBundle(null)
      safeRemoveSeries(chart, line)
      safeRemoveSeries(chart, volume)
    }
  }, [chart, chartType])

  return bundle
}

export function useMainPriceData(
  chart: IChartApi | null,
  series: MainPriceSeries | null,
  candles: CandlePoint[],
  fitContentKey: string,
) {
  const prevFitKeyRef = useRef<string | null>(null)
  const prevChartRef = useRef<IChartApi | null>(null)
  const prevSeriesRef = useRef<MainPriceSeries | null>(null)
  const prevCandlesRef = useRef<CandlePoint[]>([])

  useEffect(() => {
    if (chart !== prevChartRef.current) {
      prevFitKeyRef.current = null
      prevChartRef.current = chart
      prevSeriesRef.current = null
      prevCandlesRef.current = []
    }

    if (!chart || !series) return

    const seriesChanged = series !== prevSeriesRef.current
    if (seriesChanged) {
      prevSeriesRef.current = series
      prevCandlesRef.current = []
    }

    try {
      const prev = prevCandlesRef.current
      const canIncremental =
        !seriesChanged &&
        prev.length > 0 &&
        candles.length >= prev.length &&
        prev.every((point, index) => candles[index]?.time === point.time)

      if (series.chartType === 'candle') {
        const candleSeries = series.main as ISeriesApi<'Candlestick'>
        if (canIncremental) {
          for (let i = prev.length - 1; i < candles.length; i += 1) {
            if (i < 0) continue
            candleSeries.update(toCandlestickData(candles[i]))
            series.volume.update(toVolumeHistogramData(candles[i]))
          }
        } else {
          candleSeries.setData(candles.map(toCandlestickData))
          series.volume.setData(candles.map((c) => toVolumeHistogramData(c)))
        }
      } else {
        const lineSeries = series.main as ISeriesApi<'Line'>
        if (canIncremental) {
          for (let i = prev.length - 1; i < candles.length; i += 1) {
            if (i < 0) continue
            lineSeries.update(toLineData(candles[i]))
            const prevClose = i > 0 ? candles[i - 1].close : candles[i].open
            series.volume.update(toVolumeHistogramData(candles[i], prevClose))
          }
        } else {
          lineSeries.setData(candles.map(toLineData))
          series.volume.setData(
            candles.map((candle, index) =>
              toVolumeHistogramData(candle, index > 0 ? candles[index - 1].close : candle.open),
            ),
          )
        }
      }

      if (candles.length > prev.length) {
        chart.timeScale().scrollToRealTime()
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
