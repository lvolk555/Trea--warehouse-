import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as userApi from '../api/user'

// 用户状态：登录信息、角色判断（教师/管理员）
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const user = ref(JSON.parse(localStorage.getItem('admin_user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isTeacher = computed(() => user.value?.role === 2)
  const isAdmin = computed(() => user.value?.role === 3)

  async function login(form) {
    const data = await userApi.login(form)
    token.value = data.token
    user.value = data.user
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('admin_user', JSON.stringify(data.user))
    return data.user
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
  }

  function updateUser(data) {
    user.value = { ...user.value, ...data }
    localStorage.setItem('admin_user', JSON.stringify(user.value))
  }

  return { token, user, isLoggedIn, isTeacher, isAdmin, login, logout, updateUser }
})
