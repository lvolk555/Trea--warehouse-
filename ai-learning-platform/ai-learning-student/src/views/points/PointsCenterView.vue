<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { pointsAccount, pointsRecords, dailySign, signMonth, exchangeCourse, myExchanges, pointsActivities, claimActivity, myCoupons } from '../../api/points'
import { courseSquare } from '../../api/course'

const message = useMessage()

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
// 积分活动
const activities = ref([])
// 我的优惠券
const coupons = ref([])

const typeText = { 1: '完课奖励', 2: '签到奖励', 3: '考试奖励', 4: 'AI 提问', 5: '兑换扣减', 6: '注册赠送', 7: '积分活动' }

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

// 兑换弹窗：选择优惠券抵扣
const exchangeTarget = ref(null)
const exchangeVisible = ref(false)
const selectedCouponId = ref(null)

// 可用优惠券（未使用、未过期，满减券满足门槛）
const usableCoupons = computed(() => {
  const price = exchangeTarget.value?.pointsPrice || 0
  return coupons.value.filter(c => {
    if (c.status !== 0) return false
    if (c.expireTime && new Date(c.expireTime) < new Date()) return false
    if (c.type === 1 && c.threshold > 0 && price < c.threshold) return false
    return true
  })
})

// 折后应付积分
const exchangePay = computed(() => {
  const price = exchangeTarget.value?.pointsPrice || 0
  const c = usableCoupons.value.find(x => x.id === selectedCouponId.value)
  if (!c) return price
  if (c.type === 2) return Math.max(0, Math.round(price * c.value / 100))
  return Math.max(0, price - c.value)
})

const exchangeDiscount = computed(() => {
  const price = exchangeTarget.value?.pointsPrice || 0
  return Math.max(0, price - exchangePay.value)
})

function openExchange(course) {
  exchangeTarget.value = course
  selectedCouponId.value = null
  exchangeVisible.value = true
}

async function confirmExchange() {
  const course = exchangeTarget.value
  try {
    await exchangeCourse(course.id, selectedCouponId.value || undefined)
    message.success('兑换成功，已自动选课')
    exchangeVisible.value = false
    await Promise.all([loadAccount(), loadMall(), loadCoupons()])
  } catch (e) {
    message.error(e.message)
  }
}

async function loadActivities() {
  try {
    activities.value = await pointsActivities()
  } catch (e) {
    /* 忽略 */
  }
}

async function loadCoupons() {
  try {
    coupons.value = await myCoupons()
  } catch (e) {
    /* 忽略 */
  }
}

function isCouponActivity(a) {
  return a.activityType === 2
}

function couponText(c) {
  if (c.type === 2) return `${(c.value / 10).toFixed(1)} 折`
  return c.threshold > 0 ? `满${c.threshold}减${c.value}积分` : `立减${c.value}积分`
}

function couponActivityText(a) {
  if (a.couponType === 2) return `${(a.couponValue / 10).toFixed(1)} 折券`
  return a.couponThreshold > 0 ? `满${a.couponThreshold}减${a.couponValue}` : `立减${a.couponValue}积分`
}

function couponStatusText(s) {
  return s === 1 ? '已使用' : s === 2 ? '已过期' : '未使用'
}

async function handleClaim(activity) {
  try {
    const r = await claimActivity(activity.id)
    if (r.activityType === 2) {
      message.success(`领取成功，已发放「${r.couponName}」`)
      await Promise.all([loadActivities(), loadCoupons()])
    } else {
      message.success(`领取成功，+${r.reward} 积分`)
      await Promise.all([loadAccount(), loadActivities()])
    }
  } catch (e) {
    message.warning(e.message)
  }
}

onMounted(() => {
  loadAccount()
  loadSign()
  loadRecords()
  loadMall()
  loadActivities()
  loadCoupons()
})
</script>

<template>
  <div>
    <n-h2 style="margin-bottom: 4px">积分中心</n-h2>
    <n-text depth="3">签到、完课、考试获得积分，可用积分兑换课程</n-text>

    <!-- 账户概览 -->
    <n-card style="margin-top: 16px">
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

    <!-- 积分活动 -->
    <n-card title="积分活动" size="small" style="margin-top: 16px">
      <template #header-extra>
        <n-text depth="3" style="font-size: 12px">完成任务/领取限时优惠券</n-text>
      </template>
      <n-grid cols="1 s:2 m:4" responsive="screen" :x-gap="12" :y-gap="12">
        <n-grid-item v-for="a in activities" :key="a.id">
          <div class="activity-item">
            <div class="activity-icon">{{ a.title.charAt(0) }}</div>
            <div class="activity-body">
              <div class="activity-title">{{ a.title }}</div>
              <div class="activity-desc">{{ a.description }}</div>
            </div>
            <div class="activity-right">
              <n-tag :type="isCouponActivity(a) ? 'error' : 'warning'" size="small">
                {{ isCouponActivity(a) ? couponActivityText(a) : `+${a.reward} 积分` }}
              </n-tag>
              <n-button size="tiny" :type="a.claimed ? 'default' : 'primary'" :disabled="a.claimed" @click="handleClaim(a)">
                {{ a.claimed ? '已领取' : '领取' }}
              </n-button>
            </div>
          </div>
        </n-grid-item>
      </n-grid>
      <n-empty v-if="activities.length === 0" description="暂无积分活动" style="margin: 16px 0" />
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
          <n-grid cols="1 s:2 m:3" responsive="screen" :x-gap="16" :y-gap="16">
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
                  @click="openExchange(course)"
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
              <tr><th>课程 ID</th><th>优惠</th><th>实付积分</th><th>状态</th><th>时间</th></tr>
            </thead>
            <tbody>
              <tr v-for="e in exchanges" :key="e.id">
                <td>#{{ e.courseId }}</td>
                <td>{{ e.discount ? `-${e.discount}` : '-' }}</td>
                <td>{{ e.pointsCost }}</td>
                <td><n-tag :type="e.status === 1 ? 'success' : 'error'" size="small">{{ e.status === 1 ? '成功' : '失败' }}</n-tag></td>
                <td class="muted">{{ e.createTime }}</td>
              </tr>
            </tbody>
          </n-table>
          <n-empty v-if="exchanges.length === 0" description="暂无兑换记录" style="margin: 24px 0" />
        </n-spin>
      </n-tab-pane>

      <!-- 我的优惠券 -->
      <n-tab-pane name="coupons" tab="我的优惠券">
        <n-grid v-if="coupons.length" cols="1 s:2 m:3" responsive="screen" :x-gap="16" :y-gap="16">
          <n-grid-item v-for="c in coupons" :key="c.id">
            <div class="coupon-card" :class="{ used: c.status === 1, expired: c.status === 2 }">
              <div class="coupon-left">
                <div class="coupon-value">{{ c.type === 2 ? (c.value / 10).toFixed(1) + '折' : c.value + '积分' }}</div>
                <div class="coupon-name">{{ c.name }}</div>
              </div>
              <div class="coupon-right">
                <div class="coupon-desc">{{ couponText(c) }}</div>
                <div class="coupon-expire">有效期至 {{ c.expireTime }}</div>
                <n-tag :type="c.status === 0 ? 'info' : 'default'" size="small">{{ couponStatusText(c.status) }}</n-tag>
              </div>
            </div>
          </n-grid-item>
        </n-grid>
        <n-empty v-else description="暂无优惠券，去积分活动领取吧" style="margin: 24px 0" />
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
.activity-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;
}
.activity-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.activity-body {
  flex: 1;
  min-width: 0;
}
.activity-title {
  font-weight: 600;
  font-size: 14px;
}
.activity-desc {
  color: #9ca3af;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.activity-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}
.muted {
  color: #9ca3af;
  font-size: 13px;
}
.coupon-card {
  display: flex;
  align-items: stretch;
  border: 1px solid #f0e6d2;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.coupon-left {
  flex-shrink: 0;
  min-width: 92px;
  padding: 14px;
  background: linear-gradient(135deg, #ff7a45, #fa541c);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.coupon-value {
  font-size: 22px;
  font-weight: 700;
}
.coupon-name {
  font-size: 12px;
  margin-top: 4px;
  opacity: 0.9;
}
.coupon-right {
  flex: 1;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}
.coupon-desc {
  font-weight: 600;
  font-size: 14px;
}
.coupon-expire {
  color: #9ca3af;
  font-size: 12px;
}
.coupon-card.used, .coupon-card.expired {
  filter: grayscale(100%);
  opacity: 0.6;
}
@media (max-width: 768px) {
  .account-row {
    flex-wrap: wrap;
    gap: 16px;
  }
  .account-row .stat {
    flex: 1 1 40%;
  }
}
</style>
