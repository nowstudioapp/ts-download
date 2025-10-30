<template>
  <div class="page-container">
    <div class="page-header">
      <h1>数据下载管理系统</h1>
      <p>支持数据查询、单类型下载和多条件组合下载</p>
    </div>
    
    <DownloadPage />
  </div>
</template>

<script setup>
import DownloadPage from './components/DownloadPage.vue'
</script>
