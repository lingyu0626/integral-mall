<script setup lang="ts">
import { h, onMounted, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NInput,
  NPopconfirm,
  NSelect,
  NSpace,
  NTag,
  useMessage,
  type DataTableColumns,
} from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import { fetchOutboxEvents, retryOutboxEvent, type OutboxEventListQuery } from '../api/admin/monitor'
import type { AdminOutboxEvent, OutboxEventStatus } from '../mock/monitor-center'

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const statusCode = ref<OutboxEventStatus | ''>('')
const rows = ref<AdminOutboxEvent[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待投递', value: 'PENDING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
]

const columns: DataTableColumns<AdminOutboxEvent> = [
  { title: '事件ID', key: 'id', width: 90 },
  { title: '事件类型', key: 'event_type_code', minWidth: 150 },
  { title: '聚合类型', key: 'aggregate_type_code', minWidth: 120 },
  { title: '聚合ID', key: 'aggregate_id', width: 90 },
  {
    title: '状态',
    key: 'status_code',
    width: 90,
    render: (row) => {
      const map: Record<OutboxEventStatus, { type: 'warning' | 'success' | 'error'; text: string }> = {
        PENDING: { type: 'warning', text: '待投递' },
        SUCCESS: { type: 'success', text: '成功' },
        FAILED: { type: 'error', text: '失败' },
      }
      const current = map[row.status_code]
      return h(NTag, { type: current.type, size: 'small' }, { default: () => current.text })
    },
  },
  { title: '重试次数', key: 'retry_count', width: 90 },
  { title: '下次重试', key: 'next_retry_at', minWidth: 160, render: (row) => row.next_retry_at || '-' },
  { title: '错误信息', key: 'last_error_msg', minWidth: 180, render: (row) => row.last_error_msg || '-' },
  { title: '创建时间', key: 'created_at', minWidth: 160 },
  { title: '更新时间', key: 'updated_at', minWidth: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 90,
    render: (row) =>
      h(
        NPopconfirm,
        {
          onPositiveClick: () => void onRetry(row.id),
        },
        {
          trigger: () =>
            h(
              NButton,
              {
                size: 'small',
                tertiary: true,
                type: 'primary',
                disabled: row.status_code === 'SUCCESS',
              },
              { default: () => '重试' },
            ),
          default: () => '确认重试投递该事件？',
        },
      ),
  },
]

async function loadList() {
  loading.value = true
  try {
    const payload: OutboxEventListQuery = {
      pageNo: query.value.pageNo,
      pageSize: query.value.pageSize,
      keyword: keyword.value.trim(),
      status_code: statusCode.value,
    }
    const data = await fetchOutboxEvents(payload)
    rows.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  updatePage(1)
  void loadList()
}

function onPageNoChange(value: number) {
  updatePage(value)
  void loadList()
}

function onPageSizeChange(value: number) {
  updatePageSize(value)
  void loadList()
}

async function onRetry(eventId: number) {
  await retryOutboxEvent(eventId)
  message.success('事件重试已触发')
  await loadList()
}

onMounted(() => {
  void loadList()
})
</script>

<template>
  <NCard title="事件外发表（D15）">
    <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按事件类型/聚合信息/错误信息搜索" style="width: 320px" @keyup.enter="onSearch" />
        <NSelect v-model:value="statusCode" :options="statusOptions" style="width: 130px" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
    </NSpace>

    <NDataTable :columns="columns" :data="rows" :loading="loading" :pagination="false" :scroll-x="1500" />

    <div style="margin-top: 14px">
      <AppPagination
        :page-no="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:page-no="onPageNoChange"
        @update:page-size="onPageSizeChange"
      />
    </div>
  </NCard>
</template>
