<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { courseDetail, reportProgress, resumePosition, getNote, saveNote } from '../../api/course'
import NoteEditor from '../../components/NoteEditor.vue'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const courseId = Number(route.params.courseId)
const videoId = ref(Number(route.params.videoId))

const course = ref(null)
const videoEl = ref(null)
const resuming = ref(false)
const reporting = ref(false)
let reportTimer = null

// 学习笔记
const noteShow = ref(false)
const noteContent = ref('')
const noteSaving = ref(false)

// 当前视频与章节信息
const currentVideo = computed(() => {
  if (!course.value) return null
  for (const chapter of course.value.chapters || []) {
    const video = (chapter.videos || []).find(v => v.id === videoId.value)
    if (video) return { ...video, chapterTitle: chapter.title }
  }
  return null
})

// 全部视频按顺序展平，用于上一节/下一节
const flatVideos = computed(() => {
  const list = []
  for (const chapter of course.value?.chapters || []) {
    for (const video of chapter.videos || []) {
      list.push(video)
    }
  }
  return list
})

const currentIndex = computed(() => flatVideos.value.findIndex(v => v.id === videoId.value))

// 当前小节是否为文章小节
const isArticle = computed(() => currentVideo.value?.sectionType === 2)
const articleFinishing = ref(false)

async function loadCourse() {
  course.value = await courseDetail(courseId)
}

// 切换小节
async function switchVideo(video) {
  flushProgress()
  noteShow.value = false
  videoId.value = video.id
  router.replace(`/study/${courseId}/${video.id}`)
  await nextTick()
  if (video.sectionType !== 2) {
    await initPlayer()
  }
}

// 初始化播放器：断点续播
async function initPlayer() {
  resuming.value = true
  try {
    const { position } = await resumePosition(videoId.value)
    const el = videoEl.value
    if (el && position > 0) {
      const applyPosition = () => {
        el.currentTime = position
        message.info(`已为你续播到 ${formatTime(position)}`)
      }
      if (el.readyState >= 1) {
        applyPosition()
      } else {
        el.addEventListener('loadedmetadata', applyPosition, { once: true })
      }
    }
  } catch (e) {
    // 续播失败不阻断播放
  } finally {
    resuming.value = false
  }
}

// 播放中每 10 秒上报一次进度
function onTimeUpdate() {
  if (!reportTimer) {
    reportTimer = setTimeout(() => {
      reportTimer = null
      flushProgress(false)
    }, 10000)
  }
}

// 播放结束 → 标记完课
function onEnded() {
  flushProgress(true)
  message.success('本节视频学习完成 +10 积分（积分模块阶段五接入）')
}

// 上报进度
async function flushProgress(finished = false) {
  const el = videoEl.value
  if (!el || reporting.value) return
  reporting.value = true
  try {
    await reportProgress({
      videoId: videoId.value,
      position: Math.floor(el.currentTime || 0),
      finished
    })
    if (finished) {
      await loadCourse()
    }
  } catch (e) {
    // 上报失败静默处理，不影响观看
  } finally {
    reporting.value = false
  }
}

function goPrev() {
  if (currentIndex.value > 0) switchVideo(flatVideos.value[currentIndex.value - 1])
}

function goNext() {
  if (currentIndex.value < flatVideos.value.length - 1) {
    switchVideo(flatVideos.value[currentIndex.value + 1])
  } else {
    message.info('已是最后一节')
  }
}

// 文章小节：标记学完
async function finishArticle() {
  if (currentVideo.value?.finished) return
  articleFinishing.value = true
  try {
    await reportProgress({ videoId: videoId.value, position: 0, finished: true })
    message.success('恭喜完成本节学习')
    await loadCourse()
  } catch (e) {
    message.error(e.message)
  } finally {
    articleFinishing.value = false
  }
}

function formatTime(seconds) {
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

// 打开记笔记：加载该视频已有笔记并弹出悬浮编辑器
async function openNote() {
  try {
    noteContent.value = await getNote(videoId.value)
  } catch (e) {
    noteContent.value = ''
  }
  noteShow.value = true
}

async function handleSaveNote(content) {
  noteSaving.value = true
  try {
    await saveNote({ videoId: videoId.value, content })
    message.success('笔记已保存')
    noteShow.value = false
  } catch (e) {
    message.error(e.message)
  } finally {
    noteSaving.value = false
  }
}

onMounted(async () => {
  await loadCourse()
  if (!isArticle.value) {
    await initPlayer()
  }
})

onBeforeUnmount(() => {
  if (reportTimer) clearTimeout(reportTimer)
  flushProgress(false)
})
</script>

<template>
  <div v-if="course" class="study-page">
    <n-grid :x-gap="16" cols="24" responsive="screen" item-responsive>
      <!-- 播放区 / 文章阅读区 -->
      <n-grid-item span="24 m:17">
        <n-card :bordered="false" class="player-card">
          <div class="breadcrumb">
            <n-breadcrumb>
              <n-breadcrumb-item @click="router.push(`/course/${courseId}`)">{{ course.title }}</n-breadcrumb-item>
              <n-breadcrumb-item>{{ currentVideo?.chapterTitle }}</n-breadcrumb-item>
            </n-breadcrumb>
          </div>
          <h2 class="video-title">
            <span v-if="isArticle" class="article-badge">📄 图文</span>
            {{ currentVideo?.title }}
          </h2>

          <!-- 文章小节：渲染教程正文 -->
          <template v-if="isArticle">
            <div class="article-content" v-html="currentVideo?.articleContent || '<p>暂无文章内容</p>'"></div>
            <n-space justify="space-between" style="margin-top: 16px">
              <n-button :disabled="currentIndex <= 0" @click="goPrev">上一节</n-button>
              <n-space>
                <n-button secondary @click="openNote">📝 记笔记</n-button>
                <n-button v-if="!currentVideo?.finished" type="primary" :loading="articleFinishing" @click="finishArticle">
                  ✓ 学完本节
                </n-button>
                <n-tag v-else type="success">已完成</n-tag>
              </n-space>
              <n-button type="primary" :disabled="currentIndex >= flatVideos.length - 1" @click="goNext">
                下一节
              </n-button>
            </n-space>
          </template>

          <!-- 视频小节 -->
          <template v-else>
            <video
              ref="videoEl"
              :src="currentVideo?.url"
              controls
              class="video-player"
              @timeupdate="onTimeUpdate"
              @ended="onEnded"
            >
              你的浏览器不支持视频播放
            </video>
            <n-space justify="space-between" style="margin-top: 12px">
              <n-button :disabled="currentIndex <= 0" @click="goPrev">上一节</n-button>
              <n-button secondary @click="openNote">📝 记笔记</n-button>
              <n-button type="primary" :disabled="currentIndex >= flatVideos.length - 1" @click="goNext">
                下一节
              </n-button>
            </n-space>
          </template>
        </n-card>
      </n-grid-item>

      <!-- 章节目录 -->
      <n-grid-item span="24 m:7">
        <n-card title="课程章节" :bordered="false">
          <div v-for="chapter in course.chapters" :key="chapter.id">
            <div class="chapter-title">{{ chapter.title }}</div>
            <div
              v-for="video in chapter.videos"
              :key="video.id"
              class="video-item"
              :class="{ active: video.id === videoId }"
              @click="switchVideo(video)"
            >
              <span class="item-title">
                <span v-if="video.sectionType === 2" class="item-icon">📄</span>
                <span v-else class="item-icon play">▶</span>
                {{ video.title }}
              </span>
              <n-tag v-if="video.finished" size="tiny" type="success">已完成</n-tag>
            </div>
          </div>
        </n-card>
      </n-grid-item>
    </n-grid>

    <!-- 悬浮可拖拽的记笔记富文本编辑器 -->
    <NoteEditor v-model:show="noteShow" :initial-content="noteContent" :saving="noteSaving" @save="handleSaveNote" />
  </div>
</template>

<style scoped>
.player-card {
  background: #fff;
}
.breadcrumb {
  margin-bottom: 8px;
}
.video-title {
  font-size: 18px;
  margin-bottom: 12px;
}
.video-player {
  width: 100%;
  max-height: 480px;
  background: #000;
  border-radius: 8px;
}
.chapter-title {
  font-weight: 600;
  font-size: 14px;
  margin: 12px 0 6px;
  color: #374151;
}
.video-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #4b5563;
}
.video-item:hover {
  background: #f5f6fa;
}
.video-item.active {
  background: #eef2ff;
  color: #6366f1;
  font-weight: 600;
}
.item-title {
  flex: 1;
  margin-right: 8px;
}
.item-icon {
  font-size: 11px;
  margin-right: 4px;
  opacity: 0.8;
}
.item-icon.play {
  color: #6366f1;
}
.article-badge {
  display: inline-block;
  font-size: 13px;
  background: #eef2ff;
  color: #6366f1;
  border-radius: 4px;
  padding: 2px 8px;
  margin-right: 8px;
  vertical-align: middle;
}
.article-content {
  line-height: 1.9;
  color: #374151;
  font-size: 15px;
  word-break: break-word;
}
.article-content :deep(h1) { font-size: 24px; margin: 18px 0 10px; }
.article-content :deep(h2) { font-size: 20px; margin: 16px 0 8px; }
.article-content :deep(h3) { font-size: 17px; margin: 14px 0 6px; }
.article-content :deep(pre) {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}
.article-content :deep(code) {
  background: #f6f8fa;
  padding: 2px 5px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 13px;
}
.article-content :deep(pre code) { background: transparent; padding: 0; }
.article-content :deep(blockquote) {
  border-left: 3px solid #d0d7de;
  margin: 12px 0;
  padding: 4px 14px;
  color: #57606a;
}
.article-content :deep(ul), .article-content :deep(ol) { padding-left: 24px; margin: 8px 0; }
.article-content :deep(img) { max-width: 100%; border-radius: 6px; }
.article-content :deep(a) { color: #6366f1; }
.article-content :deep(table) { border-collapse: collapse; margin: 12px 0; display: block; overflow-x: auto; max-width: 100%; }
.article-content :deep(th), .article-content :deep(td) { border: 1px solid #e5e7eb; padding: 8px 14px; text-align: left; }
.article-content :deep(th) { background: #f9fafb; font-weight: 600; }
</style>
