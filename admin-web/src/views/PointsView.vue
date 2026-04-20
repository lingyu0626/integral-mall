<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { NCard, NDataTable, type DataTableColumns } from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import { fetchAdminPointLedger } from '../api/admin/users'
import type { AdminPointLedger } from '../mock/admin'

const loading = ref(false)
const tableData = ref<AdminPointLedger[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const columns: DataTableColumns<AdminPointLedger> = [
  { title: '流水ID', key: 'id', width: 120 },
  { title: '用户', key: 'user_name', minWidth: 130 },
  { title: '业务类型', key: 'biz_type_code', minWidth: 150 },
  { title: '变动碎片', key: 'change_amount', minWidth: 110 },
  { title: '余额', key: 'balance_after', minWidth: 100 },
  { title: '备注', key: 'note', minWidth: 180, render: (row) => row.note || '-' },
  { title: '时间', key: 'occurred_at', minWidth: 170 },
]

async function loadPointLedger() {
  loading.value = true
  try {
    const data = await fetchAdminPointLedger({
      pageNo: query.value.pageNo,
      pageSize: query.value.pageSize,
    })
    tableData.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onChangePage(value: number) {
  updatePage(value)
  void loadPointLedger()
}

function onChangePageSize(value: number) {
  updatePageSize(value)
  void loadPointLedger()
}

onMounted(() => {
  void loadPointLedger()
})
</script>

<template>
  <NCard title="碎片中心 / 全局流水">
    <NDataTable :columns="columns" :data="tableData" :loading="loading" :pagination="false" />
    <div style="margin-top: 14px">
      <AppPagination
        :page-no="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:page-no="onChangePage"
        @update:page-size="onChangePageSize"
      />
    </div>
  </NCard>
</template>
