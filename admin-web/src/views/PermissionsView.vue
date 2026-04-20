<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NGrid,
  NGridItem,
  NInput,
  NSpace,
  NTag,
  NTree,
  useMessage,
  type DataTableColumns,
} from 'naive-ui'
import { fetchPermissions, fetchPermissionTree } from '../api/admin/rbac'
import type { PermissionItem, PermissionTreeNode } from '../mock/rbac'

const message = useMessage()
const loading = ref(false)
const keyword = ref('')
const permissionList = ref<PermissionItem[]>([])
const permissionTree = ref<PermissionTreeNode[]>([])
const selectedTreeKeys = ref<Array<string | number>>([])

const selectedPermissionId = computed<number | null>(() => {
  const first = selectedTreeKeys.value[0]
  if (first === undefined || first === null) return null
  const id = Number(first)
  return Number.isFinite(id) ? id : null
})

const hasFilter = computed(() => Boolean(keyword.value.trim()) || selectedPermissionId.value !== null)

const columns: DataTableColumns<PermissionItem> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '模块', key: 'module_name', width: 110 },
  { title: '权限名称', key: 'permission_name', minWidth: 160 },
  { title: '权限编码', key: 'permission_code', minWidth: 180 },
  {
    title: '方法',
    key: 'method',
    width: 90,
    render: (row) => {
      const type = row.method === 'GET' ? 'info' : row.method === 'POST' ? 'success' : row.method === 'PUT' ? 'warning' : 'error'
      return h(NTag, { type, size: 'small' }, { default: () => row.method })
    },
  },
  { title: '路径', key: 'path', minWidth: 260 },
  {
    title: '上级ID',
    key: 'parent_id',
    width: 90,
    render: (row) => (row.parent_id === null ? '-' : String(row.parent_id)),
  },
]

const filteredRows = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  const scopeIds = selectedPermissionIds.value
  return permissionList.value.filter((item) => {
    if (scopeIds && !scopeIds.has(item.id)) {
      return false
    }

    if (!key) return true
    return [item.permission_name, item.permission_code, item.module_name, item.path, item.method]
      .some((field) => field.toLowerCase().includes(key))
  })
})

const selectedPermissionIds = computed<Set<number> | null>(() => {
  if (selectedPermissionId.value === null) return null
  const queue: number[] = [selectedPermissionId.value]
  const result = new Set<number>()

  while (queue.length > 0) {
    const id = queue.shift()
    if (id === undefined || result.has(id)) continue
    result.add(id)
    permissionList.value.forEach((item) => {
      if (item.parent_id === id) {
        queue.push(item.id)
      }
    })
  }

  return result
})

async function loadData() {
  loading.value = true
  try {
    const [tree, list] = await Promise.all([fetchPermissionTree(), fetchPermissions()])
    permissionTree.value = tree
    permissionList.value = list
  } catch {
    message.error('权限数据加载失败')
  } finally {
    loading.value = false
  }
}

function onTreeSelect(keys: Array<string | number>) {
  selectedTreeKeys.value = keys
}

function onResetFilters() {
  keyword.value = ''
  selectedTreeKeys.value = []
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <NCard title="权限点管理（D08）">
    <NSpace style="margin-bottom: 12px" justify="space-between" align="center">
      <NSpace>
        <NInput v-model:value="keyword" clearable placeholder="按权限名称/编码/路径搜索" style="width: 320px" />
        <NButton v-if="hasFilter" @click="onResetFilters">清空筛选</NButton>
      </NSpace>
      <NButton tertiary type="primary" :loading="loading" @click="loadData">刷新</NButton>
    </NSpace>

    <NGrid :cols="24" :x-gap="12">
      <NGridItem :span="7">
        <NCard size="small" title="权限树" content-style="max-height: 560px; overflow: auto;">
          <NTree
            block-line
            selectable
            expand-on-click
            :data="permissionTree"
            :selected-keys="selectedTreeKeys"
            @update:selected-keys="onTreeSelect"
          />
        </NCard>
      </NGridItem>

      <NGridItem :span="17">
        <NCard size="small" :title="`权限列表（${filteredRows.length}）`">
          <NDataTable
            :columns="columns"
            :data="filteredRows"
            :loading="loading"
            :pagination="false"
            :scroll-x="980"
            max-height="560"
          />
        </NCard>
      </NGridItem>
    </NGrid>
  </NCard>
</template>
