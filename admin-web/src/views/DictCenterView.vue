<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NGrid,
  NGridItem,
  NInput,
  NInputNumber,
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
  createDictItem,
  createDictType,
  deleteDictItem,
  deleteDictType,
  fetchDictItems,
  fetchDictTypes,
  updateDictItem,
  updateDictType,
  type SaveDictItemPayload,
  type SaveDictTypePayload,
} from '../api/admin/dict'
import type { AdminDictItem, AdminDictType, DictStatus } from '../mock/platform-center'

type DictTypeFormModel = {
  dict_type_code: string
  dict_type_name: string
  status_code: DictStatus
  remark: string
}

type DictItemFormModel = {
  item_code: string
  item_name: string
  item_value: string
  sort_no: number | null
  status_code: DictStatus
}

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const typeRows = ref<AdminDictType[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const selectedTypeCode = ref('')
const selectedTypeName = ref('')
const itemRows = ref<AdminDictItem[]>([])
const itemLoading = ref(false)

const typeModalVisible = ref(false)
const typeModalMode = ref<'create' | 'edit'>('create')
const editingTypeId = ref<number | null>(null)
const typeSaving = ref(false)
const typeForm = reactive<DictTypeFormModel>({
  dict_type_code: '',
  dict_type_name: '',
  status_code: 'ENABLED',
  remark: '',
})

const itemModalVisible = ref(false)
const itemModalMode = ref<'create' | 'edit'>('create')
const editingItemId = ref<number | null>(null)
const itemSaving = ref(false)
const itemForm = reactive<DictItemFormModel>({
  item_code: '',
  item_name: '',
  item_value: '',
  sort_no: 100,
  status_code: 'ENABLED',
})

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '禁用', value: 'DISABLED' },
]

const typeColumns: DataTableColumns<AdminDictType> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '类型编码', key: 'dict_type_code', minWidth: 140 },
  { title: '类型名称', key: 'dict_type_name', minWidth: 130 },
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
  { title: '备注', key: 'remark', minWidth: 140 },
  {
    title: '操作',
    key: 'actions',
    width: 240,
    render: (row) =>
      h(NSpace, { size: 8 }, {
        default: () => [
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: selectedTypeCode.value === row.dict_type_code ? 'primary' : 'default',
              onClick: () => void onSelectType(row),
            },
            { default: () => '字典项' },
          ),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => onOpenEditType(row),
            },
            { default: () => '编辑' },
          ),
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onDeleteType(row.id),
            },
            {
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', tertiary: true, type: 'error' },
                  { default: () => '删除' },
                ),
              default: () => '确认删除该字典类型？',
            },
          ),
        ],
      }),
  },
]

const itemColumns: DataTableColumns<AdminDictItem> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '项编码', key: 'item_code', minWidth: 130 },
  { title: '项名称', key: 'item_name', minWidth: 120 },
  { title: '值', key: 'item_value', minWidth: 130 },
  { title: '排序', key: 'sort_no', width: 80 },
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
    width: 150,
    render: (row) =>
      h(NSpace, { size: 8 }, {
        default: () => [
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => onOpenEditItem(row),
            },
            { default: () => '编辑' },
          ),
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onDeleteItem(row.id),
            },
            {
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', tertiary: true, type: 'error' },
                  { default: () => '删除' },
                ),
              default: () => '确认删除该字典项？',
            },
          ),
        ],
      }),
  },
]

function resetTypeForm() {
  typeForm.dict_type_code = ''
  typeForm.dict_type_name = ''
  typeForm.status_code = 'ENABLED'
  typeForm.remark = ''
}

function resetItemForm() {
  itemForm.item_code = ''
  itemForm.item_name = ''
  itemForm.item_value = ''
  itemForm.sort_no = 100
  itemForm.status_code = 'ENABLED'
}

function buildTypePayload(): SaveDictTypePayload | null {
  const code = typeForm.dict_type_code.trim().toUpperCase()
  const name = typeForm.dict_type_name.trim()
  if (!code || !name) return null
  return {
    dict_type_code: code,
    dict_type_name: name,
    status_code: typeForm.status_code,
    remark: typeForm.remark.trim(),
  }
}

function buildItemPayload(): SaveDictItemPayload | null {
  if (!selectedTypeCode.value) return null
  const code = itemForm.item_code.trim().toUpperCase()
  const name = itemForm.item_name.trim()
  const value = itemForm.item_value.trim()
  if (!code || !name || !value || itemForm.sort_no === null) return null
  return {
    item_code: code,
    item_name: name,
    item_value: value,
    sort_no: itemForm.sort_no,
    status_code: itemForm.status_code,
  }
}

async function loadTypes() {
  loading.value = true
  try {
    const data = await fetchDictTypes({
      pageNo: query.value.pageNo,
      pageSize: query.value.pageSize,
      keyword: keyword.value.trim(),
    })
    typeRows.value = data.list
    total.value = data.total
    const exists = typeRows.value.some((item) => item.dict_type_code === selectedTypeCode.value)
    if (!exists) {
      const first = typeRows.value[0]
      if (first) {
        await onSelectType(first)
      } else {
        selectedTypeCode.value = ''
        selectedTypeName.value = ''
        itemRows.value = []
      }
    }
  } finally {
    loading.value = false
  }
}

async function loadItems(typeCode: string) {
  itemLoading.value = true
  try {
    itemRows.value = await fetchDictItems(typeCode)
  } finally {
    itemLoading.value = false
  }
}

function onSearch() {
  updatePage(1)
  void loadTypes()
}

function onPageNoChange(value: number) {
  updatePage(value)
  void loadTypes()
}

function onPageSizeChange(value: number) {
  updatePageSize(value)
  void loadTypes()
}

async function onSelectType(row: AdminDictType) {
  selectedTypeCode.value = row.dict_type_code
  selectedTypeName.value = row.dict_type_name
  await loadItems(row.dict_type_code)
}

function onOpenCreateType() {
  typeModalMode.value = 'create'
  editingTypeId.value = null
  resetTypeForm()
  typeModalVisible.value = true
}

function onOpenEditType(row: AdminDictType) {
  typeModalMode.value = 'edit'
  editingTypeId.value = row.id
  typeForm.dict_type_code = row.dict_type_code
  typeForm.dict_type_name = row.dict_type_name
  typeForm.status_code = row.status_code
  typeForm.remark = row.remark
  typeModalVisible.value = true
}

async function onSubmitType() {
  const payload = buildTypePayload()
  if (!payload) {
    message.warning('请完整填写字典类型信息')
    return
  }
  typeSaving.value = true
  try {
    if (typeModalMode.value === 'create') {
      await createDictType(payload)
      message.success('字典类型已创建')
    } else if (editingTypeId.value !== null) {
      await updateDictType(editingTypeId.value, payload)
      message.success('字典类型已更新')
    }
    typeModalVisible.value = false
    await loadTypes()
  } finally {
    typeSaving.value = false
  }
}

async function onDeleteType(typeId: number) {
  await deleteDictType(typeId)
  message.success('字典类型已删除')
  await loadTypes()
}

function onOpenCreateItem() {
  if (!selectedTypeCode.value) {
    message.warning('请先选择字典类型')
    return
  }
  itemModalMode.value = 'create'
  editingItemId.value = null
  resetItemForm()
  itemModalVisible.value = true
}

function onOpenEditItem(row: AdminDictItem) {
  itemModalMode.value = 'edit'
  editingItemId.value = row.id
  itemForm.item_code = row.item_code
  itemForm.item_name = row.item_name
  itemForm.item_value = row.item_value
  itemForm.sort_no = row.sort_no
  itemForm.status_code = row.status_code
  itemModalVisible.value = true
}

async function onSubmitItem() {
  if (!selectedTypeCode.value) return
  const payload = buildItemPayload()
  if (!payload) {
    message.warning('请完整填写字典项信息')
    return
  }
  itemSaving.value = true
  try {
    if (itemModalMode.value === 'create') {
      await createDictItem(selectedTypeCode.value, payload)
      message.success('字典项已创建')
    } else if (editingItemId.value !== null) {
      await updateDictItem(editingItemId.value, payload)
      message.success('字典项已更新')
    }
    itemModalVisible.value = false
    await loadItems(selectedTypeCode.value)
  } finally {
    itemSaving.value = false
  }
}

async function onDeleteItem(itemId: number) {
  if (!selectedTypeCode.value) return
  await deleteDictItem(itemId)
  message.success('字典项已删除')
  await loadItems(selectedTypeCode.value)
}

onMounted(() => {
  void loadTypes()
})
</script>

<template>
  <NCard title="字典中心（D14）">
    <NGrid :cols="24" :x-gap="12">
      <NGridItem :span="11">
        <NCard size="small" title="字典类型">
          <NSpace style="margin-bottom: 12px" justify="space-between">
            <NSpace>
              <NInput v-model:value="keyword" clearable placeholder="按编码/名称搜索" style="width: 220px" @keyup.enter="onSearch" />
              <NButton type="primary" @click="onSearch">查询</NButton>
            </NSpace>
            <NButton tertiary type="primary" @click="onOpenCreateType">新增类型</NButton>
          </NSpace>

          <NDataTable :columns="typeColumns" :data="typeRows" :loading="loading" :pagination="false" :scroll-x="760" max-height="520" />

          <div style="margin-top: 12px">
            <AppPagination
              :page-no="pageNo"
              :page-size="pageSize"
              :total="total"
              @update:page-no="onPageNoChange"
              @update:page-size="onPageSizeChange"
            />
          </div>
        </NCard>
      </NGridItem>

      <NGridItem :span="13">
        <NCard size="small" :title="selectedTypeCode ? `字典项 · ${selectedTypeName}（${selectedTypeCode}）` : '字典项'">
          <NSpace style="margin-bottom: 12px" justify="space-between">
            <div />
            <NButton tertiary type="primary" :disabled="!selectedTypeCode" @click="onOpenCreateItem">新增字典项</NButton>
          </NSpace>
          <NDataTable :columns="itemColumns" :data="itemRows" :loading="itemLoading" :pagination="false" :scroll-x="820" max-height="580" />
        </NCard>
      </NGridItem>
    </NGrid>

    <NModal v-model:show="typeModalVisible" preset="card" :title="typeModalMode === 'create' ? '新增字典类型' : '编辑字典类型'" style="width: 560px">
      <NForm label-placement="left" label-width="96">
        <NFormItem label="类型编码" required>
          <NInput v-model:value="typeForm.dict_type_code" maxlength="40" />
        </NFormItem>
        <NFormItem label="类型名称" required>
          <NInput v-model:value="typeForm.dict_type_name" maxlength="30" />
        </NFormItem>
        <NFormItem label="状态">
          <NSelect v-model:value="typeForm.status_code" :options="statusOptions" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput v-model:value="typeForm.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="typeModalVisible = false">取消</NButton>
          <NButton type="primary" :loading="typeSaving" @click="onSubmitType">保存</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="itemModalVisible" preset="card" :title="itemModalMode === 'create' ? '新增字典项' : '编辑字典项'" style="width: 560px">
      <NForm label-placement="left" label-width="96">
        <NFormItem label="项编码" required>
          <NInput v-model:value="itemForm.item_code" maxlength="40" />
        </NFormItem>
        <NFormItem label="项名称" required>
          <NInput v-model:value="itemForm.item_name" maxlength="30" />
        </NFormItem>
        <NFormItem label="项值" required>
          <NInput v-model:value="itemForm.item_value" maxlength="60" />
        </NFormItem>
        <NFormItem label="排序号">
          <NInputNumber v-model:value="itemForm.sort_no" :min="1" :max="9999" style="width: 100%" />
        </NFormItem>
        <NFormItem label="状态">
          <NSelect v-model:value="itemForm.status_code" :options="statusOptions" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="itemModalVisible = false">取消</NButton>
          <NButton type="primary" :loading="itemSaving" @click="onSubmitItem">保存</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>
