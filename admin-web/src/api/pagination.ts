import type { ApiPageData, PageQuery } from './types'

export const DEFAULT_PAGE_QUERY: PageQuery = {
  pageNo: 1,
  pageSize: 10,
}

export function normalizePageQuery(input?: Partial<PageQuery>): PageQuery {
  return {
    pageNo: input?.pageNo && input.pageNo > 0 ? input.pageNo : DEFAULT_PAGE_QUERY.pageNo,
    pageSize: input?.pageSize && input.pageSize > 0 ? input.pageSize : DEFAULT_PAGE_QUERY.pageSize,
  }
}

export function createEmptyPage<T>(): ApiPageData<T> {
  return {
    pageNo: DEFAULT_PAGE_QUERY.pageNo,
    pageSize: DEFAULT_PAGE_QUERY.pageSize,
    total: 0,
    list: [],
  }
}
