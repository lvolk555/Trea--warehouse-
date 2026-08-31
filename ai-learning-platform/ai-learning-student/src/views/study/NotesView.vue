<script setup>
import { ref, onMounted } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { myNotes, saveNote, deleteNote } from '../../api/course'
import NoteEditor from '../../components/NoteEditor.vue'

const message = useMessage()
const dialog = useDialog()
const loading = ref(false)
const notes = ref([])

// 点击查看全文
const modalShow = ref(false)
const currentNote = ref(null)

// 编辑笔记（复用悬浮富文本编辑器）
const editShow = ref(false)
const editContent = ref('')
const editSaving = ref(false)
const editingNote = ref(null)

async function loadData() {
  loading.value = true
  try {
    notes.value = await myNotes()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

// 去除 HTML 标签，取出纯文本用于列表预览
function stripHtml(html) {
  const div = document.createElement('div')
  div.innerHTML = html || ''
  return (div.textContent || div.innerText || '').replace(/\s+/g, ' ').trim()
}

function openNote(n) {
  currentNote.value = n
  modalShow.value = true
}

function openEdit(n) {
  editingNote.value = n
  editContent.value = n.content
  editShow.value = true
}

async function handleEditSave(content) {
  editSaving.value = true
  try {
    await saveNote({ videoId: editingNote.value.videoId, content })
    message.success('笔记已更新')
    editShow.value = false
    loadData()
  } catch (e) {
    message.error(e.message)
  } finally {
    editSaving.value = false
  }
}

function handleDelete(n) {
  dialog.warning({
    title: '删除笔记',
    content: '确定删除这条笔记吗？删除后不可恢复。',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteNote(n.id)
        message.success('笔记已删除')
        loadData()
      } catch (e) {
        message.error(e.message)
      }
    }
  })
}

onMounted(loadData)
</script>

<template>
  <div>
    <n-h2 style="margin-bottom: 4px">学习笔记</n-h2>
    <n-text depth="3">在各门课程视频学习时随手记下的笔记，可查看、编辑与删除</n-text>

    <n-spin :show="loading">
      <n-list bordered hoverable style="margin-top: 16px">
        <n-list-item v-for="n in notes" :key="n.id">
          <n-thing>
            <template #header>
              <n-space align="center" size="small" style="flex-wrap: wrap">
                <n-tag type="info" size="small">{{ n.courseTitle || '未知课程' }}</n-tag>
                <n-text strong style="font-size: 14px">{{ n.videoTitle }}</n-text>
              </n-space>
            </template>
            <template #description>
              <n-text depth="3" style="font-size: 12px">
                {{ n.chapterTitle }} · {{ n.createTime }}
              </n-text>
            </template>
            <div class="note-preview" @click="openNote(n)">{{ stripHtml(n.content) }}</div>
            <n-space size="small" style="margin-top: 10px">
              <n-button size="tiny" tertiary type="primary" @click="openEdit(n)">编辑</n-button>
              <n-button size="tiny" tertiary type="error" @click="handleDelete(n)">删除</n-button>
            </n-space>
          </n-thing>
        </n-list-item>
      </n-list>
      <n-empty v-if="!loading && notes.length === 0" description="还没有笔记，去课程视频中点击「记笔记」吧" style="margin: 60px 0" />
    </n-spin>

    <!-- 笔记全文弹窗 -->
    <n-modal v-model:show="modalShow" preset="card" :title="currentNote?.videoTitle || '笔记详情'" style="max-width: 720px">
      <template v-if="currentNote">
        <n-space align="center" size="small" style="flex-wrap: wrap; margin-bottom: 12px">
          <n-tag type="info" size="small">{{ currentNote.courseTitle || '未知课程' }}</n-tag>
          <n-tag size="small" :bordered="false">{{ currentNote.chapterTitle }}</n-tag>
          <n-text depth="3" style="font-size: 12px">{{ currentNote.createTime }}</n-text>
        </n-space>
        <div class="note-content" v-html="currentNote.content"></div>
      </template>
    </n-modal>

    <!-- 悬浮可拖拽的编辑富文本编辑器 -->
    <NoteEditor v-model:show="editShow" :initial-content="editContent" :saving="editSaving" @save="handleEditSave" />
  </div>
</template>

<style scoped>
.note-preview {
  margin-top: 8px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
  cursor: pointer;
}
.note-preview:hover {
  color: #6366f1;
}
.note-content {
  color: #374151;
  font-size: 14px;
  line-height: 1.8;
  word-break: break-word;
  white-space: normal;
  max-height: 60vh;
  overflow-y: auto;
}
.note-content :deep(img) {
  max-width: 100%;
}
</style>