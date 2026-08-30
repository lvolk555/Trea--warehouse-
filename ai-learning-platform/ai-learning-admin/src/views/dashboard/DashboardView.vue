<script setup>
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
</script>

<template>
  <a-card>
    <a-result status="success" :title="`管理端骨架搭建完成`" :sub-title="`欢迎，${userStore.user?.nickname || userStore.user?.username}！登录鉴权已打通，后续将按角色上线课程管理、题库、组卷、AI 工具、学情分析（教师）与审核、公告、积分运营、用户管理（管理员）等模块。`">
      <template #extra>
        <a-tag color="blue">阶段一：基础骨架</a-tag>
        <a-tag color="green">登录鉴权 ✓</a-tag>
        <a-tag>角色菜单 ✓</a-tag>
      </template>
    </a-result>
  </a-card>
</template>
