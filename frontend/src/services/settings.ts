import api from './api'

export async function listUsers(): Promise<any[]> {
  const res = await api.get<any, { code: number; message: string; data: any[] }>('/settings/users')
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function listRoles(): Promise<any[]> {
  const res = await api.get<any, { code: number; message: string; data: any[] }>('/settings/roles')
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function listPermissions(): Promise<any[]> {
  const res = await api.get<any, { code: number; message: string; data: any[] }>('/settings/permissions')
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}
