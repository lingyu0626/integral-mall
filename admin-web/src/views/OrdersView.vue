<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NCheckbox,
  NDataTable,
  NDescriptions,
  NDescriptionsItem,
  NEmpty,
  NForm,
  NFormItem,
  NInput,
  NModal,
  NPopconfirm,
  NSelect,
  NSpace,
  NSpin,
  NTag,
  useMessage,
  type DataTableColumns,
} from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import {
  approveAdminOrder,
  closeAdminOrder,
  completeAdminOrder,
  detectAdminLogistics,
  deleteAdminOrder,
  fetchAdminOrderDelivery,
  fetchAdminOrderDetail,
  fetchAdminOrderFlows,
  fetchAdminOrderItems,
  fetchAdminOrders,
  rejectAdminOrder,
  shipAdminOrder,
  updateAdminOrderProcurement,
  updateAdminOrderRemark,
  type LogisticsDetectItem,
  type OrderListQuery,
} from '../api/admin/orders'
import type { AdminOrderDelivery, AdminOrderFlow, AdminOrderListItem, AdminOrderStatus, AdminOrderItem } from '../mock/orders-center'

type StatusMeta = {
  type: 'warning' | 'info' | 'primary' | 'success' | 'error' | 'default'
  text: string
}

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const statusCode = ref<AdminOrderStatus | ''>('')
const statusCountMap = ref<Record<string, number>>({})
const rows = ref<AdminOrderListItem[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()
const checkedOrderIds = ref<number[]>([])

const detailModalVisible = ref(false)
const detailLoading = ref(false)
const detailOrder = ref<AdminOrderListItem | null>(null)
const detailItems = ref<AdminOrderItem[]>([])
const detailFlows = ref<AdminOrderFlow[]>([])
const detailDelivery = ref<AdminOrderDelivery | null>(null)

const rejectModalVisible = ref(false)
const rejectOrderId = ref<number | null>(null)
const rejectForm = reactive({
  reject_reason: '',
  refund_point: true,
})
const rejectSubmitting = ref(false)

const shipModalVisible = ref(false)
const shipOrderId = ref<number | null>(null)
const shipForm = reactive({
  express_company: '',
  express_no: '',
  shipper_code: '',
})
const shipperCandidates = ref<LogisticsDetectItem[]>([])
const shipperDetecting = ref(false)
const shipSubmitting = ref(false)

const closeModalVisible = ref(false)
const closeOrderId = ref<number | null>(null)
const closeReason = ref('')
const closeMode = ref<'REJECT' | 'CLOSE'>('CLOSE')
const closeSubmitting = ref(false)

const remarkModalVisible = ref(false)
const remarkOrderId = ref<number | null>(null)
const remarkText = ref('')
const remarkSubmitting = ref(false)

const statusTabs: Array<{ label: string; value: AdminOrderStatus | '' }> = [
  { label: '全部订单', value: '' },
  { label: '待审核', value: 'PENDING_AUDIT' },
  { label: '待发货', value: 'PENDING_SHIP' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已完成', value: 'FINISHED' },
]

const countStatusKeys: Array<AdminOrderStatus | ''> = ['', 'PENDING_AUDIT', 'PENDING_SHIP', 'SHIPPED']

const flowStatusTextMap: Record<string, string> = {
  INIT: '初始状态',
  PENDING_AUDIT: '待审核',
  WAIT_AUDIT: '待审核',
  REJECTED: '已驳回',
  PENDING_SHIP: '待发货',
  WAIT_SHIP: '待发货',
  SHIPPED: '已发货',
  DELIVERED: '已送达',
  FINISHED: '已完成',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
  CANCELED: '已取消',
}

function mapFlowStatusText(status?: string) {
  const key = String(status || '').trim().toUpperCase()
  if (!key) return '-'
  return flowStatusTextMap[key] || key
}

function resolveStatusMeta(status: AdminOrderStatus): StatusMeta {
  if (status === 'PENDING_AUDIT') return { type: 'warning', text: '待审核' }
  if (status === 'REJECTED') return { type: 'error', text: '已驳回' }
  if (status === 'PENDING_SHIP') return { type: 'info', text: '待发货' }
  if (status === 'SHIPPED') return { type: 'primary', text: '已发货' }
  if (status === 'FINISHED') return { type: 'success', text: '已完成' }
  return { type: 'default', text: '已关闭' }
}

function resolveProcurementMeta(status?: string): StatusMeta {
  if (String(status || '').toUpperCase() === 'PROCURED') return { type: 'success', text: '已采购' }
  return { type: 'warning', text: '待采购' }
}

const itemColumns: DataTableColumns<AdminOrderItem> = [
  { title: '商品名称', key: 'spu_name', minWidth: 170 },
  { title: 'SKU', key: 'sku_name', minWidth: 170 },
  { title: '单价', key: 'point_price', width: 80 },
  { title: '数量', key: 'quantity', width: 70 },
  { title: '小计', key: 'total_point_amount', width: 90 },
]

const flowColumns: DataTableColumns<AdminOrderFlow> = [
  { title: '时间', key: 'occurred_at', width: 170 },
  { title: '动作', key: 'action_text', width: 100 },
  {
    title: '状态流转',
    key: 'from_status',
    width: 180,
    render: (row) => `${mapFlowStatusText(row.from_status)} → ${mapFlowStatusText(row.to_status)}`,
  },
  { title: '操作人', key: 'operator_name', width: 100 },
  { title: '备注', key: 'note', minWidth: 200 },
]

const columns: DataTableColumns<AdminOrderListItem> = [
  {
    type: 'selection',
    width: 46,
  } as any,
  {
    title: '用户ID',
    key: 'user_id',
    minWidth: 90,
    render: (row) => row.user_id ?? '-',
  },
  {
    title: '买家信息',
    key: 'buyer_display',
    minWidth: 170,
    render: (row) => row.buyer_display || '-',
  },
  {
    title: '商品信息',
    key: 'goods_summary',
    minWidth: 290,
    render: (row) => row.goods_summary || '-',
  },
  {
    title: '状态',
    key: 'order_status_code',
    minWidth: 100,
    render: (row) => {
      const meta = resolveStatusMeta(row.order_status_code)
      return h(NTag, { type: meta.type }, { default: () => meta.text })
    },
  },
  {
    title: '采购状态',
    key: 'procurement_status',
    minWidth: 100,
    render: (row) => {
      const meta = resolveProcurementMeta((row as any).procurement_status)
      return h(NTag, { type: meta.type }, { default: () => meta.text })
    },
  },
  { title: '碎片', key: 'total_point_amount', minWidth: 90 },
  { title: '备注', key: 'remark', minWidth: 130, render: (row) => row.remark || '-' },
  {
    title: '操作',
    key: 'actions',
    width: 580,
    render: (row) => {
      const nodes = [
        h(
          NButton,
          {
            size: 'small',
            tertiary: true,
            onClick: () => void onOpenDetail(row.id),
          },
          { default: () => '详情' },
        ),
        h(
          NButton,
          {
            size: 'small',
            tertiary: true,
            onClick: () => onOpenRemarkModal(row),
          },
          { default: () => '备注' },
        ),
      ]

      if (row.order_status_code === 'PENDING_AUDIT') {
        nodes.push(
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onApprove(row),
            },
            {
              trigger: () =>
                h(
                  NButton,
                  {
                    size: 'small',
                    tertiary: true,
                    type: 'success',
                  },
                  { default: () => '通过' },
                ),
              default: () => '确认审核通过该订单？',
            },
          ),
        )
      }

      if (row.order_status_code === 'PENDING_SHIP') {
        nodes.push(
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: 'info',
              onClick: () => onOpenShipModal(row.id),
            },
            { default: () => '发货' },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: String((row as any).procurement_status || '').toUpperCase() === 'PROCURED' ? 'success' : 'warning',
              onClick: () => void onUpdateProcurement(row),
            },
            { default: () => (String((row as any).procurement_status || '').toUpperCase() === 'PROCURED' ? '已采购' : '待采购') },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: 'warning',
              onClick: () => onOpenRejectModal(row.id),
            },
            { default: () => '驳回' },
          ),
        )
      }

      if (row.order_status_code === 'SHIPPED') {
        nodes.push(
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onComplete(row),
            },
            {
              trigger: () =>
                h(
                  NButton,
                  {
                    size: 'small',
                    tertiary: true,
                    type: 'primary',
                  },
                  { default: () => '完成' },
                ),
              default: () => '确认将订单置为已完成？',
            },
          ),
        )
      }

      if (['PENDING_AUDIT', 'PENDING_SHIP', 'SHIPPED'].includes(row.order_status_code)) {
        const isPendingAudit = row.order_status_code === 'PENDING_AUDIT'
        nodes.push(
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: isPendingAudit ? 'warning' : 'error',
              onClick: () => onOpenCloseModal(row.id, isPendingAudit ? 'REJECT' : 'CLOSE'),
            },
            { default: () => (isPendingAudit ? '拒绝' : '关闭') },
          ),
        )
      }

      if (row.order_status_code === 'CLOSED') {
        nodes.push(
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onDelete(row),
            },
            {
              trigger: () =>
                h(
                  NButton,
                  {
                    size: 'small',
                    tertiary: true,
                    type: 'error',
                  },
                  { default: () => '删除' },
                ),
              default: () => '仅用于清理已关闭订单，确认删除？',
            },
          ),
        )
      }

      return h(NSpace, { size: 6 }, { default: () => nodes })
    },
  },
]

function onSelectStatusTab(value: AdminOrderStatus | '') {
  if (statusCode.value === value) return
  statusCode.value = value
  updatePage(1)
  void loadOrders()
}

async function loadOrders() {
  loading.value = true
  try {
    const queryPayload: OrderListQuery = {
      pageNo: query.value.pageNo,
      pageSize: query.value.pageSize,
      keyword: keyword.value.trim(),
      status_code: statusCode.value,
    }
    const data = await fetchAdminOrders(queryPayload)
    rows.value = data.list
    total.value = data.total
    checkedOrderIds.value = []
    void loadStatusTabCounts()
  } finally {
    loading.value = false
  }
}

async function loadStatusTabCounts() {
  try {
    const pairs = await Promise.all(
      countStatusKeys.map(async (key) => {
        const page = await fetchAdminOrders({
          pageNo: 1,
          pageSize: 1,
          status_code: key,
        })
        return [key || 'ALL', Number(page.total || 0)] as const
      }),
    )
    const map: Record<string, number> = {}
    for (const [key, value] of pairs) {
      map[key] = value
    }
    statusCountMap.value = map
  } catch {
    // ignore count errors
  }
}

function resolveTabCount(tab: { label: string; value: AdminOrderStatus | '' }) {
  const key = tab.value || 'ALL'
  if (!['PENDING_AUDIT', 'PENDING_SHIP', 'SHIPPED'].includes(key)) return 0
  return Number(statusCountMap.value[key] || 0)
}

const selectedRows = computed(() => {
  const idSet = new Set(checkedOrderIds.value)
  return rows.value.filter((item) => idSet.has(item.id))
})

const selectedCount = computed(() => selectedRows.value.length)

function onUpdateCheckedRowKeys(keys: Array<number | string>) {
  checkedOrderIds.value = keys.map((item) => Number(item)).filter((item) => Number.isFinite(item))
}

async function onBatchApprove() {
  const list = selectedRows.value.filter((item) => item.order_status_code === 'PENDING_AUDIT')
  if (!list.length) {
    message.warning('请先选择待审核订单')
    return
  }
  let ok = 0
  let fail = 0
  for (const row of list) {
    try {
      await approveAdminOrder(row.id, '批量审核通过')
      ok += 1
    } catch {
      fail += 1
    }
  }
  checkedOrderIds.value = []
  await loadOrders()
  message.success(`批量通过完成：成功${ok}，失败${fail}`)
}

async function onBatchMarkProcured() {
  const list = selectedRows.value.filter((item) => ['PENDING_SHIP', 'SHIPPED'].includes(item.order_status_code))
  if (!list.length) {
    message.warning('请先选择待发货/已发货订单')
    return
  }
  let ok = 0
  let fail = 0
  for (const row of list) {
    try {
      await updateAdminOrderProcurement(row.id, 'PROCURED')
      ok += 1
    } catch {
      fail += 1
    }
  }
  checkedOrderIds.value = []
  await loadOrders()
  message.success(`批量采购标记完成：成功${ok}，失败${fail}`)
}

async function loadDetail(orderId: number) {
  detailLoading.value = true
  try {
    const [order, items, flows, delivery] = await Promise.all([
      fetchAdminOrderDetail(orderId),
      fetchAdminOrderItems(orderId),
      fetchAdminOrderFlows(orderId),
      fetchAdminOrderDelivery(orderId),
    ])
    detailOrder.value = order
    detailItems.value = items
    detailFlows.value = flows
    detailDelivery.value = delivery
  } finally {
    detailLoading.value = false
  }
}

async function reloadDetailIfCurrent(orderId: number) {
  if (detailModalVisible.value && detailOrder.value?.id === orderId) {
    await loadDetail(orderId)
  }
}

function onSearch() {
  updatePage(1)
  void loadOrders()
}

function onPageNoChange(value: number) {
  updatePage(value)
  void loadOrders()
}

function onPageSizeChange(value: number) {
  updatePageSize(value)
  void loadOrders()
}

async function onOpenDetail(orderId: number) {
  detailModalVisible.value = true
  await loadDetail(orderId)
}

async function onApprove(row: AdminOrderListItem) {
  await approveAdminOrder(row.id, '审核通过')
  message.success('订单已审核通过')
  await Promise.all([loadOrders(), reloadDetailIfCurrent(row.id)])
}

async function onDelete(row: AdminOrderListItem) {
  await deleteAdminOrder(row.id)
  message.success('订单已删除')
  if (detailModalVisible.value && detailOrder.value?.id === row.id) {
    detailModalVisible.value = false
  }
  await loadOrders()
}

function onOpenRejectModal(orderId: number) {
  rejectOrderId.value = orderId
  rejectForm.reject_reason = ''
  rejectForm.refund_point = false
  rejectModalVisible.value = true
}

async function onSubmitReject() {
  if (rejectOrderId.value === null) return
  const reason = rejectForm.reject_reason.trim()
  if (!reason) {
    message.warning('请填写驳回原因')
    return
  }
  rejectSubmitting.value = true
  try {
    await rejectAdminOrder(rejectOrderId.value, {
      reject_reason: reason,
      refund_point: rejectForm.refund_point,
    })
    message.success('订单已驳回')
    const currentId = rejectOrderId.value
    rejectModalVisible.value = false
    await Promise.all([loadOrders(), reloadDetailIfCurrent(currentId)])
  } finally {
    rejectSubmitting.value = false
  }
}

function onOpenShipModal(orderId: number) {
  shipOrderId.value = orderId
  shipForm.express_company = ''
  shipForm.express_no = ''
  shipForm.shipper_code = ''
  shipperCandidates.value = []
  shipModalVisible.value = true
}

const shipperOptions = computed(() =>
  shipperCandidates.value.map((item) => ({
    label: `${item.express_company}（${item.shipper_code}${item.score ? `，命中${item.score}` : ''}）`,
    value: item.shipper_code,
  })),
)

function onSelectShipper(value: string | null) {
  const code = String(value || '')
  shipForm.shipper_code = code
  const target = shipperCandidates.value.find((item) => item.shipper_code === code)
  if (target) {
    shipForm.express_company = target.express_company
  }
}

async function onDetectShipper(showToast = false) {
  const no = shipForm.express_no.trim()
  if (!no) return
  shipperDetecting.value = true
  try {
    const list = await detectAdminLogistics(no)
    shipperCandidates.value = list
    if (list.length > 0) {
      shipForm.shipper_code = list[0].shipper_code
      shipForm.express_company = list[0].express_company
      if (showToast) message.success(`已识别：${list[0].express_company}`)
    } else if (showToast) {
      message.warning('未识别到物流公司，请手动填写')
    }
  } catch (error: any) {
    if (showToast) message.warning(error?.message || '自动识别失败，请手动填写')
  } finally {
    shipperDetecting.value = false
  }
}

async function onSubmitShip() {
  if (shipOrderId.value === null) return
  if (!shipForm.express_company.trim() && shipForm.express_no.trim()) {
    await onDetectShipper(false)
  }
  const company = shipForm.express_company.trim()
  const no = shipForm.express_no.trim()
  if (!no) {
    message.warning('请填写快递单号')
    return
  }
  if (!company) {
    message.warning('请填写或识别快递公司')
    return
  }
  shipSubmitting.value = true
  try {
    await shipAdminOrder(shipOrderId.value, {
      express_company: company,
      express_no: no,
      shipper_code: shipForm.shipper_code.trim() || undefined,
    })
    message.success('订单已发货')
    const currentId = shipOrderId.value
    shipModalVisible.value = false
    await Promise.all([loadOrders(), reloadDetailIfCurrent(currentId)])
  } finally {
    shipSubmitting.value = false
  }
}

async function onComplete(row: AdminOrderListItem) {
  await completeAdminOrder(row.id)
  message.success('订单已完成')
  await Promise.all([loadOrders(), reloadDetailIfCurrent(row.id)])
}

async function onUpdateProcurement(row: AdminOrderListItem) {
  const current = String((row as any).procurement_status || '').toUpperCase()
  const target = current === 'PROCURED' ? 'PENDING_PROCURE' : 'PROCURED'
  await updateAdminOrderProcurement(row.id, target as any)
  message.success(target === 'PROCURED' ? '已标记为已采购' : '已恢复为待采购')
  await Promise.all([loadOrders(), reloadDetailIfCurrent(row.id)])
}

function onOpenCloseModal(orderId: number, mode: 'REJECT' | 'CLOSE' = 'CLOSE') {
  closeOrderId.value = orderId
  closeMode.value = mode
  closeReason.value = ''
  closeModalVisible.value = true
}

async function onSubmitClose() {
  if (closeOrderId.value === null) return
  closeSubmitting.value = true
  try {
    await closeAdminOrder(closeOrderId.value, closeReason.value.trim())
    message.success(closeMode.value === 'REJECT' ? '订单已拒绝' : '订单已关闭')
    const currentId = closeOrderId.value
    closeModalVisible.value = false
    await Promise.all([loadOrders(), reloadDetailIfCurrent(currentId)])
  } finally {
    closeSubmitting.value = false
  }
}

function onOpenRemarkModal(row: AdminOrderListItem) {
  remarkOrderId.value = row.id
  remarkText.value = row.remark
  remarkModalVisible.value = true
}

async function onSubmitRemark() {
  if (remarkOrderId.value === null) return
  remarkSubmitting.value = true
  try {
    await updateAdminOrderRemark(remarkOrderId.value, remarkText.value.trim())
    message.success('备注已更新')
    const currentId = remarkOrderId.value
    remarkModalVisible.value = false
    await Promise.all([loadOrders(), reloadDetailIfCurrent(currentId)])
  } finally {
    remarkSubmitting.value = false
  }
}

onMounted(() => {
  void loadOrders()
})
</script>

<template>
  <NCard title="订单中心（D12）">
    <NSpace style="margin-bottom: 10px" justify="space-between" align="center">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按订单号/用户ID/订单备注搜索" style="width: 300px" @keyup.enter="onSearch" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
      <NSpace>
        <NTag type="info">已选 {{ selectedCount }} 笔</NTag>
        <NButton tertiary type="success" :disabled="selectedCount === 0" @click="onBatchApprove">批量通过</NButton>
        <NButton tertiary type="warning" :disabled="selectedCount === 0" @click="onBatchMarkProcured">批量设为已采购</NButton>
      </NSpace>
    </NSpace>

    <NSpace style="margin-bottom: 12px" size="small" wrap>
      <NButton
        v-for="tab in statusTabs"
        :key="tab.value || 'ALL'"
        :type="statusCode === tab.value ? 'primary' : 'default'"
        :tertiary="statusCode !== tab.value"
        size="small"
        @click="onSelectStatusTab(tab.value)"
      >
        {{ tab.label }}<template v-if="resolveTabCount(tab) > 0">（{{ resolveTabCount(tab) }}）</template>
      </NButton>
    </NSpace>

    <NDataTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      :pagination="false"
      :scroll-x="1600"
      :row-key="(row) => row.id"
      :checked-row-keys="checkedOrderIds"
      @update:checked-row-keys="onUpdateCheckedRowKeys"
    />

    <div style="margin-top: 14px">
      <AppPagination
        :page-no="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:page-no="onPageNoChange"
        @update:page-size="onPageSizeChange"
      />
    </div>

    <NModal v-model:show="detailModalVisible" preset="card" title="订单详情" style="width: 980px">
      <NSpin :show="detailLoading">
        <template v-if="detailOrder">
          <NDescriptions bordered label-placement="left" :column="3" size="small" style="margin-bottom: 12px">
            <NDescriptionsItem label="订单号">{{ detailOrder.order_no }}</NDescriptionsItem>
            <NDescriptionsItem label="用户ID">{{ detailOrder.user_id || '-' }}</NDescriptionsItem>
            <NDescriptionsItem label="状态">{{ resolveStatusMeta(detailOrder.order_status_code).text }}</NDescriptionsItem>
            <NDescriptionsItem label="碎片">{{ detailOrder.total_point_amount }}</NDescriptionsItem>
            <NDescriptionsItem label="提交时间">{{ detailOrder.submit_at }}</NDescriptionsItem>
            <NDescriptionsItem label="备注">{{ detailOrder.remark || '-' }}</NDescriptionsItem>
          </NDescriptions>

          <NCard size="small" title="订单明细" style="margin-bottom: 12px">
            <NDataTable :columns="itemColumns" :data="detailItems" :pagination="false" />
          </NCard>

          <NCard size="small" title="物流信息" style="margin-bottom: 12px">
            <template v-if="detailDelivery">
              <NDescriptions bordered label-placement="left" :column="2" size="small">
                <NDescriptionsItem label="收件人">{{ detailDelivery.receiver_name }}</NDescriptionsItem>
                <NDescriptionsItem label="手机号">{{ detailDelivery.receiver_phone }}</NDescriptionsItem>
                <NDescriptionsItem label="收货地址" :span="2">{{ detailDelivery.receiver_address }}</NDescriptionsItem>
                <NDescriptionsItem label="快递公司">{{ detailDelivery.express_company || '-' }}</NDescriptionsItem>
                <NDescriptionsItem label="快递单号">{{ detailDelivery.express_no || '-' }}</NDescriptionsItem>
                <NDescriptionsItem label="物流状态">{{ detailDelivery.delivery_status_text || '-' }}</NDescriptionsItem>
                <NDescriptionsItem label="签收时间">{{ detailDelivery.signed_at || '-' }}</NDescriptionsItem>
                <NDescriptionsItem label="发货时间" :span="2">{{ detailDelivery.ship_at || '-' }}</NDescriptionsItem>
              </NDescriptions>
            </template>
            <NEmpty v-else description="暂无物流信息" />
          </NCard>

          <NCard size="small" title="状态流转记录">
            <NDataTable :columns="flowColumns" :data="detailFlows" :pagination="false" :scroll-x="860" max-height="260" />
          </NCard>
        </template>
      </NSpin>
    </NModal>

    <NModal v-model:show="rejectModalVisible" preset="card" title="订单驳回（待发货）" style="width: 520px">
      <NForm label-placement="left" label-width="90">
        <NFormItem label="驳回原因" required>
          <NInput
            v-model:value="rejectForm.reject_reason"
            type="textarea"
            placeholder="请输入驳回原因"
            :autosize="{ minRows: 3, maxRows: 5 }"
          />
        </NFormItem>
        <NFormItem label="碎片回补">
          <NCheckbox v-model:checked="rejectForm.refund_point">立即回补碎片（不让买家选择）</NCheckbox>
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="rejectModalVisible = false">取消</NButton>
          <NButton type="warning" :loading="rejectSubmitting" @click="onSubmitReject">确认驳回</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="shipModalVisible" preset="card" title="订单发货" style="width: 520px">
      <NForm label-placement="left" label-width="90">
        <NFormItem label="快递单号" required>
          <NSpace style="width: 100%" align="center" :wrap="false">
            <NInput v-model:value="shipForm.express_no" placeholder="请输入快递单号" @blur="onDetectShipper(false)" />
            <NButton tertiary type="primary" :loading="shipperDetecting" @click="onDetectShipper(true)">自动识别</NButton>
          </NSpace>
        </NFormItem>
        <NFormItem label="物流公司" required>
          <NSpace vertical style="width: 100%">
            <NSelect
              :value="shipForm.shipper_code || null"
              :options="shipperOptions"
              clearable
              placeholder="识别后可切换物流商（可选）"
              @update:value="onSelectShipper"
            />
            <NInput v-model:value="shipForm.express_company" placeholder="物流公司（识别失败可手填）" />
          </NSpace>
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="shipModalVisible = false">取消</NButton>
          <NButton type="info" :loading="shipSubmitting" @click="onSubmitShip">确认发货</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal
      v-model:show="closeModalVisible"
      preset="card"
      :title="closeMode === 'REJECT' ? '拒绝订单' : '关闭订单'"
      style="width: 520px"
    >
      <NForm label-placement="left" label-width="90">
        <NFormItem :label="closeMode === 'REJECT' ? '拒绝原因' : '关闭原因'">
          <NInput
            v-model:value="closeReason"
            type="textarea"
            :placeholder="closeMode === 'REJECT' ? '请输入拒绝原因（可选）' : '请输入关闭原因（可选）'"
            :autosize="{ minRows: 3, maxRows: 5 }"
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="closeModalVisible = false">取消</NButton>
          <NButton :type="closeMode === 'REJECT' ? 'warning' : 'error'" :loading="closeSubmitting" @click="onSubmitClose">
            {{ closeMode === 'REJECT' ? '确认拒绝' : '确认关闭' }}
          </NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="remarkModalVisible" preset="card" title="订单备注" style="width: 520px">
      <NForm label-placement="left" label-width="90">
        <NFormItem label="备注内容">
          <NInput
            v-model:value="remarkText"
            type="textarea"
            placeholder="请输入订单备注"
            :autosize="{ minRows: 3, maxRows: 5 }"
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="remarkModalVisible = false">取消</NButton>
          <NButton type="primary" :loading="remarkSubmitting" @click="onSubmitRemark">保存备注</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>
