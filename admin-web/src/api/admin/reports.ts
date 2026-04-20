import { API_ENDPOINTS } from '../endpoints'
import { get, post } from '../http'
import { getToken } from '../auth-storage'
import { compilePath } from '../path'

export type UserBalanceReportItem = {
  file_name: string
  file_size_kb: number
  updated_at: string
}

export async function fetchUserBalanceReports(): Promise<UserBalanceReportItem[]> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_REPORTS_USER_BALANCES
  const data = await get<{ list?: UserBalanceReportItem[] } | UserBalanceReportItem[]>(endpoint.path, {
    scope: 'admin',
  })
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export async function exportTodayUserBalanceReport(): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_REPORTS_USER_BALANCES_EXPORT_TODAY
  await post(endpoint.path, {}, { scope: 'admin' })
}

export async function downloadUserBalanceReport(fileName: string): Promise<void> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_REPORTS_USER_BALANCES_BY_FILE_NAME
  const path = compilePath(endpoint.path, { fileName })
  const baseURL = import.meta.env.VITE_API_BASE_URL?.trim() || ''
  const url = `${baseURL}${path}`

  const token = getToken('admin')
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
    },
  })
  if (!response.ok) {
    throw new Error('下载失败，请稍后重试')
  }
  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = objectUrl
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(objectUrl)
}

