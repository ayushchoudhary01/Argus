import ReactFlow, {
  type Node,
  type Edge,
  Background,
  BackgroundVariant,
  useNodesState,
} from 'reactflow'
import 'reactflow/dist/style.css'
import type { KeyConflict } from '../types'

interface Props {
  conflicts: KeyConflict[]
  selectedAgent: string | null
}

const nodeStyle = (accent: string, fill: string) => ({
  background: fill,
  border: `1.5px solid ${accent}`,
  borderRadius: '10px',
  color: '#e4e4e7',
  fontWeight: 600,
  fontSize: '12px',
  padding: '10px 18px',
})

const AGENT_NODES: Node[] = [
  {
    id: 'structural',
    position: { x: 0, y: 150 },
    data: { label: 'Structural' },
    style: nodeStyle('#3987e5', 'rgba(57,135,229,0.12)'),
  },
  {
    id: 'risk',
    position: { x: 300, y: 0 },
    data: { label: 'Risk' },
    style: nodeStyle('#d95926', 'rgba(217,89,38,0.12)'),
  },
  {
    id: 'contrarian',
    position: { x: 300, y: 300 },
    data: { label: 'Contrarian' },
    style: nodeStyle('#199e70', 'rgba(25,158,112,0.12)'),
  },
]

function buildEdges(conflicts: KeyConflict[], selectedAgent: string | null): Edge[] {
  return conflicts.map((conflict, i) => {
    // `label` exists only on FlaggedTension; the wire format gives DirectionalConflict
    // a `type` instead. Narrowing with `in` is what lets TS discriminate the union.
    let source: string
    let target: string
    let label: string
    let tension: boolean

    if ('label' in conflict) {
      tension = true
      source = conflict.premise.agent
      target = conflict.hypothesis.agent
      label = `Tension ${(conflict.probability * 100).toFixed(0)}%`
    } else {
      tension = false
      source = conflict.agents[0]
      target = conflict.agents[1]
      label = 'Directional Conflict'
    }

    // Focusing an agent recedes everything it is not party to.
    const involved = !selectedAgent || source === selectedAgent || target === selectedAgent
    const stroke = tension ? '#ec835a' : '#d03b3b'

    return {
      id: `edge-${i}`,
      source,
      target,
      label,
      labelStyle: { fontSize: 11, fontWeight: 600, fill: '#e4e4e7' },
      labelBgStyle: { fill: '#18181b', fillOpacity: 0.92 },
      labelBgPadding: [6, 4] as [number, number],
      labelBgBorderRadius: 4,
      style: {
        stroke,
        strokeWidth: 2,
        strokeDasharray: tension ? '5 3' : '0',
        opacity: involved ? 1 : 0.15,
        transition: 'opacity 200ms',
      },
      animated: !tension && involved,
    }
  })
}

export default function ContradictionMap({ conflicts, selectedAgent }: Props) {
  const [nodes, , onNodesChange] = useNodesState(AGENT_NODES)
  // Edges are derived, not stateful: useEdgesState would freeze them at their
  // initial value and the focus dimming would never re-render.
  const edges = buildEdges(conflicts, selectedAgent)

  if (conflicts.length === 0) {
    return (
      <div className="rounded-xl border border-zinc-800 bg-zinc-900/80 p-6 text-center">
        <p className="text-sm text-zinc-400">No conflicts detected between agents.</p>
        <p className="mt-1 text-xs text-zinc-600">
          Three mandates built to disagree found nothing to disagree about.
        </p>
      </div>
    )
  }

  return (
    <div className="overflow-hidden rounded-xl border border-zinc-800 bg-zinc-900/80" style={{ height: 420 }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        fitView
        attributionPosition="bottom-right"
      >
        <Background variant={BackgroundVariant.Dots} gap={20} size={1} color="#27272a" />
      </ReactFlow>
      <div className="flex flex-wrap items-center gap-4 border-t border-zinc-800 px-4 py-2.5">
        <span className="inline-flex items-center gap-2 text-[11px] text-zinc-500">
          <span className="h-0.5 w-5 rounded" style={{ background: '#d03b3b' }} />
          Directional conflict — deterministic
        </span>
        <span className="inline-flex items-center gap-2 text-[11px] text-zinc-500">
          <span
            className="h-0.5 w-5 rounded"
            style={{ background: 'repeating-linear-gradient(90deg,#ec835a 0 5px,transparent 5px 8px)' }}
          />
          Flagged tension — NLI classifier
        </span>
      </div>
    </div>
  )
}
