import type { DivergenceType } from '../types'

const config: Record<DivergenceType, { label: string; className: string }> = {
  GENUINE_DIVERGENCE: {
    label: 'Genuine Divergence',
    className: 'bg-red-500/10 text-red-400 border-red-500/30',
  },
  CONSENSUS_HIGH_CONVICTION: {
    label: 'Consensus — High Conviction',
    className: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
  },
  EMPHASIS_DISPUTE: {
    label: 'Emphasis Dispute',
    className: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
  },
  INSUFFICIENT_SIGNAL: {
    label: 'Insufficient Signal',
    className: 'bg-zinc-500/10 text-zinc-400 border-zinc-500/30',
  },
}

interface Props {
  type: DivergenceType
}

export default function DivergenceBadge({ type }: Props) {
  const { label, className } = config[type]

  return (
    <span className={`inline-flex items-center px-4 py-1.5 rounded-full border text-sm font-semibold tracking-wide uppercase ${className}`}>
      {label}
    </span>
  )
}