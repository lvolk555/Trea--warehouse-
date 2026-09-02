<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SaveOutlined, RobotOutlined, TeamOutlined, HomeOutlined } from '@ant-design/icons-vue'
import * as opsApi from '../../api/ops'

const loading = ref(false)
const saving = ref(false)

// 配置表单：站点名称 + 两个开关
const form = reactive({
  site_name: '',
  ai_enabled: '1',
  register_enabled: '1'
})

async function load() {
  loading.value = true
  try {
    const list = await opsApi.systemSettings()
    for (const item of list) {
      if (item.configKey in form) {
        form[item.configKey] = item.configValue
      }
    }
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.site_name.trim()) {
    message.warning('站点名称不能为空')
    return
  }
  saving.value = true
  try {
    await opsApi.saveSystemSettings({ ...form })
    message.success('系统设置已保存')
    load()
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <a-spin :spinning="loading">
    <div class="settings-page">
      <a-card :bordered="false">
        <template #title>
          <HomeOutlined /> 基础设置
        </template>
        <a-form layout="vertical" style="max-width: 560px">
          <a-form-item label="站点名称" extra="展示在管理端侧边栏与学生端页面">
            <a-input v-model:value="form.site_name" placeholder="请输入站点名称" :maxlength="50" show-count />
          </a-form-item>
          <a-form-item label="学生自主注册">
            <a-switch v-model:checked="form.register_enabled" checked-value="1" un-checked-value="0" />
            <span class="switch-tip">{{ form.register_enabled === '1' ? '已开放：学生可在登录页自行注册' : '已关闭：账号由管理员在「用户管理」中创建' }}</span>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card :bordered="false" style="margin-top: 16px">
        <template #title>
          <RobotOutlined /> AI 功能设置
        </template>
        <a-form layout="vertical" style="max-width: 560px">
          <a-form-item label="AI 功能总开关">
            <a-switch v-model:checked="form.ai_enabled" checked-value="1" un-checked-value="0" />
            <span class="switch-tip">{{ form.ai_enabled === '1' ? '已开启：AI 答疑 / 智能出题 / 智能批改正常可用' : '已关闭：所有 AI 功能将提示「AI 功能已被管理员关闭」' }}</span>
          </a-form-item>
          <a-alert type="info" show-icon style="margin-top: 4px">
            <template #message>
              当前接入智谱 GLM-4.7-Flash 模型；关闭总开关后不发起任何模型调用，学生端与管理端 AI 页面均降级提示。
            </template>
          </a-alert>
        </a-form>
      </a-card>

      <div class="actions">
        <a-button type="primary" :loading="saving" @click="handleSave">
          <SaveOutlined /> 保存设置
        </a-button>
      </div>
    </div>
  </a-spin>
</template>

<style scoped>
.settings-page {
  max-width: 720px;
}
.switch-tip {
  margin-left: 12px;
  color: #6b7280;
  font-size: 13px;
}
.actions {
  margin-top: 16px;
}
</style>
