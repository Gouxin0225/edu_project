import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export interface UserInfo {
  id: number
  realName: string
  role: 'ADMIN' | 'TEACHER' | 'ASSISTANT' | 'STUDENT'
  mustChangePassword?: boolean
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(
    JSON.parse(localStorage.getItem('userInfo') || 'null')
  )

  async function login(username: string, password: string) {
    clear()
    const res = await request.post<any, { code: number; data: { token: string } & UserInfo }>(
      '/auth/login',
      { username, password }
    )
    const { token: newToken, id, realName, role, mustChangePassword } = res.data
    token.value = newToken
    userInfo.value = { id, realName, role, mustChangePassword }
    localStorage.setItem('token', newToken)
    localStorage.setItem('userInfo', JSON.stringify({ id, realName, role, mustChangePassword }))
  }

  async function register(data: { realName: string; phone: string; schoolName: string; password: string }) {
    clear()
    const res = await request.post<any, { code: number; data: { token: string } & UserInfo }>(
      '/auth/register',
      data
    )
    const { token: newToken, id, realName, role, mustChangePassword } = res.data
    token.value = newToken
    userInfo.value = { id, realName, role, mustChangePassword }
    localStorage.setItem('token', newToken)
    localStorage.setItem('userInfo', JSON.stringify({ id, realName, role, mustChangePassword }))
  }

  async function logout() {
    try {
      await request.post('/auth/logout')
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }

  function clear() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, login, register, logout, clear }
})
