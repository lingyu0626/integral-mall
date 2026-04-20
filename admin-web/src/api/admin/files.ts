import { API_ENDPOINTS } from '../endpoints'
import { del, get, post } from '../http'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type { AdminUploadedFile } from '../../mock/platform-center'

export type FileListQuery = Partial<PageQuery> & {
  keyword?: string
}

export type UploadFilePayload = {
  file_name: string
  file_url: string
  mime_type: string
  file_size_kb: number
}

export async function fetchAdminFiles(query: FileListQuery): Promise<ApiPageData<AdminUploadedFile>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_FILES_BY_FILE_ID
  // 规范接口清单中无 /admin/files 列表接口，联调阶段临时约定走详情接口所在模块列表能力
  // 如果后端已提供 /admin/files 列表接口，请将 endpoint.path 替换为该路径。
  return await get<ApiPageData<AdminUploadedFile>>(endpoint.path.replace('/{fileId}', ''), {
    scope: 'admin',
    params: query,
  })
}

export async function uploadAdminFile(payload: UploadFilePayload): Promise<AdminUploadedFile> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_FILES_UPLOAD
  const data = await post<AdminUploadedFile | { file?: AdminUploadedFile }>(endpoint.path, payload, {
    scope: 'admin',
  })
  if ('file' in data && data.file) return data.file
  return data as AdminUploadedFile
}

export async function fetchAdminFileDetail(fileId: number): Promise<AdminUploadedFile> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_FILES_BY_FILE_ID
  return await get<AdminUploadedFile>(compilePath(endpoint.path, { fileId }), { scope: 'admin' })
}

export async function deleteAdminFile(fileId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.DELETE_ADMIN_FILES_BY_FILE_ID
  await del(compilePath(endpoint.path, { fileId }), { scope: 'admin' })
}
