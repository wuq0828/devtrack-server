import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, feishuLogin as feishuLoginApi } from '@/api/auth'
import type { LoginRequest, LoginResult } from '@/types'

const TOKEN_KEY = 'devtrack_token'
const USER_KEY = 'devtrack_user'

interface StoredUser {
  userId: number
  username: string
  admin: boolean
}

function loadUser(): StoredUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as StoredUser
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<StoredUser | null>(loadUser())

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => user.value?.username || '')

  function setSession(result: LoginResult) {
    token.value = result.token
    user.value = {
      userId: result.userId,
      username: result.username,
      admin: result.admin,
    }
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  function clearSession() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  async function login(payload: LoginRequest) {
    const result = await loginApi(payload)
    setSession(result)
    return result
  }

  async function loginByFeishu(code: string) {
    const result = await feishuLoginApi(code)
    setSession(result)
    return result
  }

  function logout() {
    clearSession()
  }

  return { token, user, isLoggedIn, username, login, loginByFeishu, logout, clearSession }
})
