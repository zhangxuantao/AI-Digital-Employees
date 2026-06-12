import { useState, useEffect, useRef, useCallback } from 'react'
import {
  Typography, Input, Button, Badge, Tag, Avatar, Empty, message,
  Tooltip, Popconfirm, Skeleton,
} from 'antd'
import {
  MessageOutlined, SendOutlined, SwapOutlined,
  CustomerServiceOutlined, RobotOutlined, SettingOutlined,
  ClockCircleOutlined, CheckCircleOutlined, StopOutlined,
  UserOutlined,
} from '@ant-design/icons'
import {
  listConversations, getMessages, closeConversation,
  getTicket, getCustomerDetail,
} from '../../services/conversation'
import { wsService } from '../../services/websocket'
import type { Conversation, Message } from '../../services/conversation'

const { Text, Title } = Typography
const { TextArea } = Input

/* ============================================================
   Constants
   ============================================================ */

const NAV_ITEMS = [
  { key: 'all', icon: <MessageOutlined />, label: '全部会话' },
  { key: 'waiting', icon: <ClockCircleOutlined />, label: '待接入' },
  { key: 'active', icon: <CustomerServiceOutlined />, label: '进行中' },
  { key: 'ai', icon: <RobotOutlined />, label: 'AI接待' },
  { key: 'transferred', icon: <SwapOutlined />, label: '已转接' },
  { key: 'closed', icon: <CheckCircleOutlined />, label: '已关闭' },
  { key: 'settings', icon: <SettingOutlined />, label: '设置' },
]

const CHANNEL_MAP: Record<string, { color: string; label: string }> = {
  WECHAT: { color: 'green', label: '微信' },
  WEBSITE: { color: 'blue', label: '网站' },
  APP: { color: 'purple', label: 'APP' },
  WECHAT_MP: { color: 'cyan', label: '公众号' },
  CUSTOM: { color: 'orange', label: '自定义' },
}

const SENDER_CONFIG: Record<string, { align: 'left' | 'right'; bg: string; textColor: string; label: string }> = {
  CUSTOMER: { align: 'left', bg: '#f0f0f0', textColor: '#333', label: '客户' },
  AI: { align: 'right', bg: '#e6f4ff', textColor: '#1677ff', label: 'AI' },
  AGENT: { align: 'right', bg: '#f6ffed', textColor: '#52c41a', label: '客服' },
  SYSTEM: { align: 'left', bg: 'transparent', textColor: '#faad14', label: '系统' },
}

/* ============================================================
   Helpers
   ============================================================ */

function formatTime(dateStr?: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const hhmm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  // Same day → HH:mm
  if (d.toDateString() === now.toDateString()) return hhmm
  // This year → MM-DD HH:mm
  if (d.getFullYear() === now.getFullYear()) return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${hhmm}`
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${hhmm}`
}

function getUnreadTotal(conversations: Conversation[]): number {
  return conversations.reduce((sum, c) => sum + (c.unreadCount ?? 0), 0)
}

/* ============================================================
   NavPanel (leftmost 60px)
   ============================================================ */

interface NavPanelProps {
  activeNav: string
  onNavChange: (key: string) => void
  conversations: Conversation[]
}

function NavPanel({ activeNav, onNavChange, conversations }: NavPanelProps) {
  const badgeCount = (key: string) => {
    if (key === 'all') return getUnreadTotal(conversations)
    // For specific status filters with unread
    if (key === 'waiting') return conversations.filter((c) => c.status === 'WAITING').length
    return 0
  }

  return (
    <div
      style={{
        width: 60,
        borderRight: '1px solid #f0f0f0',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        paddingTop: 8,
        gap: 4,
        background: '#fafafa',
        flexShrink: 0,
      }}
    >
      {NAV_ITEMS.map((item) => {
        const isActive = activeNav === item.key
        const count = badgeCount(item.key)
        return (
          <Tooltip key={item.key} title={item.label} placement="right">
            <Badge count={count} size="small" offset={[2, -2]}>
              <Button
                type="text"
                size="large"
                icon={item.icon}
                onClick={() => onNavChange(item.key)}
                style={{
                  width: 44,
                  height: 44,
                  fontSize: 18,
                  color: isActive ? '#1677ff' : '#666',
                  background: isActive ? '#e6f4ff' : 'transparent',
                  borderRadius: 8,
                }}
              />
            </Badge>
          </Tooltip>
        )
      })}
    </div>
  )
}

/* ============================================================
   ConversationPanel (top-center)
   ============================================================ */

interface ConversationPanelProps {
  conversations: Conversation[]
  selectedId: number | null
  loading: boolean
  searchText: string
  onSearchChange: (v: string) => void
  onSelect: (id: number) => void
}

function ConversationPanel({
  conversations, selectedId, loading,
  searchText, onSearchChange, onSelect,
}: ConversationPanelProps) {
  const renderItem = (conv: Conversation) => {
    const channelMeta = CHANNEL_MAP[conv.channel ?? ''] ?? { color: 'default', label: conv.channel ?? '-' }
    const isSelected = conv.id === selectedId

    return (
      <div
        key={conv.id}
        onClick={() => conv.id && onSelect(conv.id)}
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          padding: '12px 16px',
          cursor: 'pointer',
          borderBottom: '1px solid #f5f5f5',
          background: isSelected ? '#e6f4ff' : 'transparent',
          transition: 'background 0.2s',
        }}
        onMouseEnter={(e) => { if (!isSelected) e.currentTarget.style.background = '#fafafa' }}
        onMouseLeave={(e) => { if (!isSelected) e.currentTarget.style.background = 'transparent' }}
      >
        <Avatar
          size={40}
          src={conv.customerAvatar}
          icon={!conv.customerAvatar ? <UserOutlined /> : undefined}
          style={{ flexShrink: 0 }}
        />
        <div style={{ flex: 1, marginLeft: 12, minWidth: 0 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Text strong ellipsis style={{ maxWidth: 140, fontSize: 14 }}>
              {conv.customerName ?? `客户${conv.customerId}`}
            </Text>
            <Text type="secondary" style={{ fontSize: 11, whiteSpace: 'nowrap' }}>
              {formatTime(conv.lastMessageTime)}
            </Text>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
            <Text
              type="secondary"
              ellipsis
              style={{ fontSize: 12, maxWidth: 160, color: '#999' }}
            >
              {conv.lastMessage ?? '暂无消息'}
            </Text>
            <div style={{ display: 'flex', alignItems: 'center', gap: 4, flexShrink: 0 }}>
              <Tag color={channelMeta.color} style={{ margin: 0, fontSize: 10, lineHeight: '18px' }}>
                {channelMeta.label}
              </Tag>
              {conv.unreadCount > 0 && (
                <Badge count={conv.unreadCount} size="small" />
              )}
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ padding: '12px 16px', borderBottom: '1px solid #f0f0f0' }}>
        <Input.Search
          placeholder="搜索客户..."
          value={searchText}
          onChange={(e) => onSearchChange(e.target.value)}
          allowClear
          size="small"
        />
      </div>
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {loading ? (
          <div style={{ padding: 16 }}>
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
                <Skeleton.Avatar active size={40} />
                <div style={{ flex: 1 }}>
                  <Skeleton active paragraph={{ rows: 1, width: '60%' }} title={{ width: '40%' }} />
                </div>
              </div>
            ))}
          </div>
        ) : conversations.length === 0 ? (
          <Empty
            description="暂无会话"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            style={{ marginTop: 60 }}
          />
        ) : (
          conversations.map(renderItem)
        )}
      </div>
    </div>
  )
}

/* ============================================================
   MessageBubble
   ============================================================ */

function MessageBubble({ msg }: { msg: Message }) {
  const config = SENDER_CONFIG[msg.senderType] ?? SENDER_CONFIG.SYSTEM

  if (msg.senderType === 'SYSTEM') {
    return (
      <div style={{ textAlign: 'center', margin: '12px 0' }}>
        <Tag color="gold" style={{ fontSize: 11 }}>{msg.content}</Tag>
      </div>
    )
  }

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: config.align === 'right' ? 'flex-end' : 'flex-start',
        marginBottom: 12,
      }}
    >
      <div
        style={{
          maxWidth: '70%',
          padding: '8px 14px',
          borderRadius: 12,
          background: config.bg,
          color: config.textColor,
          fontSize: 14,
          lineHeight: 1.5,
          wordBreak: 'break-word',
          borderBottomRightRadius: config.align === 'right' ? 4 : 12,
          borderBottomLeftRadius: config.align === 'left' ? 4 : 12,
        }}
      >
        {msg.content}
      </div>
      <Text type="secondary" style={{ fontSize: 10, marginTop: 2 }}>
        {config.label} · {formatTime(msg.createdAt)}
      </Text>
    </div>
  )
}

/* ============================================================
   MessagePanel (bottom-center)
   ============================================================ */

interface MessagePanelProps {
  messages: Message[]
  loading: boolean
  selectedConv: Conversation | null
  onSend: (content: string) => void
  onEndSession: () => void
  onTransfer: () => void
}

function MessagePanel({
  messages, loading, selectedConv,
  onSend, onEndSession, onTransfer,
}: MessagePanelProps) {
  const [inputValue, setInputValue] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = useCallback(() => {
    setTimeout(() => {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }, 50)
  }, [])

  useEffect(() => {
    scrollToBottom()
  }, [messages, scrollToBottom])

  const handleSend = () => {
    const trimmed = inputValue.trim()
    if (!trimmed) return
    onSend(trimmed)
    setInputValue('')
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  if (!selectedConv) {
    return (
      <div
        style={{
          flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
          background: '#fafafa',
        }}
      >
        <Empty
          description="选择一个会话开始聊天"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      </div>
    )
  }

  const isClosed = selectedConv.status === 'CLOSED'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: '#fff' }}>
      {/* Messages area */}
      <div
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '16px 20px',
          background: '#fafafa',
        }}
      >
        {loading ? (
          <div style={{ padding: 20 }}>
            {[1, 2, 3].map((i) => (
              <div key={i} style={{ marginBottom: 16, display: 'flex', justifyContent: i % 2 === 0 ? 'flex-end' : 'flex-start' }}>
                <Skeleton active paragraph={{ rows: 1, width: 120 }} title={false} />
              </div>
            ))}
          </div>
        ) : messages.length === 0 ? (
          <Empty
            description="暂无消息"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            style={{ marginTop: 40 }}
          />
        ) : (
          messages.map((msg) => (
            <MessageBubble key={msg.id ?? Math.random()} msg={msg} />
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input area */}
      <div
        style={{
          borderTop: '1px solid #f0f0f0',
          padding: '12px 16px',
          background: '#fff',
        }}
      >
        <TextArea
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={isClosed ? '会话已关闭' : '输入消息...'}
          disabled={isClosed}
          rows={2}
          style={{ resize: 'none', marginBottom: 8 }}
        />
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', gap: 8 }}>
            {isClosed ? (
              <Button size="small" disabled icon={<CheckCircleOutlined />}>
                已关闭
              </Button>
            ) : (
              <>
                <Popconfirm
                  title="确定结束此会话?"
                  onConfirm={onEndSession}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button size="small" icon={<StopOutlined />} danger>
                    结束会话
                  </Button>
                </Popconfirm>
                <Popconfirm
                  title="确定转接此会话?"
                  onConfirm={onTransfer}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button size="small" icon={<SwapOutlined />}>
                    转接
                  </Button>
                </Popconfirm>
              </>
            )}
          </div>
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSend}
            disabled={isClosed || !inputValue.trim()}
          >
            发送
          </Button>
        </div>
      </div>
    </div>
  )
}

/* ============================================================
   CustomerCard (rightmost 280px)
   ============================================================ */

interface CustomerCardProps {
  customerDetail: any
  loading: boolean
}

function CustomerCard({ customerDetail, loading }: CustomerCardProps) {
  if (loading) {
    return (
      <div style={{ padding: 16 }}>
        <Skeleton active avatar paragraph={{ rows: 6 }} />
      </div>
    )
  }

  if (!customerDetail) {
    return (
      <div
        style={{
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          height: '100%',
        }}
      >
        <Empty description="选择客户查看详情" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      </div>
    )
  }

  const basicFields = [
    { label: '渠道', value: CHANNEL_MAP[customerDetail.channel]?.label ?? customerDetail.channel ?? '-' },
    { label: '来源', value: customerDetail.source ?? '-' },
    { label: '首次联系', value: formatTime(customerDetail.firstContactTime) || '-' },
    { label: '状态', value: customerDetail.status ?? '-' },
  ]

  const aiFields = customerDetail.aiCollectedFields ?? {}

  return (
    <div style={{ padding: 16, height: '100%', overflowY: 'auto' }}>
      {/* Avatar + Name */}
      <div style={{ textAlign: 'center', marginBottom: 20 }}>
        <Avatar
          size={64}
          src={customerDetail.avatar}
          icon={!customerDetail.avatar ? <UserOutlined /> : undefined}
        />
        <Title level={5} style={{ marginTop: 8, marginBottom: 4 }}>
          {customerDetail.name ?? `客户${customerDetail.id}`}
        </Title>
        <Badge
          status={customerDetail.online ? 'success' : 'default'}
          text={customerDetail.online ? '在线' : '离线'}
        />
      </div>

      {/* Basic Info */}
      <div style={{ marginBottom: 16 }}>
        <Text strong style={{ fontSize: 12, color: '#999' }}>基本信息</Text>
        <div style={{ marginTop: 8 }}>
          {basicFields.map((f) => (
            <div key={f.label} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>{f.label}</Text>
              <Text style={{ fontSize: 12 }}>{f.value}</Text>
            </div>
          ))}
        </div>
      </div>

      {/* Tags */}
      {customerDetail.tags && customerDetail.tags.length > 0 && (
        <div style={{ marginBottom: 16 }}>
          <Text strong style={{ fontSize: 12, color: '#999' }}>标签</Text>
          <div style={{ marginTop: 6, display: 'flex', flexWrap: 'wrap', gap: 4 }}>
            {customerDetail.tags.map((tag: string, i: number) => (
              <Tag key={i} color="blue" style={{ fontSize: 11 }}>{tag}</Tag>
            ))}
          </div>
        </div>
      )}

      {/* AI Collected Fields */}
      {Object.keys(aiFields).length > 0 && (
        <div style={{ marginBottom: 16 }}>
          <Text strong style={{ fontSize: 12, color: '#999' }}>AI 收集信息</Text>
          <div style={{ marginTop: 8 }}>
            {Object.entries(aiFields).map(([key, value]) => (
              <div key={key} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                <Text type="secondary" style={{ fontSize: 12 }}>{key}</Text>
                <Text style={{ fontSize: 12 }}>{String(value)}</Text>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recent Conversations (placeholder) */}
      {customerDetail.recentConversations && customerDetail.recentConversations.length > 0 && (
        <div>
          <Text strong style={{ fontSize: 12, color: '#999' }}>近期会话</Text>
          <div style={{ marginTop: 8 }}>
            {customerDetail.recentConversations.map((rc: any, i: number) => (
              <div
                key={i}
                style={{
                  padding: '8px 0',
                  borderBottom: i < customerDetail.recentConversations.length - 1 ? '1px solid #f5f5f5' : 'none',
                }}
              >
                <Text style={{ fontSize: 12 }} ellipsis>{rc.summary ?? '-'}</Text>
                <br />
                <Text type="secondary" style={{ fontSize: 10 }}>{formatTime(rc.time)}</Text>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

/* ============================================================
   Main IM Page Component
   ============================================================ */

export default function IMPage() {
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [convsLoading, setConvsLoading] = useState(false)
  const [selectedConvId, setSelectedConvId] = useState<number | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [msgsLoading, setMsgsLoading] = useState(false)
  const [customerDetail, setCustomerDetail] = useState<any>(null)
  const [custLoading, setCustLoading] = useState(false)
  const [activeNav, setActiveNav] = useState('all')
  const [searchText, setSearchText] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [connected, setConnected] = useState(false)

  // Refs for WebSocket callback freshness
  const selectedConvIdRef = useRef(selectedConvId)
  const conversationsRef = useRef(conversations)
  const fetchConversationsRef = useRef<(() => Promise<void>) | null>(null)

  selectedConvIdRef.current = selectedConvId
  conversationsRef.current = conversations

  // ── Fetch conversations ──────────────────────────────────

  const fetchConversations = useCallback(async () => {
    setConvsLoading(true)
    setError(null)
    try {
      const data = await listConversations()
      setConversations(data)
    } catch (e: any) {
      setError(e?.message ?? '获取会话列表失败')
      message.error(e?.message ?? '获取会话列表失败')
    } finally {
      setConvsLoading(false)
    }
  }, [])

  fetchConversationsRef.current = fetchConversations

  // ── Fetch messages ──────────────────────────────────────

  const fetchMessages = useCallback(async (convId: number) => {
    setMsgsLoading(true)
    try {
      const data = await getMessages(convId)
      setMessages(data)
    } catch (e: any) {
      message.error(e?.message ?? '获取消息失败')
    } finally {
      setMsgsLoading(false)
    }
  }, [])

  // ── Fetch customer detail ───────────────────────────────

  const fetchCustomerDetail = useCallback(async (conv: Conversation | undefined) => {
    if (!conv?.customerId) {
      setCustomerDetail(null)
      return
    }
    setCustLoading(true)
    try {
      const data = await getCustomerDetail(conv.customerId)
      setCustomerDetail(data)
    } catch {
      setCustomerDetail(null)
    } finally {
      setCustLoading(false)
    }
  }, [])

  // ── Conversation selection ──────────────────────────────

  const handleSelectConversation = useCallback((convId: number) => {
    setSelectedConvId(convId)
    fetchMessages(convId)
    const conv = conversations.find((c) => c.id === convId)
    fetchCustomerDetail(conv)
  }, [conversations, fetchMessages, fetchCustomerDetail])

  // ── Initial load ────────────────────────────────────────

  useEffect(() => {
    fetchConversations()
  }, [fetchConversations])

  // ── WebSocket Connection ────────────────────────────────

  useEffect(() => {
    let cancelled = false

    const initWs = async () => {
      try {
        const ticket = await getTicket()
        if (cancelled) return
        wsService.connect(ticket)

        // Listen for new messages
        wsService.on('message.request', (msg: any) => {
          const data = msg.data ?? msg
          const currentConvId = selectedConvIdRef.current
          if (data.conversationId === currentConvId) {
            setMessages((prev) => [...prev, data])
          }
          // Refresh conversations list to update lastMessage/unread
          fetchConversationsRef.current?.()
        })

        // Listen for customer online/offline
        wsService.on('subscriber.open', () => {
          const refetch = fetchConversationsRef.current
          if (refetch) refetch()
        })

        wsService.on('subscriber.close', () => {
          const refetch = fetchConversationsRef.current
          if (refetch) refetch()
        })

        setConnected(true)
      } catch (e: any) {
        console.error('WebSocket init error', e)
        message.warning('WebSocket 连接失败，部分实时功能不可用')
      }
    }

    initWs()

    return () => {
      cancelled = true
      wsService.disconnect()
      setConnected(false)
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // ── WebSocket Message Handlers ──────────────────────────

  const handleSend = useCallback((content: string) => {
    if (!selectedConvId) return
    // Optimistically add the message
    const tempMsg: Message = {
      id: Date.now(),
      conversationId: selectedConvId,
      senderType: 'AGENT',
      content,
      createdAt: new Date().toISOString(),
    }
    setMessages((prev) => [...prev, tempMsg])
    // Send via WebSocket
    wsService.send('message.response', { conversationId: selectedConvId, content })
    // Refresh conversations
    fetchConversations()
  }, [selectedConvId, fetchConversations])

  const handleEndSession = useCallback(async () => {
    if (!selectedConvId) return
    try {
      await closeConversation(selectedConvId)
      wsService.send('session.close', { conversationId: selectedConvId })
      message.success('会话已关闭')
      fetchConversations()
      // Update status locally
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now(),
          conversationId: selectedConvId,
          senderType: 'SYSTEM',
          content: '会话已结束',
          createdAt: new Date().toISOString(),
        },
      ])
    } catch (e: any) {
      message.error(e?.message ?? '关闭会话失败')
    }
  }, [selectedConvId, fetchConversations])

  const handleTransfer = useCallback(() => {
    if (!selectedConvId) return
    wsService.send('transfer.request', { conversationId: selectedConvId })
    message.success('已发送转接请求')
    setMessages((prev) => [
      ...prev,
      {
        id: Date.now(),
        conversationId: selectedConvId,
        senderType: 'SYSTEM',
        content: '转接请求已发送',
        createdAt: new Date().toISOString(),
      },
    ])
  }, [selectedConvId])

  // ── Filter conversations ────────────────────────────────

  const filteredConversations = conversations
    .filter((c) => {
      if (activeNav === 'all') return true
      if (activeNav === 'waiting') return c.status === 'WAITING'
      if (activeNav === 'active') return c.status === 'ACTIVE' || c.status === 'AI'
      if (activeNav === 'ai') return c.status === 'AI'
      if (activeNav === 'transferred') return c.status === 'TRANSFERRED'
      if (activeNav === 'closed') return c.status === 'CLOSED'
      return true
    })
    .filter((c) => {
      if (!searchText) return true
      const kw = searchText.toLowerCase()
      return (
        c.customerName?.toLowerCase().includes(kw) ||
        String(c.customerId).includes(kw) ||
        c.lastMessage?.toLowerCase().includes(kw)
      )
    })

  const selectedConv = conversations.find((c) => c.id === selectedConvId) ?? null

  // ── Render ──────────────────────────────────────────────

  return (
    <div
      style={{
        display: 'flex',
        height: 'calc(100vh - 112px)', // viewport minus header + margins
        border: '1px solid #f0f0f0',
        borderRadius: 8,
        overflow: 'hidden',
        background: '#fff',
      }}
    >
      {/* NavPanel */}
      <NavPanel
        activeNav={activeNav}
        onNavChange={(key) => {
          setActiveNav(key)
          setSearchText('')
        }}
        conversations={conversations}
      />

      {/* Conversation Panel */}
      <div style={{ width: 300, borderRight: '1px solid #f0f0f0', display: 'flex', flexDirection: 'column' }}>
        {error && (
          <div style={{ padding: '8px 16px', background: '#fff2f0', borderBottom: '1px solid #ffccc7', color: '#ff4d4f', fontSize: 12 }}>
            {error}
          </div>
        )}
        <ConversationPanel
          conversations={filteredConversations}
          selectedId={selectedConvId}
          loading={convsLoading}
          searchText={searchText}
          onSearchChange={setSearchText}
          onSelect={handleSelectConversation}
        />
      </div>

      {/* Message Panel */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        {/* Conversation header */}
        {selectedConv && (
          <div
            style={{
              padding: '10px 16px',
              borderBottom: '1px solid #f0f0f0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              background: '#fff',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <Text strong>{selectedConv.customerName ?? `客户${selectedConv.customerId}`}</Text>
              {!connected && (
                <Tag color="warning" style={{ fontSize: 10 }}>离线</Tag>
              )}
              <Tag color={CHANNEL_MAP[selectedConv.channel ?? '']?.color ?? 'default'} style={{ fontSize: 10 }}>
                {CHANNEL_MAP[selectedConv.channel ?? '']?.label ?? selectedConv.channel}
              </Tag>
            </div>
            <Badge
              status={selectedConv.status === 'CLOSED' ? 'default' : selectedConv.status === 'WAITING' ? 'warning' : 'processing'}
              text={selectedConv.status === 'CLOSED' ? '已关闭' : selectedConv.status === 'WAITING' ? '待接入' : selectedConv.status === 'AI' ? 'AI接待中' : '进行中'}
            />
          </div>
        )}
        <MessagePanel
          messages={messages}
          loading={msgsLoading}
          selectedConv={selectedConv}
          onSend={handleSend}
          onEndSession={handleEndSession}
          onTransfer={handleTransfer}
        />
      </div>

      {/* Customer Detail Card */}
      <div
        style={{
          width: 280,
          borderLeft: '1px solid #f0f0f0',
          background: '#fafafa',
          flexShrink: 0,
          overflow: 'hidden',
        }}
      >
        <CustomerCard customerDetail={customerDetail} loading={custLoading} />
      </div>
    </div>
  )
}
