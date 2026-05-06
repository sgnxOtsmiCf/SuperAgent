<template>
  <div
      class="markdown-renderer"
      ref="rendererRef"
      v-html="renderedHtml"
      @click="handleRendererClick"
  ></div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { marked, Renderer } from 'marked'
import DOMPurify from 'dompurify'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.min.css'
import { logger } from '@/utils/logger'

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
  },
  isStreaming: {
    type: Boolean,
    default: false
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

 // 处理块级公式 $...$
  content = content.replace(/\$\$([\s\S]*?)\$\$/g, (match, formula) => {
    formulas.push({ type: 'block', formula: formula.trim() })
    return `__LATEX_BLOCK_${formulas.length - 1}__`
  })

 // 处理行内公式 $...$（排除转义的 \$ 和表情符号如 ）
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
    logger.warn('[MarkdownRenderer] LaTeX render error:', error)
    return isBlock
        ? `<div class="latex-error">$${formula}$</div>`
        : `<span class="latex-error">${formula}$</span>`
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

// 保护代码块内容：提取所有围栏代码块替换为占位符，避免预处理正则破坏代码块内的内容
// 例如 #{keyword}、-X:、一、等模式在代码块内不应被当作 markdown 语法处理
function protectCodeBlocks(content) {
  const blocks = []
  const lines = content.split('\n')
  const result = []
  let inCodeBlock = false
  let fenceChar = ''
  let fenceIndent = ''
  let currentBlockLines = []

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const fenceMatch = line.match(/^([ \t]*)(`{3,}|~{3,})/)

    if (!inCodeBlock && fenceMatch) {
      inCodeBlock = true
      fenceIndent = fenceMatch[1]
      fenceChar = fenceMatch[2].charAt(0)
      currentBlockLines = [line]
    } else if (inCodeBlock) {
      currentBlockLines.push(line)
      const closeMatch = line.match(/^([ \t]*)(`{3,}|~{3,})\s*$/)
      if (closeMatch && closeMatch[1] === fenceIndent && closeMatch[2].charAt(0) === fenceChar) {
        blocks.push(currentBlockLines.join('\n'))
        result.push(`__CODE_BLOCK_${blocks.length - 1}__`)
        currentBlockLines = []
        inCodeBlock = false
        fenceIndent = ''
        fenceChar = ''
      }
    } else {
      result.push(line)
    }
  }

  if (inCodeBlock && currentBlockLines.length > 0) {
    blocks.push(currentBlockLines.join('\n'))
    result.push(`__CODE_BLOCK_${blocks.length - 1}__`)
  }

  return { content: result.join('\n'), blocks }
}

function restoreCodeBlocks(content, blocks) {
  if (!blocks || blocks.length === 0) return content
  return content.replace(/__CODE_BLOCK_(\d+)__/g, (_, i) => blocks[parseInt(i)] || '')
}

function countNonListAsterisks(content) {
  const lines = content.split('\n')
  let count = 0
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    if (/^\s*\*\s/.test(line)) continue
    let inCode = false
    for (let j = 0; j < line.length; j++) {
      if (line[j] === '`') {
        inCode = !inCode
        continue
      }
      if (inCode) continue
      if (line[j] === '*' && line[j - 1] !== '*' && line[j + 1] !== '*') {
        count++
      }
    }
  }
  return count
}

function countNonWordUnderscores(content) {
  let count = 0
  let inCode = false
  for (let i = 0; i < content.length; i++) {
    if (content[i] === '`') {
      inCode = !inCode
      continue
    }
    if (inCode) continue
    if (content[i] === '_' && content[i - 1] !== '_' && content[i + 1] !== '_') {
      const prevChar = content[i - 1] || ' '
      const nextChar = content[i + 1] || ' '
      if (/\w/.test(prevChar) || /\w/.test(nextChar)) {
        count++
      }
    }
  }
  return count
}

function hasTableStructure(content) {
  const lines = content.split('\n')
  let pipeLineCount = 0
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim()
    if (!line) continue
    
    // 检查是否包含至少2个管道符
    const pipeCount = (line.match(/\|/g) || []).length
    if (pipeCount >= 2) {
      pipeLineCount++
    }
    
    // 检查表格分隔行（支持任意列数）
    if (/^(\s*\|?\s*[:-]+\s*\|)+\s*$/.test(line) || /^(\s*[:-]+\s*\|)+\s*[:-]+\s*$/.test(line)) {
      return true
    }
  }
  
  // 如果有至少一行是表格行格式，就认为有表格结构
  return pipeLineCount >= 1
}

function closeOpenCodeFence(content) {
  const fenceRe = /^([ \t]*)(```+|~~~+)/gm
  const fences = []
  let match
  while ((match = fenceRe.exec(content)) !== null) {
    fences.push({ indent: match[1], backticks: match[2], pos: match.index })
  }
  if (fences.length === 0 || fences.length % 2 === 0) return content
  const lastOpen = fences[fences.length - 1]
  return content + '\n' + lastOpen.indent + lastOpen.backticks
}

function detectBlockContext(content) {
  if (!content) return { inCodeBlock: false, inBlockquote: false, inList: false, listDepth: 0, inTable: false }

  const lines = content.split('\n')
  let inCodeBlock = false
  let inBlockquote = false
  let inList = false
  let listDepth = 0
  let inTable = false

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue

    const fenceMatch = line.match(/^([ \t]*)(```+|~~~+)/)
    if (fenceMatch) {
      inCodeBlock = !inCodeBlock
      continue
    }
    if (inCodeBlock) continue

    if (/^(\s*)>\s?/.test(line)) {
      inBlockquote = true
      continue
    }

    const listMatch = line.match(/^(\s*)([-*+]|\d+[.)])\s/)
    if (listMatch) {
      inList = true
      inBlockquote = false
      listDepth = Math.floor(listMatch[1].length / 2) + 1
      continue
    }

    // 表格分隔行检测（支持任意列数）
    // 表格分隔行特征：由管道符分隔的多个单元格，每个单元格包含至少3个-或:字符
    // 例如：|------|------|----------| 或 |:-:|:---|---:|
    if (/^(\s*\|?\s*[:-]+\s*\|)+\s*$/.test(trimmed) || /^(\s*[:-]+\s*\|)+\s*[:-]+\s*$/.test(trimmed)) {
      inTable = true
      continue
    }

    // 缩进内容（列表项延续），保持当前状态
    if (/^\s{2,}\S/.test(line)) {
      continue
    }

    // 非缩进非特殊行：重置引用；标题/分割线则同时重置列表
    inBlockquote = false
    // 简化正则：只要以 1-6 个 # 开头就重置列表
    if (/^#{1,6}/u.test(line) || /^[-*_]{3,}\s*$/.test(line)) {
      inList = false
      listDepth = 0
    }
  }

  return { inCodeBlock, inBlockquote, inList, listDepth, inTable }
}

function sanitizeUnstableLine(line, blockContext) {
  if (!line) return line

  const ctx = blockContext || { inCodeBlock: false, inBlockquote: false, inList: false, listDepth: 0, inTable: false }
  if (ctx.inCodeBlock) return line

  const trimmed = line.trim()

  // 检测表格行：任何包含多个管道符的行都视为表格行
  const pipeCount = (trimmed.match(/\|/g) || []).length
  if (pipeCount >= 2 && /^\|.*\|/.test(trimmed)) {
    return line
  }

  // 注意：不再在此处处理标题，因为：
  // 1. 完整标题行会被 isCompleteBlockLine 检测到并移到 stablePart
  // 2. stablePart 使用 marked.parse() 可以正确渲染标题
  // 3. 此处插入零宽空格会破坏标题语法

  // 无序列表标记：不在列表中时用反斜杠转义
  const ulMatch = line.match(/^(\s*)([-*+])(\s)/)
  if (ulMatch) {
    if (!ctx.inList) {
      return line.replace(/^(\s*)([-*+])(\s)/, '$1\\$2$3')
    }
    return line
  }

  // 有序列表标记：不在列表中时用反斜杠转义
  const olMatch = line.match(/^(\s*)(\d+[.)])(\s)/)
  if (olMatch) {
    if (!ctx.inList) {
      return line.replace(/^(\s*)(\d+[.)])(\s)/, '$1\\$2$3')
    }
    return line
  }

  // 引用：不在引用块中时转义
  if (/^(\s*)>\s/.test(line) && !ctx.inBlockquote) {
    return line.replace(/^(\s*)(>)(\s)/, '$1\\$2$3')
  }

  // 代码围栏：不稳定行中出现几乎总是意外，转义
  if (/^[ \t]*```/.test(line) || /^[ \t]*~~~/.test(line)) {
    return line.replace(/^([ \t]*)(```+|~~~+)/, '$1\\`\\`\\`')
  }

  // 表格管道：仅当不是表格上下文时才转义
  // 以 | 开头且包含管道符的行，很可能是表格
  if (/^(\s*)\|/.test(line) && pipeCount >= 1) {
    return line
  }

  // 水平分割线
  if (/^(-{3,}|_{3,}|\*{3,})\s*$/.test(trimmed)) {
    return line.replace(/^(\s*)([-_*]{3,})/, '$1\\$2')
  }

  return line
}

// 常见 HTML 标签白名单，用于安全闭合未闭合标签
const KNOWN_HTML_TAGS = new Set([
  'div', 'span', 'p', 'a', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
  'ul', 'ol', 'li', 'br', 'hr', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'code', 'pre', 'blockquote', 'em', 'strong', 'del', 'ins', 'sub', 'sup',
  'section', 'article', 'header', 'footer', 'nav', 'main', 'aside',
  'details', 'summary', 'input', 'button', 'select', 'option', 'textarea',
  'dl', 'dt', 'dd', 'figure', 'figcaption', 'caption', 'colgroup', 'col',
  'iframe', 'video', 'audio', 'source'
])

function closeOpenHtmlTag(content) {
  const openTagRe = /<([a-zA-Z][a-zA-Z0-9]*)((?:\s+(?:[a-zA-Z][a-zA-Z0-9-]*(?:=(?:"[^"]*"|'[^']*'|\{[^\}]*\}|[^\s>]*))?))*)\s*>?$/g
  let match
  let lastMatch = null
  while ((match = openTagRe.exec(content)) !== null) {
    lastMatch = { index: match.index, tagName: match[1].toLowerCase(), fullMatch: match[0] }
  }

  if (!lastMatch) return content
  if (!KNOWN_HTML_TAGS.has(lastMatch.tagName)) return content
  if (lastMatch.fullMatch.endsWith('>')) return content

  return content + '>'
}

function preprocessIncompleteMarkdown(content, streaming = false) {
  if (!content) return ''
  let processed = content

  if (streaming) {
    const lastNewlineIndex = processed.lastIndexOf('\n')
    let stablePart = ''
    let unstablePart = ''

    if (lastNewlineIndex === -1) {
      unstablePart = processed
    } else {
      stablePart = processed.substring(0, lastNewlineIndex + 1)
      unstablePart = processed.substring(lastNewlineIndex + 1)
    }

 // 先修复稳定部分的块级结构（代码围栏闭合等）
    if (stablePart) {
      stablePart = applyAllMarkdownFixes(stablePart)
    }

 // 基于修复后的稳定部分检测块级上下文
    const blockContext = stablePart ? detectBlockContext(stablePart) : null

 // 不稳定部分：先转义块级语法冲突，再修复内联元素
    if (unstablePart) {
      unstablePart = applyInlineOnlyFixes(unstablePart, blockContext)
    }

    return stablePart + unstablePart
  }

  return applyAllMarkdownFixes(processed)
}

function applyAllMarkdownFixes(processed) {
  const { content: protectedContent, blocks } = protectCodeBlocks(processed)
  let result = protectedContent

  // 0. 修复标题前有非标题内容的情况（流式输出常见）
  // 例如：Python与Java核心区别##一、语言特性 → Python与Java核心区别\n##一、语言特性
  // 注意：使用捕获组替代 (?<!^|\n) lookbehind，因为 JS 中 ^ 在 lookbehind 中不作为锚点
  result = result.replace(/([^\n])(#{1,6})/g, '$1\n$2')

  // 0.5. 修复中文式标题（#后无空格），转换为标准格式
  // 例如：##一、语言特性 → ## 一、语言特性
  result = result.replace(/^(#{1,6})([^\s#])/gm, '$1 $2')

  // 0.6. 修复非标准列表格式：-X: → - X: （在 - 后插入空格）
  // 例如：-Python: → - Python:
  // 注意：不匹配标准的 - 空格 格式，也不匹配水平分割线 ---
  // 修复：保留冒号后的内容
  result = result.replace(/^(\s*)-([^\s\n-])/gm, '$1- $2')

  // 0.7. 修复中文序号标题：将 "一、XXX" 转换为 "## 一、XXX"
  // 例如：二、关键差异要点 → ## 二、关键差异要点
  // 支持：一、二、三...十、以及数字序号
  // 注意：需要处理中文序号+分隔符+内容的格式
  // 排除已经是列表项的行（以 - * + 数字. 开头）
  result = result.replace(/^(一|二|三|四|五|六|七|八|九|十)[、.].+$/gm, (match) => {
    if (/^\s*(?:[-*+]|\d+[.)])\s/.test(match)) return match
    return '## ' + match
  })
  result = result.replace(/^([0-9]+)[、.].+$/gm, (match) => {
    if (/^\s*(?:[-*+]|\d+[.)])\s/.test(match)) return match
    return '## ' + match
  })

  // 1. 修复标题后紧跟表格的情况（流式输出常见）
  // 例如：##标题|列1|列2| → 在标题和表格之间插入换行
  // 例如：##标题|列1|列2| → 在标题和表格之间插入换行
  // 注意：这里需要在标准化标题格式之后执行，所以同时处理有/无空格的情况
  result = result.replace(/^(#{1,6}\s*.+?)\|(.+)$/gm, '$1\n|$2')

  // 1.5 修复粗体和标题在同一行的情况
  // 例如：## 一些文字 **粗体** → 确保粗体标记正确闭合
  const boldCount = (result.match(/\*\*/g) || []).length
  if (boldCount % 2 !== 0) {
    result += '**'
  }
  const underlineBoldCount = (result.match(/__/g) || []).length
  if (underlineBoldCount % 2 !== 0) {
    result += '__'
  }

  // 2. 修复不完整的代码块（保留缩进，避免破坏列表/引用嵌套）
  result = closeOpenCodeFence(result)

  // 3. 修复可能被截断的列表项（支持 - * + 和 1. 1) 样式）
  result = result.replace(/^(\s*[-*+]\s*)$/gm, '$1 ')
  result = result.replace(/^(\s*\d+[.)]\s*)$/gm, '$1 ')

  // 4. 修复可能被截断的标题
  result = result.replace(/^(#{1,6})$/gm, '$1 ')

  // 6. 修复不完整的斜体（单星号 *italic*，排除列表标记符号）
  const singleAsteriskCount = countNonListAsterisks(result)
  if (singleAsteriskCount % 2 !== 0) {
    result += '*'
  }

  // 7. 修复不完整的斜体（单下划线 _italic_，仅匹配单词边界的下划线）
  const singleUnderscoreCount = countNonWordUnderscores(result)
  if (singleUnderscoreCount % 2 !== 0) {
    result += '_'
  }

  // 8. 修复不完整的行内代码
  const backtickCount = (result.match(/`/g) || []).length
  if (backtickCount % 2 !== 0) {
    result += '`'
  }

  // 9. 修复不完整的链接 [text](url
  if (/\[[^\]]+\]\([^)]*$/.test(result)) {
    result += ')'
  }

  // 10. 修复不完整的表格行
  if (hasTableStructure(result)) {
    const lines = result.split('\n')
    let lastLineIdx = lines.length - 1
    while (lastLineIdx >= 0 && !lines[lastLineIdx].trim()) {
      lastLineIdx--
    }
    if (lastLineIdx >= 0) {
      const lastLine = lines[lastLineIdx]
      if (lastLine && lastLine.includes('|') && !lastLine.trim().endsWith('|')) {
        const trimmedLastLine = lastLine.trim()
        if (!/^\s*\|?[-:\s|]+[-:\s|]+\|?\s*$/.test(trimmedLastLine)) {
          lines[lastLineIdx] = lastLine + ' |'
          result = lines.join('\n')
        }
      }
    }
  }

  // 11. 修复不完整的引用块
  result = result.replace(/^(\s*>\s*)$/gm, '$1 ')

  // 12. 修复可能被截断的 HTML 标签（仅白名单标签，避免误匹配比较运算符）
  result = closeOpenHtmlTag(result)

  return restoreCodeBlocks(result, blocks)
}

function applyInlineOnlyFixes(line, blockContext) {
 // 对于正在输入的行(incomplete line)，先转义可能破坏块结构的语法
  let processed = sanitizeUnstableLine(line, blockContext)

 // 1. 修复不完整的行内代码
  const backtickCount = (processed.match(/`/g) || []).length
  if (backtickCount % 2 !== 0) {
    processed += '`'
  }

 // 2. 修复不完整的粗体（双星号）
  const boldCount = (processed.match(/\*\*/g) || []).length
  if (boldCount % 2 !== 0) {
    processed += '**'
  }

 // 3. 修复不完整的斜体（单星号，排除以"* "开头的情况因为那可能是新列表项的开头）
  const nonListAsterisk = processed.replace(/^\s*\*\s/, '')
  const singleAsteriskMatches = nonListAsterisk.match(/(?<!\*)\*(?!\*)/g) || []
  if (singleAsteriskMatches.length % 2 !== 0) {
    processed += '*'
  }

 // 4. 修复不完整的粗体/斜体（下划线样式）
  const underlineBoldCount = (processed.match(/__/g) || []).length
  if (underlineBoldCount % 2 !== 0) {
    processed += '__'
  }

 // 5. 修复不完整的斜体（单下划线）
  let singleUnderscoreCount = 0
  let inCode = false
  for (let i = 0; i < processed.length; i++) {
    if (processed[i] === '`') {
      inCode = !inCode
      continue
    }
    if (inCode) continue
    if (processed[i] === '_' && processed[i - 1] !== '_' && processed[i + 1] !== '_') {
      const prevChar = processed[i - 1] || ' '
      const nextChar = processed[i + 1] || ' '
      if (/\w/.test(prevChar) || /\w/.test(nextChar)) {
        singleUnderscoreCount++
      }
    }
  }
  if (singleUnderscoreCount % 2 !== 0) {
    processed += '_'
  }

 // 6. 修复不完整的链接
  if (/\[[^\]]+\]\([^)]*$/.test(processed)) {
    processed += ')'
  }

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

function highlightCode(code, language) {
  if (language && hljs.getLanguage(language)) {
    try {
      return hljs.highlight(code, { language, ignoreIllegals: true }).value
    } catch (_) { /* fallback */ }
  }
  try {
    return hljs.highlightAuto(code).value
  } catch (_) { /* fallback */ }
  return escapeHtml(code)
}

function renderCodeBlock(code, infostring) {
  const language = (infostring || '').trim().split(/\s+/)[0]
  const languageLabel = language || 'text'
  const highlightedCode = highlightCode(code, language)
  const codeClassAttr = language ? ` class="hljs language-${escapeHtml(language)}"` : ' class="hljs"'

  if (!props.showCodeBlockHeader) {
    return `<pre><code${codeClassAttr}>${highlightedCode}</code></pre>`
  }

  return `
    <div class="code-block">
      <div class="code-block-header">
        <span class="code-block-language">${escapeHtml(languageLabel)}</span>
        <button type="button" class="code-copy-button" aria-label="复制代码">
          <svg class="copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
          <span class="copy-text">复制</span>
        </button>
      </div>
      <pre><code${codeClassAttr}>${highlightedCode}</code></pre>
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
  const textEl = button.querySelector('.copy-text')
  if (textEl) textEl.textContent = text
  button.classList.toggle('is-copied', copied)

  const existingTimer = copyResetTimers.get(button)
  if (existingTimer) {
    clearTimeout(existingTimer)
  }

  const timer = setTimeout(() => {
    if (button.isConnected) {
      const el = button.querySelector('.copy-text')
      if (el) el.textContent = '复制'
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
    logger.error('[MarkdownRenderer] copy failed:', error)
    setCopyButtonState(button, '复制失败')
  }
}

// 关闭未闭合的内联格式（bold、italic、inline code、links）
// 仅处理内联元素，不处理块级结构 — 配合 marked.parseInline() 使用
function closeInlineFormatting(text) {
  if (!text) return text

  const { content: protectedText, blocks } = protectCodeBlocks(text)
  let result = protectedText

  // 0. 修复标题前有非标题内容的情况（流式输出常见）
  // 例如：Python与Java核心区别##一、语言特性 → Python与Java核心区别\n##一、语言特性
  // 注意：不使用 (?<!^|\n) 因为 JS 中 ^ 在 lookbehind 中不被识别为锚点
  result = result.replace(/([^\n])(#{1,6})/g, '$1\n$2')
  
  // 0.5. 修复中文式标题（#后无空格），转换为标准格式
  result = result.replace(/^(#{1,6})([^\s#])/gm, '$1 $2')

  // 0.6. 修复非标准列表格式：-X: → - X: （在 - 后插入空格）
  // 例如：-Python: → - Python:
  // 修复：保留冒号后的内容，简化正则
  result = result.replace(/^(\s*)-([^\s\n-])/gm, '$1- $2')

  // 0.7. 修复中文序号标题：将 "一、XXX" 转换为 "## 一、XXX"
  result = result.replace(/^(一|二|三|四|五|六|七|八|九|十)[、.].+$/gm, '## $&')
  result = result.replace(/^([0-9]+)[、.].+$/gm, '## $&')

  // 1. 检查是否有完整的粗体（**text**），不添加额外的闭合标记
  const doubleAsteriskMatches = result.match(/\*\*/g) || []
  if (doubleAsteriskMatches.length % 2 !== 0) result += '**'

  // 2. 闭合粗体（__）
  const doubleUnderscoreMatches = result.match(/__/g) || []
  if (doubleUnderscoreMatches.length % 2 !== 0) result += '__'

  // 3. 闭合斜体（*），排除列表标记和已配对的情况
  const singleAsteriskCount = countNonListAsterisks(result)
  if (singleAsteriskCount % 2 !== 0) result += '*'

  // 4. 闭合斜体（_），仅单词边界的下划线
  const singleUnderscoreCount = countNonWordUnderscores(result)
  if (singleUnderscoreCount % 2 !== 0) result += '_'

  // 5. 闭行内代码
  const backtickCount = (result.match(/`/g) || []).length
  if (backtickCount % 2 !== 0) result += '`'

  // 6. 闭合未完成的链接/图片 [text](url 或 ![alt](url
  const lastParenOpen = result.lastIndexOf('](')
  if (lastParenOpen !== -1) {
    const rest = result.substring(lastParenOpen + 2)
    if (!rest.includes(')')) {
      result += ')'
    }
  }

  return restoreCodeBlocks(result, blocks)
}

function isCompleteBlockLine(line, stablePart) {
  if (!line || !line.trim()) return false

  const trimmed = line.trim()

  const blockContext = detectBlockContext(stablePart)
  if (blockContext.inCodeBlock) return true

  // 代码块围栏行（``` 或 ~~~）应该作为块级元素处理
  if (/^[ \t]*(```+|~~~+)/.test(line)) return true

  // 使用 u 标志正确处理 Unicode 字符（emoji、中文等）
  // 简化正则：只要以 1-6 个 # 开头就视为块级元素
  if (/^#{1,6}/u.test(trimmed)) return true

  // 中文风格标题：如 一、XXX 或 二、XXX（支持一到十及数字序号）
  if (/^(一|二|三|四|五|六|七|八|九|十|[0-9]+)[、\.]/u.test(trimmed)) return true

  // 无序列表：标准格式（- 后有空格）或中文风格格式（-后直接跟文字）
  if (/^\s*[-*+]\s+.+/.test(line)) return true
  if (/^\s*[-*+][^\s-].+/.test(line)) return true

  // 有序列表：标准格式
  if (/^\s*\d+[.)]\s+.+/.test(line)) return true

  if (/^>+\s+.+/.test(trimmed)) return true

  if (/^(-{3,}|_{3,}|\*{3,})\s*$/.test(trimmed)) return true

  if (blockContext.inList && /^\s{2,}\S/.test(line)) return true

  // 表格行检测：必须是明确的表格结构
  // 1. 表格分隔行（支持任意列数）
  // 例如：|------|------|----------| 或 |:-:|:---|---:| 或 ------|------|
  if (/^(\s*\|?\s*[:-]+\s*\|)+\s*$/.test(trimmed) || /^(\s*[:-]+\s*\|)+\s*[:-]+\s*$/.test(trimmed)) return true

  // 2. 表格内容行：至少2个管道符（不要求必须以|开头和结尾，流式输出常见）
  const pipeCount = (trimmed.match(/\|/g) || []).length
  if (pipeCount >= 2 && /\|.*\|/.test(trimmed)) return true

  // 3. 表格头行：包含管道符且看起来像表格头
  if (pipeCount >= 2 && /^\|/.test(trimmed)) return true

  // 普通文本行：如果包含完整的 Markdown 格式（如粗体），也视为完整行
  // 使用 [\s\S]+ 替代 [^*]+ 以支持粗体内容中包含单个 * 的情况
  // 使用 u 标志支持 Unicode 字符
  const hasInlineFormatting = /\*\*[\s\S]+?\*\*/u.test(trimmed) ||
                              /_[^_]+_/u.test(trimmed) ||
                              /`[^`]+`/u.test(trimmed) ||
                              /\[[^\]]+\]\([^)]+\)/u.test(trimmed)
  if (hasInlineFormatting) return true

  return false
}

function renderMarkdown(markdown, streaming = false) {
  if (!markdown || markdown.trim() === '') {
    return ''
  }

  try {
 // 1. 预处理 LaTeX 公式
    const { content: processedContent, formulas } = preprocessLatex(markdown)

    let finalHtml

    if (streaming) {
      // ── 流式渲染：分离稳定行与不稳定行 ──

      // 保护代码块内容，避免预处理正则破坏代码块内的 #{keyword}、-X:、一、等模式
      const { content: streamProtected, blocks: streamBlocks } = protectCodeBlocks(processedContent)
      
      // 0. 先修复标题前有非标题内容的情况（流式输出常见）
      // 例如：Python与Java核心区别##一、语言特性 → Python与Java核心区别\n##一、语言特性
      // 注意：不使用 (?<!^|\n) 因为 JS 中 ^ 在 lookbehind 中不被识别为锚点
      let normalizedContent = streamProtected.replace(/([^\n])(#{1,6})/g, '$1\n$2')
      
      // 0.5. 修复中文式标题（#后无空格），转换为标准格式
      normalizedContent = normalizedContent.replace(/^(#{1,6})([^\s#])/gm, '$1 $2')

      // 0.6. 修复非标准列表格式：-X: → - X: （在 - 后插入空格）
      // 例如：-Python: → - Python:
      // 修复：保留冒号后的内容，简化正则
      normalizedContent = normalizedContent.replace(/^(\s*)-([^\s\n-])/gm, '$1- $2')

      // 0.7. 修复中文序号标题：将 "一、XXX" 转换为 "## 一、XXX"
      // 例如：二、关键差异要点 → ## 二、关键差异要点
      normalizedContent = normalizedContent.replace(/^(一|二|三|四|五|六|七|八|九|十)[、.].+$/gm, '## $&')
      normalizedContent = normalizedContent.replace(/^([0-9]+)[、.].+$/gm, '## $&')

      // 还原代码块内容
      normalizedContent = restoreCodeBlocks(normalizedContent, streamBlocks)
      
      // 稳定行（已有完整换行结尾）→ 全量 markdown 解析
      // 不稳定行（最后一行，可能不完整）→ 仅内联解析，避免产生块级结构
      const lastNewlineIndex = normalizedContent.lastIndexOf('\n')
      let stablePart = ''
      let unstablePart = ''

      if (lastNewlineIndex === -1) {
        // 整段内容都在一行上，但如果这是一个完整的块级行，按块级处理
        if (isCompleteBlockLine(normalizedContent, '')) {
          stablePart = normalizedContent + '\n'
          unstablePart = ''
        } else {
          // 否则按内联处理
          unstablePart = normalizedContent
        }
      } else {
        stablePart = normalizedContent.substring(0, lastNewlineIndex + 1)
        unstablePart = normalizedContent.substring(lastNewlineIndex + 1)
      }

      // 将不稳定部分中的完整行移到稳定部分，仅保留最后一个不完整行
      // 这样可以确保表格、列表等多行块级结构能被正确解析
      if (unstablePart) {
        const unstableLines = unstablePart.split('\n')
        if (unstableLines.length > 1) {
          const completeLines = unstableLines.slice(0, -1).join('\n') + '\n'
          const lastLine = unstableLines[unstableLines.length - 1]
          stablePart += completeLines
          unstablePart = lastLine
        } else if (isCompleteBlockLine(unstablePart, stablePart)) {
          // 检查是否是标题行，如果是标题，保留在 unstablePart 中以便与后续内容一起渲染
          // 这样可以避免标题和后续内容之间出现额外的间距
          const trimmedUnstable = unstablePart.trim()
          const isHeading = /^#{1,6}/u.test(trimmedUnstable) ||
                           /^(一|二|三|四|五|六|七|八|九|十|[0-9]+)[、\.]/u.test(trimmedUnstable)

          if (!isHeading) {
            stablePart += unstablePart + '\n'
            unstablePart = ''
          }
        }
      }

      // 修复稳定部分后，重新检查是否有完整的块级行应该被移到稳定部分
      // 这对于流式输出中刚刚完成的块级元素很重要
      let stableHtml = ''
      if (stablePart && stablePart.trim()) {
        const fixedStable = applyAllMarkdownFixes(stablePart)
        try {
          const parsed = marked.parse(fixedStable, { async: false, renderer: markdownRenderer })
          stableHtml = typeof parsed === 'string' ? parsed : String(parsed)
        } catch (e) {
          stableHtml = escapeHtml(stablePart).replace(/\n/g, '<br>')
        }
      }

      // 渲染不稳定部分：统一使用 parse 并包裹在 p 标签中，保持与稳定部分一致的样式
      let unstableHtml = ''
      if (unstablePart && unstablePart.trim()) {
        const isBlockLine = isCompleteBlockLine(unstablePart, stablePart)

        if (isBlockLine) {
          // 块级行：全量解析（表格、标题等）
          const fixedUnstable = applyAllMarkdownFixes(unstablePart)
          try {
            const parsed = marked.parse(fixedUnstable, { async: false, renderer: markdownRenderer })
            unstableHtml = typeof parsed === 'string' ? parsed : String(parsed)
          } catch (e) {
            unstableHtml = escapeHtml(unstablePart).replace(/\n/g, '<br>')
          }
        } else {
          // 内联行：使用 parseInline 但包裹在 p 标签中，确保与稳定部分的段落样式一致
          const inlineFixed = closeInlineFormatting(unstablePart)
          try {
            const parsed = marked.parseInline(inlineFixed, { async: false, renderer: markdownRenderer })
            const inlineHtml = typeof parsed === 'string' ? parsed : String(parsed)
            // 包裹在 p 标签中，保持与 marked.parse() 生成的段落一致的样式
            // 添加 streaming-paragraph 类以便在流式渲染时统一间距
            unstableHtml = `<p class="streaming-paragraph">${inlineHtml}</p>`
          } catch (e) {
            unstableHtml = `<p class="streaming-paragraph">${escapeHtml(unstablePart)}</p>`
          }
        }
      }

      finalHtml = stableHtml + unstableHtml
    } else {
 // ── 非流式渲染：修复后全量解析 ──
      const preprocessed = preprocessIncompleteMarkdown(processedContent, false)
      const html = marked.parse(preprocessed, { async: false, renderer: markdownRenderer })
      finalHtml = typeof html === 'string' ? html : String(html)
    }

 // 4. 后处理 LaTeX 公式
    finalHtml = postprocessLatex(finalHtml, formulas)

 // 5. 清理 HTML
    return DOMPurify.sanitize(finalHtml, {
      ADD_TAGS: ['pre', 'code', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'input', 'button', 'span', 'div', 'math', 'annotation'],
      ADD_ATTR: ['class', 'language', 'data-language', 'type', 'checked', 'disabled', 'aria-label', 'style', 'aria-hidden']
    })
  } catch (error) {
    logger.warn('[MarkdownRenderer] parse error:', error)
    return markdown
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\n/g, '<br>')
  }
}

const renderedHtml = computed(() => renderMarkdown(props.markdown, props.isStreaming))
</script>

<style lang="scss">
.markdown-renderer {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  font-size: 15px;
  line-height: 1.6;
  color: #1f2937;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  word-wrap: break-word;
  overflow-wrap: break-word;

 // ── KaTeX 公式 ──
  .katex {
    font-size: 1.1em;
  }

  .katex-display {
    margin: 12px 0;
    overflow-x: auto;
    overflow-y: hidden;
    padding: 8px 0;
  }

  .latex-error {
    color: #ef4444;
    background-color: #fef2f2;
    padding: 4px 8px;
    border-radius: 6px;
    font-family: 'SF Mono', Monaco, Consolas, monospace;
    font-size: 13px;
    border: 1px solid #fecaca;
  }

 // ── 标题 ──
  h1, h2, h3, h4, h5, h6 {
    font-weight: 700;
    line-height: 1.35;
    color: #111827;

    &:first-child {
      margin-top: 0;
    }
  }

  h1 {
    font-size: 1.85em;
    line-height: 1.3;
    margin-top: 0.8em;
    margin-bottom: 0.5em;
    font-weight: 700;
    letter-spacing: -0.025em;
    padding-bottom: 12px;
    background: linear-gradient(135deg, #1e40af 0%, #7c3aed 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;

    // 修复：确保 h1 内的行内代码、链接等元素可见
    code:not(pre code) {
      -webkit-text-fill-color: #dc2626;
      background: linear-gradient(135deg, #fef2f2 0%, #fff1f2 100%);
    }

    a {
      -webkit-text-fill-color: #2563eb;
      background: linear-gradient(transparent 70%, rgba(59, 130, 246, 0.15) 70%);
    }

    strong, b {
      -webkit-text-fill-color: #111827;
    }

    em, i {
      -webkit-text-fill-color: #374151;
    }
  }

  h2 {
    font-size: 1.5em;
    line-height: 1.35;
    margin-top: 0.7em;
    margin-bottom: 0.45em;
    font-weight: 650;
    padding-bottom: 14px;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      bottom: 4px;
      left: 0;
      width: 100%;
      height: 3px;
      background: linear-gradient(90deg, #3b82f6 0%, #8b5cf6 50%, #ec4899 100%);
      border-radius: 2px;
      opacity: 0.7;
    }
  }

  h3 {
    font-size: 1.3em;
    line-height: 1.4;
    margin-top: 0.6em;
    margin-bottom: 0.4em;
    font-weight: 600;
    color: #1e40af;
  }

  h4 {
    font-size: 1.15em;
    line-height: 1.45;
    margin-top: 0.55em;
    margin-bottom: 0.35em;
    font-weight: 600;
    color: #374151;
  }

  h5, h6 {
    font-size: 1em;
    line-height: 1.5;
    margin-top: 0.5em;
    margin-bottom: 0.3em;
    font-weight: 600;
    color: #6b7280;
  }

  // 流式渲染时的段落样式：确保与标题之间的间距一致
  // 当段落紧跟在标题后面时，移除额外的上边距
  h1 + p,
  h2 + p,
  h3 + p,
  h4 + p,
  h5 + p,
  h6 + p,
  h1 + .streaming-paragraph,
  h2 + .streaming-paragraph,
  h3 + .streaming-paragraph,
  h4 + .streaming-paragraph,
  h5 + .streaming-paragraph,
  h6 + .streaming-paragraph {
    margin-top: 0;
  }

 // ── 段落 ──
  p {
    margin-top: 0;
    margin-bottom: 0.6em;
    text-align: left;

    &:last-child {
      margin-bottom: 0;
    }
  }

 // ── 列表 ──
  ul, ol {
    margin-top: 0.3em;
    margin-bottom: 0.6em;
    padding-left: 1.5em;
  }

  li {
    margin-bottom: 0.3em;
    line-height: 1.6;

    > p {
      margin-bottom: 0.4em;
    }

    ul, ol {
      margin-top: 0.2em;
      margin-bottom: 0.2em;

      li {
        margin-bottom: 0.2em;
        font-size: 0.98em;
        line-height: 1.55;
      }
    }
  }

  ul {
    list-style-type: disc;

    & > li::marker {
      color: #60a5fa;
      font-size: 0.85em;
    }

    ul {
      list-style-type: circle;

      ul {
        list-style-type: square;
      }
    }
  }

  ol {
    list-style-type: decimal;

    & > li::marker {
      color: #818cf8;
      font-weight: 500;
    }

    ol {
      list-style-type: lower-alpha;

      ol {
        list-style-type: lower-roman;
      }
    }
  }

  input[type="checkbox"] {
    width: 16px;
    height: 16px;
    margin-right: 8px;
    accent-color: #3b82f6;
    vertical-align: middle;
  }

 // ── 链接 ──
  a {
    color: #2563eb;
    text-decoration: none;
    background: linear-gradient(transparent 70%, rgba(59, 130, 246, 0.15) 70%);
    transition: all 0.2s ease;

    &:hover {
      color: #1d4ed8;
      background: linear-gradient(transparent 60%, rgba(59, 130, 246, 0.25) 60%);
    }
  }

 // ── 图片 ──
  img {
    max-width: 100%;
    height: auto;
    border-radius: 12px;
    margin: 12px 0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    transition: transform 0.2s ease;

    &:hover {
      transform: scale(1.01);
    }
  }

 // ── 水平分割线 ──
  hr {
    height: 2px;
    border: none;
    margin: 14px 0;
    background: linear-gradient(90deg, transparent, #cbd5e1, #a78bfa, #cbd5e1, transparent);
    border-radius: 1px;
  }

 // ── 引用块 ──
  blockquote {
    margin: 12px 0;
    padding: 12px 18px;
    border-left: 4px solid;
    border-image: linear-gradient(to bottom, #3b82f6, #8b5cf6) 1;
    background: linear-gradient(135deg, #eff6ff 0%, #f5f3ff 100%);
    color: #1e3a5f;
    border-radius: 0 12px 12px 0;
    position: relative;
    line-height: 1.6;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(135deg, rgba(59, 130, 246, 0.04), rgba(139, 92, 246, 0.04));
      border-radius: inherit;
      pointer-events: none;
    }

    p {
      margin: 0;
      margin-bottom: 0.4em;
      position: relative;
      color: inherit;
    }

    p:last-child {
      margin-bottom: 0;
    }

    p + p {
      margin-top: 8px;
    }
  }

 // ── 行内代码 ──
  code:not(pre code) {
    background: linear-gradient(135deg, #fef2f2 0%, #fff1f2 100%);
    color: #dc2626;
    padding: 0.15em 0.45em;
    border-radius: 6px;
    font-family: 'SF Mono', Monaco, Consolas, 'Liberation Mono', monospace;
    font-size: 0.88em;
    border: 1px solid #fecaca;
    font-weight: 500;
    white-space: nowrap;
  }

 // ── 代码块（无 header 的简单模式） ──
  pre {
    margin: 12px 0;
    padding: 16px 20px;
    background: #f6f8fa;
    border: 1px solid #e5e7eb;
    border-radius: 14px;
    overflow-x: auto;
    position: relative;
    line-height: 1.6;
    font-size: 13.5px;
    font-family: 'SF Mono', Monaco, Consolas, 'Liberation Mono', monospace;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);

    code {
      background-color: transparent;
      padding: 0;
      border: none;
      border-radius: 0;
      font-size: inherit;
      color: #24292f;
      white-space: pre;
      line-height: inherit;
    }
  }

 // ── 代码块（带 header） ──
  .code-block {
    margin: 12px 0;
    border-radius: 14px;
    overflow: hidden;
    background: #f6f8fa;
    border: 1px solid #e5e7eb;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
    transition: box-shadow 0.3s ease;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .code-block-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 16px;
      background: linear-gradient(180deg, #ffffff 0%, #f6f8fa 100%);
      border-bottom: 1px solid #e5e7eb;
    }

    .code-block-language {
      font-size: 12px;
      line-height: 1;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: #0969da;
      background: rgba(9, 105, 218, 0.08);
      padding: 4px 10px;
      border-radius: 6px;
    }

    .code-copy-button {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      appearance: none;
      border: 1px solid #d0d7de;
      background: rgba(255, 255, 255, 0.8);
      color: #656d76;
      border-radius: 8px;
      padding: 5px 12px;
      font-size: 12px;
      line-height: 1;
      cursor: pointer;
      transition: all 0.2s ease;

      .copy-icon {
        width: 14px;
        height: 14px;
        flex-shrink: 0;
      }

      &:hover {
        border-color: #0969da;
        color: #0969da;
        background: rgba(9, 105, 218, 0.08);
      }

      &.is-copied {
        color: #1a7f37;
        border-color: #2da44e;
        background: rgba(31, 136, 61, 0.08);
      }
    }

    pre {
      margin: 0;
      border: none;
      border-radius: 0;
      background-color: transparent;
      box-shadow: none;
      padding: 16px 20px;
    }
  }

 // ── 表格 ──
  table {
    width: 100%;
    margin: 12px 0;
    border-collapse: separate;
    border-spacing: 0;
    border-radius: 12px;
    overflow: hidden;
    border: 1px solid #e5e7eb;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
    font-size: 14px;

    th, td {
      padding: 12px 14px;
      border-bottom: 1px solid #f3f4f6;
      border-right: 1px solid #f3f4f6;
      text-align: left;

      &:last-child {
        border-right: none;
      }
    }

    thead {
      th {
        background: linear-gradient(180deg, #f8fafc, #f1f5f9);
        font-weight: 600;
        color: #1e293b;
        border-bottom: 2px solid #e2e8f0;
        white-space: nowrap;
      }
    }

    tbody {
      tr {
        transition: background-color 0.15s ease;

        &:nth-child(even) {
          background-color: #f9fafb;
        }

        &:hover {
          background-color: #eff6ff;
        }

        &:not(:last-child) td {
          border-bottom: 1px solid #f3f4f6;
        }

        td {
          color: #374151;
          line-height: 1.5;

          code {
            background: linear-gradient(135deg, #fef2f2, #fff1f2);
            padding: 0.15em 0.4em;
            border-radius: 5px;
            font-size: 0.92em;
            border: 1px solid #fecaca;
          }
        }
      }
    }
  }

 // ── 文本格式 ──
  strong, b {
    font-weight: 700;
    color: #111827;
  }

  em, i {
    font-style: italic;
    color: #374151;
  }

  del {
    text-decoration: line-through;
    color: #9ca3af;
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
    color: #374151;
    background-color: #f9fafb;
    padding: 16px;
    border-radius: 10px;
    border: 1px solid #e5e7eb;
  }
}
</style>