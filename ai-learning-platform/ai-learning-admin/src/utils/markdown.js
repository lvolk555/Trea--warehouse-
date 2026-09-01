// 轻量 Markdown → HTML 转换工具（零依赖）。
// 用于：导入 .md 文章时渲染为富文本、AI 生成的 Markdown 教程渲染为正文。
// 支持：标题、粗体、斜体、删除线、行内/块级代码、有序/无序列表、引用、分割线、链接、图片。

function escapeHtml(s) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

// 行内元素解析（入参已经过 HTML 转义）
function inline(s) {
  return String(s)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, '<img src="$2" alt="$1">')
    .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_]+)__/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/_([^_]+)_/g, '<em>$1</em>')
    .replace(/~~([^~]+)~~/g, '<del>$1</del>')
}

const BLANK = /^\s*$/
const HEADING = /^(#{1,6})\s+(.*)$/
const FENCE = /^\s*```/
const QUOTE = /^\s*>\s?/
const UL = /^\s*[-*+]\s+/
const OL = /^\s*\d+\.\s+/
const HR = /^\s*(---+|\*\*\*+|___+)\s*$/

export function mdToHtml(md) {
  if (!md) return ''
  const lines = escapeHtml(md).replace(/\r\n/g, '\n').split('\n')
  const out = []
  let i = 0
  while (i < lines.length) {
    const line = lines[i]

    // 代码块
    if (FENCE.test(line)) {
      const lang = line.replace(FENCE, '').trim()
      const code = []
      i++
      while (i < lines.length && !FENCE.test(lines[i])) {
        code.push(lines[i])
        i++
      }
      i++ // 跳过结束标记
      out.push(
        '<pre><code' + (lang ? ` class="language-${lang}"` : '') + '>' +
        code.join('\n') + '</code></pre>'
      )
      continue
    }

    // 标题
    const h = line.match(HEADING)
    if (h) {
      const level = h[1].length
      out.push(`<h${level}>${inline(h[2])}</h${level}>`)
      i++
      continue
    }

    // 分割线
    if (HR.test(line)) {
      out.push('<hr>')
      i++
      continue
    }

    // 引用
    if (QUOTE.test(line)) {
      const buf = []
      while (i < lines.length && QUOTE.test(lines[i])) {
        buf.push(lines[i].replace(QUOTE, ''))
        i++
      }
      out.push('<blockquote>' + buf.map(inline).join('<br>') + '</blockquote>')
      continue
    }

    // 无序列表
    if (UL.test(line)) {
      const items = []
      while (i < lines.length && UL.test(lines[i])) {
        items.push(inline(lines[i].replace(UL, '')))
        i++
      }
      out.push('<ul>' + items.map((t) => `<li>${t}</li>`).join('') + '</ul>')
      continue
    }

    // 有序列表
    if (OL.test(line)) {
      const items = []
      while (i < lines.length && OL.test(lines[i])) {
        items.push(inline(lines[i].replace(OL, '')))
        i++
      }
      out.push('<ol>' + items.map((t) => `<li>${t}</li>`).join('') + '</ol>')
      continue
    }

    // 空行
    if (BLANK.test(line)) {
      i++
      continue
    }

    // 段落：收集连续普通行
    const buf = [line]
    i++
    while (i < lines.length && !BLANK.test(lines[i]) && !HEADING.test(lines[i]) &&
      !FENCE.test(lines[i]) && !QUOTE.test(lines[i]) && !UL.test(lines[i]) &&
      !OL.test(lines[i]) && !HR.test(lines[i])) {
      buf.push(lines[i])
      i++
    }
    out.push('<p>' + buf.map(inline).join('<br>') + '</p>')
  }
  return out.join('\n')
}