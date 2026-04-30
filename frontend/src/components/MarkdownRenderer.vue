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
  for (let i = 1; i < lines.length; i++) {
    if (/^\s*\|?[\s:-]+\|[\s|:-]+\|?\s*$/.test(lines[i])) {
      return true
    }
  }
  return false
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
  if (!content) return { inCodeBlock: false, inBlockquote: false, inList: false, listDepth: 0 }

  const lines = content.split('\n')
  let inCodeBlock = false
  let inBlockquote = false
  let inList = false
  let listDepth = 0

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

    // 缩进内容（列表项延续），保持当前状态
    if (/^\s{2,}\S/.test(line)) {
      continue
    }

    // 非缩进非特殊行：重置引用；标题/分割线则同时重置列表
    inBlockquote = false
    if (/^(#{1,6}\s|[-*_]{3,}\s*$)/.test(line)) {
      inList = false
      listDepth = 0
    }
  }

  return { inCodeBlock, inBlockquote, inList, listDepth }
}

function sanitizeUnstableLine(line, blockContext) {
  if (!line) return line

  const ctx = blockContext || { inCodeBlock: false, inBlockquote: false, inList: false, listDepth: 0 }
  if (ctx.inCodeBlock) return line

  const trimmed = line.trim()

  // 标题：流式过程中几乎总是不完整的，插入零宽空格阻止 marked 解析为标题
  if (/^#{1,6}\s/.test(line)) {
    return line.replace(/^(#{1,6})(\s)/, '$1​$2')
  }

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

  // 表格管道：转义
  if (/^(\s*)\|/.test(line)) {
    return line.replace(/^(\s*)(\|)/, '$1\\$2')
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
  // 1. 修复不完整的代码块（保留缩进，避免破坏列表/引用嵌套）
  processed = closeOpenCodeFence(processed)

  // 2. 修复可能被截断的列表项（支持 - * + 和 1. 1) 样式）
  processed = processed.replace(/^(\s*[-*+]\s*)$/gm, '$1 ')
  processed = processed.replace(/^(\s*\d+[.)]\s*)$/gm, '$1 ')

  // 3. 修复可能被截断的标题
  processed = processed.replace(/^(#{1,6})$/gm, '$1 ')

  // 4. 修复不完整的粗体（双星号 **bold** 和双下划线 __bold__）
  const boldCount = (processed.match(/\*\*/g) || []).length
  if (boldCount % 2 !== 0) {
    processed += '**'
  }
  const underlineBoldCount = (processed.match(/__/g) || []).length
  if (underlineBoldCount % 2 !== 0) {
    processed += '__'
  }

  // 5. 修复不完整的斜体（单星号 *italic*，排除列表标记符号）
  const singleAsteriskCount = countNonListAsterisks(processed)
  if (singleAsteriskCount % 2 !== 0) {
    processed += '*'
  }

  // 6. 修复不完整的斜体（单下划线 _italic_，仅匹配单词边界的下划线）
  const singleUnderscoreCount = countNonWordUnderscores(processed)
  if (singleUnderscoreCount % 2 !== 0) {
    processed += '_'
  }

  // 7. 修复不完整的行内代码
  const backtickCount = (processed.match(/`/g) || []).length
  if (backtickCount % 2 !== 0) {
    processed += '`'
  }

  // 8. 修复不完整的链接 [text](url
  if (/\[[^\]]+\]\([^)]*$/.test(processed)) {
    processed += ')'
  }

  // 9. 修复不完整的表格行
  if (hasTableStructure(processed)) {
    const lines = processed.split('\n')
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
          processed = lines.join('\n')
        }
      }
    }
  }

  // 10. 修复不完整的引用块
  processed = processed.replace(/^(\s*>\s*)$/gm, '$1 ')

  // 11. 修复可能被截断的 HTML 标签（仅白名单标签，避免误匹配比较运算符）
  processed = closeOpenHtmlTag(processed)

  return processed
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

// 关闭未闭合的内联格式（bold、italic、inline code、links）
// 仅处理内联元素，不处理块级结构 — 配合 marked.parseInline() 使用
function closeInlineFormatting(text) {
  if (!text) return text
  let result = text

  // 1. 闭合粗体（**）
  const doubleAsteriskCount = (result.match(/\*\*/g) || []).length
  if (doubleAsteriskCount % 2 !== 0) result += '**'

  // 2. 闭合粗体（__）
  const doubleUnderscoreCount = (result.match(/__/g) || []).length
  if (doubleUnderscoreCount % 2 !== 0) result += '__'

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

  return result
}

function isCompleteBlockLine(line, stablePart) {
  if (!line || !line.trim()) return false

  const trimmed = line.trim()

  const blockContext = detectBlockContext(stablePart)
  if (blockContext.inCodeBlock) return true

  if (/^#{1,6}\s+.+/.test(trimmed)) return true

  if (/^\s*[-*+]\s+.+/.test(line)) return true

  if (/^\s*\d+[.)]\s+.+/.test(line)) return true

  if (/^>+\s+.+/.test(trimmed)) return true

  if (/^(-{3,}|_{3,}|\*{3,})\s*$/.test(trimmed)) return true

  if (/^\|?[\s:-]+\|[\s|:-]+\|?\s*$/.test(trimmed) && /[-:]/.test(trimmed)) return true

  if (trimmed.includes('|')) {
    const pipeCount = (trimmed.match(/\|/g) || []).length
    if (hasTableStructure(stablePart) && pipeCount >= 1) return true
    if (/^\|/.test(trimmed) && pipeCount >= 2) return true
    if (pipeCount >= 3) return true
  }

  if (blockContext.inList && /^\s{2,}\S/.test(line)) return true

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
      // 稳定行（已有完整换行结尾）→ 全量 markdown 解析
      // 不稳定行（最后一行，可能不完整）→ 仅内联解析，避免产生块级结构
      const lastNewlineIndex = processedContent.lastIndexOf('\n')
      let stablePart = ''
      let unstablePart = ''

      if (lastNewlineIndex === -1) {
        // 整段内容都在一行上，全部按内联处理
        unstablePart = processedContent
      } else {
        stablePart = processedContent.substring(0, lastNewlineIndex + 1)
        unstablePart = processedContent.substring(lastNewlineIndex + 1)
      }

      if (unstablePart && isCompleteBlockLine(unstablePart, stablePart)) {
        stablePart += unstablePart + '\n'
        unstablePart = ''
      }

      // 渲染稳定部分：修复块级结构后全量解析
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

      // 渲染不稳定部分：仅内联解析（marked.parseInline 不会产生块级元素）
      let unstableHtml = ''
      if (unstablePart && unstablePart.trim()) {
        const inlineFixed = closeInlineFormatting(unstablePart)
        try {
          const parsed = marked.parseInline(inlineFixed, { async: false, renderer: markdownRenderer })
          unstableHtml = typeof parsed === 'string' ? parsed : String(parsed)
        } catch (e) {
          unstableHtml = escapeHtml(unstablePart)
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
    console.warn('[MarkdownRenderer] parse error:', error)
    return markdown
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\n/g, '<br>')
  }
}

const renderedHtml = computed(() => renderMarkdown(props.markdown, props.isStreaming))

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