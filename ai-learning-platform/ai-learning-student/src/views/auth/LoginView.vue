<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref(null)
const form = reactive({ username: '', password: '' })

const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' }
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const user = await userStore.login(form)
    if (user.role !== 1) {
      userStore.logout()
      message.warning('该账号不是学生账号，请使用管理端登录')
      return
    }
    message.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch (e) {
    message.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand">
        <div class="logo">AI</div>
        <h1>AI 辅助在线学习平台</h1>
        <p>学生端登录</p>
      </div>
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" size="large">
        <n-form-item path="username">
          <n-input v-model:value="form.username" placeholder="用户名" />
        </n-form-item>
        <n-form-item path="password">
          <n-input v-model:value="form.password" type="password" placeholder="密码" show-password-on="click" @keyup.enter="handleLogin" />
        </n-form-item>
        <n-button type="primary" block size="large" :loading="loading" @click="handleLogin">
          登 录
        </n-button>
      </n-form>
      <div class="footer">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </div>
      <div class="tip">测试账号：student1 / 123456</div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #ec4899 100%);
}
.auth-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}
.brand {
  text-align: center;
  margin-bottom: 28px;
}
.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand h1 {
  font-size: 20px;
  color: #1f2937;
}
.brand p {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}
.footer {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}
.footer a {
  color: #6366f1;
}
.tip {
  margin-top: 12px;
  text-align: center;
  font-size: 12px;
  color: #c4c7cf;
}
</style>
