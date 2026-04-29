import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useChatStore } from './chat'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))
  const isLoggedIn = ref(!!token.value)

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
    isLoggedIn.value = !!newToken
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function logout() {
    // 🔑🔑🔑 关键：登出时清理 chat store 的会话数据
    try {
      const chatStore = useChatStore()
      chatStore.clearAllSessions()
    } catch (e) {
      console.warn('[UserStore] 清理 chat store 失败:', e)
    }

    // 清理用户数据
    token.value = ''
    userInfo.value = {}
    isLoggedIn.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    setUserInfo,
    logout
  }
})
