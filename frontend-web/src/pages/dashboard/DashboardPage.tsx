import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'

export function DashboardPage() {
  useDocumentTitle('Ana Sayfa | Finans Platformu')

  return (
    <section>
      <h2>Ana Sayfa</h2>
      <p>Genel özet bileşenleri burada yer alacak.</p>
    </section>
  )
}

