<template>
  <div class="history-overlay" @click.self="$emit('close')">
    <div class="history-dialog">
      <div class="dialog-header">
        <h3>历史对话</h3>
        <el-button text circle :icon="Close" @click="$emit('close')" />
      </div>

      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="searchText"
          placeholder="搜索历史对话"
          prefix-icon="Search"
          clearable
          size="large"
        />
      </div>

      <!-- 历史会话列表 -->
      <div class="sessions-container">
        <div 
          v-for="session in filteredSessions" 
          :key="session.sessionId"
          class="session-card"
          @click="$emit('select-session', session)"
        >
          <div class="session-header">
            <h4>{{ session.title || session.sessionName || '新对话' }}</h4>
            <span class="time">{{ formatTime(session.lastActive || session.createdAt) }}</span>
          </div>
          
          <p class="preview">
            {{ getPreview(session) }}
          </p>

          <!-- 附件信息（如果有） -->
          <div v-if="session.attachments && session.attachments.length > 0" class="attachments">
            <div 
              v-for="(file, index) in session.attachments.slice(0, 4)" 
              :key="index"
              class="attachment-item"
            >
              <el-icon><Document /></el-icon>
              <span>{{ file.name }}</span>
              <span class="size">{{ formatFileSize(file.size) }}</span>
            </div>
            <div v-if="session.attachments.length > 4" class="more-attachments">
              +{{ session.attachments.length - 4 }} 更多文件
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="filteredSessions.length === 0" class="empty-state">
          <el-icon :size="48" color="#d0d0d0"><ChatDotRound /></el-icon>
          <p>暂无历史对话</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Close, Search, ChatDotRound, Document } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'
import { sessionApi } from '@/api/session'
import { ElMessage } from 'element-plus'
import { logger } from '@/utils/logger'

const props = defineProps({
  appKey: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['close', 'select-session'])

const chatStore = useChatStore()
const searchText = ref('')
const allSessions = ref([])
const isLoading = ref(false)

// 过滤后的会话列表
const filteredSessions = computed(() => {
  if (!searchText.value) {
    return allSessions.value
  }
  
  const keyword = searchText.value.toLowerCase()
  return allSessions.value.filter(session => {
    const title = (session.title || session.sessionName || '').toLowerCase()
    return title.includes(keyword)
  })
})

onMounted(() => {
  loadAllSessions()
})

async function loadAllSessions() {
  isLoading.value = true
  
  try {
 // 调用后端API获取所有会话
    const agentId = chatStore.appConfig[props.appKey]?.agentId
    if (!agentId) {
      logger.error('[HistoryDialog] 未找到agentId:', props.appKey)
 // 回退到从store获取
      allSessions.value = chatStore.getSessionHistory(props.appKey)
      return
    }
    
    const res = await sessionApi.getAllSessions(agentId)
    
    if (res.code === 200 && Array.isArray(res.data)) {
 // 转换后端数据格式为前端格式
      allSessions.value = res.data.map(session => ({
        sessionId: session.sessionId,
        agentId: session.agentId || 1,
        title: session.sessionName || '历史对话',
        createdAt: session.lastActive,
        lastActive: session.lastActive,
        isPinned: !!session.isTop,
        isTop: session.isTop ? Number(session.isTop) : null,
        messages: [] // 历史对话框不需要显示消息内容
      }))
    } else {
 // 回退到从store获取
      allSessions.value = chatStore.getSessionHistory(props.appKey)
    }
  } catch (error) {
    logger.error('[HistoryDialog] 加载所有会话失败:', error)
    ElMessage.error('加载历史会话失败')
 // 回退到从store获取
    allSessions.value = chatStore.getSessionHistory(props.appKey)
  } finally {
    isLoading.value = false
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
 // 小于24小时显示"今天 HH:mm"
  if (diff < 86400000) {
    return `今天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }
  
 // 小于7天显示"星期X HH:mm"
  if (diff < 604800000) {
    const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    return `${days[date.getDay()]} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }
  
 // 其他情况显示完整日期
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

function getPreview(session) {
  if (!session.messages || session.messages.length === 0) {
    return '暂无消息内容'
  }
  
 // 获取第一条用户消息或最后一条AI回复作为预览
  const lastMessage = session.messages[session.messages.length - 1]
  if (lastMessage && lastMessage.content) {
    return lastMessage.content.substring(0, 100) + (lastMessage.content.length > 100 ? '...' : '')
  }
  
  return '暂无消息内容'
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  
  const units = ['B', 'KB', 'MB', 'GB']
  let unitIndex = 0
  let size = bytes
  
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  
  return `${size.toFixed(2)} ${units[unitIndex]}`
}
</script>

<style lang="scss" scoped>
.history-overlay {
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

.history-dialog {
  background-color: #fff;
  border-radius: 16px;
  width: 700px;
  max-width: 90vw;
  height: 80vh;
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

.search-box {
  padding: 20px 28px 16px;
}

.sessions-container {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px 20px;

  .session-card {
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    padding: 16px;
    margin-bottom: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: #1890ff;
      box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
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
        max-width: 400px;
      }

      .time {
        font-size: 13px;
        color: #999;
        white-space: nowrap;
      }
    }

    .preview {
      font-size: 13px;
      color: #666;
      line-height: 1.5;
      margin-bottom: 10px;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .attachments {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;

      .attachment-item {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 6px 10px;
        background-color: #f7f8fa;
        border-radius: 6px;
        font-size: 12px;
        color: #666;

        .el-icon {
          color: #999;
        }

        .size {
          color: #bbb;
          margin-left: 4px;
        }
      }

      .more-attachments {
        padding: 6px 10px;
        font-size: 12px;
        color: #1890ff;
        cursor: pointer;
        
        &:hover {
          text-decoration: underline;
        }
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
  }
}
</style>
