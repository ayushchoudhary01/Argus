import { TriangleAlert, ArrowUp, ArrowDown, Minus } from 'lucide-react'
import type { HistoricalAnalogue, HistoricalCoverage } from '../types'

interface Props {
  analogues: HistoricalAnalogue[]
  coverage: HistoricalCoverage
}

/**
 * Diverging bar, zero-anchored at the centre line.
 *
 * The gain/loss hues sit in the 6-8 CVD separation band, which is only
 * permissible with secondary encoding — so the signed value and a direction
 * arrow are always rendered beside the bar. The bar is never the only cue.
 *
 * `max` is shared across every outcome on the panel so bar lengths are
 * comparable between analogues, not just within one.
 */
function ReturnBar({ value, max }: { value: number; max: number }) {
  const magnitude = max > 0 ? Math.min(Math.abs(value) / max, 1) * 50 : 0
  const positive = value >= 0

  return (
    <div className="relative h-2.5 w-full min-w-[72px]" title={`${value.toFixed(4)}%`}>
      <span className="absolute top-0 left-1/2 h-full w-px -translate-x-1/2 bg-midpoint" />
      <span
        className={`grow-x absolute top-0 h-full ${
          positive ? 'left-1/2 ml-px rounded-r-[4px] bg-gain' : 'right-1/2 mr-px rounded-l-[4px] bg-loss'
        }`}
        style={{ width: `${magnitude}%` }}
      />
    </div>
  )
}

function ReturnValue({ value }: { value: number | null }) {
  if (value === null) {
    return (
      <span className="inline-flex items-center gap-1 font-mono text-xs text-zinc-600">
        <Minus size={11} /> n/a
      </span>
    )
  }
  const positive = value >= 0
  return (
    <span className={`inline-flex items-center gap-0.5 font-mono text-xs font-semibold ${positive ? 'text-gain' : 'text-loss'}`}>
      {positive ? <ArrowUp size={11} strokeWidth={2.6} /> : <ArrowDown size={11} strokeWidth={2.6} />}
      {positive ? '+' : '−'}
      {Math.abs(value).toFixed(1)}%
    </span>
  )
}

export default function HistoricalPanel({ analogues, coverage }: Props) {
  if (coverage === 'INSUFFICIENT') {
    return (
      <div className="rounded-xl border border-warning/30 bg-warning/5 p-5">
        <p className="mb-1 inline-flex items-center gap-2 text-sm font-semibold text-warning">
          <TriangleAlert size={14} /> Low Historical Confidence
        </p>
        <p className="text-sm text-zinc-400">
          No analogue cleared the similarity threshold. Treat this analysis with extra caution — the reasoning
          is ungrounded in precedent.
        </p>
      </div>
    )
  }

  const max = Math.max(
    ...analogues.flatMap((a) => a.outcomes.map((o) => Math.abs(o.return_pct ?? 0))),
    1
  )

  return (
    <div className="flex flex-col gap-4">
      {analogues.map((analogue, idx) => (
        <div
          key={analogue.event_id}
          className="rise overflow-hidden rounded-xl border border-zinc-800 bg-zinc-900/80 transition-colors hover:border-zinc-700"
          style={{ animationDelay: `${idx * 60}ms` }}
        >
          <div className="flex flex-col gap-4 p-5">
            <div className="flex items-start justify-between gap-4">
              <div className="flex flex-col gap-0.5">
                <h4 className="text-sm font-semibold text-zinc-100">{analogue.title}</h4>
                <p className="font-mono text-xs text-zinc-500">
                  {new Date(analogue.date).toLocaleDateString('en-GB', {
                    day: '2-digit',
                    month: 'short',
                    year: 'numeric',
                  })}
                </p>
              </div>
              <div className="flex shrink-0 flex-col items-end gap-1">
                <p className="text-[10px] font-semibold tracking-widest text-zinc-600 uppercase">Similarity</p>
                <p className="font-mono text-lg leading-none font-semibold text-zinc-100">
                  {(analogue.similarity_score * 100).toFixed(0)}
                  <span className="text-xs font-normal text-zinc-500">%</span>
                </p>
                <span className="h-1 w-20 overflow-hidden rounded-full bg-zinc-800">
                  <span
                    className="grow-x block h-full rounded-full bg-structural"
                    style={{ width: `${Math.min(analogue.similarity_score * 100, 100)}%` }}
                  />
                </span>
              </div>
            </div>

            <p className="text-sm leading-relaxed text-zinc-400">{analogue.what_happened}</p>

            {analogue.outcomes.length > 0 && (
              <div>
                <p className="mb-2 text-[10px] font-semibold tracking-widest text-zinc-600 uppercase">
                  Realised Outcomes
                </p>
                <div className="overflow-x-auto">
                  <table className="w-full border-separate border-spacing-0 text-sm">
                    <thead>
                      <tr>
                        <th className="pb-2 pr-4 text-left text-[10px] font-medium tracking-widest text-zinc-600 uppercase">
                          Ticker
                        </th>
                        <th className="pb-2 pr-4 text-right text-[10px] font-medium tracking-widest text-zinc-600 uppercase">
                          Window
                        </th>
                        <th className="w-[38%] pb-2 pr-3 text-left text-[10px] font-medium tracking-widest text-zinc-600 uppercase">
                          Return
                        </th>
                        <th className="pb-2 pr-4 text-right text-[10px] font-medium tracking-widest text-zinc-600 uppercase">
                          Value
                        </th>
                        <th className="pb-2 text-right text-[10px] font-medium tracking-widest text-zinc-600 uppercase">
                          Source
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {analogue.outcomes.map((o, i) => (
                        <tr key={i} className="group">
                          <td className="border-t border-zinc-800/70 py-2 pr-4 font-mono text-zinc-200">{o.ticker}</td>
                          <td className="border-t border-zinc-800/70 py-2 pr-4 text-right font-mono text-xs text-zinc-500">
                            {o.window_days}d
                          </td>
                          <td className="border-t border-zinc-800/70 py-2 pr-3">
                            {o.return_pct !== null && <ReturnBar value={o.return_pct} max={max} />}
                          </td>
                          <td className="border-t border-zinc-800/70 py-2 pr-4 text-right">
                            <ReturnValue value={o.return_pct} />
                          </td>
                          <td className="border-t border-zinc-800/70 py-2 text-right">
                            <span
                              className={`text-[10px] tracking-wide uppercase ${
                                o.quality === 'VERIFIED' ? 'text-zinc-500' : 'text-warning/80'
                              }`}
                            >
                              {o.quality}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  )
}
