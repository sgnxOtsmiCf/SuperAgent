<template>
  <div class="archive-overlay" @click.self="$emit('close')">
    <div class="archive-dialog">
      <div class="dialog-header">
        <h3>归档会话</h3>
        <el-button text circle :icon="Close" @click="$emit('close')" />
      </div>

      <!-- 加载状态 -->
      <div v-if="chatStore.isLoadingArchives" class="loading-state">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>加载归档数据中...</p>
      </div>

      <!-- 归档会话列表 -->
      <div v-else class="sessions-container">
        <div
          v-for="session in archivedSessions"
          :key="session.sessionId"
          class="session-card"
          @click="$emit('select-session', session)"
        >
          <div class="session-header">
            <h4>{{ session.title || session.sessionName || '归档对话' }}</h4>
            <span class="time">{{ formatTime(session.lastActive || session.createdAt) }}</span>
          </div>
          <div class="session-meta">
            <el-tag size="small" type="info" effect="plain">已归档</el-tag>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="archivedSessions.length === 0" class="empty-state">
          <el-icon :size="48" color="#d0d0d0"><FolderChecked /></el-icon>
          <p>暂无归档会话</p>
          <p class="hint">在历史对话中点击"..."菜单选择"归档"即可归档会话</p>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore" class="load-more" @click="loadMore">
          <span>加载更多</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Close, Loading, FolderChecked } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'

const props = defineProps({
  appKey: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['close', 'select-session'])

const chatStore = useChatStore()
const currentPage = ref(1)
const pageSize = 20

const archivedSessions = computed(() => chatStore.archivedSessions[props.appKey] || [])
const hasMore = ref(false)

onMounted(async () => {
  await loadArchives()
})

async function loadArchives() {
  await chatStore.fetchArchivedSessions(props.appKey, currentPage.value, pageSize)
}

async function loadMore() {
  currentPage.value++
  await chatStore.fetchArchivedSessions(props.appKey, currentPage.value, pageSize)
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date

  if (diff < 86400000) {
    return `今天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }

  if (diff < 604800000) {
    const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    return `${days[date.getDay()]} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }

  return `${date.getMonth() + 1}月${date.getDate()}日`
}
</script>

<style lang="scss" scoped>
.archive-overlay {
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

.archive-dialog {
  background-color: #fff;
  border-radius: 16px;
  width: 600px;
  max-width: 90vw;
  height: 70vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.dialog-header {
  padding: 24px 28px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h3 {
    font-size: 22px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }
}

.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #999;

  p {
    font-size: 14px;
    margin: 0;
  }
}

.sessions-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;

  .session-card {
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    padding: 16px;
    margin-bottom: 12px;
    cursor: pointer;
    transition: all 0.2s;
    background-color: #fafbfc;

    &:hover {
      border-color: #faad14;
      box-shadow: 0 2px 8px rgba(250, 173, 20, 0.15);
      transform: translateY(-1px);
    }

    .session-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      h4 {
        font-size: 15px;
        font-weight: 600;
        color: #1a1a1a;
        margin: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 350px;
      }

      .time {
        font-size: 13px;
        color: #999;
        white-space: nowrap;
      }
    }

    .session-meta {
      display: flex;
      align-items: center;
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 12px;

  p {
    font-size: 14px;
    color: #999;
    margin: 0;
  }

  .hint {
    font-size: 12px;
    color: #bbb;
  }
}

.load-more {
  text-align: center;
  padding: 12px;
  cursor: pointer;
  color: #1890ff;
  font-size: 14px;

  &:hover {
    color: #40a9ff;
  }
}
</style>
