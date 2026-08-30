<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { pointsAccount, pointsRecords, dailySign, signMonth, exchangeCourse, myExchanges } from '../../api/points'
import { courseSquare } from '../../api/course'

const message = useMessage()
const dialog = useDialog()

const tab = ref('account')
const loading = ref(false)

// 账户
const account = ref(null)
// 签到
const signedToday = ref(false)
const monthSigns = ref([])
// 明细
const records = ref([])
const recordPage = ref(1)
const recordTotal = ref(0)
// 商城
const mallCourses = ref([])
const exchanges = ref([])

const typeText = { 1: '完课奖励', 2: '签到奖励', 3: '考试奖励', 4: 'AI 提问', 5: '兑换扣减', 6: '注册赠送' }

async function loadAccount() {
  try {
    account.value = await pointsAccount()
  } catch (e) {
    message.error(e.message)
  }
}

async function loadSign() {
  try {
    monthSigns.value = await signMonth()
    const today = new Date().toISOString().slice(0, 10)
    signedToday.value = monthSigns.value.some(s => String(s.signDate).slice(0, 10) === today)
  } catch (e) {
    /* 忽略 */
  }
}

async function handleSign() {
  try {
    await dailySign()
    message.success('签到成功，积分已到账')
    await Promise.all([loadSign(), loadAccount()])
  } catch (e) {
    message.warning(e.message)
  }
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await pointsRecords({ page: recordPage.value, size: 10 })
    records.value = res.records
    recordTotal.value = Number(res.total)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadMall() {
  loading.value = true
  try {
    const res = await courseSquare({ page: 1, size: 50 })
    mallCourses.value = (res.records || []).filter(c => c.priceType === 2)
    exchanges.value = await myExchanges()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

const exchangedCourseIds = computed(() => new Set(exchanges.value.filter(e => e.status === 1).map(e => e.courseId)))

function handleExchange(course) {
  dialog.warning({
    title: '确认兑换',
    content: `确定使用 ${course.pointsPrice} 积分兑换《${course.title}》吗？`,
    positiveText: '兑换',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await exchangeCourse(course.id)
        message.success('兑换成功，已自动选课')
        await Promise.all([loadAccount(), loadMall()])
      } catch (e) {
        message.error(e.message)
      }
    }
  })
}

onMounted(() => {
  loadAccount()
  loadSign()
  loadRecords()
  loadMall()
})
</script>

<template>
  <div>
    <h2 style="margin-bottom: 16px">积分中心</h2>

    <!-- 账户概览 -->
    <n-card>
      <div class="account-row">
        <div class="balance">
          <div class="num">{{ account?.balance ?? '--' }}</div>
          <div class="label">当前可用积分</div>
        </div>
        <n-divider vertical style="height: 60px" />
        <div class="stat">
          <div class="num small">{{ account?.totalEarned ?? '--' }}</div>
          <div class="label">累计获得</div>
        </div>
        <div class="stat">
          <div class="num small">{{ account?.totalSpent ?? '--' }}</div>
          <div class="label">累计消耗</div>
        </div>
        <div style="flex: 1" />
        <n-button type="primary" :disabled="signedToday" @click="handleSign">
          {{ signedToday ? '今日已签到' : '每日签到 +5' }}
        </n-button>
      </div>
      <div class="tips">
        获取方式：完成视频 +10 · 每日签到 +5 · 考试及格 +20 · AI 提问 +2（均有每日上限）
      </div>
    </n-card>

    <n-tabs v-model:value="tab" type="line" style="margin-top: 16px">
      <!-- 积分明细 -->
      <n-tab-pane name="records" tab="积分明细">
        <n-spin :show="loading">
          <n-table :bordered="false" :single-line="false" size="small">
            <thead>
              <tr><th>类型</th><th>说明</th><th>变动</th><th>时间</th></tr>
            </thead>
            <tbody>
              <tr v-for="r in records" :key="r.id">
                <td>{{ typeText[r.type] || '其他' }}</td>
                <td>{{ r.description }}</td>
                <td :style="{ color: r.changeValue > 0 ? '#18a058' : '#d03050', fontWeight: 600 }">
                  {{ r.changeValue > 0 ? '+' : '' }}{{ r.changeValue }}
                </td>
                <td class="muted">{{ r.createTime }}</td>
              </tr>
            </tbody>
          </n-table>
          <n-empty v-if="records.length === 0" description="暂无积分记录" style="margin: 24px 0" />
          <div style="margin-top: 12px; text-align: right">
            <n-pagination v-model:page="recordPage" :page-size="10" :item-count="recordTotal" @update:page="loadRecords" />
          </div>
        </n-spin>
      </n-tab-pane>

      <!-- 积分商城 -->
      <n-tab-pane name="mall" tab="积分商城">
        <n-spin :show="loading">
          <n-grid :cols="3" :x-gap="16" :y-gap="16">
            <n-grid-item v-for="course in mallCourses" :key="course.id">
              <n-card>
                <img :src="course.cover" class="mall-cover" />
                <div class="mall-title">{{ course.title }}</div>
                <div class="mall-meta">
                  <n-tag type="warning" size="small">{{ course.pointsPrice }} 积分</n-tag>
                </div>
                <n-button
                  block
                  :type="exchangedCourseIds.has(course.id) ? 'default' : 'primary'"
                  :disabled="exchangedCourseIds.has(course.id)"
                  style="margin-top: 12px"
                  @click="handleExchange(course)"
                >
                  {{ exchangedCourseIds.has(course.id) ? '已兑换' : '立即兑换' }}
                </n-button>
              </n-card>
            </n-grid-item>
          </n-grid>
          <n-empty v-if="mallCourses.length === 0" description="暂无可兑换课程" style="margin: 24px 0" />

          <h3 style="margin: 24px 0 12px">我的兑换记录</h3>
          <n-table :bordered="false" :single-line="false" size="small">
            <thead>
              <tr><th>课程 ID</th><th>消耗积分</th><th>状态</th><th>时间</th></tr>
            </thead>
            <tbody>
              <tr v-for="e in exchanges" :key="e.id">
                <td>#{{ e.courseId }}</td>
                <td>{{ e.pointsCost }}</td>
                <td><n-tag :type="e.status === 1 ? 'success' : 'error'" size="small">{{ e.status === 1 ? '成功' : '失败' }}</n-tag></td>
                <td class="muted">{{ e.createTime }}</td>
              </tr>
            </tbody>
          </n-table>
          <n-empty v-if="exchanges.length === 0" description="暂无兑换记录" style="margin: 24px 0" />
        </n-spin>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

<style scoped>
.account-row {
  display: flex;
  align-items: center;
  gap: 32px;
}
.balance .num {
  font-size: 40px;
  font-weight: 700;
  color: #6366f1;
}
.stat .num.small {
  font-size: 22px;
  font-weight: 600;
}
.label {
  color: #9ca3af;
  font-size: 13px;
}
.tips {
  margin-top: 12px;
  color: #9ca3af;
  font-size: 13px;
}
.mall-cover {
  width: 100%;
  height: 140px;
  object-fit: cover;
  border-radius: 6px;
}
.mall-title {
  font-weight: 600;
  margin: 10px 0 6px;
}
.mall-meta {
  display: flex;
  align-items: center;
}
.muted {
  color: #9ca3af;
  font-size: 13px;
}
</style>
