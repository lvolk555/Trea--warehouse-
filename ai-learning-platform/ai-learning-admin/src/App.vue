<script setup>
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

dayjs.locale('zh-cn')
</script>

<template>
  <!-- Ant Design Vue 全局配置：中文语言包 -->
  <a-config-provider :locale="zhCN">
    <router-view />
  </a-config-provider>
</template>
