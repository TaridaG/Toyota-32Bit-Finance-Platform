import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { fetchFavoriteNewsEnriched } from '../../../features/news/api/newsFavoritesApi'
import { mapNewsItem, stripHtml } from '../../news/utils/newsItemMappers'
import type { NewsDataPoint } from '../../news/types'

const PREVIEW_SIZE = 4

export function FavoriteNewsInsightCard() {
  const { t, i18n } = useTranslation(['portfolio', 'newsPage', 'common'])
  const [items, setItems] = useState<NewsDataPoint[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    void fetchFavoriteNewsEnriched(0, PREVIEW_SIZE, i18n.language)
      .then((page) => {
        if (!cancelled) {
          setItems(
            (page.content ?? []).map((row) =>
              mapNewsItem(row, (key, opts) => t(`newsPage:${key}`, opts)),
            ),
          )
        }
      })
      .catch(() => {
        if (!cancelled) {
          setItems([])
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [i18n.language, t])

  return (
    <article className="card my-portfolio-card">
      <div className="my-portfolio-card-head">
        <h3>{t('portfolio:favoriteNewsTitle')}</h3>
        <Link to="/app/my-portfolio?section=news" className="my-portfolio-card-link">
          {t('portfolio:actions.viewAll')}
        </Link>
      </div>
      {loading ? (
        <p className="my-portfolio-insight-empty">{t('common:loading')}</p>
      ) : items.length === 0 ? (
        <p className="my-portfolio-insight-empty">{t('portfolio:favoriteNewsEmpty')}</p>
      ) : (
        <ul className="my-portfolio-insight-list my-portfolio-favorite-news-list">
          {items.map((item) => (
            <li key={item.id}>
              <FavoriteNewsThumb imageUrl={item.imageUrl} title={item.title} />
              <div>
                <strong>{item.title}</strong>
                <small>{stripHtml(item.summary)}</small>
              </div>
            </li>
          ))}
        </ul>
      )}
    </article>
  )
}

function FavoriteNewsThumb({ imageUrl, title }: { imageUrl?: string | null; title: string }) {
  const [broken, setBroken] = useState(false)
  const showImage = Boolean(imageUrl?.trim()) && !broken

  if (!showImage) {
    return (
      <span className="my-portfolio-favorite-news-fallback" aria-hidden>
        📰
      </span>
    )
  }

  return (
    <img
      src={imageUrl ?? ''}
      alt=""
      className="my-portfolio-favorite-news-thumb"
      loading="lazy"
      decoding="async"
      onError={() => setBroken(true)}
    />
  )
}
