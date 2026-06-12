import api from './api'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  username: string
  roleCode: string
  agentId: number | null
  permissions: string[]
}

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const res = await api.post<any, { code: number; message: string; data: LoginResponse }>('/auth/login', data)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function getTicket(): Promise<{ ticket: string }> {
  const res = await api.post<any, { code: number; message: string; data: { ticket: string } }>('/auth/ticket')
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}
