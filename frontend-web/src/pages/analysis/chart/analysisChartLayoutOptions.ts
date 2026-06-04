import { ColorType, CrosshairMode } from 'lightweight-charts'

/** Lightweight Charts layout/grid options for the analysis workbench (theme-aware). */
export function getAnalysisChartLayoutOptions(isDark: boolean) {
  if (isDark) {
    return {
      layout: {
        background: { type: ColorType.Solid, color: '#0f172a' },
        textColor: '#cbd5e1',
      },
      rightPriceScale: { borderColor: '#334155' },
      timeScale: {
        borderColor: '#334155',
        timeVisible: true,
        secondsVisible: false,
        rightOffset: 40,
        fixRightEdge: false,
      },
      grid: {
        vertLines: { color: 'rgba(148, 163, 184, 0.12)' },
        horzLines: { color: 'rgba(148, 163, 184, 0.12)' },
      },
      crosshair: {
        mode: CrosshairMode.MagnetOHLC,
        vertLine: { color: '#64748b', labelBackgroundColor: '#334155' },
        horzLine: { color: '#64748b', labelBackgroundColor: '#334155' },
      },
      handleScroll: true,
      handleScale: true,
    } as const
  }

  return {
    layout: {
      background: { type: ColorType.Solid, color: '#ffffff' },
      textColor: '#64748b',
    },
    rightPriceScale: { borderColor: '#e2e8f0' },
    timeScale: {
      borderColor: '#e2e8f0',
      timeVisible: true,
      secondsVisible: false,
      rightOffset: 40,
      fixRightEdge: false,
    },
    grid: {
      vertLines: { color: 'rgba(100, 116, 139, 0.14)' },
      horzLines: { color: 'rgba(100, 116, 139, 0.14)' },
    },
    crosshair: {
      mode: CrosshairMode.MagnetOHLC,
      vertLine: { color: '#94a3b8', labelBackgroundColor: '#e2e8f0' },
      horzLine: { color: '#94a3b8', labelBackgroundColor: '#e2e8f0' },
    },
    handleScroll: true,
    handleScale: true,
  } as const
}
