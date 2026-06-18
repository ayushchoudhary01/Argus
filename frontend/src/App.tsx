import { useState } from 'react'
import { QueryClient, QueryClientProvider, useMutation } from '@tanstack/react-query'
import { analyseEvent } from './api/client'
import type { AnalysisRequest, DivergenceReport } from './types'
import DivergenceBadge from './components/DivergenceBadge'
import AgentCard from './components/AgentCard'
import ContradictionMap from './components/ContradictionMap'
import HistoricalPanel from './components/HistoricalPanel'
import AssumptionMatrix from './components/AssumptionMatrix'

const queryClient = new QueryClient()

function Dashboard() {
  const [event, setEvent] = useState('')
  const [assetContext, setAssetContext] = useState('SPY, TLT, GLD')
  const [report, setReport] = useState<DivergenceReport | null>(null)

  const { mutate, isPending, error } = useMutation({
    mutationFn: (req: AnalysisRequest) => analyseEvent(req),
    onSuccess: (data) => setReport(data),
  })

  const handleSubmit = () => {
    if (!event.trim()) return
    const assets = assetContext.split(',').map((a) => a.trim()).filter(Boolean)
    mutate({
      event: event.trim(),
      asset_context: assets,
      timestamp: new Date().toISOString(),
    })
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      <div className="max-w-7xl mx-auto px-6 py-10 flex flex-col gap-10">

        <div>
          <h1 className="text-2xl font-bold text-zinc-100 tracking-tight">Argus</h1>
          <p className="text-sm text-zinc-500 mt-1">Autonomous Multi-Agent Reasoning System</p>
        </div>

        <div className="rounded-xl border border-zinc-700/50 bg-zinc-900 p-6 flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <label className="text-xs text-zinc-500 uppercase tracking-widest">Market Event</label>
            <textarea
              className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-3 text-sm text-zinc-100 placeholder-zinc-600 resize-none focus:outline-none focus:border-zinc-500 transition-colors"
              rows={3}
              placeholder="US CPI prints 3.8% vs 3.4% forecast. ISM Manufacturing drops to 46.2."
              value={event}
              onChange={(e) => setEvent(e.target.value)}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-xs text-zinc-500 uppercase tracking-widest">Asset Context</label>
            <input
              className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-3 text-sm text-zinc-100 placeholder-zinc-600 focus:outline-none focus:border-zinc-500 transition-colors"
              placeholder="SPY, TLT, GLD"
              value={assetContext}
              onChange={(e) => setAssetContext(e.target.value)}
            />
          </div>

          <div className="flex items-center justify-between">
            <p className="text-xs text-zinc-600">Pipeline runs 3 agents + NLI contradiction detection. Allow 2–3 minutes.</p>
            <button
              onClick={handleSubmit}
              disabled={isPending || !event.trim()}
              className="px-6 py-2.5 bg-zinc-100 text-zinc-900 text-sm font-semibold rounded-lg disabled:opacity-40 disabled:cursor-not-allowed hover:bg-white transition-colors"
            >
              {isPending ? 'Analysing…' : 'Run Analysis'}
            </button>
          </div>

          {error && (
            <p className="text-sm text-red-400">Analysis failed. Check that the backend is running and try again.</p>
          )}
        </div>

        {isPending && (
          <div className="rounded-xl border border-zinc-700/50 bg-zinc-900 p-10 flex flex-col items-center gap-4">
            <div className="w-8 h-8 border-2 border-zinc-700 border-t-zinc-300 rounded-full animate-spin" />
            <p className="text-sm text-zinc-500">Agents reasoning independently — this takes 2–3 minutes on CPU…</p>
          </div>
        )}

        {report && !isPending && (
          <>
            <div className="flex items-center justify-between">
              <DivergenceBadge type={report.divergence_type} />
            </div>

            <div>
              <p className="text-xs text-zinc-500 uppercase tracking-widest mb-4">Agent Outputs</p>
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
                {report.agent_outputs.map((output) => (
                  <AgentCard key={output.agent} output={output} />
                ))}
              </div>
            </div>

            <div>
              <p className="text-xs text-zinc-500 uppercase tracking-widest mb-4">Contradiction Map</p>
              <ContradictionMap conflicts={report.key_conflicts} />
            </div>

            <AssumptionMatrix
              structural={report.agent_outputs.find(a => a.agent === 'structural')?.explicit_assumptions ?? []}
              risk={report.agent_outputs.find(a => a.agent === 'risk')?.explicit_assumptions ?? []}
              contrarian={report.agent_outputs.find(a => a.agent === 'contrarian')?.explicit_assumptions ?? []}
              conflicts={report.key_conflicts}
            />

            <div>
              <p className="text-xs text-zinc-500 uppercase tracking-widest mb-4">Historical Analogues</p>
              <HistoricalPanel
                analogues={report.historical_analogues}
                coverage={report.historical_coverage}
              />
            </div>
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