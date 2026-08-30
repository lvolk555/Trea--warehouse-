<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { courseDetail, reportProgress, resumePosition } from '../../api/course'

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

async function loadCourse() {
  course.value = await courseDetail(courseId)
}

// 切换视频
async function switchVideo(video) {
  flushProgress()
  videoId.value = video.id
  router.replace(`/study/${courseId}/${video.id}`)
  await nextTick()
  await initPlayer()
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
    message.info('已是最后一节视频')
  }
}

function formatTime(seconds) {
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

onMounted(async () => {
  await loadCourse()
  await initPlayer()
})

onBeforeUnmount(() => {
  if (reportTimer) clearTimeout(reportTimer)
  flushProgress(false)
})
</script>

<template>
  <div v-if="course" class="study-page">
    <n-grid :x-gap="16" cols="24" responsive="screen" item-responsive>
      <!-- 播放区 -->
      <n-grid-item span="24 m:17">
        <n-card :bordered="false" class="player-card">
          <div class="breadcrumb">
            <n-breadcrumb>
              <n-breadcrumb-item @click="router.push(`/course/${courseId}`)">{{ course.title }}</n-breadcrumb-item>
              <n-breadcrumb-item>{{ currentVideo?.chapterTitle }}</n-breadcrumb-item>
            </n-breadcrumb>
          </div>
          <h2 class="video-title">{{ currentVideo?.title }}</h2>
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
            <n-button type="primary" :disabled="currentIndex >= flatVideos.length - 1" @click="goNext">
              下一节
            </n-button>
          </n-space>
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
              <span class="item-title">{{ video.title }}</span>
              <n-tag v-if="video.finished" size="tiny" type="success">已完成</n-tag>
            </div>
          </div>
        </n-card>
      </n-grid-item>
    </n-grid>
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
</style>
