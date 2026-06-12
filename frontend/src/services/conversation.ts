import api from './api'

export interface Conversation {
  id?: number
  customerId: number
  customerName?: string
  customerAvatar?: string
  channel?: string
  status: string
  lastMessage?: string
  lastMessageTime?: string
  unreadCount: number
}

export interface Message {
  id?: number
  conversationId: number
  senderType: string // CUSTOMER, AI, AGENT, SYSTEM
  content: string
  createdAt?: string
}

export async function listConversations(): Promise<Conversation[]> {
  const res = await api.get<any, { code: number; message: string; data: Conversation[] }>('/conversations')
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function getMessages(conversationId: number): Promise<Message[]> {
  const res = await api.get<any, { code: number; message: string; data: Message[] }>(`/conversations/${conversationId}/messages`)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function closeConversation(id: number): Promise<void> {
  const res = await api.post<any, { code: number; message: string }>(`/conversations/${id}/close`)
  if (res.code !== 0) throw new Error(res.message)
}

export async function getTicket(): Promise<string> {
  const res = await api.post<any, { code: number; message: string; data: string }>('/auth/ticket')
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function getCustomerDetail(id: number): Promise<any> {
  const res = await api.get<any, { code: number; message: string; data: any }>(`/customers/${id}`)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}
