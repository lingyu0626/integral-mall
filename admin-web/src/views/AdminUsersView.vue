<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { NButton, NCard, NDataTable, NInput, NPopconfirm, NSpace, NTag, useMessage, type DataTableColumns } from 'naive-ui'
import AppPagination from '../components/AppPagination.vue'
import { usePagination } from '../composables/usePagination'
import { fetchAdminOperatorPassword, fetchAdminOperators, resetAdminOperatorPassword, updateAdminOperatorStatus } from '../api/admin/rbac'
import type { AdminOperator } from '../mock/rbac'
import { useAuthStore } from '../stores/auth'

const message = useMessage()
const authStore = useAuthStore()
const loading = ref(false)
const keyword = ref('')
const rows = ref<AdminOperator[]>([])
const total = ref(0)
const { pageNo, pageSize, query, updatePage, updatePageSize } = usePagination()
const canViewPassword = computed(() => (authStore.adminMe?.roles ?? []).includes('超级管理员'))

const columns: DataTableColumns<AdminOperator> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '账号', key: 'username', minWidth: 120 },
  { title: '姓名', key: 'display_name', minWidth: 120 },
  { title: '手机号', key: 'phone', minWidth: 130 },
  {
    title: '角色',
    key: 'roles',
    minWidth: 180,
    render: (row) => row.roles.join(' / '),
  },
  {
    title: '状态',
    key: 'status_code',
    minWidth: 100,
    render: (row) =>
      h(
        NTag,
        { type: row.status_code === 'ACTIVE' ? 'success' : 'warning' },
        { default: () => (row.status_code === 'ACTIVE' ? '正常' : '冻结') },
      ),
  },
  { title: '最近登录', key: 'last_login_at', minWidth: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 300,
    render: (row) =>
      h(NSpace, { size: 8 }, {
        default: () => [
          h(
            NPopconfirm,
            {
              onPositiveClick: () => void onToggleStatus(row),
            },
            {
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', tertiary: true, type: row.status_code === 'ACTIVE' ? 'warning' : 'primary' },
                  { default: () => (row.status_code === 'ACTIVE' ? '冻结' : '解冻') },
                ),
              default: () => `确认${row.status_code === 'ACTIVE' ? '冻结' : '解冻'}该管理员？`,
            },
          ),
          h(
            NButton,
            { size: 'small', tertiary: true, type: 'info', onClick: () => void onResetPassword(row) },
            { default: () => '重置密码' },
          ),
          ...(canViewPassword.value
            ? [
                h(
                  NButton,
                  { size: 'small', tertiary: true, type: 'primary', onClick: () => void onViewPassword(row) },
                  { default: () => '查看密码' },
                ),
              ]
            : []),
        ],
      }),
  },
]

async function loadList() {
  loading.value = true
  try {
    const data = await fetchAdminOperators({
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

async function onToggleStatus(row: AdminOperator) {
  const next = row.status_code === 'ACTIVE' ? 'FROZEN' : 'ACTIVE'
  await updateAdminOperatorStatus(row.id, next)
  message.success('状态已更新')
  await loadList()
}

async function onResetPassword(row: AdminOperator) {
  const tempPwd = await resetAdminOperatorPassword(row.id)
  message.success(`临时密码：${tempPwd}`)
}

async function onViewPassword(row: AdminOperator) {
  if (!canViewPassword.value) {
    message.error('仅超级管理员可查看密码')
    return
  }
  const pwd = await fetchAdminOperatorPassword(row.id)
  if (!pwd) {
    message.warning('未查询到密码')
    return
  }
  message.success(`当前密码：${pwd}`)
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

onMounted(() => {
  void loadList()
})
</script>

<template>
  <NCard title="管理员管理（D08）">
    <NSpace style="margin-bottom: 12px">
      <NInput v-model:value="keyword" clearable placeholder="按账号/姓名/手机号搜索" style="width: 260px" @keyup.enter="onSearch" />
      <NButton type="primary" @click="onSearch">查询</NButton>
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
  </NCard>
</template>
