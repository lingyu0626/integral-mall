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
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import { uploadAdminFile } from '../api/admin/files'
import {
  createGroupResource,
  deleteGroupResource,
  fetchGroupResources,
  updateGroupResource,
  updateGroupResourceStatus,
  type SaveGroupResourcePayload,
} from '../api/admin/group-resources'
import type { AdminGroupResource, GroupResourceStatus } from '../mock/backpack-group'

type GroupFormModel = {
  group_name: string
  qr_image_url: string
  intro_text: string
  max_member_count: number | null
  current_member_count: number | null
  expire_at: string
  status_code: GroupResourceStatus
}

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const rows = ref<AdminGroupResource[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const modalVisible = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const saving = ref(false)
const qrUploadLoading = ref(false)
const qrUploadFileList = ref<UploadFileInfo[]>([])
const formModel = reactive<GroupFormModel>({
  group_name: '',
  qr_image_url: '',
  intro_text: '',
  max_member_count: 500,
  current_member_count: 0,
  expire_at: '2026-12-31 23:59:59',
  status_code: 'ENABLED',
})

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '禁用', value: 'DISABLED' },
]

const columns: DataTableColumns<AdminGroupResource> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '群名称', key: 'group_name', minWidth: 150 },
  {
    title: '二维码',
    key: 'qr_image_url',
    width: 100,
    render: (row) =>
      h(NImage, {
        src: row.qr_image_url,
        width: 42,
        height: 42,
        objectFit: 'cover',
        previewDisabled: false,
      }),
  },
  { title: '简介', key: 'intro_text', minWidth: 220 },
  { title: '人数', key: 'current_member_count', width: 120, render: (row) => `${row.current_member_count}/${row.max_member_count}` },
  { title: '过期时间', key: 'expire_at', minWidth: 160 },
  {
    title: '状态',
    key: 'status_code',
    width: 90,
    render: (row) =>
      h(
        NTag,
        { type: row.status_code === 'ENABLED' ? 'success' : 'default' },
        { default: () => (row.status_code === 'ENABLED' ? '启用' : '禁用') },
      ),
  },
  { title: '更新时间', key: 'updated_at', minWidth: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 250,
    render: (row) =>
      h(NSpace, { size: 8 }, {
        default: () => [
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => onOpenEdit(row),
            },
            { default: () => '编辑' },
          ),
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onToggleStatus(row),
            },
            {
              trigger: () =>
                h(
                  NButton,
                  {
                    size: 'small',
                    tertiary: true,
                    type: row.status_code === 'ENABLED' ? 'warning' : 'primary',
                  },
                  { default: () => (row.status_code === 'ENABLED' ? '禁用' : '启用') },
                ),
              default: () => `确认${row.status_code === 'ENABLED' ? '禁用' : '启用'}该群资源？`,
            },
          ),
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onDelete(row.id),
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
              default: () => '确认删除该群资源？',
            },
          ),
        ],
      }),
  },
]

function resetForm() {
  formModel.group_name = ''
  formModel.qr_image_url = ''
  formModel.intro_text = ''
  formModel.max_member_count = 500
  formModel.current_member_count = 0
  formModel.expire_at = '2026-12-31 23:59:59'
  formModel.status_code = 'ENABLED'
  qrUploadFileList.value = []
}

function buildPayload(): SaveGroupResourcePayload | null {
  const groupName = formModel.group_name.trim()
  const qrUrl = formModel.qr_image_url.trim()
  const intro = formModel.intro_text.trim()
  const expireAt = formModel.expire_at.trim()
  if (!groupName || !qrUrl || !intro || !expireAt) return null
  if (formModel.max_member_count === null || formModel.current_member_count === null) return null
  if (formModel.max_member_count <= 0 || formModel.current_member_count < 0) return null
  return {
    group_name: groupName,
    qr_image_url: qrUrl,
    intro_text: intro,
    max_member_count: formModel.max_member_count,
    current_member_count: formModel.current_member_count,
    expire_at: expireAt,
    status_code: formModel.status_code,
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await fetchGroupResources({
      pageNo: query.value.pageNo,
      pageSize: query.value.pageSize,
      keyword: keyword.value.trim(),
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

function onOpenCreate() {
  modalMode.value = 'create'
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

function onOpenEdit(row: AdminGroupResource) {
  modalMode.value = 'edit'
  editingId.value = row.id
  qrUploadFileList.value = []
  formModel.group_name = row.group_name
  formModel.qr_image_url = row.qr_image_url
  formModel.intro_text = row.intro_text
  formModel.max_member_count = row.max_member_count
  formModel.current_member_count = row.current_member_count
  formModel.expire_at = row.expire_at
  formModel.status_code = row.status_code
  modalVisible.value = true
}

async function onSubmit() {
  const payload = buildPayload()
  if (!payload) {
    message.warning('请完整填写群资源信息')
    return
  }
  saving.value = true
  try {
    if (modalMode.value === 'create') {
      await createGroupResource(payload)
      message.success('群资源已创建')
    } else if (editingId.value !== null) {
      await updateGroupResource(editingId.value, payload)
      message.success('群资源已更新')
    }
    modalVisible.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

async function onToggleStatus(row: AdminGroupResource) {
  const nextStatus: GroupResourceStatus = row.status_code === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  await updateGroupResourceStatus(row.id, nextStatus)
  message.success('状态已更新')
  await loadList()
}

async function onDelete(resourceId: number) {
  await deleteGroupResource(resourceId)
  message.success('群资源已删除')
  await loadList()
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })
}

async function onUploadQrImage(options: UploadCustomRequestOptions) {
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

  qrUploadLoading.value = true
  try {
    const dataUrl = await readFileAsDataUrl(rawFile)
    const file = await uploadAdminFile({
      file_name: rawFile.name,
      file_url: dataUrl,
      mime_type: rawFile.type || 'image/png',
      file_size_kb: Math.max(1, Math.round(rawFile.size / 1024)),
    })
    formModel.qr_image_url = file.file_url
    options.onFinish()
    message.success('二维码图片上传成功')
  } catch {
    options.onError()
    message.error('二维码图片上传失败')
  } finally {
    qrUploadLoading.value = false
  }
}

function onRemoveQrImage() {
  formModel.qr_image_url = ''
  return true
}

onMounted(() => {
  void loadList()
})
</script>

<template>
  <NCard title="群资源管理（D13）">
    <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按群名称/简介搜索" style="width: 280px" @keyup.enter="onSearch" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
      <NButton type="primary" tertiary @click="onOpenCreate">新增群资源</NButton>
    </NSpace>

    <NDataTable :columns="columns" :data="rows" :loading="loading" :pagination="false" :scroll-x="1420" />

    <div style="margin-top: 14px">
      <AppPagination
        :page-no="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:page-no="onPageNoChange"
        @update:page-size="onPageSizeChange"
      />
    </div>

    <NModal v-model:show="modalVisible" preset="card" :title="modalMode === 'create' ? '新增群资源' : '编辑群资源'" style="width: 640px">
      <NForm label-placement="left" label-width="96">
        <NFormItem label="群名称" required>
          <NInput v-model:value="formModel.group_name" maxlength="40" />
        </NFormItem>
        <NFormItem label="二维码" required>
          <NSpace vertical style="width: 100%">
            <NUpload
              v-model:file-list="qrUploadFileList"
              :max="1"
              accept="image/*"
              :custom-request="onUploadQrImage"
              :default-upload="true"
              @remove="onRemoveQrImage"
            >
              <NButton :loading="qrUploadLoading">上传二维码图片</NButton>
            </NUpload>
            <NImage
              v-if="formModel.qr_image_url"
              :src="formModel.qr_image_url"
              width="84"
              height="84"
              object-fit="cover"
              preview-disabled
            />
          </NSpace>
        </NFormItem>
        <NFormItem label="群简介" required>
          <NInput v-model:value="formModel.intro_text" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </NFormItem>
        <NSpace :size="12" style="width: 100%">
          <NFormItem label="最大人数" style="flex: 1">
            <NInputNumber v-model:value="formModel.max_member_count" :min="1" style="width: 100%" />
          </NFormItem>
          <NFormItem label="当前人数" style="flex: 1">
            <NInputNumber v-model:value="formModel.current_member_count" :min="0" style="width: 100%" />
          </NFormItem>
        </NSpace>
        <NFormItem label="过期时间" required>
          <NInput v-model:value="formModel.expire_at" placeholder="yyyy-MM-dd HH:mm:ss" />
        </NFormItem>
        <NFormItem label="状态">
          <NSelect v-model:value="formModel.status_code" :options="statusOptions" />
        </NFormItem>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="modalVisible = false">取消</NButton>
          <NButton type="primary" :loading="saving" @click="onSubmit">保存</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>
