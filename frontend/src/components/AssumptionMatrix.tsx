import { Swords, Check, Minus } from 'lucide-react'
import type { Assumption, KeyConflict } from '../types'

interface Props {
  structural: Assumption[]
  risk: Assumption[]
  contrarian: Assumption[]
  conflicts: KeyConflict[]
  challenged: string[]
  validated: string[]
}

type Status = 'challenged' | 'validated' | 'neutral'

function getAssumptionStatus(
  id: string,
  agent: string,
  conflicts: KeyConflict[],
  challenged: string[],
  validated: string[]
): Status {
  // The Contrarian Agent's explicit verdict on an upstream assumption.
  if (challenged.includes(id)) return 'challenged'

  // NLI-detected linguistic tension involving this assumption.
  for (const conflict of conflicts) {
    if ('label' in conflict && conflict.label === 'FLAGGED_TENSION') {
      if (conflict.premise.id === id && conflict.premise.agent === agent) return 'challenged'
      if (conflict.hypothesis.id === id && conflict.hypothesis.agent === agent) return 'challenged'
    }
  }

  if (validated.includes(id)) return 'validated'
  return 'neutral'
}

// Status is never colour alone — each pairs an icon with a word.
const statusMeta: Record<Status, { cls: string; label: string; icon: typeof Check }> = {
  challenged: { cls: 'border-critical/35 bg-critical/5 text-critical', label: 'Challenged', icon: Swords },
  validated: { cls: 'border-good/35 bg-good/5 text-good', label: 'Validated', icon: Check },
  neutral: { cls: 'border-zinc-800 bg-zinc-900/60 text-zinc-400', label: 'Unchallenged', icon: Minus },
}

function AssumptionCell({
  assumption,
  conflicts,
  challenged,
  validated,
}: {
  assumption: Assumption
  conflicts: KeyConflict[]
  challenged: string[]
  validated: string[]
}) {
  const status = getAssumptionStatus(assumption.id, assumption.agent, conflicts, challenged, validated)
  const meta = statusMeta[status]
  const Icon = meta.icon

  return (
    <div className={`flex flex-col gap-1.5 rounded-lg border p-3 transition-colors ${meta.cls}`}>
      <div className="flex items-center justify-between gap-2">
        <span className="font-mono text-[11px] font-semibold opacity-70">{assumption.id}</span>
        <span className="inline-flex items-center gap-1 text-[10px] tracking-widest uppercase opacity-70">
          <Icon size={10} strokeWidth={2.6} />
          {meta.label}
        </span>
      </div>
      <p className="text-xs leading-relaxed">{assumption.text}</p>
    </div>
  )
}

export default function AssumptionMatrix({
  structural,
  risk,
  contrarian,
  conflicts,
  challenged,
  validated,
}: Props) {
  const columns = [
    { label: 'Structural', dot: 'bg-structural', text: 'text-structural', assumptions: structural },
    { label: 'Risk', dot: 'bg-risk', text: 'text-risk', assumptions: risk },
    { label: 'Contrarian', dot: 'bg-contrarian', text: 'text-contrarian', assumptions: contrarian },
  ]

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
      {columns.map(({ label, dot, text, assumptions }) => (
        <div key={label} className="flex flex-col gap-3">
          <p className={`inline-flex items-center gap-2 text-[11px] font-semibold tracking-widest uppercase ${text}`}>
            <span className={`h-1.5 w-1.5 rounded-full ${dot}`} />
            {label}
          </p>
          {assumptions.length === 0 ? (
            <p className="text-xs text-zinc-600">No assumptions</p>
          ) : (
            assumptions.map((a) => (
              <AssumptionCell
                key={a.id}
                assumption={a}
                conflicts={conflicts}
                challenged={challenged}
                validated={validated}
              />
            ))
          )}
        </div>
      ))}
    </div>
  )
}
