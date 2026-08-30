<script setup>
import { reactive, ref, computed } from 'vue'
import { useMessage } from 'naive-ui'
import { useUserStore } from '../../stores/user'
import * as userApi from '../../api/user'
import { uploadFile } from '../../api/upload'

const message = useMessage()
const userStore = useUserStore()

const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const avatarInput = ref(null)
const avatarUploading = ref(false)

const profileForm = reactive({ nickname: '', avatar: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

if (userStore.user) {
  profileForm.nickname = userStore.user.nickname || ''
  profileForm.avatar = userStore.user.avatar || ''
}

const roleText = computed(() => (userStore.isStudent ? '学生' : '用户'))

const profileRules = {
  nickname: { required: true, message: '请输入昵称', trigger: 'blur' }
}

const passwordRules = {
  oldPassword: { required: true, message: '请输入原密码', trigger: 'blur' },
  newPassword: { required: true, min: 6, message: '新密码至少 6 位', trigger: 'blur' },
  confirmPassword: {
    required: true,
    trigger: 'blur',
    validator: (_rule, value) => (value === passwordForm.newPassword ? true : new Error('两次输入的新密码不一致'))
  }
}

// 头像上传：选取本地图片后上传并立即应用到资料表单
function chooseAvatar() {
  avatarInput.value?.click()
}

async function onAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  avatarUploading.value = true
  try {
    const res = await uploadFile(file)
    profileForm.avatar = res.url
    message.success('头像已上传，点击「保存资料」生效')
  } catch (err) {
    message.error(err.message)
  } finally {
    avatarUploading.value = false
    e.target.value = ''
  }
}

async function handleSaveProfile() {
  await profileFormRef.value.validate()
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
  <div>
    <h2 style="margin-bottom: 16px">个人中心</h2>

    <n-grid :cols="2" :x-gap="16" :y-gap="16">
      <!-- 账号信息 -->
      <n-grid-item>
        <n-card title="账号信息">
          <div class="info-head">
            <n-avatar :size="64" :src="userStore.user?.avatar" round>
              {{ (userStore.user?.nickname || userStore.user?.username || '?').slice(0, 1) }}
            </n-avatar>
            <div class="info-meta">
              <div class="name">{{ userStore.user?.nickname || userStore.user?.username }}</div>
              <div class="sub">@{{ userStore.user?.username }} · {{ roleText }}</div>
            </div>
          </div>
          <n-descriptions :column="1" size="small" style="margin-top: 16px">
            <n-descriptions-item label="用户名">{{ userStore.user?.username }}</n-descriptions-item>
            <n-descriptions-item label="角色">
              <n-tag type="info" size="small">{{ roleText }}</n-tag>
            </n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-grid-item>

      <!-- 修改资料 -->
      <n-grid-item>
        <n-card title="修改资料">
          <n-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-placement="top">
            <n-form-item label="昵称" path="nickname">
              <n-input v-model:value="profileForm.nickname" placeholder="请输入昵称" />
            </n-form-item>
            <n-form-item label="头像" path="avatar">
              <div class="avatar-edit">
                <n-avatar :size="64" round :src="profileForm.avatar">
                  {{ (userStore.user?.nickname || userStore.user?.username || '?').slice(0, 1) }}
                </n-avatar>
                <div class="avatar-actions">
                  <input ref="avatarInput" type="file" accept="image/*" style="display: none" @change="onAvatarChange" />
                  <n-space>
                    <n-button size="small" type="primary" ghost :loading="avatarUploading" @click="chooseAvatar">上传头像</n-button>
                    <n-button size="small" quaternary @click="profileForm.avatar = ''">移除</n-button>
                  </n-space>
                  <span class="muted">支持 jpg / png / gif / webp</span>
                  <n-input v-model:value="profileForm.avatar" size="small" placeholder="或填写头像图片 URL（选填）" style="margin-top: 8px" />
                </div>
              </div>
            </n-form-item>
            <n-button type="primary" @click="handleSaveProfile">保存资料</n-button>
          </n-form>
        </n-card>
      </n-grid-item>

      <!-- 修改密码 -->
      <n-grid-item span="2">
        <n-card title="修改密码">
          <n-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-placement="top" style="max-width: 420px">
            <n-form-item label="原密码" path="oldPassword">
              <n-input v-model:value="passwordForm.oldPassword" type="password" show-password-on="click" placeholder="请输入原密码" />
            </n-form-item>
            <n-form-item label="新密码" path="newPassword">
              <n-input v-model:value="passwordForm.newPassword" type="password" show-password-on="click" placeholder="请输入新密码（至少 6 位）" />
            </n-form-item>
            <n-form-item label="确认新密码" path="confirmPassword">
              <n-input v-model:value="passwordForm.confirmPassword" type="password" show-password-on="click" placeholder="请再次输入新密码" />
            </n-form-item>
            <n-button type="error" @click="handleChangePassword">修改密码</n-button>
          </n-form>
        </n-card>
      </n-grid-item>
    </n-grid>
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
.avatar-edit {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}
.avatar-actions {
  display: flex;
  flex-direction: column;
  flex: 1;
}
.muted {
  color: #9ca3af;
  font-size: 12px;
  margin-top: 6px;
}
</style>