import type { RequestScope } from './types'

const TOKEN_KEY: Record<RequestScope, string> = {
  admin: 'pm_admin_token',
  app: 'pm_app_token',
}

function canUseStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

export function getToken(scope: RequestScope = 'admin'): string | null {
  if (!canUseStorage()) return null
  return window.localStorage.getItem(TOKEN_KEY[scope])
}

export function setToken(token: string, scope: RequestScope = 'admin'): void {
  if (!canUseStorage()) return
  window.localStorage.setItem(TOKEN_KEY[scope], token)
}

export function clearToken(scope: RequestScope = 'admin'): void {
  if (!canUseStorage()) return
  window.localStorage.removeItem(TOKEN_KEY[scope])
}
