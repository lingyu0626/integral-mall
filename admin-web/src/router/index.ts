import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/',
      component: AdminLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/dashboard',
        },
        {
          path: '/dashboard',
          name: 'Dashboard',
          component: () => import('../views/DashboardView.vue'),
        },
        {
          path: '/users',
          name: 'Users',
          component: () => import('../views/UsersView.vue'),
        },
        {
          path: '/products',
          name: 'Products',
          component: () => import('../views/ProductsView.vue'),
        },
        {
          path: '/orders',
          name: 'Orders',
          component: () => import('../views/OrdersView.vue'),
        },
        {
          path: '/order-export',
          name: 'OrderExport',
          component: () => import('../views/OrderExportView.vue'),
        },
        {
          path: '/wish-demands',
          name: 'WishDemands',
          component: () => import('../views/WishDemandsView.vue'),
        },
        {
          path: '/points',
          redirect: '/users',
        },
        {
          path: '/admin-users',
          name: 'AdminUsers',
          component: () => import('../views/AdminUsersView.vue'),
          meta: { requiresSuperAdmin: true },
        },
        {
          path: '/roles',
          name: 'Roles',
          component: () => import('../views/RolesView.vue'),
        },
        {
          path: '/permissions',
          name: 'Permissions',
          component: () => import('../views/PermissionsView.vue'),
        },
        {
          path: '/categories',
          name: 'Categories',
          component: () => import('../views/CategoriesView.vue'),
        },
        {
          path: '/recommend-slots',
          name: 'RecommendSlots',
          component: () => import('../views/RecommendSlotsView.vue'),
        },
        {
          path: '/product-attrs',
          name: 'ProductAttrs',
          component: () => import('../views/ProductAttrsView.vue'),
        },
        {
          path: '/backpack-assets',
          redirect: '/orders',
        },
        {
          path: '/group-resources',
          name: 'GroupResources',
          component: () => import('../views/GroupResourcesView.vue'),
        },
        {
          path: '/profile-group-entry',
          name: 'ProfileGroupEntry',
          component: () => import('../views/ProfileGroupEntryView.vue'),
        },
        {
          path: '/dict-center',
          name: 'DictCenter',
          component: () => import('../views/DictCenterView.vue'),
        },
        {
          path: '/system-configs',
          name: 'SystemConfigs',
          component: () => import('../views/SystemConfigsView.vue'),
        },
        {
          path: '/file-center',
          name: 'FileCenter',
          component: () => import('../views/FileCenterView.vue'),
        },
        {
          path: '/operation-logs',
          name: 'OperationLogs',
          component: () => import('../views/OperationLogsView.vue'),
        },
        {
          path: '/outbox-events',
          name: 'OutboxEvents',
          component: () => import('../views/OutboxEventsView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (authStore.token && !authStore.adminMe) {
    try {
      await authStore.bootstrap()
    } catch {
      // 避免初始化鉴权异常导致页面空白
    }
  }

  const requiresAuth = Boolean(to.meta.requiresAuth)
  const guestOnly = Boolean(to.meta.guestOnly)

  if (requiresAuth && !authStore.isAuthed) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (guestOnly && authStore.isAuthed) {
    return '/dashboard'
  }

  const requiresSuperAdmin = Boolean(to.meta.requiresSuperAdmin)
  if (requiresSuperAdmin) {
    const roles = authStore.adminMe?.roles ?? []
    const isSuperAdmin = roles.includes('超级管理员')
    if (!isSuperAdmin) {
      return '/dashboard'
    }
  }

  return true
})

export default router
