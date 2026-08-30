<script setup>
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
</script>

<template>
  <div>
    <n-card>
      <n-result status="success" title="学生端骨架搭建完成" :description="`欢迎，${userStore.user?.nickname || userStore.user?.username}！登录鉴权已打通，后续将陆续上线课程广场、学习中心、练习考试、积分中心与 AI 答疑。`">
        <template #footer>
          <n-space justify="center">
            <n-tag type="info">阶段一：基础骨架</n-tag>
            <n-tag type="success">登录鉴权 ✓</n-tag>
            <n-tag>课程广场（阶段二）</n-tag>
            <n-tag>积分中心（阶段五）</n-tag>
          </n-space>
        </template>
      </n-result>
    </n-card>
  </div>
</template>
