<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { myCourses } from '../../api/course'

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const courses = ref([])

async function loadData() {
  loading.value = true
  try {
    courses.value = await myCourses()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

function continueLearning(course) {
  router.push(`/course/${course.id}`)
}

onMounted(loadData)
</script>

<template>
  <div>
    <n-spin :show="loading">
      <n-grid :x-gap="16" :y-gap="16" cols="1 s:2 m:3" responsive="screen">
        <n-grid-item v-for="course in courses" :key="course.id">
          <n-card hoverable class="course-card">
            <template #cover>
              <img :src="course.cover" class="cover" />
            </template>
            <div class="title">{{ course.title }}</div>
            <n-progress type="line" :percentage="Number(course.progress || 0)" :height="8" border-radius="4px" fill-border-radius="4px" />
            <div class="footer">
              <span class="muted">完成度 {{ course.progress }}%</span>
              <n-button size="small" type="primary" @click="continueLearning(course)">继续学习</n-button>
            </div>
          </n-card>
        </n-grid-item>
      </n-grid>
      <n-empty v-if="!loading && courses.length === 0" description="还没有选课，去课程广场看看吧" style="margin: 60px 0">
        <template #extra>
          <n-button @click="router.push('/square')">去课程广场</n-button>
        </template>
      </n-empty>
    </n-spin>
  </div>
</template>

<style scoped>
.course-card {
  cursor: default;
}
.cover {
  width: 100%;
  height: 150px;
  object-fit: cover;
}
.title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 10px;
}
.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}
.muted {
  color: #9ca3af;
  font-size: 13px;
}
</style>
