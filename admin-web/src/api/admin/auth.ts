import { API_ENDPOINTS } from '../endpoints'
import { post, get } from '../http'
import { clearToken, setToken } from '../auth-storage'
import type { AdminMe } from '../../mock/admin'

export type AdminLoginPayload = {
  username: string
  password: string
}

export type AdminLoginResult = {
  access_token: string
  refresh_token?: string
  admin_user?: AdminMe
}

export async function adminLogin(payload: AdminLoginPayload): Promise<AdminLoginResult> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_AUTH_LOGIN
  return await post<AdminLoginResult, AdminLoginPayload>(endpoint.path, payload, {
    scope: 'admin',
    skipAuth: true,
  })
}

export async function adminFetchMe(): Promise<AdminMe> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_AUTH_ME
  return await get<AdminMe>(endpoint.path, { scope: 'admin' })
}

export async function adminLogout(): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_AUTH_LOGOUT
  try {
    await post(endpoint.path, {}, { scope: 'admin' })
  } finally {
    clearToken('admin')
  }
}

export function persistAdminToken(token: string): void {
  setToken(token, 'admin')
}
