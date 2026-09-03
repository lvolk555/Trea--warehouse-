// Markdown → HTML 转换工具（基于 marked + DOMPurify）。
// 用于：AI 答疑流式回复的实时 Markdown 渲染。
// 支持 GFM：标题、粗体、斜体、删除线、行内/块级代码、列表、引用、表格、链接、图片等。
// 渲染结果经 DOMPurify 清洗，防止 AI 输出中夹带的脚本执行（XSS）。
// 对流式增量内容天然容错：未闭合的代码块/表格会随内容增长自动修正。

import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({
  breaks: true,      // 单个换行符转为 <br>（贴近聊天习惯）
  gfm: true          // GitHub 风格：表格、任务列表、删除线等
})

const PURIFY_CONFIG = {
  ALLOWED_TAGS: [
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'p', 'br', 'hr', 'blockquote',
    'ul', 'ol', 'li', 'input',
    'strong', 'em', 'del', 's', 'u', 'mark', 'sub', 'sup',
    'a', 'img', 'figure', 'figcaption',
    'code', 'pre', 'span', 'div',
    'table', 'thead', 'tbody', 'tr', 'th', 'td'
  ],
  ALLOWED_ATTR: [
    'href', 'title', 'target', 'rel',
    'src', 'alt', 'width', 'height',
    'class', 'type', 'checked', 'disabled'
  ],
  ALLOW_DATA_ATTR: false
}

export function mdToHtml(md) {
  if (!md) return ''
  const raw = typeof md === 'string' ? md : String(md)
  const html = marked.parse(raw)
  return DOMPurify.sanitize(html, PURIFY_CONFIG)
}
