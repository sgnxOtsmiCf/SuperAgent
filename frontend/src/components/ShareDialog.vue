<template>
  <div class="share-overlay" @click.self="handleClose">
    <div class="share-dialog">
      <div class="dialog-header">
        <h3>分享对话</h3>
        <el-button text circle :icon="Close" @click="handleClose" />
      </div>

      <div class="share-content">
        <!-- 提示信息 -->
        <div class="share-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>请选择要分享的消息（点击消息进行选中/取消）</span>
        </div>

        <!-- 消息选择列表 -->
        <div class="message-list" ref="messageListRef">
          <div
            v-for="(item, index) in displayItems"
            :key="index"
            class="message-item"
            :class="{ 
              'is-selected': selectedIndices.has(item.originalIndex),
              'is-user': item.type === 'user',
              'is-assistant': item.type === 'assistant',
              'is-tool-usage': item.type === 'toolUsage',
              'is-tool-response': item.type === 'toolResponse'
            }"
            @click="toggleSelection(item.originalIndex)"
          >
            <div class="message-checkbox">
              <el-checkbox :model-value="selectedIndices.has(item.originalIndex)" size="large" />
            </div>
            <div class="message-content-preview">
              <div class="message-role">
                <el-tag 
                  size="small" 
                  :type="getRoleTagType(item.type)"
                  :effect="item.type === 'toolUsage' || item.type === 'toolResponse' ? 'plain' : 'light'"
                >
                  {{ getRoleLabel(item.type) }}
                </el-tag>
                <!-- 工具名称标签 -->
                <el-tag 
                  v-if="item.toolName" 
                  size="small" 
                  type="info" 
                  effect="plain"
                  class="tool-name-tag"
                >
                  {{ item.toolName }}
                </el-tag>
              </div>
              <div class="message-text">{{ truncateContent(item.content, 100) }}</div>
            </div>
          </div>
        </div>

        <!-- 底部操作栏 -->
        <div class="share-actions">
          <div class="selected-count">
            已选择 <span class="count">{{ selectedIndices.size }}</span> 条消息
          </div>
          <div class="action-buttons">
            <el-button @click="handleClose">取消</el-button>
            <el-button type="primary" :disabled="selectedIndices.size === 0" @click="generateImage">
              <el-icon><Picture /></el-icon>
              生成长图
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览对话框 -->
    <div v-if="showImagePreview" class="preview-overlay" @click.self="showImagePreview = false">
      <div class="preview-dialog">
        <div class="preview-header">
          <h3>分享预览</h3>
          <el-button text circle :icon="Close" @click="showImagePreview = false" />
        </div>
        <div class="preview-content">
          <div ref="htmlContainerRef" class="html-container">
            <!-- 生成的 HTML 内容将放在这里 -->
            <div class="share-card">
              <!-- 头部 -->
              <div class="share-header">
                <div class="logo">
                  <span class="logo-icon">AI</span>
                  <span class="logo-text">对话分享</span>
                </div>
              </div>
              
              <!-- 消息列表 -->
              <div class="share-messages">
                <div 
                  v-for="(msg, index) in selectedMessagesForRender" 
                  :key="index"
                  class="share-message"
                  :class="msg.role"
                >
                  <!-- 用户消息 -->
                  <template v-if="msg.role === 'user'">
                    <div class="message-row user-row">
                      <div class="message-bubble user-bubble">
                        <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
                      </div>
                      <div class="avatar user-avatar">我</div>
                    </div>
                  </template>
                  
                  <!-- 助手消息 -->
                  <template v-else-if="msg.role === 'assistant'">
                    <div class="message-row assistant-row">
                      <div class="avatar assistant-avatar">AI</div>
                      <div class="assistant-content">
                        <!-- 助手文本 -->
                        <div v-if="msg.content" class="message-bubble assistant-bubble">
                          <div class="message-content markdown-body" v-html="renderMarkdown(msg.content)"></div>
                        </div>
                        
                        <!-- 工具调用 -->
                        <div v-if="msg.metadata?.toolUsages?.length" class="tool-section">
                          <div 
                            v-for="tool in msg.metadata.toolUsages" 
                            :key="tool.id"
                            class="tool-usage"
                          >
                            <div class="tool-header">
                              <span class="tool-icon">🔧</span>
                              <span class="tool-name">{{ tool.name }}</span>
                            </div>
                            <div class="tool-args">
                              <pre><code>{{ formatToolArgs(tool.arguments) }}</code></pre>
                            </div>
                          </div>
                        </div>
                        
                        <!-- 工具返回 -->
                        <div v-if="msg.metadata?.toolResponses?.length" class="tool-section">
                          <div 
                            v-for="resp in msg.metadata.toolResponses" 
                            :key="resp.id"
                            class="tool-response"
                          >
                            <div class="tool-header">
                              <span class="tool-icon">✅</span>
                              <span class="tool-result">返回结果</span>
                            </div>
                            <div class="tool-result-content">
                              <pre><code>{{ formatToolResponse(resp.responseData) }}</code></pre>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
              </div>
              
              <!-- 底部 -->
              <div class="share-footer">
                <span>由 SuperAgent 生成 | 作者 sgnxotsmicf</span>
              </div>
            </div>
          </div>
        </div>
        <div class="preview-actions">
          <el-button @click="showImagePreview = false">返回</el-button>
          <el-button type="primary" :loading="isGenerating" @click="downloadImage">
            <el-icon><Download /></el-icon>
            下载图片
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { Close, InfoFilled, Picture, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import html2canvas from 'html2canvas'
import { logger } from '@/utils/logger'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  initialMessageIndex: {
    type: Number,
    default: -1
  }
})

const emit = defineEmits(['close'])

const selectedIndices = ref(new Set())
const showImagePreview = ref(false)
const htmlContainerRef = ref(null)
const messageListRef = ref(null)
const isGenerating = ref(false)

// 解析消息，提取工具调用信息（根据前端实际存储结构）
const displayItems = computed(() => {
  const items = []
  
  props.messages.forEach((msg, index) => {
 // 优先使用 messageType，如果没有则根据 role 推断
    let messageType = msg.messageType
    if (!messageType && msg.role) {
      messageType = msg.role.toUpperCase()
    }
    
 // 用户消息 (messageType: "USER" 或 role: "user")
    if (messageType === 'USER' || msg.role === 'user') {
      items.push({
        type: 'user',
        content: msg.content,
        originalIndex: index,
        toolName: null
      })
    }
 // 助手消息 (messageType: "ASSISTANT" 或 role: "assistant")
    else if (messageType === 'ASSISTANT' || msg.role === 'assistant') {
 // 添加助手文本内容
      if (msg.content) {
        items.push({
          type: 'assistant',
          content: msg.content,
          originalIndex: index,
          toolName: null
        })
      }
      
 // 从 metadata.toolUsages 中提取工具调用信息（前端存储的字段名）
      const toolUsages = msg.metadata?.toolUsages || []
      const toolResponses = msg.metadata?.toolResponses || []
      
 // 添加工具调用
      toolUsages.forEach((tool) => {
        items.push({
          type: 'toolUsage',
          content: `调用工具: ${tool.name}\n参数: ${formatToolArgs(tool.arguments)}`,
          originalIndex: index,
          toolName: tool.name,
          toolId: tool.id,
          toolData: tool
        })
      })
      
 // 添加工具返回结果（从前端 metadata.toolResponses 获取）
      toolResponses.forEach((response) => {
        items.push({
          type: 'toolResponse',
          content: `工具返回结果: ${formatToolResponse(response.responseData)}`,
          originalIndex: index,
          toolName: response.name,
          toolId: response.id,
          responseData: response.responseData
        })
      })
    }
  })
  
  return items
})

// 获取选中的消息（用于渲染）
const selectedMessagesForRender = computed(() => {
  const selected = []
  for (let i = 0; i < props.messages.length; i++) {
    if (selectedIndices.value.has(i)) {
      selected.push(props.messages[i])
    }
  }
  return selected
})

// 格式化工具参数
function formatToolArgs(args) {
  if (!args) return '无参数'
  try {
    const parsed = typeof args === 'string' ? JSON.parse(args) : args
    return JSON.stringify(parsed, null, 2)
  } catch {
    return String(args)
  }
}

// 格式化工具返回
function formatToolResponse(data) {
  if (!data) return '无返回数据'
  if (typeof data === 'string') {
 // 尝试解析 JSON
    try {
      const parsed = JSON.parse(data)
      return JSON.stringify(parsed, null, 2)
    } catch {
      return data
    }
  }
  try {
    return JSON.stringify(data, null, 2)
  } catch {
    return String(data)
  }
}

// 渲染 Markdown
function renderMarkdown(content) {
  if (!content) return ''
  try {
    const rawHtml = marked.parse(content, { async: false })
    return DOMPurify.sanitize(typeof rawHtml === 'string' ? rawHtml : String(rawHtml), {
      ADD_TAGS: ['pre', 'code', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'span', 'div'],
      ADD_ATTR: ['class', 'language']
    })
  } catch (e) {
    return DOMPurify.sanitize(content)
  }
}

// 获取角色标签类型
function getRoleTagType(type) {
  switch (type) {
    case 'user': return 'primary'
    case 'assistant': return 'success'
    case 'toolUsage': return 'warning'
    case 'toolResponse': return 'info'
    default: return 'default'
  }
}

// 获取角色标签文字
function getRoleLabel(type) {
  switch (type) {
    case 'user': return '用户'
    case 'assistant': return '助手'
    case 'toolUsage': return '工具调用'
    case 'toolResponse': return '工具返回'
    default: return '未知'
  }
}

// 初始化选中状态：默认选中当前消息和最近一条其他角色的消息
function initSelection() {
  selectedIndices.value.clear()
  
  if (props.initialMessageIndex >= 0 && props.initialMessageIndex < props.messages.length) {
    const currentMsg = props.messages[props.initialMessageIndex]
    selectedIndices.value.add(props.initialMessageIndex)
    
 // 查找最近一条其他角色的消息
    const otherRole = currentMsg.role === 'user' ? 'assistant' : 'user'
    let nearestIndex = -1
    let minDistance = Infinity
    
    for (let i = 0; i < props.messages.length; i++) {
      if (i !== props.initialMessageIndex && props.messages[i].role === otherRole) {
        const distance = Math.abs(i - props.initialMessageIndex)
        if (distance < minDistance) {
          minDistance = distance
          nearestIndex = i
        }
      }
    }
    
    if (nearestIndex !== -1) {
      selectedIndices.value.add(nearestIndex)
    }
  }
}

// 初始化
initSelection()

function toggleSelection(index) {
  if (selectedIndices.value.has(index)) {
    selectedIndices.value.delete(index)
  } else {
    selectedIndices.value.add(index)
  }
}

function truncateContent(content, maxLength) {
  if (!content) return ''
 // 移除 Markdown 标记
  const plainText = content.replace(/[#*`\[\]()]/g, '').replace(/\n/g, ' ')
  if (plainText.length <= maxLength) return plainText
  return plainText.substring(0, maxLength) + '...'
}

function handleClose() {
  emit('close')
}

// 生成分享图片（使用 html2canvas）
async function generateImage() {
  if (selectedIndices.value.size === 0) {
    ElMessage.warning('请至少选择一条消息')
    return
  }

 // 先显示预览对话框，渲染 HTML
  showImagePreview.value = true
  
 // 等待 DOM 更新和 Markdown 渲染
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 300))
  
  ElMessage.success('预览已生成，点击下载按钮保存图片')
}

// 下载图片
async function downloadImage() {
  if (!htmlContainerRef.value) {
    ElMessage.error('生成区域未找到')
    return
  }
  
  isGenerating.value = true
  
  try {
    const element = htmlContainerRef.value.querySelector('.share-card')
    if (!element) {
      throw new Error('分享卡片未找到')
    }
    
 // 使用 html2canvas 生成图片
    const canvas = await html2canvas(element, {
      backgroundColor: '#f8f9fa',
      scale: 2, // 高清输出
      useCORS: true,
      allowTaint: true,
      logging: false,
      windowWidth: 800,
      width: 800
    })
    
 // 下载图片
    const link = document.createElement('a')
    link.download = `对话分享_${new Date().getTime()}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
    
    ElMessage.success('图片已下载')
  } catch (error) {
    logger.error('生成图片失败:', error)
    ElMessage.error('生成图片失败: ' + error.message)
  } finally {
    isGenerating.value = false
  }
}
</script>

<style lang="scss" scoped>
.share-overlay {
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

.share-dialog {
  background-color: #fff;
  border-radius: 16px;
  width: 650px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.dialog-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }
}

.share-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.share-tip {
  margin: 16px 20px 0;
  padding: 12px 16px;
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #096dd9;

  .el-icon {
    color: #1890ff;
    font-size: 16px;
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #d0d0d0;
    background-color: #f9fafb;
  }

  &.is-selected {
    border-color: #1890ff;
    background-color: #e6f7ff;
  }

  &.is-tool-usage {
    border-left: 4px solid #faad14;
  }

  &.is-tool-response {
    border-left: 4px solid #52c41a;
  }

  .message-checkbox {
    flex-shrink: 0;
    padding-top: 2px;
  }

  .message-content-preview {
    flex: 1;
    min-width: 0;

    .message-role {
      margin-bottom: 6px;
      display: flex;
      align-items: center;
      gap: 8px;

      .tool-name-tag {
        font-size: 11px;
      }
    }

    .message-text {
      font-size: 14px;
      color: #4b5563;
      line-height: 1.5;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      white-space: pre-wrap;
    }
  }
}

.share-actions {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .selected-count {
    font-size: 14px;
    color: #666;

    .count {
      color: #1890ff;
      font-weight: 600;
    }
  }

  .action-buttons {
    display: flex;
    gap: 12px;
  }
}

// 预览对话框
.preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
}

.preview-dialog {
  background-color: #fff;
  border-radius: 16px;
  width: 900px;
  max-width: 95vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.preview-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }
}

.preview-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
  display: flex;
  justify-content: center;
  background-color: #f5f5f5;

  .html-container {
    width: 800px;
    max-width: 100%;
  }
}

.preview-actions {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

// 分享卡片样式（用于 html2canvas 截图）
.share-card {
  background-color: #f8f9fa;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.share-header {
  background-color: #fff;
  padding: 24px 32px;
  border-bottom: 1px solid #e5e7eb;

  .logo {
    display: flex;
    align-items: center;
    gap: 12px;

    .logo-icon {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, #1890ff, #36cfc9);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-weight: bold;
      font-size: 14px;
    }

    .logo-text {
      font-size: 20px;
      font-weight: 600;
      color: #1a1a1a;
    }
  }
}

.share-messages {
  padding: 24px 32px;
}

.share-message {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.message-row {
  display: flex;
  gap: 12px;

  &.user-row {
    justify-content: flex-end;
  }

  &.assistant-row {
    justify-content: flex-start;
  }
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 13px;
  flex-shrink: 0;

  &.user-avatar {
    background-color: #1890ff;
    color: #fff;
  }

  &.assistant-avatar {
    background-color: #10b981;
    color: #fff;
  }
}

.assistant-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: calc(100% - 60px);
}

.message-bubble {
  padding: 16px 20px;
  border-radius: 16px;
  max-width: 100%;

  &.user-bubble {
    background-color: #1890ff;
    color: #fff;
    border-bottom-right-radius: 4px;
  }

  &.assistant-bubble {
    background-color: #fff;
    color: #1a1a1a;
    border: 1px solid #e5e7eb;
    border-bottom-left-radius: 4px;
  }
}

.message-content {
  font-size: 15px;
  line-height: 1.7;
  word-wrap: break-word;

 // Markdown 样式
  :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
    margin: 18px 0 12px;
    font-weight: 700;
    line-height: 1.35;
    color: #111827;
  }

  :deep(h1) { font-size: 21px; }
  :deep(h2) {
    font-size: 18px;
    padding-bottom: 8px;
    border-bottom: 2px solid #e5e7eb;
  }
  :deep(h3) { font-size: 16px; color: #1e40af; }

  :deep(p) {
    margin: 8px 0;
  }

  :deep(strong) {
    font-weight: 700;
    color: #111827;
  }

  :deep(em) {
    font-style: italic;
  }

  :deep(code) {
    background-color: #fef2f2;
    color: #dc2626;
    padding: 2px 6px;
    border-radius: 5px;
    font-family: 'SF Mono', Monaco, Consolas, monospace;
    font-size: 13px;
    border: 1px solid #fecaca;
  }

  :deep(pre) {
    background-color: #0d1117;
    padding: 18px;
    border-radius: 10px;
    overflow-x: auto;
    margin: 14px 0;
    border: 1px solid #30363d;

    code {
      background: none;
      padding: 0;
      color: #e6edf3;
      border: none;
    }
  }

  :deep(ul), :deep(ol) {
    margin: 8px 0;
    padding-left: 24px;
  }

  :deep(li) {
    margin: 4px 0;
  }

  :deep(blockquote) {
    border-left: 4px solid #8b5cf6;
    margin: 14px 0;
    padding: 12px 16px;
    color: #1e3a5f;
    background-color: #f5f3ff;
    border-radius: 0 8px 8px 0;
  }

  :deep(a) {
    color: #2563eb;
    text-decoration: none;
  }

  :deep(table) {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
    margin: 14px 0;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #e5e7eb;
    font-size: 13px;

    th, td {
      border-bottom: 1px solid #f3f4f6;
      border-right: 1px solid #f3f4f6;
      padding: 10px 14px;
      text-align: left;

      &:last-child { border-right: none; }
    }

    th {
      background-color: #f1f5f9;
      font-weight: 600;
      color: #1e293b;
      border-bottom: 2px solid #e2e8f0;
    }

    tr:nth-child(even) {
      background-color: #f9fafb;
    }
  }
}

// 工具区域样式
.tool-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tool-usage,
.tool-response {
  background-color: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 12px;
  padding: 12px 16px;
  max-width: 100%;
}

.tool-response {
  background-color: #f6ffed;
  border-color: #b7eb8f;
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;

  .tool-icon {
    font-size: 16px;
  }

  .tool-name {
    font-weight: 600;
    color: #ad6800;
  }

  .tool-result {
    font-weight: 600;
    color: #389e0d;
  }
}

.tool-args,
.tool-result-content {
  pre {
    margin: 0;
    background-color: rgba(0, 0, 0, 0.03);
    padding: 12px;
    border-radius: 8px;
    overflow-x: auto;

    code {
      font-family: 'Monaco', 'Menlo', monospace;
      font-size: 12px;
      color: #374151;
      line-height: 1.5;
    }
  }
}

.share-footer {
  background-color: #fff;
  padding: 16px 32px;
  border-top: 1px solid #e5e7eb;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}
</style>