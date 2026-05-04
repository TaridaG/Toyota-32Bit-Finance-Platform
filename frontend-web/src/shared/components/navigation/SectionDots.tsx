import { useEffect, useState } from 'react'

type SectionDotsItem = {
  id: string
  label: string
}

type SectionDotsProps = {
  sections: SectionDotsItem[]
  ariaLabel: string
}

export function SectionDots({ sections, ariaLabel }: SectionDotsProps) {
  const [activeSection, setActiveSection] = useState(sections[0]?.id ?? '')

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio)

        if (visible[0]?.target?.id) {
          setActiveSection(visible[0].target.id)
        }
      },
      {
        threshold: [0.35, 0.55, 0.75],
        rootMargin: '-20% 0px -20% 0px',
      },
    )

    sections.forEach((section) => {
      const node = document.getElementById(section.id)
      if (node) observer.observe(node)
    })

    return () => observer.disconnect()
  }, [sections])

  return (
    <nav className="landing-section-dots" aria-label={ariaLabel}>
      {sections.map((section) => {
        const isActive = section.id === activeSection
        return (
          <button
            key={section.id}
            type="button"
            className={`landing-section-dot${isActive ? ' landing-section-dot-active' : ''}`}
            aria-label={section.label}
            aria-current={isActive ? 'true' : undefined}
            onClick={() => {
              const target = document.getElementById(section.id)
              target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
            }}
          />
        )
      })}
    </nav>
  )
}
