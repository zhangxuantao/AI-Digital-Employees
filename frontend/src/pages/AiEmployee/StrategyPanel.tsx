import { useEffect, useState } from 'react'
import { Modal, Switch, Button, Space, Input, message, Spin } from 'antd'
import {
  DndContext, closestCenter, PointerSensor, useSensor, useSensors,
  type DragEndEvent,
} from '@dnd-kit/core'
import {
  SortableContext, useSortable, verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import {
  listStrategies, saveStrategy, deleteStrategy, batchSortStrategies,
  ReplyStrategy, AiEmployee,
} from '../../services/aiEmployee'

const STRATEGY_LABELS: Record<string, string> = {
  GREETING: '问候策略', EXCLUDE: '排除策略', MODERATION: '异常策略',
  KNOWLEDGE: '调用策略', COLLECT: '收集策略', FILTER: '筛选策略',
  FOLLOWUP: '沉默追问', CONTACT: '留资策略',
}

interface Props {
  open: boolean
  employee: AiEmployee
  onClose: () => void
}

function SortableCard({ strategy, onToggle, onEdit }: {
  strategy: ReplyStrategy
  onToggle: (s: ReplyStrategy) => void
  onEdit: (s: ReplyStrategy) => void
}) {
  const { attributes, listeners, setNodeRef, transform, transition } = useSortable({ id: strategy.id! })
  const style = { transform: CSS.Transform.toString(transform), transition }

  return (
    <div ref={setNodeRef} style={{
      ...style, border: '1px solid #e8e8e8', borderRadius: 8, padding: '10px 16px',
      marginBottom: 8, display: 'flex', alignItems: 'center', gap: 10,
      background: strategy.enabled ? '#fff' : '#fafafa', opacity: strategy.enabled ? 1 : 0.7,
    }}>
      <span {...attributes} {...listeners} style={{ cursor: 'grab', color: '#bfbfbf', fontSize: 18 }}>⋮⋮</span>
      <div style={{ flex: 1, cursor: 'pointer' }} onClick={() => onEdit(strategy)}>
        <div style={{ fontWeight: 600, fontSize: 13 }}>
          {STRATEGY_LABELS[strategy.strategyType] ?? strategy.strategyType}
          <span style={{ color: '#8c8c8c', fontWeight: 400, fontSize: 11, marginLeft: 6 }}>
            {strategy.strategyType}
          </span>
        </div>
        <div style={{ fontSize: 11, color: '#8c8c8c', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {strategy.configJson}
        </div>
      </div>
      <Switch checked={strategy.enabled} size="small" onChange={() => onToggle(strategy)} />
    </div>
  )
}

export default function StrategyPanel({ open, employee, onClose }: Props) {
  const [strategies, setStrategies] = useState<ReplyStrategy[]>([])
  const [loading, setLoading] = useState(false)
  const [editJson, setEditJson] = useState<string>('')
  const [editingStrategy, setEditingStrategy] = useState<ReplyStrategy | null>(null)

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }))

  useEffect(() => {
    if (open && employee.id) {
      setLoading(true)
      listStrategies(employee.id)
        .then(setStrategies)
        .catch((e) => message.error(e?.message ?? '加载策略失败'))
        .finally(() => setLoading(false))
    }
  }, [open, employee.id])

  const handleToggle = async (s: ReplyStrategy) => {
    try {
      if (s.enabled) {
        await deleteStrategy(employee.id!, s.id!)
      } else {
        await saveStrategy(employee.id!, { ...s, enabled: true })
      }
      message.success(s.enabled ? '已禁用' : '已启用')
    } catch (e: any) {
      message.error(e?.message ?? '操作失败')
      return
    }
    // Re-fetch after successful toggle (separate from toggle error handling)
    try {
      const updated = await listStrategies(employee.id!)
      setStrategies(updated)
    } catch {
      // Silently fail — the toggle itself succeeded, UI will update on next open
    }
  }

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event
    if (!over || active.id === over.id) return
    const oldIndex = strategies.findIndex((s) => s.id === active.id)
    const newIndex = strategies.findIndex((s) => s.id === over.id)
    const reordered = [...strategies]
    const [moved] = reordered.splice(oldIndex, 1)
    reordered.splice(newIndex, 0, moved)
    const items = reordered.map((s, i) => ({ id: s.id!, sortOrder: i }))
    setStrategies(reordered)
    try {
      await batchSortStrategies(employee.id!, items)
      message.success('排序已更新')
    } catch (e: any) {
      message.error(e?.message ?? '排序更新失败')
      try {
        const fresh = await listStrategies(employee.id!)
        setStrategies(fresh)
      } catch {
        // Rollback fetch failed — close panel to force re-fetch on next open
        onClose()
      }
    }
  }

  const handleSaveJson = async () => {
    if (!editingStrategy) return
    try {
      JSON.parse(editJson) // Validate JSON syntax
      await saveStrategy(employee.id!, { ...editingStrategy, configJson: editJson })
      setEditingStrategy(null)
      const updated = await listStrategies(employee.id!)
      setStrategies(updated)
      message.success('配置已保存')
    } catch (e: any) {
      if (e instanceof SyntaxError) {
        message.error('JSON 格式错误，请检查语法')
      } else {
        message.error(e?.message ?? '保存失败')
      }
    }
  }

  return (
    <Modal title={`${employee.name} — 回复策略配置`} open={open} onCancel={onClose} footer={null} width={640}
      destroyOnClose>
      <Spin spinning={loading}>
        <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
          <SortableContext items={strategies.map((s) => s.id!)} strategy={verticalListSortingStrategy}>
            {strategies.map((s) => (
              <SortableCard key={s.id} strategy={s} onToggle={handleToggle} onEdit={(st) => {
                setEditingStrategy(st)
                setEditJson(st.configJson)
              }} />
            ))}
          </SortableContext>
        </DndContext>
        {strategies.length === 0 && !loading && (
          <div style={{ textAlign: 'center', color: '#bfbfbf', padding: 40 }}>暂无策略配置</div>
        )}
      </Spin>
      {editingStrategy && (
        <div style={{ marginTop: 16, border: '1px solid #e8e8e8', borderRadius: 8, padding: 16 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>
            编辑 {STRATEGY_LABELS[editingStrategy.strategyType]} 配置 (JSON)
          </div>
          <Input.TextArea rows={6} value={editJson} onChange={(e) => setEditJson(e.target.value)}
            style={{ fontFamily: 'monospace' }} />
          <Space style={{ marginTop: 8 }}>
            <Button type="primary" onClick={handleSaveJson}>保存</Button>
            <Button onClick={() => setEditingStrategy(null)}>取消</Button>
          </Space>
        </div>
      )}
    </Modal>
  )
}
