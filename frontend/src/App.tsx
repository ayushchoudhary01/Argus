import { useEffect, useState } from 'react'
import { QueryClient, QueryClientProvider, useMutation } from '@tanstack/react-query'
import { Sparkles, Loader, TriangleAlert } from 'lucide-react'
import { analyseEvent } from './api/client'
import type { AnalysisRequest, DivergenceReport } from './types'
import DivergenceBadge from './components/DivergenceBadge'
import AgentCard from './components/AgentCard'
import ContradictionMap from './components/ContradictionMap'
import HistoricalPanel from './components/HistoricalPanel'
import AssumptionMatrix from './components/AssumptionMatrix'

const queryClient = new QueryClient()

/** The backend returns {error, message, correlation_id} on failure — surface the id so a
 *  failed run can be traced to its logs and its persisted report. */
function errorBody(err: unknown): { message?: string; correlation_id?: string } | undefined {
  const data = (err as { response?: { data?: unknown } })?.response?.data
  return data && typeof data === 'object' ? (data as { message?: string; correlation_id?: string }) : undefined
}

// Each sample is chosen to push the pipeline down a different path. The agents are
// non-deterministic, so these bias toward an outcome rather than guarantee one — the
// label names the scenario, not a promised verdict.
type Example = { label: string; probes: string; event: string }

const EXAMPLES: Example[] = [
  {
    label: 'Opposing rate paths',
    probes: 'Two plausible opposite readings of the same statement. When the agents happen to name the same target variable, the deterministic causal-chain check fires before NLI runs.',
    event: 'Fed holds rates but removes further tightening language from the statement.',
  },
  {
    label: 'Stagflationary print',
    probes: 'Growth and inflation pulling opposite ways, so the agents tend to emphasise different risks.',
    event: 'US CPI prints 3.8% vs 3.4% forecast. ISM Manufacturing drops to 46.2.',
  },
  {
    label: 'Systemic shock',
    probes: 'One unambiguous reading. The Contrarian still reports substantive disagreement rather than taking its LOW_VARIANCE escape hatch - see Honest Limitations in the README.',
    event: 'A top-five US bank fails overnight. Interbank lending freezes and the Fed opens an emergency liquidity facility.',
  },
  {
    label: 'Thin print',
    probes: 'Almost no information to reason from. The agents still construct theses rather than reporting DATA_GAP.',
    event: 'Durable goods orders revised down 0.1% from the prior month initial estimate.',
  },
  {
    label: 'Outside the corpus',
    probes: 'Nothing in the 1977-2023 macro seed set is genuinely close, yet retrieval still reports SUFFICIENT at 0.61-0.62 similarity - the 0.60 threshold is too permissive.',
    event: 'A major stablecoin depegs and the largest DeFi lending protocols halt withdrawals.',
  },
]

// The pipeline runs three sequential LLM calls on CPU, so a bare spinner leaves
// two to three minutes of unexplained silence. Naming the stage and showing
// elapsed time makes a slow run legible instead of looking hung.
const STAGES = [
  { at: 0, label: 'Fetching market context', detail: 'Volatility, macro, anomalies, historical analogues' },
  { at: 12, label: 'Structural and Risk agents reasoning', detail: 'Running in parallel on virtual threads' },
  { at: 75, label: 'Contrarian agent cross-challenging', detail: 'Reading both upstream theses' },
  { at: 130, label: 'Detecting contradictions', detail: 'Causal-chain comparison, then DeBERTa NLI' },
  { at: 155, label: 'Classifying divergence', detail: 'Rules engine over the conflict evidence' },
]

function Progress() {
  const [elapsed, setElapsed] = useState(0)

  useEffect(() => {
    const t = setInterval(() => setElapsed((s) => s + 1), 1000)
    return () => clearInterval(t)
  }, [])

  const stage = [...STAGES].reverse().find((s) => elapsed >= s.at) ?? STAGES[0]
  const mins = Math.floor(elapsed / 60)
  const secs = String(elapsed % 60).padStart(2, '0')

  return (
    <div className="flex flex-col items-center gap-4 rounded-xl border border-zinc-800 bg-zinc-900/80 p-10">
      <Loader size={22} className="animate-spin text-structural" />
      <div className="flex flex-col items-center gap-1">
        <p className="text-sm font-medium text-zinc-200">{stage.label}</p>
        <p className="text-xs text-zinc-500">{stage.detail}</p>
      </div>
      <p className="font-mono text-xs text-zinc-600">
        {mins}:{secs} elapsed &middot; agents reason independently, this takes 2&ndash;3 min on CPU
      </p>
      <div className="flex gap-1.5">
        {STAGES.map((s) => (
          <span
            key={s.at}
            className={`h-1 w-8 rounded-full transition-colors duration-500 ${
              elapsed >= s.at ? 'bg-structural' : 'bg-zinc-800'
            }`}
          />
        ))}
      </div>
    </div>
  )
}

function Section({
  title,
  hint,
  children,
  delay = 0,
}: {
  title: string
  hint?: string
  children: React.ReactNode
  delay?: number
}) {
  return (
    <section className="rise flex flex-col gap-4" style={{ animationDelay: `${delay}ms` }}>
      <div className="flex flex-wrap items-baseline gap-3">
        <h2 className="text-[11px] font-semibold tracking-widest text-zinc-500 uppercase">{title}</h2>
        {hint && <p className="text-xs text-zinc-600">{hint}</p>}
      </div>
      {children}
    </section>
  )
}

function Dashboard() {
  const [event, setEvent] = useState('')
  const [assetContext, setAssetContext] = useState('SPY, TLT, GLD')
  const [report, setReport] = useState<DivergenceReport | null>(null)
  const [selectedAgent, setSelectedAgent] = useState<string | null>(null)

  const { mutate, isPending, error } = useMutation({
    mutationFn: (req: AnalysisRequest) => analyseEvent(req),
    onSuccess: (data) => {
      setReport(data)
      setSelectedAgent(null)
    },
  })

  const handleSubmit = () => {
    if (!event.trim()) return
    const assets = assetContext
      .split(',')
      .map((a) => a.trim())
      .filter(Boolean)
    mutate({ event: event.trim(), asset_context: assets, timestamp: new Date().toISOString() })
  }

  const agentOf = (name: string) => report?.agent_outputs.find((a) => a.agent === name)

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      <div className="pointer-events-none fixed inset-x-0 top-0 h-64 bg-[radial-gradient(60%_100%_at_50%_0%,rgba(57,135,229,0.10),transparent)]" />

      <div className="relative mx-auto flex max-w-7xl flex-col gap-10 px-4 py-10 sm:px-6">
        <header className="flex flex-col gap-2">
          <h1 className="wordmark text-3xl font-bold tracking-tight">Argus</h1>
          <p className="text-sm text-zinc-500">
            Multi-Agent Reasoning System &mdash;{' '}
            <span className="text-zinc-400">the disagreement is the product</span>
          </p>
        </header>

        <div className="flex flex-col gap-4 rounded-xl border border-zinc-800 bg-zinc-900/80 p-6">
          <div className="flex flex-col gap-2">
            <label htmlFor="event" className="text-[11px] font-semibold tracking-widest text-zinc-500 uppercase">
              Market Event
            </label>
            <textarea
              id="event"
              className="w-full resize-none rounded-lg border border-zinc-800 bg-zinc-950/60 px-4 py-3 text-sm text-zinc-100 transition-colors placeholder-zinc-600 focus:border-structural/60 focus:outline-none"
              rows={3}
              placeholder={EXAMPLES[0].event}
              value={event}
              onChange={(e) => setEvent(e.target.value)}
            />
            <div className="flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center gap-1 text-[11px] text-zinc-600">
                <Sparkles size={11} /> Try
              </span>
              {EXAMPLES.map((ex) => (
                <button
                  key={ex.label}
                  type="button"
                  onClick={() => setEvent(ex.event)}
                  title={`${ex.event}

${ex.probes}`}
                  className={`rounded-full border px-2.5 py-1 text-[11px] transition-colors ${
                    event === ex.event
                      ? 'border-structural/60 bg-structural/10 text-zinc-200'
                      : 'border-zinc-800 text-zinc-400 hover:border-structural/50 hover:text-zinc-200'
                  }`}
                >
                  {ex.label}
                </button>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="assets" className="text-[11px] font-semibold tracking-widest text-zinc-500 uppercase">
              Asset Context
            </label>
            <input
              id="assets"
              className="w-full rounded-lg border border-zinc-800 bg-zinc-950/60 px-4 py-3 text-sm text-zinc-100 transition-colors placeholder-zinc-600 focus:border-structural/60 focus:outline-none"
              placeholder="SPY, TLT, GLD"
              value={assetContext}
              onChange={(e) => setAssetContext(e.target.value)}
            />
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-xs text-zinc-600">Three agents, then NLI contradiction detection. No temperature or seed is set, so the same event can classify differently between runs.</p>
            <button
              onClick={handleSubmit}
              disabled={isPending || !event.trim()}
              className="rounded-lg bg-zinc-100 px-6 py-2.5 text-sm font-semibold text-zinc-900 transition-all hover:bg-white disabled:cursor-not-allowed disabled:opacity-40"
            >
              {isPending ? 'Analysing…' : 'Run Analysis'}
            </button>
          </div>

          {error && (
            <div className="flex flex-col gap-1 rounded-lg border border-critical/30 bg-critical/5 p-3">
              <p className="inline-flex items-center gap-2 text-sm text-critical">
                <TriangleAlert size={14} />
                {errorBody(error)?.message ?? 'Analysis failed. Check the backend is running, then try again.'}
              </p>
              {errorBody(error)?.correlation_id && (
                <p className="font-mono text-[11px] text-zinc-500">
                  correlation_id: {errorBody(error)?.correlation_id}
                </p>
              )}
            </div>
          )}
        </div>

        {isPending && <Progress />}

        {report && !isPending && (
          <>
            <div className="rise">
              <DivergenceBadge type={report.divergence_type} />
            </div>

            <Section
              title="Agent Outputs"
              hint={selectedAgent ? 'Click again to clear focus' : 'Click an agent to focus its conflicts'}
              delay={60}
            >
              <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
                {report.agent_outputs.map((output) => (
                  <AgentCard
                    key={output.agent}
                    output={output}
                    selected={selectedAgent === output.agent}
                    onSelect={() => setSelectedAgent((cur) => (cur === output.agent ? null : output.agent))}
                  />
                ))}
              </div>
            </Section>

            <Section title="Contradiction Map" hint="Where the reasoning actually collides" delay={120}>
              <ContradictionMap conflicts={report.key_conflicts} selectedAgent={selectedAgent} />
            </Section>

            <Section title="Assumption Matrix" hint="What each agent took for granted" delay={180}>
              <AssumptionMatrix
                structural={agentOf('structural')?.explicit_assumptions ?? []}
                risk={agentOf('risk')?.explicit_assumptions ?? []}
                contrarian={agentOf('contrarian')?.explicit_assumptions ?? []}
                conflicts={report.key_conflicts}
                challenged={agentOf('contrarian')?.challenged_assumptions ?? []}
                validated={agentOf('contrarian')?.validated_assumptions ?? []}
              />
            </Section>

            <Section title="Historical Analogues" hint="Real events, real realised outcomes" delay={240}>
              <HistoricalPanel analogues={report.historical_analogues} coverage={report.historical_coverage} />
            </Section>
          </>
        )}
      </div>
    </div>
  )
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Dashboard />
    </QueryClientProvider>
  )
}
