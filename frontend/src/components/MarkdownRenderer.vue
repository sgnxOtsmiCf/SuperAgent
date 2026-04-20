<template>
  <div
    class="markdown-renderer"
    ref="rendererRef"
    v-html="renderedHtml"
    @click="handleRendererClick"
  ></div>
</template>

<script setup>
import { computed, ref, watch, nextTick } from 'vue'
import { marked, Renderer } from 'marked'
import DOMPurify from 'dompurify'
import katex from 'katex'
import 'katex/dist/katex.min.css'

const props = defineProps({
  markdown: {
    type: String,
    default: ''
  },
  isDark: {
    type: Boolean,
    default: false
  },
  showCodeBlockHeader: {
    type: Boolean,
    default: true
  }
})

const rendererRef = ref(null)
const copyResetTimers = new WeakMap()
const markdownRenderer = new Renderer()

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false
})

// 预处理 LaTeX 公式，将其转换为占位符
function preprocessLatex(content) {
  if (!content) return { content: '', formulas: [] }
  
  // 保存公式的数组
  const formulas = []
  
  // 处理块级公式 $$...$$
  content = content.replace(/\$\$([\s\S]*?)\$\$/g, (match, formula) => {
    formulas.push({ type: 'block', formula: formula.trim() })
    return `__LATEX_BLOCK_${formulas.length - 1}__`
  })
  
  // 处理行内公式 $...$（排除转义的 \$ 和表情符号如 ✅）
  // 使用更严格的匹配：公式应该包含数学相关字符
  content = content.replace(/(?<!\\)\$([\s\S]*?)(?<!\\)\$/g, (match, formula) => {
    const trimmed = formula.trim()
    // 如果内容看起来像数学公式（包含字母、数字、运算符等），则处理
    // 排除纯表情符号或特殊字符
    if (/[a-zA-Z0-9\\+\-\*\/\=\^_\{\}\(\)\[\]]/.test(trimmed) && trimmed.length > 0) {
      formulas.push({ type: 'inline', formula: trimmed })
      return `__LATEX_INLINE_${formulas.length - 1}__`
    }
    // 否则保留原始内容
    return match
  })
  
  return { content, formulas }
}

// 渲染 LaTeX 公式
function renderLatex(formula, isBlock = false) {
  try {
    return katex.renderToString(formula, {
      throwOnError: false,
      displayMode: isBlock,
      strict: false
    })
  } catch (error) {
    console.warn('[MarkdownRenderer] LaTeX render error:', error)
    return isBlock 
      ? `<div class="latex-error">$$${formula}$$</div>`
      : `<span class="latex-error">$${formula}$</span>`
  }
}

// 后处理，将占位符替换为渲染后的公式
function postprocessLatex(content, formulas) {
  if (!formulas || formulas.length === 0) return content
  
  formulas.forEach((item, index) => {
    const placeholder = item.type === 'block' 
      ? `__LATEX_BLOCK_${index}__`
      : `__LATEX_INLINE_${index}__`
    const rendered = renderLatex(item.formula, item.type === 'block')
    content = content.replace(placeholder, rendered)
  })
  
  return content
}

function preprocessIncompleteMarkdown(content) {
  if (!content) return ''
  let processed = content
  
  // 1. 修复不完整的代码块
  const codeBlockCount = (processed.match(/```/g) || []).length
  if (codeBlockCount % 2 !== 0) {
    processed += '\n```'
  }
  
  // 2. 修复可能被截断的列表项（处理特殊字符如 ✅ 在列表中的情况）
  // 如果行以 - 或 * 或数字. 开头但没有内容，添加占位符
  processed = processed.replace(/^(\s*[-*]\s*)$/gm, '$1 ')
  processed = processed.replace(/^(\s*\d+\.\s*)$/gm, '$1 ')
  
  // 3. 修复可能被截断的标题（### ✅ 这样的情况）
  // 如果行以 # 结尾，添加空格
  processed = processed.replace(/^(#{1,6})$/gm, '$1 ')
  
  return processed
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function renderCodeBlock(code, infostring) {
  const language = (infostring || '').trim().split(/\s+/)[0]
  const languageLabel = language || 'text'
  const escapedCode = escapeHtml(code)
  const codeClassAttr = language ? ` class="language-${escapeHtml(language)}"` : ''

  if (!props.showCodeBlockHeader) {
    return `<pre><code${codeClassAttr}>${escapedCode}</code></pre>`
  }

  return `
    <div class="code-block">
      <div class="code-block-header">
        <span class="code-block-language">${escapeHtml(languageLabel)}</span>
        <button type="button" class="code-copy-button" aria-label="复制代码">复制</button>
      </div>
      <pre><code${codeClassAttr}>${escapedCode}</code></pre>
    </div>
  `
}

markdownRenderer.code = (code, infostring) => {
  return renderCodeBlock(code, infostring)
}

async function copyText(text) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  textarea.style.pointerEvents = 'none'
  document.body.appendChild(textarea)
  textarea.select()

  try {
    const success = document.execCommand('copy')
    if (!success) {
      throw new Error('execCommand copy failed')
    }
  } finally {
    document.body.removeChild(textarea)
  }
}

function setCopyButtonState(button, text, copied = false) {
  button.textContent = text
  button.classList.toggle('is-copied', copied)

  const existingTimer = copyResetTimers.get(button)
  if (existingTimer) {
    clearTimeout(existingTimer)
  }

  const timer = setTimeout(() => {
    if (button.isConnected) {
      button.textContent = '复制'
      button.classList.remove('is-copied')
    }
    copyResetTimers.delete(button)
  }, copied ? 1600 : 2000)

  copyResetTimers.set(button, timer)
}

async function handleRendererClick(event) {
  const button = event.target.closest('.code-copy-button')
  if (!button || !rendererRef.value?.contains(button)) {
    return
  }

  const codeElement = button.closest('.code-block')?.querySelector('pre code')
  const codeText = codeElement?.textContent || ''
  if (!codeText) {
    setCopyButtonState(button, '无内容')
    return
  }

  try {
    await copyText(codeText)
    setCopyButtonState(button, '已复制', true)
  } catch (error) {
    console.error('[MarkdownRenderer] copy failed:', error)
    setCopyButtonState(button, '复制失败')
  }
}

function renderMarkdown(markdown) {
  if (!markdown || markdown.trim() === '') {
    return ''
  }

  try {
    // 1. 预处理 LaTeX 公式
    const { content: processedContent, formulas } = preprocessLatex(markdown)
    
    // 2. 预处理不完整的 Markdown
    const preprocessed = preprocessIncompleteMarkdown(processedContent)
    
    // 3. 解析 Markdown
    const html = marked.parse(preprocessed, {
      async: false,
      renderer: markdownRenderer
    })

    if (typeof html !== 'string') {
      return String(html)
    }

    // 4. 后处理 LaTeX 公式
    const finalHtml = postprocessLatex(html, formulas)

    // 5. 清理 HTML
    return DOMPurify.sanitize(finalHtml, {
      ADD_TAGS: ['pre', 'code', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'input', 'button', 'span', 'div', 'math', 'annotation'],
      ADD_ATTR: ['class', 'language', 'data-language', 'type', 'checked', 'disabled', 'aria-label', 'style', 'aria-hidden']
    })
  } catch (error) {
    console.warn('[MarkdownRenderer] parse error:', error)
    return markdown
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>')
  }
}

const renderedHtml = computed(() => renderMarkdown(props.markdown))

// 监听 markdown 变化，在 DOM 更新后重新渲染 KaTeX
watch(() => props.markdown, () => {
  nextTick(() => {
    // KaTeX 已经在 renderMarkdown 中渲染完成
    // 这里可以添加额外的 DOM 操作如果需要
  })
}, { immediate: true })
</script>

<style lang="scss">
.markdown-renderer {
  font-size: 15px;
  line-height: 1.8;
  color: #1a1a1a;
  word-wrap: break-word;
  overflow-wrap: break-word;

  // KaTeX 公式样式
  .katex {
    font-size: 1.1em;
  }
  
  .katex-display {
    margin: 16px 0;
    overflow-x: auto;
    overflow-y: hidden;
  }
  
  .latex-error {
    color: #d73a49;
    background-color: #ffe6e6;
    padding: 2px 4px;
    border-radius: 3px;
    font-family: monospace;
  }

  h1, h2, h3, h4, h5, h6 {
    margin-top: 24px;
    margin-bottom: 16px;
    font-weight: 600;
    line-height: 1.4;

    &:first-child {
      margin-top: 0;
    }
  }

  h1 { font-size: 28px; border-bottom: 2px solid #eaeaea; padding-bottom: 8px; }
  h2 { font-size: 22px; border-bottom: 1px solid #eaeaea; padding-bottom: 6px; }
  h3 { font-size: 18px; }
  h4 { font-size: 16px; }

  p {
    margin-top: 0;
    margin-bottom: 16px;
  }

  ul, ol {
    margin-top: 8px;
    margin-bottom: 16px;
    padding-left: 28px;
  }

  li {
    margin-bottom: 6px;
    line-height: 1.7;

    > p {
      margin-bottom: 6px;
    }
  }

  ul {
    list-style-type: disc;

    ul {
      list-style-type: circle;

      ul {
        list-style-type: square;
      }
    }
  }

  ol {
    list-style-type: decimal;

    ol {
      list-style-type: lower-alpha;

      ol {
        list-style-type: lower-roman;
      }
    }
  }

  input[type="checkbox"] {
    margin-right: 8px;
    vertical-align: middle;
  }

  a {
    color: #0969da;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  img {
    max-width: 100%;
    height: auto;
    border-radius: 6px;
    margin: 12px 0;
  }

  hr {
    height: 2px;
    background-color: #eaeaea;
    border: none;
    margin: 24px 0;
  }

  blockquote {
    margin: 16px 0;
    padding: 12px 20px;
    border-left: 4px solid #0969da;
    background-color: #f6f8fa;
    color: #57606a;

    p {
      margin: 0;
    }
  }

  code:not(pre code) {
    background-color: #f3f4f6;
    color: #d73a49;
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
    font-size: 13px;
    border: 1px solid #e1e4e8;
  }

  pre {
    margin: 16px 0;
    padding: 16px;
    background-color: #f6f8fa;
    border: 1px solid #e1e4e8;
    border-radius: 8px;
    overflow-x: auto;
    position: relative;

    code {
      background-color: transparent;
      padding: 0;
      border: none;
      border-radius: 0;
      font-size: 14px;
      line-height: 1.6;
      color: #24292e;
    }
  }

  .code-block {
    margin: 16px 0;
    border: 1px solid #e1e4e8;
    border-radius: 10px;
    overflow: hidden;
    background-color: #f6f8fa;

    .code-block-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 14px;
      background: linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
      border-bottom: 1px solid #e1e4e8;
    }

    .code-block-language {
      font-size: 12px;
      line-height: 1;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: #64748b;
    }

    .code-copy-button {
      appearance: none;
      border: 1px solid #d0d7de;
      background: #ffffff;
      color: #334155;
      border-radius: 6px;
      padding: 4px 10px;
      font-size: 12px;
      line-height: 1.2;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        border-color: #94a3b8;
        background: #f8fafc;
      }

      &.is-copied {
        color: #065f46;
        border-color: #6ee7b7;
        background: #ecfdf5;
      }
    }

    pre {
      margin: 0;
      border: none;
      border-radius: 0;
      background-color: transparent;
    }
  }

  table {
    width: 100%;
    margin: 16px 0;
    border-collapse: collapse;
    border-spacing: 0;
    overflow: auto;
    border: 1px solid #d0d7de;

    th, td {
      padding: 10px 14px;
      border: 1px solid #d0d7de;
      text-align: left;
    }

    th {
      font-weight: 600;
      background-color: #f9fafb;
    }

    tr:nth-child(even) {
      background-color: #f6f8fa;
    }
  }

  strong {
    font-weight: 600;
    color: #1a1a1a;
  }

  em {
    font-style: italic;
  }

  del {
    text-decoration: line-through;
    color: #6a737d;
  }

  sup, sub {
    font-size: 75%;
    line-height: 0;
    position: relative;
    vertical-align: baseline;
  }

  sup { top: -0.5em; }
  sub { bottom: -0.25em; }

  .fallback-content {
    white-space: pre-wrap;
    word-break: break-word;
    line-height: 1.75;
  }

  .raw-content {
    white-space: pre-wrap;
    word-break: break-word;
    font-family: monospace;
    font-size: 13px;
    line-height: 1.6;
    color: #333;
    background-color: #f9f9f9;
    padding: 12px;
    border-radius: 6px;
  }
}
</style>