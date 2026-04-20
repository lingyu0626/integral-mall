<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute, RouterView } from 'vue-router'
import { NButton, NLayout, NLayoutContent, NLayoutHeader, NLayoutSider, NMenu, NSpace, NText, useMessage, type MenuOption } from 'naive-ui'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const authStore = useAuthStore()
const isSuperAdmin = computed(() => (authStore.adminMe?.roles ?? []).includes('超级管理员'))

const menuOptions = computed<MenuOption[]>(() => [
  {
    label: '数据看板',
    key: 'group-dashboard',
    children: [
      { label: '运营看板', key: '/dashboard' },
    ],
  },
  {
    label: '首页与商品',
    key: 'group-home-goods',
    children: [
      { label: '首页轮播图', key: '/recommend-slots' },
      { label: '分类管理', key: '/categories' },
      { label: '商品管理', key: '/products' },
    ],
  },
  {
    label: '兑换与履约',
    key: 'group-exchange',
    children: [
      { label: '订单管理', key: '/orders' },
      { label: '订单导出', key: '/order-export' },
      { label: '意向留言', key: '/wish-demands' },
    ],
  },
  {
    label: '用户与权限',
    key: 'group-user',
    children: [
      { label: '用户管理', key: '/users' },
      { label: '加入群聊设置', key: '/profile-group-entry' },
      ...(isSuperAdmin.value ? [{ label: '管理员管理', key: '/admin-users' }] : []),
    ],
  },
])

const currentMenu = computed(() => route.path)
const adminName = computed(() => authStore.adminMe?.display_name || '管理员')
const expandedMenuKeys = ['group-dashboard', 'group-home-goods', 'group-exchange', 'group-user']

function onSelect(key: string | number) {
  if (typeof key !== 'string' || !key.startsWith('/')) return
  void router.push(key)
}

async function onLogout() {
  await authStore.logout()
  message.success('已退出登录')
  void router.replace('/login')
}
</script>

<template>
  <NLayout has-sider class="admin-layout">
    <NLayoutSider bordered collapse-mode="width" :collapsed-width="64" :width="220" show-trigger>
      <div class="brand">碎片商城 · 管理端</div>
      <NMenu :options="menuOptions" :value="currentMenu" :default-expanded-keys="expandedMenuKeys" @update:value="onSelect" />
    </NLayoutSider>

    <NLayout>
      <NLayoutHeader bordered class="top-header">
        <NSpace justify="end" align="center" style="width: 100%">
          <NSpace align="center">
            <NText>{{ adminName }}</NText>
            <NButton size="small" tertiary @click="onLogout">退出</NButton>
          </NSpace>
        </NSpace>
      </NLayoutHeader>

      <NLayoutContent content-style="padding: 18px;">
        <RouterView v-slot="{ Component }">
          <component :is="Component" />
        </RouterView>
      </NLayoutContent>
    </NLayout>
  </NLayout>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.brand {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--n-border-color);
  font-weight: 700;
  letter-spacing: 0.3px;
}

.top-header {
  display: flex;
  align-items: center;
  padding: 0 18px;
  height: 56px;
}
</style>
