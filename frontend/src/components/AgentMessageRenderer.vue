<template>
  <div class="agent-message-renderer">
    <!-- 1. 模型思考过程 -->
    <div v-if="metadata?.thinking" class="thinking-section">
      <div class="thinking-header" @click="toggleExpand('thinking')">
        <el-icon class="thinking-icon" :class="{ 'is-animated': metadata.isThinking }">
          <MagicStick />
        </el-icon>
        <span class="thinking-title">{{ metadata.isThinking ? '模型思考中...' : '模型思考过程' }}</span>
        <el-icon class="expand-icon" :class="{ 'is-expanded': expandedSections.thinking }">
          <ArrowRight />
        </el-icon>
      </div>
      <div v-show="expandedSections.thinking" class="thinking-content" ref="thinkingContent">
        {{ metadata.thinking }}
      </div>
    </div>

    <!-- 2. 工具调用信息（显示在文字内容之前） -->
    <div v-if="hasToolUsages" class="tools-section">
      <div v-for="(tool, index) in toolUsages" :key="'tool-' + index" class="tool-usage-card">
        <div class="tool-header" @click="toggleExpand('tool-' + index)">
          <el-icon class="tool-icon"><Setting /></el-icon>
          <span class="tool-name">{{ tool.name || '调用工具' }}</span>
          <el-tag size="small" :type="getToolStatusType(tool)" effect="plain">
            {{ getToolStatusText(tool) }}
          </el-tag>
          <el-icon class="expand-icon" :class="{ 'is-expanded': expandedSections['tool-' + index] }">
            <ArrowRight />
          </el-icon>
        </div>
        <div v-show="expandedSections['tool-' + index]" class="tool-detail">
          <div v-if="tool.arguments" class="tool-arguments">
            <div class="arguments-label">参数</div>
            <pre class="arguments-content">{{ formatArguments(tool.arguments) }}</pre>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 工具返回结果（显示在工具调用之后） -->
    <div v-if="hasToolResponses" class="tools-response-section">
      <div v-for="(response, index) in toolResponses" :key="'response-' + index" class="tool-response-card">
        <div class="response-header" @click="toggleExpand('response-' + index)">
          <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
          <span class="response-tool-name">{{ response.name || '工具完成' }}</span>
          <el-tag size="small" type="success" effect="plain">完成</el-tag>
          <el-icon class="expand-icon" :class="{ 'is-expanded': expandedSections['response-' + index] }">
            <ArrowRight />
          </el-icon>
        </div>
        <div v-show="expandedSections['response-' + index]" class="response-detail">
          <div class="response-content">
            <pre>{{ formatResponseData(response.responseData) }}</pre>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. Markdown 内容渲染 - 直接使用 content -->
    <div v-if="content" class="markdown-content">
      <MarkdownRenderer :markdown="content" :is-streaming="isStreaming" />
    </div>

    <!-- 5. 空状态/加载中 -->
    <div v-if="!content && !metadata?.thinking && !hasToolUsages" class="empty-state">
      <el-skeleton :rows="2" animated />
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, ref, watch, nextTick } from 'vue'
import { MagicStick, Setting, CircleCheckFilled, ArrowRight } from '@element-plus/icons-vue'
import MarkdownRenderer from './MarkdownRenderer.vue'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  metadata: {
    type: Object,
    default: () => ({})
  },
  isStreaming: {
    type: Boolean,
    default: false
  }
})

const expandedSections = reactive({})
const thinkingContent = ref(null)

const toolUsages = computed(() => props.metadata?.toolUsages || [])
const toolResponses = computed(() => props.metadata?.toolResponses || [])
const hasToolUsages = computed(() => toolUsages.value.length > 0)
const hasToolResponses = computed(() => toolResponses.value.length > 0)

function toggleExpand(key) {
  expandedSections[key] = !expandedSections[key]
}

function getToolStatusType(tool) {
  const hasResponse = toolResponses.value.some(r => r.name === tool.name || r.id === tool.id)
  return hasResponse ? 'success' : 'warning'
}

function getToolStatusText(tool) {
  const hasResponse = toolResponses.value.some(r => r.name === tool.name || r.id === tool.id)
  return hasResponse ? '完成' : '调用中'
}

function formatArguments(args) {
  if (typeof args === 'string') {
    try {
      return JSON.stringify(JSON.parse(args), null, 2)
    } catch {
      return args
    }
  } else if (typeof args === 'object') {
    return JSON.stringify(args, null, 2)
  }
  return String(args)
}

function formatResponseData(data) {
  if (!data) return '(无返回数据)'
  if (typeof data === 'string') {
    try {
      return JSON.stringify(JSON.parse(data), null, 2)
    } catch {
      return data
    }
  } else if (typeof data === 'object') {
    return JSON.stringify(data, null, 2)
  }
  return String(data)
}

// 流式输出时自动展开思考卡片并滚动到底部
watch(() => props.metadata?.thinking, async (newVal) => {
  if (props.isStreaming && newVal && !expandedSections.thinking) {
    // 流式输出时自动展开思考卡片
    expandedSections.thinking = true
    
    // 等待 DOM 更新后滚动到底部
    await nextTick()
    if (thinkingContent.value) {
      thinkingContent.value.scrollTop = thinkingContent.value.scrollHeight
    }
  }
})

// 思考内容更新时滚动到底部
watch(() => props.metadata?.thinking, async () => {
  if (props.isStreaming && expandedSections.thinking) {
    await nextTick()
    if (thinkingContent.value) {
      thinkingContent.value.scrollTop = thinkingContent.value.scrollHeight
    }
  }
})
</script>

<style lang="scss" scoped>
.agent-message-renderer {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;

  .thinking-section {
    width: 100%;
    max-width: 100%;
    margin-bottom: 12px;
    border-left: 3px solid #0ea5e9;
    border-radius: 6px;
    background: #f0f9ff;
    overflow: hidden;
    box-sizing: border-box;

    .thinking-header {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 12px;
      cursor: pointer;
      user-select: none;
      transition: background 0.2s;
      width: 100%;
      max-width: 100%;
      box-sizing: border-box;

      &:hover {
        background: rgba(14, 165, 233, 0.08);
      }

      .thinking-icon {
        font-size: 14px;
        color: #0ea5e9;
        flex-shrink: 0;

        &.is-animated {
          animation: pulse 1.5s ease-in-out infinite;
        }
      }

      .thinking-title {
        flex: 1;
        font-size: 13px;
        font-weight: 500;
        color: #0369a1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .expand-icon {
        font-size: 12px;
        color: #94a3b8;
        transition: transform 0.2s;

        &.is-expanded {
          transform: rotate(90deg);
        }
      }
    }

    .thinking-content {
      padding: 0 12px 10px;
      font-size: 13px;
      line-height: 1.6;
      color: #475569;
      white-space: pre-wrap;
      word-break: break-word;
      max-height: 160px;
      overflow-y: auto;
      width: 100%;
      max-width: 100%;
      box-sizing: border-box;

      &::-webkit-scrollbar {
        width: 3px;
      }

      &::-webkit-scrollbar-thumb {
        background-color: #cbd5e1;
        border-radius: 2px;
      }
    }
  }

  .tools-section {
    width: 100%;
    max-width: 100%;
    margin-bottom: 12px;
    box-sizing: border-box;
  }

  .tool-usage-card {
    width: 100%;
    max-width: 100%;
    border-left: 3px solid #f59e0b;
    border-radius: 6px;
    background: #fffbeb;
    overflow: hidden;
    margin-bottom: 8px;
    box-sizing: border-box;

    .tool-header {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 7px 12px;
      cursor: pointer;
      user-select: none;
      transition: background 0.2s;
      width: 100%;
      max-width: 100%;
      box-sizing: border-box;

      &:hover {
        background: rgba(245, 158, 11, 0.08);
      }

      .tool-icon {
        font-size: 14px;
        color: #d97706;
        flex-shrink: 0;
      }

      .tool-name {
        flex: 1;
        font-size: 13px;
        font-weight: 500;
        color: #92400e;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .expand-icon {
        font-size: 12px;
        color: #94a3b8;
        transition: transform 0.2s;

        &.is-expanded {
          transform: rotate(90deg);
        }
      }
    }

    .tool-detail {
      padding: 0 12px 8px;

      .tool-arguments {
        background: rgba(255, 255, 255, 0.7);
        padding: 8px;
        border-radius: 4px;
        border: 1px solid #fde68a;

        .arguments-label {
          font-size: 11px;
          font-weight: 600;
          color: #b45309;
          margin-bottom: 4px;
          text-transform: uppercase;
          letter-spacing: 0.5px;
        }

        .arguments-content {
          font-family: 'SFMono-Regular', Consolas, monospace;
          font-size: 12px;
          line-height: 1.4;
          color: #78350f;
          white-space: pre-wrap;
          word-break: break-all;
          margin: 0;
          max-height: 120px;
          overflow-y: auto;

          &::-webkit-scrollbar {
            width: 3px;
          }

          &::-webkit-scrollbar-thumb {
            background: #d1d5db;
            border-radius: 2px;
          }
        }
      }
    }
  }

  .tools-response-section {
    width: 100%;
    max-width: 100%;
    margin-bottom: 12px;
    box-sizing: border-box;
  }

  .tool-response-card {
    width: 100%;
    max-width: 100%;
    border-left: 3px solid #059669;
    border-radius: 6px;
    background: #ecfdf5;
    overflow: hidden;
    margin-bottom: 8px;
    box-sizing: border-box;

    .response-header {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 7px 12px;
      cursor: pointer;
      user-select: none;
      transition: background 0.2s;
      width: 100%;
      max-width: 100%;
      box-sizing: border-box;

      &:hover {
        background: rgba(5, 150, 105, 0.08);
      }

      .success-icon {
        font-size: 14px;
        color: #059669;
        flex-shrink: 0;
      }

      .response-tool-name {
        flex: 1;
        font-size: 13px;
        font-weight: 500;
        color: #047857;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .expand-icon {
        font-size: 12px;
        color: #94a3b8;
        transition: transform 0.2s;

        &.is-expanded {
          transform: rotate(90deg);
        }
      }
    }

    .response-detail {
      padding: 0 12px 8px;

      .response-content {
        background: rgba(255, 255, 255, 0.7);
        padding: 8px;
        border-radius: 4px;
        border: 1px solid #a7f3d0;

        pre {
          font-family: 'SFMono-Regular', Consolas, monospace;
          font-size: 12px;
          line-height: 1.4;
          color: #065f46;
          white-space: pre-wrap;
          word-break: break-all;
          margin: 0;
          max-height: 160px;
          overflow-y: auto;

          &::-webkit-scrollbar {
            width: 3px;
          }

          &::-webkit-scrollbar-thumb {
            background: #99f6e4;
            border-radius: 2px;
          }
        }
      }
    }
  }

  .markdown-content {
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    padding: 0;

    :deep(.markdown-renderer) {
      width: 100% !important;
      max-width: 100% !important;
      padding: 0;
    }
  }

  .empty-state {
    padding: 16px;
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.95);
  }
}
</style>
