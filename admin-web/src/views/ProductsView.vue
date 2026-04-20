<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NGrid,
  NGridItem,
  NImage,
  NInput,
  NInputNumber,
  NUpload,
  NModal,
  NPopconfirm,
  NSelect,
  NSpace,
  NTag,
  useMessage,
  type DataTableColumns,
  type UploadCustomRequestOptions,
  type UploadFileInfo,
} from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import {
  adjustProductSkuStock,
  createAdminProduct,
  createProductMedia,
  createProductSku,
  deleteAdminProduct,
  deleteProductMedia,
  fetchAdminProductDetail,
  fetchAdminProducts,
  fetchProductMedia,
  fetchProductSkus,
  updateAdminProduct,
  updateAdminProductStatus,
  updateProductSku,
  type SaveSkuPayload,
  type SaveSpuMediaPayload,
  type SaveSpuPayload,
} from '../api/admin/products'
import { fetchAdminCategories } from '../api/admin/categories'
import { uploadAdminFile } from '../api/admin/files'
import type { AdminProductMedia, AdminSku, AdminSpu, ProductStatus, SkuStatus } from '../mock/product-center'

type SpuFormModel = {
  spu_name: string
  category_names: string[]
  detail_html: string
  image_urls: string[]
  create_skus: Array<{
    key: number
    sku_name: string
    point_price: number
    stock_available: number
  }>
}

type SkuFormModel = {
  sku_name: string
  point_price: number
  stock_available: number
  status_code: SkuStatus
}

type MediaFormModel = {
  media_type: 'IMAGE' | 'VIDEO'
  media_url: string
  sort_no: number
}

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const statusCode = ref<ProductStatus | ''>('')
const rows = ref<AdminSpu[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()
const checkedSpuIds = ref<number[]>([])
const batchStockDelta = ref<number | null>(100)
const batchCategoryNames = ref<string[]>([])

const spuModalVisible = ref(false)
const spuModalMode = ref<'create' | 'edit'>('create')
const editingSpuId = ref<number | null>(null)
const spuSaving = ref(false)
const spuImageUploadLoading = ref(false)
const spuImageUploadFileList = ref<UploadFileInfo[]>([])
const MAX_SPU_SKU_COUNT = 9
let createSkuKeySeed = 1

function createDefaultCreateSkuItem() {
  createSkuKeySeed += 1
  return {
    key: createSkuKeySeed,
    sku_name: '',
    point_price: 1,
    stock_available: 0,
  }
}

const spuForm = reactive<SpuFormModel>({
  spu_name: '',
  category_names: [],
  detail_html: '',
  image_urls: [],
  create_skus: [createDefaultCreateSkuItem()],
})

const skuLoading = ref(false)
const skuRows = ref<AdminSku[]>([])
const skuSpu = ref<AdminSpu | null>(null)
const skuFormVisible = ref(false)
const skuFormMode = ref<'create' | 'edit'>('create')
const editingSkuId = ref<number | null>(null)
const skuSaving = ref(false)
const skuForm = reactive<SkuFormModel>({
  sku_name: '',
  point_price: 1,
  stock_available: 0,
  status_code: 'ENABLED',
})

const mediaLoading = ref(false)
const mediaRows = ref<AdminProductMedia[]>([])
const mediaSpu = ref<AdminSpu | null>(null)
const mediaSaving = ref(false)
const mediaUploadLoading = ref(false)
const mediaUploadFileList = ref<UploadFileInfo[]>([])
const mediaForm = reactive<MediaFormModel>({
  media_type: 'IMAGE',
  media_url: '',
  sort_no: 100,
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '上架中', value: 'ON_SHELF' },
  { label: '已下架', value: 'OFF_SHELF' },
]

const categoryOptions = ref<Array<{ label: string; value: string }>>([])

const skuStatusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '禁用', value: 'DISABLED' },
]

const mediaTypeOptions = [
  { label: '图片', value: 'IMAGE' },
  { label: '视频', value: 'VIDEO' },
]

function formatSixId(value: unknown): string {
  const num = Number(value || 0)
  if (!Number.isFinite(num) || num <= 0) return '000000'
  return String(Math.floor(num)).padStart(6, '0')
}

const columns: DataTableColumns<AdminSpu> = [
  {
    type: 'selection',
    width: 46,
  } as any,
  { title: 'SKC ID', key: 'id', width: 100, render: (row) => formatSixId(row.id) },
  { title: '商品名称', key: 'spu_name', minWidth: 180 },
  { title: '分类', key: 'category_name', width: 110 },
  { title: 'SKU数量', key: 'sku_count', width: 90, render: (row) => Number(row.sku_count || 0) },
  { title: '总库存', key: 'total_stock', width: 90 },
  {
    title: '状态',
    key: 'status_code',
    width: 90,
    render: (row) =>
      h(
        NTag,
        { type: row.status_code === 'ON_SHELF' ? 'success' : 'default' },
        { default: () => (row.status_code === 'ON_SHELF' ? '上架中' : '已下架') },
      ),
  },
  {
    title: '更新时间', key: 'updated_at', minWidth: 160
  },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    render: (row) =>
      h(NSpace, { size: 8 }, {
        default: () => [
          h(
            NButton,
            { size: 'small', tertiary: true, onClick: () => onOpenEditSpu(row) },
            { default: () => '编辑' },
          ),
          h(
            NPopconfirm,
            { onPositiveClick: () => void onToggleSpuStatus(row) },
            {
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', tertiary: true, type: row.status_code === 'ON_SHELF' ? 'warning' : 'primary' },
                  { default: () => (row.status_code === 'ON_SHELF' ? '下架' : '上架') },
                ),
              default: () => `确认${row.status_code === 'ON_SHELF' ? '下架' : '上架'}该商品？`,
            },
          ),
          h(
            NPopconfirm,
            { onPositiveClick: () => void onDeleteSpu(row) },
            {
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', tertiary: true, type: 'error' },
                  { default: () => '删除' },
                ),
              default: () => '确认删除该商品？删除后不可恢复',
            },
          ),
        ],
      }),
  },
]

const skuColumns: DataTableColumns<AdminSku> = [
  { title: 'SKU ID', key: 'id', width: 100, render: (row) => formatSixId(row.id) },
  { title: 'SKU名称', key: 'sku_name', minWidth: 120 },
  { title: '碎片价', key: 'point_price', width: 90 },
  { title: '库存', key: 'stock_available', width: 90 },
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
    width: 180,
    render: (row) =>
      h(NSpace, { size: 8 }, {
        default: () => [
          h(
            NButton,
            { size: 'small', tertiary: true, onClick: () => onOpenEditSku(row) },
            { default: () => '编辑' },
          ),
          h(
            NButton,
            { size: 'small', tertiary: true, type: 'warning', onClick: () => void onQuickStock(row) },
            { default: () => '+100库存' },
          ),
        ],
      }),
  },
]

const mediaColumns: DataTableColumns<AdminProductMedia> = [
  { title: '媒体ID', key: 'id', width: 90 },
  { title: '类型', key: 'media_type', width: 90 },
  {
    title: '预览',
    key: 'media_url',
    minWidth: 220,
    render: (row) =>
      row.media_type === 'IMAGE'
        ? h(NImage, {
            src: row.media_url,
            width: 72,
            height: 48,
            objectFit: 'cover',
            previewDisabled: false,
          })
        : h(NTag, { type: 'info' }, { default: () => '视频' }),
  },
  { title: '排序号', key: 'sort_no', width: 90 },
  {
    title: '操作',
    key: 'actions',
    width: 90,
    render: (row) =>
      h(
        NPopconfirm,
        { onPositiveClick: () => void onDeleteMedia(row.id) },
        {
          trigger: () =>
            h(
              NButton,
              { size: 'small', tertiary: true, type: 'error' },
              { default: () => '删除' },
            ),
          default: () => '确认删除该媒体？',
        },
      ),
  },
]

function resetSpuForm() {
  spuForm.spu_name = ''
  spuForm.category_names = []
  spuForm.detail_html = ''
  spuForm.image_urls = []
  spuForm.create_skus = [createDefaultCreateSkuItem()]
  spuImageUploadFileList.value = []
}

function resetSkuForm() {
  skuForm.sku_name = ''
  skuForm.point_price = 1
  skuForm.stock_available = 0
  skuForm.status_code = 'ENABLED'
}

function resetMediaForm() {
  mediaForm.media_type = 'IMAGE'
  mediaForm.media_url = ''
  mediaForm.sort_no = 100
  mediaUploadFileList.value = []
}

async function loadList() {
  loading.value = true
  try {
    const data = await fetchAdminProducts({
      pageNo: query.value.pageNo,
      pageSize: query.value.pageSize,
      keyword: keyword.value.trim(),
      status_code: statusCode.value,
    })
    rows.value = data.list
    total.value = data.total
    checkedSpuIds.value = []
  } finally {
    loading.value = false
  }
}

async function loadCategoryOptions() {
  const data = await fetchAdminCategories({ pageNo: 1, pageSize: 500 })
  const list = (data.list || [])
    .filter((item) => item.status_code === 'ENABLED')
    .sort((a, b) => Number(b.sort_no || 0) - Number(a.sort_no || 0))
  categoryOptions.value = list.map((item) => ({
    label: item.category_name,
    value: item.category_name,
  }))
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

function onUpdateCheckedSpuIds(keys: Array<number | string>) {
  checkedSpuIds.value = keys.map((item) => Number(item)).filter((item) => Number.isFinite(item))
}

async function onBatchUpdateStatus(status: ProductStatus) {
  const ids = checkedSpuIds.value.slice()
  if (!ids.length) {
    message.warning('请先勾选商品')
    return
  }
  let ok = 0
  let fail = 0
  for (const spuId of ids) {
    try {
      await updateAdminProductStatus(spuId, status)
      ok += 1
    } catch {
      fail += 1
    }
  }
  checkedSpuIds.value = []
  await loadList()
  message.success(`批量${status === 'ON_SHELF' ? '上架' : '下架'}完成：成功${ok}，失败${fail}`)
}

async function onBatchAddStock() {
  const ids = checkedSpuIds.value.slice()
  const delta = Math.round(Number(batchStockDelta.value || 0))
  if (!ids.length) {
    message.warning('请先勾选商品')
    return
  }
  if (!delta) {
    message.warning('批量加库存数量不能为0')
    return
  }
  let ok = 0
  let fail = 0
  for (const spuId of ids) {
    try {
      const skuList = await fetchProductSkus(spuId)
      for (const sku of skuList) {
        await adjustProductSkuStock(sku.id, delta)
      }
      ok += 1
    } catch {
      fail += 1
    }
  }
  checkedSpuIds.value = []
  await loadList()
  message.success(`批量加库存完成：成功${ok}，失败${fail}`)
}

async function onBatchUpdateCategory() {
  const ids = checkedSpuIds.value.slice()
  const names = (batchCategoryNames.value || []).map((item) => String(item || '').trim()).filter(Boolean)
  if (!ids.length) {
    message.warning('请先勾选商品')
    return
  }
  if (!names.length) {
    message.warning('请选择要批量设置的分类')
    return
  }
  let ok = 0
  let fail = 0
  for (const spuId of ids) {
    try {
      const current = rows.value.find((item) => item.id === spuId)
      await updateAdminProduct(spuId, {
        spu_name: current?.spu_name || '',
        category_name: names[0],
        category_names: names,
      })
      ok += 1
    } catch {
      fail += 1
    }
  }
  checkedSpuIds.value = []
  await loadList()
  message.success(`批量修改分类完成：成功${ok}，失败${fail}`)
}

function onOpenCreateSpu() {
  spuModalMode.value = 'create'
  editingSpuId.value = null
  resetSpuForm()
  skuSpu.value = null
  mediaSpu.value = null
  skuRows.value = []
  mediaRows.value = []
  spuModalVisible.value = true
}

function onOpenEditSpu(row: AdminSpu) {
  spuModalMode.value = 'edit'
  editingSpuId.value = row.id
  resetSpuForm()
  spuForm.spu_name = row.spu_name
  spuForm.detail_html = String((row as any).detail_html || '')
  const names = Array.isArray((row as any).category_names)
    ? ((row as any).category_names as string[]).map((item) => String(item || '').trim()).filter(Boolean)
    : String(row.category_name || '')
        .split(/[/,，]/)
        .map((item) => item.trim())
        .filter(Boolean)
  spuForm.category_names = names.length ? names : [String(row.category_name || '未分类')]
  for (const name of spuForm.category_names) {
    if (!categoryOptions.value.some((item) => item.value === name)) {
      categoryOptions.value = [{ label: name, value: name }, ...categoryOptions.value]
    }
  }
  skuSpu.value = row
  mediaSpu.value = row
  void loadSkus(row.id)
  void loadSpuDetail(row.id)
  spuModalVisible.value = true
}

async function loadSpuDetail(spuId: number) {
  mediaLoading.value = true
  try {
    const detail = await fetchAdminProductDetail(spuId)
    spuForm.detail_html = String(detail?.detail_html || '')
    mediaRows.value = Array.isArray(detail?.media_list) ? detail.media_list : []
  } finally {
    mediaLoading.value = false
  }
}

function onAddCreateSkuRow() {
  if (spuForm.create_skus.length >= MAX_SPU_SKU_COUNT) {
    message.warning(`单个商品最多支持${MAX_SPU_SKU_COUNT}个SKU`)
    return
  }
  spuForm.create_skus = [...spuForm.create_skus, createDefaultCreateSkuItem()]
}

function onRemoveCreateSkuRow(index: number) {
  if (spuForm.create_skus.length <= 1) {
    message.warning('至少保留1个SKU')
    return
  }
  if (!Number.isFinite(index) || index < 0 || index >= spuForm.create_skus.length) return
  const next = [...spuForm.create_skus]
  next.splice(index, 1)
  spuForm.create_skus = next
}

function buildSpuPayload(): SaveSpuPayload | null {
  const name = spuForm.spu_name.trim()
  const categoryNames = (spuForm.category_names || []).map((item) => String(item || '').trim()).filter(Boolean)
  if (!name || !categoryNames.length) return null

  if (spuModalMode.value === 'create') {
    const rawSkuList = (spuForm.create_skus || []).slice(0, MAX_SPU_SKU_COUNT)
    if (!rawSkuList.length) return null
    const skuList: SaveSpuPayload['sku_list'] = []
    for (const row of rawSkuList) {
      const skuName = String(row.sku_name || '').trim()
      const pointPrice = Number(row.point_price)
      const stockAvailable = Number(row.stock_available)
      if (!skuName || pointPrice <= 0 || stockAvailable < 0) return null
      skuList.push({
        sku_name: skuName,
        spec_text: skuName,
        point_price: pointPrice,
        stock_available: stockAvailable,
        status_code: 'ENABLED',
      })
    }
    if (!skuList.length) return null

    const payload: SaveSpuPayload = {
      spu_name: name,
      category_name: categoryNames[0],
      category_names: categoryNames,
      detail_html: String(spuForm.detail_html || ''),
      sku_list: skuList,
    }
    const imageUrls = (spuForm.image_urls || []).map((item) => String(item || '').trim()).filter(Boolean).slice(0, 9)
    if (imageUrls.length) {
      payload.image_urls = imageUrls
      payload.cover_image_url = imageUrls[0]
    }
    return payload
  }

  return {
    spu_name: name,
    category_name: categoryNames[0],
    category_names: categoryNames,
    detail_html: String(spuForm.detail_html || ''),
  }
}

async function onSubmitSpu() {
  const payload = buildSpuPayload()
  if (!payload) {
    message.warning(
      spuModalMode.value === 'create'
        ? '请完整填写SKC信息（商品名/分类/SKU/库存）'
        : '请完整填写SKC信息',
    )
    return
  }
  spuSaving.value = true
  try {
    if (spuModalMode.value === 'create') {
      await createAdminProduct(payload)
      message.success('SKC已创建')
    } else if (editingSpuId.value !== null) {
      await updateAdminProduct(editingSpuId.value, payload)
      message.success('SKC已更新')
    }
    spuModalVisible.value = false
    await loadList()
  } finally {
    spuSaving.value = false
  }
}

async function onToggleSpuStatus(row: AdminSpu) {
  const next: ProductStatus = row.status_code === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
  await updateAdminProductStatus(row.id, next)
  message.success('商品状态已更新')
  await loadList()
}

async function onDeleteSpu(row: AdminSpu) {
  await deleteAdminProduct(row.id)
  message.success(`已删除 ${row.spu_name}`)
  await loadList()
}

async function loadSkus(spuId: number) {
  skuLoading.value = true
  try {
    skuRows.value = await fetchProductSkus(spuId)
  } finally {
    skuLoading.value = false
  }
}

function onOpenCreateSku() {
  if (skuRows.value.length >= MAX_SPU_SKU_COUNT) {
    message.warning(`当前商品最多支持${MAX_SPU_SKU_COUNT}个SKU，请编辑已有SKU`)
    return
  }
  skuFormMode.value = 'create'
  editingSkuId.value = null
  resetSkuForm()
  skuFormVisible.value = true
}

function onOpenEditSku(row: AdminSku) {
  skuFormMode.value = 'edit'
  editingSkuId.value = row.id
  skuForm.sku_name = row.sku_name
  skuForm.point_price = row.point_price
  skuForm.stock_available = row.stock_available
  skuForm.status_code = row.status_code
  skuFormVisible.value = true
}

function buildSkuPayload(): SaveSkuPayload | null {
  const skuName = skuForm.sku_name.trim()
  if (!skuName) return null
  if (skuForm.point_price <= 0 || skuForm.stock_available < 0) return null
  return {
    sku_name: skuName,
    spec_text: skuName,
    point_price: skuForm.point_price,
    stock_available: skuForm.stock_available,
    status_code: skuForm.status_code,
  }
}

async function onSubmitSku() {
  if (!skuSpu.value) return
  const payload = buildSkuPayload()
  if (!payload) {
    message.warning('请完整填写SKU信息')
    return
  }
  skuSaving.value = true
  try {
    if (skuFormMode.value === 'create') {
      await createProductSku(skuSpu.value.id, payload)
      message.success('SKU已创建')
    } else if (editingSkuId.value !== null) {
      await updateProductSku(editingSkuId.value, payload)
      message.success('SKU已更新')
    }
    skuFormVisible.value = false
    await loadSkus(skuSpu.value.id)
    await loadList()
  } finally {
    skuSaving.value = false
  }
}

async function onQuickStock(row: AdminSku) {
  if (!skuSpu.value) return
  await adjustProductSkuStock(row.id, 100)
  message.success('库存已增加100')
  await loadSkus(skuSpu.value.id)
  await loadList()
}

async function loadMedia(spuId: number) {
  mediaLoading.value = true
  try {
    mediaRows.value = await fetchProductMedia(spuId)
  } finally {
    mediaLoading.value = false
  }
}

function buildMediaPayload(): SaveSpuMediaPayload | null {
  const url = mediaForm.media_url.trim()
  if (!url) return null
  if (mediaForm.sort_no <= 0) return null
  return {
    media_type: mediaForm.media_type,
    media_url: url,
    sort_no: mediaForm.sort_no,
  }
}

function currentImageMediaCount(): number {
  return mediaRows.value.filter((item) => String(item.media_type || '').toUpperCase() === 'IMAGE').length
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })
}

async function uploadImageFile(rawFile: File): Promise<string> {
  const dataUrl = await readFileAsDataUrl(rawFile)
  const file = await uploadAdminFile({
    file_name: rawFile.name,
    file_url: dataUrl,
    mime_type: rawFile.type || 'image/png',
    file_size_kb: Math.max(1, Math.round(rawFile.size / 1024)),
  })
  return file.file_url
}

async function onUploadSpuImage(options: UploadCustomRequestOptions) {
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
  if ((spuForm.image_urls || []).length >= 9) {
    options.onError()
    message.warning('商品图片最多支持9张')
    return
  }

  spuImageUploadLoading.value = true
  try {
    const imageUrl = await uploadImageFile(rawFile)
    if (!spuForm.image_urls.includes(imageUrl)) {
      spuForm.image_urls = [...spuForm.image_urls, imageUrl].slice(0, 9)
    }
    spuImageUploadFileList.value = []
    options.onFinish()
    message.success('图片上传成功')
  } catch {
    options.onError()
    message.error('图片上传失败')
  } finally {
    spuImageUploadLoading.value = false
  }
}

function onRemoveSpuImage(index: number) {
  if (!Number.isFinite(index) || index < 0 || index >= spuForm.image_urls.length) return
  const list = [...spuForm.image_urls]
  list.splice(index, 1)
  spuForm.image_urls = list
}

function onClearSpuImages() {
  spuForm.image_urls = []
  spuImageUploadFileList.value = []
}

async function onUploadMediaImage(options: UploadCustomRequestOptions) {
  if (currentImageMediaCount() >= 9) {
    options.onError()
    message.warning('商品图片最多支持9张')
    return
  }
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

  mediaUploadLoading.value = true
  try {
    mediaForm.media_type = 'IMAGE'
    mediaForm.media_url = await uploadImageFile(rawFile)
    options.onFinish()
    message.success('图片上传成功')
  } catch {
    options.onError()
    message.error('图片上传失败')
  } finally {
    mediaUploadLoading.value = false
  }
}

function onRemoveMediaImage() {
  mediaForm.media_url = ''
  mediaUploadFileList.value = []
  return true
}

async function onAddMedia() {
  if (!mediaSpu.value) return
  if (mediaForm.media_type === 'IMAGE' && currentImageMediaCount() >= 9) {
    message.warning('商品图片最多支持9张')
    return
  }
  const payload = buildMediaPayload()
  if (!payload) {
    message.warning('请完整填写媒体信息')
    return
  }
  mediaSaving.value = true
  try {
    await createProductMedia(mediaSpu.value.id, payload)
    message.success('媒体已添加')
    resetMediaForm()
    await loadMedia(mediaSpu.value.id)
  } finally {
    mediaSaving.value = false
  }
}

async function onDeleteMedia(mediaId: number) {
  if (!mediaSpu.value) return
  await deleteProductMedia(mediaId)
  message.success('媒体已删除')
  await loadMedia(mediaSpu.value.id)
}

onMounted(() => {
  void loadList()
  void loadCategoryOptions()
})
</script>

<template>
  <NCard title="商品中心（SKC）">
    <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按商品名称/分类/SKC搜索" style="width: 280px" @keyup.enter="onSearch" />
        <NSelect v-model:value="statusCode" :options="statusOptions" style="width: 140px" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
      <NSpace>
        <NButton type="primary" tertiary @click="onOpenCreateSpu">新增SKC</NButton>
      </NSpace>
    </NSpace>

    <NSpace style="margin-bottom: 12px" align="center" wrap>
      <NTag type="info">已选 {{ checkedSpuIds.length }} 个SKC</NTag>
      <NButton tertiary type="primary" :disabled="checkedSpuIds.length === 0" @click="onBatchUpdateStatus('ON_SHELF')">批量上架</NButton>
      <NButton tertiary type="warning" :disabled="checkedSpuIds.length === 0" @click="onBatchUpdateStatus('OFF_SHELF')">批量下架</NButton>
      <NInputNumber v-model:value="batchStockDelta" :min="-999999" :max="999999" style="width: 140px" />
      <NButton tertiary type="warning" :disabled="checkedSpuIds.length === 0" @click="onBatchAddStock">批量加库存</NButton>
      <NSelect
        v-model:value="batchCategoryNames"
        :options="categoryOptions"
        multiple
        filterable
        max-tag-count="responsive"
        placeholder="批量修改分类"
        style="width: 260px"
      />
      <NButton tertiary type="info" :disabled="checkedSpuIds.length === 0" @click="onBatchUpdateCategory">批量改分类</NButton>
    </NSpace>

    <NDataTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      :pagination="false"
      :scroll-x="1500"
      :row-key="(row) => row.id"
      :checked-row-keys="checkedSpuIds"
      @update:checked-row-keys="onUpdateCheckedSpuIds"
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

    <NModal
      v-model:show="spuModalVisible"
      preset="card"
      :title="spuModalMode === 'create' ? '新增SKC（一步上架）' : '编辑SKC'"
      :style="{ width: spuModalMode === 'create' ? '860px' : '980px' }"
    >
      <NForm label-placement="left" label-width="96">
        <NGrid :cols="2" :x-gap="12">
          <NGridItem>
            <NFormItem label="商品名称" required>
              <NInput v-model:value="spuForm.spu_name" maxlength="60" />
            </NFormItem>
          </NGridItem>
          <NGridItem>
            <NFormItem label="分类" required>
              <NSelect
                v-model:value="spuForm.category_names"
                :options="categoryOptions"
                filterable
                multiple
                max-tag-count="responsive"
                placeholder="请选择分类"
              />
            </NFormItem>
          </NGridItem>
        </NGrid>
        <NFormItem label="商品详情">
          <NInput
            v-model:value="spuForm.detail_html"
            type="textarea"
            :autosize="{ minRows: 6, maxRows: 14 }"
            placeholder="请输入商品详情（支持粘贴HTML或纯文本）"
          />
        </NFormItem>

        <template v-if="spuModalMode === 'create'">
          <NFormItem label="商品图片">
            <NSpace align="center" wrap>
              <NUpload
                v-model:file-list="spuImageUploadFileList"
                :max="1"
                accept="image/*"
                :custom-request="onUploadSpuImage"
                :default-upload="true"
              >
                <NButton :loading="spuImageUploadLoading" :disabled="spuForm.image_urls.length >= 9">上传图片</NButton>
              </NUpload>
              <NTag :type="spuForm.image_urls.length ? 'success' : 'default'">
                {{ spuForm.image_urls.length }}/9
              </NTag>
              <NButton v-if="spuForm.image_urls.length" tertiary size="small" type="warning" @click="onClearSpuImages">
                清空
              </NButton>
            </NSpace>
            <NSpace v-if="spuForm.image_urls.length" style="margin-top: 8px" wrap>
              <div
                v-for="(url, index) in spuForm.image_urls"
                :key="`${url}-${index}`"
                style="position: relative; width: 88px; height: 88px; border-radius: 8px; overflow: hidden; border: 1px solid #eee;"
              >
                <NImage :src="url" width="88" height="88" object-fit="cover" :preview-disabled="false" />
                <NButton
                  size="tiny"
                  type="error"
                  tertiary
                  style="position: absolute; right: 4px; top: 4px"
                  @click="onRemoveSpuImage(index)"
                >
                  删除
                </NButton>
              </div>
            </NSpace>
          </NFormItem>

          <NCard size="small" title="SKU列表（创建时最多9个）" style="margin-bottom: 12px">
            <NSpace justify="space-between" align="center" style="margin-bottom: 10px">
              <NTag type="info">{{ spuForm.create_skus.length }}/{{ MAX_SPU_SKU_COUNT }}</NTag>
              <NButton tertiary type="primary" :disabled="spuForm.create_skus.length >= MAX_SPU_SKU_COUNT" @click="onAddCreateSkuRow">
                新增SKU
              </NButton>
            </NSpace>
            <NGrid
              v-for="(row, index) in spuForm.create_skus"
              :key="row.key"
              :cols="3"
              :x-gap="12"
              style="margin-bottom: 8px"
            >
              <NGridItem>
                <NFormItem :label="`SKU名称 #${index + 1}`" required>
                  <NInput v-model:value="row.sku_name" maxlength="50" placeholder="例如：标准装" />
                </NFormItem>
              </NGridItem>
              <NGridItem>
                <NFormItem label="碎片价" required>
                  <NInputNumber v-model:value="row.point_price" :min="1" style="width: 100%" />
                </NFormItem>
              </NGridItem>
              <NGridItem>
                <NFormItem label="库存" required>
                  <NSpace align="center" style="width: 100%">
                    <NInputNumber v-model:value="row.stock_available" :min="0" style="width: 100%" />
                    <NButton
                      tertiary
                      type="error"
                      size="small"
                      :disabled="spuForm.create_skus.length <= 1"
                      @click="onRemoveCreateSkuRow(index)"
                    >
                      删除
                    </NButton>
                  </NSpace>
                </NFormItem>
              </NGridItem>
            </NGrid>
          </NCard>
        </template>

        <template v-else>
          <NCard size="small" title="SKU列表（平面编辑）" style="margin-bottom: 12px">
            <NSpace style="margin-bottom: 10px">
              <NButton tertiary type="primary" :disabled="skuRows.length >= MAX_SPU_SKU_COUNT" @click="onOpenCreateSku">新增SKU</NButton>
            </NSpace>
            <NDataTable :columns="skuColumns" :data="skuRows" :loading="skuLoading" :pagination="false" :scroll-x="720" />
          </NCard>

          <NCard size="small" title="媒体列表（平面编辑，图片最多9张）">
            <NGrid :cols="3" :x-gap="12" style="margin-bottom: 12px">
              <NGridItem>
                <NSelect v-model:value="mediaForm.media_type" :options="mediaTypeOptions" />
              </NGridItem>
              <NGridItem :span="2">
                <NUpload
                  v-model:file-list="mediaUploadFileList"
                  :max="1"
                  accept="image/*"
                  :custom-request="onUploadMediaImage"
                  :default-upload="true"
                  @remove="onRemoveMediaImage"
                >
                  <NButton :loading="mediaUploadLoading" :disabled="currentImageMediaCount() >= 9">上传图片</NButton>
                </NUpload>
              </NGridItem>
            </NGrid>
            <NSpace justify="space-between" align="center" style="margin-bottom: 12px">
              <NSpace align="center">
                <NInputNumber v-model:value="mediaForm.sort_no" :min="1" :max="9999" style="width: 160px" />
                <NTag type="info">图片 {{ currentImageMediaCount() }}/9</NTag>
              </NSpace>
              <NButton
                type="primary"
                tertiary
                :loading="mediaSaving"
                :disabled="!mediaForm.media_url || (mediaForm.media_type === 'IMAGE' && currentImageMediaCount() >= 9)"
                @click="onAddMedia"
              >
                新增媒体
              </NButton>
            </NSpace>
            <NDataTable :columns="mediaColumns" :data="mediaRows" :loading="mediaLoading" :pagination="false" :scroll-x="680" />
          </NCard>
        </template>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="spuModalVisible = false">取消</NButton>
          <NButton type="primary" :loading="spuSaving" @click="onSubmitSpu">保存</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="skuFormVisible" preset="card" :title="skuFormMode === 'create' ? '新增SKU' : '编辑SKU'" style="width: 560px">
      <NForm label-placement="left" label-width="90">
        <NFormItem label="SKU名称" required>
          <NInput v-model:value="skuForm.sku_name" maxlength="50" />
        </NFormItem>
        <NGrid :cols="2" :x-gap="12">
          <NGridItem>
            <NFormItem label="碎片价">
              <NInputNumber v-model:value="skuForm.point_price" :min="1" style="width: 100%" />
            </NFormItem>
          </NGridItem>
          <NGridItem>
            <NFormItem label="库存">
              <NInputNumber v-model:value="skuForm.stock_available" :min="0" style="width: 100%" />
            </NFormItem>
          </NGridItem>
        </NGrid>
        <NFormItem label="状态">
          <NSelect v-model:value="skuForm.status_code" :options="skuStatusOptions" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="skuFormVisible = false">取消</NButton>
          <NButton type="primary" :loading="skuSaving" @click="onSubmitSku">保存</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>
