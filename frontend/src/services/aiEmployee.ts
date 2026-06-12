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

export async function listEmployees() {
  const res = await api.get<any, { code: number; data: AiEmployee[] }>('/ai-employees')
  return res.data
}

export async function getEmployee(id: number) {
  const res = await api.get<any, { code: number; data: AiEmployee }>(`/ai-employees/${id}`)
  return res.data
}

export async function createEmployee(data: AiEmployee) {
  const res = await api.post<any, { code: number; data: AiEmployee }>('/ai-employees', data)
  return res.data
}

export async function updateEmployee(id: number, data: Partial<AiEmployee>) {
  const res = await api.put<any, { code: number; data: AiEmployee }>(`/ai-employees/${id}`, data)
  return res.data
}

export async function deleteEmployee(id: number) {
  await api.delete(`/ai-employees/${id}`)
}

export async function listStrategies(employeeId: number) {
  const res = await api.get<any, { code: number; data: ReplyStrategy[] }>(`/ai-employees/${employeeId}/strategies`)
  return res.data
}

export async function saveStrategy(employeeId: number, data: ReplyStrategy) {
  const res = await api.post<any, { code: number; data: ReplyStrategy }>(`/ai-employees/${employeeId}/strategies`, data)
  return res.data
}

export async function deleteStrategy(employeeId: number, strategyId: number) {
  await api.delete(`/ai-employees/${employeeId}/strategies/${strategyId}`)
}

export async function batchSortStrategies(employeeId: number, items: { id: number; sortOrder: number }[]) {
  await api.patch(`/ai-employees/${employeeId}/strategies/batch-sort`, items)
}
