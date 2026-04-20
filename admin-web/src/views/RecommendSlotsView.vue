<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NImage,
  NInputNumber,
  NModal,
  NPopconfirm,
  NSelect,
  NSpace,
  NTag,
  NUpload,
  useMessage,
  type DataTableColumns,
  type UploadCustomRequestOptions,
  type UploadFileInfo,
} from 'naive-ui'
import {
  createRecommendItem,
  createRecommendSlot,
  deleteRecommendItem,
  fetchRecommendItems,
  fetchRecommendSlots,
  sortRecommendItems,
  updateRecommendItem,
  type SaveRecommendItemPayload,
} from '../api/admin/recommends'
import { fetchAdminProducts } from '../api/admin/products'
import { uploadAdminFile } from '../api/admin/files'
import type { AdminRecommendItem, AdminRecommendSlot } from '../mock/admin'
import type { AdminSpu } from '../mock/product-center'

type BannerFormModel = {
  banner_image_url: string
  sort_no: number | null
  status_code: 'ENABLED' | 'DISABLED'
}

type RecommendFormModel = {
  spu_id: number | null
  sort_no: number | null
  status_code: 'ENABLED' | 'DISABLED'
}

const HOME_BANNER_SLOT_CODE = 'HOME_BANNER'
const HOME_RECOMMEND_SLOT_CODE = 'HOME_RECOMMEND'
const MAX_HOME_RECOMMEND_COUNT = 14

const message = useMessage()

const bannerLoading = ref(false)
const bannerSaving = ref(false)
const bannerSortSaving = ref(false)
const bannerSortDirty = ref(false)
const uploadLoading = ref(false)
const uploadFileList = ref<UploadFileInfo[]>([])
const bannerSlot = ref<AdminRecommendSlot | null>(null)
const bannerRows = ref<AdminRecommendItem[]>([])

const bannerModalVisible = ref(false)
const bannerModalMode = ref<'create' | 'edit'>('create')
const editingBannerItemId = ref<number | null>(null)
const bannerForm = reactive<BannerFormModel>({
  banner_image_url: '',
  sort_no: 100,
  status_code: 'ENABLED',
})

const productLoading = ref(false)
const products = ref<AdminSpu[]>([])
const recommendSlot = ref<AdminRecommendSlot | null>(null)
const recommendRows = ref<AdminRecommendItem[]>([])
const recommendLoading = ref(false)
const recommendSortSaving = ref(false)
const recommendSortDirty = ref(false)
const recommendSaving = ref(false)
const recommendModalVisible = ref(false)
const recommendModalMode = ref<'create' | 'edit'>('create')
const editingRecommendItemId = ref<number | null>(null)
const recommendForm = reactive<RecommendFormModel>({
  spu_id: null,
  sort_no: 100,
  status_code: 'ENABLED',
})

const productOptions = ref<Array<{ label: string; value: number; point_price: number }>>([])

function resolveBannerUrl(item: Partial<AdminRecommendItem> | null | undefined) {
  return String(item?.banner_image_url || item?.image_url || '').trim()
}

function ensureSortNo(source: number, fallback = 100) {
  const value = Number(source || 0)
  return value > 0 ? value : fallback
}

const bannerColumns: DataTableColumns<AdminRecommendItem> = [
  { title: 'ID', key: 'id', width: 90 },
  {
    title: '轮播图',
    key: 'banner_image_url',
    minWidth: 220,
    render: (row) => {
      const url = resolveBannerUrl(row)
      return url
        ? h(NImage, {
            src: url,
            width: 120,
            height: 68,
            objectFit: 'cover',
            previewDisabled: false,
          })
        : h(NTag, { type: 'warning' }, { default: () => '未上传图片' })
    },
  },
  { title: '排序号', key: 'sort_no', width: 100 },
  {
    title: '状态',
    key: 'status_code',
    width: 100,
    render: (row) =>
      h(
        NTag,
        { type: row.status_code === 'ENABLED' ? 'success' : 'default' },
        { default: () => (row.status_code === 'ENABLED' ? '启用' : '禁用') },
      ),
  },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    render: (row) =>
      h(NSpace, { size: 6 }, {
        default: () => [
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => onMoveBannerItem(row, -1),
            },
            { default: () => '上移' },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => onMoveBannerItem(row, 1),
            },
            { default: () => '下移' },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => onOpenEditBanner(row),
            },
            { default: () => '编辑' },
          ),
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onDeleteBannerItem(row.id),
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
              default: () => '确认删除该轮播图？',
            },
          ),
        ],
      }),
  },
]

const recommendColumns: DataTableColumns<AdminRecommendItem> = [
  { title: '推荐ID', key: 'id', width: 90 },
  { title: '商品ID', key: 'spu_id', width: 100 },
  {
    title: '商品名称',
    key: 'product_name',
    minWidth: 220,
    render: (row) => row.product_name || '-',
  },
  { title: '碎片价', key: 'point_price', width: 100 },
  { title: '排序号', key: 'sort_no', width: 100 },
  {
    title: '状态',
    key: 'status_code',
    width: 90,
    render: (row) =>
      h(
        NTag,
        { type: row.status_code === 'ENABLED' ? 'success' : 'default', size: 'small' },
        { default: () => (row.status_code === 'ENABLED' ? '启用' : '禁用') },
      ),
  },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    render: (row) =>
      h(NSpace, { size: 6 }, {
        default: () => [
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => onMoveRecommendItem(row, -1),
            },
            { default: () => '上移' },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => onMoveRecommendItem(row, 1),
            },
            { default: () => '下移' },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => onOpenEditRecommend(row),
            },
            { default: () => '编辑' },
          ),
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onDeleteRecommendItem(row.id),
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
              default: () => '确认删除该推荐商品？',
            },
          ),
        ],
      }),
  },
]

function resetBannerForm() {
  bannerForm.banner_image_url = ''
  bannerForm.sort_no = 100
  bannerForm.status_code = 'ENABLED'
  uploadFileList.value = []
}

function resetRecommendForm() {
  recommendForm.spu_id = null
  recommendForm.sort_no = Math.max(100, (recommendRows.value.length + 1) * 10)
  recommendForm.status_code = 'ENABLED'
}

async function ensureHomeSlots() {
  let data = await fetchRecommendSlots({ pageNo: 1, pageSize: 200 })
  let list = data.list || []
  let banner = list.find((item) => String(item.slot_code || '').toUpperCase() === HOME_BANNER_SLOT_CODE)
  let recommend = list.find((item) => String(item.slot_code || '').toUpperCase() === HOME_RECOMMEND_SLOT_CODE)

  if (!banner) {
    await createRecommendSlot({
      slot_name: '首页轮播图',
      slot_code: HOME_BANNER_SLOT_CODE,
    })
  }
  if (!recommend) {
    await createRecommendSlot({
      slot_name: '首页推荐商品',
      slot_code: HOME_RECOMMEND_SLOT_CODE,
    })
  }

  if (!banner || !recommend) {
    data = await fetchRecommendSlots({ pageNo: 1, pageSize: 200 })
    list = data.list || []
    banner = list.find((item) => String(item.slot_code || '').toUpperCase() === HOME_BANNER_SLOT_CODE)
    recommend = list.find((item) => String(item.slot_code || '').toUpperCase() === HOME_RECOMMEND_SLOT_CODE)
  }

  bannerSlot.value = banner ?? null
  recommendSlot.value = recommend ?? null
}

async function loadProducts() {
  productLoading.value = true
  try {
    const data = await fetchAdminProducts({ pageNo: 1, pageSize: 500 })
    products.value = (data.list || []).filter((item) => item.status_code === 'ON_SHELF')
    productOptions.value = products.value.map((item) => ({
      label: `${item.spu_name}（ID:${item.id}）`,
      value: item.id,
      point_price: Number(item.point_price_min || 0),
    }))
  } finally {
    productLoading.value = false
  }
}

async function loadBannerItems() {
  if (!bannerSlot.value) {
    bannerRows.value = []
    return
  }
  bannerLoading.value = true
  try {
    const list = await fetchRecommendItems(bannerSlot.value.id)
    bannerRows.value = list
      .filter((item) => item.slot_id === bannerSlot.value?.id)
      .sort((a, b) => Number(b.sort_no || 0) - Number(a.sort_no || 0))
    bannerSortDirty.value = false
  } finally {
    bannerLoading.value = false
  }
}

async function loadRecommendItems() {
  if (!recommendSlot.value) {
    recommendRows.value = []
    return
  }
  recommendLoading.value = true
  try {
    const list = await fetchRecommendItems(recommendSlot.value.id)
    recommendRows.value = list
      .filter((item) => item.slot_id === recommendSlot.value?.id)
      .sort((a, b) => Number(b.sort_no || 0) - Number(a.sort_no || 0))
    recommendSortDirty.value = false
  } finally {
    recommendLoading.value = false
  }
}

function normalizeBannerSortByCurrentOrder() {
  const maxSort = bannerRows.value.length * 10
  bannerRows.value.forEach((row, index) => {
    row.sort_no = maxSort - index * 10
  })
}

function normalizeRecommendSortByCurrentOrder() {
  const maxSort = recommendRows.value.length * 10
  recommendRows.value.forEach((row, index) => {
    row.sort_no = maxSort - index * 10
  })
}

function onMoveBannerItem(row: AdminRecommendItem, direction: -1 | 1) {
  const index = bannerRows.value.findIndex((item) => item.id === row.id)
  if (index < 0) return
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= bannerRows.value.length) return

  const cloned = bannerRows.value.slice()
  const [current] = cloned.splice(index, 1)
  cloned.splice(nextIndex, 0, current)
  bannerRows.value = cloned
  normalizeBannerSortByCurrentOrder()
  bannerSortDirty.value = true
}

function onMoveRecommendItem(row: AdminRecommendItem, direction: -1 | 1) {
  const index = recommendRows.value.findIndex((item) => item.id === row.id)
  if (index < 0) return
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= recommendRows.value.length) return

  const cloned = recommendRows.value.slice()
  const [current] = cloned.splice(index, 1)
  cloned.splice(nextIndex, 0, current)
  recommendRows.value = cloned
  normalizeRecommendSortByCurrentOrder()
  recommendSortDirty.value = true
}

async function onSaveBannerSort() {
  if (!bannerSlot.value) return
  bannerSortSaving.value = true
  try {
    await sortRecommendItems(
      bannerSlot.value.id,
      bannerRows.value.map((item) => ({ item_id: item.id, sort_no: Number(item.sort_no || 0) })),
    )
    message.success('轮播图排序已保存')
    bannerSortDirty.value = false
    await loadBannerItems()
  } finally {
    bannerSortSaving.value = false
  }
}

async function onSaveRecommendSort() {
  if (!recommendSlot.value) return
  recommendSortSaving.value = true
  try {
    await sortRecommendItems(
      recommendSlot.value.id,
      recommendRows.value.map((item) => ({ item_id: item.id, sort_no: Number(item.sort_no || 0) })),
    )
    message.success('推荐商品排序已保存')
    recommendSortDirty.value = false
    await loadRecommendItems()
  } finally {
    recommendSortSaving.value = false
  }
}

function onOpenCreateBanner() {
  bannerModalMode.value = 'create'
  editingBannerItemId.value = null
  resetBannerForm()
  bannerModalVisible.value = true
}

function onOpenEditBanner(row: AdminRecommendItem) {
  bannerModalMode.value = 'edit'
  editingBannerItemId.value = row.id
  bannerForm.banner_image_url = resolveBannerUrl(row)
  bannerForm.sort_no = ensureSortNo(Number(row.sort_no || 100))
  bannerForm.status_code = row.status_code || 'ENABLED'
  uploadFileList.value = []
  bannerModalVisible.value = true
}

function onOpenCreateRecommend() {
  if (recommendRows.value.length >= MAX_HOME_RECOMMEND_COUNT) {
    message.warning(`首页推荐最多 ${MAX_HOME_RECOMMEND_COUNT} 个商品`)
    return
  }
  recommendModalMode.value = 'create'
  editingRecommendItemId.value = null
  resetRecommendForm()
  recommendModalVisible.value = true
}

function onOpenEditRecommend(row: AdminRecommendItem) {
  recommendModalMode.value = 'edit'
  editingRecommendItemId.value = row.id
  recommendForm.spu_id = Number(row.spu_id || 0) || null
  recommendForm.sort_no = ensureSortNo(Number(row.sort_no || 100))
  recommendForm.status_code = row.status_code || 'ENABLED'
  recommendModalVisible.value = true
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })
}

async function onUploadImage(options: UploadCustomRequestOptions) {
  const rawFile = options.file.file
  if (!(rawFile instanceof File)) {
    options.onError()
    message.error('请选择图片文件')
    return
  }
  if (!rawFile.type.startsWith('image/')) {
    options.onError()
    message.warning('仅支持图片文件')
    return
  }

  uploadLoading.value = true
  try {
    const dataUrl = await readFileAsDataUrl(rawFile)
    const file = await uploadAdminFile({
      file_name: rawFile.name,
      file_url: dataUrl,
      mime_type: rawFile.type || 'image/png',
      file_size_kb: Math.max(1, Math.round(rawFile.size / 1024)),
    })
    bannerForm.banner_image_url = String(file.file_url || '')
    options.onFinish()
    message.success('图片上传成功')
  } catch {
    options.onError()
    message.error('图片上传失败')
  } finally {
    uploadLoading.value = false
  }
}

function onRemoveImage() {
  bannerForm.banner_image_url = ''
  uploadFileList.value = []
  return true
}

function buildBannerPayload(): SaveRecommendItemPayload | null {
  const bannerUrl = bannerForm.banner_image_url.trim()
  if (!bannerUrl) return null
  const sortNo = Number(bannerForm.sort_no || 0)
  if (sortNo <= 0) return null

  return {
    spu_id: 0,
    product_name: '首页轮播图',
    point_price: 0,
    sort_no: sortNo,
    status_code: bannerForm.status_code,
    start_at: '2026-01-01 00:00:00',
    end_at: '2099-12-31 23:59:59',
    banner_image_url: bannerUrl,
    image_url: bannerUrl,
  }
}

function buildRecommendPayload(): SaveRecommendItemPayload | null {
  const spuId = Number(recommendForm.spu_id || 0)
  const sortNo = Number(recommendForm.sort_no || 0)
  if (spuId <= 0 || sortNo <= 0) return null
  const selected = productOptions.value.find((item) => item.value === spuId)
  if (!selected) return null

  return {
    spu_id: spuId,
    product_name: selected.label.replace(/（ID:\d+）$/, ''),
    point_price: Number(selected.point_price || 0),
    sort_no: sortNo,
    status_code: recommendForm.status_code,
    start_at: '2026-01-01 00:00:00',
    end_at: '2099-12-31 23:59:59',
  }
}

async function onSubmitBanner() {
  if (!bannerSlot.value) {
    message.warning('轮播位初始化失败，请刷新重试')
    return
  }
  const payload = buildBannerPayload()
  if (!payload) {
    message.warning('请先上传轮播图并填写正确排序号')
    return
  }

  bannerSaving.value = true
  try {
    if (bannerModalMode.value === 'create') {
      await createRecommendItem(bannerSlot.value.id, payload)
      message.success('轮播图已新增')
    } else if (editingBannerItemId.value !== null) {
      await updateRecommendItem(editingBannerItemId.value, payload)
      message.success('轮播图已更新')
    }
    bannerModalVisible.value = false
    await loadBannerItems()
  } finally {
    bannerSaving.value = false
  }
}

async function onSubmitRecommend() {
  if (!recommendSlot.value) {
    message.warning('首页推荐位初始化失败，请刷新重试')
    return
  }
  if (recommendModalMode.value === 'create' && recommendRows.value.length >= MAX_HOME_RECOMMEND_COUNT) {
    message.warning(`首页推荐最多 ${MAX_HOME_RECOMMEND_COUNT} 个商品`)
    return
  }
  const payload = buildRecommendPayload()
  if (!payload) {
    message.warning('请选择商品并填写正确排序号')
    return
  }
  recommendSaving.value = true
  try {
    if (recommendModalMode.value === 'create') {
      await createRecommendItem(recommendSlot.value.id, payload)
      message.success('推荐商品已新增')
    } else if (editingRecommendItemId.value !== null) {
      await updateRecommendItem(editingRecommendItemId.value, payload)
      message.success('推荐商品已更新')
    }
    recommendModalVisible.value = false
    await loadRecommendItems()
  } finally {
    recommendSaving.value = false
  }
}

async function onDeleteBannerItem(itemId: number) {
  await deleteRecommendItem(itemId)
  message.success('轮播图已删除')
  await loadBannerItems()
}

async function onDeleteRecommendItem(itemId: number) {
  await deleteRecommendItem(itemId)
  message.success('推荐商品已删除')
  await loadRecommendItems()
}

async function bootstrap() {
  await ensureHomeSlots()
  await Promise.all([loadProducts(), loadBannerItems(), loadRecommendItems()])
}

onMounted(() => {
  void bootstrap()
})
</script>

<template>
  <NSpace vertical :size="14">
    <NCard title="首页轮播图管理">
      <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
        <NSpace>
          <NButton :disabled="!bannerSortDirty" :loading="bannerSortSaving" @click="onSaveBannerSort">保存排序</NButton>
        </NSpace>
        <NButton type="primary" tertiary @click="onOpenCreateBanner">新增轮播图</NButton>
      </NSpace>

      <NDataTable
        :columns="bannerColumns"
        :data="bannerRows"
        :loading="bannerLoading"
        :pagination="false"
        :scroll-x="760"
        max-height="420"
      />

      <NModal
        v-model:show="bannerModalVisible"
        preset="card"
        :title="bannerModalMode === 'create' ? '新增轮播图' : '编辑轮播图'"
        style="width: 620px"
      >
        <NForm label-placement="left" label-width="96">
          <NFormItem label="轮播图片" required>
            <NSpace vertical>
              <NUpload
                v-model:file-list="uploadFileList"
                :max="1"
                accept="image/*"
                :custom-request="onUploadImage"
                :default-upload="true"
                @remove="onRemoveImage"
              >
                <NButton :loading="uploadLoading">上传图片</NButton>
              </NUpload>
              <NImage
                v-if="bannerForm.banner_image_url"
                :src="bannerForm.banner_image_url"
                width="180"
                height="102"
                object-fit="cover"
                preview-disabled
              />
            </NSpace>
          </NFormItem>

          <NFormItem label="排序号" required>
            <NInputNumber v-model:value="bannerForm.sort_no" :min="1" :max="9999" style="width: 220px" />
          </NFormItem>

          <NFormItem label="状态">
            <NSpace>
              <NButton
                size="small"
                :type="bannerForm.status_code === 'ENABLED' ? 'primary' : 'default'"
                @click="bannerForm.status_code = 'ENABLED'"
              >
                启用
              </NButton>
              <NButton
                size="small"
                :type="bannerForm.status_code === 'DISABLED' ? 'primary' : 'default'"
                @click="bannerForm.status_code = 'DISABLED'"
              >
                禁用
              </NButton>
            </NSpace>
          </NFormItem>
        </NForm>

        <template #footer>
          <NSpace justify="end">
            <NButton @click="bannerModalVisible = false">取消</NButton>
            <NButton type="primary" :loading="bannerSaving" @click="onSubmitBanner">保存</NButton>
          </NSpace>
        </template>
      </NModal>
    </NCard>

    <NCard title="首页推荐商品（固定14个商品位）">
      <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
        <NSpace>
          <NTag type="info">已配置 {{ recommendRows.length }}/{{ MAX_HOME_RECOMMEND_COUNT }}</NTag>
          <NButton :disabled="!recommendSortDirty" :loading="recommendSortSaving" @click="onSaveRecommendSort">保存排序</NButton>
        </NSpace>
        <NButton type="primary" tertiary :disabled="recommendRows.length >= MAX_HOME_RECOMMEND_COUNT" @click="onOpenCreateRecommend">
          新增推荐商品
        </NButton>
      </NSpace>

      <NDataTable
        :columns="recommendColumns"
        :data="recommendRows"
        :loading="recommendLoading"
        :pagination="false"
        :scroll-x="980"
        max-height="500"
      />

      <NModal
        v-model:show="recommendModalVisible"
        preset="card"
        :title="recommendModalMode === 'create' ? '新增首页推荐商品' : '编辑首页推荐商品'"
        style="width: 620px"
      >
        <NForm label-placement="left" label-width="106">
          <NFormItem label="选择商品" required>
            <NSelect
              v-model:value="recommendForm.spu_id"
              filterable
              clearable
              :loading="productLoading"
              :options="productOptions"
              placeholder="请选择上架商品"
            />
          </NFormItem>
          <NFormItem label="排序号" required>
            <NInputNumber v-model:value="recommendForm.sort_no" :min="1" :max="9999" style="width: 220px" />
          </NFormItem>
          <NFormItem label="状态">
            <NSpace>
              <NButton
                size="small"
                :type="recommendForm.status_code === 'ENABLED' ? 'primary' : 'default'"
                @click="recommendForm.status_code = 'ENABLED'"
              >
                启用
              </NButton>
              <NButton
                size="small"
                :type="recommendForm.status_code === 'DISABLED' ? 'primary' : 'default'"
                @click="recommendForm.status_code = 'DISABLED'"
              >
                禁用
              </NButton>
            </NSpace>
          </NFormItem>
        </NForm>
        <template #footer>
          <NSpace justify="end">
            <NButton @click="recommendModalVisible = false">取消</NButton>
            <NButton type="primary" :loading="recommendSaving" @click="onSubmitRecommend">保存</NButton>
          </NSpace>
        </template>
      </NModal>
    </NCard>
  </NSpace>
</template>
