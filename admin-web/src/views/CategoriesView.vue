<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NModal,
  NPopconfirm,
  NSpace,
  NTag,
  useMessage,
  type DataTableColumns,
} from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import {
  createAdminCategory,
  deleteAdminCategory,
  fetchAdminCategories,
  sortAdminCategories,
  updateAdminCategory,
  updateAdminCategoryStatus,
  type SaveCategoryPayload,
} from '../api/admin/categories'
import type { AdminCategory } from '../mock/admin'

type CategoryFormModel = {
  category_name: string
  sort_no: number
}

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const rows = ref<AdminCategory[]>([])
const total = ref(0)
const sortDirty = ref(false)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const modalVisible = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const saving = ref(false)

const formModel = reactive<CategoryFormModel>({
  category_name: '',
  sort_no: 100,
})

const columns: DataTableColumns<AdminCategory> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '分类名称', key: 'category_name', minWidth: 140 },
  { title: '排序号', key: 'sort_no', width: 100 },
  { title: '商品数', key: 'product_count', width: 100 },
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
  { title: '更新时间', key: 'updated_at', minWidth: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 320,
    render: (row) =>
      h(NSpace, { size: 8 }, {
        default: () => [
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => onMove(row, -1),
            },
            { default: () => '上移' },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => onMove(row, 1),
            },
            { default: () => '下移' },
          ),
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
              default: () => `确认${row.status_code === 'ENABLED' ? '禁用' : '启用'}该分类？`,
            },
          ),
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onDeleteCategory(row),
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
              default: () => '确认删除该分类？',
            },
          ),
        ],
      }),
  },
]

function resetForm() {
  formModel.category_name = ''
  formModel.sort_no = 100
}

function normalizeSortByCurrentOrder() {
  const maxSort = rows.value.length * 10
  rows.value.forEach((row, index) => {
    row.sort_no = maxSort - index * 10
  })
}

function onMove(row: AdminCategory, direction: -1 | 1) {
  const index = rows.value.findIndex((item) => item.id === row.id)
  if (index < 0) return
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= rows.value.length) return

  const nextRows = rows.value.slice()
  const [current] = nextRows.splice(index, 1)
  nextRows.splice(nextIndex, 0, current)
  rows.value = nextRows
  normalizeSortByCurrentOrder()
  sortDirty.value = true
}

async function loadList() {
  loading.value = true
  try {
    const data = await fetchAdminCategories({
      pageNo: query.value.pageNo,
      pageSize: query.value.pageSize,
      keyword: keyword.value.trim(),
    })
    rows.value = data.list.slice()
    total.value = data.total
    sortDirty.value = false
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

function onOpenEdit(row: AdminCategory) {
  modalMode.value = 'edit'
  editingId.value = row.id
  formModel.category_name = row.category_name
  formModel.sort_no = row.sort_no
  modalVisible.value = true
}

async function onSubmit() {
  const name = formModel.category_name.trim()
  if (!name) {
    message.warning('请输入分类名称')
    return
  }
  const payload: SaveCategoryPayload = {
    category_name: name,
    sort_no: formModel.sort_no,
  }
  saving.value = true
  try {
    if (modalMode.value === 'create') {
      await createAdminCategory(payload)
      message.success('分类已创建')
    } else if (editingId.value) {
      await updateAdminCategory(editingId.value, payload)
      message.success('分类已更新')
    }
    modalVisible.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

async function onToggleStatus(row: AdminCategory) {
  const next = row.status_code === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  await updateAdminCategoryStatus(row.id, next)
  message.success('状态已更新')
  await loadList()
}

async function onDeleteCategory(row: AdminCategory) {
  await deleteAdminCategory(row.id)
  message.success('分类已删除')
  await loadList()
}

async function onSaveSort() {
  const payload = rows.value.map((item) => ({
    category_id: item.id,
    sort_no: item.sort_no,
  }))
  await sortAdminCategories(payload)
  message.success('排序已保存')
  sortDirty.value = false
  await loadList()
}

onMounted(() => {
  void loadList()
})
</script>

<template>
  <NCard title="分类管理（D10）">
    <NSpace justify="space-between" align="center" style="margin-bottom: 12px">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按分类名称搜索" style="width: 260px" @keyup.enter="onSearch" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
      <NSpace>
        <NButton :disabled="!sortDirty" @click="onSaveSort">保存排序</NButton>
        <NButton type="primary" tertiary @click="onOpenCreate">新增分类</NButton>
      </NSpace>
    </NSpace>

    <NDataTable :columns="columns" :data="rows" :loading="loading" :pagination="false" />

    <div style="margin-top: 14px">
      <AppPagination
        :page-no="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:page-no="onPageNoChange"
        @update:page-size="onPageSizeChange"
      />
    </div>

    <NModal v-model:show="modalVisible" preset="card" :title="modalMode === 'create' ? '新增分类' : '编辑分类'" style="width: 480px">
      <NForm label-placement="left" label-width="90">
        <NFormItem label="分类名称" required>
          <NInput v-model:value="formModel.category_name" maxlength="20" placeholder="请输入分类名称" />
        </NFormItem>
        <NFormItem label="排序号">
          <NInputNumber v-model:value="formModel.sort_no" :min="1" :max="9999" style="width: 100%" />
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
