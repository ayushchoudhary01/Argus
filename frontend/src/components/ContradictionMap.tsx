import { memo, useEffect } from 'react'
import ReactFlow, {
  type Node,
  type Edge,
  type NodeProps,
  Background,
  BackgroundVariant,
  Handle,
  Position,
  useNodesState,
} from 'reactflow'
import 'reactflow/dist/style.css'
import { Activity, ShieldAlert, Swords } from 'lucide-react'
import type { KeyConflict } from '../types'

interface Props {
  conflicts: KeyConflict[]
  selectedAgent: string | null
}

type AgentNodeData = {
  label: string
  mandate: string
  accent: string
  icon: typeof Activity
  dimmed: boolean
  focused: boolean
}

/**
 * Default React Flow nodes render as a bare labelled box with visible connection
 * handles, which reads as a diagram stub rather than a graph. This gives each agent
 * a card carrying its identity colour, its icon and its mandate, and hides the
 * handles so only the conflict edges are visible.
 */
const AgentNode = memo(({ data }: NodeProps<AgentNodeData>) => {
  const Icon = data.icon
  return (
    <div
      className="flex w-[186px] flex-col gap-1 rounded-xl border-2 px-3.5 py-2.5 transition-all duration-200"
      style={{
        borderColor: data.accent,
        background: `linear-gradient(160deg, ${data.accent}22, #18181b 70%)`,
        opacity: data.dimmed ? 0.3 : 1,
        boxShadow: data.focused ? `0 0 0 3px ${data.accent}44` : 'none',
      }}
    >
      <Handle type="target" position={Position.Top} style={{ opacity: 0 }} />
      <Handle type="source" position={Position.Bottom} style={{ opacity: 0 }} />
      <Handle type="target" position={Position.Left} id="l" style={{ opacity: 0 }} />
      <Handle type="source" position={Position.Right} id="r" style={{ opacity: 0 }} />

      <div className="flex items-center gap-2">
        <Icon size={15} strokeWidth={2.4} style={{ color: data.accent }} />
        <span className="text-[13px] font-semibold text-zinc-100">{data.label}</span>
      </div>
      <span className="text-[10px] leading-snug text-zinc-500">{data.mandate}</span>
    </div>
  )
})

const nodeTypes = { agent: AgentNode }

const AGENTS = [
  {
    id: 'structural',
    label: 'Structural',
    mandate: 'Which regime does this confirm?',
    accent: '#3987e5',
    icon: Activity,
    // Equilateral layout: apex top-centre, base below. Reads as a relationship
    // triangle rather than the lopsided L the previous coordinates produced.
    position: { x: 190, y: 0 },
  },
  {
    id: 'risk',
    label: 'Risk',
    mandate: 'What is consensus not pricing?',
    accent: '#d95926',
    icon: ShieldAlert,
    position: { x: 390, y: 230 },
  },
  {
    id: 'contrarian',
    label: 'Contrarian',
    mandate: 'What if both of them are wrong?',
    accent: '#199e70',
    icon: Swords,
    position: { x: -10, y: 230 },
  },
]

function buildNodes(selectedAgent: string | null): Node<AgentNodeData>[] {
  return AGENTS.map((a) => ({
    id: a.id,
    type: 'agent',
    position: a.position,
    data: {
      label: a.label,
      mandate: a.mandate,
      accent: a.accent,
      icon: a.icon,
      dimmed: !!selectedAgent && selectedAgent !== a.id,
      focused: selectedAgent === a.id,
    },
  }))
}

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
      label = `NLI tension ${(conflict.probability * 100).toFixed(0)}%`
    } else {
      tension = false
      source = conflict.agents[0]
      target = conflict.agents[1]
      // Naming the contested variable says far more than "Directional Conflict" did.
      label = `opposed on ${conflict.target_variable}`
    }

    // Focusing an agent recedes everything it is not party to.
    const involved = !selectedAgent || source === selectedAgent || target === selectedAgent
    const stroke = tension ? '#ec835a' : '#d03b3b'

    return {
      id: `edge-${i}`,
      source,
      target,
      type: 'straight',
      label,
      labelStyle: { fontSize: 10, fontWeight: 600, fill: '#e4e4e7' },
      labelBgStyle: { fill: '#09090b', fillOpacity: 0.95, stroke, strokeWidth: 1 },
      labelBgPadding: [7, 4] as [number, number],
      labelBgBorderRadius: 6,
      style: {
        stroke,
        strokeWidth: tension ? 2 : 2.5,
        strokeDasharray: tension ? '6 4' : '0',
        opacity: involved ? 1 : 0.1,
        transition: 'opacity 200ms',
      },
      animated: !tension && involved,
    }
  })
}

export default function ContradictionMap({ conflicts, selectedAgent }: Props) {
  const [nodes, setNodes, onNodesChange] = useNodesState(buildNodes(null))

  // React Flow records each node's measured width/height on the node object itself and
  // keeps the node `visibility: hidden` until it has them. Rebuilding the node array
  // from scratch each render discarded those measurements, so nodes never became
  // visible and edges — which need measured endpoints — never drew at all. Mutate only
  // `data` instead, leaving everything React Flow owns intact.
  useEffect(() => {
    setNodes((nds) =>
      nds.map((n) => ({
        ...n,
        data: {
          ...n.data,
          dimmed: !!selectedAgent && selectedAgent !== n.id,
          focused: selectedAgent === n.id,
        },
      }))
    )
  }, [selectedAgent, setNodes])

  // Edges stay derived: useEdgesState would freeze them at their initial value and the
  // focus dimming would never re-render.
  const edges = buildEdges(conflicts, selectedAgent)

  if (conflicts.length === 0) {
    return (
      <div className="rounded-xl border border-zinc-800 bg-zinc-900/80 p-8 text-center">
        <p className="text-sm text-zinc-300">No conflicts detected between agents.</p>
        <p className="mt-1 text-xs text-zinc-600">
          Three mandates built to disagree found nothing to disagree about.
        </p>
      </div>
    )
  }

  const directional = conflicts.filter((c) => !('label' in c)).length
  const tensions = conflicts.length - directional

  return (
    <div className="overflow-hidden rounded-xl border border-zinc-800 bg-zinc-900/80">
      <div
        className="relative"
        style={{
          height: 380,
          background:
            'radial-gradient(60% 70% at 50% 40%, rgba(57,135,229,0.06), transparent 70%)',
        }}
      >
        <ReactFlow
          nodes={nodes}
          edges={edges}
          nodeTypes={nodeTypes}
          onNodesChange={onNodesChange}
          fitView
          fitViewOptions={{ padding: 0.22 }}
          proOptions={{ hideAttribution: true }}
          nodesConnectable={false}
          edgesFocusable={false}
        >
          <Background variant={BackgroundVariant.Dots} gap={22} size={1} color="#27272a" />
        </ReactFlow>
      </div>

      <div className="flex flex-wrap items-center gap-x-5 gap-y-2 border-t border-zinc-800 px-4 py-2.5">
        <span className="inline-flex items-center gap-2 text-[11px] text-zinc-400">
          <span className="h-[2px] w-6 rounded" style={{ background: '#d03b3b' }} />
          {directional} directional {directional === 1 ? 'conflict' : 'conflicts'}
          <span className="text-zinc-600">· causal-chain comparison, deterministic</span>
        </span>
        <span className="inline-flex items-center gap-2 text-[11px] text-zinc-400">
          <span
            className="h-[2px] w-6 rounded"
            style={{ background: 'repeating-linear-gradient(90deg,#ec835a 0 6px,transparent 6px 10px)' }}
          />
          {tensions} flagged {tensions === 1 ? 'tension' : 'tensions'}
          <span className="text-zinc-600">· DeBERTa NLI classifier</span>
        </span>
      </div>
    </div>
  )
}
