<script setup>
import { reactive, ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '../../stores/user'
import * as userApi from '../../api/user'
import { uploadFile } from '../../api/upload'

const userStore = useUserStore()
const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const avatarUploading = ref(false)

const profileForm = reactive({ nickname: '', avatar: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 首次进入时用当前用户信息回填
if (userStore.user) {
  profileForm.nickname = userStore.user.nickname || ''
  profileForm.avatar = userStore.user.avatar || ''
}

const roleText = computed(() => {
  const map = { 1: '学生', 2: '教师', 3: '管理员' }
  return map[userStore.user?.role] || '未知'
})

// 头像上传：上传成功后立即应用到当前账号
async function beforeAvatarUpload(file) {
  avatarUploading.value = true
  try {
    const res = await uploadFile(file)
    const updated = await userApi.updateProfile({ avatar: res.url })
    profileForm.avatar = res.url
    userStore.updateUser(updated)
    message.success('头像已更新')
  } catch (e) {
    message.error(e.message)
  } finally {
    avatarUploading.value = false
  }
  return false
}

async function handleSaveProfile() {
  try {
    const updated = await userApi.updateProfile({
      nickname: profileForm.nickname || undefined,
      avatar: profileForm.avatar || undefined
    })
    userStore.updateUser(updated)
    message.success('资料已更新')
  } catch (e) {
    message.error(e.message)
  }
}

async function handleChangePassword() {
  await passwordFormRef.value.validate()
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.error('两次输入的新密码不一致')
    return
  }
  try {
    await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    message.success('密码已修改，请重新登录')
    setTimeout(() => {
      userStore.logout()
      window.location.href = '/login'
    }, 800)
  } catch (e) {
    message.error(e.message)
  }
}
</script>

<template>
  <div class="profile-page">
    <a-row :gutter="16">
      <!-- 账号信息 -->
      <a-col :xs="24" :sm="10">
        <a-card :bordered="false" title="账号信息">
          <div class="info-head">
            <a-avatar :size="64" :src="userStore.user?.avatar">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <div class="info-meta">
              <div class="name">{{ userStore.user?.nickname || userStore.user?.username }}</div>
              <div class="sub">@{{ userStore.user?.username }} · {{ roleText }}</div>
            </div>
          </div>
          <a-descriptions :column="1" size="small" style="margin-top: 16px">
            <a-descriptions-item label="用户名">{{ userStore.user?.username }}</a-descriptions-item>
            <a-descriptions-item label="角色">
              <a-tag :color="userStore.isAdmin ? 'blue' : 'green'">{{ roleText }}</a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <!-- 修改资料 -->
      <a-col :xs="24" :sm="14">
        <a-card :bordered="false" title="修改资料">
          <a-form ref="profileFormRef" :model="profileForm" layout="vertical" style="max-width: 420px">
            <a-form-item label="昵称" name="nickname">
              <a-input v-model:value="profileForm.nickname" placeholder="请输入昵称" />
            </a-form-item>
            <a-form-item label="头像" name="avatar">
              <a-space direction="vertical" style="width: 100%">
                <div class="avatar-row">
                  <a-upload :show-upload-list="false" :before-upload="beforeAvatarUpload" accept="image/*">
                    <a-button :loading="avatarUploading"><UploadOutlined /> 上传头像</a-button>
                  </a-upload>
                  <a-avatar :size="48" :src="profileForm.avatar">
                    <template #icon><UserOutlined /></template>
                  </a-avatar>
                </div>
                <a-input v-model:value="profileForm.avatar" placeholder="或填写头像图片 URL" />
              </a-space>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleSaveProfile">保存资料</a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- 修改密码 -->
        <a-card :bordered="false" title="修改密码" style="margin-top: 16px">
          <a-form ref="passwordFormRef" :model="passwordForm" layout="vertical" style="max-width: 420px">
            <a-form-item
              label="原密码"
              name="oldPassword"
              :rules="[{ required: true, message: '请输入原密码' }]"
            >
              <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入原密码" />
            </a-form-item>
            <a-form-item
              label="新密码"
              name="newPassword"
              :rules="[{ required: true, min: 6, message: '新密码至少 6 位' }]"
            >
              <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码（至少 6 位）" />
            </a-form-item>
            <a-form-item
              label="确认新密码"
              name="confirmPassword"
              :rules="[{ required: true, message: '请再次输入新密码' }]"
            >
              <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" danger @click="handleChangePassword">
                <LockOutlined /> 修改密码
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.info-head {
  display: flex;
  align-items: center;
  gap: 16px;
}
.name {
  font-size: 18px;
  font-weight: 600;
}
.sub {
  color: #9ca3af;
  font-size: 13px;
  margin-top: 4px;
}
.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>