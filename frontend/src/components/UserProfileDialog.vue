<template>
  <div class="profile-overlay" @click.self="$emit('close')">
    <div class="profile-dialog">
      <div class="dialog-header">
        <h3>用户画像</h3>
        <el-button text circle :icon="Close" @click="$emit('close')" />
      </div>

      <div class="profile-content">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!profileData.profiles || profileData.profiles.length === 0" class="empty-state">
          <el-icon :size="48" color="#ccc"><User /></el-icon>
          <p>暂无用户画像数据</p>
          <span class="tip">系统将根据您的对话自动学习并生成画像</span>
        </div>

        <!-- 画像列表 -->
        <div v-else class="profile-list">
          <div
            v-for="item in profileData.profiles"
            :key="item.key"
            class="profile-item"
          >
            <div class="profile-header">
              <span class="profile-key">{{ item.key }}</span>
              <span class="profile-time">{{ item.updatedTimeStr }}</span>
            </div>
            <div class="profile-value">{{ item.value }}</div>
          </div>
        </div>
      </div>

      <div class="dialog-footer">
        <el-button type="primary" size="large" @click="$emit('close')">关闭</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Close, Loading, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userProfileApi } from '@/api/userProfile'

const emit = defineEmits(['close'])
const userStore = useUserStore()

const loading = ref(false)
const profileData = ref({
  userId: null,
  profiles: []
})

async function loadUserProfile() {
  if (!userStore.userInfo.userId) {
    ElMessage.warning('请先登录')
    return
  }

  loading.value = true
  try {
    const res = await userProfileApi.getUserProfile(userStore.userInfo.userId)
    if (res.code === 200 && res.data) {
      profileData.value = res.data
    } else {
      ElMessage.error(res.message || '获取用户画像失败')
    }
  } catch (error) {
    console.error('获取用户画像失败:', error)
    ElMessage.error('获取用户画像失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadUserProfile()
})
</script>

<style lang="scss" scoped>
.profile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.profile-dialog {
  background-color: #fff;
  border-radius: 16px;
  width: 520px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 24px 16px;
  border-bottom: 1px solid #f0f0f0;

  h3 {
    font-size: 20px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }
}

.profile-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  min-height: 200px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
  color: #999;

  .is-loading {
    animation: rotate 1s linear infinite;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
  text-align: center;

  p {
    font-size: 16px;
    color: #666;
    margin: 0;
  }

  .tip {
    font-size: 13px;
    color: #999;
  }
}

.profile-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.profile-item {
  background-color: #f7f8fa;
  border-radius: 10px;
  padding: 16px;
  transition: all 0.2s;

  &:hover {
    background-color: #eef0f2;
  }

  .profile-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;

    .profile-key {
      font-size: 14px;
      font-weight: 600;
      color: #1890ff;
      background-color: rgba(24, 144, 255, 0.1);
      padding: 2px 10px;
      border-radius: 4px;
    }

    .profile-time {
      font-size: 12px;
      color: #999;
    }
  }

  .profile-value {
    font-size: 14px;
    color: #333;
    line-height: 1.6;
    word-break: break-word;
  }
}

.dialog-footer {
  padding: 16px 24px 24px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
