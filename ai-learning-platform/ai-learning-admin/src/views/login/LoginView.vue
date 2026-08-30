<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function handleLogin() {
  if (!form.username || !form.password) {
    message.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const user = await userStore.login(form)
    // 管理端仅允许教师与管理员登录
    if (user.role === 1) {
      userStore.logout()
      message.warning('该账号是学生账号，请使用学生端登录')
      return
    }
    message.success('登录成功')
    router.push('/')
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
        <p>管理端登录（教师 / 管理员）</p>
      </div>
      <a-form layout="vertical" @finish="handleLogin">
        <a-form-item>
          <a-input v-model:value="form.username" size="large" placeholder="用户名">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-input-password v-model:value="form.password" size="large" placeholder="密码" @pressEnter="handleLogin">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-button type="primary" block size="large" :loading="loading" @click="handleLogin">
          登 录
        </a-button>
      </a-form>
      <div class="tip">测试账号：teacher1 / 123456（教师） admin / 123456（管理员）</div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
}
.auth-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.brand {
  text-align: center;
  margin-bottom: 28px;
}
.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
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
.tip {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #c4c7cf;
}
</style>
