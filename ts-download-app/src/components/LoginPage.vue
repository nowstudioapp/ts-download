<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>系统访问验证</h2>
        <p>请输入访问密钥以继续使用系统</p>
      </div>
      
      <el-form 
        :model="loginForm" 
        :rules="rules"
        ref="loginFormRef"
        @submit.prevent="handleLogin"
        class="login-form"
      >
        <el-form-item prop="secretKey">
          <el-input
            v-model="loginForm.secretKey"
            type="password"
            placeholder="请输入访问密钥"
            show-password
            size="large"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-button"
          >
            <el-icon><Unlock /></el-icon>
            验证访问
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <p>请联系管理员获取访问密钥</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, Unlock } from '@element-plus/icons-vue'

const emit = defineEmits(['login-success'])

// 预设的密钥（实际项目中应该从后端验证）
const VALID_SECRET_KEY = 'jia1qaz!QAZ'

const loading = ref(false)
const loginFormRef = ref()

const loginForm = reactive({
  secretKey: ''
})

const rules = {
  secretKey: [
    { required: true, message: '请输入访问密钥', trigger: 'blur' },
    { min: 6, message: '密钥长度不能少于6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  try {
    await loginFormRef.value.validate()
    
    loading.value = true
    
    // 模拟验证延迟
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    if (loginForm.secretKey === VALID_SECRET_KEY) {
      ElMessage.success('验证成功，欢迎使用系统！')
      
      // 保存登录状态到localStorage
      localStorage.setItem('ts_download_auth', 'true')
      localStorage.setItem('ts_download_auth_time', Date.now().toString())
      
      emit('login-success')
    } else {
      ElMessage.error('访问密钥错误，请重新输入')
      loginForm.secretKey = ''
    }
  } catch (error) {
    console.log('表单验证失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-box {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  color: #333;
  margin-bottom: 10px;
  font-size: 24px;
  font-weight: 600;
}

.login-header p {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.login-form {
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
}

.login-footer {
  text-align: center;
}

.login-footer p {
  color: #999;
  font-size: 12px;
  margin: 0;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-button) {
  border-radius: 8px;
}
</style>
