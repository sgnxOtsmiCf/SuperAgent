<template>
  <div class="message-actions" :class="{ visible: isVisible || isHovered }" @mouseenter="isHovered = true"
    @mouseleave="isHovered = false">
    <div class="actions-container">
      <!-- 复制 -->
      <el-tooltip content="复制" placement="top" :show-after="300">
        <button class="action-btn" @click="handleCopy">
          <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            stroke-linecap="round" stroke-linejoin="round">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
          </svg>
        </button>
      </el-tooltip>

      <!-- 朗读 -->
      <el-tooltip content="朗读" placement="top" :show-after="300">
        <button class="action-btn" @click="handleSpeak">
          <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
            <line x1="12" y1="19" x2="12" y2="23"></line>
            <line x1="8" y1="23" x2="16" y2="23"></line>
          </svg>
        </button>
      </el-tooltip>

      <!-- 点赞 -->
      <el-tooltip content="点赞" placement="top" :show-after="300">
        <button class="action-btn" :class="{ active: isLiked }" @click="handleLike">
          <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path>
          </svg>
        </button>
      </el-tooltip>

      <!-- 点踩 -->
      <el-tooltip content="点踩" placement="top" :show-after="300">
        <button class="action-btn" :class="{ active: isDisliked }" @click="handleDislike">
          <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M10 15v4a3 3 0 0 0 3 3l4-9V2H5.72a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3zm7-13h3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-3"></path>
          </svg>
        </button>
      </el-tooltip>

      <!-- 分享 -->
      <el-tooltip content="分享" placement="top" :show-after="300">
        <button class="action-btn" @click="handleShare">
          <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"></path>
            <polyline points="16 6 12 2 8 6"></polyline>
            <line x1="12" y1="2" x2="12" y2="15"></line>
          </svg>
        </button>
      </el-tooltip>

      <!-- 更多（仅归档消息显示删除选项） -->
      <el-dropdown v-if="canDelete" trigger="click" @command="handleMoreCommand">
        <button class="action-btn">
          <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="1"></circle>
            <circle cx="19" cy="12" r="1"></circle>
            <circle cx="5" cy="12" r="1"></circle>
          </svg>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="delete" class="delete-item">
              <svg class="dropdown-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
              <span>删除</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { messageApi } from '@/api/message'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  messageId: {
    type: Number,
    default: -1
  },
  sessionId: {
    type: String,
    default: ''
  },
  isArchived: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['delete', 'share'])

const isHovered = ref(false)
const isVisible = ref(false)
const isLiked = ref(false)
const isDisliked = ref(false)

const canDelete = computed(() => {
  return props.isArchived && props.messageId && props.messageId !== -1
})

function show() {
  isVisible.value = true
}

function hide() {
  isVisible.value = false
}

async function handleCopy() {
  try {
    await navigator.clipboard.writeText(props.content)
    ElMessage.success('已复制到剪贴板')
  } catch (err) {
    const textarea = document.createElement('textarea')
    textarea.value = props.content
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('已复制到剪贴板')
  }
}

function handleSpeak() {
  if (!props.content) {
    ElMessage.warning('没有可朗读的内容')
    return
  }

  window.speechSynthesis.cancel()

  const utterance = new SpeechSynthesisUtterance(props.content)
  utterance.lang = 'zh-CN'
  utterance.rate = 1
  utterance.pitch = 1

  window.speechSynthesis.speak(utterance)
  ElMessage.success('开始朗读')
}

function handleLike() {
  isLiked.value = !isLiked.value
  if (isLiked.value) {
    isDisliked.value = false
    ElMessage.success('已点赞')
  }
}

function handleDislike() {
  isDisliked.value = !isDisliked.value
  if (isDisliked.value) {
    isLiked.value = false
    ElMessage.success('已点踩')
  }
}

function handleShare() {
  emit('share')
}

async function handleMoreCommand(command) {
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定要删除这条消息吗？', '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      })

      const res = await messageApi.deleteMessageByMessageId(props.sessionId, props.messageId)

      if (res.code === 200) {
        ElMessage.success('删除成功')
        emit('delete', props.messageId)
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除消息失败:', error)
        ElMessage.error('删除失败')
      }
    }
  }
}

defineExpose({
  show,
  hide
})
</script>

<style scoped lang="scss">
.message-actions {
  opacity: 0;
  transition: opacity 0.2s ease;
  margin-top: 10px;

  &.visible {
    opacity: 1;
  }

  .actions-container {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 4px;
  }

  .action-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    border: none;
    background: transparent;
    border-radius: 6px;
    cursor: pointer;
    color: #9ca3af;
    transition: all 0.15s ease;

    &:hover {
      background-color: #f3f4f6;
      color: #6b7280;
    }

    &:active {
      transform: scale(0.92);
    }

    &.active {
      color: #2563eb;
      background-color: #eff6ff;

      .action-icon {
        fill: rgba(37, 99, 235, 0.1);
      }
    }

    .action-icon {
      width: 16px;
      height: 16px;
    }
  }
}

.delete-item {
  color: #ef4444;

  .dropdown-icon {
    width: 16px;
    height: 16px;
    margin-right: 6px;
  }

  &:hover {
    background-color: #fef2f2;
  }
}
</style>
