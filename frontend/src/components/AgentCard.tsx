import { useState } from 'react'
import { Activity, ShieldAlert, Swords, ChevronDown, TriangleAlert, ArrowUp, ArrowDown } from 'lucide-react'
import type { AgentOutput } from '../types'

interface Props {
  output: AgentOutput
  selected: boolean
  onSelect: () => void
}

const agentMeta: Record<
  string,
  { label: string; description: string; icon: typeof Activity; text: string; ring: string; bar: string; glow: string }
> = {
  structural: {
    label: 'Structural Agent',
    description: 'What regime does this confirm or disrupt?',
    icon: Activity,
    text: 'text-structural',
    ring: 'border-structural/45',
    bar: 'bg-structural',
    glow: 'shadow-[0_0_0_1px_var(--color-structural)]',
  },
  risk: {
    label: 'Risk Agent',
    description: 'What is consensus not pricing?',
    icon: ShieldAlert,
    text: 'text-risk',
    ring: 'border-risk/45',
    bar: 'bg-risk',
    glow: 'shadow-[0_0_0_1px_var(--color-risk)]',
  },
  contrarian: {
    label: 'Contrarian Agent',
    description: 'What if both of them are wrong?',
    icon: Swords,
    text: 'text-contrarian',
    ring: 'border-contrarian/45',
    bar: 'bg-contrarian',
    glow: 'shadow-[0_0_0_1px_var(--color-contrarian)]',
  },
}

const fallback = {
  label: 'Agent',
  description: '',
  icon: Activity,
  text: 'text-zinc-300',
  ring: 'border-zinc-700/50',
  bar: 'bg-zinc-600',
  glow: 'shadow-[0_0_0_1px_#52525b]',
}

export default function AgentCard({ output, selected, onSelect }: Props) {
  const [open, setOpen] = useState(false)
  const meta = agentMeta[output.agent] ?? { ...fallback, label: output.agent }
  const Icon = meta.icon
  const chain = output.reasoning_chain ?? []
  const assumptions = output.explicit_assumptions ?? []
  const visibleChain = open ? chain : chain.slice(0, 2)
  const degraded = output.status === 'DEGRADED'

  return (
    <div
      className={`group relative flex flex-col overflow-hidden rounded-xl border bg-zinc-900/80 transition-all duration-200 hover:-translate-y-0.5 ${
        selected ? `${meta.ring} ${meta.glow}` : 'border-zinc-800 hover:border-zinc-700'
      }`}
    >
      <div className={`h-0.5 w-full ${meta.bar} opacity-80`} />

      <button
        type="button"
        onClick={onSelect}
        aria-pressed={selected}
        className="flex items-start gap-3 px-5 pt-4 pb-3 text-left"
      >
        <span className={`mt-0.5 shrink-0 ${meta.text}`}>
          <Icon size={18} strokeWidth={2.2} />
        </span>
        <span className="flex flex-col gap-0.5">
          <span className="flex flex-wrap items-center gap-2">
            <span className="text-sm font-semibold text-zinc-100">{meta.label}</span>
            {degraded && (
              <span className="inline-flex items-center gap-1 rounded border border-warning/40 bg-warning/10 px-1.5 py-0.5 text-[10px] font-semibold tracking-widest text-warning uppercase">
                <TriangleAlert size={10} /> Degraded
              </span>
            )}
            {selected && (
              <span className="text-[10px] font-semibold tracking-widest text-zinc-500 uppercase">Focused</span>
            )}
          </span>
          <span className="text-xs leading-relaxed text-zinc-500">{meta.description}</span>
        </span>
      </button>

      <div className="flex flex-1 flex-col gap-5 px-5 pb-5">
        <p className="text-sm leading-relaxed text-zinc-200">{output.thesis ?? '—'}</p>

        {output.causal_chain && (
          <div className="flex flex-col gap-2">
            <p className="text-[10px] font-semibold tracking-widest text-zinc-600 uppercase">Causal Chain</p>
            <div className="flex flex-wrap items-center gap-1.5 text-xs">
              <span className="rounded-md bg-zinc-800 px-2 py-1 text-zinc-200">{output.causal_chain.driver}</span>
              <span
                className={`inline-flex items-center gap-0.5 font-mono font-semibold ${
                  output.causal_chain.direction === 'UPWARD_PRESSURE' ? 'text-gain' : 'text-loss'
                }`}
              >
                {output.causal_chain.direction === 'UPWARD_PRESSURE' ? <ArrowUp size={12} /> : <ArrowDown size={12} />}
              </span>
              <span className="rounded-md bg-zinc-800 px-2 py-1 text-zinc-200">
                {output.causal_chain.target_variable}
              </span>
            </div>
          </div>
        )}

        {chain.length > 0 && (
          <div className="flex flex-col gap-2">
            <p className="text-[10px] font-semibold tracking-widest text-zinc-600 uppercase">Reasoning</p>
            <ol className="flex flex-col gap-2">
              {visibleChain.map((step, i) => (
                <li key={i} className="flex gap-2.5 text-sm text-zinc-400">
                  <span className={`shrink-0 font-mono text-xs ${meta.text} opacity-70`}>{i + 1}</span>
                  <span className="leading-relaxed">{step}</span>
                </li>
              ))}
            </ol>
            {chain.length > 2 && (
              <button
                type="button"
                onClick={() => setOpen((v) => !v)}
                className="mt-0.5 inline-flex items-center gap-1 self-start text-[11px] font-semibold tracking-wide text-zinc-500 transition-colors hover:text-zinc-300"
              >
                <ChevronDown size={12} className={`transition-transform ${open ? 'rotate-180' : ''}`} />
                {open ? 'Show less' : `${chain.length - 2} more step${chain.length - 2 > 1 ? 's' : ''}`}
              </button>
            )}
          </div>
        )}

        {assumptions.length > 0 && (
          <div className="flex flex-col gap-2">
            <p className="text-[10px] font-semibold tracking-widest text-zinc-600 uppercase">Assumptions</p>
            <ul className="flex flex-col gap-1.5">
              {assumptions.map((a) => (
                <li key={a.id} className="flex gap-2 text-xs text-zinc-400">
                  <span className="shrink-0 font-mono text-zinc-600">{a.id}</span>
                  <span className="leading-relaxed">{a.text}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {(output.historical_refs?.length ?? 0) > 0 && (
          <div className="flex flex-col gap-2">
            <p className="text-[10px] font-semibold tracking-widest text-zinc-600 uppercase">Leaned On</p>
            <div className="flex flex-wrap gap-1.5">
              {output.historical_refs?.map((ref, i) => (
                <span
                  key={i}
                  className="rounded border border-zinc-800 bg-zinc-800/40 px-2 py-0.5 text-[11px] text-zinc-400"
                >
                  {ref}
                </span>
              ))}
            </div>
          </div>
        )}

        {(output.key_uncertainties?.length ?? 0) > 0 && (
          <div className="flex flex-col gap-1.5 border-t border-zinc-800 pt-4">
            <p className="text-[10px] font-semibold tracking-widest text-zinc-600 uppercase">Uncertainties</p>
            <ul className="flex flex-col gap-1">
              {output.key_uncertainties?.map((u, i) => (
                <li key={i} className="text-xs leading-relaxed text-zinc-500">
                  {u}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  )
}
