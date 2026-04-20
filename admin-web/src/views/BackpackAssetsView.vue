<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
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
  expireBackpackAsset,
  fetchBackpackAssetFlows,
  fetchBackpackAssets,
  grantBackpackAsset,
  invalidateBackpackAsset,
  type GrantAssetPayload,
} from '../api/admin/backpack'
import type { AdminBackpackAsset, AdminBackpackAssetFlow, BackpackAssetStatus, BackpackAssetType } from '../mock/backpack-group'

type GrantFormModel = {
  user_name: string
  asset_name: string
  asset_type_code: BackpackAssetType
  expire_at: string
}

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const statusCode = ref<BackpackAssetStatus | ''>('')
const rows = ref<AdminBackpackAsset[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const grantModalVisible = ref(false)
const grantSaving = ref(false)
const grantForm = reactive<GrantFormModel>({
  user_name: '',
  asset_name: '',
  asset_type_code: 'GROUP_QR',
  expire_at: '2026-12-31 23:59:59',
})

const flowModalVisible = ref(false)
const flowRows = ref<AdminBackpackAssetFlow[]>([])
const currentAssetName = ref('')
const flowLoading = ref(false)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '有效', value: 'ACTIVE' },
  { label: '已使用', value: 'USED' },
  { label: '已过期', value: 'EXPIRED' },
  { label: '已失效', value: 'INVALID' },
]

const assetTypeOptions = [
  { label: '群二维码', value: 'GROUP_QR' },
  { label: '优惠券', value: 'COUPON' },
  { label: '实物权益', value: 'PHYSICAL' },
]

const columns: DataTableColumns<AdminBackpackAsset> = [
  { title: '资产ID', key: 'id', width: 88 },
  { title: '资产编号', key: 'asset_no', minWidth: 140 },
  { title: '用户', key: 'user_name', width: 120 },
  { title: '资产名称', key: 'asset_name', minWidth: 150 },
  {
    title: '类型',
    key: 'asset_type_code',
    width: 90,
    render: (row) => {
      if (row.asset_type_code === 'GROUP_QR') return '群二维码'
      if (row.asset_type_code === 'COUPON') return '优惠券'
      return '实物权益'
    },
  },
  {
    title: '状态',
    key: 'status_code',
    width: 90,
    render: (row) => {
      const map: Record<BackpackAssetStatus, { text: string; type: 'success' | 'default' | 'warning' | 'error' }> = {
        ACTIVE: { text: '有效', type: 'success' },
        USED: { text: '已使用', type: 'default' },
        EXPIRED: { text: '已过期', type: 'warning' },
        INVALID: { text: '已失效', type: 'error' },
      }
      return h(NTag, { type: map[row.status_code].type }, { default: () => map[row.status_code].text })
    },
  },
  { title: '获取时间', key: 'obtained_at', minWidth: 160 },
  { title: '过期时间', key: 'expire_at', minWidth: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    render: (row) => {
      const nodes = [
        h(
          NButton,
          {
            size: 'small',
            tertiary: true,
            onClick: () => void onOpenFlows(row),
          },
          { default: () => '流水' },
        ),
      ]
      if (row.status_code === 'ACTIVE') {
        nodes.push(
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onInvalidate(row.id),
            },
            {
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', tertiary: true, type: 'warning' },
                  { default: () => '失效' },
                ),
              default: () => '确认将该资产标记为失效？',
            },
          ),
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onExpire(row.id),
            },
            {
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', tertiary: true, type: 'error' },
                  { default: () => '过期' },
                ),
              default: () => '确认将该资产标记为过期？',
            },
          ),
        )
      }
      return h(NSpace, { size: 8 }, { default: () => nodes })
    },
  },
]

const flowColumns: DataTableColumns<AdminBackpackAssetFlow> = [
  { title: '时间', key: 'occurred_at', width: 170 },
  { title: '动作', key: 'action_text', width: 100 },
  { title: '类型', key: 'action_type_code', width: 120 },
  { title: '操作人', key: 'operator_name', width: 100 },
  { title: '备注', key: 'note', minWidth: 220 },
]

function resetGrantForm() {
  grantForm.user_name = ''
  grantForm.asset_name = ''
  grantForm.asset_type_code = 'GROUP_QR'
  grantForm.expire_at = '2026-12-31 23:59:59'
}

function buildGrantPayload(): GrantAssetPayload | null {
  const userName = grantForm.user_name.trim()
  const assetName = grantForm.asset_name.trim()
  const expireAt = grantForm.expire_at.trim()
  if (!userName || !assetName || !expireAt) return null
  return {
    user_name: userName,
    asset_name: assetName,
    asset_type_code: grantForm.asset_type_code,
    expire_at: expireAt,
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await fetchBackpackAssets({
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

function onOpenGrantModal() {
  resetGrantForm()
  grantModalVisible.value = true
}

async function onSubmitGrant() {
  const payload = buildGrantPayload()
  if (!payload) {
    message.warning('请完整填写发放信息')
    return
  }
  grantSaving.value = true
  try {
    await grantBackpackAsset(payload)
    message.success('资产已发放')
    grantModalVisible.value = false
    await loadList()
  } finally {
    grantSaving.value = false
  }
}

async function onInvalidate(assetId: number) {
  await invalidateBackpackAsset(assetId, '后台手工失效')
  message.success('资产已失效')
  await loadList()
}

async function onExpire(assetId: number) {
  await expireBackpackAsset(assetId, '后台手工过期')
  message.success('资产已过期')
  await loadList()
}

async function onOpenFlows(row: AdminBackpackAsset) {
  currentAssetName.value = row.asset_name
  flowModalVisible.value = true
  flowLoading.value = true
  try {
    flowRows.value = await fetchBackpackAssetFlows(row.id)
  } finally {
    flowLoading.value = false
  }
}

onMounted(() => {
  void loadList()
})
</script>

<template>
  <NCard title="背包资产管理（D13）">
    <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按资产编号/用户/名称搜索" style="width: 280px" @keyup.enter="onSearch" />
        <NSelect v-model:value="statusCode" :options="statusOptions" style="width: 140px" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
      <NButton tertiary type="primary" @click="onOpenGrantModal">人工发放</NButton>
    </NSpace>

    <NDataTable :columns="columns" :data="rows" :loading="loading" :pagination="false" :scroll-x="1320" />

    <div style="margin-top: 14px">
      <AppPagination
        :page-no="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:page-no="onPageNoChange"
        @update:page-size="onPageSizeChange"
      />
    </div>

    <NModal v-model:show="grantModalVisible" preset="card" title="人工发放资产" style="width: 560px">
      <NForm label-placement="left" label-width="96">
        <NFormItem label="用户名称" required>
          <NInput v-model:value="grantForm.user_name" placeholder="请输入用户名称" />
        </NFormItem>
        <NFormItem label="资产名称" required>
          <NInput v-model:value="grantForm.asset_name" placeholder="请输入资产名称" />
        </NFormItem>
        <NFormItem label="资产类型">
          <NSelect v-model:value="grantForm.asset_type_code" :options="assetTypeOptions" />
        </NFormItem>
        <NFormItem label="过期时间" required>
          <NInput v-model:value="grantForm.expire_at" placeholder="yyyy-MM-dd HH:mm:ss" />
        </NFormItem>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="grantModalVisible = false">取消</NButton>
          <NButton type="primary" :loading="grantSaving" @click="onSubmitGrant">确认发放</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="flowModalVisible" preset="card" :title="`资产流水 · ${currentAssetName}`" style="width: 860px">
      <NDataTable :columns="flowColumns" :data="flowRows" :loading="flowLoading" :pagination="false" :scroll-x="780" max-height="460" />
    </NModal>
  </NCard>
</template>
