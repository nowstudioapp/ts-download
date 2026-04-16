<template>
  <div class="log-page">
    <h3 class="page-title">登录日志</h3>

    <el-form :model="queryForm" inline class="filter-form">
      <el-form-item label="用户名">
        <el-input v-model="queryForm.username" placeholder="筛选用户名" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 340px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchLogs">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="logList" v-loading="loading" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="ip" label="登录IP" width="160" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createTime" label="登录时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-area">
      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :page-sizes="[20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchLogs"
        @current-change="fetchLogs"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getLoginLogs } from '../api/log'

const loading = ref(false)
const logList = ref([])
const total = ref(0)
const dateRange = ref(null)

const queryForm = reactive({
  username: '',
  startTime: '',
  endTime: '',
  page: 1,
  pageSize: 20
})

const fetchLogs = async () => {
  loading.value = true
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      queryForm.startTime = dateRange.value[0]
      queryForm.endTime = dateRange.value[1]
    } else {
      queryForm.startTime = ''
      queryForm.endTime = ''
    }
    const res = await getLoginLogs(queryForm)
    if (res.code === 200 && res.data) {
      logList.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryForm.username = ''
  queryForm.startTime = ''
  queryForm.endTime = ''
  queryForm.page = 1
  dateRange.value = null
  fetchLogs()
}

const formatDate = (val) => {
  if (!val) return ''
  return new Date(val).toLocaleString('zh-CN')
}

onMounted(() => fetchLogs())
</script>

<style scoped>
.log-page {
  padding: 0;
}

.page-title {
  margin: 0 0 20px 0;
  font-size: 20px;
  color: #303133;
}

.filter-form {
  margin-bottom: 16px;
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
}

.pagination-area {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
