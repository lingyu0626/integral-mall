<script setup lang="ts">
import { h, onMounted, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NDescriptions,
  NDescriptionsItem,
  NInput,
  NModal,
  NSelect,
  NSpace,
  NTag,
  useMessage,
  type DataTableColumns,
} from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import { fetchOperationLogDetail, fetchOperationLogs, type OperationLogListQuery } from '../api/admin/monitor'
import type { AdminOperationLog, AdminOperationLogStatus } from '../mock/monitor-center'

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const statusCode = ref<AdminOperationLogStatus | ''>('')
const rows = ref<AdminOperationLog[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const detailModalVisible = ref(false)
const detailLoading = ref(false)
const detailRow = ref<AdminOperationLog | null>(null)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
]

const columns: DataTableColumns<AdminOperationLog> = [
  { title: '日志ID', key: 'id', width: 90 },
  { title: '时间', key: 'occurred_at', width: 170 },
  { title: '模块', key: 'module_name', width: 100 },
  { title: '动作', key: 'action_name', width: 110 },
  { title: '操作人', key: 'operator_name', width: 100 },
  { title: 'IP', key: 'client_ip', width: 110 },
  {
    title: '状态',
    key: 'status_code',
    width: 90,
    render: (row) =>
      h(
        NTag,
        { type: row.status_code === 'SUCCESS' ? 'success' : 'error', size: 'small' },
        { default: () => (row.status_code === 'SUCCESS' ? '成功' : '失败') },
      ),
  },
  { title: '请求', key: 'request_path', minWidth: 260 },
  {
    title: '操作',
    key: 'actions',
    width: 80,
    render: (row) =>
      h(
        NButton,
        {
          size: 'small',
          tertiary: true,
          onClick: () => void onOpenDetail(row.id),
        },
        { default: () => '详情' },
      ),
  },
]

async function loadList() {
  loading.value = true
  try {
    const payload: OperationLogListQuery = {
      pageNo: query.value.pageNo,
      pageSize: query.value.pageSize,
      keyword: keyword.value.trim(),
      status_code: statusCode.value,
    }
    const data = await fetchOperationLogs(payload)
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

async function onOpenDetail(logId: number) {
  detailModalVisible.value = true
  detailLoading.value = true
  try {
    detailRow.value = await fetchOperationLogDetail(logId)
    if (!detailRow.value) {
      message.warning('日志不存在')
      detailModalVisible.value = false
    }
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  void loadList()
})
</script>

<template>
  <NCard title="操作日志（D15）">
    <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按模块/动作/路径/请求ID搜索" style="width: 300px" @keyup.enter="onSearch" />
        <NSelect v-model:value="statusCode" :options="statusOptions" style="width: 130px" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
    </NSpace>

    <NDataTable :columns="columns" :data="rows" :loading="loading" :pagination="false" :scroll-x="1300" />

    <div style="margin-top: 14px">
      <AppPagination
        :page-no="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:page-no="onPageNoChange"
        @update:page-size="onPageSizeChange"
      />
    </div>

    <NModal v-model:show="detailModalVisible" preset="card" title="日志详情" style="width: 760px">
      <NDescriptions v-if="detailRow" bordered :column="2" label-placement="left" size="small">
        <NDescriptionsItem label="日志ID">{{ detailRow.id }}</NDescriptionsItem>
        <NDescriptionsItem label="时间">{{ detailRow.occurred_at }}</NDescriptionsItem>
        <NDescriptionsItem label="模块">{{ detailRow.module_name }}</NDescriptionsItem>
        <NDescriptionsItem label="动作">{{ detailRow.action_name }}</NDescriptionsItem>
        <NDescriptionsItem label="操作人">{{ detailRow.operator_name }}</NDescriptionsItem>
        <NDescriptionsItem label="客户端IP">{{ detailRow.client_ip }}</NDescriptionsItem>
        <NDescriptionsItem label="请求方法">{{ detailRow.request_method }}</NDescriptionsItem>
        <NDescriptionsItem label="请求ID">{{ detailRow.request_id }}</NDescriptionsItem>
        <NDescriptionsItem label="请求路径" :span="2">{{ detailRow.request_path }}</NDescriptionsItem>
        <NDescriptionsItem label="状态">{{ detailRow.status_code }}</NDescriptionsItem>
        <NDescriptionsItem label="详情">{{ detailRow.detail_text }}</NDescriptionsItem>
      </NDescriptions>
      <div v-if="detailLoading" style="padding: 24px; text-align: center">加载中...</div>
    </NModal>
  </NCard>
</template>
