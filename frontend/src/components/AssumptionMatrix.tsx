import type { Assumption, KeyConflict } from '../types'

interface Props {
  structural: Assumption[]
  risk: Assumption[]
  contrarian: Assumption[]
  conflicts: KeyConflict[]
}

function getAssumptionStatus(
  id: string,
  agent: string,
  conflicts: KeyConflict[]
): 'challenged' | 'validated' | 'neutral' {
  for (const conflict of conflicts) {
    if (conflict.label === 'FLAGGED_TENSION') {
      if (conflict.premise.id === id && conflict.premise.agent === agent) return 'challenged'
      if (conflict.hypothesis.id === id && conflict.hypothesis.agent === agent) return 'challenged'
    }
  }
  return 'neutral'
}

const statusStyle: Record<string, string> = {
  challenged: 'border-red-500/40 bg-red-500/5 text-red-300',
  validated: 'border-emerald-500/40 bg-emerald-500/5 text-emerald-300',
  neutral: 'border-zinc-700/50 bg-zinc-800/50 text-zinc-300',
}

const statusLabel: Record<string, string> = {
  challenged: 'Challenged',
  validated: 'Validated',
  neutral: 'Unchallenged',
}

interface AssumptionCellProps {
  assumption: Assumption
  conflicts: KeyConflict[]
}

function AssumptionCell({ assumption, conflicts }: AssumptionCellProps) {
  const status = getAssumptionStatus(assumption.id, assumption.agent, conflicts)

  return (
    <div className={`rounded-lg border p-3 flex flex-col gap-1.5 ${statusStyle[status]}`}>
      <div className="flex items-center justify-between gap-2">
        <span className="font-mono text-xs font-semibold opacity-70">{assumption.id}</span>
        <span className="text-xs uppercase tracking-widest opacity-60">{statusLabel[status]}</span>
      </div>
      <p className="text-xs leading-relaxed">{assumption.text}</p>
    </div>
  )
}

export default function AssumptionMatrix({ structural, risk, contrarian, conflicts }: Props) {
  const columns = [
    { label: 'Structural', color: 'text-blue-400', assumptions: structural },
    { label: 'Risk', color: 'text-amber-400', assumptions: risk },
    { label: 'Contrarian', color: 'text-violet-400', assumptions: contrarian },
  ]

  return (
    <div className="grid grid-cols-3 gap-4">
      {columns.map(({ label, color, assumptions }) => (
        <div key={label} className="flex flex-col gap-3">
          <p className={`text-xs font-semibold uppercase tracking-widest ${color}`}>{label}</p>
          {assumptions.length === 0 ? (
            <p className="text-xs text-zinc-600">No assumptions</p>
          ) : (
            assumptions.map((a) => (
              <AssumptionCell key={a.id} assumption={a} conflicts={conflicts} />
            ))
          )}
        </div>
      ))}
    </div>
  )
}