export type ApiSuccessCode = 0

export type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

export type ApiPageData<T> = {
  pageNo: number
  pageSize: number
  total: number
  list: T[]
}

export type PageQuery = {
  pageNo: number
  pageSize: number
}

export type RequestScope = 'admin' | 'app'
