import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as userApi from '../api/user'

// 用户状态：登录信息、角色判断
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('student_token') || '')
  const user = ref(JSON.parse(localStorage.getItem('student_user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isStudent = computed(() => user.value?.role === 1)

  async function login(form) {
    const data = await userApi.login(form)
    token.value = data.token
    user.value = data.user
    localStorage.setItem('student_token', data.token)
    localStorage.setItem('student_user', JSON.stringify(data.user))
    return data.user
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('student_token')
    localStorage.removeItem('student_user')
  }

  function updateUser(data) {
    user.value = { ...user.value, ...data }
    localStorage.setItem('student_user', JSON.stringify(user.value))
  }

  return { token, user, isLoggedIn, isStudent, login, logout, updateUser }
})
