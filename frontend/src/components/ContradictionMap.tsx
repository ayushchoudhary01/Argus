import { useCallback } from 'react'
import ReactFlow, {
  type Node,
  type Edge,
  Background,
  BackgroundVariant,
  useNodesState,
  useEdgesState,
} from 'reactflow'
import 'reactflow/dist/style.css'
import type { KeyConflict } from '../types'

interface Props {
  conflicts: KeyConflict[]
}

const AGENT_NODES: Node[] = [
  {
    id: 'structural',
    position: { x: 0, y: 150 },
    data: { label: 'Structural Agent' },
    style: {
      background: '#1e3a5f',
      border: '1.5px solid #3b82f6',
      borderRadius: '10px',
      color: '#93c5fd',
      fontWeight: 600,
      fontSize: '13px',
      padding: '10px 18px',
    },
  },
  {
    id: 'risk',
    position: { x: 300, y: 0 },
    data: { label: 'Risk Agent' },
    style: {
      background: '#3d2a00',
      border: '1.5px solid #f59e0b',
      borderRadius: '10px',
      color: '#fcd34d',
      fontWeight: 600,
      fontSize: '13px',
      padding: '10px 18px',
    },
  },
  {
    id: 'contrarian',
    position: { x: 300, y: 300 },
    data: { label: 'Contrarian Agent' },
    style: {
      background: '#2d1f4e',
      border: '1.5px solid #8b5cf6',
      borderRadius: '10px',
      color: '#c4b5fd',
      fontWeight: 600,
      fontSize: '13px',
      padding: '10px 18px',
    },
  },
]

function buildEdges(conflicts: KeyConflict[]): Edge[] {
  return conflicts.map((conflict, i) => {
    if (conflict.label === 'FLAGGED_TENSION') {
      return {
        id: `edge-${i}`,
        source: conflict.premise.agent,
        target: conflict.hypothesis.agent,
        label: `Tension (${(conflict.probability * 100).toFixed(0)}%)`,
        labelStyle: { fontSize: 11, fontWeight: 600 },
        labelBgStyle: { fill: '#431407', fillOpacity: 0.9 },
        labelBgPadding: [6, 4] as [number, number],
        labelBgBorderRadius: 4,
        style: { stroke: '#f97316', strokeWidth: 2, strokeDasharray: '5 3' },
        animated: false,
      }
    }

    return {
      id: `edge-${i}`,
      source: conflict.agents[0],
      target: conflict.agents[1],
      label: 'Directional Conflict',
      labelStyle: { fontSize: 11, fontWeight: 600 },
      labelBgStyle: { fill: '#450a0a', fillOpacity: 0.9 },
      labelBgPadding: [6, 4] as [number, number],
      labelBgBorderRadius: 4,
      style: { stroke: '#ef4444', strokeWidth: 2, strokeDasharray: '0' },
      animated: true,
    }
  })
}

export default function ContradictionMap({ conflicts }: Props) {
  const edges = buildEdges(conflicts)
  const [nodes, , onNodesChange] = useNodesState(AGENT_NODES)
  const [edgeState, , onEdgesChange] = useEdgesState(edges)

  const onInit = useCallback(() => {}, [])

  if (conflicts.length === 0) {
    return (
      <div className="rounded-xl border border-zinc-700/50 bg-zinc-900 p-6 text-center">
        <p className="text-sm text-zinc-500">No conflicts detected between agents.</p>
      </div>
    )
  }

  return (
    <div className="rounded-xl border border-zinc-700/50 bg-zinc-900 overflow-hidden" style={{ height: 420 }}>
      <ReactFlow
        nodes={nodes}
        edges={edgeState}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onInit={onInit}
        fitView
        attributionPosition="bottom-right"
      >
        <Background variant={BackgroundVariant.Dots} gap={20} size={1} color="#27272a" />
      </ReactFlow>
    </div>
  )
}