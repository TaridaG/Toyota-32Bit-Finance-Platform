import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { tickerItems, newsPoints, topGainers, topLosers } from './mockData'
import type { NewsCategory, NewsDataPoint, SentimentType } from './types'
import { MarketTicker } from './components/MarketTicker'
import { NewsCard } from './components/NewsCard'
import { TrendingList } from './components/TrendingList'
import { SentimentChart } from './components/SentimentChart'
import { InsightBox } from './components/InsightBox'

export function NewsPage() {
  const { t } = useTranslation()
  const [selectedCategory, setSelectedCategory] = useState<NewsCategory>('all')
  const [selectedRange, setSelectedRange] = useState<'1h' | '6h' | '24h'>('24h')
  const [selectedSentiment, setSelectedSentiment] = useState<'all' | SentimentType>('all')
  const [selectedNews, setSelectedNews] = useState<NewsDataPoint | null>(null)
  useDocumentTitle(t('newsPage.titleDoc'))

  const filteredNews = useMemo(() => {
    const byCategory = selectedCategory === 'all' ? newsPoints : newsPoints.filter((item) => item.category === selectedCategory)
    const bySentiment =
      selectedSentiment === 'all' ? byCategory : byCategory.filter((item) => item.sentiment === selectedSentiment)
    const maxMinutes = selectedRange === '1h' ? 60 : selectedRange === '6h' ? 360 : 1440
    return bySentiment.filter((item) => item.timeAgoMinutes <= maxMinutes)
  }, [selectedCategory, selectedRange, selectedSentiment])

  const sentimentCounts = useMemo(
    () => ({
      positive: filteredNews.filter((item) => item.sentiment === 'positive').length,
      negative: filteredNews.filter((item) => item.sentiment === 'negative').length,
      neutral: filteredNews.filter((item) => item.sentiment === 'neutral').length,
    }),
    [filteredNews],
  )

  const aiInsight = useMemo(() => {
    if (sentimentCounts.positive > sentimentCounts.negative) {
      return 'Bankacilik ve buyuk hacimli hisselerde son haber akisina bagli olarak birikim egilimi gucleniyor.'
    }
    if (sentimentCounts.negative > sentimentCounts.positive) {
      return 'Kisa vadede korumaci pozisyonlanma one cikiyor; volatil varliklarda risk azaltimi izleniyor.'
    }
    return 'Piyasa haber akisinda denge korunuyor; sektor bazli secici yaklasim daha rasyonel gorunuyor.'
  }, [sentimentCounts.negative, sentimentCounts.positive])

  return (
    <>
      <section className="fi-news-page">
        <header className="card fi-news-header">
          <div>
            <p className="fi-news-kicker">{t('newsPage.kicker')}</p>
            <h2>{t('newsPage.title')}</h2>
            <p>{t('newsPage.lead')}</p>
          </div>
          <div className="fi-news-ticker-grid">
            {tickerItems.map((item) => (
              <MarketTicker key={item.symbol} item={item} />
            ))}
          </div>
        </header>

        <section className="card fi-filter-bar">
          <div className="fi-filter-group">
            <span>Category</span>
            <div>
              {(['all', 'bist', 'viop', 'fx', 'crypto', 'macro'] as const).map((category) => (
                <button
                  key={category}
                  type="button"
                  className={`fi-filter-chip${selectedCategory === category ? ' fi-filter-chip-active' : ''}`}
                  onClick={() => setSelectedCategory(category)}
                >
                  {t(`newsPage.categories.${category}`)}
                </button>
              ))}
            </div>
          </div>

          <div className="fi-filter-group">
            <span>Time Range</span>
            <div>
              {(['1h', '6h', '24h'] as const).map((range) => (
                <button
                  key={range}
                  type="button"
                  className={`fi-filter-chip${selectedRange === range ? ' fi-filter-chip-active' : ''}`}
                  onClick={() => setSelectedRange(range)}
                >
                  {range}
                </button>
              ))}
            </div>
          </div>

          <div className="fi-filter-group">
            <span>Sentiment</span>
            <div>
              {(['all', 'positive', 'negative', 'neutral'] as const).map((sentiment) => (
                <button
                  key={sentiment}
                  type="button"
                  className={`fi-filter-chip${selectedSentiment === sentiment ? ' fi-filter-chip-active' : ''}`}
                  onClick={() => setSelectedSentiment(sentiment)}
                >
                  {sentiment}
                </button>
              ))}
            </div>
          </div>
        </section>

        <div className="fi-main-grid">
          <article className="card fi-news-feed">
            <div className="fi-panel-head">
              <h3>{t('newsPage.streamTitle')}</h3>
            </div>
            <div className="fi-news-list">
              {filteredNews.length > 0 ? (
                filteredNews.map((item) => <NewsCard key={item.id} item={item} onOpen={setSelectedNews} />)
              ) : (
                <p className="fi-empty">{t('newsPage.empty')}</p>
              )}
            </div>
          </article>

          <aside className="fi-right-column">
            <TrendingList title={t('newsPage.risingTitle')} items={topGainers} />
            <TrendingList title={t('newsPage.fallingTitle')} items={topLosers} />
            <SentimentChart counts={sentimentCounts} />
            <InsightBox text={aiInsight} />
          </aside>
        </div>
      </section>

      {selectedNews ? (
        <div className="fi-modal-wrap" role="dialog" aria-modal="true">
          <button className="fi-modal-backdrop" onClick={() => setSelectedNews(null)} aria-label="close details" />
          <article className="card fi-modal">
            <div className="fi-modal-head">
              <h3>{selectedNews.title}</h3>
              <button type="button" onClick={() => setSelectedNews(null)}>
                ×
              </button>
            </div>
            <p>{selectedNews.details}</p>
            <p className="fi-modal-corr">{selectedNews.correlationNote}</p>
            <div className="fi-modal-tags">
              {selectedNews.relatedAssets.map((asset) => (
                <span key={asset}>{asset}</span>
              ))}
            </div>
          </article>
        </div>
      ) : null}
    </>
  )
}
