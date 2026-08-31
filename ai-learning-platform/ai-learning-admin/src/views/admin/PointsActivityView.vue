<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import * as opsApi from '../../api/ops'

const loading = ref(false)
const list = ref([])

const modalVisible = ref(false)
const editingId = ref(null)
const form = reactive({
  title: '',
  description: '',
  icon: '',
  activityType: 1,
  reward: 10,
  couponName: '',
  couponType: 1,
  couponValue: 10,
  couponThreshold: 0,
  couponExpireDays: 30,
  sortOrder: 0
})

const columns = [
  { title: '活动名称', dataIndex: 'title' },
  { title: '类型', key: 'type', width: 100 },
  { title: '奖励', key: 'benefit', width: 160 },
  { title: '完成/使用说明', dataIndex: 'description' },
  { title: '排序', dataIndex: 'sortOrder', width: 80 },
  { title: '状态', key: 'enabled', width: 100 },
  { title: '操作', key: 'action', width: 200 }
]

function couponText(a) {
  return a.couponType === 2 ? `${(a.couponValue / 10).toFixed(1)} 折券` : (a.couponThreshold > 0 ? `满${a.couponThreshold}减${a.couponValue}` : `立减${a.couponValue} 元`)
}

async function load() {
  loading.value = true
  try {
    list.value = await opsApi.activityList()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    title: '', description: '', icon: '', activityType: 1, reward: 10,
    couponName: '', couponType: 1, couponValue: 10, couponThreshold: 0,
    couponExpireDays: 30, sortOrder: 0
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

function openEdit(record) {
  editingId.value = record.id
  Object.assign(form, {
    title: record.title,
    description: record.description,
    icon: record.icon,
    activityType: record.activityType,
    reward: record.reward,
    couponName: record.couponName,
    couponType: record.couponType || 1,
    couponValue: record.couponValue,
    couponThreshold: record.couponThreshold || 0,
    couponExpireDays: record.couponExpireDays || 30,
    sortOrder: record.sortOrder
  })
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.title) return message.warning('请填写活动名称')
  try {
    const payload = { ...form }
    if (editingId.value) {
      await opsApi.updateActivity(editingId.value, payload)
      message.success('已更新')
    } else {
      await opsApi.createActivity(payload)
      message.success('已创建（默认未发布）')
    }
    modalVisible.value = false
    load()
  } catch (e) {
    message.error(e.message)
  }
}

async function handleToggle(record) {
  try {
    const next = record.enabled === 1 ? 0 : 1
    await opsApi.toggleActivity(record.id, next)
    message.success(next === 1 ? '已发布' : '已下线')
    load()
  } catch (e) {
    message.error(e.message)
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '删除活动',
    content: `确定删除活动「${record.title}」吗？删除后学生端不再展示。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await opsApi.deleteActivity(record.id)
        message.success('已删除')
        load()
      } catch (e) {
        message.error(e.message)
      }
    }
  })
}

onMounted(load)
</script>

<template>
  <a-card title="积分活动管理" :bordered="false">
    <a-alert message="活动可发布/下线控制学生端展示；类型支持「积分任务」与「优惠券」，优惠券领取后发放到学生「我的优惠券」。" type="info" show-icon style="margin-bottom: 16px" />
    <div style="margin-bottom: 16px">
      <a-button type="primary" @click="openCreate">新建活动</a-button>
    </div>
    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag :color="record.activityType === 2 ? 'purple' : 'blue'">{{ record.activityType === 2 ? '优惠券' : '积分任务' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'benefit'">
          <span v-if="record.activityType === 2">{{ record.couponName }}（{{ couponText(record) }}）</span>
          <span v-else class="reward">+{{ record.reward }} 积分</span>
        </template>
        <template v-else-if="column.key === 'enabled'">
          <a-tag :color="record.enabled === 1 ? 'green' : 'default'">{{ record.enabled === 1 ? '已发布' : '未发布' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="openEdit(record)">编辑</a-button>
            <a-button size="small" :type="record.enabled === 1 ? 'default' : 'primary'" @click="handleToggle(record)">
              {{ record.enabled === 1 ? '下线' : '发布' }}
            </a-button>
            <a-button size="small" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑活动' : '新建活动'" :width="560" @ok="handleSubmit">
      <a-form layout="vertical" style="margin-top: 8px">
        <a-form-item label="活动名称" required>
          <a-input v-model:value="form.title" placeholder="例如：完成章节学习" />
        </a-form-item>
        <a-form-item label="活动类型">
          <a-radio-group v-model:value="form.activityType">
            <a-radio :value="1">积分任务</a-radio>
            <a-radio :value="2">优惠券</a-radio>
          </a-radio-group>
        </a-form-item>

        <template v-if="form.activityType === 1">
          <a-form-item label="奖励积分">
            <a-input-number v-model:value="form.reward" :min="0" :max="10000" style="width: 140px" />
          </a-form-item>
        </template>

        <template v-else>
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item label="券名称">
                <a-input v-model:value="form.couponName" placeholder="例如：课程满减券" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="券类型">
                <a-select v-model:value="form.couponType">
                  <a-select-option :value="1">满减券</a-select-option>
                  <a-select-option :value="2">折扣券</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item :label="form.couponType === 2 ? '折扣（85 = 8.5 折）' : '减免金额（元）'">
                <a-input-number v-model:value="form.couponValue" :min="1" :max="10000" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item v-if="form.couponType === 1" label="使用门槛（0 = 无门槛）">
                <a-input-number v-model:value="form.couponThreshold" :min="0" :max="100000" style="width: 100%" />
              </a-form-item>
              <a-form-item v-else label="有效期（天）">
                <a-input-number v-model:value="form.couponExpireDays" :min="1" :max="365" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item v-if="form.couponType === 1" label="有效期（天）">
            <a-input-number v-model:value="form.couponExpireDays" :min="1" :max="365" style="width: 140px" />
          </a-form-item>
        </template>

        <a-form-item label="完成/使用说明">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="展示给学生的说明文字" />
        </a-form-item>
        <a-form-item label="图标标识">
          <a-input v-model:value="form.icon" placeholder="例如：chapter / robot / coupon" />
        </a-form-item>
        <a-form-item label="排序（越小越靠前）">
          <a-input-number v-model:value="form.sortOrder" :min="0" :max="9999" style="width: 140px" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<style scoped>
.reward {
  color: #fa8c16;
  font-weight: 600;
}
</style>