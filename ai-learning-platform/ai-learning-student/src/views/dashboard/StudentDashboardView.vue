<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useMessage } from 'naive-ui'
import * as echarts from 'echarts'
import { studentStats } from '../../api/stats'

const message = useMessage()
const loading = ref(false)
const stats = ref(null)

const chartEl = ref(null)
let chart = null

function formatSeconds(sec) {
  const s = Number(sec || 0)
  if (s < 60) return `${s} 秒`
  if (s < 3600) return `${Math.floor(s / 60)} 分钟`
  return `${(s / 3600).toFixed(1)} 小时`
}

function renderChart(trend) {
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: trend.map(t => t.day), boundaryGap: false },
    yAxis: { type: 'value', name: '积分变动' },
    series: [{
      name: '积分',
      type: 'line',
      smooth: true,
      data: trend.map(t => Number(t.value)),
      areaStyle: { opacity: 0.15 },
      itemStyle: { color: '#6366f1' }
    }]
  })
}

async function load() {
  loading.value = true
  try {
    stats.value = await studentStats()
    // DOM 更新后再初始化图表
    setTimeout(() => renderChart(stats.value.pointsTrend || []), 0)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleResize() {
  chart?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<template>
  <n-spin :show="loading">
    <div v-if="stats">
      <n-h2 style="margin-bottom: 4px">学习看板</n-h2>
      <n-text depth="3">学习进度、积分与成绩总览</n-text>

      <!-- 概况指标卡 -->
      <n-grid cols="1 s:2 m:4" responsive="screen" :x-gap="16" :y-gap="16" style="margin-top: 16px">
        <n-grid-item>
          <n-card>
            <n-statistic label="在学课程" :value="stats.courseCount">
              <template #suffix>门</template>
            </n-statistic>
          </n-card>
        </n-grid-item>
        <n-grid-item>
          <n-card>
            <n-statistic label="平均完成度" :value="stats.avgProgress">
              <template #suffix>%</template>
            </n-statistic>
          </n-card>
        </n-grid-item>
        <n-grid-item>
          <n-card>
            <n-statistic label="累计学习时长" :value="formatSeconds(stats.studySeconds)" />
          </n-card>
        </n-grid-item>
        <n-grid-item>
          <n-card>
            <n-statistic label="已完成视频" :value="stats.finishedVideos">
              <template #suffix>节</template>
            </n-statistic>
          </n-card>
        </n-grid-item>
        <n-grid-item>
          <n-card>
            <n-statistic label="当前积分" :value="stats.pointsBalance" />
          </n-card>
        </n-grid-item>
        <n-grid-item>
          <n-card>
            <n-statistic label="练习正确率" :value="stats.practiceAccuracy">
              <template #suffix>%</template>
            </n-statistic>
          </n-card>
        </n-grid-item>
        <n-grid-item>
          <n-card>
            <n-statistic label="参加考试" :value="stats.examCount">
              <template #suffix>次</template>
            </n-statistic>
          </n-card>
        </n-grid-item>
        <n-grid-item>
          <n-card>
            <n-statistic label="考试平均分" :value="stats.examAvgScore" />
          </n-card>
        </n-grid-item>
      </n-grid>

      <!-- 积分趋势 -->
      <n-card title="近 14 天积分变动趋势" style="margin-top: 16px">
        <div ref="chartEl" style="height: 300px" />
        <n-empty v-if="!stats.pointsTrend || stats.pointsTrend.length === 0" description="暂无积分数据" />
      </n-card>
    </div>
  </n-spin>
</template>
