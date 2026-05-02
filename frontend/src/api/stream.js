import { parseError, getFriendlyErrorMessage } from '@/utils/errorHandler'
import { logger } from '@/utils/logger'
import { ElMessageBox } from 'element-plus'

export async function fetchStream(apiPath, method, params, onMessage, onError, onComplete, abortController) {
 // 关键：在发送请求前检查是否已登录
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessageBox.alert(
      '请先登录后再发送消息',
      '未登录',
      {
        confirmButtonText: '去登录',
        type: 'warning',
        callback: () => {
 // 触发自定义事件打开登录对话框
          window.dispatchEvent(new CustomEvent('show-login-dialog'))
        }
      }
    )
 // 调用onError让上层知道请求被取消了
    onError(new Error('未登录'))
    return
  }

  let url = `/api${apiPath}`
  const SYSTEM_MESSAGES = [
    '模型推理完毕',
    'Agent响应完成',
    '工具执行完成',
    'Tool execution completed',
    'Model inference complete',
    'Response complete'
  ]

  try {
    if (method === 'GET') {
      const queryString = Object.keys(params)
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
        .join('&')
      url += `?${queryString}`
    }

    const options = {
      method: method,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
      },
      signal: abortController?.signal
    }

    if (method === 'POST') {
      options.body = JSON.stringify(params)
    }

    const response = await fetch(url, options)

    if (!response.ok) {
      let backendErrorMsg = ''
      try {
        const errorBody = await response.text()
        try {
          const parsed = JSON.parse(errorBody)
          backendErrorMsg = parsed.message || parsed.msg || parsed.error || parsed.data || ''
        } catch {
          backendErrorMsg = errorBody.substring(0, 200)
        }
      } catch {
        backendErrorMsg = `HTTP ${response.status}`
      }

      const httpError = new Error(backendErrorMsg || `HTTP ${response.status}`)
      httpError.status = response.status
      httpError.isBackendError = true
      throw httpError
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('响应流不可用')
    }

    const decoder = new TextDecoder()
    let buffer = ''
    let currentEventType = null
    let currentDataLines = []

    const dispatchEvent = (eventType, rawData) => {
      const normalizedEventType = eventType?.trim() || null
      const data = rawData ?? ''
      const trimmedData = data.trim()

      if (!trimmedData || trimmedData === '[DONE]') {
        return
      }

      if (['modelFinish', 'complete', 'toolFinish'].includes(normalizedEventType)) {
        return
      }

      if (normalizedEventType === 'init' || (normalizedEventType === null && trimmedData.includes('"sessionId"'))) {
        try {
          const parsed = JSON.parse(trimmedData)
          if (parsed.sessionId) {
            onMessage({
              type: 'init',
              sessionId: parsed.sessionId,
              content: '',
              raw: parsed,
              isError: false
            })
          }
        } catch (e) {
          logger.error('[Stream] 解析init数据失败:', e)
        }

        return
      }

      if (normalizedEventType === 'thinking' ||
          (normalizedEventType === null && trimmedData.includes('"thinking"'))) {
        try {
          const thinkingData = JSON.parse(trimmedData)
          const thinkingContent = thinkingData.thinking || thinkingData.content || data

          onMessage({
            type: 'thinking',
            content: thinkingContent,
            raw: thinkingData,
            isError: false
          })
        } catch (e) {
          if (data.length > 0 && data.length < 2000) {
            onMessage({
              type: 'thinking',
              content: data,
              raw: { raw: data },
              isError: false
            })
          }
        }

        return
      }

      if (normalizedEventType === 'toolUsage' ||
          (normalizedEventType === null && (trimmedData.includes('"type":"function"') || trimmedData.includes('"name":')))) {
        try {
          let toolData = JSON.parse(trimmedData)

          if (!Array.isArray(toolData)) {
            toolData = [toolData]
          }

          onMessage({
            type: 'toolUsage',
            tools: toolData,
            raw: toolData,
            isError: false
          })
        } catch (e) {
          logger.error('[Stream] 解析toolUsage失败:', e)
        }

        return
      }

      if (normalizedEventType === 'toolResponse' ||
          (normalizedEventType === null && trimmedData.includes('"responseData"'))) {
        try {
          let responseData = JSON.parse(trimmedData)

          if (!Array.isArray(responseData)) {
            responseData = [responseData]
          }

          onMessage({
            type: 'toolResponse',
            responses: responseData,
            raw: responseData,
            isError: false
          })
        } catch (e) {
          logger.error('[Stream] 解析toolResponse失败:', e)
        }

        return
      }

      if (normalizedEventType === 'error') {
 // 解析错误数据，提取 code 和 message
        let errorContent = trimmedData.substring(0, 500)
        let errorCode = null
        
        try {
          const parsedError = JSON.parse(trimmedData)
          errorCode = parsedError.code
          errorContent = parsedError.message || parsedError.msg || parsedError.data || trimmedData
        } catch {
 // 如果不是JSON格式，使用原始数据
        }
        
 // 使用错误处理工具获取友好提示
        const friendlyMessage = getFriendlyErrorMessage(errorCode, errorContent)
        
        onMessage({
          type: 'error',
          content: friendlyMessage,
          raw: data,
          isError: true,
          code: errorCode
        })
        return
      }

      if (normalizedEventType === 'message' || normalizedEventType === null) {
        if (SYSTEM_MESSAGES.some(msg => trimmedData.includes(msg))) {
          return
        }

        if (trimmedData.toLowerCase().startsWith('error:')) {
          onMessage({
            type: 'error',
            content: trimmedData.substring(0, 200),
            raw: data,
            isError: true
          })
          return
        }

        let content = ''

        try {
          const parsed = JSON.parse(trimmedData)

          if (parsed.eventType?.includes('FAIL') || parsed.eventType === 'AGENT_FAIL') {
            onMessage({
              type: 'error',
              content: parsed.message || parsed.data || 'Agent执行出错',
              raw: parsed,
              isError: true
            })
            return
          }

          if (parsed.message?.text) {
            content = parsed.message.text
          } else if (parsed.content) {
            content = parsed.content
          } else if (parsed.data && typeof parsed.data === 'string' && !parsed.data.includes('{')) {
            content = parsed.data
          }

        } catch (jsonError) {
          if (data.length > 0 && data.length < 5000) {
            content = data
          }
        }

        if (content && content.length > 0) {
          onMessage({
            type: 'message',
            content,
            raw: { content },
            isError: false
          })
        }
      }
    }

    const flushEvent = () => {
      if (currentEventType === null && currentDataLines.length === 0) {
        return
      }

      const data = currentDataLines.join('\n')
      dispatchEvent(currentEventType, data)

      currentEventType = null
      currentDataLines = []
    }

    while (true) {
      if (abortController?.signal.aborted) {
        reader.cancel()
        onComplete()
        return
      }

      let readResult
      try {
        readResult = await reader.read()
      } catch (readError) {
        if (abortController?.signal.aborted) {
          onComplete()
          return
        }
        const connError = new Error(readError.message || '连接中断')
        connError.isConnectionLost = true
        throw connError
      }

      const { done, value } = readResult

      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const rawLine of lines) {
        const line = rawLine.replace(/\r$/, '')

        if (line === '') {
          flushEvent()
          continue
        }

        if (line.startsWith(':')) {
          continue
        }

        if (line.startsWith('event:')) {
          currentEventType = line.slice(6).trim() || null
          continue
        }

        if (line.startsWith('data:')) {
          let dataLine = line.slice(5)
          if (dataLine.startsWith(' ')) {
            dataLine = dataLine.slice(1)
          }
          currentDataLines.push(dataLine)
        }
      }
    }

    buffer += decoder.decode()

    if (buffer.length > 0) {
      const line = buffer.replace(/\r$/, '')

      if (line.startsWith('data:')) {
        let dataLine = line.slice(5)
        if (dataLine.startsWith(' ')) {
          dataLine = dataLine.slice(1)
        }
        currentDataLines.push(dataLine)
      } else if (line.startsWith('event:')) {
        currentEventType = line.slice(6).trim() || null
      }
    }

    flushEvent()

    onComplete()

  } catch (error) {
    const isAbortError = error.name === 'AbortError' ||
      error.message?.toLowerCase().includes('abort') ||
      error.message?.includes('ERR_ABORTED') ||
      error.message?.includes('net::ERR_ABORTED') ||
      abortController?.signal.aborted

    if (isAbortError && !error.isBackendError && !error.isConnectionLost) {
      onComplete()
      return
    }

    onError(error)
  }
}
