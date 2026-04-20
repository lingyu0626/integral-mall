<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
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
  NSpin,
  NTag,
  NTree,
  useMessage,
  type DataTableColumns,
} from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import {
  assignRolePermissions,
  createRole,
  deleteRole,
  fetchPermissionTree,
  fetchRolePermissionIds,
  fetchRoles,
  updateRole,
  type SaveRolePayload,
} from '../api/admin/rbac'
import type { PermissionTreeNode, RoleItem, RoleStatus } from '../mock/rbac'

type RoleFormModel = {
  role_name: string
  role_code: string
  remark: string
  status_code: RoleStatus
}

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const rows = ref<RoleItem[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()

const roleModalVisible = ref(false)
const roleModalMode = ref<'create' | 'edit'>('create')
const editingRoleId = ref<number | null>(null)
const savingRole = ref(false)

const permissionModalVisible = ref(false)
const permissionLoading = ref(false)
const assigningRole = ref<RoleItem | null>(null)
const permissionTree = ref<PermissionTreeNode[]>([])
const checkedPermissionIds = ref<number[]>([])

const roleForm = reactive<RoleFormModel>({
  role_name: '',
  role_code: '',
  remark: '',
  status_code: 'ACTIVE',
})

const roleStatusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '禁用', value: 'DISABLED' },
]

const roleModalTitle = computed(() => (roleModalMode.value === 'create' ? '新增角色' : '编辑角色'))
const permissionModalTitle = computed(() => `权限分配 · ${assigningRole.value?.role_name ?? ''}`)

const columns: DataTableColumns<RoleItem> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '角色名称', key: 'role_name', minWidth: 140 },
  { title: '角色编码', key: 'role_code', minWidth: 140 },
  {
    title: '状态',
    key: 'status_code',
    width: 100,
    render: (row) =>
      h(
        NTag,
        { type: row.status_code === 'ACTIVE' ? 'success' : 'default' },
        { default: () => (row.status_code === 'ACTIVE' ? '启用' : '禁用') },
      ),
  },
  { title: '权限数量', key: 'permission_count', width: 100 },
  { title: '备注', key: 'remark', minWidth: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 260,
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
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: 'info',
              onClick: () => void onOpenPermissionModal(row),
            },
            { default: () => '分配权限' },
          ),
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onDeleteRole(row),
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
              default: () => '确认删除该角色？',
            },
          ),
        ],
      }),
  },
]

function resetRoleForm() {
  roleForm.role_name = ''
  roleForm.role_code = ''
  roleForm.remark = ''
  roleForm.status_code = 'ACTIVE'
}

async function loadRoles() {
  loading.value = true
  try {
    const data = await fetchRoles({
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
  void loadRoles()
}

function onPageNoChange(value: number) {
  updatePage(value)
  void loadRoles()
}

function onPageSizeChange(value: number) {
  updatePageSize(value)
  void loadRoles()
}

function onOpenCreate() {
  roleModalMode.value = 'create'
  editingRoleId.value = null
  resetRoleForm()
  roleModalVisible.value = true
}

function onOpenEdit(row: RoleItem) {
  roleModalMode.value = 'edit'
  editingRoleId.value = row.id
  roleForm.role_name = row.role_name
  roleForm.role_code = row.role_code
  roleForm.remark = row.remark
  roleForm.status_code = row.status_code
  roleModalVisible.value = true
}

async function onSubmitRole() {
  const roleName = roleForm.role_name.trim()
  const roleCode = roleForm.role_code.trim().toUpperCase()

  if (!roleName) {
    message.warning('请输入角色名称')
    return
  }
  if (!roleCode) {
    message.warning('请输入角色编码')
    return
  }

  const payload: SaveRolePayload = {
    role_name: roleName,
    role_code: roleCode,
    remark: roleForm.remark.trim(),
    status_code: roleForm.status_code,
  }

  savingRole.value = true
  try {
    if (roleModalMode.value === 'create') {
      await createRole(payload)
      message.success('角色已创建')
    } else if (editingRoleId.value) {
      await updateRole(editingRoleId.value, payload)
      message.success('角色已更新')
    }
    roleModalVisible.value = false
    await loadRoles()
  } finally {
    savingRole.value = false
  }
}

async function onDeleteRole(row: RoleItem) {
  await deleteRole(row.id)
  message.success('角色已删除')
  if (rows.value.length === 1 && pageNo.value > 1) {
    updatePage(pageNo.value - 1)
  }
  await loadRoles()
}

async function onOpenPermissionModal(row: RoleItem) {
  assigningRole.value = row
  permissionModalVisible.value = true
  permissionLoading.value = true
  try {
    const [tree, ids] = await Promise.all([fetchPermissionTree(), fetchRolePermissionIds(row.id)])
    permissionTree.value = tree
    checkedPermissionIds.value = ids
  } finally {
    permissionLoading.value = false
  }
}

function onCheckedPermissionKeys(keys: Array<string | number>) {
  checkedPermissionIds.value = keys
    .map((key) => Number(key))
    .filter((id) => Number.isFinite(id))
}

async function onSavePermissions() {
  if (!assigningRole.value) return
  await assignRolePermissions(assigningRole.value.id, checkedPermissionIds.value)
  message.success('权限已更新')
  permissionModalVisible.value = false
  await loadRoles()
}

onMounted(() => {
  void loadRoles()
})
</script>

<template>
  <NCard title="角色管理（D08）">
    <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按角色名称/编码搜索" style="width: 280px" @keyup.enter="onSearch" />
        <NButton type="primary" @click="onSearch">查询</NButton>
      </NSpace>
      <NButton type="primary" tertiary @click="onOpenCreate">新增角色</NButton>
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

    <NModal v-model:show="roleModalVisible" preset="card" :title="roleModalTitle" style="width: 520px">
      <NForm label-placement="left" label-width="96">
        <NFormItem label="角色名称" required>
          <NInput v-model:value="roleForm.role_name" maxlength="30" placeholder="如：订单审核" />
        </NFormItem>
        <NFormItem label="角色编码" required>
          <NInput v-model:value="roleForm.role_code" maxlength="30" placeholder="如：ORDER_AUDIT" />
        </NFormItem>
        <NFormItem label="状态">
          <NSelect v-model:value="roleForm.status_code" :options="roleStatusOptions" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput
            v-model:value="roleForm.remark"
            type="textarea"
            placeholder="请输入角色说明"
            :autosize="{ minRows: 2, maxRows: 4 }"
          />
        </NFormItem>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="roleModalVisible = false">取消</NButton>
          <NButton type="primary" :loading="savingRole" @click="onSubmitRole">保存</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="permissionModalVisible" preset="card" :title="permissionModalTitle" style="width: 680px">
      <NSpin :show="permissionLoading">
        <div style="max-height: 420px; overflow: auto; padding-right: 6px">
          <NTree
            block-line
            checkable
            cascade
            :data="permissionTree"
            :checked-keys="checkedPermissionIds"
            @update:checked-keys="onCheckedPermissionKeys"
          />
        </div>
      </NSpin>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="permissionModalVisible = false">取消</NButton>
          <NButton type="primary" @click="onSavePermissions">保存授权</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>
