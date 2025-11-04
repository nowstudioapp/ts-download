<template>
  <!-- 未登录状态显示登录页面 -->
  <LoginPage v-if="!isAuthenticated" @login-success="handleLoginSuccess" />
  
  <!-- 已登录状态显示主页面 -->
  <div v-else class="page-container">
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
        </div>
        <div class="header-right">
          <el-button 
            type="danger" 
            size="small" 
            @click="handleLogout"
            :icon="SwitchButton"
          >
            退出系统
          </el-button>
        </div>
      </div>
    </div>
    
    <DownloadPage />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { SwitchButton } from '@element-plus/icons-vue'
import LoginPage from './components/LoginPage.vue'
import DownloadPage from './components/DownloadPage.vue'

const isAuthenticated = ref(false)

// 检查登录状态
const checkAuthStatus = () => {
  const authStatus = localStorage.getItem('ts_download_auth')
  const authTime = localStorage.getItem('ts_download_auth_time')
  
  if (authStatus === 'true' && authTime) {
    const loginTime = parseInt(authTime)
    const currentTime = Date.now()
    const timeDiff = currentTime - loginTime
    
    // 登录状态有效期24小时
    const VALID_DURATION = 24 * 60 * 60 * 1000
    
    if (timeDiff < VALID_DURATION) {
      isAuthenticated.value = true
    } else {
      // 登录过期，清除状态
      clearAuthStatus()
    }
  }
}

// 清除登录状态
const clearAuthStatus = () => {
  localStorage.removeItem('ts_download_auth')
  localStorage.removeItem('ts_download_auth_time')
  isAuthenticated.value = false
}

// 处理登录成功
const handleLoginSuccess = () => {
  isAuthenticated.value = true
}

// 处理退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出系统吗？',
      '退出确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    clearAuthStatus()
    ElMessage.success('已安全退出系统')
  } catch {
    // 用户取消退出
  }
}

onMounted(() => {
  checkAuthStatus()
})
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 30px 40px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
}

.header-left h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
}

.header-left p {
  margin: 0;
  font-size: 16px;
  opacity: 0.9;
}

.header-right {
  display: flex;
  align-items: center;
}

:deep(.el-button--danger) {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
}

:deep(.el-button--danger:hover) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.5);
}
</style>
