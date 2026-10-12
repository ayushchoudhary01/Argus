import { Swords, Scale, ScanSearch, CircleAlert } from 'lucide-react'
import type { DivergenceType } from '../types'

const config: Record<
  DivergenceType,
  { label: string; blurb: string; icon: typeof Swords; cls: string; dot: string }
> = {
  GENUINE_DIVERGENCE: {
    label: 'Genuine Divergence',
    blurb: 'Agents conflict, and the conflict is structural',
    icon: Swords,
    cls: 'border-critical/40 bg-critical/10 text-critical',
    dot: 'bg-critical',
  },
  CONSENSUS_HIGH_CONVICTION: {
    label: 'Consensus — High Conviction',
    blurb: 'Three mandates tried to disagree and could not',
    icon: Scale,
    cls: 'border-good/40 bg-good/10 text-good',
    dot: 'bg-good',
  },
  EMPHASIS_DISPUTE: {
    label: 'Emphasis Dispute',
    blurb: 'Different weighting, no detected contradiction',
    icon: ScanSearch,
    cls: 'border-warning/40 bg-warning/10 text-warning',
    dot: 'bg-warning',
  },
  INSUFFICIENT_SIGNAL: {
    label: 'Insufficient Signal',
    blurb: 'Evidence conflicts or is missing — no claim made',
    icon: CircleAlert,
    cls: 'border-zinc-600/50 bg-zinc-700/15 text-zinc-400',
    dot: 'bg-zinc-500',
  },
}

export default function DivergenceBadge({ type }: { type: DivergenceType }) {
  const c = config[type]
  const Icon = c.icon

  return (
    <div className="flex flex-wrap items-center gap-3">
      <span
        className={`inline-flex items-center gap-2 rounded-full border px-4 py-1.5 text-sm font-semibold tracking-wide uppercase ${c.cls}`}
      >
        <span className={`h-1.5 w-1.5 rounded-full ${c.dot} pulse-dot`} />
        <Icon size={14} strokeWidth={2.4} />
        {c.label}
      </span>
      <span className="text-xs text-zinc-500">{c.blurb}</span>
    </div>
  )
}
