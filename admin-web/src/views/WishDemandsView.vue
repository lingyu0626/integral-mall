<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NImage,
  NInput,
  NModal,
  NPopconfirm,
  NSelect,
  NSpace,
  NTag,
  useMessage,
  type DataTableColumns,
} from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import {
  decideAdminWishDemand,
  fetchAdminWishDemands,
  type AdminWishDemand,
  type WishDemandStatus,
} from '../api/admin/wish-demands'

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const statusCode = ref<WishDemandStatus | ''>('')
const rows = ref<AdminWishDemand[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const rejectModalVisible = ref(false)
const rejectSubmitting = ref(false)
const rejectForm = reactive({
  id: 0,
  decision_note: '',
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '已确认', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
]

function statusMeta(status?: string) {
  const key = String(status || '').toUpperCase()
  if (key === 'APPROVED') return { type: 'success' as const, text: '已确认' }
  if (key === 'REJECTED') return { type: 'error' as const, text: '已拒绝' }
  return { type: 'warning' as const, text: '待处理' }
}

const columns: DataTableColumns<AdminWishDemand> = [
  { title: 'ID', key: 'id', width: 90 },
  { title: '用户ID', key: 'user_id', width: 90 },
  {
    title: '买家',
    key: 'user_name',
    minWidth: 140,
    render: (row) => `${row.user_name || '-'} ${row.phone_masked || ''}`.trim(),
  },
  { title: '商品名称', key: 'wish_title', minWidth: 220 },
  { title: '留言', key: 'wish_message', minWidth: 220, render: (row) => row.wish_message || '-' },
  {
    title: '图片',
    key: 'image_url',
    width: 110,
    render: (row) =>
      row.image_url
        ? h(NImage, {
            src: row.image_url,
            width: 54,
            height: 54,
            objectFit: 'cover',
            previewDisabled: false,
          })
        : '-',
  },
  {
    title: '状态',
    key: 'status_code',
    width: 90,
    render: (row) => {
      const meta = statusMeta(row.status_code)
      return h(NTag, { type: meta.type }, { default: () => meta.text })
    },
  },
  { title: '处理备注', key: 'decision_note', minWidth: 180, render: (row) => row.decision_note || '-' },
  { title: '提交时间', key: 'created_at', width: 165 },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render: (row) => {
      if (row.status_code !== 'PENDING') {
        return '-'
      }
      return h(NSpace, { size: 6 }, {
        default: () => [
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onApprove(row.id),
            },
            {
              trigger: () => h(NButton, { size: 'small', tertiary: true, type: 'success' }, { default: () => '确认' }),
              default: () => '确认通过该意向留言？',
            },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: 'error',
              onClick: () => onOpenReject(row.id),
            },
            { default: () => '拒绝' },
          ),
        ],
      })
    },
  },
]

async function loadRows() {
  loading.value = true
  try {
    const data = await fetchAdminWishDemands({
      pageNo: query.value.pageNo,
      pageSize: query.value.pageSize,
      keyword: keyword.value.trim(),
      status_code: statusCode.value,
    })
    rows.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function onApprove(id: number) {
  await decideAdminWishDemand(id, { decision: 'APPROVED' })
  message.success('已确认')
  await loadRows()
}

function onOpenReject(id: number) {
  rejectForm.id = id
  rejectForm.decision_note = ''
  rejectModalVisible.value = true
}

async function onSubmitReject() {
  if (!rejectForm.id) return
  rejectSubmitting.value = true
  try {
    await decideAdminWishDemand(rejectForm.id, {
      decision: 'REJECTED',
      decision_note: rejectForm.decision_note.trim(),
    })
    message.success('已拒绝')
    rejectModalVisible.value = false
    await loadRows()
  } finally {
    rejectSubmitting.value = false
  }
}

function onSearch() {
  updatePage(1)
  void loadRows()
}

function onPageNoChange(value: number) {
  updatePage(value)
  void loadRows()
}

function onPageSizeChange(value: number) {
  updatePageSize(value)
  void loadRows()
}

onMounted(() => {
  void loadRows()
})
</script>

<template>
  <NCard title="意向商品留言">
    <NSpace style="margin-bottom: 12px" align="center" justify="space-between">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按用户ID/商品名/留言搜索" style="width: 280px" @keyup.enter="onSearch" />
        <NSelect v-model:value="statusCode" :options="statusOptions" style="width: 140px" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
    </NSpace>

    <NDataTable :columns="columns" :data="rows" :loading="loading" :pagination="false" :scroll-x="1600" />

    <div style="margin-top: 14px">
      <AppPagination
        :page-no="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:page-no="onPageNoChange"
        @update:page-size="onPageSizeChange"
      />
    </div>

    <NModal v-model:show="rejectModalVisible" preset="card" title="拒绝备注" style="width: 520px">
      <NForm label-placement="left" label-width="86">
        <NFormItem label="拒绝原因">
          <NInput
            v-model:value="rejectForm.decision_note"
            type="textarea"
            placeholder="填写拒绝原因（可选）"
            :autosize="{ minRows: 3, maxRows: 5 }"
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="rejectModalVisible = false">取消</NButton>
          <NButton type="error" :loading="rejectSubmitting" @click="onSubmitReject">确认拒绝</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>
