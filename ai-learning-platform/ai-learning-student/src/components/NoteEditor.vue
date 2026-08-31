<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  initialContent: { type: String, default: '' },
  saving: { type: Boolean, default: false }
})
const emit = defineEmits(['update:show', 'save'])

const editorRef = ref(null)

// 悬浮面板位置（可拖拽）
const panelStyle = ref({ left: 120, top: 100 })
let positioned = false
let dragging = false
let startX = 0
let startY = 0
let startLeft = 0
let startTop = 0

watch(
  () => props.show,
  (val) => {
    if (!val) return
    // 首次打开时定位到视口右上角
    if (!positioned) {
      const margin = 12
      const width = Math.min(380, window.innerWidth - margin * 2)
      panelStyle.value = {
        left: Math.max(margin, window.innerWidth - width - margin),
        top: 80
      }
      positioned = true
    }
    // 打开时回填已有笔记内容
    if (editorRef.value) {
      editorRef.value.innerHTML = props.initialContent || ''
    }
  }
)

function exec(cmd, val) {
  document.execCommand(cmd, false, val || null)
  editorRef.value?.focus()
}

function getHtml() {
  const html = (editorRef.value?.innerHTML || '').trim()
  // 空编辑器常见占位内容视为空
  if (!html || html === '<br>' || html === '<div><br></div>' || html === '<p><br></p>') {
    return ''
  }
  return html
}

function save() {
  emit('save', getHtml())
}

function close() {
  emit('update:show', false)
}

function onDragStart(e) {
  dragging = true
  startX = e.clientX
  startY = e.clientY
  startLeft = panelStyle.value.left
  startTop = panelStyle.value.top
  document.addEventListener('pointermove', onDrag)
  document.addEventListener('pointerup', onDragEnd)
}

function onDrag(e) {
  if (!dragging) return
  panelStyle.value.left = Math.max(0, startLeft + (e.clientX - startX))
  panelStyle.value.top = Math.max(0, startTop + (e.clientY - startY))
}

function onDragEnd() {
  dragging = false
  document.removeEventListener('pointermove', onDrag)
  document.removeEventListener('pointerup', onDragEnd)
}

onBeforeUnmount(() => {
  document.removeEventListener('pointermove', onDrag)
  document.removeEventListener('pointerup', onDragEnd)
})
</script>

<template>
  <div v-if="show" class="note-editor" :style="{ left: panelStyle.left + 'px', top: panelStyle.top + 'px' }">
    <!-- 标题栏（拖拽把手） -->
    <div class="note-header" @pointerdown="onDragStart">
      <span class="note-title">记笔记</span>
      <button class="note-close" title="关闭" @click.stop="close">×</button>
    </div>

    <!-- 富文本工具栏 -->
    <div class="note-toolbar">
      <button class="tb-btn" title="加粗" @mousedown.prevent="exec('bold')"><b>B</b></button>
      <button class="tb-btn" title="斜体" @mousedown.prevent="exec('italic')"><i>I</i></button>
      <button class="tb-btn" title="下划线" @mousedown.prevent="exec('underline')"><u>U</u></button>
      <button class="tb-btn" title="删除线" @mousedown.prevent="exec('strikeThrough')"><s>S</s></button>
      <button class="tb-btn" title="标题" @mousedown.prevent="exec('formatBlock', 'H3')">H1</button>
      <button class="tb-btn" title="无序列表" @mousedown.prevent="exec('insertUnorderedList')">•≡</button>
      <button class="tb-btn" title="有序列表" @mousedown.prevent="exec('insertOrderedList')">1≡</button>
      <button class="tb-btn" title="清除格式" @mousedown.prevent="exec('removeFormat')">清除</button>
    </div>

    <!-- 编辑区 -->
    <div ref="editorRef" class="note-body" contenteditable="true" placeholder="在这里记录本节课的笔记…"></div>

    <!-- 底部操作 -->
    <div class="note-footer">
      <button class="note-btn" @click="close">取消</button>
      <button class="note-btn primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存笔记' }}</button>
    </div>
  </div>
</template>

<style scoped>
.note-editor {
  position: fixed;
  z-index: 9999;
  width: 380px;
  max-width: calc(100vw - 24px);
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.note-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  cursor: move;
  user-select: none;
  touch-action: none;
}
.note-title {
  font-size: 14px;
  font-weight: 600;
}
.note-close {
  border: none;
  background: transparent;
  color: #fff;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  padding: 0 4px;
}
.note-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
}
.tb-btn {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 6px;
  min-width: 30px;
  height: 28px;
  font-size: 13px;
  cursor: pointer;
  color: #374151;
}
.tb-btn:hover {
  background: #f5f6fa;
}
.note-body {
  min-height: 200px;
  max-height: 50vh;
  overflow-y: auto;
  padding: 12px 14px;
  outline: none;
  font-size: 14px;
  line-height: 1.7;
  color: #1f2937;
}
.note-body:empty::before {
  content: attr(placeholder);
  color: #c0c4cc;
}
.note-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 14px;
  border-top: 1px solid #f0f0f0;
}
.note-btn {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 6px;
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
  color: #374151;
}
.note-btn.primary {
  background: #6366f1;
  border-color: #6366f1;
  color: #fff;
}
.note-btn.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>