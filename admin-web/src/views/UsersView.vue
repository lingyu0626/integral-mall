<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NDivider,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NModal,
  NSpace,
  NTag,
  useMessage,
  type DataTableColumns,
} from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import {
  adjustAdminUserPoints,
  fetchAdminUserPointLedger,
  fetchAdminUsers,
  restoreAdminUserPointAdjust,
  updateAdminUserFinance,
  updateAdminUserRemark,
  updateAdminUserStatus,
} from '../api/admin/users'
import type { AdminPointLedger, AdminUser } from '../mock/admin'

type PrizePreset = {
  name: string
  point: number
}

type UserPointLedgerRow = AdminPointLedger & {
  user_id?: number
  consume_amount?: number
  profit_change?: number
  restored_flag?: number | boolean
  restore_of_ledger_id?: number
  restored_at?: string
}

const PRESET_STORAGE_KEY = 'mall_user_point_prize_presets_v1'
const DEFAULT_PRESETS: PrizePreset[] = [
  { name: '风铃高达', point: 60 },
  { name: '自由高达', point: 100 },
  { name: '宝可梦', point: 30 },
]

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const tableData = ref<AdminUser[]>([])
const total = ref(0)
const listSortField = ref<'id' | 'point_balance' | 'total_consume_amount' | 'profit_amount' | ''>('')
const listSortOrder = ref<'ascend' | 'descend' | false>(false)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const pointModalVisible = ref(false)
const pointSearchLoading = ref(false)
const pointSearchKeyword = ref('')
const pointCandidates = ref<AdminUser[]>([])
const pointSelectedUserId = ref<number | null>(null)
const pointDelta = ref<number | null>(0)
const pointAdjustRemark = ref('')
const pointSubmitting = ref(false)
const drawUnitPrice = ref<number | null>(39.9)
const drawCount = ref<number | null>(1)
const consumeAmount = ref<number | null>(39.9)
const manualProfitAdjust = ref<number | null>(0)
const selectedPrizeName = ref('')
const prizePresets = ref<PrizePreset[]>([])
const presetEditText = ref('')

const remarkModalVisible = ref(false)
const remarkEditingUserId = ref<number | null>(null)
const remarkText = ref('')
const remarkSubmitting = ref(false)

const financeModalVisible = ref(false)
const financeEditingUser = ref<AdminUser | null>(null)
const financeTotalConsume = ref<number | null>(0)
const financeProfit = ref<number | null>(0)
const financeSubmitting = ref(false)

const pointHistoryModalVisible = ref(false)
const pointHistoryLoading = ref(false)
const pointHistoryUser = ref<AdminUser | null>(null)
const pointHistoryList = ref<UserPointLedgerRow[]>([])
const pointHistoryRestoringId = ref<number | null>(null)

function safeMoney(value: unknown): number {
  const num = Number(value ?? 0)
  if (!Number.isFinite(num)) return 0
  return Math.round(num * 100) / 100
}

function moneyText(value: unknown): string {
  return safeMoney(value).toFixed(2)
}

function formatBackpackId(value: unknown): string {
  const id = Number(value ?? 0)
  if (!Number.isFinite(id) || id <= 0) return '01000'
  return String(Math.floor(id)).padStart(5, '0')
}

function toBool(value: unknown): boolean {
  if (typeof value === 'boolean') return value
  const text = String(value ?? '').trim().toLowerCase()
  return text === '1' || text === 'true' || text === 'yes'
}

function pointBizTypeText(code: unknown): string {
  const key = String(code || '').trim().toUpperCase()
  if (!key) return '-'
  if (key === 'MANUAL_ADJUST') return '手动调整'
  if (key === 'MANUAL_ADJUST_RESTORE') return '恢复调整'
  if (key === 'EXCHANGE_ORDER') return '兑换扣减'
  if (key === 'ORDER_CANCEL_REFUND') return '取消订单返还'
  if (key === 'ORDER_REJECT_REFUND') return '驳回返还'
  if (key === 'ORDER_CLOSE_REFUND') return '关闭返还'
  if (key === 'ORDER_REFUND') return '订单返还'
  return key
}

function parsePrizePresetText(text: string): PrizePreset[] {
  return String(text || '')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [nameRaw, pointRaw] = line.split(':')
      const name = String(nameRaw || '').trim()
      const point = Number(String(pointRaw || '').trim())
      if (!name || !Number.isFinite(point)) return null
      return { name, point: Math.max(0, Math.round(point)) }
    })
    .filter((item): item is PrizePreset => !!item)
}

function formatPrizePresetText(list: PrizePreset[]): string {
  return list.map((item) => `${item.name}:${item.point}`).join('\n')
}

function loadPresetOptions() {
  try {
    const raw = localStorage.getItem(PRESET_STORAGE_KEY)
    if (!raw) {
      prizePresets.value = DEFAULT_PRESETS.slice()
      presetEditText.value = formatPrizePresetText(prizePresets.value)
      return
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) throw new Error('invalid')
    const list = parsed
      .map((item) => ({
        name: String(item?.name || '').trim(),
        point: Number(item?.point || 0),
      }))
      .filter((item) => item.name && Number.isFinite(item.point))
      .map((item) => ({ ...item, point: Math.max(0, Math.round(item.point)) }))
    prizePresets.value = list.length ? list : DEFAULT_PRESETS.slice()
  } catch {
    prizePresets.value = DEFAULT_PRESETS.slice()
  }
  presetEditText.value = formatPrizePresetText(prizePresets.value)
}

function savePresetOptions() {
  const list = parsePrizePresetText(presetEditText.value)
  if (!list.length) {
    message.warning('常用奖品不能为空')
    return
  }
  prizePresets.value = list
  presetEditText.value = formatPrizePresetText(list)
  localStorage.setItem(PRESET_STORAGE_KEY, JSON.stringify(list))
  message.success('常用奖品已保存')
}

function applyDrawCostFromUnitPrice() {
  const unit = safeMoney(drawUnitPrice.value ?? 0)
  const count = Math.max(0, Number(drawCount.value || 0))
  consumeAmount.value = safeMoney(unit * count)
}

const selectedPointUser = computed(() => {
  if (pointSelectedUserId.value === null) return null
  return pointCandidates.value.find((item) => item.id === pointSelectedUserId.value) ?? null
})

const pointCandidateColumns: DataTableColumns<AdminUser> = [
  { title: '背包ID', key: 'id', width: 110, render: (row) => row.backpack_id || formatBackpackId(row.id) },
  { title: '客户备注', key: 'admin_remark', minWidth: 180, render: (row) => row.admin_remark || '-' },
  { title: '手机号', key: 'phone_masked', minWidth: 120, render: (row) => row.phone || row.phone_masked || '-' },
  { title: '碎片余额', key: 'point_balance', width: 120, align: 'center' },
  { title: '总消费', key: 'total_consume_amount', width: 100, render: (row) => moneyText(row.total_consume_amount || 0) },
  { title: '盈亏', key: 'profit_amount', width: 100, render: (row) => moneyText(row.profit_amount || 0) },
  {
    title: '操作',
    key: 'actions',
    width: 90,
    render: (row) =>
      h(
        NButton,
        {
          size: 'small',
          tertiary: true,
          type: pointSelectedUserId.value === row.id ? 'primary' : 'default',
          onClick: () => {
            pointSelectedUserId.value = row.id
          },
        },
        { default: () => (pointSelectedUserId.value === row.id ? '已选择' : '选择') },
      ),
  },
]

const pointHistoryColumns: DataTableColumns<UserPointLedgerRow> = [
  { title: '时间', key: 'occurred_at', width: 165 },
  { title: '类型', key: 'biz_type_code', width: 110, render: (row) => pointBizTypeText(row.biz_type_code) },
  {
    title: '碎片变动',
    key: 'change_amount',
    width: 120,
    render: (row) => {
      const value = Number(row.change_amount || 0)
      return value >= 0 ? `+${value}` : String(value)
    },
  },
  { title: '消费变动', key: 'consume_amount', width: 110, render: (row) => moneyText(row.consume_amount || 0) },
  { title: '盈亏变动', key: 'profit_change', width: 110, render: (row) => moneyText(row.profit_change || 0) },
  { title: '备注', key: 'note', minWidth: 360, render: (row) => row.note || '-' },
  {
    title: '操作',
    key: 'actions',
    width: 108,
    render: (row) => {
      const canRestore = String(row.biz_type_code || '').toUpperCase() === 'MANUAL_ADJUST' && !toBool(row.restored_flag)
      return h(
        NButton,
        {
          size: 'small',
          tertiary: true,
          type: canRestore ? 'warning' : 'default',
          disabled: !canRestore || pointHistoryRestoringId.value === row.id,
          loading: pointHistoryRestoringId.value === row.id,
          onClick: () => void onRestorePointAdjust(row),
        },
        { default: () => (canRestore ? '恢复' : '已恢复') },
      )
    },
  },
]

function columnSortOrder(key: 'id' | 'point_balance' | 'total_consume_amount' | 'profit_amount') {
  return listSortField.value === key ? listSortOrder.value : false
}

const columns = computed<DataTableColumns<AdminUser>>(() => [
  {
    title: '背包ID',
    key: 'id',
    width: 120,
    sorter: 'default',
    sortOrder: columnSortOrder('id'),
    render: (row) => row.backpack_id || formatBackpackId(row.id),
  },
  { title: '客户备注', key: 'admin_remark', minWidth: 180, render: (row) => row.admin_remark || '-' },
  { title: '手机号', key: 'phone_masked', minWidth: 120, render: (row) => row.phone || row.phone_masked || '-' },
  {
    title: '碎片余额',
    key: 'point_balance',
    width: 120,
    align: 'center',
    sorter: 'default',
    sortOrder: columnSortOrder('point_balance'),
  },
  {
    title: '总消费',
    key: 'total_consume_amount',
    minWidth: 100,
    sorter: 'default',
    sortOrder: columnSortOrder('total_consume_amount'),
    render: (row) => moneyText(row.total_consume_amount || 0),
  },
  {
    title: '盈亏',
    key: 'profit_amount',
    minWidth: 100,
    sorter: 'default',
    sortOrder: columnSortOrder('profit_amount'),
    render: (row) => moneyText(row.profit_amount || 0),
  },
  { title: '订单数', key: 'order_count', width: 100, align: 'center' },
  {
    title: '状态',
    key: 'user_status_code',
    minWidth: 100,
    render: (row) =>
      h(
        NTag,
        { type: row.user_status_code === 'ACTIVE' ? 'success' : 'warning' },
        { default: () => (row.user_status_code === 'ACTIVE' ? '正常' : '已冻结') },
      ),
  },
  {
    title: '操作',
    key: 'actions',
    width: 510,
    render: (row) =>
      h(NSpace, { size: 8 }, {
        default: () => [
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: row.user_status_code === 'ACTIVE' ? 'warning' : 'primary',
              onClick: () => void onToggleStatus(row),
            },
            { default: () => (row.user_status_code === 'ACTIVE' ? '冻结' : '解冻') },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: 'success',
              onClick: () => void onAdjustPoint(row),
            },
            { default: () => '碎片编辑' },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: 'primary',
              onClick: () => onOpenFinanceModal(row),
            },
            { default: () => '财务编辑' },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: 'warning',
              onClick: () => void onOpenPointHistoryModal(row),
            },
            { default: () => '碎片调整记录' },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: 'info',
              onClick: () => onOpenRemarkModal(row),
            },
            { default: () => '客户备注' },
          ),
        ],
      }),
  },
])

async function loadUsers() {
  loading.value = true
  try {
    const data = await fetchAdminUsers({
      pageNo: query.value.pageNo,
      pageSize: query.value.pageSize,
      keyword: keyword.value.trim(),
      sortField: listSortField.value || undefined,
      sortOrder: listSortOrder.value || undefined,
    })
    tableData.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function onToggleStatus(row: AdminUser) {
  const nextStatus = row.user_status_code === 'ACTIVE' ? 'FROZEN' : 'ACTIVE'
  try {
    await updateAdminUserStatus(row.id, nextStatus)
    message.success(nextStatus === 'FROZEN' ? '已冻结' : '已解冻')
    await loadUsers()
  } catch (error: any) {
    message.error(error?.message || '状态更新失败')
  }
}

async function onAdjustPoint(row: AdminUser) {
  pointModalVisible.value = true
  pointSearchKeyword.value = String(row.id)
  pointCandidates.value = [row]
  pointSelectedUserId.value = row.id
  pointDelta.value = 0
  pointAdjustRemark.value = ''
  drawUnitPrice.value = 39.9
  drawCount.value = 0
  applyDrawCostFromUnitPrice()
  manualProfitAdjust.value = 0
  selectedPrizeName.value = ''
}

function onOpenRemarkModal(row: AdminUser) {
  remarkEditingUserId.value = row.id
  remarkText.value = row.admin_remark || ''
  remarkModalVisible.value = true
}

function onOpenFinanceModal(row: AdminUser) {
  financeEditingUser.value = row
  financeTotalConsume.value = safeMoney(row.total_consume_amount || 0)
  financeProfit.value = safeMoney(row.profit_amount || 0)
  financeModalVisible.value = true
}

async function loadUserPointHistory() {
  if (!pointHistoryUser.value) return
  pointHistoryLoading.value = true
  try {
    const page = await fetchAdminUserPointLedger(pointHistoryUser.value.id, {
      pageNo: 1,
      pageSize: 200,
    })
    pointHistoryList.value = page.list as UserPointLedgerRow[]
  } catch (error: any) {
    message.error(error?.message || '碎片调整记录加载失败')
    pointHistoryList.value = []
  } finally {
    pointHistoryLoading.value = false
  }
}

async function onOpenPointHistoryModal(row: AdminUser) {
  pointHistoryUser.value = row
  pointHistoryModalVisible.value = true
  await loadUserPointHistory()
}

async function onRestorePointAdjust(row: UserPointLedgerRow) {
  if (!pointHistoryUser.value) return
  const ledgerId = Number(row.id || 0)
  if (!ledgerId) return
  const ok = window.confirm(`确认恢复这条调整记录吗？\n记录ID：${ledgerId}`)
  if (!ok) return
  pointHistoryRestoringId.value = ledgerId
  try {
    await restoreAdminUserPointAdjust(pointHistoryUser.value.id, ledgerId)
    message.success('已恢复该调整记录')
    await Promise.all([loadUsers(), loadUserPointHistory()])
  } catch (error: any) {
    message.error(error?.message || '恢复失败')
  } finally {
    pointHistoryRestoringId.value = null
  }
}

async function onSubmitRemark() {
  if (remarkEditingUserId.value === null) return
  remarkSubmitting.value = true
  try {
    await updateAdminUserRemark(remarkEditingUserId.value, remarkText.value.trim())
    message.success('客户备注已保存')
    remarkModalVisible.value = false
    await loadUsers()
  } finally {
    remarkSubmitting.value = false
  }
}

async function onSubmitFinance() {
  if (!financeEditingUser.value) return
  financeSubmitting.value = true
  try {
    await updateAdminUserFinance(financeEditingUser.value.id, {
      total_consume_amount: safeMoney(financeTotalConsume.value || 0),
      profit_amount: safeMoney(financeProfit.value || 0),
    })
    message.success('用户财务数据已更新')
    financeModalVisible.value = false
    await loadUsers()
  } catch (error: any) {
    message.error(error?.message || '保存失败')
  } finally {
    financeSubmitting.value = false
  }
}

async function loadPointCandidatesByKeyword(rawKeyword = '') {
  pointSearchLoading.value = true
  try {
    const data = await fetchAdminUsers({
      pageNo: 1,
      pageSize: 10,
      keyword: String(rawKeyword || '').trim(),
    })
    pointCandidates.value = data.list
    if (
      pointSelectedUserId.value !== null &&
      !data.list.some((item) => item.id === pointSelectedUserId.value)
    ) {
      pointSelectedUserId.value = null
    }
  } finally {
    pointSearchLoading.value = false
  }
}

async function onOpenPointModal() {
  pointModalVisible.value = true
  pointSearchKeyword.value = ''
  pointSelectedUserId.value = null
  pointDelta.value = 0
  pointAdjustRemark.value = ''
  drawUnitPrice.value = 39.9
  drawCount.value = 0
  applyDrawCostFromUnitPrice()
  manualProfitAdjust.value = 0
  selectedPrizeName.value = ''
  await loadPointCandidatesByKeyword('')
}

function onChoosePrizePreset(preset: PrizePreset) {
  drawCount.value = Math.max(0, Number(drawCount.value || 0)) + 1
  applyDrawCostFromUnitPrice()
  pointDelta.value = Number(pointDelta.value || 0) + Number(preset.point || 0)
  selectedPrizeName.value = preset.name
  if (pointAdjustRemark.value.trim()) {
    pointAdjustRemark.value = `${pointAdjustRemark.value.trim()}；抽中${preset.name}+${preset.point}碎片`
  } else {
    pointAdjustRemark.value = `抽中${preset.name}+${preset.point}碎片`
  }
}

async function onSearchPointCandidates() {
  await loadPointCandidatesByKeyword(pointSearchKeyword.value)
  if (pointCandidates.value.length === 1) {
    pointSelectedUserId.value = pointCandidates.value[0].id
  }
}

async function onSubmitPointAdjust() {
  if (pointSelectedUserId.value === null) {
    message.warning('请先选择用户')
    return
  }
  const deltaValue = Number(pointDelta.value || 0)
  const manualProfit = safeMoney(manualProfitAdjust.value || 0)
  if (deltaValue === 0 && manualProfit === 0) {
    message.warning('调整碎片和手动盈亏至少填写一项')
    return
  }
  const drawTimes = Math.max(0, Number(drawCount.value || 0))
  const payload = {
    adjust_point: deltaValue,
    adjust_remark: pointAdjustRemark.value.trim(),
    draw_unit_price: safeMoney(drawUnitPrice.value || 0),
    consume_amount: safeMoney(consumeAmount.value || 0),
    manual_profit_adjust: manualProfit,
    prize_name: selectedPrizeName.value.trim(),
    draw_count: drawTimes,
  }
  pointSubmitting.value = true
  try {
    await adjustAdminUserPoints(pointSelectedUserId.value, payload)
    message.success(payload.adjust_point > 0 ? '碎片增加成功' : '碎片扣减成功')
    pointModalVisible.value = false
    await loadUsers()
  } finally {
    pointSubmitting.value = false
  }
}

function onUpdateListSorter(sorter: any) {
  const next = Array.isArray(sorter) ? sorter[0] : sorter
  const columnKey = String(next?.columnKey || '')
  const order = next?.order
  const allowColumns = ['id', 'point_balance', 'total_consume_amount', 'profit_amount']
  const validOrder = order === 'ascend' || order === 'descend'
  if (allowColumns.includes(columnKey) && validOrder) {
    listSortField.value = columnKey as 'id' | 'point_balance' | 'total_consume_amount' | 'profit_amount'
    listSortOrder.value = order
  } else {
    listSortField.value = ''
    listSortOrder.value = false
  }
  updatePage(1)
  void loadUsers()
}

function onSearch() {
  updatePage(1)
  void loadUsers()
}

function onChangePage(value: number) {
  updatePage(value)
  void loadUsers()
}

function onChangePageSize(value: number) {
  updatePageSize(value)
  void loadUsers()
}

onMounted(() => {
  loadPresetOptions()
  void loadUsers()
})
</script>

<template>
  <NCard title="用户管理">
    <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
      <NSpace>
        <NInput
          v-model:value="keyword"
          clearable
          placeholder="按用户ID/手机号/客户备注/订单备注搜索"
          style="width: 360px"
          @keyup.enter="onSearch"
        />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
      <NButton type="primary" tertiary @click="onOpenPointModal">碎片调整窗口</NButton>
    </NSpace>

    <NDataTable
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :pagination="false"
      remote
      @update:sorter="onUpdateListSorter"
    />

    <div style="margin-top: 14px">
      <AppPagination
        :page-no="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:page-no="onChangePage"
        @update:page-size="onChangePageSize"
      />
    </div>

    <NModal v-model:show="pointModalVisible" preset="card" title="碎片调整窗口" style="width: 1080px">
      <NSpace align="center" style="margin-bottom: 12px">
        <NInput
          v-model:value="pointSearchKeyword"
          clearable
          placeholder="输入用户ID / 手机号 / 客户备注 / 订单备注"
          style="width: 420px"
          @keyup.enter="onSearchPointCandidates"
        />
        <NButton type="primary" :loading="pointSearchLoading" @click="onSearchPointCandidates">搜索用户</NButton>
      </NSpace>

      <NDataTable
        :columns="pointCandidateColumns"
        :data="pointCandidates"
        :loading="pointSearchLoading"
        :pagination="false"
        max-height="240"
      />

      <NForm label-placement="left" label-width="120" style="margin-top: 14px">
        <NFormItem label="已选用户">
          <span>{{ selectedPointUser ? `背包ID:${selectedPointUser.backpack_id || formatBackpackId(selectedPointUser.id)} / ${selectedPointUser.phone || selectedPointUser.phone_masked || '-'}` : '未选择' }}</span>
        </NFormItem>

        <NFormItem label="抽奖单价">
          <NInputNumber v-model:value="drawUnitPrice" :min="0" :precision="2" style="width: 220px" @update:value="applyDrawCostFromUnitPrice" />
          <span style="margin-left: 10px; color: #8f96a3">例如：39.90</span>
        </NFormItem>

        <NFormItem label="抽奖次数">
          <NInputNumber v-model:value="drawCount" :min="0" :precision="0" style="width: 220px" @update:value="applyDrawCostFromUnitPrice" />
        </NFormItem>

        <NFormItem label="常用奖品">
          <NSpace>
            <NButton
              v-for="item in prizePresets"
              :key="item.name"
              tertiary
              type="info"
              @click="onChoosePrizePreset(item)"
            >
              {{ item.name }} +{{ item.point }}
            </NButton>
          </NSpace>
        </NFormItem>

        <NFormItem label="常用奖品配置">
          <NSpace vertical style="width: 100%">
            <NInput
              v-model:value="presetEditText"
              type="textarea"
              :autosize="{ minRows: 3, maxRows: 5 }"
              placeholder="每行一个：奖品名:碎片值，例如 风铃高达:60"
            />
            <NSpace>
              <NButton tertiary @click="savePresetOptions">保存常用奖品</NButton>
              <span style="color: #8f96a3">保存后本浏览器记住</span>
            </NSpace>
          </NSpace>
        </NFormItem>

        <NDivider />

        <NFormItem label="调整碎片" required>
          <NInputNumber v-model:value="pointDelta" :min="-99999999" :max="99999999" style="width: 220px" />
          <span style="margin-left: 10px; color: #8f96a3">正数=增加，负数=减少</span>
        </NFormItem>

        <NFormItem label="手动盈亏调整">
          <NInputNumber v-model:value="manualProfitAdjust" :min="-99999999" :max="99999999" :precision="2" style="width: 220px" />
          <span style="margin-left: 10px; color: #8f96a3">用于补偿优惠（可正可负）</span>
        </NFormItem>

        <NFormItem label="操作备注">
          <NInput
            v-model:value="pointAdjustRemark"
            maxlength="120"
            placeholder="例如：抽中自由高达+100碎片（选填）"
          />
        </NFormItem>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="pointModalVisible = false">取消</NButton>
          <NButton type="primary" :loading="pointSubmitting" @click="onSubmitPointAdjust">确认调整</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="pointHistoryModalVisible" preset="card" title="碎片调整记录" style="width: 1280px">
      <div style="margin-bottom: 12px; color: #4b5563">
        {{ pointHistoryUser ? `背包ID:${pointHistoryUser.backpack_id || formatBackpackId(pointHistoryUser.id)} / ${pointHistoryUser.phone || pointHistoryUser.phone_masked || '-'}` : '-' }}
      </div>
      <NDataTable
        :columns="pointHistoryColumns"
        :data="pointHistoryList"
        :loading="pointHistoryLoading"
        :pagination="false"
        max-height="560"
      />
      <template #footer>
        <NSpace justify="space-between" align="center">
          <span style="color: #8f96a3">仅“手动调整”可恢复，且每条仅允许恢复一次</span>
          <NSpace>
            <NButton :loading="pointHistoryLoading" @click="loadUserPointHistory">刷新</NButton>
            <NButton @click="pointHistoryModalVisible = false">关闭</NButton>
          </NSpace>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="remarkModalVisible" preset="card" title="编辑客户备注" style="width: 520px">
      <NForm label-placement="left" label-width="90">
        <NFormItem label="备注内容">
          <NInput
            v-model:value="remarkText"
            type="textarea"
            placeholder="请输入对顾客的内部备注"
            :autosize="{ minRows: 3, maxRows: 6 }"
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

    <NModal v-model:show="financeModalVisible" preset="card" title="编辑用户财务" style="width: 520px">
      <NForm label-placement="left" label-width="90">
        <NFormItem label="用户">
          <span>{{ financeEditingUser ? `背包ID:${financeEditingUser.backpack_id || formatBackpackId(financeEditingUser.id)} / ${financeEditingUser.phone || financeEditingUser.phone_masked || '-'}` : '-' }}</span>
        </NFormItem>
        <NFormItem label="总消费">
          <NInputNumber v-model:value="financeTotalConsume" :min="-99999999" :max="99999999" :precision="2" style="width: 220px" />
        </NFormItem>
        <NFormItem label="盈亏">
          <NInputNumber v-model:value="financeProfit" :min="-99999999" :max="99999999" :precision="2" style="width: 220px" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="financeModalVisible = false">取消</NButton>
          <NButton type="primary" :loading="financeSubmitting" @click="onSubmitFinance">保存</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>
