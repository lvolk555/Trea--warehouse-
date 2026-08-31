<script setup>
import { ref, computed, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { pointsAccount, myCoupons, exchangeCourse } from '../api/points'

const props = defineProps({
  show: { type: Boolean, default: false },
  course: { type: Object, default: null }
})
const emit = defineEmits(['update:show', 'success'])

const message = useMessage()

const account = ref(null)
const coupons = ref([])
const selectedCouponId = ref(null)

const price = computed(() => props.course?.pointsPrice || 0)

// 可用优惠券（未使用、未过期，满减券满足门槛）
const usableCoupons = computed(() =>
  coupons.value.filter(c => {
    if (c.status !== 0) return false
    if (c.expireTime && new Date(c.expireTime) < new Date()) return false
    if (c.type === 1 && c.threshold > 0 && price.value < c.threshold) return false
    return true
  })
)

function couponText(c) {
  if (c.type === 2) return `${(c.value / 10).toFixed(1)} 折`
  return c.threshold > 0 ? `满${c.threshold}减${c.value}积分` : `立减${c.value}积分`
}

const couponOptions = computed(() => [
  { label: '不使用优惠券', value: null },
  ...usableCoupons.value.map(c => ({ label: `${c.name}（${couponText(c)}）`, value: c.id }))
])

const exchangePay = computed(() => {
  const c = usableCoupons.value.find(x => x.id === selectedCouponId.value)
  if (!c) return price.value
  if (c.type === 2) return Math.max(0, Math.round(price.value * c.value / 100))
  return Math.max(0, price.value - c.value)
})

const exchangeDiscount = computed(() => Math.max(0, price.value - exchangePay.value))

async function load() {
  selectedCouponId.value = null
  try {
    const [a, cs] = await Promise.all([pointsAccount(), myCoupons()])
    account.value = a
    coupons.value = cs
  } catch (e) {
    message.error(e.message)
  }
}

watch(() => props.show, v => { if (v) load() })

function close() {
  emit('update:show', false)
}

async function confirmExchange() {
  if (!props.course) return
  const pay = exchangePay.value
  const balance = account.value?.balance ?? 0
  if (balance < pay) {
    message.error(`积分不足，无法兑换（当前 ${balance}，需 ${pay}）`)
    return
  }
  try {
    await exchangeCourse(props.course.id, selectedCouponId.value || undefined)
    message.success('兑换成功，已自动选课')
    close()
    emit('success')
  } catch (e) {
    message.error(e.message)
  }
}
</script>

<template>
  <n-modal :show="show" preset="card" title="兑换课程" style="width: 480px" @update:show="v => emit('update:show', v)">
    <div v-if="course" class="exchange-box">
      <div class="course-title">{{ course.title }}</div>
      <div class="price-info">
        <div class="row"><span>原价</span><span>{{ course.pointsPrice }} 积分</span></div>
        <div v-if="exchangeDiscount > 0" class="row discount"><span>优惠券抵扣</span><span>-{{ exchangeDiscount }} 积分</span></div>
        <div class="row pay"><span>实付</span><span>{{ exchangePay }} 积分</span></div>
        <div class="row balance"><span>当前积分</span><span>{{ account?.balance ?? 0 }}</span></div>
      </div>
      <div class="coupon-select">
        <div class="select-label">选择优惠券</div>
        <n-select v-model:value="selectedCouponId" :options="couponOptions" placeholder="选择优惠券" />
        <n-text v-if="usableCoupons.length === 0" depth="3" style="font-size: 12px">
          暂无可用优惠券，去「积分活动」领取
        </n-text>
      </div>
    </div>
    <template #footer>
      <n-space justify="end">
        <n-button @click="close">取消</n-button>
        <n-button type="primary" @click="confirmExchange">确认兑换</n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<style scoped>
.exchange-box .course-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}
.price-info {
  background: #f7f7fa;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 16px;
}
.price-info .row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 14px;
}
.price-info .discount {
  color: #18a058;
}
.price-info .pay {
  font-weight: 700;
  color: #6366f1;
  border-top: 1px dashed #e0e0e6;
  margin-top: 6px;
  padding-top: 8px;
}
.price-info .balance {
  color: #9ca3af;
  font-size: 13px;
}
.coupon-select .select-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}
</style>