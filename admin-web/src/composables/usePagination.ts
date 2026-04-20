import { computed, ref } from 'vue'
import { DEFAULT_PAGE_QUERY } from '../api/pagination'

export function usePagination(initial = DEFAULT_PAGE_QUERY) {
  const pageNo = ref(initial.pageNo)
  const pageSize = ref(initial.pageSize)

  const query = computed(() => ({
    pageNo: pageNo.value,
    pageSize: pageSize.value,
  }))

  function updatePage(nextPageNo: number) {
    pageNo.value = nextPageNo
  }

  function updatePageSize(nextPageSize: number) {
    pageSize.value = nextPageSize
    pageNo.value = 1
  }

  function reset() {
    pageNo.value = DEFAULT_PAGE_QUERY.pageNo
    pageSize.value = DEFAULT_PAGE_QUERY.pageSize
  }

  return {
    pageNo,
    pageSize,
    query,
    updatePage,
    updatePageSize,
    reset,
  }
}
