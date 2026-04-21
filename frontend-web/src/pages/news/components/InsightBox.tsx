export function InsightBox({ text }: { text: string }) {
  return (
    <article className="card fi-panel-card fi-insight-box">
      <h3>AI Insight</h3>
      <p>{text}</p>
    </article>
  )
}
