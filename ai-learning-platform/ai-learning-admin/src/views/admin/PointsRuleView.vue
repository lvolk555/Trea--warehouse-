<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import * as opsApi from '../../api/ops'

const loading = ref(false)
const rules = ref([])

const ruleName = {
  video_finish: '完成视频奖励',
  daily_sign: '每日签到奖励',
  exam_pass: '考试及格奖励',
  ai_ask: 'AI 提问奖励',
  register_gift: '注册赠送'
}

const columns = [
  { title: '规则', key: 'name' },
  { title: '奖励积分', key: 'value', width: 180 },
  { title: '每日上限', key: 'limit', width: 180 },
  { title: '状态', key: 'enabled', width: 100 },
  { title: '操作', key: 'action', width: 100 }
]

// 行内编辑暂存
const editing = ref({})

async function load() {
  loading.value = true
  try {
    rules.value = await opsApi.pointsRules()
    rules.value.forEach(r => {
      editing.value[r.id] = { ruleValue: r.ruleValue, dailyLimit: r.dailyLimit }
    })
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function handleSave(record) {
  try {
    const e = editing.value[record.id]
    await opsApi.updatePointsRule(record.id, { ruleValue: e.ruleValue, dailyLimit: e.dailyLimit })
    message.success('已保存')
    load()
  } catch (e) {
    message.error(e.message)
  }
}

async function handleToggle(record) {
  try {
    await opsApi.updatePointsRule(record.id, { enabled: record.enabled === 1 ? 0 : 1 })
    message.success(record.enabled === 1 ? '已停用' : '已启用')
    load()
  } catch (e) {
    message.error(e.message)
  }
}

onMounted(load)
</script>

<template>
  <a-card title="积分规则配置" :bordered="false">
    <a-alert message="调整奖励值与每日上限后立即对新发生的积分行为生效；停用规则后对应行为不再发放积分。" type="info" show-icon style="margin-bottom: 16px" />
    <a-table :columns="columns" :data-source="rules" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 'max-content' }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          {{ ruleName[record.ruleKey] || record.ruleKey }}
          <span class="key">（{{ record.ruleKey }}）</span>
        </template>
        <template v-else-if="column.key === 'value'">
          <a-input-number v-model:value="editing[record.id].ruleValue" :min="0" :max="10000" style="width: 110px" />
        </template>
        <template v-else-if="column.key === 'limit'">
          <a-input-number v-model:value="editing[record.id].dailyLimit" :min="0" :max="100000" style="width: 110px" />
          <span class="key">（0 = 不限）</span>
        </template>
        <template v-else-if="column.key === 'enabled'">
          <a-tag :color="record.enabled === 1 ? 'green' : 'default'">{{ record.enabled === 1 ? '启用' : '停用' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" type="primary" @click="handleSave(record)">保存</a-button>
            <a-button size="small" @click="handleToggle(record)">{{ record.enabled === 1 ? '停用' : '启用' }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<style scoped>
.key {
  color: #9ca3af;
  font-size: 12px;
}
</style>
