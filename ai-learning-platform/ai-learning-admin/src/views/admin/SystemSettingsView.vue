<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SaveOutlined, RobotOutlined, HomeOutlined } from '@ant-design/icons-vue'
import * as opsApi from '../../api/ops'

const loading = ref(false)
const saving = ref(false)

// 配置表单：基础 + 开关
const form = reactive({
  site_name: '',
  ai_enabled: '1',
  register_enabled: '1'
})

// AI 接入配置：密钥留空表示不修改（回显为脱敏值，不回填输入框）
const aiApiKeyInput = ref('')
const aiConfig = reactive({
  ai_model: '',
  ai_base_url: ''
})
const maskedKeyHint = ref('')

// 模型建议（可自由输入其他模型名）
const modelSuggestions = ['glm-4.7-flash', 'glm-4.6-flash', 'glm-4.5-flash', 'glm-4.5-air', 'glm-4-plus']

function handleModelSearch(val) {
  modelOptions.value = modelSuggestions.filter(m => !val || m.toLowerCase().includes(val.toLowerCase()))
}
const modelOptions = ref(modelSuggestions)

async function load() {
  loading.value = true
  try {
    const list = await opsApi.systemSettings()
    for (const item of list) {
      if (item.configKey in form) {
        form[item.configKey] = item.configValue
      } else if (item.configKey === 'ai_model') {
        aiConfig.ai_model = item.configValue
      } else if (item.configKey === 'ai_base_url') {
        aiConfig.ai_base_url = item.configValue
      } else if (item.configKey === 'ai_api_key') {
        maskedKeyHint.value = item.configValue
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
    const payload = {
      ...form,
      ai_model: aiConfig.ai_model.trim(),
      ai_base_url: aiConfig.ai_base_url.trim()
    }
    // 密钥留空 = 保持不变
    if (aiApiKeyInput.value.trim()) {
      payload.ai_api_key = aiApiKeyInput.value.trim()
    }
    await opsApi.saveSystemSettings(payload)
    message.success('系统设置已保存')
    aiApiKeyInput.value = ''
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
        <a-form layout="vertical" class="form-area">
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
        <a-form layout="vertical" class="form-area">
          <a-form-item label="AI 功能总开关">
            <a-switch v-model:checked="form.ai_enabled" checked-value="1" un-checked-value="0" />
            <span class="switch-tip">{{ form.ai_enabled === '1' ? '已开启：AI 答疑 / 智能出题 / 智能批改正常可用' : '已关闭：所有 AI 功能将提示「AI 功能已被管理员关闭」' }}</span>
          </a-form-item>

          <a-divider class="divider" orientation="left" plain>模型接入（修改后即时生效，无需重启）</a-divider>

          <a-form-item label="API 密钥" extra="留空表示不修改当前密钥；仅管理员可见，保存后脱敏存储">
            <a-input-password
              v-model:value="aiApiKeyInput"
              :placeholder="maskedKeyHint ? `不修改请留空（当前：${maskedKeyHint}）` : '留空使用后端默认配置'"
            />
          </a-form-item>
          <a-form-item label="模型名称" extra="如 glm-4.7-flash；留空使用后端默认配置">
            <a-auto-complete
              v-model:value="aiConfig.ai_model"
              :options="modelOptions.map(m => ({ value: m }))"
              placeholder="输入或选择模型名称"
              @search="handleModelSearch"
            />
          </a-form-item>
          <a-form-item label="服务端点（Base URL）" extra="OpenAI 兼容端点；留空使用后端默认配置（智谱）">
            <a-input v-model:value="aiConfig.ai_base_url" placeholder="https://open.bigmodel.cn/api/paas/v4" allow-clear />
          </a-form-item>

          <a-alert type="info" show-icon style="margin-top: 4px">
            <template #message>
              配置优先级：此处设置 &gt; 后端配置文件；密钥仅脱敏回显，模型与端点可随时切换，配置变更后下一次调用即生效。
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
.form-area {
  max-width: 560px;
}
.switch-tip {
  margin-left: 12px;
  color: #6b7280;
  font-size: 13px;
}
.divider {
  margin: 8px 0 20px;
}
.actions {
  margin-top: 16px;
}
</style>
