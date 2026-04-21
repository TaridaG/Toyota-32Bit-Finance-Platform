import { useEffect, useLayoutEffect, useRef } from 'react'
import { LineSeries, type IChartApi, type ISeriesApi } from 'lightweight-charts'
import type { CandlePoint } from '../../types'
import { calculateMovingAverage } from '../utils/movingAverage'
import type { CandleVolumeSeries } from './useSeries'

function safeRemoveLine(chart: IChartApi, series: ISeriesApi<'Line'>) {
  try {
    chart.removeSeries(series)
  } catch {
    /* chart may be disposed */
  }
}

export type MaSeriesBundle = {
  ma20: ISeriesApi<'Line'>
  ma50: ISeriesApi<'Line'>
}

export function useMovingAverageIndicators(
  chart: IChartApi | null,
  seriesBundle: CandleVolumeSeries | null,
  candles: CandlePoint[],
  visible: boolean,
) {
  const maRef = useRef<MaSeriesBundle | null>(null)
  const ma20Data = calculateMovingAverage(candles, 20)
  const ma50Data = calculateMovingAverage(candles, 50)

  useLayoutEffect(() => {
    if (!chart || !seriesBundle) return

    const ma20 = chart.addSeries(LineSeries, {
      color: '#38bdf8',
      lineWidth: 2,
      priceLineVisible: false,
      lastValueVisible: false,
      visible: true,
    })
    const ma50 = chart.addSeries(LineSeries, {
      color: '#a78bfa',
      lineWidth: 2,
      priceLineVisible: false,
      lastValueVisible: false,
      visible: true,
    })

    const mainPane = seriesBundle.candle.getPane().paneIndex()
    ma20.moveToPane(mainPane)
    ma50.moveToPane(mainPane)

    maRef.current = { ma20, ma50 }

    return () => {
      maRef.current = null
      safeRemoveLine(chart, ma20)
      safeRemoveLine(chart, ma50)
    }
  }, [chart, seriesBundle])

  useEffect(() => {
    const m = maRef.current
    if (!m) return
    m.ma20.setData(ma20Data)
    m.ma50.setData(ma50Data)
    m.ma20.applyOptions({ visible })
    m.ma50.applyOptions({ visible })
  }, [chart, seriesBundle, ma20Data, ma50Data, visible])
}
