<template>
  <div class="app-layout" v-if="currentUser">
    <el-container style="min-height: 100vh">
      <el-aside width="200px" class="app-aside">
        <div class="logo-area">
          <h2>TS 下载系统</h2>
        </div>
        <el-menu
          :default-active="$route.path"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/download">
            <el-icon><Download /></el-icon>
            <span>文件下载</span>
          </el-menu-item>
          <el-menu-item v-if="isLeaderOrAdmin" index="/validUsers">
            <el-icon><UserFilled /></el-icon>
            <span>有效用户下载</span>
          </el-menu-item>
          <el-menu-item v-if="isAdmin" index="/logs">
            <el-icon><Document /></el-icon>
            <span>下载日志</span>
          </el-menu-item>
          <el-menu-item v-if="isAdmin" index="/loginLogs">
            <el-icon><Tickets /></el-icon>
            <span>登录日志</span>
          </el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="app-header">
          <div class="header-info">
            <span>{{ currentUser.nickname || currentUser.username }}</span>
            <el-tag size="small" :type="isAdmin ? 'danger' : (currentUser.role === 'leader' ? 'warning' : 'info')" style="margin-left: 8px">
              {{ isAdmin ? '管理员' : (currentUser.role === 'leader' ? '组长' : '用户') }}
            </el-tag>
          </div>
          <el-button size="small" @click="showChangePwd">
            <el-icon><Lock /></el-icon>
            修改密码
          </el-button>
          <el-button type="danger" size="small" @click="handleLogout" :icon="SwitchButton">
            退出
          </el-button>
        </el-header>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
    <!-- 修改密码弹窗 -->
    <el-dialog title="修改密码" v-model="pwdDialogVisible" width="420px" append-to-body>
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="80px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handleChangePwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
  <router-view v-else />
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { SwitchButton, Download, Document, User, Lock, Tickets, UserFilled } from '@element-plus/icons-vue'
import { getUserInfo, logout as apiLogout } from './api/auth'
import { changePassword } from './api/user'

const router = useRouter()
const route = useRoute()
const currentUser = ref(null)
const isAdmin = computed(() => currentUser.value && (currentUser.value.role === 'admin' || currentUser.value.role === 'superAdmin'))
const isLeaderOrAdmin = computed(() => currentUser.value && (currentUser.value.role === 'leader' || currentUser.value.role === 'admin' || currentUser.value.role === 'superAdmin'))

const checkAuth = async () => {
  const cached = sessionStorage.getItem('currentUser')
  if (cached) {
    currentUser.value = JSON.parse(cached)
    return
  }
  try {
    const res = await getUserInfo()
    if (res.code === 200 && res.data) {
      currentUser.value = res.data
      sessionStorage.setItem('currentUser', JSON.stringify(res.data))
    }
  } catch {
    // not logged in
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出系统吗？', '退出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await apiLogout()
    currentUser.value = null
    sessionStorage.removeItem('currentUser')
    router.push('/login')
    ElMessage.success('已安全退出')
  } catch {
    // cancelled
  }
}

const pwdDialogVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const showChangePwd = () => {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
}

const handleChangePwd = async () => {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }
  pwdLoading.value = true
  try {
    const res = await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      pwdDialogVisible.value = false
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } catch (e) {
    console.error(e)
  } finally {
    pwdLoading.value = false
  }
}

window.__setCurrentUser = (user) => {
  currentUser.value = user
  sessionStorage.setItem('currentUser', JSON.stringify(user))
}

onMounted(() => {
  if (route.path !== '/login') {
    checkAuth()
  }
})

watch(() => route.path, (newPath) => {
  if (newPath !== '/login' && !currentUser.value) {
    checkAuth()
  }
})
</script>

<style scoped>
.app-aside {
  background: #304156;
  overflow: hidden;
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #263445;
}

.logo-area h2 {
  color: #fff;
  font-size: 16px;
  margin: 0;
  white-space: nowrap;
}

.app-header {
  background: #fff;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 20px;
}

.header-info {
  margin-right: 16px;
  display: flex;
  align-items: center;
}
</style>
