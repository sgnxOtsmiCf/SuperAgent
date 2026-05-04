<template>
  <div class="main-content">
    <div class="chat-header">
      <h2>{{ currentAppName }}</h2>

      <!-- 模型选择下拉框（仅 superagent 显示） -->
      <el-dropdown
          v-if="chatStore.appConfig[chatStore.currentApp]?.supportsModelSelection"
          trigger="click"
          class="model-selector"
          popper-class="model-dropdown-popper"
      >
        <span class="model-dropdown-trigger">
          <el-icon><Cpu /></el-icon>
          {{ currentModelName }}
          <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled class="dropdown-group-title">选择模型</el-dropdown-item>
            <template v-for="group in groupedModelList" :key="group.provider">
              <el-dropdown-item disabled class="dropdown-group-title provider-group-title">
                {{ group.provider || '其他厂商' }}
              </el-dropdown-item>
              <el-dropdown-item
                  v-for="model in group.models"
                  :key="model.id"
                  :class="{ 'is-active': chatStore.currentModel === model.id }"
                  class="model-item-wrapper"
              >
                <div class="model-item-row" @click.stop="handleModelChange(model.id)">
                  <span class="model-name">{{ model.name }}</span>
                  <el-tag v-for="tag in parseTags(model.tags)" :key="tag" size="small" type="warning" effect="plain" class="model-tag">{{ tag }}</el-tag>
                  <el-tag v-if="model.id === chatStore.currentModel" size="small" type="success" effect="plain" class="default-tag">当前</el-tag>
                </div>
                <el-button
                    text
                    circle
                    size="small"
                    class="param-settings-btn"
                    @click.stop="openParamSettings(model.id)"
                    title="参数设置"
                >
                  <el-icon><Setting /></el-icon>
                </el-button>
              </el-dropdown-item>
            </template>
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

    <div class="chat-messages" ref="messagesContainer" @scroll="handleMessagesScroll">
      <!-- 浮动导航箭头：跳转到上一条/下一条用户消息 -->
      <transition name="fade-arrow">
        <div v-show="showScrollArrows" class="scroll-nav-arrows">
          <button
            class="scroll-nav-btn scroll-up"
            :class="{ 'is-hidden': prevUserMessageIndex === -1 }"
            :disabled="prevUserMessageIndex === -1"
            @click="scrollToUserMessage(prevUserMessageIndex)"
            title="上一条用户消息"
          >
            <el-icon><ArrowUp /></el-icon>
          </button>
          <button
            class="scroll-nav-btn scroll-down"
            :class="{ 'is-hidden': nextUserMessageIndex === -1 }"
            :disabled="nextUserMessageIndex === -1"
            @click="scrollToUserMessage(nextUserMessageIndex)"
            title="下一条用户消息"
          >
            <el-icon><ArrowDown /></el-icon>
          </button>
        </div>
      </transition>

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
          :data-index="index"
          class="message-wrapper"
          :class="[message.role, { 'is-hovered': hoveredMessageIndex === index, 'no-animation': !shouldAnimateMessage(index) }]"
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
                  :is-streaming="chatStore.isStreamingResponse && index === currentMessages.length - 1 && message.role === 'assistant'"
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

    <!-- 🔑 模型参数设置对话框 -->
    <ModelParamDialog
        v-if="showParamDialog"
        :model-id="paramDialogModelId"
        :visible="showParamDialog"
        @close="closeParamDialog"
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
import { Promotion, VideoPause, ArrowDown, ArrowUp, ChatDotRound, Folder, Cpu, MagicStick, Lightning, CircleCheck, Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { parseError, isSystemError, isSessionError } from '@/utils/errorHandler'
import AgentMessageRenderer from './AgentMessageRenderer.vue'
import MessageActions from './MessageActions.vue'
import ShareDialog from './ShareDialog.vue'
import ModelParamDialog from './ModelParamDialog.vue'

const chatStore = useChatStore()
const { currentMessages } = storeToRefs(chatStore)
const inputMessage = ref('')
const isLoading = ref(false)
const messagesContainer = ref(null)
const hoveredMessageIndex = ref(null)
let lastMessageCount = 0

// 浮动导航箭头相关
const showScrollArrows = ref(false)
const prevUserMessageIndex = ref(-1)
const nextUserMessageIndex = ref(-1)
let scrollArrowTimer = null
let rafPending = false

function computeScrollArrowState() {
  if (!messagesContainer.value || currentMessages.value.length === 0) {
    prevUserMessageIndex.value = -1
    nextUserMessageIndex.value = -1
    showScrollArrows.value = false
    return
  }

  const container = messagesContainer.value
  const scrollTop = container.scrollTop
  const clientHeight = container.clientHeight
  const containerRect = container.getBoundingClientRect()

  // 使用容器内容坐标系（滚动期间稳定不变）
  const viewportCenterInContent = scrollTop + clientHeight / 2

  const userPositions = []
  const wrappers = container.querySelectorAll('.message-wrapper.user')
  wrappers.forEach((el) => {
    const indexAttr = el.getAttribute('data-index')
    if (indexAttr !== null) {
      const rect = el.getBoundingClientRect()
      // 元素在容器内容中的绝对位置（不受滚动影响）
      const topInContent = rect.top - containerRect.top + scrollTop
      const bottomInContent = rect.bottom - containerRect.top + scrollTop
      userPositions.push({
        index: parseInt(indexAttr, 10),
        top: topInContent,
        bottom: bottomInContent
      })
    }
  })

  if (userPositions.length === 0) {
    prevUserMessageIndex.value = -1
    nextUserMessageIndex.value = -1
    showScrollArrows.value = false
    return
  }

  // 找到视口中心上方最近的用户消息
  let prevIdx = -1
  for (let i = userPositions.length - 1; i >= 0; i--) {
    if (userPositions[i].bottom < viewportCenterInContent - 20) {
      prevIdx = userPositions[i].index
      break
    }
  }

  // 找到视口中心下方最近的用户消息
  let nextIdx = -1
  for (let i = 0; i < userPositions.length; i++) {
    if (userPositions[i].top > viewportCenterInContent + 20) {
      nextIdx = userPositions[i].index
      break
    }
  }

  prevUserMessageIndex.value = prevIdx
  nextUserMessageIndex.value = nextIdx

  const hasScrollableContent = container.scrollHeight > clientHeight + 100
  const hasNavTarget = prevIdx !== -1 || nextIdx !== -1
  showScrollArrows.value = hasScrollableContent && hasNavTarget
}

function handleMessagesScroll() {
  // 用 rAF 节流，避免平滑滚动期间过度计算
  if (!rafPending) {
    rafPending = true
    requestAnimationFrame(() => {
      rafPending = false
      computeScrollArrowState()
    })
  }

  if (scrollArrowTimer) clearTimeout(scrollArrowTimer)
  scrollArrowTimer = setTimeout(() => {
    showScrollArrows.value = false
  }, 2500)
}

function scrollToUserMessage(index) {
  const container = messagesContainer.value
  if (!container) return
  const target = container.querySelector(`.message-wrapper.user[data-index="${index}"]`)
  if (!target) return

  target.scrollIntoView({ behavior: 'smooth', block: 'center' })

  if (scrollArrowTimer) clearTimeout(scrollArrowTimer)
  scrollArrowTimer = setTimeout(() => {
    showScrollArrows.value = false
  }, 3000)
}

function shouldAnimateMessage(index) {
  return index >= lastMessageCount - 1
}

// 分享功能相关
const showShareDialog = ref(false)
const shareInitialIndex = ref(-1)

// 模型参数设置对话框
const showParamDialog = ref(false)
const paramDialogModelId = ref('')

function openParamSettings(modelId) {
  paramDialogModelId.value = modelId
  showParamDialog.value = true
}

function closeParamDialog() {
  showParamDialog.value = false
}

const abortController = ref(null)

const currentAppName = computed(() => chatStore.appConfig[chatStore.currentApp]?.name || '智能助手')
const currentAppIcon = computed(() => chatStore.appConfig[chatStore.currentApp]?.icon || 'MagicStick')

// 当前模型名称
const currentModelName = computed(() => {
  const model = chatStore.modelList.find(m => m.id === chatStore.currentModel)
  return model?.name || '通义千问 Plus'
})

// 按供应商分组的模型列表
const groupedModelList = computed(() => {
  const groups = {}
  chatStore.modelList.forEach(m => {
    const provider = m.provider || '其他'
    if (!groups[provider]) {
      groups[provider] = []
    }
    groups[provider].push(m)
  })
  return Object.keys(groups).map(provider => ({
    provider,
    models: groups[provider]
  }))
})

// 解析tags字段（后端返回JSON数组字符串）
function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  try {
    const parsed = JSON.parse(tags)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

// 获取模型图标
function getModelIcon(provider) {
  switch (provider) {
    case 'qwen': return 'MagicStick'
    case 'deepseek': return 'Lightning'
    case 'glm': return 'CircleCheck'
    default: return 'Cpu'
  }
}

// 处理模型切换
function handleModelChange(modelId) {
  chatStore.switchModel(modelId)
}

// 判断当前会话是否是归档会话
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

// 打开分享对话框
function openShareDialog(index) {
  shareInitialIndex.value = index
  showShareDialog.value = true
}

// 关闭分享对话框
function closeShareDialog() {
  showShareDialog.value = false
  shareInitialIndex.value = -1
}

// 处理会话下拉框命令
function handleSessionCommand(command) {
  if (command.type === 'switch') {
    if (chatStore.isStreamingResponse) {
      ElMessage.warning('请先等待当前回复完成或停止接收')
      return
    }

    const session = command.session
    const appKey = chatStore.currentApp

    const isArchived = session.isArchived === true

    if (isArchived) {
      const sessionList = chatStore.sessions[appKey]
      if (sessionList) {
        let existingIndex = sessionList.findIndex(s => s.sessionId === session.sessionId)
        if (existingIndex !== -1) {
          sessionList[existingIndex] = { ...sessionList[existingIndex], ...session, isArchived: true }
        } else {
          sessionList.unshift({ ...session, isArchived: true })
        }
      }
      chatStore.loadSession(session.sessionId, appKey)
    } else {
      chatStore.loadSession(session.sessionId, appKey)
    }
  }
}

watch(currentMessages, (newMessages) => {
  lastMessageCount = newMessages.length
  scrollToBottom()
}, { deep: true })

onMounted(() => {
  isLoading.value = false
  chatStore.setStreamingResponse(false)
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  scrollToBottom()
  chatStore.fetchModelList()
  chatStore.fetchModelConfig()
})

onUnmounted(() => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  chatStore.setStreamingResponse(false)
  if (scrollArrowTimer) {
    clearTimeout(scrollArrowTimer)
  }
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
let streamErrorMessage = ''

const agentStreamState = ref({
  thinking: '',
  toolUsages: [],
  toolResponses: [],
  isThinking: false,
  isUsingTools: false
})

// 新增：流式内容更新节流控制
let pendingContentUpdate = false
let pendingMetadataUpdate = false
let contentUpdateTimer = null
let metadataUpdateTimer = null
const CONTENT_UPDATE_INTERVAL = 80  // 内容更新间隔(ms)，平衡实时性和性能
const METADATA_UPDATE_INTERVAL = 120  // 元数据更新间隔(ms)

function resetStreamState() {
  accumulatedContent = ''
  streamErrorMessage = ''
  pendingContentUpdate = false
  pendingMetadataUpdate = false
  if (contentUpdateTimer) {
    clearTimeout(contentUpdateTimer)
    contentUpdateTimer = null
  }
  if (metadataUpdateTimer) {
    clearTimeout(metadataUpdateTimer)
    metadataUpdateTimer = null
  }
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

 // 新增：发送消息后提升会话排序（置顶到第一个）
  if (sessionId) {
    chatStore.promoteSessionAfterChat(sessionId, chatStore.currentApp)
  }

  resetStreamState()

  const assistantMessageIndex = chatStore.currentMessages.length

  addAssistantPlaceholder()

  inputMessage.value = ''
  isLoading.value = true
  chatStore.setStreamingResponse(true)

 // 新增：创建 AbortController 用于中断连接
  abortController.value = new AbortController()

  const appConfig = chatStore.appConfig[chatStore.currentApp]

  let params = {
    agentId: appConfig.agentId,
    message: message,
    sessionId: sessionId,
    chatId: '',
    modelId: appConfig.supportsModelSelection ? chatStore.currentModel : undefined
  }

  if (appConfig.supportsModelSelection && chatStore.currentModel) {
    const effectiveParams = chatStore.getEffectiveParams(chatStore.currentModel)
    params.temperature = effectiveParams.temperature
    params.topP = effectiveParams.topP
    params.topK = effectiveParams.topK
    params.maxTokens = effectiveParams.maxTokens
    params.thinkingBudget = effectiveParams.thinkingBudget
    if (effectiveParams.enableThinking != null) params.enableThinking = effectiveParams.enableThinking
    if (effectiveParams.enableSearch != null) params.enableSearch = effectiveParams.enableSearch
  }

  await fetchStream(
      appConfig.apiPath,
      appConfig.method,
      params,
      (data) => {
        handleStreamData(data, assistantMessageIndex)
      },
      (error) => {
        handleError(error)
      },
      () => {
        if (contentUpdateTimer) {
          clearTimeout(contentUpdateTimer)
          contentUpdateTimer = null
          pendingContentUpdate = false
        }
        if (metadataUpdateTimer) {
          clearTimeout(metadataUpdateTimer)
          metadataUpdateTimer = null
          pendingMetadataUpdate = false
        }
        finalizeAssistantMessage(assistantMessageIndex)
        isLoading.value = false
        abortController.value = null
        chatStore.setStreamingResponse(false)
      },
      abortController.value
  )
}

// 新增：停止接收流式数据
function handleStop() {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  isLoading.value = false
  chatStore.setStreamingResponse(false)
  ElMessage.info('已停止接收')
}

function handleStreamData(data, messageIndex) {
  if (data.type === 'init' && data.sessionId) {
    chatStore.bindBackendSessionId(data.sessionId, chatStore.currentApp)
    return
  }

  if (data.isError || data.type === 'error') {
 // 清除所有待处理的更新
    pendingContentUpdate = false
    pendingMetadataUpdate = false
    if (contentUpdateTimer) {
      clearTimeout(contentUpdateTimer)
      contentUpdateTimer = null
    }
    if (metadataUpdateTimer) {
      clearTimeout(metadataUpdateTimer)
      metadataUpdateTimer = null
    }

 // 使用错误处理工具获取友好提示
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

    abortController.value?.abort()
    abortController.value = null
    isLoading.value = false
    chatStore.setStreamingResponse(false)

    ElMessage.error(streamErrorMessage)
    return
  }

  if (data.type === 'thinking') {
    agentStreamState.value.isThinking = true
    agentStreamState.value.thinking += data.content
    scheduleMetadataUpdate(messageIndex)
    return
  }

  if (data.type === 'toolUsage') {
    agentStreamState.value.isUsingTools = true
    data.tools.forEach(tool => {
      agentStreamState.value.toolUsages.push(tool)
    })
    scheduleMetadataUpdate(messageIndex)
    return
  }

  if (data.type === 'toolResponse') {
    agentStreamState.value.isUsingTools = false
    data.responses.forEach(response => {
      agentStreamState.value.toolResponses.push(response)
    })
    scheduleMetadataUpdate(messageIndex)
    return
  }

  if (data.content) {
    if (agentStreamState.value.isThinking) {
      agentStreamState.value.isThinking = false
    }

    accumulatedContent += data.content
    scheduleContentUpdate(messageIndex)
  }
}

// 新增：节流的内容更新
function scheduleContentUpdate(messageIndex) {
  if (pendingContentUpdate) return
  pendingContentUpdate = true

  contentUpdateTimer = setTimeout(() => {
    pendingContentUpdate = false
    contentUpdateTimer = null
    if (replaceAssistantMessage(messageIndex)) {
      requestAnimationFrame(() => {
        scrollToBottom()
      })
    }
  }, CONTENT_UPDATE_INTERVAL)
}

// 新增：节流的元数据更新
function scheduleMetadataUpdate(messageIndex) {
  if (pendingMetadataUpdate) return
  pendingMetadataUpdate = true

  metadataUpdateTimer = setTimeout(() => {
    pendingMetadataUpdate = false
    metadataUpdateTimer = null
    if (replaceAssistantMessage(messageIndex)) {
      requestAnimationFrame(() => {
        scrollToBottom()
      })
    }
  }, METADATA_UPDATE_INTERVAL)
}

function handleError(error) {
  abortController.value?.abort()
  abortController.value = null

  isLoading.value = false
  chatStore.setStreamingResponse(false)

 // 使用错误处理工具解析错误
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

 // 如果是系统错误或会话错误，确保添加"稍后重试"提示
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

 // 模型选择器样式
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

  .model-item-wrapper {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    .model-item-row {
      flex: 1;
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      min-width: 0;
      cursor: pointer;

      .model-name {
        font-size: 13px;
        font-weight: 500;
        max-width: 160px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .model-tag {
        font-size: 10px;
        line-height: 1;
        flex-shrink: 0;
      }

      .default-tag {
        flex-shrink: 0;
      }
    }

    .param-settings-btn {
      flex-shrink: 0;
      opacity: 0;
      transition: opacity 0.15s ease;
      color: #909399;

      &:hover {
        color: #0958d9;
        background-color: #f0f9ff;
      }
    }

    &:hover .param-settings-btn {
      opacity: 1;
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

  &.no-animation {
    animation: none;
  }

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
      align-items: stretch;
      max-width: 90%;

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
        flex: 1 1 100%;
        min-width: 0;
        max-width: 100%;
        background-color: transparent;
        border-radius: 0;
        box-shadow: none;
        padding: 0;

        .markdown-wrapper {
          width: 100%;
          max-width: 100%;
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

 // 新增：停止按钮样式
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

// 浮动导航箭头样式
.scroll-nav-arrows {
  position: fixed;
  right: 28px;
  bottom: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  z-index: 100;
  pointer-events: none;

  .scroll-nav-btn {
    pointer-events: auto;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    padding: 0;
    margin: 0;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 50%;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
    color: #4b5563;
    transition: opacity 0.25s ease, background 0.25s ease, border-color 0.25s ease, color 0.25s ease, box-shadow 0.25s ease;
    font-size: 16px;
    cursor: pointer;
    outline: none;

    &.is-hidden {
      opacity: 0;
      pointer-events: none;
      box-shadow: none;
    }

    &:hover:not(.is-hidden):not(:disabled) {
      background: #f3f4f6;
      border-color: #d1d5db;
      color: #111827;
      box-shadow: 0 6px 18px rgba(0, 0, 0, 0.15);
    }

    &:active:not(.is-hidden):not(:disabled) {
      transform: scale(0.96);
    }

    .el-icon {
      font-size: 16px;
    }
  }
}

.fade-arrow-enter-active,
.fade-arrow-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-arrow-enter-from,
.fade-arrow-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>

<style lang="scss">
.model-dropdown-popper {
  min-width: 360px !important;

  .el-dropdown-menu__item.model-item-wrapper {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding-right: 8px;

    .model-item-row {
      flex: 1;
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;
      cursor: pointer;

      .model-name {
        font-size: 13px;
        font-weight: 500;
        color: #333;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .model-tag {
        font-size: 10px;
        line-height: 1;
        flex-shrink: 0;
      }

      .default-tag {
        flex-shrink: 0;
      }
    }

    .param-settings-btn {
      flex-shrink: 0;
      opacity: 0;
      transition: opacity 0.15s ease;
      color: #909399;
      width: 28px;
      height: 28px;

      &:hover {
        color: #0958d9;
        background-color: #f0f9ff;
      }
    }

    &:hover .param-settings-btn {
      opacity: 1;
    }
  }
}
</style>
