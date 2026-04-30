import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { sessionApi } from '@/api/session'
import { modelApi } from '@/api/model'
import { ElMessage } from 'element-plus'

const STORAGE_KEY = 'superagent_chat_sessions'
const CURRENT_SESSION_KEY = 'superagent_chat_current_session'
const CURRENT_APP_KEY = 'superagent_chat_current_app'
const MODEL_PARAMS_KEY = 'superagent_model_params'

export const useChatStore = defineStore('chat', () => {
  // 🔑🔑🔑 关键修复：从localStorage初始化currentApp
  const savedCurrentApp = localStorage.getItem(CURRENT_APP_KEY)
  const currentApp = ref(savedCurrentApp || 'superagent')

  // 从localStorage初始化sessions（本地缓存）
  const savedSessions = localStorage.getItem(STORAGE_KEY)
  const sessions = ref(savedSessions ? JSON.parse(savedSessions) : {
    superagent: [],
    manus: [],
    family: []
  })

  // 后端会话数据（用于与后端同步）
  const backendSessions = ref({
    superagent: [],
    manus: [],
    family: []
  })

  // 🔑🔑🔑 关键修复：从localStorage初始化currentSessionId
  const savedCurrentSession = localStorage.getItem(CURRENT_SESSION_KEY)
  const currentSessionId = ref(savedCurrentSession ? JSON.parse(savedCurrentSession) : {
    superagent: null,
    manus: null,
    family: null
  })

  const currentMessages = ref([])

  // 加载状态
  const isLoadingSessions = ref(false)
  const isStreamingResponse = ref(false)

  // 🔑 当前选择的模型ID（仅 superagent 支持切换模型）
  const CURRENT_MODEL_KEY = 'superagent_current_model'
  const savedCurrentModel = localStorage.getItem(CURRENT_MODEL_KEY)
  const currentModel = ref(savedCurrentModel || 'qwen-plus')

  // 🔑 支持的模型列表（从后端动态获取）
  const modelList = ref([])

  const modelListLoaded = ref(false)

  // 🔑 用户默认模型配置（从 /model/config 获取，作为参数回退）
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

  // 🔑 每个模型的参数覆盖值（用户自定义），key为modelCode
  const savedModelParams = localStorage.getItem(MODEL_PARAMS_KEY)
  const modelParams = ref(savedModelParams ? JSON.parse(savedModelParams) : {})

  const appConfig = {
    superagent: {
      name: '超级智能体',
      icon: 'MagicStick',
      apiPath: '/superagent/chat/text/stream',
      method: 'POST',
      agentId: 1,
      supportsModelSelection: true  // 🔑 支持模型选择
    },
    manus: {
      name: 'OpenManus',
      icon: 'Cpu',
      apiPath: '/manus/chat/sse',  // 后端：@PostMapping("/sse") + @RequestMapping("/manus/chat")
      method: 'POST',              // 🔑 修正：后端是POST方法
      agentId: 2,
      supportsModelSelection: false
    },
    family: {
      name: '家庭和睦助手',
      icon: 'House',
      apiPath: '/familyHarmony/chat/sse',  // 后端：@PostMapping("/sse") + @RequestMapping("/familyHarmony/chat")
      method: 'POST',                       // 🔑 修正：后端是POST方法
      agentId: 3,
      supportsModelSelection: false
    }
  }

  // 🔑 监听模型变化，自动保存到localStorage
  watch(currentModel, (newVal) => {
    localStorage.setItem(CURRENT_MODEL_KEY, newVal)
  })

  // 🔑 监听模型参数变化，自动保存到localStorage
  watch(modelParams, (newVal) => {
    localStorage.setItem(MODEL_PARAMS_KEY, JSON.stringify(newVal))
  }, { deep: true })

  // 监听sessions变化，自动保存到localStorage
  watch(sessions, (newVal) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newVal))
  }, { deep: true })

  // 🔑🔑🔑 关键修复：监听currentSessionId变化，自动保存到localStorage
  watch(currentSessionId, (newVal) => {
    localStorage.setItem(CURRENT_SESSION_KEY, JSON.stringify(newVal))
  }, { deep: true })

  // 🔑🔑🔑 关键修复：监听currentApp变化，自动保存到localStorage
  watch(currentApp, (newVal) => {
    localStorage.setItem(CURRENT_APP_KEY, newVal)
  })

  function switchApp(appKey) {
    if (!appConfig[appKey]) return

    currentApp.value = appKey

    const sessionList = sessions.value[appKey]
    if (sessionList.length > 0) {
      // 🔑🔑🔑 关键修复：优先使用保存的currentSessionId恢复会话
      const savedSessionId = currentSessionId.value[appKey]
      let targetSession = null

      if (savedSessionId) {
        // 查找保存的会话
        targetSession = sessionList.find(s => s.sessionId === savedSessionId)
      }

      if (!targetSession) {
        // 如果没有保存的会话或找不到，则按lastActive降序排序，取最新的会话
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
    // 🔑 关键：新建会话时，sessionId设为空字符串
    // 表示这是新会话，第一次请求时会发送空sessionId给后端
    // 后端会通过SSE的init事件返回正式的sessionId
    const tempSessionId = ''  // 空字符串，等待后端生成
    
    const newSession = {
      sessionId: tempSessionId,
      messages: [],
      createdAt: new Date().toISOString(),
      lastActive: new Date().toISOString(),
      title: '新对话',
      attachments: [],
      syncedWithBackend: false,  // 标记是否已同步到后端
      isPinned: false  // 是否置顶
    }

    sessions.value[appKey].unshift(newSession)  // 新会话放在最前面
    currentSessionId.value[appKey] = tempSessionId  // 当前sessionId为空
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

      // 🔑🔑🔑 优化：使用前10个字符作为会话标题（用户需求）
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
      // 🔑🔑🔑 关键：创建新对象以触发 Vue 响应式更新（而不是直接修改属性）
      const updatedMessage = {
        ...currentMessages.value[lastIndex],
        content: content,
        ...(chatId ? { chatId } : {})
      }

      // 使用 splice 替换，确保 Vue 能检测到变化
      currentMessages.value.splice(lastIndex, 1, updatedMessage)

      if (chatId) {
        // chatId 已经在 updatedMessage 中处理了
      }

      // 同步到当前会话的 messages 数组
      const appKey = currentApp.value
      const sessionList = sessions.value[appKey]
      const currentSession = sessionList.find(s => s.sessionId === currentSessionId.value[appKey])
      if (currentSession && currentSession.messages[lastIndex]) {
        // 🔑 同样使用 splice 确保响应式
        currentSession.messages.splice(lastIndex, 1, updatedMessage)
        saveToLocalStorage()
      }
    }
  }

  /**
   * 🔑🔑🔑 新增：更新助手消息（包含完整metadata）
   * 用于保存thinking、toolUsages、toolResponses等流式元数据
   * 确保刷新页面后能恢复这些信息
   *
   * @param {Object} updatedMessage - 包含content和metadata的完整消息对象
   */
  function updateLastAssistantMessageWithMetadata(updatedMessage) {
    const lastIndex = currentMessages.value.findLastIndex(msg => msg.role === 'assistant')
    if (lastIndex !== -1) {
      // 更新内存中的消息
      currentMessages.value.splice(lastIndex, 1, updatedMessage)

      // 🔑 关键：同步到当前会话的messages数组（包含metadata）
      const appKey = currentApp.value
      const sessionList = sessions.value[appKey]
      const currentSession = sessionList.find(s => s.sessionId === currentSessionId.value[appKey])

      if (currentSession) {
        // 确保 messages 数组存在
        if (!currentSession.messages) {
          currentSession.messages = []
        }

        // 如果索引存在则更新，否则添加
        if (currentSession.messages[lastIndex]) {
          currentSession.messages.splice(lastIndex, 1, {
            ...updatedMessage,
            timestamp: new Date().toISOString()  // 更新时间戳
          })
        } else if (lastIndex === currentSession.messages.length) {
          // 追加新消息
          currentSession.messages.push({
            ...updatedMessage,
            timestamp: new Date().toISOString()
          })
        }

        // 🔑🔑🔑 关键：立即保存到localStorage
        saveToLocalStorage()
      }
    }
  }

  /**
   * 更新会话的消息列表
   * @param {string} sessionId - 会话ID
   * @param {string} appKey - 应用key
   * @param {Array} messages - 新的消息列表
   */
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

    // 🔑🔑🔑 关键：只显示已绑定后端sessionId的会话（过滤掉新创建的空会话）
    const validHistory = history.filter(session => {
      // 条件1：有有效的sessionId（非空）
      if (!session.sessionId) return false

      // 条件2：不是本地临时生成的sessionId（不以'session_'开头）或者已同步到后端
      // 本地临时ID格式：'session_时间戳_随机数'
      const isLocalTempId = session.sessionId.startsWith('session_')

      // 如果是本地临时ID且未同步到后端，则不显示
      if (isLocalTempId && !session.syncedWithBackend) {
        return false
      }

      return true
    })

    // 🔑 智能排序：置顶优先（按isTop时间戳倒序）→ 最近活跃优先（按lastActive）
    return [...validHistory].sort((a, b) => {
      // 1️⃣ 第一优先级：置顶的排前面，按isTop时间戳倒序（最新的置顶在前）
      if (a.isTop && b.isTop) {
        return b.isTop - a.isTop  // 降序：isTop大的在前（最近置顶的在前）
      }
      if (a.isTop && !b.isTop) return -1
      if (!a.isTop && b.isTop) return 1

      // 2️⃣ 第二优先级：使用lastActive排序（最近对话的在前面）
      // 🔑 注意：不使用lastAccessed，避免点击会话时改变排序
      const timeA = new Date(a.lastActive || a.createdAt).getTime()
      const timeB = new Date(b.lastActive || b.createdAt).getTime()

      return timeB - timeA  // 降序：最新的在前
    })
  }

  function loadSession(sessionId, appKey) {
    const sessionList = sessions.value[appKey]

    if (!sessionList || sessionList.length === 0) {
      console.error('[ChatStore] ❌❌❌ sessionList为空或不存在！appKey:', appKey)
      return
    }

    const sessionIndex = sessionList.findIndex(s => s.sessionId === sessionId)

    if (sessionIndex === -1) {
      console.error('[ChatStore] ❌❌❌ 未找到该sessionId的会话！')
      return
    }

    const session = sessionList[sessionIndex]

    // 🔑🔑🔑 修改：点击会话只对焦，不改变排序
    // 更新最后访问时间（用于追踪），但不改变在列表中的位置
    session.lastAccessed = Date.now()

    // 设置当前会话ID
    currentSessionId.value[appKey] = sessionId

    // 加载消息内容
    if (session.messages && session.messages.length > 0) {
      currentMessages.value = [...session.messages]
    } else {
      currentMessages.value = session.messages || []
    }

    // 保存到localStorage
    saveToLocalStorage()
  }

  /**
   * 🔑🔑🔑 新增：在发送消息后提升会话排序
   * 只有在用户发送消息或收到回复后才调用此函数
   * @param {string} sessionId - 会话ID
   * @param {string} appKey - 应用key
   */
  function promoteSessionAfterChat(sessionId, appKey) {
    if (!appKey) appKey = currentApp.value
    const sessionList = sessions.value[appKey]

    if (!sessionList || sessionList.length === 0) return

    const sessionIndex = sessionList.findIndex(s => s.sessionId === sessionId)
    if (sessionIndex === -1) return

    const session = sessionList[sessionIndex]

    // 1️⃣ 更新最后活跃时间为当前时间
    const now = new Date()
    session.lastActive = now.toISOString()
    session.lastAccessed = now.getTime()

    // 2️⃣ 从当前位置移除
    sessionList.splice(sessionIndex, 1)

    // 3️⃣ 计算插入位置（所有置顶消息之后）
    const pinnedCount = sessionList.filter(s => s.isPinned).length
    const insertIndex = pinnedCount

    // 4️⃣ 插入到正确位置（置顶会话之后的第一位）
    sessionList.splice(insertIndex, 0, session)

    // 5️⃣ 保存
    saveToLocalStorage()
  }

  // ==================== 企业级：后端同步功能 ====================

  /**
   * 从后端加载会话列表（分页查询，显示8条）
   * @param {string} appKey - 应用key
   * @returns {Promise<void>}
   */
  async function fetchSessionsFromBackend(appKey) {
    if (!appKey) appKey = currentApp.value

    const agentId = appConfig[appKey]?.agentId
    if (!agentId) {
      console.error('[ChatStore] 未找到agentId:', appKey)
      return
    }

    isLoadingSessions.value = true

    try {
      // 🔑 修改：使用分页接口，查询9条数据（后端已按置顶时间排序）
      // 查询9条是为了判断是否有更多数据，如果返回9条说明有第9条，需要显示"查看更多"
      const res = await sessionApi.getSessionsPage(agentId, 1, 9)

      // 🔑 关键：后端返回的res已经是 {code, message, data} 格式（request拦截器已处理）
      if (res.data && Array.isArray(res.data)) {
        // 转换后端数据格式为前端格式，并解析content消息
        const backendSessionList = res.data.map((session, index) => {

          // 🔑🔑🔑 核心修复：解析content数组中的消息，处理USER/ASSISTANT/TOOL类型
          const messages = []
          const toolResults = {} // 临时存储工具返回结果

          // 第一遍遍历：收集所有TOOL消息的结果
          ;(session.content || []).forEach(msg => {
            if (msg.messageType === 'TOOL' && msg.content) {
              // TOOL消息的content是数组，包含工具返回结果
              msg.content.forEach(toolResult => {
                if (toolResult.id) {
                  toolResults[toolResult.id] = toolResult
                }
              })
            }
          })

          // 第二遍遍历：构建消息列表
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
              // 🔑 提取工具调用信息
              const toolCalls = msg.metadata?.toolCalls || []
              const toolUsages = toolCalls.map(tc => ({
                id: tc.id,
                name: tc.name,
                arguments: tc.arguments
              }))

              // 🔑 匹配工具返回结果
              const toolResponses = toolCalls
                .map(tc => toolResults[tc.id])
                .filter(Boolean)
                .map(tr => ({
                  id: tr.id,
                  name: tr.name,
                  responseData: tr.responseData
                }))

              // 🔑 构建segments数组，实现工具与文字穿插显示
              const segments = []

              // 添加工具调用
              toolUsages.forEach(tool => {
                segments.push({
                  type: 'toolUsage',
                  data: tool,
                  timestamp: new Date(msg.messageTime).getTime()
                })
              })

              // 添加工具返回
              toolResponses.forEach(response => {
                segments.push({
                  type: 'toolResponse',
                  data: response,
                  timestamp: new Date(msg.messageTime).getTime() + 1
                })
              })

              // 添加文字内容（如果有）
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
            // TOOL消息不单独显示，已经合并到对应的ASSISTANT消息中
          })

          return {
            sessionId: session.sessionId,
            agentId: session.agentId || 1,  // 🔑 agentId为null时默认设置为1（超级智能体）
            title: session.sessionName || '历史对话',
            createdAt: session.lastActive,
            lastActive: session.lastActive,
            messages: messages,  // ✅ 保存解析后的消息数组
            syncedWithBackend: true,
            isPinned: !!session.isTop,  // 🔑 使用后端返回的isTop字段判断置顶状态
            isTop: session.isTop  // 🔑 保存后端返回的置顶时间戳，用于排序
          }
        })

        // 更新后端缓存
        backendSessions.value[appKey] = backendSessionList

        // 合并到本地sessions（保留本地未同步的会话）
        mergeBackendSessions(appKey, backendSessionList)
      }

      // 🔑 关键：加载完成后强制校验置顶数量（防止旧数据超标）
      enforcePinLimit(appKey, 3)

    } catch (error) {
      console.error('[ChatStore] 从后端加载会话失败:', error)
      ElMessage.error('加载历史会话失败')
    } finally {
      isLoadingSessions.value = false
    }
  }

  /**
   * 从后端加载单个会话的详细消息
   * @param {string} sessionId - 会话ID
   * @param {string} appKey - 应用key
   * @returns {Promise<void>}
   */
  async function fetchSessionDetailFromBackend(sessionId, appKey) {
    if (!appKey) appKey = currentApp.value

    const agentId = appConfig[appKey]?.agentId
    if (!agentId || !sessionId) return

    try {
      const res = await sessionApi.getSessionBySessionId(agentId, sessionId)

      // 🔑 关键：res已经是 {code, message, data} 格式
      if (res.data) {
        const sessionData = res.data

        // 🔑🔑🔑 核心修复：统一消息格式转换逻辑（与fetchSessionsFromBackend保持一致）
        const messages = []
        const toolResults = {}

        // 第一遍遍历：收集所有TOOL消息的结果
        ;(sessionData.content || []).forEach(msg => {
          if (msg.messageType === 'TOOL' && msg.content) {
            msg.content.forEach(toolResult => {
              if (toolResult.id) {
                toolResults[toolResult.id] = toolResult
              }
            })
          }
        })

        // 第二遍遍历：构建消息列表
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

        // 更新本地session
        const sessionList = sessions.value[appKey]
        const sessionIndex = sessionList.findIndex(s => s.sessionId === sessionId)

        if (sessionIndex !== -1) {
          sessions.value[appKey][sessionIndex].messages = messages
          sessions.value[appKey][sessionIndex].syncedWithBackend = true

          // 如果当前正在查看这个会话，更新消息显示
          if (currentSessionId.value[appKey] === sessionId) {
            currentMessages.value = [...messages]
          }

          saveToLocalStorage()
        }

      }
    } catch (error) {
      console.error('[ChatStore] 加载会话详情失败:', error)
      ElMessage.error('加载会话详情失败')
    }
  }

  /**
   * 删除会话（同时删除本地和后端）
   * @param {string} sessionId - 会话ID
   * @param {string} appKey - 应用key
   * @returns {Promise<void>}
   */
  async function deleteSession(sessionId, appKey) {
    if (!appKey) appKey = currentApp.value
    
    const agentId = appConfig[appKey]?.agentId
    if (!agentId || !sessionId) return

    try {
      // 先尝试从后端删除
      await sessionApi.deleteSessionById(agentId, sessionId)
      
      // 删除成功后，从本地移除
      const sessionList = sessions.value[appKey]
      const index = sessionList.findIndex(s => s.sessionId === sessionId)
      
      if (index !== -1) {
        sessions.value[appKey].splice(index, 1)
        
        // 如果删除的是当前会话，切换到最新会话或清空
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
      console.error('[ChatStore] 删除会话失败:', error)
      ElMessage.error('删除会话失败')
    }
  }

  /**
   * 更新会话标题（同时更新本地和后端）
   * @param {string} sessionId - 会话ID
   * @param {string} sessionName - 新标题（符合controller.md文档参数名）
   * @param {string} appKey - 应用key
   * @returns {Promise<void>}
   */
  async function updateSessionTitle(sessionId, sessionName, appKey) {
    if (!appKey) appKey = currentApp.value

    const agentId = appConfig[appKey]?.agentId
    if (!agentId || !sessionId) return false

    try {
      // 调用后端接口更新标题（参数名：sessionName）
      await sessionApi.updateSessionTitle(agentId, sessionId, sessionName)

      // 更新本地数据
      const sessionList = sessions.value[appKey]
      const session = sessionList.find(s => s.sessionId === sessionId)

      if (session) {
        session.title = sessionName
        saveToLocalStorage()
      }

      return true
    } catch (error) {
      console.error('[ChatStore] 更新会话标题失败:', error)
      ElMessage.error('更新标题失败')
      return false
    }
  }

  /**
   * 置顶/取消置顶会话（最多3条置顶）
   * @param {string} sessionId - 会话ID
   * @param {string} appKey - 应用key
   * @returns {Promise<boolean>} - 是否成功执行（如果超过3条限制则返回false）
   */
  async function togglePinSession(sessionId, appKey) {
    if (!appKey) appKey = currentApp.value

    const sessionList = sessions.value[appKey]
    const session = sessionList.find(s => s.sessionId === sessionId)

    if (!session) {
      console.error('[ChatStore] ❌ 未找到会话:', sessionId)
      return false
    }

    // 🔑 关键：如果要置顶，先检查是否已达到上限（3条）
    if (!session.isPinned) {
      // 🔴 强制重新计算当前置顶数量（防止数据不一致）
      const currentPinnedCount = sessionList.filter(s => s.isPinned).length

      if (currentPinnedCount >= 3) {
        ElMessage.warning('⚠️ 最多只能置顶3条会话，请先取消其他置顶')
        return false  // 🔴 返回false，不执行任何操作
      }
    }

    // 🔑 调用后端API进行置顶/取消置顶
    try {
      const agentId = appConfig[appKey]?.agentId
      if (!agentId) {
        console.error('[ChatStore] 未找到agentId:', appKey)
        return false
      }

      // 🔑 根据当前状态调用不同的接口
      const res = session.isPinned
        ? await sessionApi.setUpTopSession(agentId, sessionId)  // 取消置顶
        : await sessionApi.setTopSession(agentId, sessionId)     // 置顶

      if (res.code === 200) {
        // ✅ 后端操作成功，更新本地状态
        if (!session.isPinned) {
          // 置顶：设置isTop为当前时间戳
          session.isPinned = true
          session.isTop = Date.now()
        } else {
          // 取消置顶：清空isTop
          session.isPinned = false
          session.isTop = null
        }
        session.lastActive = new Date().toISOString()
        saveToLocalStorage()

        // 🔑 重新排序会话列表（置顶的在前面）
        sessionList.sort((a, b) => {
          if (a.isTop && b.isTop) {
            return b.isTop - a.isTop  // 降序：isTop大的在前
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
      console.error('[ChatStore] 置顶操作失败:', error)
      ElMessage.error((session.isPinned ? '取消置顶' : '置顶') + '操作失败，请稍后重试')
      return false
    }
  }

  /**
   * 🔑 强制校验并修正置顶数量（防止旧数据超标）
   * 在加载数据后调用此函数
   */
  function enforcePinLimit(appKey, maxPins = 3) {
    if (!appKey) appKey = currentApp.value

    const sessionList = sessions.value[appKey]
    if (!sessionList || sessionList.length === 0) return

    const pinnedSessions = sessionList.filter(s => s.isPinned)

    if (pinnedSessions.length > maxPins) {
      // 🔴 超出限制，按时间排序后移除多余的
      const sortedByPinTime = [...pinnedSessions].sort((a, b) => {
        return new Date(a.lastActive || a.createdAt) - new Date(b.lastActive || b.createdAt)
      })

      // 移除最早置顶的（保留最新的maxPins个）
      const toUnpin = sortedByPinTime.slice(0, pinnedSessions.length - maxPins)

      toUnpin.forEach(session => {
        session.isPinned = false
      })

      saveToLocalStorage()
      ElMessage.warning(`已自动取消 ${toUnpin.length} 条置顶（超过${maxPins}条限制）`)
    }
  }

  /**
   * 🔑 绑定后端生成的sessionId（核心功能）
   * 当首次请求时，前端发送空sessionId
   * 后端通过SSE的init事件返回生成的sessionId
   * 前端调用此方法保存sessionId，用于后续请求
   * 
   * @param {string} backendSessionId - 后端生成的sessionId
   * @param {string} appKey - 应用key
   */
  function bindBackendSessionId(backendSessionId, appKey) {
    if (!appKey) appKey = currentApp.value
    if (!backendSessionId) {
      console.error('[ChatStore] bindBackendSessionId: sessionId为空')
      return
    }

    // 更新当前会话的sessionId
    const oldSessionId = currentSessionId.value[appKey]

    // 🔑🔑🔑 关键修复：处理空字符串的情况（新建会话时sessionId为''）
    // 旧代码：if (oldSessionId && oldSessionId !== backendSessionId)
    // 问题：空字符串是falsy值，导致条件永远为false，sessionId永远不会更新！
    const shouldUpdate = (
      oldSessionId === '' ||  // 空字符串（新会话）
      (oldSessionId && oldSessionId !== backendSessionId)  // 有值且不同
    )

    if (shouldUpdate) {
      const sessionList = sessions.value[appKey]

      // 🔑🔑🔑 优化：直接查找当前活跃会话（最后一个操作过的）
      let sessionIndex = -1

      if (oldSessionId === '') {
        // 新建会话：查找当前正在使用的会话（currentMessages对应的会话）
        // 策略1：查找sessionId为空的最新会话
        const emptySessions = sessionList.filter(s => !s.sessionId)
        if (emptySessions.length > 0) {
          // 找到最新的空sessionId会话（按lastActive排序）
          const sortedByTime = [...emptySessions].sort(
            (a, b) => new Date(b.lastActive || b.createdAt) - new Date(a.lastActive || a.createdAt)
          )
          sessionIndex = sessionList.findIndex(s => s === sortedByTime[0])
        }
      } else {
        // 已有临时ID的会话
        sessionIndex = sessionList.findIndex(s => s.sessionId === oldSessionId)
      }

      if (sessionIndex !== -1) {
        // 更新sessionId为后端生成的正式ID
        sessions.value[appKey][sessionIndex].sessionId = backendSessionId
        sessions.value[appKey][sessionIndex].syncedWithBackend = true
      } else {
        console.error('[ChatStore] ❌❌❌ 未找到需要更新的会话！oldSessionId:', oldSessionId)
      }
    }

    // 保存到currentSessionId
    currentSessionId.value[appKey] = backendSessionId

    saveToLocalStorage()
  }

  /**
   * 合并后端会话数据到本地
   * 🔑🔑🔑 关键修复：智能保留本地的metadata（thinking/toolUsages/toolResponses）
   * @param {string} appKey - 应用key
   * @param {Array} backendList - 后端会话列表
   */
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

  // ==================== 归档功能 ====================

  // 归档会话列表
  const archivedSessions = ref({
    superagent: [],
    manus: [],
    family: []
  })

  const isLoadingArchives = ref(false)

  /**
   * 归档指定会话
   * @param {string} sessionId - 会话ID
   * @param {string} appKey - 应用key
   * @returns {Promise<boolean>}
   */
  async function archiveSession(sessionId, appKey) {
    if (!appKey) appKey = currentApp.value
    const agentId = appConfig[appKey]?.agentId
    if (!agentId || !sessionId) return false

    try {
      await sessionApi.archiveSession(agentId, sessionId)
      ElMessage.success('归档请求已提交，正在异步处理中')
      return true
    } catch (error) {
      console.error('[ChatStore] 归档会话失败:', error)
      ElMessage.error('归档会话失败')
      return false
    }
  }

  /**
   * 查询会话归档状态
   * @param {string} sessionId - 会话ID
   * @returns {Promise<boolean>}
   */
  async function checkArchiveStatus(sessionId) {
    try {
      const res = await sessionApi.getArchiveStatus(sessionId)
      return res.data === true
    } catch (error) {
      console.error('[ChatStore] 查询归档状态失败:', error)
      return false
    }
  }

  /**
   * 加载归档会话列表
   * @param {string} appKey - 应用key
   * @param {number} pageNo
   * @param {number} pageSize
   * @returns {Promise<void>}
   */
  async function fetchArchivedSessions(appKey, pageNo = 1, pageSize = 20) {
    if (!appKey) appKey = currentApp.value
    const agentId = appConfig[appKey]?.agentId
    if (!agentId) {
      console.warn('[ChatStore] 加载归档会话失败: agentId为空，appKey=', appKey)
      return
    }

    isLoadingArchives.value = true
    try {
      const res = await sessionApi.getArchiveSessions(agentId, pageNo, pageSize)

      if (res.data && Array.isArray(res.data)) {
        archivedSessions.value[appKey] = res.data.map(session => {
          // 解析归档会话的完整消息内容
          const messages = []
          const toolResults = {}
          
          // 处理 session.content 字段：可能是字符串或数组
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

          // 第一遍遍历：收集所有TOOL消息的结果
          contentArray.forEach(msg => {
            if (msg.messageType === 'TOOL' && msg.content) {
              // 确保 msg.content 是数组
              const toolContentArray = Array.isArray(msg.content) ? msg.content : []
              toolContentArray.forEach(toolResult => {
                if (toolResult.id) toolResults[toolResult.id] = toolResult
              })
            }
          })

          // 第二遍遍历：构建消息列表
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
            messages, // ✅ 完整消息数据
            syncedWithBackend: true,
            isPinned: false,
            isArchived: true
          }
        })
      } else {
        archivedSessions.value[appKey] = []
      }
    } catch (error) {
      console.error('[ChatStore] 加载归档会话失败:', error)
      // request.js 拦截器已显示业务错误，这里仅补充日志
      ElMessage.error('加载归档会话失败，请检查控制台日志')
    } finally {
      isLoadingArchives.value = false
    }
  }

  function setStreamingResponse(streaming) {
    isStreamingResponse.value = !!streaming
  }

  // ==================== 工具函数 ====================

  function generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  }

  function saveToLocalStorage() {
    try {
      const data = JSON.stringify(sessions.value)
      // 容量保护：如果数据超过 4MB，清理旧会话的消息内容
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

  /**
   * 清理旧会话的消息内容以释放localStorage空间
   * 策略：保留最近3个会话的完整消息，其余只保留前5条消息
   */
  function trimOldSessionMessages() {
    Object.keys(sessions.value).forEach(appKey => {
      const sessionList = sessions.value[appKey]
      // 按 lastActive 降序排序
      const sorted = [...sessionList].sort(
        (a, b) => new Date(b.lastActive || b.createdAt) - new Date(a.lastActive || a.createdAt)
      )
      
      sorted.forEach((session, index) => {
        if (index >= 3 && session.messages && session.messages.length > 5) {
          // 只保留前5条消息
          session.messages = session.messages.slice(0, 5)
          session.messagesTrimmed = true
        }
      })
    })

    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions.value))
    } catch (e) {
      // 清理后仍无法保存，静默处理
    }
  }

  // 🔑🔑🔑 关键修复：保存当前会话ID到localStorage
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

  // 🔑 切换模型
  function switchModel(modelId) {
    const model = modelList.value.find(m => m.id === modelId)
    if (model) {
      currentModel.value = modelId
      return true
    }
    return false
  }

  // 🔑 从后端获取模型列表
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
      console.error('[ChatStore] 获取模型列表失败:', error)
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

  // 🔑 获取用户默认模型配置（参数回退）
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
      console.error('[ChatStore] 获取模型配置失败:', error)
    } finally {
      defaultConfigLoaded.value = true
    }
  }

  // 🔑 获取某个模型的有效参数（约束 + 默认 + 用户覆盖 合并）
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

  // 🔑 设置某个模型的参数覆盖
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
