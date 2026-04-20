<template>
  <div class="main-content">
    <div class="chat-header">
      <h2>{{ currentAppName }}</h2>
      
      <!-- 🔑 模型选择下拉框（仅 superagent 显示） -->
      <el-dropdown 
        v-if="chatStore.appConfig[chatStore.currentApp]?.supportsModelSelection" 
        trigger="click" 
        @command="handleModelChange"
        class="model-selector"
      >
        <span class="model-dropdown-trigger">
          <el-icon><Cpu /></el-icon>
          {{ currentModelName }}
          <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled class="dropdown-group-title">选择模型</el-dropdown-item>
            <el-dropdown-item
              v-for="model in chatStore.modelList"
              :key="model.id"
              :command="model.id"
              :class="{ 'is-active': chatStore.currentModel === model.id }"
            >
              <el-icon><component :is="getModelIcon(model.provider)" /></el-icon>
              <span class="model-name">{{ model.name }}</span>
              <el-tag v-if="model.id === 'qwen-plus'" size="small" type="success" effect="plain" class="default-tag">默认</el-tag>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      
      <!-- 当前会话名下拉框 -->
      <el-dropdown v-if="currentSessionTitle" trigger="click" @command="handleSessionCommand">
        <span class="session-title-dropdown">
          {{ currentSessionTitle }}
          <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <!-- 普通会话 -->
            <el-dropdown-item disabled class="dropdown-group-title">普通会话</el-dropdown-item>
            <el-dropdown-item
              v-for="session in normalSessions"
              :key="session.sessionId"
              :command="{ type: 'switch', session }"
              :class="{ 'is-active': session.sessionId === currentSessionIdValue }"
            >
              <el-icon><ChatDotRound /></el-icon>
              <span class="session-name">{{ session.title || session.sessionName || '新对话' }}</span>
            </el-dropdown-item>
            
            <!-- 分隔线 -->
            <el-dropdown-item v-if="archivedSessions.length > 0" divided disabled class="dropdown-group-title">归档会话</el-dropdown-item>
            
            <!-- 归档会话 -->
            <el-dropdown-item
              v-for="session in archivedSessions"
              :key="session.sessionId"
              :command="{ type: 'switch', session }"
              :class="{ 'is-active': session.sessionId === currentSessionIdValue }"
            >
              <el-icon><Folder /></el-icon>
              <span class="session-name">{{ session.title || session.sessionName || '归档对话' }}</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div class="chat-messages" ref="messagesContainer">
      <div v-if="currentMessages.length === 0" class="empty-state">
        <el-icon :size="64" color="#d0d0d0">
          <component :is="currentAppIcon" />
        </el-icon>
        <h3>开始新对话</h3>
        <p>选择一个 AI 助手，开始你的智能对话</p>
      </div>

      <div
        v-for="(message, index) in currentMessages"
        :key="index"
        class="message-wrapper"
        :class="[message.role, { 'is-hovered': hoveredMessageIndex === index }]"
        @mouseenter="hoveredMessageIndex = index"
        @mouseleave="hoveredMessageIndex = null"
      >
        <!-- 🔑 用户消息：无头像，内容靠左 -->
        <div v-if="message.role === 'user'" class="user-message">
          <div class="text">{{ message.content }}</div>
          <!-- 用户消息操作按钮 -->
          <MessageActions
            v-if="message.content"
            :content="message.content"
            :message-id="message.chatId"
            :session-id="currentSessionIdValue"
            :is-archived="isArchivedSession"
            class="user-actions"
            :class="{ visible: hoveredMessageIndex === index }"
            @delete="handleDeleteMessage(index)"
            @share="openShareDialog(index)"
          />
        </div>

        <!-- 🔑 AI 消息：无头像，透明背景（使用 AgentMessageRenderer 渲染完整内容） -->
        <div v-else class="assistant-message">
          <div class="message-content">
            <div class="markdown-wrapper">
              <AgentMessageRenderer
                :content="message.content"
                :metadata="message.metadata"
              />
            </div>
          </div>
          <!-- AI消息操作按钮 - 只在非流式状态下显示（最后一条消息在流式时隐藏） -->
          <MessageActions
            v-if="message.content && !(chatStore.isStreamingResponse && index === currentMessages.length - 1)"
            :content="message.content"
            :message-id="message.chatId"
            :session-id="currentSessionIdValue"
            :is-archived="isArchivedSession"
            class="assistant-actions"
            :class="{ visible: hoveredMessageIndex === index }"
            @delete="handleDeleteMessage(index)"
            @share="openShareDialog(index)"
          />
        </div>
      </div>

      <!-- 加载状态：AI 消息 -->
      <div v-if="isLoading" class="message-wrapper assistant">
        <div class="assistant-message">
          <div class="message-content">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 🔑🔑🔑 分享对话框 -->
    <ShareDialog
      v-if="showShareDialog"
      :messages="currentMessages"
      :initial-message-index="shareInitialIndex"
      @close="closeShareDialog"
    />

    <div v-if="!isArchivedSession" class="chat-input-area">
      <!-- 🔑 重新设计的输入框：圆角 + 无色差 -->
      <div class="input-container-modern">
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="问点难的，让我多想一步"
            @keydown.enter.exact="handleSend"
            resize="none"
          />
          <div class="input-actions">
            <!-- 🔑🔑🔑 新增：暂停/停止按钮（当加载中时显示） -->
            <el-button
              v-if="isLoading"
              type="danger"
              circle
              :icon="VideoPause"
              @click="handleStop"
              class="stop-btn"
              title="停止接收"
            />
            <el-button
              v-else
              type="primary"
              circle
              :icon="Promotion"
              @click="handleSend"
              :disabled="!inputMessage.trim()"
              class="send-btn"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useChatStore } from '@/stores/chat'
import { fetchStream } from '@/api/stream'
import { Promotion, VideoPause, ArrowDown, ChatDotRound, Folder, Cpu, MagicStick, Lightning, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { parseError, isSystemError, isSessionError } from '@/utils/errorHandler'
import AgentMessageRenderer from './AgentMessageRenderer.vue'
import MessageActions from './MessageActions.vue'
import ShareDialog from './ShareDialog.vue'

const chatStore = useChatStore()
const { currentMessages } = storeToRefs(chatStore)
const inputMessage = ref('')
const isLoading = ref(false)
const messagesContainer = ref(null)
const hoveredMessageIndex = ref(null)

// 🔑🔑🔑 分享功能相关
const showShareDialog = ref(false)
const shareInitialIndex = ref(-1)

const abortController = ref(null)

const currentAppName = computed(() => chatStore.appConfig[chatStore.currentApp]?.name || '智能助手')
const currentAppIcon = computed(() => chatStore.appConfig[chatStore.currentApp]?.icon || 'MagicStick')

// 🔑 当前模型名称
const currentModelName = computed(() => {
  const model = chatStore.modelList.find(m => m.id === chatStore.currentModel)
  return model?.name || '通义千问 Plus'
})

// 🔑 获取模型图标
function getModelIcon(provider) {
  switch (provider) {
    case 'qwen': return 'MagicStick'
    case 'deepseek': return 'Lightning'
    case 'glm': return 'CircleCheck'
    default: return 'Cpu'
  }
}

// 🔑 处理模型切换
function handleModelChange(modelId) {
  if (chatStore.switchModel(modelId)) {
    ElMessage.success(`已切换到 ${currentModelName.value}`)
  }
}

// 🔑 判断当前会话是否是归档会话
const isArchivedSession = computed(() => {
  const appKey = chatStore.currentApp
  const sessionId = chatStore.currentSessionId[appKey]
  if (!sessionId) return false

  const sessionList = chatStore.sessions[appKey]
  if (!sessionList) return false

  const session = sessionList.find(s => s.sessionId === sessionId)
  return session?.isArchived === true
})

// 当前会话ID
const currentSessionIdValue = computed(() => {
  return chatStore.currentSessionId[chatStore.currentApp] || ''
})

// 当前会话标题
const currentSessionTitle = computed(() => {
  const appKey = chatStore.currentApp
  const sessionId = chatStore.currentSessionId[appKey]
  if (!sessionId) return ''

  // 先从普通会话中查找
  const sessionList = chatStore.sessions[appKey]
  if (sessionList) {
    const session = sessionList.find(s => s.sessionId === sessionId)
    if (session) {
      return session.title || session.sessionName || '新对话'
    }
  }

  // 再从归档会话中查找
  const archivedList = chatStore.archivedSessions[appKey]
  if (archivedList) {
    const archivedSession = archivedList.find(s => s.sessionId === sessionId)
    if (archivedSession) {
      return archivedSession.title || archivedSession.sessionName || '归档对话'
    }
  }

  return ''
})

// 普通会话列表
const normalSessions = computed(() => {
  const appKey = chatStore.currentApp
  return chatStore.getSessionHistory(appKey)
})

// 归档会话列表
const archivedSessions = computed(() => {
  const appKey = chatStore.currentApp
  return chatStore.archivedSessions[appKey] || []
})

// 删除消息处理
function handleDeleteMessage(index) {
  chatStore.currentMessages.splice(index, 1)
  // 同步更新到会话存储
  const appKey = chatStore.currentApp
  const sessionId = chatStore.currentSessionId[appKey]
  if (sessionId) {
    chatStore.updateSessionMessages(sessionId, appKey, chatStore.currentMessages)
  }
}

// 🔑🔑🔑 打开分享对话框
function openShareDialog(index) {
  shareInitialIndex.value = index
  showShareDialog.value = true
}

// 🔑🔑🔑 关闭分享对话框
function closeShareDialog() {
  showShareDialog.value = false
  shareInitialIndex.value = -1
}

// 处理会话下拉框命令
function handleSessionCommand(command) {
  if (command.type === 'switch') {
    const session = command.session
    const appKey = chatStore.currentApp

    // 判断是否为归档会话
    const isArchived = session.isArchived === true

    if (isArchived) {
      // 加载归档会话详情
      chatStore.loadArchivedSession(session.sessionId, appKey)
    } else {
      // 加载普通会话
      chatStore.loadSession(session.sessionId, appKey)
    }
  }
}

watch(currentMessages, () => {
  scrollToBottom()
}, { deep: true })

onMounted(() => {
  scrollToBottom()
})

onUnmounted(() => {
  chatStore.setStreamingResponse(false)
})

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

function ensureSessionReady() {
  const activeSessionId = chatStore.currentSessionId[chatStore.currentApp]

  if (activeSessionId === null || activeSessionId === undefined) {
    chatStore.createNewSession(chatStore.currentApp)
  }
}

let accumulatedContent = ''
let isStreaming = false
let streamErrorMessage = ''

const agentStreamState = ref({
  thinking: '',
  toolUsages: [],
  toolResponses: [],
  isThinking: false,
  isUsingTools: false
})

function resetStreamState() {
  accumulatedContent = ''
  isStreaming = false
  streamErrorMessage = ''
  agentStreamState.value = {
    thinking: '',
    toolUsages: [],
    toolResponses: [],
    isThinking: false,
    isUsingTools: false
  }
}

function addAssistantPlaceholder() {
  chatStore.addMessage({
    role: 'assistant',
    content: '',
    chatId: null,
    timestamp: new Date().toISOString(),
    metadata: {
      thinking: '',
      toolUsages: [],
      toolResponses: [],
      isThinking: false,
      isUsingTools: false
    }
  })
}

function getAssistantContent(baseContent = '') {
  if (!streamErrorMessage) {
    return baseContent
  }

  const errorSuffix = `> ⚠️ ${streamErrorMessage}`
  if (baseContent === streamErrorMessage || baseContent.endsWith(errorSuffix)) {
    return baseContent
  }

  return baseContent
    ? `${baseContent}\n\n${errorSuffix}`
    : streamErrorMessage
}

function replaceAssistantMessage(messageIndex, { persist = false, forceIdle = false } = {}) {
  const oldMsg = chatStore.currentMessages[messageIndex]
  if (!oldMsg) return null

  const newMsg = {
    ...oldMsg,
    content: getAssistantContent(accumulatedContent || oldMsg.content || ''),
    metadata: {
      ...oldMsg.metadata,
      thinking: agentStreamState.value.thinking,
      toolUsages: [...agentStreamState.value.toolUsages],
      toolResponses: [...agentStreamState.value.toolResponses],
      isThinking: forceIdle ? false : agentStreamState.value.isThinking,
      isUsingTools: forceIdle ? false : agentStreamState.value.isUsingTools
    }
  }

  chatStore.currentMessages.splice(messageIndex, 1, newMsg)

  if (persist) {
    chatStore.updateLastAssistantMessageWithMetadata(newMsg)
  }

  return newMsg
}

function finalizeAssistantMessage(messageIndex) {
  replaceAssistantMessage(messageIndex, {
    persist: true,
    forceIdle: true
  })
}

async function handleSend() {
  const message = inputMessage.value.trim()
  if (!message || isLoading.value) return

  ensureSessionReady()

  let sessionId = chatStore.currentSessionId[chatStore.currentApp] || ''

  chatStore.addMessage({
    role: 'user',
    content: message,
    timestamp: new Date().toISOString()
  })

  // 🔑🔑🔑 新增：发送消息后提升会话排序（置顶到第一个）
  if (sessionId) {
    chatStore.promoteSessionAfterChat(sessionId, chatStore.currentApp)
  }

  resetStreamState()

  const assistantMessageIndex = chatStore.currentMessages.length

  addAssistantPlaceholder()

  inputMessage.value = ''
  isLoading.value = true
  chatStore.setStreamingResponse(true)

  // 🔑🔑🔑 新增：创建 AbortController 用于中断连接
  abortController.value = new AbortController()

  const appConfig = chatStore.appConfig[chatStore.currentApp]

  await fetchStream(
    appConfig.apiPath,
    appConfig.method,
    {
      agentId: appConfig.agentId,
      message: message,
      sessionId: sessionId,
      chatId: '',
      modelId: appConfig.supportsModelSelection ? chatStore.currentModel : undefined
    },
    (data) => {
      handleStreamData(data, assistantMessageIndex)
    },
    (error) => {
      handleError(error)
    },
    () => {
      finalizeAssistantMessage(assistantMessageIndex)
      isLoading.value = false
      isStreaming = false
      abortController.value = null
      chatStore.setStreamingResponse(false)
    },
    abortController.value
  )
}

// 🔑🔑🔑 新增：停止接收流式数据
function handleStop() {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  isLoading.value = false
  isStreaming = false
  chatStore.setStreamingResponse(false)
  ElMessage.info('已停止接收')
}

function handleStreamData(data, messageIndex) {
  if (data.type === 'init' && data.sessionId) {
    chatStore.bindBackendSessionId(data.sessionId, chatStore.currentApp)
    return
  }

  if (data.isError || data.type === 'error') {
    isLoading.value = false
    isStreaming = false
    abortController.value = null
    chatStore.setStreamingResponse(false)

    // 🔑 使用错误处理工具获取友好提示
    const errorContent = data.content || '服务异常'
    const errorCode = data.code
    
    // 如果是系统错误或会话错误，添加"稍后重试"提示
    if ((isSystemError(errorCode) || isSessionError(errorCode)) && 
        !errorContent.includes('稍后') && !errorContent.includes('重试')) {
      streamErrorMessage = `${errorContent}，请稍后重试`
    } else {
      streamErrorMessage = errorContent
    }
    
    replaceAssistantMessage(messageIndex, {
      persist: true,
      forceIdle: true
    })

    ElMessage.error(streamErrorMessage)
    return
  }

  if (data.type === 'thinking') {
    isStreaming = true
    agentStreamState.value.isThinking = true
    agentStreamState.value.thinking += data.content
    updateAssistantMessageWithMetadata(messageIndex)
    return
  }

  if (data.type === 'toolUsage') {
    isStreaming = true
    agentStreamState.value.isUsingTools = true
    data.tools.forEach(tool => {
      agentStreamState.value.toolUsages.push(tool)
    })
    updateAssistantMessageWithMetadata(messageIndex)
    return
  }

  if (data.type === 'toolResponse') {
    isStreaming = true
    agentStreamState.value.isUsingTools = false
    data.responses.forEach(response => {
      agentStreamState.value.toolResponses.push(response)
    })
    updateAssistantMessageWithMetadata(messageIndex)
    return
  }

  if (data.content) {
    isStreaming = true

    if (agentStreamState.value.isThinking) {
      agentStreamState.value.isThinking = false
    }

    accumulatedContent += data.content

    if (replaceAssistantMessage(messageIndex)) {
      requestAnimationFrame(() => {
        scrollToBottom()
      })
    }
  }
}

function updateAssistantMessageWithMetadata(messageIndex) {
  replaceAssistantMessage(messageIndex)
}

function handleError(error) {
  if (abortController.value) {
    abortController.value = null
  }

  isLoading.value = false
  isStreaming = false
  chatStore.setStreamingResponse(false)

  // 🔑 使用错误处理工具解析错误
  const parsedError = parseError(error)
  let errorMessage = parsedError.message

  // 如果没有解析到消息，使用默认消息
  if (!errorMessage || errorMessage === 'undefined' || errorMessage === 'null') {
    if (error.isConnectionLost) {
      errorMessage = '连接已断开，请检查网络后重试'
    } else if (error.isBackendError && error.message) {
      errorMessage = error.message
    } else {
      errorMessage = '网络繁忙，请稍后重试'
    }
  }

  // 🔑 如果是系统错误或会话错误，确保添加"稍后重试"提示
  if ((isSystemError(parsedError.code) || isSessionError(parsedError.code)) && 
      !errorMessage.includes('稍后') && !errorMessage.includes('重试')) {
    errorMessage = `${errorMessage}，请稍后重试`
  }

  streamErrorMessage = errorMessage

  const lastIndex = chatStore.currentMessages.length - 1
  if (lastIndex >= 0 && chatStore.currentMessages[lastIndex].role === 'assistant') {
    replaceAssistantMessage(lastIndex, {
      persist: true,
      forceIdle: true
    })
  }

  ElMessage.error(errorMessage)
}
</script>

<style lang="scss" scoped>
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #ffffff;
}

.chat-header {
  padding: 20px 40px;
  background-color: transparent;
  border-bottom: none;
  display: flex;
  align-items: center;
  gap: 12px;

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }

  // 🔑 模型选择器样式
  .model-selector {
    margin-left: auto;
    
    .model-dropdown-trigger {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      background-color: #f0f9ff;
      border: 1px solid #91caff;
      border-radius: 8px;
      cursor: pointer;
      font-size: 13px;
      color: #0958d9;
      transition: all 0.2s ease;
      font-weight: 500;

      &:hover {
        background-color: #e6f4ff;
        border-color: #4096ff;
      }

      .el-icon {
        font-size: 14px;
      }

      .dropdown-icon {
        font-size: 12px;
      }
    }
  }

  .session-title-dropdown {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 6px 12px;
    background-color: #f7f8fa;
    border-radius: 8px;
    cursor: pointer;
    font-size: 14px;
    color: #666;
    transition: all 0.2s ease;

    &:hover {
      background-color: #e5e7eb;
    }

    .dropdown-icon {
      font-size: 12px;
    }
  }
}

.dropdown-group-title {
  font-size: 12px;
  color: #999;
  padding: 8px 16px;
  font-weight: 500;
}

.session-name {
  margin-left: 8px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.is-active {
  background-color: #e6f7ff;
  color: #1890ff;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 20%;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;

  h3 {
    font-size: 24px;
    color: #333;
    margin: 0;
  }

  p {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

.message-wrapper {
  margin-bottom: 20px;
  animation: fadeInUp 0.3s ease-out;

  &.user {
    display: flex;
    justify-content: flex-end;
    padding-right: 10%;
    
    .user-message {
      max-width: 80%;
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      
      .text {
        padding: 10px 16px;
        background-color: #f7f8fa;
        border-radius: 12px;
        line-height: 1.75;
        word-wrap: break-word;
        white-space: pre-wrap;
        text-align: right;
        font-size: 15px;
      }

      .user-actions {
        opacity: 0;
        transition: opacity 0.2s ease;
        
        &.visible {
          opacity: 1;
        }
      }
    }
  }

  &.assistant {
    padding-left: 10%;
    
    .assistant-message {
      display: flex;
      flex-direction: column;
      gap: 10px;
      align-items: flex-start;
      max-width: 80%;

      .ai-avatar-wrapper {
        flex-shrink: 0;
        cursor: pointer;
        margin-top: 16px;
        
        &:hover {
          transform: scale(1.05);
          transition: transform 0.2s ease;
        }
      }

      .message-content {
        flex: 1;
        min-width: 0;
        background-color: transparent;
        border-radius: 0;
        box-shadow: none;
        padding: 0;

        .markdown-wrapper {
          width: 100%;
          
          :deep(.markdown-renderer) {
            font-size: 15px;
            line-height: 2;
            color: #1f2937;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            
            h1 {
              font-size: 2em;
              line-height: 1.35;
              margin-top: 1.6em;
              margin-bottom: 0.8em;
              font-weight: 700;
              color: #111827;
              letter-spacing: -0.02em;
            }
            
            h2 {
              font-size: 1.6em;
              line-height: 1.4;
              margin-top: 1.5em;
              margin-bottom: 0.75em;
              font-weight: 650;
              color: #1f2937;
              border-bottom: 1px solid #e5e7eb;
              padding-bottom: 0.45em;
            }
            
            h3 {
              font-size: 1.35em;
              line-height: 1.45;
              margin-top: 1.35em;
              margin-bottom: 0.65em;
              font-weight: 600;
              color: #374151;
            }
            
            h4 {
              font-size: 1.15em;
              line-height: 1.5;
              margin-top: 1.2em;
              margin-bottom: 0.6em;
              font-weight: 600;
              color: #4b5563;
            }
            
            h5, h6 {
              font-size: 1em;
              line-height: 1.55;
              margin-top: 1.1em;
              margin-bottom: 0.55em;
              font-weight: 600;
              color: #6b7280;
            }

            p {
              margin-bottom: 1.4em;
              text-align: justify;
              
              &:last-child {
                margin-bottom: 0;
              }
            }
            
            ul, ol {
              margin-top: 0.8em;
              margin-bottom: 1.3em;
              padding-left: 1.75em;
            
              li {
                margin-bottom: 0.7em;
                line-height: 1.85;
                
                ul, ol {
                  margin-top: 0.4em;
                  margin-bottom: 0.4em;
                  
                  li {
                    margin-bottom: 0.35em;
                    font-size: 0.98em;
                    line-height: 1.8;
                  }
                }
              }
            }
            
            ul {
              list-style-type: disc;
              
              & > li::marker {
                color: #60a5fa;
                font-size: 0.85em;
              }
            }
            
            ol {
              list-style-type: decimal;
              
              & > li::marker {
                color: #818cf8;
                font-weight: 500;
              }
            }

            strong, b {
              font-weight: 650;
              color: #111827;
            }
            
            em, i {
              font-style: italic;
              color: #374151;
            }
            
            code:not(pre code) {
              background-color: #f3f4f6;
              color: #dc2626;
              padding: 0.15em 0.4em;
              border-radius: 4px;
              font-family: 'SF Mono', Monaco, Consolas, 'Liberation Mono', monospace;
              font-size: 0.9em;
              border: 1px solid #e5e7eb;
            }

            pre {
              background-color: #1f2937;
              color: #e5e7eb;
              border-radius: 10px;
              padding: 22px;
              margin: 20px 0;
              overflow-x: auto;
              line-height: 1.7;
              font-size: 13.5px;
              font-family: 'SF Mono', Monaco, Consolas, 'Liberation Mono', monospace;
              border: 1px solid #374151;
              box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
              
              code {
                background-color: transparent;
                color: inherit;
                padding: 0;
                border-radius: 0;
                border: none;
                font-size: inherit;
                line-height: inherit;
              }
            }

            blockquote {
              border-left: 4px solid #60a5fa;
              background-color: #eff6ff;
              padding: 18px 22px;
              margin: 20px 0;
              border-radius: 0 8px 8px 0;
              color: #1e40af;
              font-style: italic;
              line-height: 1.8;
              
              p {
                margin-bottom: 0.6em;
                color: inherit;
              }
              
              p:last-child {
                margin-bottom: 0;
              }
            }

            table {
              width: 100%;
              border-collapse: collapse;
              margin: 20px 0;
              font-size: 14px;
              border-radius: 8px;
              overflow: hidden;
              box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
              
              thead {
                background-color: #f9fafb;
                
                th {
                  padding: 14px 16px;
                  text-align: left;
                  font-weight: 600;
                  color: #111827;
                  border-bottom: 2px solid #d1d5db;
                }
              }
              
              tbody {
                tr {
                  transition: background-color 0.15s ease;
                  
                  &:hover {
                    background-color: #f9fafb;
                  }
                  
                  &:not(:last-child) td {
                    border-bottom: 1px solid #e5e7eb;
                  }
                  
                  td {
                    padding: 13px 16px;
                    color: #374151;
                    line-height: 1.6;
                    
                    code {
                      background-color: #f3f4f6;
                      padding: 0.15em 0.35em;
                      border-radius: 3px;
                      font-size: 0.92em;
                    }
                  }
                }
              }
            }

            hr {
              border: none;
              height: 2px;
              background: linear-gradient(to right, transparent, #d1d5db, transparent);
              margin: 28px 0;
            }

            a {
              color: #2563eb;
              text-decoration: none;
              border-bottom: 1px solid rgba(37, 99, 235, 0.3);
              transition: all 0.2s ease;
              
              &:hover {
                color: #1d4ed8;
                border-bottom-color: #1d4ed8;
              }
            }

            img {
              max-width: 100%;
              height: auto;
              border-radius: 8px;
              margin: 16px 0;
              box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            }

            input[type="checkbox"] {
              width: 16px;
              height: 16px;
              margin-right: 8px;
              accent-color: #3b82f6;
              vertical-align: middle;
            }
          }
        }
      }

      .assistant-actions {
        opacity: 0;
        transition: opacity 0.2s ease;
        margin-top: 4px;
        
        &.visible {
          opacity: 1;
        }
      }
    }
  }
}

.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 12px 0;

  span {
    width: 8px;
    height: 8px;
    background-color: #1890ff;
    border-radius: 50%;
    animation: bounce 1.4s infinite ease-in-out both;

    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

.chat-input-area {
  padding: 16px 40px 24px;
  background-color: transparent;
  border-top: none;
}

.input-container-modern {
  max-width: 900px;
  margin: 0 auto;

  .input-wrapper {
    display: flex;
    align-items: flex-end;
    gap: 12px;
    padding: 12px 16px;
    background-color: #f7f8fa;
    border-radius: 16px;
    border: 1px solid #e5e7eb;
    transition: all 0.2s ease;

    &:hover {
      border-color: #d0d0d0;
    }

    &:focus-within {
      border-color: #1890ff;
      box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.1);
    }

    :deep(.el-textarea__inner) {
      border: none;
      background-color: transparent;
      padding: 4px 0;
      font-size: 15px;
      line-height: 1.5;
      resize: none;
      box-shadow: none;

      &::placeholder {
        color: #999;
      }

      &:focus {
        box-shadow: none;
      }
    }

    .input-actions {
      display: flex;
      align-items: center;
      flex-shrink: 0;

      .send-btn {
        width: 36px;
        height: 36px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border: none;

        &:hover:not(:disabled) {
          opacity: 0.9;
          transform: scale(1.05);
        }

        &:disabled {
          background-color: #d9d9d9;
        }
      }

      // 🔑🔑🔑 新增：停止按钮样式
      .stop-btn {
        width: 36px;
        height: 36px;
        background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
        border: none;
        animation: pulse 1.5s ease-in-out infinite;

        &:hover {
          opacity: 0.9;
          transform: scale(1.05);
          animation: none;
        }
      }
    }
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}
</style>
