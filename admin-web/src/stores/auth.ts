import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getToken, clearToken } from '../api/auth-storage'
import { adminFetchMe, adminLogin, adminLogout, persistAdminToken, type AdminLoginPayload } from '../api/admin/auth'
import type { AdminMe } from '../mock/admin'

export const useAuthStore = defineStore('admin-auth', () => {
  const token = ref<string | null>(getToken('admin'))
  const adminMe = ref<AdminMe | null>(null)

  const isAuthed = computed(() => Boolean(token.value))

  async function login(payload: AdminLoginPayload) {
    const data = await adminLogin(payload)
    token.value = data.access_token
    persistAdminToken(data.access_token)
    adminMe.value = data.admin_user ?? (await adminFetchMe())
  }

  async function bootstrap() {
    token.value = getToken('admin')
    if (!token.value) return
    try {
      adminMe.value = await adminFetchMe()
    } catch {
      token.value = null
      adminMe.value = null
      clearToken('admin')
    }
  }

  async function logout() {
    await adminLogout()
    token.value = null
    adminMe.value = null
    clearToken('admin')
  }

  return {
    token,
    adminMe,
    isAuthed,
    login,
    bootstrap,
    logout,
  }
})
