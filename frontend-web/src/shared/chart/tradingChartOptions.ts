import { ColorType, CrosshairMode, type Time, type TickMarkFormatter } from 'lightweight-charts'
import { ENTERPRISE_KINETIC_SCROLL, ENTERPRISE_LINE_CHART_TIME_SCALE } from './chartConfig'

const DAY_SEC = 86_400
const TWO_HOUR_SEC = 2 * 3600

export type TradingChartPresentationArgs = {
  width: number
  height: number
  isDark: boolean
  locale: string
  maskAmounts: boolean
  visibleSpanSec: number
  tickMarkFormatter: TickMarkFormatter
  timeFormatter: (t: Time) => string
  valueFormatter?: (value: number) => string
}

export function buildTradingAreaChartOptions(a: TradingChartPresentationArgs) {
  return {
    width: a.width,
    height: a.height,
    layout: {
      background: { type: ColorType.Solid, color: 'transparent' },
      textColor: a.isDark ? '#94a3b8' : '#64748b',
      fontSize: 11,
      attributionLogo: false,
    },
    localization: {
      locale: a.locale,
      timeFormatter: a.timeFormatter,
      dateFormat: "dd MMM ''yy" as const,
      ...(a.maskAmounts ? { priceFormatter: () => '••' } : a.valueFormatter ? { priceFormatter: a.valueFormatter } : {}),
    },
    grid: {
      vertLines: { color: 'rgba(148, 163, 184, 0.07)' },
      horzLines: { color: 'rgba(148, 163, 184, 0.07)' },
    },
    rightPriceScale: {
      borderVisible: false,
      scaleMargins: { top: 0.12, bottom: 0.08 },
    },
    timeScale: {
      ...ENTERPRISE_LINE_CHART_TIME_SCALE,
      lockVisibleTimeRangeOnResize: true,
      borderVisible: false,
      shiftVisibleRangeOnNewBar: false,
      visible: true,
      ticksVisible: true,
      /** Dar pencerede saat; 1Y+ için saniye asla (formatter ile uyumlu). */
      timeVisible: a.visibleSpanSec <= DAY_SEC,
      secondsVisible: a.visibleSpanSec < TWO_HOUR_SEC,
      tickMarkFormatter: a.tickMarkFormatter,
    },
    crosshair: {
      mode: CrosshairMode.Magnet,
      vertLine: { color: 'rgba(148, 163, 184, 0.35)', labelBackgroundColor: a.isDark ? '#334155' : '#e2e8f0' },
      horzLine: { color: 'rgba(148, 163, 184, 0.35)', labelBackgroundColor: a.isDark ? '#334155' : '#e2e8f0' },
    },
    handleScroll: {
      mouseWheel: false,
      pressedMouseMove: true,
      horzTouchDrag: true,
      vertTouchDrag: false,
    },
    handleScale: {
      axisPressedMouseMove: true,
      mouseWheel: false,
      pinch: true,
    },
    kineticScroll: ENTERPRISE_KINETIC_SCROLL,
  }
}
