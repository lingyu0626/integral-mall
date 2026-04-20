import { API_ENDPOINTS } from '../endpoints'
import { get } from '../http'
import type { DashboardOverview } from '../../mock/admin'

export async function fetchDashboardOverview(): Promise<DashboardOverview> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_DASHBOARD_OVERVIEW
  return await get<DashboardOverview>(endpoint.path, { scope: 'admin' })
}
