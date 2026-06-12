import { create } from 'zustand'

interface AuthState {
  token: string | null
  username: string | null
  roleCode: string | null
  agentId: number | null
  permissions: string[]
  setAuth: (data: {
    token: string
    username: string
    roleCode: string
    agentId: number | null
    permissions: string[]
  }) => void
  logout: () => void
  hasPermission: (code: string) => boolean
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: localStorage.getItem('token'),
  username: localStorage.getItem('username'),
  roleCode: localStorage.getItem('roleCode'),
  agentId: localStorage.getItem('agentId') ? Number(localStorage.getItem('agentId')) : null,
  permissions: JSON.parse(localStorage.getItem('permissions') || '[]'),

  setAuth: (data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('roleCode', data.roleCode)
    if (data.agentId) localStorage.setItem('agentId', String(data.agentId))
    localStorage.setItem('permissions', JSON.stringify(data.permissions))
    set(data)
  },

  logout: () => {
    localStorage.clear()
    set({ token: null, username: null, roleCode: null, agentId: null, permissions: [] })
  },

  hasPermission: (code: string) => get().permissions.includes(code),
}))
