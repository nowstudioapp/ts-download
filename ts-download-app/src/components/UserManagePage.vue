<template>
  <div class="user-manage-page">
    <div class="page-title">
      <h3>用户管理</h3>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        添加用户
      </el-button>
    </div>

    <el-table :data="userList" v-loading="loading" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="nickname" label="昵称" width="150" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'admin' ? 'danger' : 'info'">
            {{ row.role === 'admin' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="200">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button 
            size="small" 
            :type="row.status === 1 ? 'warning' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="info" @click="showResetPwdDialog(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)" :disabled="row.role === 'admin'">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑用户弹窗 -->
    <el-dialog :title="dialogMode === 'add' ? '添加用户' : '编辑用户'" v-model="dialogVisible" width="500px">
      <el-form :model="dialogForm" :rules="dialogRules" ref="dialogFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username" v-if="dialogMode === 'add'">
          <el-input v-model="dialogForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="dialogForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item :label="dialogMode === 'add' ? '密码' : '新密码'" prop="password">
          <el-input v-model="dialogForm.password" type="password" show-password
            :placeholder="dialogMode === 'add' ? '请输入密码' : '留空则不修改'" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="dialogForm.role">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    <!-- 重置密码弹窗 -->
    <el-dialog title="重置密码" v-model="resetPwdVisible" width="420px">
      <el-form :model="resetPwdForm" :rules="resetPwdRules" ref="resetPwdFormRef" label-width="80px">
        <el-form-item label="用户">
          <el-input :model-value="resetPwdForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetPwdForm.password" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="resetPwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="handleResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getUserList, addUser, updateUser, deleteUser, resetPassword } from '../api/user'

const loading = ref(false)
const userList = ref([])
const dialogVisible = ref(false)
const dialogMode = ref('add')
const submitLoading = ref(false)
const dialogFormRef = ref()

const dialogForm = reactive({
  id: null,
  username: '',
  nickname: '',
  password: '',
  role: 'user'
})

const dialogRules = computed(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: dialogMode.value === 'add', message: '请输入密码', trigger: 'blur' }]
}))

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getUserList()
    if (res.code === 200) {
      userList.value = res.data || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  dialogMode.value = 'add'
  dialogForm.id = null
  dialogForm.username = ''
  dialogForm.nickname = ''
  dialogForm.password = ''
  dialogForm.role = 'user'
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  dialogMode.value = 'edit'
  dialogForm.id = row.id
  dialogForm.username = row.username
  dialogForm.nickname = row.nickname
  dialogForm.password = ''
  dialogForm.role = row.role
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (dialogFormRef.value) {
    try {
      await dialogFormRef.value.validate()
    } catch {
      return
    }
  }
  
  submitLoading.value = true
  try {
    if (dialogMode.value === 'add') {
      const res = await addUser(dialogForm)
      if (res.code === 200) {
        ElMessage.success('添加成功')
        dialogVisible.value = false
        fetchList()
      } else {
        ElMessage.error(res.msg)
      }
    } else {
      const data = { id: dialogForm.id, nickname: dialogForm.nickname, role: dialogForm.role }
      if (dialogForm.password) data.password = dialogForm.password
      const res = await updateUser(data)
      if (res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        fetchList()
      } else {
        ElMessage.error(res.msg)
      }
    }
  } finally {
    submitLoading.value = false
  }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}用户 "${row.username}" 吗？`, '确认', { type: 'warning' })
    const res = await updateUser({ id: row.id, status: newStatus })
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      fetchList()
    } else {
      ElMessage.error(res.msg)
    }
  } catch { /* cancelled */ }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除用户 "${row.username}" 吗？此操作不可恢复`, '警告', {
      type: 'warning',
      confirmButtonText: '确定删除',
      confirmButtonClass: 'el-button--danger'
    })
    const res = await deleteUser(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchList()
    } else {
      ElMessage.error(res.msg)
    }
  } catch { /* cancelled */ }
}

const resetPwdVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdFormRef = ref()
const resetPwdForm = reactive({
  id: null,
  username: '',
  password: '',
  confirmPassword: ''
})
const resetPwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetPwdForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const showResetPwdDialog = (row) => {
  resetPwdForm.id = row.id
  resetPwdForm.username = row.username
  resetPwdForm.password = ''
  resetPwdForm.confirmPassword = ''
  resetPwdVisible.value = true
}

const handleResetPwd = async () => {
  if (!resetPwdFormRef.value) return
  try {
    await resetPwdFormRef.value.validate()
  } catch {
    return
  }
  resetPwdLoading.value = true
  try {
    const res = await resetPassword({ id: resetPwdForm.id, password: resetPwdForm.password })
    if (res.code === 200) {
      ElMessage.success('密码重置成功')
      resetPwdVisible.value = false
    } else {
      ElMessage.error(res.msg || '重置失败')
    }
  } catch (e) {
    console.error(e)
  } finally {
    resetPwdLoading.value = false
  }
}

const formatDate = (val) => {
  if (!val) return ''
  const d = new Date(val)
  return d.toLocaleString('zh-CN')
}

onMounted(() => fetchList())
</script>

<style scoped>
.user-manage-page {
  padding: 0;
}

.page-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title h3 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}
</style>
