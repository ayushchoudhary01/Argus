import type { Assumption, CausalChain } from '../types'

interface AgentOutput {
  agent: string
  thesis: string
  reasoning_chain: string[]
  causal_chain: CausalChain
  explicit_assumptions: Assumption[]
  key_uncertainties: string[]
}

interface Props {
  output: AgentOutput
}

const agentMeta: Record<string, { label: string; description: string; color: string }> = {
  structural: {
    label: 'Structural Agent',
    description: 'What structural regime does this event confirm or disrupt?',
    color: 'border-blue-500/40',
  },
  risk: {
    label: 'Risk Agent',
    description: 'What could go wrong that consensus is not pricing?',
    color: 'border-amber-500/40',
  },
  contrarian: {
    label: 'Contrarian Agent',
    description: 'What is the strongest case that both agents are wrong?',
    color: 'border-violet-500/40',
  },
}

export default function AgentCard({ output }: Props) {
  const meta = agentMeta[output.agent] ?? {
    label: output.agent,
    description: '',
    color: 'border-zinc-500/40',
  }

  return (
    <div className={`rounded-xl border-2 ${meta.color} bg-zinc-900 p-6 flex flex-col gap-5`}>
      <div>
        <p className="text-xs text-zinc-500 uppercase tracking-widest mb-1">{meta.description}</p>
        <h3 className="text-base font-semibold text-zinc-100">{meta.label}</h3>
      </div>

      <div>
        <p className="text-xs text-zinc-500 uppercase tracking-widest mb-2">Thesis</p>
        <p className="text-sm text-zinc-200 leading-relaxed">{output.thesis}</p>
      </div>

      <div>
        <p className="text-xs text-zinc-500 uppercase tracking-widest mb-2">Reasoning Chain</p>
        <ol className="flex flex-col gap-2">
          {output.reasoning_chain.map((step, i) => (
            <li key={i} className="flex gap-3 text-sm text-zinc-300">
              <span className="text-zinc-600 font-mono shrink-0">{i + 1}.</span>
              <span>{step}</span>
            </li>
          ))}
        </ol>
      </div>

      <div>
        <p className="text-xs text-zinc-500 uppercase tracking-widest mb-2">Causal Chain</p>
        <div className="flex items-center gap-2 flex-wrap text-sm">
          <span className="bg-zinc-800 text-zinc-200 px-3 py-1 rounded-md">{output.causal_chain.driver}</span>
          <span className={`font-mono text-xs font-semibold ${output.causal_chain.direction === 'UPWARD_PRESSURE' ? 'text-emerald-400' : 'text-red-400'}`}>
            {output.causal_chain.direction === 'UPWARD_PRESSURE' ? '↑' : '↓'} {output.causal_chain.direction.replace('_', ' ')}
          </span>
          <span className="bg-zinc-800 text-zinc-200 px-3 py-1 rounded-md">{output.causal_chain.target_variable}</span>
        </div>
      </div>

      <div>
        <p className="text-xs text-zinc-500 uppercase tracking-widest mb-2">Assumptions</p>
        <ul className="flex flex-col gap-1.5">
          {output.explicit_assumptions.map((a) => (
            <li key={a.id} className="flex gap-2 text-sm text-zinc-400">
              <span className="text-zinc-600 font-mono shrink-0">{a.id}</span>
              <span>{a.text}</span>
            </li>
          ))}
        </ul>
      </div>

      {output.key_uncertainties?.length > 0 && (
        <div>
          <p className="text-xs text-zinc-500 uppercase tracking-widest mb-2">Key Uncertainties</p>
          <ul className="flex flex-col gap-1">
            {output.key_uncertainties.map((u, i) => (
              <li key={i} className="text-sm text-zinc-400">— {u}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}