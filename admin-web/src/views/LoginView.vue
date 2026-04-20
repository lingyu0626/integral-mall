<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NCard, NForm, NFormItem, NInput, useMessage } from 'naive-ui'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const message = useMessage()

const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

async function onSubmit() {
  if (!form.username || !form.password) {
    message.warning('请输入账号和密码')
    return
  }

  loading.value = true
  try {
    await authStore.login({ username: form.username.trim(), password: form.password })
    message.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    void router.replace(redirect)
  } catch (error) {
    const msg = error instanceof Error ? error.message : '登录失败'
    message.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <span class="bg-glow bg-glow-left"></span>
    <span class="bg-glow bg-glow-right"></span>

    <NCard class="login-card" :bordered="false">
      <div class="login-header">
        <div class="brand-badge">SC</div>
        <div>
          <h2 class="title">碎片商城管理后台</h2>
          <p class="login-tip">真实生产环境登录</p>
        </div>
      </div>

      <NForm label-placement="top" @submit.prevent="onSubmit">
        <NFormItem label="账号">
          <NInput v-model:value="form.username" placeholder="请输入管理员账号" />
        </NFormItem>
        <NFormItem label="密码">
          <NInput
            v-model:value="form.password"
            type="password"
            show-password-on="click"
            placeholder="请输入管理员密码"
          />
        </NFormItem>
        <NButton type="primary" attr-type="submit" block :loading="loading">登录</NButton>
      </NForm>

      <p class="login-footer">© 2026 碎片商城后台系统</p>
    </NCard>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: radial-gradient(circle at 15% 20%, #dff4ff 0%, #f5f8ff 35%, #eef1f7 100%);
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(460px, 100%);
  border-radius: 16px;
  box-shadow: 0 18px 40px rgba(31, 45, 61, 0.12);
}

.login-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.brand-badge {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #18a058 0%, #36ad6a 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
}

.title {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
}

.bg-glow {
  position: absolute;
  width: 380px;
  height: 380px;
  border-radius: 50%;
  filter: blur(56px);
  opacity: 0.35;
}

.bg-glow-left {
  top: -80px;
  left: -90px;
  background: #8ec5ff;
}

.bg-glow-right {
  right: -100px;
  bottom: -120px;
  background: #9de6bc;
}

.login-tip {
  margin: 2px 0 0;
  color: #6b7280;
  line-height: 1.6;
}

.login-footer {
  margin: 12px 0 2px;
  color: #9ca3af;
  font-size: 12px;
  text-align: center;
}
</style>
