<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { register } from '../../api/user'

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const formRef = ref(null)
const form = reactive({ username: '', password: '', confirmPassword: '', nickname: '' })

const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' },
  confirmPassword: {
    required: true,
    trigger: 'blur',
    validator: (_rule, value) => (value === form.password ? true : new Error('两次输入的密码不一致'))
  }
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register({ username: form.username, password: form.password, nickname: form.nickname || undefined })
    message.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    message.error(e.message || '注册失败')
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
        <h1>注册学生账号</h1>
        <p>注册即赠送 100 积分</p>
      </div>
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" size="large">
        <n-form-item path="username">
          <n-input v-model:value="form.username" placeholder="用户名（3-50 位）" />
        </n-form-item>
        <n-form-item path="nickname">
          <n-input v-model:value="form.nickname" placeholder="昵称（选填）" />
        </n-form-item>
        <n-form-item path="password">
          <n-input v-model:value="form.password" type="password" placeholder="密码（至少 6 位）" show-password-on="click" />
        </n-form-item>
        <n-form-item path="confirmPassword">
          <n-input v-model:value="form.confirmPassword" type="password" placeholder="确认密码" show-password-on="click" />
        </n-form-item>
        <n-button type="primary" block size="large" :loading="loading" @click="handleRegister">
          注 册
        </n-button>
      </n-form>
      <div class="footer">
        已有账号？
        <router-link to="/login">去登录</router-link>
      </div>
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
  margin-bottom: 24px;
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
</style>
