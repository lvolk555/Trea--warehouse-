<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { courseDetail, enrollCourse } from '../../api/course'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const loading = ref(false)
const course = ref(null)
const enrolling = ref(false)

async function loadDetail() {
  loading.value = true
  try {
    course.value = await courseDetail(route.params.courseId)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function handleEnroll() {
  if (course.value.priceType === 2) {
    message.info('该课程为积分兑换课程，请到积分中心兑换（阶段五上线）')
    return
  }
  enrolling.value = true
  try {
    await enrollCourse(course.value.id)
    message.success('选课成功，开始学习吧')
    await loadDetail()
  } catch (e) {
    message.error(e.message)
  } finally {
    enrolling.value = false
  }
}

function goStudy(video) {
  if (!course.value.enrolled) {
    message.warning('请先选课')
    return
  }
  router.push(`/study/${course.value.id}/${video.id}`)
}

onMounted(loadDetail)
</script>

<template>
  <n-spin :show="loading">
    <div v-if="course">
      <!-- 课程头部 -->
      <n-card>
        <div class="head">
          <img :src="course.cover || 'https://picsum.photos/seed/' + course.id + '/640/360'" class="cover" />
          <div class="info">
            <h1>{{ course.title }}</h1>
            <n-space align="center" style="margin: 8px 0">
              <n-tag type="info">{{ course.category }}</n-tag>
              <n-tag v-if="course.priceType === 1" type="success">免费</n-tag>
              <n-tag v-else type="warning">{{ course.pointsPrice }} 积分兑换</n-tag>
              <span class="muted">授课：{{ course.teacherName }}</span>
              <span class="muted">{{ course.videoCount }} 节视频</span>
            </n-space>
            <p class="desc">{{ course.description }}</p>
            <n-space>
              <n-button v-if="!course.enrolled" type="primary" size="large" :loading="enrolling" @click="handleEnroll">
                {{ course.priceType === 2 ? '积分兑换（阶段五）' : '免费选课' }}
              </n-button>
              <n-button v-else type="primary" size="large" @click="router.push('/my-courses')">
                已选课 · 完成度 {{ course.progress }}%
              </n-button>
            </n-space>
          </div>
        </div>
      </n-card>

      <!-- 章节目录 -->
      <n-card title="课程目录" style="margin-top: 16px">
        <n-collapse>
          <n-collapse-item v-for="chapter in course.chapters" :key="chapter.id" :title="chapter.title">
            <div v-for="video in chapter.videos" :key="video.id" class="video-row" @click="goStudy(video)">
              <span class="video-title">
                <span class="play-icon">▶</span>
                {{ video.title }}
              </span>
              <span class="muted">{{ Math.round((video.duration || 0) / 60) }} 分钟</span>
              <n-tag v-if="video.finished" size="small" type="success">已完成</n-tag>
            </div>
          </n-collapse-item>
        </n-collapse>
        <n-empty v-if="!course.chapters || course.chapters.length === 0" description="暂无章节" />
      </n-card>
    </div>
  </n-spin>
</template>

<style scoped>
.head {
  display: flex;
  gap: 24px;
}
.cover {
  width: 360px;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}
.info h1 {
  font-size: 22px;
}
.desc {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 16px;
}
.muted {
  color: #9ca3af;
  font-size: 13px;
}
.video-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 8px;
  border-radius: 6px;
  cursor: pointer;
}
.video-row:hover {
  background: #f5f6fa;
}
.video-title {
  flex: 1;
}
.play-icon {
  color: #6366f1;
  font-size: 12px;
  margin-right: 6px;
}
</style>
