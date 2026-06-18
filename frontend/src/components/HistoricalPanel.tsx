import type { HistoricalAnalogue, HistoricalCoverage } from '../types'

interface Props {
    analogues: HistoricalAnalogue[]
    coverage: HistoricalCoverage
}

function returnColor(value: number | null): string {
    if (value === null) return 'text-zinc-500'
    return value >= 0 ? 'text-emerald-400' : 'text-red-400'
}

function formatReturn(value: number | null): string {
    if (value === null) return '—'
    return `${value >= 0 ? '+' : ''}${value.toFixed(1)}%`
}

export default function HistoricalPanel({ analogues, coverage }: Props) {
    if (coverage === 'INSUFFICIENT') {
        return (
            <div className="rounded-xl border border-amber-500/30 bg-amber-500/5 p-5">
                <p className="text-sm font-semibold text-amber-400 mb-1">Low Historical Confidence</p>
                <p className="text-sm text-zinc-400">
                    No strong historical analogues found above the similarity threshold. Treat this analysis with extra caution.
                </p>
            </div>
        )
    }

    return (
        <div className="flex flex-col gap-4">
            {analogues.map((analogue) => (
                <div key={analogue.event_id} className="rounded-xl border border-zinc-700/50 bg-zinc-900 p-6">
                    <div className="flex items-start justify-between gap-4 mb-4">
                        <div>
                            <h4 className="text-sm font-semibold text-zinc-100">{analogue.title}</h4>
                            <p className="text-xs text-zinc-500 mt-0.5">
                                {new Date(analogue.date).toLocaleDateString('en-GB')}
                            </p>
                        </div>
                        <div className="shrink-0 text-right">
                            <p className="text-xs text-zinc-500 uppercase tracking-widest mb-0.5">Similarity</p>
                            <p className="text-lg font-mono font-semibold text-zinc-100">
                                {(analogue.similarity_score * 100).toFixed(0)}
                                <span className="text-xs text-zinc-500 font-normal">%</span>
                            </p>
                        </div>
                    </div>

                    <p className="text-sm text-zinc-400 leading-relaxed mb-5">{analogue.what_happened}</p>

                    {analogue.outcomes.length > 0 && (
                        <div>
                            <p className="text-xs text-zinc-500 uppercase tracking-widest mb-3">Outcomes</p>
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm">
                                    <thead>
                                        <tr className="border-b border-zinc-800">
                                            <th className="text-left text-xs text-zinc-500 font-medium pb-2 pr-4">Ticker</th>
                                            <th className="text-right text-xs text-zinc-500 font-medium pb-2 pr-4">Window</th>
                                            <th className="text-right text-xs text-zinc-500 font-medium pb-2 pr-4">Return</th>
                                            <th className="text-right text-xs text-zinc-500 font-medium pb-2">Quality</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {analogue.outcomes.map((outcome, i) => (
                                            <tr key={i} className="border-b border-zinc-800/50">
                                                <td className="py-2 pr-4 font-mono text-zinc-200">{outcome.ticker}</td>
                                                <td className="py-2 pr-4 text-right text-zinc-400">{outcome.window_days}d</td>
                                                <td className={`py-2 pr-4 text-right font-mono font-semibold ${returnColor(outcome.return_pct)}`}>
                                                    {formatReturn(outcome.return_pct)}
                                                </td>
                                                <td className="py-2 text-right text-xs text-zinc-500 uppercase tracking-wide">
                                                    {outcome.quality}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    )}
                </div>
            ))}
        </div>
    )
}