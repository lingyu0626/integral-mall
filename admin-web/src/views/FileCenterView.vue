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
  NSpace,
  NUpload,
  useMessage,
  type DataTableColumns,
  type UploadCustomRequestOptions,
  type UploadFileInfo,
} from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import { deleteAdminFile, fetchAdminFiles, uploadAdminFile, type UploadFilePayload } from '../api/admin/files'
import type { AdminUploadedFile } from '../mock/platform-center'

type UploadFormModel = {
  file_name: string
  file_url: string
  mime_type: string
  file_size_kb: number | null
}

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const rows = ref<AdminUploadedFile[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const uploadModalVisible = ref(false)
const uploading = ref(false)
const filePickLoading = ref(false)
const uploadFileList = ref<UploadFileInfo[]>([])
const uploadForm = reactive<UploadFormModel>({
  file_name: '',
  file_url: '',
  mime_type: 'image/png',
  file_size_kb: 100,
})

const columns: DataTableColumns<AdminUploadedFile> = [
  { title: '文件ID', key: 'id', width: 90 },
  { title: '文件名', key: 'file_name', minWidth: 180 },
  {
    title: '预览',
    key: 'file_url',
    width: 90,
    render: (row) =>
      h(NImage, {
        src: row.file_url,
        width: 42,
        height: 42,
        objectFit: 'cover',
        previewDisabled: false,
      }),
  },
  { title: 'MIME', key: 'mime_type', width: 130 },
  { title: '大小(KB)', key: 'file_size_kb', width: 100 },
  { title: 'URL', key: 'file_url', minWidth: 280 },
  { title: '上传时间', key: 'uploaded_at', minWidth: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 90,
    render: (row) =>
      h(
        NPopconfirm,
        {
          onPositiveClick: () => void onDelete(row.id),
        },
        {
          trigger: () =>
            h(
              NButton,
              { size: 'small', tertiary: true, type: 'error' },
              { default: () => '删除' },
            ),
          default: () => '确认删除该文件？',
        },
      ),
  },
]

function resetUploadForm() {
  uploadForm.file_name = ''
  uploadForm.file_url = ''
  uploadForm.mime_type = 'image/png'
  uploadForm.file_size_kb = 100
  uploadFileList.value = []
}

function buildUploadPayload(): UploadFilePayload | null {
  const fileName = uploadForm.file_name.trim()
  const fileUrl = uploadForm.file_url.trim()
  const mimeType = uploadForm.mime_type.trim()
  if (!fileName || !fileUrl || !mimeType || uploadForm.file_size_kb === null) return null
  if (uploadForm.file_size_kb <= 0) return null
  return {
    file_name: fileName,
    file_url: fileUrl,
    mime_type: mimeType,
    file_size_kb: uploadForm.file_size_kb,
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await fetchAdminFiles({
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

function onOpenUploadModal() {
  resetUploadForm()
  uploadModalVisible.value = true
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })
}

async function onPickImageFile(options: UploadCustomRequestOptions) {
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

  filePickLoading.value = true
  try {
    const dataUrl = await readFileAsDataUrl(rawFile)
    uploadForm.file_name = rawFile.name
    uploadForm.file_url = dataUrl
    uploadForm.mime_type = rawFile.type || 'image/png'
    uploadForm.file_size_kb = Math.max(1, Math.round(rawFile.size / 1024))
    options.onFinish()
    message.success('图片已选择，点击确认上传')
  } catch {
    options.onError()
    message.error('图片读取失败')
  } finally {
    filePickLoading.value = false
  }
}

function onRemoveSelectedImage() {
  uploadForm.file_url = ''
  uploadForm.file_name = ''
  uploadForm.file_size_kb = null
  uploadForm.mime_type = 'image/png'
  return true
}

async function onSubmitUpload() {
  const payload = buildUploadPayload()
  if (!payload) {
    message.warning('请完整填写文件信息')
    return
  }
  uploading.value = true
  try {
    await uploadAdminFile(payload)
    message.success('文件已上传')
    uploadModalVisible.value = false
    await loadList()
  } finally {
    uploading.value = false
  }
}

async function onDelete(fileId: number) {
  await deleteAdminFile(fileId)
  message.success('文件已删除')
  await loadList()
}

onMounted(() => {
  void loadList()
})
</script>

<template>
  <NCard title="文件中心（D14）">
    <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按文件名/URL/MIME搜索" style="width: 300px" @keyup.enter="onSearch" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
      <NButton tertiary type="primary" @click="onOpenUploadModal">上传文件</NButton>
    </NSpace>

    <NDataTable :columns="columns" :data="rows" :loading="loading" :pagination="false" :scroll-x="1440" />

    <div style="margin-top: 14px">
      <AppPagination
        :page-no="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:page-no="onPageNoChange"
        @update:page-size="onPageSizeChange"
      />
    </div>

    <NModal v-model:show="uploadModalVisible" preset="card" title="上传文件" style="width: 620px">
      <NForm label-placement="left" label-width="96">
        <NFormItem label="文件名" required>
          <NInput v-model:value="uploadForm.file_name" placeholder="如 banner-home.png" />
        </NFormItem>
        <NFormItem label="上传图片" required>
          <NSpace vertical style="width: 100%">
            <NUpload
              v-model:file-list="uploadFileList"
              :max="1"
              accept="image/*"
              :custom-request="onPickImageFile"
              :default-upload="true"
              @remove="onRemoveSelectedImage"
            >
              <NButton :loading="filePickLoading">选择图片</NButton>
            </NUpload>
            <NImage
              v-if="uploadForm.file_url"
              :src="uploadForm.file_url"
              width="92"
              height="92"
              object-fit="cover"
              preview-disabled
            />
          </NSpace>
        </NFormItem>
        <NSpace :size="12" style="width: 100%">
          <NFormItem label="MIME" style="flex: 1">
            <NInput v-model:value="uploadForm.mime_type" placeholder="image/png" />
          </NFormItem>
          <NFormItem label="大小(KB)" style="flex: 1">
            <NInputNumber v-model:value="uploadForm.file_size_kb" :min="1" style="width: 100%" />
          </NFormItem>
        </NSpace>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="uploadModalVisible = false">取消</NButton>
          <NButton type="primary" :loading="uploading" @click="onSubmitUpload">确认上传</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>
