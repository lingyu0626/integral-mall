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
  NSwitch,
  NTag,
  useMessage,
  type DataTableColumns,
} from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import {
  createProductAttrDef,
  deleteProductAttrDef,
  fetchProductAttrDefs,
  updateProductAttrDef,
  type SaveAttrDefPayload,
} from '../api/admin/products'
import type { AdminAttrDef, AttrValueType } from '../mock/product-center'

type AttrFormModel = {
  attr_name: string
  attr_code: string
  value_type: AttrValueType
  required_flag: boolean
  status_code: 'ENABLED' | 'DISABLED'
}

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const rows = ref<AdminAttrDef[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const modalVisible = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const saving = ref(false)
const formModel = reactive<AttrFormModel>({
  attr_name: '',
  attr_code: '',
  value_type: 'TEXT',
  required_flag: false,
  status_code: 'ENABLED',
})

const valueTypeOptions = [
  { label: '文本', value: 'TEXT' },
  { label: '数值', value: 'NUMBER' },
  { label: '枚举', value: 'ENUM' },
]

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '禁用', value: 'DISABLED' },
]

const columns: DataTableColumns<AdminAttrDef> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '属性名称', key: 'attr_name', minWidth: 150 },
  { title: '属性编码', key: 'attr_code', minWidth: 150 },
  {
    title: '值类型',
    key: 'value_type',
    width: 100,
    render: (row) => {
      if (row.value_type === 'NUMBER') return '数值'
      if (row.value_type === 'ENUM') return '枚举'
      return '文本'
    },
  },
  {
    title: '必填',
    key: 'required_flag',
    width: 90,
    render: (row) =>
      h(
        NTag,
        { type: row.required_flag ? 'warning' : 'default', size: 'small' },
        { default: () => (row.required_flag ? '必填' : '选填') },
      ),
  },
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
  { title: '更新时间', key: 'updated_at', minWidth: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 170,
    render: (row) =>
      h(NSpace, { size: 8 }, {
        default: () => [
          h(
            NButton,
            { size: 'small', tertiary: true, onClick: () => onOpenEdit(row) },
            { default: () => '编辑' },
          ),
          h(
            NPopconfirm,
            { onPositiveClick: () => void onDelete(row.id) },
            {
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', tertiary: true, type: 'error' },
                  { default: () => '删除' },
                ),
              default: () => '确认删除该属性定义？',
            },
          ),
        ],
      }),
  },
]

function resetForm() {
  formModel.attr_name = ''
  formModel.attr_code = ''
  formModel.value_type = 'TEXT'
  formModel.required_flag = false
  formModel.status_code = 'ENABLED'
}

function buildPayload(): SaveAttrDefPayload | null {
  const name = formModel.attr_name.trim()
  const code = formModel.attr_code.trim().toUpperCase()
  if (!name || !code) return null
  return {
    attr_name: name,
    attr_code: code,
    value_type: formModel.value_type,
    required_flag: formModel.required_flag,
    status_code: formModel.status_code,
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await fetchProductAttrDefs({
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

function onOpenEdit(row: AdminAttrDef) {
  modalMode.value = 'edit'
  editingId.value = row.id
  formModel.attr_name = row.attr_name
  formModel.attr_code = row.attr_code
  formModel.value_type = row.value_type
  formModel.required_flag = row.required_flag
  formModel.status_code = row.status_code
  modalVisible.value = true
}

async function onSubmit() {
  const payload = buildPayload()
  if (!payload) {
    message.warning('请完整填写属性定义')
    return
  }
  saving.value = true
  try {
    if (modalMode.value === 'create') {
      await createProductAttrDef(payload)
      message.success('属性定义已创建')
    } else if (editingId.value !== null) {
      await updateProductAttrDef(editingId.value, payload)
      message.success('属性定义已更新')
    }
    modalVisible.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

async function onDelete(attrDefId: number) {
  await deleteProductAttrDef(attrDefId)
  message.success('属性定义已删除')
  await loadList()
}

onMounted(() => {
  void loadList()
})
</script>

<template>
  <NCard title="商品属性定义（D11）">
    <NSpace justify="space-between" align="center" style="margin-bottom: 12px">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按属性名称/编码搜索" style="width: 280px" @keyup.enter="onSearch" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
      <NButton type="primary" tertiary @click="onOpenCreate">新增属性</NButton>
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

    <NModal v-model:show="modalVisible" preset="card" :title="modalMode === 'create' ? '新增属性定义' : '编辑属性定义'" style="width: 560px">
      <NForm label-placement="left" label-width="96">
        <NFormItem label="属性名称" required>
          <NInput v-model:value="formModel.attr_name" maxlength="30" />
        </NFormItem>
        <NFormItem label="属性编码" required>
          <NInput v-model:value="formModel.attr_code" maxlength="30" />
        </NFormItem>
        <NFormItem label="值类型">
          <NSelect v-model:value="formModel.value_type" :options="valueTypeOptions" />
        </NFormItem>
        <NFormItem label="状态">
          <NSelect v-model:value="formModel.status_code" :options="statusOptions" />
        </NFormItem>
        <NFormItem label="必填">
          <NSwitch v-model:value="formModel.required_flag" />
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
