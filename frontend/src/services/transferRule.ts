import api from './api'

export interface TransferRule {
  id?: number
  employeeId: number
  triggerType: string
  conditionJson: string
  actions: string
  priority: number
  enabled: boolean
}

export async function listTransferRules(employeeId?: number): Promise<TransferRule[]> {
  const params = employeeId ? `?employeeId=${employeeId}` : ''
  const res = await api.get<any, { code: number; message: string; data: TransferRule[] }>(`/transfer-rules${params}`)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function createTransferRule(data: TransferRule): Promise<TransferRule> {
  const res = await api.post<any, { code: number; message: string; data: TransferRule }>('/transfer-rules', data)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function updateTransferRule(id: number, data: Partial<TransferRule>): Promise<TransferRule> {
  const res = await api.put<any, { code: number; message: string; data: TransferRule }>(`/transfer-rules/${id}`, data)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function deleteTransferRule(id: number): Promise<void> {
  const res = await api.delete<any, { code: number; message: string }>(`/transfer-rules/${id}`)
  if (res.code !== 0) throw new Error(res.message)
}

export async function getAssignmentStrategy(): Promise<string> {
  const res = await api.get<any, { code: number; message: string; data: string }>('/assignment-strategy')
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function updateAssignmentStrategy(strategy: string): Promise<void> {
  const res = await api.put<any, { code: number; message: string }>('/assignment-strategy', { strategy })
  if (res.code !== 0) throw new Error(res.message)
}
