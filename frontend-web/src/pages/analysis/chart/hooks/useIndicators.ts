import { useEffect, useLayoutEffect, useRef } from 'react'
import { LineSeries, type IChartApi, type ISeriesApi, type LineData, type Time } from 'lightweight-charts'
import type { MainPriceSeries } from './useSeries'

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
  seriesBundle: MainPriceSeries | null,
  maData: { ma20: LineData<Time>[]; ma50: LineData<Time>[] },
  visibility: { ma20Visible: boolean; ma50Visible: boolean },
) {
  const maRef = useRef<MaSeriesBundle | null>(null)

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

    const mainPane = seriesBundle.main.getPane().paneIndex()
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
    m.ma20.setData(maData.ma20)
    m.ma50.setData(maData.ma50)
    m.ma20.applyOptions({ visible: visibility.ma20Visible })
    m.ma50.applyOptions({ visible: visibility.ma50Visible })
  }, [chart, maData.ma20, maData.ma50, seriesBundle, visibility.ma20Visible, visibility.ma50Visible])

}

type RsiSeriesBundle = {
  rsi: ISeriesApi<'Line'>
}

export function useRsiIndicator(
  chart: IChartApi | null,
  seriesBundle: MainPriceSeries | null,
  rsiData: LineData<Time>[],
  visible: boolean,
) {
  const rsiRef = useRef<RsiSeriesBundle | null>(null)
  const rsiBandsRef = useRef<{ upper: ISeriesApi<'Line'>; lower: ISeriesApi<'Line'> } | null>(null)

  useLayoutEffect(() => {
    if (!chart || !seriesBundle) return

    const rsi = chart.addSeries(LineSeries, {
      color: '#f59e0b',
      lineWidth: 2,
      priceLineVisible: false,
      lastValueVisible: true,
    })
    const upper = chart.addSeries(LineSeries, {
      color: 'rgba(239,68,68,0.75)',
      lineWidth: 1,
      lineStyle: 2,
      priceLineVisible: false,
      lastValueVisible: false,
    })
    const lower = chart.addSeries(LineSeries, {
      color: 'rgba(34,197,94,0.75)',
      lineWidth: 1,
      lineStyle: 2,
      priceLineVisible: false,
      lastValueVisible: false,
    })

    const mainPane = seriesBundle.main.getPane().paneIndex()
    rsi.moveToPane(mainPane + 1)
    upper.moveToPane(mainPane + 1)
    lower.moveToPane(mainPane + 1)
    rsi.priceScale().applyOptions({ scaleMargins: { top: 0.1, bottom: 0.1 } })
    rsiRef.current = { rsi }
    rsiBandsRef.current = { upper, lower }

    return () => {
      rsiRef.current = null
      rsiBandsRef.current = null
      safeRemoveLine(chart, rsi)
      safeRemoveLine(chart, upper)
      safeRemoveLine(chart, lower)
    }
  }, [chart, seriesBundle])

  useEffect(() => {
    const r = rsiRef.current
    const bands = rsiBandsRef.current
    if (!r || !bands) return
    r.rsi.setData(rsiData)
    const bandData = rsiData.map((point) => ({ time: point.time, value: 70 }))
    const lowerBandData = rsiData.map((point) => ({ time: point.time, value: 30 }))
    bands.upper.setData(bandData)
    bands.lower.setData(lowerBandData)
    r.rsi.applyOptions({ visible })
    bands.upper.applyOptions({ visible })
    bands.lower.applyOptions({ visible })
  }, [rsiData, visible])

}
