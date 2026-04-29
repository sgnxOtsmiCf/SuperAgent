<template>
  <div class="app-container">
    <Sidebar />
    <router-view />
    <LoginDialog v-if="showLogin" @close="showLogin = false" />
  </div>
</template>

<script setup>
import { ref, provide, onMounted, onUnmounted } from 'vue'
import Sidebar from './components/Sidebar.vue'
import LoginDialog from './components/LoginDialog.vue'

const showLogin = ref(false)

provide('toggleLogin', () => {
  showLogin.value = !showLogin.value
})

// 🔑 监听全局登录事件
function handleShowLoginEvent() {
  showLogin.value = true
}

onMounted(() => {
  window.addEventListener('show-login-dialog', handleShowLoginEvent)
})

onUnmounted(() => {
  window.removeEventListener('show-login-dialog', handleShowLoginEvent)
})
</script>

<style lang="scss">
.app-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: #f7f8fa;
}
</style>
