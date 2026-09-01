<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import { useUserStore } from '../../stores/user'
import { teacherStats, adminStats } from '../../api/stats'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

const loading = ref(false)
const stats = ref(null)

// 教师整体平均完成度（各课程平均完成度的均值）
const overallAvgProgress = computed(() => {
  const list = stats.value?.courseProgress || []
  if (list.length === 0) return 0
  const sum = list.reduce((acc, c) => acc + Number(c.avgProgress || 0), 0)
  return (sum / list.length).toFixed(1)
})

// 图表实例与 DOM 引用
const chartRefs = ref({})
const charts = {}

function setChartRef(key) {
  return (el) => {
    if (el) chartRefs.value[key] = el
  }
}

function initChart(key, option) {
  const el = chartRefs.value[key]
  if (!el) return
  if (charts[key]) charts[key].dispose()
  charts[key] = echarts.init(el)
  charts[key].setOption(option)
}

// ---------- 教师看板图表 ----------
function renderTeacher() {
  const s = stats.value
  if (!s) return
  nextTick(() => {
    // 选课人数柱状图
    initChart('enroll', {
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 30, bottom: 40 },
      xAxis: { type: 'category', data: (s.enrollmentByCourse || []).map(c => c.title), axisLabel: { interval: 0, rotate: 20 } },
      yAxis: { type: 'value', name: '选课人数' },
      series: [{ type: 'bar', data: (s.enrollmentByCourse || []).map(c => c.count), itemStyle: { color: '#1677ff' }, barMaxWidth: 40 }]
    })
    // 课程平均完成度
    initChart('progress', {
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 30, bottom: 40 },
      xAxis: { type: 'category', data: (s.courseProgress || []).map(c => c.title), axisLabel: { interval: 0, rotate: 20 } },
      yAxis: { type: 'value', name: '完成度 %', max: 100 },
      series: [{ type: 'bar', data: (s.courseProgress || []).map(c => Number(c.avgProgress)), itemStyle: { color: '#52c41a' }, barMaxWidth: 40 }]
    })
    // 章节完课率（横向条形，按完课率升序，便于发现薄弱章节）
    const chapters = [...(s.chapterCompletion || [])].sort((a, b) => Number(a.completionRate) - Number(b.completionRate))
    initChart('chapterCompletion', {
      tooltip: { trigger: 'axis', formatter: params => {
        const p = params[0]
        const c = chapters[p.dataIndex]
        return `${c.title}<br/>所属课程：${c.courseTitle}<br/>完课率：${p.value}%`
      } },
      grid: { left: 10, right: 40, top: 20, bottom: 20, containLabel: true },
      xAxis: { type: 'value', name: '完课率 %', max: 100 },
      yAxis: { type: 'category', data: chapters.map(c => c.title), axisLabel: { width: 120, overflow: 'truncate' } },
      series: [{ type: 'bar', data: chapters.map(c => Number(c.completionRate)), itemStyle: { color: '#13c2c2' }, barMaxWidth: 20 }]
    })
  })
}

// ---------- 管理看板图表 ----------
function renderAdmin() {
  const s = stats.value
  if (!s) return
  nextTick(() => {
    // 用户增长折线
    initChart('userGrowth', {
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: (s.userGrowth || []).map(t => t.day), boundaryGap: false },
      yAxis: { type: 'value', name: '新增用户' },
      series: [{ type: 'line', smooth: true, data: (s.userGrowth || []).map(t => Number(t.value)), areaStyle: { opacity: 0.15 }, itemStyle: { color: '#1677ff' } }]
    })
    // AI 调用趋势
    initChart('aiCall', {
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: (s.aiCallTrend || []).map(t => t.day), boundaryGap: false },
      yAxis: { type: 'value', name: '调用次数' },
      series: [{ type: 'line', smooth: true, data: (s.aiCallTrend || []).map(t => Number(t.value)), areaStyle: { opacity: 0.15 }, itemStyle: { color: '#722ed1' } }]
    })
    // 课程热度横向条形
    const top = s.topCourses || []
    initChart('topCourses', {
      tooltip: { trigger: 'axis' },
      grid: { left: 10, right: 40, top: 20, bottom: 20, containLabel: true },
      xAxis: { type: 'value', name: '选课数' },
      yAxis: { type: 'category', data: top.map(c => c.title).reverse() },
      series: [{ type: 'bar', data: top.map(c => c.count).reverse(), itemStyle: { color: '#fa8c16' }, barMaxWidth: 24 }]
    })
    // 积分发放/消耗
    const ps = s.pointsStats || []
    initChart('points', {
      tooltip: { trigger: 'axis' },
      legend: { data: ['发放', '消耗'] },
      grid: { left: 40, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: ps.map(p => p.typeName) },
      yAxis: { type: 'value', name: '积分' },
      series: [
        { name: '发放', type: 'bar', data: ps.map(p => Number(p.earned)), itemStyle: { color: '#52c41a' } },
        { name: '消耗', type: 'bar', data: ps.map(p => Number(p.spent)), itemStyle: { color: '#f5222d' } }
      ]
    })
  })
}

async function load() {
  loading.value = true
  try {
    stats.value = isAdmin.value ? await adminStats() : await teacherStats()
    if (isAdmin.value) renderAdmin()
    else renderTeacher()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleResize() {
  Object.values(charts).forEach(c => c?.resize())
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(charts).forEach(c => c?.dispose())
})
</script>

<template>
  <a-spin :spinning="loading">
    <!-- 教师看板 -->
    <template v-if="!isAdmin && stats">
      <a-empty v-if="stats.courseCount === 0" description="暂无课程，请先创建课程" style="margin-top: 80px" />
      <template v-else>
      <a-row :gutter="16">
        <a-col :xs="12" :sm="6"><a-card><a-statistic title="我的课程" :value="stats.courseCount" suffix="门" /></a-card></a-col>
        <a-col :xs="12" :sm="6"><a-card><a-statistic title="累计选课" :value="stats.totalStudents" suffix="人次" /></a-card></a-col>
        <a-col :xs="12" :sm="6"><a-card><a-statistic title="易错题（TOP）" :value="(stats.topWrongQuestions || []).length" suffix="道" /></a-card></a-col>
        <a-col :xs="12" :sm="6"><a-card><a-statistic title="平均完成度" :value="overallAvgProgress" suffix="%" :precision="1" /></a-card></a-col>
      </a-row>

      <a-row :gutter="16" style="margin-top: 16px">
        <a-col :xs="24" :sm="12">
          <a-card title="各课程选课人数"><div :ref="setChartRef('enroll')" style="height: 280px" /></a-card>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-card title="各课程平均完成度"><div :ref="setChartRef('progress')" style="height: 280px" /></a-card>
        </a-col>
      </a-row>

      <a-card title="章节完课率（完成全部视频的学生占比）" style="margin-top: 16px">
        <div v-if="(stats.chapterCompletion || []).length > 0" :ref="setChartRef('chapterCompletion')" style="height: 300px" />
        <a-empty v-else description="暂无完课数据" />
      </a-card>

      <a-card title="易错题 TOP10" style="margin-top: 16px">
        <a-table :data-source="stats.topWrongQuestions || []" row-key="questionId" size="small" :pagination="false">
          <a-table-column title="题目" data-index="content" ellipsis />
          <a-table-column title="所属课程" data-index="courseTitle" :width="180" />
          <a-table-column title="错误次数" data-index="wrongCount" :width="100" align="center" />
        </a-table>
        <a-empty v-if="!stats.topWrongQuestions || stats.topWrongQuestions.length === 0" description="暂无错题数据" />
      </a-card>
      </template>
    </template>

    <!-- 管理员看板 -->
    <template v-else-if="isAdmin && stats">
      <a-row :gutter="16">
        <a-col :xs="12" :sm="5"><a-card><a-statistic title="用户总数" :value="stats.userTotal" /></a-card></a-col>
        <a-col :xs="12" :sm="5"><a-card><a-statistic title="学生数" :value="stats.studentTotal" /></a-card></a-col>
        <a-col :xs="12" :sm="5"><a-card><a-statistic title="课程总数" :value="stats.courseTotal" /></a-card></a-col>
        <a-col :xs="12" :sm="5"><a-card><a-statistic title="选课总数" :value="stats.enrollmentTotal" /></a-card></a-col>
        <a-col :xs="24" :sm="4"><a-card><a-statistic title="题目总数" :value="stats.questionTotal" /></a-card></a-col>
      </a-row>

      <a-row :gutter="16" style="margin-top: 16px">
        <a-col :xs="24" :sm="12"><a-card title="近 14 天用户增长"><div :ref="setChartRef('userGrowth')" style="height: 260px" /></a-card></a-col>
        <a-col :xs="24" :sm="12"><a-card title="近 14 天 AI 调用趋势"><div :ref="setChartRef('aiCall')" style="height: 260px" /></a-card></a-col>
      </a-row>

      <a-row :gutter="16" style="margin-top: 16px">
        <a-col :xs="24" :sm="12"><a-card title="课程热度 TOP8"><div :ref="setChartRef('topCourses')" style="height: 280px" /></a-card></a-col>
        <a-col :xs="24" :sm="12"><a-card title="积分发放 / 消耗统计"><div :ref="setChartRef('points')" style="height: 280px" /></a-card></a-col>
      </a-row>
    </template>
  </a-spin>
</template>
