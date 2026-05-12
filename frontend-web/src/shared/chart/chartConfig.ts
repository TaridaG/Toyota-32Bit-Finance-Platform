/**
 * Time-scale defaults aligned with pro terminals: no infinite future pan,
 * minimal right gutter, stable zoom on resize.
 */
export const ENTERPRISE_LINE_CHART_TIME_SCALE = {
  /** Sağ sınır uygulama kodu + setVisibleRange; native fix kapalı — pan kilidi olmasın. */
  fixRightEdge: false,
  fixLeftEdge: false,
  rightOffset: 2,
  lockVisibleTimeRangeOnResize: true,
  shiftVisibleRangeOnNewBar: false,
  /** Zoom out sonrası noktalar birbirine yapışmasın (terminal hissi). */
  minBarSpacing: 1,
  /** 0 = üst sınır yok (LWC); uzak zoom’da bar aralığı kısıtlanmaz. */
  maxBarSpacing: 0,
  /** true iken sürükleyerek geçmişe kayma titreyebiliyor (imleç altı çubuğu sabitleme). */
  rightBarStaysOnScroll: false,
}

export const ENTERPRISE_KINETIC_SCROLL = {
  mouse: false,
  touch: true,
} as const

/** Görünür span’a göre timeScale / tick sunumu (sn). */
export const CHART_SPAN_BANDS = {
  /** Bu sürenin altında saat+dakika eksende. */
  timeVisibleMaxSec: 48 * 3600,
  /** Bunun altında saniye (21:00:30); üstünde saat:dakika. */
  secondsVisibleBelowSec: 15 * 60,
} as const
