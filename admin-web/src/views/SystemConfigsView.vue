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
  createSystemConfig,
  deleteSystemConfig,
  fetchSystemConfigs,
  updateSystemConfig,
  type SaveSystemConfigPayload,
} from '../api/admin/system-configs'
import type { AdminSystemConfig, ConfigStatus, ConfigValueType } from '../mock/platform-center'

type ConfigFormModel = {
  config_key: string
  config_name: string
  config_value: string
  value_type_code: ConfigValueType
  group_code: string
  status_code: ConfigStatus
  remark: string
}

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const rows = ref<AdminSystemConfig[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const modalVisible = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const saving = ref(false)
const formModel = reactive<ConfigFormModel>({
  config_key: '',
  config_name: '',
  config_value: '',
  value_type_code: 'STRING',
  group_code: '',
  status_code: 'ENABLED',
  remark: '',
})

const valueTypeOptions = [
  { label: '字符串', value: 'STRING' },
  { label: '数字', value: 'NUMBER' },
  { label: '布尔', value: 'BOOLEAN' },
  { label: 'JSON', value: 'JSON' },
]

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '禁用', value: 'DISABLED' },
]

const columns: DataTableColumns<AdminSystemConfig> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '配置键', key: 'config_key', minWidth: 180 },
  { title: '配置名', key: 'config_name', minWidth: 130 },
  { title: '值', key: 'config_value', minWidth: 180 },
  { title: '类型', key: 'value_type_code', width: 90 },
  { title: '分组', key: 'group_code', width: 130 },
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
  { title: '更新时间', key: 'updated_at', minWidth: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 170,
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
              onPositiveClick: () => void onDelete(row.id),
            },
            {
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', tertiary: true, type: 'error' },
                  { default: () => '删除' },
                ),
              default: () => '确认删除该系统配置？',
            },
          ),
        ],
      }),
  },
]

function resetForm() {
  formModel.config_key = ''
  formModel.config_name = ''
  formModel.config_value = ''
  formModel.value_type_code = 'STRING'
  formModel.group_code = ''
  formModel.status_code = 'ENABLED'
  formModel.remark = ''
}

function buildPayload(): SaveSystemConfigPayload | null {
  const configKey = formModel.config_key.trim()
  const configName = formModel.config_name.trim()
  const configValue = formModel.config_value.trim()
  const groupCode = formModel.group_code.trim()
  if (!configKey || !configName || !configValue || !groupCode) return null
  return {
    config_key: configKey,
    config_name: configName,
    config_value: configValue,
    value_type_code: formModel.value_type_code,
    group_code: groupCode,
    status_code: formModel.status_code,
    remark: formModel.remark.trim(),
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await fetchSystemConfigs({
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

function onOpenEdit(row: AdminSystemConfig) {
  modalMode.value = 'edit'
  editingId.value = row.id
  formModel.config_key = row.config_key
  formModel.config_name = row.config_name
  formModel.config_value = row.config_value
  formModel.value_type_code = row.value_type_code
  formModel.group_code = row.group_code
  formModel.status_code = row.status_code
  formModel.remark = row.remark
  modalVisible.value = true
}

async function onSubmit() {
  const payload = buildPayload()
  if (!payload) {
    message.warning('请完整填写系统配置')
    return
  }
  saving.value = true
  try {
    if (modalMode.value === 'create') {
      await createSystemConfig(payload)
      message.success('系统配置已创建')
    } else if (editingId.value !== null) {
      await updateSystemConfig(editingId.value, payload)
      message.success('系统配置已更新')
    }
    modalVisible.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

async function onDelete(configId: number) {
  await deleteSystemConfig(configId)
  message.success('系统配置已删除')
  await loadList()
}

onMounted(() => {
  void loadList()
})
</script>

<template>
  <NCard title="系统配置（D14）">
    <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按配置键/名称/分组搜索" style="width: 280px" @keyup.enter="onSearch" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
      <NButton tertiary type="primary" @click="onOpenCreate">新增配置</NButton>
    </NSpace>

    <NDataTable :columns="columns" :data="rows" :loading="loading" :pagination="false" :scroll-x="1460" />

    <div style="margin-top: 14px">
      <AppPagination
        :page-no="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:page-no="onPageNoChange"
        @update:page-size="onPageSizeChange"
      />
    </div>

    <NModal v-model:show="modalVisible" preset="card" :title="modalMode === 'create' ? '新增系统配置' : '编辑系统配置'" style="width: 640px">
      <NForm label-placement="left" label-width="96">
        <NFormItem label="配置键" required>
          <NInput v-model:value="formModel.config_key" maxlength="80" />
        </NFormItem>
        <NFormItem label="配置名" required>
          <NInput v-model:value="formModel.config_name" maxlength="40" />
        </NFormItem>
        <NFormItem label="配置值" required>
          <NInput v-model:value="formModel.config_value" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </NFormItem>
        <NSpace :size="12" style="width: 100%">
          <NFormItem label="值类型" style="flex: 1">
            <NSelect v-model:value="formModel.value_type_code" :options="valueTypeOptions" />
          </NFormItem>
          <NFormItem label="分组" style="flex: 1">
            <NInput v-model:value="formModel.group_code" maxlength="30" />
          </NFormItem>
        </NSpace>
        <NFormItem label="状态">
          <NSelect v-model:value="formModel.status_code" :options="statusOptions" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput v-model:value="formModel.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
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
