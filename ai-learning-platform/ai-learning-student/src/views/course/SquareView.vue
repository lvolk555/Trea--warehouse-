<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { courseSquare } from '../../api/course'

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const courses = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 8, keyword: '', category: null })

const categories = ['编程', '数学', '外语', '设计', '其他']

async function loadData() {
  loading.value = true
  try {
    const data = await courseSquare(query)
    courses.value = data.records
    total.value = Number(data.total)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  loadData()
}

function goDetail(course) {
  router.push(`/course/${course.id}`)
}

onMounted(loadData)
</script>

<template>
  <div>
    <!-- 搜索与分类筛选 -->
    <n-card style="margin-bottom: 16px">
      <n-space align="center">
        <n-input v-model:value="query.keyword" placeholder="搜索课程" clearable style="width: 260px" @keyup.enter="search" />
        <n-button type="primary" @click="search">搜索</n-button>
        <n-divider vertical />
        <n-radio-group v-model:value="query.category" @update:value="search">
          <n-radio-button :value="null">全部</n-radio-button>
          <n-radio-button v-for="c in categories" :key="c" :value="c">{{ c }}</n-radio-button>
        </n-radio-group>
      </n-space>
    </n-card>

    <!-- 课程卡片 -->
    <n-spin :show="loading">
      <n-grid :x-gap="16" :y-gap="16" cols="1 s:2 m:3 l:4" responsive="screen">
        <n-grid-item v-for="course in courses" :key="course.id">
          <n-card hoverable class="course-card" @click="goDetail(course)">
            <template #cover>
              <img :src="course.cover" class="cover" />
            </template>
            <div class="title">{{ course.title }}</div>
            <div class="meta">
              <n-tag size="small" type="info">{{ course.category }}</n-tag>
              <n-tag v-if="course.priceType === 1" size="small" type="success">免费</n-tag>
              <n-tag v-else size="small" type="warning">{{ course.pointsPrice }} 积分</n-tag>
            </div>
            <div class="footer">
              <span>{{ course.teacherName }}</span>
              <span>{{ course.videoCount }} 节视频</span>
            </div>
          </n-card>
        </n-grid-item>
      </n-grid>
      <n-empty v-if="!loading && courses.length === 0" description="暂无课程" style="margin: 60px 0" />
    </n-spin>

    <n-space justify="center" style="margin-top: 20px">
      <n-pagination v-model:page="query.page" :page-size="query.size" :item-count="total" @update:page="loadData" />
    </n-space>
  </div>
</template>

<style scoped>
.course-card {
  cursor: pointer;
}
.cover {
  width: 100%;
  height: 150px;
  object-fit: cover;
}
.title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}
.footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #9ca3af;
}
</style>
