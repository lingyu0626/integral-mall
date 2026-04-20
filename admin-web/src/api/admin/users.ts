import { API_ENDPOINTS } from '../endpoints'
import { get, post, put } from '../http'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type { AdminPointLedger, AdminUser, UserStatus } from '../../mock/admin'

export type UserListQuery = Partial<PageQuery> & {
  keyword?: string
  sortField?: 'id' | 'point_balance' | 'total_consume_amount' | 'profit_amount'
  sortOrder?: 'ascend' | 'descend'
}

export type AdjustUserPointPayload = {
  adjust_point: number
  adjust_remark?: string
  draw_unit_price?: number
  consume_amount?: number
  manual_profit_adjust?: number
  prize_name?: string
  draw_count?: number
}

export type UpdateUserFinancePayload = {
  total_consume_amount: number
  profit_amount: number
}

export type UserPointLedgerQuery = Partial<PageQuery>

export async function fetchAdminUsers(query: UserListQuery): Promise<ApiPageData<AdminUser>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_USERS
  return await get<ApiPageData<AdminUser>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function updateAdminUserStatus(userId: number, status: UserStatus): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_USERS_BY_USER_ID_STATUS
  await put(compilePath(endpoint.path, { userId }), { user_status_code: status }, { scope: 'admin' })
}

export async function adjustAdminUserPoints(
  userId: number,
  payload: AdjustUserPointPayload | number,
  remark = '',
): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_USERS_BY_USER_ID_POINTS_ADJUST
  const body: AdjustUserPointPayload = typeof payload === 'number'
    ? {
        adjust_point: payload,
        adjust_remark: remark,
      }
    : {
        ...payload,
        adjust_remark: payload.adjust_remark ?? remark,
      }
  await post(compilePath(endpoint.path, { userId }), body, { scope: 'admin' })
}

export async function adjustAdminUserPointsSimple(userId: number, delta: number, remark = ''): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_USERS_BY_USER_ID_POINTS_ADJUST
  const payload: AdjustUserPointPayload = {
    adjust_point: delta,
    adjust_remark: remark,
  }
  await post(compilePath(endpoint.path, { userId }), payload, { scope: 'admin' })
}

export async function updateAdminUserRemark(userId: number, adminRemark: string): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_USERS_BY_USER_ID_REMARK
  await put(compilePath(endpoint.path, { userId }), { admin_remark: adminRemark }, { scope: 'admin' })
}

export async function updateAdminUserFinance(userId: number, payload: UpdateUserFinancePayload): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_USERS_BY_USER_ID_FINANCE
  await put(compilePath(endpoint.path, { userId }), payload, { scope: 'admin' })
}

export async function fetchAdminPointLedger(query: Partial<PageQuery>): Promise<ApiPageData<AdminPointLedger>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_POINTS_LEDGER
  return await get<ApiPageData<AdminPointLedger>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function fetchAdminUserPointLedger(
  userId: number,
  query: UserPointLedgerQuery,
): Promise<ApiPageData<AdminPointLedger>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_USERS_BY_USER_ID_POINTS_LEDGER
  return await get<ApiPageData<AdminPointLedger>>(compilePath(endpoint.path, { userId }), {
    scope: 'admin',
    params: query,
  })
}

export async function restoreAdminUserPointAdjust(userId: number, ledgerId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_USERS_BY_USER_ID_POINTS_ADJUST_BY_LEDGER_ID_RESTORE
  await post(compilePath(endpoint.path, { userId, ledgerId }), {}, { scope: 'admin' })
}
