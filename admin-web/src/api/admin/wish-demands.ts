import { get, put } from '../http'
import type { ApiPageData, PageQuery } from '../types'

export type WishDemandStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export type AdminWishDemand = {
  id: number
  user_id: number
  user_name?: string
  phone_masked?: string
  wish_title: string
  wish_message?: string
  image_url?: string
  status_code: WishDemandStatus
  status_text?: string
  decision_note?: string
  created_at?: string
  decided_at?: string
  decided_by?: string
  notify_content?: string
}

export type WishDemandListQuery = Partial<PageQuery> & {
  keyword?: string
  status_code?: WishDemandStatus | ''
}

export async function fetchAdminWishDemands(query: WishDemandListQuery): Promise<ApiPageData<AdminWishDemand>> {
  return await get<ApiPageData<AdminWishDemand>>('/api/v1/admin/wish-demands', {
    scope: 'admin',
    params: query,
  })
}

export async function decideAdminWishDemand(
  wishId: number,
  payload: { decision: Extract<WishDemandStatus, 'APPROVED' | 'REJECTED'>; decision_note?: string },
): Promise<void> {
  await put(`/api/v1/admin/wish-demands/${wishId}/decision`, payload, { scope: 'admin' })
}
