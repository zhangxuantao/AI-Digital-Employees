import api from './api'

export interface HumanAgent {
  id?: number
  name: string
  phone: string
  status: string
  currentLoad: number
  maxConcurrent: number
}

export interface AgentChannelPermission {
  id?: number
  agentId: number
  platform: string
  employeeId: number
  employeeName?: string
}

export async function listAgents(): Promise<HumanAgent[]> {
  const res = await api.get<any, { code: number; message: string; data: HumanAgent[] }>('/agents')
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function createAgent(data: HumanAgent): Promise<HumanAgent> {
  const res = await api.post<any, { code: number; message: string; data: HumanAgent }>('/agents', data)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function updateAgent(id: number, data: Partial<HumanAgent>): Promise<HumanAgent> {
  const res = await api.put<any, { code: number; message: string; data: HumanAgent }>(`/agents/${id}`, data)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function deleteAgent(id: number): Promise<void> {
  const res = await api.delete<any, { code: number; message: string }>(`/agents/${id}`)
  if (res.code !== 0) throw new Error(res.message)
}

export async function listChannelPermissions(agentId: number): Promise<AgentChannelPermission[]> {
  const res = await api.get<any, { code: number; message: string; data: AgentChannelPermission[] }>(`/agents/${agentId}/channels`)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function addChannelPermission(agentId: number, data: { platform: string; employeeId: number }): Promise<AgentChannelPermission> {
  const res = await api.post<any, { code: number; message: string; data: AgentChannelPermission }>(`/agents/${agentId}/channels`, data)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function deleteChannelPermission(agentId: number, permissionId: number): Promise<void> {
  const res = await api.delete<any, { code: number; message: string }>(`/agents/${agentId}/channels/${permissionId}`)
  if (res.code !== 0) throw new Error(res.message)
}
