<script setup>
import { ref, onMounted, watch } from 'vue'

const props = defineProps({
  value: { type: String, default: '' }
})
const emit = defineEmits(['update:value'])

const editor = ref(null)

function focus() {
  editor.value && editor.value.focus()
}

function exec(cmd, value = null) {
  focus()
  document.execCommand(cmd, false, value)
  sync()
}

function execBlock(tag) {
  focus()
  document.execCommand('formatBlock', false, tag)
  sync()
}

function clearFormat() {
  focus()
  document.execCommand('removeFormat')
  sync()
}

function insertLink() {
  const url = window.prompt('请输入链接地址', 'https://')
  if (url) exec('createLink', url)
}

function insertImage() {
  const url = window.prompt('请输入图片地址', 'https://')
  if (url) exec('insertImage', url)
}

function insertInlineCode() {
  focus()
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return
  const text = sel.toString()
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  document.execCommand('insertHTML', false, text ? `<code>${text}</code>` : '<code>\u200b</code>')
  sync()
}

function sync() {
  if (editor.value) emit('update:value', editor.value.innerHTML)
}

function onInput() {
  sync()
}

watch(() => props.value, (val) => {
  if (editor.value && editor.value.innerHTML !== val) {
    editor.value.innerHTML = val || ''
  }
})

onMounted(() => {
  if (editor.value && props.value) editor.value.innerHTML = props.value
})

const tools = [
  { label: '加粗', act: () => exec('bold') },
  { label: '斜体', act: () => exec('italic') },
  { label: '下划线', act: () => exec('underline') },
  { label: '删除线', act: () => exec('strikeThrough') },
  { label: 'H1', act: () => execBlock('h1') },
  { label: 'H2', act: () => execBlock('h2') },
  { label: 'H3', act: () => execBlock('h3') },
  { label: '列表', act: () => exec('insertUnorderedList') },
  { label: '编号', act: () => exec('insertOrderedList') },
  { label: '引用', act: () => execBlock('blockquote') },
  { label: '代码块', act: () => execBlock('pre') },
  { label: '行内代码', act: insertInlineCode },
  { label: '链接', act: insertLink },
  { label: '图片', act: insertImage },
  { label: '清除格式', act: clearFormat }
]
</script>

<template>
  <div class="rte">
    <div class="rte-toolbar">
      <a-space wrap :size="4">
        <a-button v-for="t in tools" :key="t.label" size="small" @mousedown.prevent @click="t.act">
          {{ t.label }}
        </a-button>
      </a-space>
    </div>
    <div
      ref="editor"
      class="rte-body"
      contenteditable="true"
      @input="onInput"
      @blur="sync"
    ></div>
  </div>
</template>

<style scoped>
.rte {
  width: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
}
.rte-toolbar {
  padding: 6px 8px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}
.rte-body {
  min-height: 240px;
  max-height: 420px;
  overflow-y: auto;
  padding: 12px;
  outline: none;
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  background: #fff;
}
.rte-body :deep(h1) { font-size: 22px; margin: 8px 0; }
.rte-body :deep(h2) { font-size: 18px; margin: 8px 0; }
.rte-body :deep(h3) { font-size: 16px; margin: 6px 0; }
.rte-body :deep(pre) {
  background: #f6f8fa;
  padding: 10px;
  border-radius: 6px;
  overflow-x: auto;
}
.rte-body :deep(code) {
  background: #f6f8fa;
  padding: 1px 4px;
  border-radius: 4px;
  font-family: monospace;
}
.rte-body :deep(pre code) { background: transparent; padding: 0; }
.rte-body :deep(blockquote) {
  border-left: 3px solid #d0d7de;
  margin: 8px 0;
  padding: 4px 12px;
  color: #57606a;
}
.rte-body :deep(img) { max-width: 100%; }
</style>