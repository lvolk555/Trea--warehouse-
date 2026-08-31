<script setup>
import { ref, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { notices } from '../../api/points'

const message = useMessage()
const loading = ref(false)
const list = ref([])

const typeText = { 1: '系统通知', 2: '活动公告', 3: '课程上新' }

async function load() {
  loading.value = true
  try {
    list.value = await notices()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <n-h2 style="margin-bottom: 4px">平台公告</n-h2>
    <n-text depth="3">系统通知、活动公告与课程上新</n-text>
    <n-spin :show="loading">
      <n-card v-for="n in list" :key="n.id" style="margin-top: 16px">
        <div class="head">
          <n-tag v-if="n.top === 1" type="error" size="small">置顶</n-tag>
          <n-tag :type="n.type === 1 ? 'info' : n.type === 2 ? 'warning' : 'success'" size="small">
            {{ typeText[n.type] || '公告' }}
          </n-tag>
          <span class="title">{{ n.title }}</span>
          <span class="muted">{{ n.createTime }}</span>
        </div>
        <p class="content">{{ n.content }}</p>
      </n-card>
      <n-empty v-if="list.length === 0" description="暂无公告" style="margin: 40px 0" />
    </n-spin>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title {
  font-weight: 600;
  font-size: 15px;
  flex: 1;
}
.content {
  margin: 10px 0 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.7;
}
.muted {
  color: #9ca3af;
  font-size: 12px;
}
</style>
