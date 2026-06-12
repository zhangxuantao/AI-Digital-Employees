import api from './api'

export interface AiEmployee {
  id?: number
  name: string
  avatar?: string
  greetingMsg?: string
  style: string
  replyLength: string
  contentCheck?: string
  aggregateInterval?: number
  delayInterval?: number
  serviceTimeStart?: string
  serviceTimeEnd?: string
  weekdays?: string
  companyIntro: string
  productIntro: string
  serviceScope?: string
  status?: string
}

export interface ReplyStrategy {
  id?: number
  employeeId?: number
  strategyType: string
  configJson: string
  enabled: boolean
  sortOrder: number
}

export async function listEmployees(): Promise<AiEmployee[]> {
  const res = await api.get<any, { code: number; message: string; data: AiEmployee[] }>('/ai-employees')
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function getEmployee(id: number): Promise<AiEmployee> {
  const res = await api.get<any, { code: number; message: string; data: AiEmployee }>(`/ai-employees/${id}`)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function createEmployee(data: AiEmployee): Promise<AiEmployee> {
  const res = await api.post<any, { code: number; message: string; data: AiEmployee }>('/ai-employees', data)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function updateEmployee(id: number, data: Partial<AiEmployee>): Promise<AiEmployee> {
  const res = await api.put<any, { code: number; message: string; data: AiEmployee }>(`/ai-employees/${id}`, data)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function deleteEmployee(id: number): Promise<void> {
  await api.delete(`/ai-employees/${id}`)
}

export async function listStrategies(employeeId: number): Promise<ReplyStrategy[]> {
  const res = await api.get<any, { code: number; message: string; data: ReplyStrategy[] }>(`/ai-employees/${employeeId}/strategies`)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function saveStrategy(employeeId: number, data: ReplyStrategy): Promise<ReplyStrategy> {
  const res = await api.post<any, { code: number; message: string; data: ReplyStrategy }>(`/ai-employees/${employeeId}/strategies`, data)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function deleteStrategy(employeeId: number, strategyId: number): Promise<void> {
  await api.delete(`/ai-employees/${employeeId}/strategies/${strategyId}`)
}

export async function batchSortStrategies(employeeId: number, items: { id: number; sortOrder: number }[]): Promise<void> {
  await api.patch(`/ai-employees/${employeeId}/strategies/batch-sort`, items)
}
