import { API_ENDPOINTS } from '../endpoints'
import { del, get, post, put } from '../http'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type {
  AdminAttrDef,
  AdminProductMedia,
  AdminSku,
  AdminSpu,
  AttrValueType,
  ProductStatus,
  SkuStatus,
} from '../../mock/product-center'

export type SpuListQuery = Partial<PageQuery> & {
  keyword?: string
  status_code?: ProductStatus | ''
}

export type SaveSpuPayload = {
  spu_name: string
  category_name: string
  category_names?: string[]
  point_price_min?: number
  point_price_max?: number
  cover_image_url?: string
  image_urls?: string[]
  detail_html?: string
  sku_list?: Array<{
    sku_name: string
    spec_text?: string
    point_price: number
    stock_available: number
    status_code?: SkuStatus
  }>
}

export type SaveSkuPayload = {
  sku_name: string
  spec_text?: string
  point_price: number
  stock_available: number
  status_code: SkuStatus
}

export type SaveSpuMediaPayload = {
  media_type: 'IMAGE' | 'VIDEO'
  media_url: string
  sort_no: number
}

export type AttrDefListQuery = Partial<PageQuery> & {
  keyword?: string
}

export type SaveAttrDefPayload = {
  attr_name: string
  attr_code: string
  value_type: AttrValueType
  required_flag: boolean
  status_code?: 'ENABLED' | 'DISABLED'
}

export async function fetchAdminProducts(query: SpuListQuery): Promise<ApiPageData<AdminSpu>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_PRODUCTS_SPU
  return await get<ApiPageData<AdminSpu>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function createAdminProduct(payload: SaveSpuPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_PRODUCTS_SPU
  await post(endpoint.path, payload, { scope: 'admin' })
}

export async function updateAdminProduct(spuId: number, payload: SaveSpuPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_PRODUCTS_SPU_BY_SPU_ID
  await put(compilePath(endpoint.path, { spuId }), payload, { scope: 'admin' })
}

export async function deleteAdminProduct(spuId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.DELETE_ADMIN_PRODUCTS_SPU_BY_SPU_ID
  await del(compilePath(endpoint.path, { spuId }), { scope: 'admin' })
}

export async function updateAdminProductStatus(spuId: number, status: ProductStatus): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_PRODUCTS_SPU_BY_SPU_ID_STATUS
  await put(compilePath(endpoint.path, { spuId }), { status_code: status }, { scope: 'admin' })
}

export async function updateAdminProductRecommend(spuId: number, recommendFlag: boolean): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_PRODUCTS_SPU_BY_SPU_ID_RECOMMEND
  await put(compilePath(endpoint.path, { spuId }), { recommend_flag: recommendFlag }, { scope: 'admin' })
}

export async function fetchProductSkus(spuId: number): Promise<AdminSku[]> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_PRODUCTS_SPU_BY_SPU_ID_SKUS
  const data = await get<{ list?: AdminSku[] } | AdminSku[]>(compilePath(endpoint.path, { spuId }), { scope: 'admin' })
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export async function createProductSku(spuId: number, payload: SaveSkuPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_PRODUCTS_SPU_BY_SPU_ID_SKUS
  await post(compilePath(endpoint.path, { spuId }), payload, { scope: 'admin' })
}

export async function updateProductSku(skuId: number, payload: SaveSkuPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_PRODUCTS_SKUS_BY_SKU_ID
  await put(compilePath(endpoint.path, { skuId }), payload, { scope: 'admin' })
}

export async function adjustProductSkuStock(skuId: number, deltaStock: number): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_PRODUCTS_SKUS_BY_SKU_ID_STOCK
  await put(compilePath(endpoint.path, { skuId }), { delta_stock: deltaStock }, { scope: 'admin' })
}


export type AdminProductDetail = AdminSpu & {
  detail_html?: string
  media_list?: AdminProductMedia[]
}

export async function fetchAdminProductDetail(spuId: number): Promise<AdminProductDetail> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_PRODUCTS_SPU_BY_SPU_ID
  return await get<AdminProductDetail>(compilePath(endpoint.path, { spuId }), { scope: 'admin' })
}

export async function fetchProductMedia(spuId: number): Promise<AdminProductMedia[]> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_PRODUCTS_SPU_BY_SPU_ID
  const data = await get<{ media_list?: AdminProductMedia[]; list?: AdminProductMedia[] } | AdminProductMedia[]>(
    compilePath(endpoint.path, { spuId }),
    { scope: 'admin' },
  )
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.media_list)) return data.media_list
  return Array.isArray(data?.list) ? data.list : []
}

export async function createProductMedia(spuId: number, payload: SaveSpuMediaPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_PRODUCTS_SPU_BY_SPU_ID_MEDIA
  await post(compilePath(endpoint.path, { spuId }), payload, { scope: 'admin' })
}

export async function deleteProductMedia(mediaId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.DELETE_ADMIN_PRODUCTS_MEDIA_BY_MEDIA_ID
  await del(compilePath(endpoint.path, { mediaId }), { scope: 'admin' })
}

export async function fetchProductAttrDefs(query: AttrDefListQuery): Promise<ApiPageData<AdminAttrDef>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_PRODUCTS_ATTR_DEFS
  return await get<ApiPageData<AdminAttrDef>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function createProductAttrDef(payload: SaveAttrDefPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_PRODUCTS_ATTR_DEFS
  await post(endpoint.path, payload, { scope: 'admin' })
}

export async function updateProductAttrDef(attrDefId: number, payload: SaveAttrDefPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_PRODUCTS_ATTR_DEFS_BY_ATTR_DEF_ID
  await put(compilePath(endpoint.path, { attrDefId }), payload, { scope: 'admin' })
}

export async function deleteProductAttrDef(attrDefId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.DELETE_ADMIN_PRODUCTS_ATTR_DEFS_BY_ATTR_DEF_ID
  await del(compilePath(endpoint.path, { attrDefId }), { scope: 'admin' })
}
