<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { NButton, NCard, NDataTable, NInput, NSelect, NSpace, useMessage, type DataTableColumns } from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import {
  downloadProcurementExportCsv,
  fetchProcurementExportRows,
  type ProcurementExportQuery,
  type ProcurementStatus,
} from '../api/admin/orders'
import {
  downloadUserBalanceReport,
  exportTodayUserBalanceReport,
  fetchUserBalanceReports,
  type UserBalanceReportItem,
} from '../api/admin/reports'
import type { AdminOrderStatus, ProcurementExportRow } from '../mock/orders-center'

const message = useMessage()
const loading = ref(false)
const exporting = ref(false)
const reportLoading = ref(false)
const reportExporting = ref(false)
const statusCode = ref<AdminOrderStatus | ''>('')
const procurementStatus = ref<ProcurementStatus | ''>('')
const submitDate = ref('')
const rows = ref<ProcurementExportRow[]>([])
const reportRows = ref<UserBalanceReportItem[]>([])
const exportPageNo = ref(1)
const exportPageSize = ref(10)
const reportPageNo = ref(1)
const reportPageSize = ref(10)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待审核', value: 'PENDING_AUDIT' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '待发货', value: 'PENDING_SHIP' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已完成', value: 'FINISHED' },
  { label: '已关闭', value: 'CLOSED' },
]

const columns: DataTableColumns<ProcurementExportRow> = [
  { title: '订单号', key: 'order_no', minWidth: 170 },
  { title: '用户ID', key: 'user_id', width: 100 },
  { title: '订单状态', key: 'order_status_text', width: 110, render: (row) => row.order_status_text || '-' },
  { title: '采购状态', key: 'procurement_status_text', width: 100, render: (row) => row.procurement_status_text || '待采购' },
  { title: '商品+SKU+数量', key: 'product_sku_qty', minWidth: 280, render: (row) => row.product_sku_qty || '-' },
  { title: '商品碎片价', key: 'point_price', width: 100, render: (row) => Number(row.point_price || 0) },
  { title: '用户备注', key: 'user_remark', minWidth: 180, render: (row) => row.user_remark || '-' },
  { title: '买家信息', key: 'buyer_full_info', minWidth: 320, render: (row) => row.buyer_full_info || '-' },
]

const procurementStatusOptions = [
  { label: '全部采购状态', value: '' },
  { label: '待采购', value: 'PENDING_PROCURE' },
  { label: '已采购', value: 'PROCURED' },
]

const reportColumns: DataTableColumns<UserBalanceReportItem> = [
  {
    title: '报表名称',
    key: 'file_name',
    minWidth: 260,
    render: (row) => formatReportFileName(row.file_name),
  },
  { title: '大小(KB)', key: 'file_size_kb', width: 100 },
  { title: '更新时间', key: 'updated_at', minWidth: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (row) =>
      h(
        NButton,
        {
          size: 'small',
          tertiary: true,
          type: 'primary',
          onClick: () => void onDownloadReport(row.file_name),
        },
        { default: () => '下载' },
      ),
  },
]

const pagedRows = computed(() => {
  const from = (exportPageNo.value - 1) * exportPageSize.value
  return rows.value.slice(from, from + exportPageSize.value)
})

const pagedReportRows = computed(() => {
  const from = (reportPageNo.value - 1) * reportPageSize.value
  return reportRows.value.slice(from, from + reportPageSize.value)
})

function formatReportFileName(fileName?: string) {
  const raw = String(fileName || '').trim()
  if (!raw) return '-'
  const matched = raw.match(/^user-balance-(\d{4})(\d{2})(\d{2})\.csv$/i)
  if (matched) {
    return `用户背包碎片日报-${matched[1]}-${matched[2]}-${matched[3]}.csv`
  }
  return raw
}

function buildQuery(): ProcurementExportQuery {
  return {
    status_code: statusCode.value || '',
    submit_date: submitDate.value.trim() || '',
    procurement_status: procurementStatus.value || '',
  }
}

async function loadRows() {
  loading.value = true
  try {
    rows.value = await fetchProcurementExportRows(buildQuery())
    exportPageNo.value = 1
  } finally {
    loading.value = false
  }
}

async function onExportCsv() {
  exporting.value = true
  try {
    await downloadProcurementExportCsv(buildQuery())
    message.success('导出成功')
  } finally {
    exporting.value = false
  }
}

async function loadReports() {
  reportLoading.value = true
  try {
    reportRows.value = await fetchUserBalanceReports()
    reportPageNo.value = 1
  } finally {
    reportLoading.value = false
  }
}

async function onManualExportReport() {
  reportExporting.value = true
  try {
    await exportTodayUserBalanceReport()
    message.success('已触发今日导出')
    await loadReports()
  } finally {
    reportExporting.value = false
  }
}

async function onDownloadReport(fileName: string) {
  await downloadUserBalanceReport(fileName)
}

function onChangeExportPageNo(value: number) {
  exportPageNo.value = value
}

function onChangeExportPageSize(value: number) {
  exportPageSize.value = value
  exportPageNo.value = 1
}

function onChangeReportPageNo(value: number) {
  reportPageNo.value = value
}

function onChangeReportPageSize(value: number) {
  reportPageSize.value = value
  reportPageNo.value = 1
}

onMounted(async () => {
  await Promise.all([loadRows(), loadReports()])
})
</script>

<template>
  <NSpace vertical :size="14">
    <NCard title="订单采购导出">
      <NSpace style="margin-bottom: 12px" align="center">
        <NSelect v-model:value="statusCode" :options="statusOptions" style="width: 160px" />
        <NSelect v-model:value="procurementStatus" :options="procurementStatusOptions" style="width: 160px" />
        <NInput v-model:value="submitDate" placeholder="提交日期：YYYY-MM-DD" style="width: 180px" />
        <NButton type="primary" @click="loadRows">查询</NButton>
        <NButton :loading="exporting" type="success" @click="onExportCsv">导出CSV</NButton>
      </NSpace>
      <NDataTable :columns="columns" :data="pagedRows" :loading="loading" :pagination="false" :scroll-x="1600" />
      <div style="margin-top: 14px">
        <AppPagination
          :page-no="exportPageNo"
          :page-size="exportPageSize"
          :total="rows.length"
          @update:page-no="onChangeExportPageNo"
          @update:page-size="onChangeExportPageSize"
        />
      </div>
    </NCard>

    <NCard title="用户背包碎片日报（每天12:00自动导出）">
      <NSpace style="margin-bottom: 12px" align="center">
        <NButton type="primary" :loading="reportExporting" @click="onManualExportReport">手动导出今天</NButton>
        <NButton tertiary @click="loadReports">刷新列表</NButton>
      </NSpace>
      <NDataTable :columns="reportColumns" :data="pagedReportRows" :loading="reportLoading" :pagination="false" :scroll-x="700" />
      <div style="margin-top: 14px">
        <AppPagination
          :page-no="reportPageNo"
          :page-size="reportPageSize"
          :total="reportRows.length"
          @update:page-no="onChangeReportPageNo"
          @update:page-size="onChangeReportPageSize"
        />
      </div>
    </NCard>
  </NSpace>
</template>
