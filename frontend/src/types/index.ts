export type CausalDirection = 'UPWARD_PRESSURE' | 'DOWNWARD_PRESSURE'

export type DivergenceType =
  | 'GENUINE_DIVERGENCE'
  | 'CONSENSUS_HIGH_CONVICTION'
  | 'EMPHASIS_DISPUTE'
  | 'INSUFFICIENT_SIGNAL'

export type HistoricalCoverage = 'SUFFICIENT' | 'INSUFFICIENT'

export type ConflictMethod = 'causal_chain_comparison' | 'deberta_nli'

export type ConflictLabel = 'DIRECTIONAL_CONFLICT' | 'FLAGGED_TENSION'

export interface Assumption {
  id: string
  text: string
  agent: string
}

export interface CausalChain {
  driver: string
  direction: CausalDirection
  target_variable: string
  agent: string
}

export interface AgentOutput {
  agent: string
  thesis: string
  reasoning_chain: string[]
  causal_chain: CausalChain
  explicit_assumptions: Assumption[]
  key_uncertainties: string[]
  status: string
}

export interface EventOutcome {
  ticker: string
  window_days: number
  return_pct: number | null
  quality: string
}

export interface HistoricalAnalogue {
  event_id: string
  title: string
  date: string
  similarity_score: number
  what_happened: string
  outcomes: EventOutcome[]
}

export interface KeyConflict {
  premise: Assumption
  hypothesis: Assumption
  label: ConflictLabel
  probability?: number
  method: ConflictMethod
  note?: string
}

export interface DivergenceReport {
  divergence_type: DivergenceType
  primary_thesis: string
  strongest_counter_thesis: string
  key_conflicts: KeyConflict[]
  agent_outputs: AgentOutput[]
  historical_analogues: HistoricalAnalogue[]
  historical_coverage: HistoricalCoverage
}

export interface AnalysisRequest {
  event: string
  asset_context: string[]
  timestamp: string
}