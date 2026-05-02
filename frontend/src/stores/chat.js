import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { sessionApi } from '@/api/session'
import { modelApi } from '@/api/model'
import { ElMessage } from 'element-plus'
import { logger } from '@/utils/logger'

const STORAGE_KEY = 'superagent_chat_sessions'
const CURRENT_SESSION_KEY = 'superagent_chat_current_session'
const CURRENT_APP_KEY = 'superagent_chat_current_app'
const MODEL_PARAMS_KEY = 'superagent_model_params'

export const useChatStore = defineStore('chat', () => {
  const savedCurrentApp = localStorage.getItem(CURRENT_APP_KEY)
  const currentApp = ref(savedCurrentApp || 'superagent')

  const savedSessions = localStorage.getItem(STORAGE_KEY)
  const sessions = ref(savedSessions ? JSON.parse(savedSessions) : {
    superagent: [],
    manus: [],
    family: []
  })

  const backendSessions = ref({
    superagent: [],
    manus: [],
    family: []
  })

  const savedCurrentSession = localStorage.getItem(CURRENT_SESSION_KEY)
  const currentSessionId = ref(savedCurrentSession ? JSON.parse(savedCurrentSession) : {
    superagent: null,
    manus: null,
    family: null
  })

  const currentMessages = ref([])

  const isLoadingSessions = ref(false)
  const isStreamingResponse = ref(false)

  const CURRENT_MODEL_KEY = 'superagent_current_model'
  const savedCurrentModel = localStorage.getItem(CURRENT_MODEL_KEY)
  const currentModel = ref(savedCurrentModel || 'qwen-plus')

  const modelList = ref([])

  const modelListLoaded = ref(false)

  const defaultConfig = ref({
    temperature: 0.75,
    topP: 0.9,
    topK: 10,
    maxTokens: 200000,
    thinkingBudget: 0,
    enableThinking: false,
    enableSearch: false
  })

  const defaultConfigLoaded = ref(false)

  const savedModelParams = localStorage.getItem(MODEL_PARAMS_KEY)
  const modelParams = ref(savedModelParams ? JSON.parse(savedModelParams) : {})

  const appConfig = {
    superagent: {
      name: '超级智能体',
      icon: 'MagicStick',
      apiPath: '/superagent/chat/text/stream',
      method: 'POST',
      agentId: 1,
      supportsModelSelection: true
    },
    manus: {
      name: 'OpenManus',
      icon: 'Cpu',
      apiPath: '/manus/chat/sse',
      method: 'POST',
      agentId: 2,
      supportsModelSelection: false
    },
    family: {
      name: '家庭和睦助手',
      icon: 'House',
      apiPath: '/familyHarmony/chat/sse',
      method: 'POST',
      agentId: 3,
      supportsModelSelection: false
    }
  }

  watch(currentModel, (newVal) => {
    localStorage.setItem(CURRENT_MODEL_KEY, newVal)
  })

  watch(modelParams, (newVal) => {
    localStorage.setItem(MODEL_PARAMS_KEY, JSON.stringify(newVal))
  }, { deep: true })

  watch(sessions, (newVal) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newVal))
  }, { deep: true })

  watch(currentSessionId, (newVal) => {
    localStorage.setItem(CURRENT_SESSION_KEY, JSON.stringify(newVal))
  }, { deep: true })

  watch(currentApp, (newVal) => {
    localStorage.setItem(CURRENT_APP_KEY, newVal)
  })

  function switchApp(appKey) {
    if (!appConfig[appKey]) return

    currentApp.value = appKey

    const sessionList = sessions.value[appKey]
    if (sessionList.length > 0) {
      const savedSessionId = currentSessionId.value[appKey]
      let targetSession = null

      if (savedSessionId) {
        targetSession = sessionList.find(s => s.sessionId === savedSessionId)
      }

      if (!targetSession) {
        const sortedSessions = [...sessionList].sort(
          (a, b) => new Date(b.lastActive || b.createdAt) - new Date(a.lastActive || a.createdAt)
        )
        targetSession = sortedSessions[0]
        currentSessionId.value[appKey] = targetSession.sessionId
      }

      currentMessages.value = [...(targetSession.messages || [])]
    } else {
      currentMessages.value = []
      currentSessionId.value[appKey] = null
    }
  }

  function createNewSession(appKey) {
    const tempSessionId = ''

    const newSession = {
      sessionId: tempSessionId,
      messages: [],
      createdAt: new Date().toISOString(),
      lastActive: new Date().toISOString(),
      title: '新对话',
      attachments: [],
      syncedWithBackend: false,
      isPinned: false
    }

    sessions.value[appKey].unshift(newSession)
    currentSessionId.value[appKey] = tempSessionId
    currentMessages.value = []

    saveToLocalStorage()

    return tempSessionId
  }

  function addMessage(message) {
    currentMessages.value.push(message)

    const appKey = currentApp.value
    const sessionList = sessions.value[appKey]
    const currentSession = sessionList.find(s => s.sessionId === currentSessionId.value[appKey])

    if (currentSession) {
      currentSession.messages.push(message)
      currentSession.lastActive = new Date().toISOString()

      if (message.role === 'user' && currentSession.title === '新对话') {
        const titleText = message.content.substring(0, 10)
        currentSession.title = titleText + (message.content.length > 10 ? '...' : '')
      }

      saveToLocalStorage()
    }
  }

  function updateLastAssistantMessage(content, chatId) {
    const lastIndex = currentMessages.value.findLastIndex(msg => msg.role === 'assistant')
    if (lastIndex !== -1) {
      const updatedMessage = {
        ...currentMessages.value[lastIndex],
        content: content,
        ...(chatId ? { chatId } : {})
      }

      currentMessages.value.splice(lastIndex, 1, updatedMessage)

      const appKey = currentApp.value
      const sessionList = sessions.value[appKey]
      const currentSession = sessionList.find(s => s.sessionId === currentSessionId.value[appKey])
      if (currentSession && currentSession.messages[lastIndex]) {
        currentSession.messages.splice(lastIndex, 1, updatedMessage)
        saveToLocalStorage()
      }
    }
  }

  function updateLastAssistantMessageWithMetadata(updatedMessage) {
    const lastIndex = currentMessages.value.findLastIndex(msg => msg.role === 'assistant')
    if (lastIndex !== -1) {
      currentMessages.value.splice(lastIndex, 1, updatedMessage)

      const appKey = currentApp.value
      const sessionList = sessions.value[appKey]
      const currentSession = sessionList.find(s => s.sessionId === currentSessionId.value[appKey])

      if (currentSession) {
        if (!currentSession.messages) {
          currentSession.messages = []
        }

        if (currentSession.messages[lastIndex]) {
          currentSession.messages.splice(lastIndex, 1, {
            ...updatedMessage,
            timestamp: new Date().toISOString()
          })
        } else if (lastIndex === currentSession.messages.length) {
          currentSession.messages.push({
            ...updatedMessage,
            timestamp: new Date().toISOString()
          })
        }

        saveToLocalStorage()
      }
    }
  }

  function updateSessionMessages(sessionId, appKey, messages) {
    if (!appKey) appKey = currentApp.value
    if (!sessionId || !appKey) return

    const sessionList = sessions.value[appKey]
    if (!sessionList) return

    const session = sessionList.find(s => s.sessionId === sessionId)
    if (session) {
      session.messages = [...messages]
      saveToLocalStorage()
    }
  }

  function getSessionHistory(appKey) {
    const history = sessions.value[appKey] || []

    const validHistory = history.filter(session => {
      if (!session.sessionId) return false

      const isLocalTempId = session.sessionId.startsWith('session_')

      if (isLocalTempId && !session.syncedWithBackend) {
        return false
      }

      return true
    })

    return [...validHistory].sort((a, b) => {
      if (a.isTop && b.isTop) {
        return b.isTop - a.isTop
      }
      if (a.isTop && !b.isTop) return -1
      if (!a.isTop && b.isTop) return 1

      const timeA = new Date(a.lastActive || a.createdAt).getTime()
      const timeB = new Date(b.lastActive || b.createdAt).getTime()

      return timeB - timeA
    })
  }

  function loadSession(sessionId, appKey) {
    const sessionList = sessions.value[appKey]

    if (!sessionList || sessionList.length === 0) {
      logger.error('[ChatStore] sessionList is empty or does not exist, appKey:', appKey)
      return
    }

    const sessionIndex = sessionList.findIndex(s => s.sessionId === sessionId)

    if (sessionIndex === -1) {
      logger.error('[ChatStore] session not found for the sessionId')
      return
    }

    const session = sessionList[sessionIndex]

    session.lastAccessed = Date.now()

    currentSessionId.value[appKey] = sessionId

    if (session.messages && session.messages.length > 0) {
      currentMessages.value = [...session.messages]
    } else {
      currentMessages.value = session.messages || []
    }

    saveToLocalStorage()
  }

  function promoteSessionAfterChat(sessionId, appKey) {
    if (!appKey) appKey = currentApp.value
    const sessionList = sessions.value[appKey]

    if (!sessionList || sessionList.length === 0) return

    const sessionIndex = sessionList.findIndex(s => s.sessionId === sessionId)
    if (sessionIndex === -1) return

    const session = sessionList[sessionIndex]

    const now = new Date()
    session.lastActive = now.toISOString()
    session.lastAccessed = now.getTime()

    sessionList.splice(sessionIndex, 1)

    const pinnedCount = sessionList.filter(s => s.isPinned).length
    const insertIndex = pinnedCount

    sessionList.splice(insertIndex, 0, session)

    saveToLocalStorage()
  }

  async function fetchSessionsFromBackend(appKey) {
    if (!appKey) appKey = currentApp.value

    const agentId = appConfig[appKey]?.agentId
    if (!agentId) {
      logger.error('[ChatStore] agentId not found:', appKey)
      return
    }

    isLoadingSessions.value = true

    try {
      const res = await sessionApi.getSessionsPage(agentId, 1, 9)

      if (res.data && Array.isArray(res.data)) {
        const backendSessionList = res.data.map((session, index) => {

          const messages = []
          const toolResults = {}

          ;(session.content || []).forEach(msg => {
            if (msg.messageType === 'TOOL' && msg.content) {
              msg.content.forEach(toolResult => {
                if (toolResult.id) {
                  toolResults[toolResult.id] = toolResult
                }
              })
            }
          })

          ;(session.content || []).forEach((msg, index) => {
            if (msg.messageType === 'USER') {
              messages.push({
                role: 'user',
                content: msg.content,
                chatId: msg.id,
                timestamp: msg.messageTime,
                metadata: msg.metadata || {}
              })
            } else if (msg.messageType === 'ASSISTANT') {
              const toolCalls = msg.metadata?.toolCalls || []
              const toolUsages = toolCalls.map(tc => ({
                id: tc.id,
                name: tc.name,
                arguments: tc.arguments
              }))

              const toolResponses = toolCalls
                .map(tc => toolResults[tc.id])
                .filter(Boolean)
                .map(tr => ({
                  id: tr.id,
                  name: tr.name,
                  responseData: tr.responseData
                }))

              const segments = []

              toolUsages.forEach(tool => {
                segments.push({
                  type: 'toolUsage',
                  data: tool,
                  timestamp: new Date(msg.messageTime).getTime()
                })
              })

              toolResponses.forEach(response => {
                segments.push({
                  type: 'toolResponse',
                  data: response,
                  timestamp: new Date(msg.messageTime).getTime() + 1
                })
              })

              if (msg.content) {
                segments.push({
                  type: 'mergedText',
                  data: msg.content,
                  timestamp: new Date(msg.messageTime).getTime() + 2
                })
              }

              messages.push({
                role: 'assistant',
                content: msg.content || '',
                chatId: msg.id,
                timestamp: msg.messageTime,
                metadata: {
                  ...msg.metadata,
                  thinking: msg.metadata?.reasoningContent || '',
                  toolUsages,
                  toolResponses,
                  segments,
                  isThinking: false,
                  isUsingTools: false
                }
              })
            }
          })

          return {
            sessionId: session.sessionId,
            agentId: session.agentId || 1,
            title: session.sessionName || '历史对话',
            createdAt: session.lastActive,
            lastActive: session.lastActive,
            messages: messages,
            syncedWithBackend: true,
            isPinned: !!session.isTop,
            isTop: session.isTop
          }
        })

        backendSessions.value[appKey] = backendSessionList

        mergeBackendSessions(appKey, backendSessionList)
      }

      enforcePinLimit(appKey, 3)

    } catch (error) {
      logger.error('[ChatStore] failed to load sessions from backend:', error)
      ElMessage.error('加载历史会话失败')
    } finally {
      isLoadingSessions.value = false
    }
  }

  async function fetchSessionDetailFromBackend(sessionId, appKey) {
    if (!appKey) appKey = currentApp.value

    const agentId = appConfig[appKey]?.agentId
    if (!agentId || !sessionId) return

    try {
      const res = await sessionApi.getSessionBySessionId(agentId, sessionId)

      if (res.data) {
        const sessionData = res.data

        const messages = []
        const toolResults = {}

        ;(sessionData.content || []).forEach(msg => {
          if (msg.messageType === 'TOOL' && msg.content) {
            msg.content.forEach(toolResult => {
              if (toolResult.id) {
                toolResults[toolResult.id] = toolResult
              }
            })
          }
        })

        ;(sessionData.content || []).forEach(msg => {
          if (msg.messageType === 'USER') {
            messages.push({
              role: 'user',
              content: msg.content,
              chatId: msg.id,
              timestamp: msg.messageTime,
              metadata: msg.metadata || {}
            })
          } else if (msg.messageType === 'ASSISTANT') {
            const toolCalls = msg.metadata?.toolCalls || []
            const toolUsages = toolCalls.map(tc => ({
              id: tc.id,
              name: tc.name,
              arguments: tc.arguments
            }))

            const toolResponses = toolCalls
              .map(tc => toolResults[tc.id])
              .filter(Boolean)
              .map(tr => ({
                id: tr.id,
                name: tr.name,
                responseData: tr.responseData
              }))

            const segments = []

            toolUsages.forEach(tool => {
              segments.push({
                type: 'toolUsage',
                data: tool,
                timestamp: new Date(msg.messageTime).getTime()
              })
            })

            toolResponses.forEach(response => {
              segments.push({
                type: 'toolResponse',
                data: response,
                timestamp: new Date(msg.messageTime).getTime() + 1
              })
            })

            if (msg.content) {
              segments.push({
                type: 'mergedText',
                data: msg.content,
                timestamp: new Date(msg.messageTime).getTime() + 2
              })
            }

            messages.push({
              role: 'assistant',
              content: msg.content || '',
              chatId: msg.id,
              timestamp: msg.messageTime,
              metadata: {
                ...msg.metadata,
                thinking: msg.metadata?.reasoningContent || '',
                toolUsages,
                toolResponses,
                segments,
                isThinking: false,
                isUsingTools: false
              }
            })
          }
        })

        const sessionList = sessions.value[appKey]
        const sessionIndex = sessionList.findIndex(s => s.sessionId === sessionId)

        if (sessionIndex !== -1) {
          sessions.value[appKey][sessionIndex].messages = messages
          sessions.value[appKey][sessionIndex].syncedWithBackend = true

          if (currentSessionId.value[appKey] === sessionId) {
            currentMessages.value = [...messages]
          }

          saveToLocalStorage()
        }

      }
    } catch (error) {
      logger.error('[ChatStore] failed to load session detail:', error)
      ElMessage.error('加载会话详情失败')
    }
  }

  async function deleteSession(sessionId, appKey) {
    if (!appKey) appKey = currentApp.value

    const agentId = appConfig[appKey]?.agentId
    if (!agentId || !sessionId) return

    try {
      await sessionApi.deleteSessionById(agentId, sessionId)

      const sessionList = sessions.value[appKey]
      const index = sessionList.findIndex(s => s.sessionId === sessionId)

      if (index !== -1) {
        sessions.value[appKey].splice(index, 1)

        if (currentSessionId.value[appKey] === sessionId) {
          if (sessions.value[appKey].length > 0) {
            switchApp(appKey)
          } else {
            currentSessionId.value[appKey] = null
            currentMessages.value = []
          }
        }

        saveToLocalStorage()
        ElMessage.success('会话已删除')
      }
    } catch (error) {
      logger.error('[ChatStore] failed to delete session:', error)
      ElMessage.error('删除会话失败')
    }
  }

  async function updateSessionTitle(sessionId, sessionName, appKey) {
    if (!appKey) appKey = currentApp.value

    const agentId = appConfig[appKey]?.agentId
    if (!agentId || !sessionId) return false

    try {
      await sessionApi.updateSessionTitle(agentId, sessionId, sessionName)

      const sessionList = sessions.value[appKey]
      const session = sessionList.find(s => s.sessionId === sessionId)

      if (session) {
        session.title = sessionName
        saveToLocalStorage()
      }

      return true
    } catch (error) {
      logger.error('[ChatStore] failed to update session title:', error)
      ElMessage.error('更新标题失败')
      return false
    }
  }

  async function togglePinSession(sessionId, appKey) {
    if (!appKey) appKey = currentApp.value

    const sessionList = sessions.value[appKey]
    const session = sessionList.find(s => s.sessionId === sessionId)

    if (!session) {
      logger.error('[ChatStore] session not found:', sessionId)
      return false
    }

    if (!session.isPinned) {
      const currentPinnedCount = sessionList.filter(s => s.isPinned).length

      if (currentPinnedCount >= 3) {
        ElMessage.warning('最多只能置顶3条会话，请先取消其他置顶')
        return false
      }
    }

    try {
      const agentId = appConfig[appKey]?.agentId
      if (!agentId) {
        logger.error('[ChatStore] agentId not found:', appKey)
        return false
      }

      const res = session.isPinned
        ? await sessionApi.setUpTopSession(agentId, sessionId)
        : await sessionApi.setTopSession(agentId, sessionId)

      if (res.code === 200) {
        if (!session.isPinned) {
          session.isPinned = true
          session.isTop = Date.now()
        } else {
          session.isPinned = false
          session.isTop = null
        }
        session.lastActive = new Date().toISOString()
        saveToLocalStorage()

        sessionList.sort((a, b) => {
          if (a.isTop && b.isTop) {
            return b.isTop - a.isTop
          }
          if (a.isTop && !b.isTop) return -1
          if (!a.isTop && b.isTop) return 1
          return new Date(b.lastActive) - new Date(a.lastActive)
        })

        return true
      } else {
        ElMessage.error(res.message || (session.isPinned ? '取消置顶失败' : '置顶失败'))
        return false
      }
    } catch (error) {
      logger.error('[ChatStore] toggle pin failed:', error)
      ElMessage.error((session.isPinned ? '取消置顶' : '置顶') + '操作失败，请稍后重试')
      return false
    }
  }

  function enforcePinLimit(appKey, maxPins = 3) {
    if (!appKey) appKey = currentApp.value

    const sessionList = sessions.value[appKey]
    if (!sessionList || sessionList.length === 0) return

    const pinnedSessions = sessionList.filter(s => s.isPinned)

    if (pinnedSessions.length > maxPins) {
      const sortedByPinTime = [...pinnedSessions].sort((a, b) => {
        return new Date(a.lastActive || a.createdAt) - new Date(b.lastActive || b.createdAt)
      })

      const toUnpin = sortedByPinTime.slice(0, pinnedSessions.length - maxPins)

      toUnpin.forEach(session => {
        session.isPinned = false
      })

      saveToLocalStorage()
      ElMessage.warning('已自动取消 ' + toUnpin.length + ' 条置顶（超过' + maxPins + '条限制）')
    }
  }

  function bindBackendSessionId(backendSessionId, appKey) {
    if (!appKey) appKey = currentApp.value
    if (!backendSessionId) {
      logger.error('[ChatStore] bindBackendSessionId: sessionId is empty')
      return
    }

    const oldSessionId = currentSessionId.value[appKey]

    const shouldUpdate = (
      oldSessionId === '' ||
      (oldSessionId && oldSessionId !== backendSessionId)
    )

    if (shouldUpdate) {
      const sessionList = sessions.value[appKey]

      let sessionIndex = -1

      if (oldSessionId === '') {
        const emptySessions = sessionList.filter(s => !s.sessionId)
        if (emptySessions.length > 0) {
          const sortedByTime = [...emptySessions].sort(
            (a, b) => new Date(b.lastActive || b.createdAt) - new Date(a.lastActive || a.createdAt)
          )
          sessionIndex = sessionList.findIndex(s => s === sortedByTime[0])
        }
      } else {
        sessionIndex = sessionList.findIndex(s => s.sessionId === oldSessionId)
      }

      if (sessionIndex !== -1) {
        sessions.value[appKey][sessionIndex].sessionId = backendSessionId
        sessions.value[appKey][sessionIndex].syncedWithBackend = true
      } else {
        logger.error('[ChatStore] session not found for update, oldSessionId:', oldSessionId)
      }
    }

    currentSessionId.value[appKey] = backendSessionId

    saveToLocalStorage()
  }

  function mergeBackendSessions(appKey, backendList) {
    const localSessions = sessions.value[appKey] || []
    const backendSessionIds = new Set(backendList.map(session => session.sessionId))
    const localSessionMap = new Map(
      localSessions
        .filter(session => session.sessionId)
        .map(session => [session.sessionId, session])
    )

    const mergedBackendSessions = backendList.map(session => {
      const localSession = localSessionMap.get(session.sessionId)

      return {
        ...session,
        attachments: localSession?.attachments || session.attachments || [],
        isPinned: localSession?.isPinned ?? session.isPinned ?? false,
        lastAccessed: localSession?.lastAccessed ?? session.lastAccessed
      }
    })

    const unsyncedLocalSessions = localSessions.filter(session => {
      if (!session.sessionId) {
        return true
      }

      return !session.syncedWithBackend && !backendSessionIds.has(session.sessionId)
    })

    sessions.value[appKey] = [...unsyncedLocalSessions, ...mergedBackendSessions]
  }

  const archivedSessions = ref({
    superagent: [],
    manus: [],
    family: []
  })

  const isLoadingArchives = ref(false)

  async function archiveSession(sessionId, appKey) {
    if (!appKey) appKey = currentApp.value
    const agentId = appConfig[appKey]?.agentId
    if (!agentId || !sessionId) return false

    try {
      await sessionApi.archiveSession(agentId, sessionId)
      ElMessage.success('归档请求已提交，正在异步处理中')
      return true
    } catch (error) {
      logger.error('[ChatStore] failed to archive session:', error)
      ElMessage.error('归档会话失败')
      return false
    }
  }

  async function checkArchiveStatus(sessionId) {
    try {
      const res = await sessionApi.getArchiveStatus(sessionId)
      return res.data === true
    } catch (error) {
      logger.error('[ChatStore] failed to check archive status:', error)
      return false
    }
  }

  async function fetchArchivedSessions(appKey, pageNo = 1, pageSize = 20) {
    if (!appKey) appKey = currentApp.value
    const agentId = appConfig[appKey]?.agentId
    if (!agentId) {
      logger.warn('[ChatStore] failed to load archive: agentId is empty, appKey=', appKey)
      return
    }

    isLoadingArchives.value = true
    try {
      const res = await sessionApi.getArchiveSessions(agentId, pageNo, pageSize)

      if (res.data && Array.isArray(res.data)) {
        archivedSessions.value[appKey] = res.data.map(session => {
          const messages = []
          const toolResults = {}

          let contentArray = []
          if (Array.isArray(session.content)) {
            contentArray = session.content
          } else if (typeof session.content === 'string') {
            try {
              contentArray = JSON.parse(session.content)
            } catch (e) {
              contentArray = []
            }
          } else {
            contentArray = []
          }

          contentArray.forEach(msg => {
            if (msg.messageType === 'TOOL' && msg.content) {
              const toolContentArray = Array.isArray(msg.content) ? msg.content : []
              toolContentArray.forEach(toolResult => {
                if (toolResult.id) toolResults[toolResult.id] = toolResult
              })
            }
          })

          contentArray.forEach(msg => {
            if (msg.messageType === 'USER') {
              messages.push({
                role: 'user',
                content: msg.content || '',
                chatId: msg.id,
                timestamp: msg.messageTime,
                metadata: msg.metadata || {}
              })
            } else if (msg.messageType === 'ASSISTANT') {
              const toolCalls = msg.metadata?.toolCalls || []
              const toolUsages = toolCalls.map(tc => ({ id: tc.id, name: tc.name, arguments: tc.arguments }))
              const toolResponses = toolCalls
                .map(tc => toolResults[tc.id]).filter(Boolean)
                .map(tr => ({ id: tr.id, name: tr.name, responseData: tr.responseData }))

              const segments = []
              toolUsages.forEach(tool => segments.push({ type: 'toolUsage', data: tool, timestamp: new Date(msg.messageTime).getTime() }))
              toolResponses.forEach(resp => segments.push({ type: 'toolResponse', data: resp, timestamp: new Date(msg.messageTime).getTime() + 1 }))
              if (msg.content) segments.push({ type: 'mergedText', data: msg.content, timestamp: new Date(msg.messageTime).getTime() + 2 })

              messages.push({
                role: 'assistant',
                content: msg.content || '',
                chatId: msg.id,
                timestamp: msg.messageTime,
                metadata: {
                  ...msg.metadata,
                  thinking: msg.metadata?.reasoningContent || '',
                  toolUsages, toolResponses, segments,
                  isThinking: false, isUsingTools: false
                }
              })
            }
          })

          return {
            sessionId: session.sessionId,
            agentId: session.agentId || 1,
            title: session.sessionName || '归档对话',
            createdAt: session.lastActive,
            lastActive: session.lastActive,
            messages,
            syncedWithBackend: true,
            isPinned: false,
            isArchived: true
          }
        })
      } else {
        archivedSessions.value[appKey] = []
      }
    } catch (error) {
      logger.error('[ChatStore] failed to load archived sessions:', error)
      ElMessage.error('加载归档会话失败，请检查控制台日志')
    } finally {
      isLoadingArchives.value = false
    }
  }

  function setStreamingResponse(streaming) {
    isStreamingResponse.value = !!streaming
  }

  function generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  }

  function saveToLocalStorage() {
    try {
      const data = JSON.stringify(sessions.value)
      if (data.length > 4 * 1024 * 1024) {
        trimOldSessionMessages()
        return
      }
      localStorage.setItem(STORAGE_KEY, data)
    } catch (e) {
      if (e.name === 'QuotaExceededError' || e.code === 22) {
        trimOldSessionMessages()
      }
    }
  }

  function trimOldSessionMessages() {
    Object.keys(sessions.value).forEach(appKey => {
      const sessionList = sessions.value[appKey]
      const sorted = [...sessionList].sort(
        (a, b) => new Date(b.lastActive || b.createdAt) - new Date(a.lastActive || a.createdAt)
      )

      sorted.forEach((session, index) => {
        if (index >= 3 && session.messages && session.messages.length > 5) {
          session.messages = session.messages.slice(0, 5)
          session.messagesTrimmed = true
        }
      })
    })

    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions.value))
    } catch (e) {
      // silent
    }
  }

  function saveCurrentSessionToLocalStorage() {
    localStorage.setItem(CURRENT_SESSION_KEY, JSON.stringify(currentSessionId.value))
  }

  function clearAllSessions() {
    Object.keys(sessions.value).forEach(key => {
      sessions.value[key] = []
    })
    Object.keys(currentSessionId.value).forEach(key => {
      currentSessionId.value[key] = null
    })
    Object.keys(backendSessions.value).forEach(key => {
      backendSessions.value[key] = []
    })
    currentMessages.value = []
    isStreamingResponse.value = false
    modelParams.value = {}
    localStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem(MODEL_PARAMS_KEY)
  }

  function switchModel(modelId) {
    const model = modelList.value.find(m => m.id === modelId)
    if (model) {
      currentModel.value = modelId
      return true
    }
    return false
  }

  async function fetchModelList() {
    if (modelListLoaded.value) return
    try {
      const res = await modelApi.getModelList({ pageNo: 1, pageSize: 100 })
      if (res.code === 200 && res.data) {
        modelList.value = (res.data.records || res.data).map(m => ({
          id: m.modelCode,
          name: m.modelName,
          provider: m.providerName || '',
          providerId: m.providerId,
          modelType: m.modelType,
          capabilities: m.capabilities,
          contextWindow: m.contextWindow,
          maxOutputTokens: m.maxOutputTokens,
          inputPricePer1M: m.inputPricePer1M,
          outputPricePer1M: m.outputPricePer1M,
          description: m.description,
          iconUrl: m.iconUrl,
          tags: m.tags,
          paramConstraints: parseParamConstraints(m.paramConstraints)
        }))
        modelListLoaded.value = true
      }
    } catch (error) {
      logger.error('[ChatStore] failed to fetch model list:', error)
    }
  }

  function parseParamConstraints(raw) {
    if (!raw) return {}
    let parsed = raw
    if (typeof raw === 'string') {
      try {
        parsed = JSON.parse(raw)
      } catch {
        return {}
      }
    }
    if (typeof parsed !== 'object') return {}
    const normalized = {}
    for (const [key, value] of Object.entries(parsed)) {
      const camelKey = key.replace(/_([a-z])/g, (_, c) => c.toUpperCase())
      normalized[camelKey] = value
    }
    return normalized
  }

  async function fetchModelConfig() {
    if (defaultConfigLoaded.value) return
    try {
      const res = await modelApi.getModelConfig()
      if (res.code === 200 && res.data) {
        const cfg = res.data
        defaultConfig.value = {
          temperature: cfg.temperature != null ? Number(cfg.temperature) : 0.75,
          topP: cfg.topP != null ? Number(cfg.topP) : 0.9,
          topK: cfg.topK != null ? Number(cfg.topK) : 10,
          maxTokens: cfg.maxTokens != null ? Number(cfg.maxTokens) : 200000,
          thinkingBudget: cfg.thinkingBudget != null ? Number(cfg.thinkingBudget) : 0,
          enableThinking: cfg.enableThinking != null ? Boolean(cfg.enableThinking) : false,
          enableSearch: cfg.enableSearch != null ? Boolean(cfg.enableSearch) : false
        }
      }
    } catch (error) {
      logger.error('[ChatStore] failed to fetch model config:', error)
    } finally {
      defaultConfigLoaded.value = true
    }
  }

  function getEffectiveParams(modelId) {
    const model = modelList.value.find(m => m.id === modelId)
    const constraints = model?.paramConstraints || {}
    const defaults = defaultConfig.value

    function resolve(key, defVal) {
      const constraint = constraints[key]
      if (constraint && typeof constraint === 'object' && constraint.min != null && constraint.max != null) {
        const min = Number(constraint.min)
        const max = Number(constraint.max)
        const overrideVal = modelParams.value[modelId]?.[key]
        if (overrideVal != null) return Math.min(max, Math.max(min, Number(overrideVal)))
        return Math.min(max, Math.max(min, defVal))
      }
      const overrideVal = modelParams.value[modelId]?.[key]
      if (overrideVal != null) return Number(overrideVal)
      return defVal
    }

    function resolveBool(key) {
      const overrideVal = modelParams.value[modelId]?.[key]
      if (overrideVal != null) return !!overrideVal
      return constraints[key] != null ? !!constraints[key] : null
    }

    return {
      temperature: resolve('temperature', defaults.temperature),
      topP: resolve('topP', defaults.topP),
      topK: resolve('topK', defaults.topK),
      maxTokens: resolve('maxTokens', defaults.maxTokens),
      thinkingBudget: resolve('thinkingBudget', defaults.thinkingBudget),
      enableThinking: resolveBool('enableThinking'),
      enableSearch: resolveBool('enableSearch')
    }
  }

  function setModelParams(modelId, params) {
    modelParams.value = { ...modelParams.value, [modelId]: { ...modelParams.value[modelId], ...params } }
  }

  return {
    currentApp,
    sessions,
    backendSessions,
    currentSessionId,
    currentMessages,
    isLoadingSessions,
    isStreamingResponse,
    appConfig,
    archivedSessions,
    isLoadingArchives,
    currentModel,
    modelList,
    modelListLoaded,
    defaultConfig,
    defaultConfigLoaded,
    modelParams,
    fetchModelList,
    fetchModelConfig,
    getEffectiveParams,
    setModelParams,
    switchApp,
    createNewSession,
    addMessage,
    updateLastAssistantMessage,
    updateLastAssistantMessageWithMetadata,
    updateSessionMessages,
    getSessionHistory,
    loadSession,
    promoteSessionAfterChat,
    fetchSessionsFromBackend,
    fetchSessionDetailFromBackend,
    deleteSession,
    updateSessionTitle,
    togglePinSession,
    enforcePinLimit,
    bindBackendSessionId,
    archiveSession,
    checkArchiveStatus,
    fetchArchivedSessions,
    setStreamingResponse,
    clearAllSessions,
    switchModel
  }
})
