<template>
  <div class="download-page">
    <!-- 查询功能 -->
    <div class="section-card">
      <div class="section-title">
        <el-icon><Search /></el-icon>
        <span>数据查询</span>
      </div>
      
      <el-form :model="queryForm" label-width="100px">
        <div class="form-row">
          <el-form-item label="国家代码">
            <el-input v-model="queryForm.countryCode" placeholder="例如: US" clearable />
          </el-form-item>
          
          <el-form-item label="下载类型">
            <el-select v-model="queryForm.downloadType" placeholder="请选择下载类型" clearable>
              <el-option label="TXT格式" value="txt" />
              <el-option label="Excel格式" value="excel" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="限制数量">
            <el-input-number 
              v-model="queryForm.limit" 
              :min="1" 
              :max="100000"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          
          <el-form-item label="任务类型">
            <el-select v-model="queryForm.taskType" placeholder="请选择任务类型" clearable>
              <el-option label="WS验证" value="wsValid" />
              <el-option label="iOS验证" value="iosValid" />
              <el-option label="RCS验证" value="rcsValid" />
              <el-option label="性别识别" value="gender" />
            </el-select>
          </el-form-item>
        </div>
        
        <div class="button-group">
          <el-button @click="resetQueryForm">重置</el-button>
          <el-button 
            type="primary" 
            :loading="queryLoading"
            @click="handleQuery"
          >
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </div>
      </el-form>
      
      <div v-if="queryResult" class="result-section">
        <div class="result-title">查询结果</div>
        <div class="result-content">
          <pre>{{ JSON.stringify(queryResult, null, 2) }}</pre>
        </div>
      </div>
    </div>

    <!-- 单类型下载 -->
    <div class="section-card">
      <div class="section-title">
        <el-icon><Download /></el-icon>
        <span>单类型下载</span>
      </div>
      
      <el-form :model="singleForm" label-width="100px">
        <div class="form-row">
          <el-form-item label="国家代码">
            <el-input v-model="singleForm.countryCode" placeholder="例如: US" clearable />
          </el-form-item>
          
          <el-form-item label="下载类型">
            <el-select v-model="singleForm.downloadType" placeholder="请选择下载类型" clearable>
              <el-option label="TXT格式" value="txt" />
              <el-option label="Excel格式" value="excel" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="限制数量">
            <el-input-number 
              v-model="singleForm.limit" 
              :min="1" 
              :max="100000"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          
          <el-form-item label="任务类型">
            <el-select v-model="singleForm.taskType" placeholder="请选择任务类型" clearable>
              <el-option label="WS验证" value="wsValid" />
              <el-option label="iOS验证" value="iosValid" />
              <el-option label="RCS验证" value="rcsValid" />
              <el-option label="性别识别" value="gender" />
            </el-select>
          </el-form-item>
        </div>
        
        <div class="button-group">
          <el-button @click="resetSingleForm">重置</el-button>
          <el-button 
            type="success" 
            :loading="singleLoading"
            @click="handleSingleDownload"
          >
            <el-icon><Download /></el-icon>
            生成下载链接
          </el-button>
        </div>
      </el-form>
      
      <div v-if="singleResult" class="result-section">
        <div class="result-title">下载结果</div>
        <div class="result-content">
          <pre>{{ JSON.stringify(singleResult, null, 2) }}</pre>
        </div>
      </div>
    </div>

    <!-- 多条件下载 -->
    <div class="section-card">
      <div class="section-title">
        <el-icon><FolderOpened /></el-icon>
        <span>多条件组合下载</span>
      </div>
      
      <el-form :model="mergeForm" label-width="120px">
        <div class="form-row">
          <el-form-item label="国家代码">
            <el-input v-model="mergeForm.countryCode" placeholder="例如: US" clearable />
          </el-form-item>
          
          <el-form-item label="第一任务类型">
            <el-select v-model="mergeForm.firstTaskType" placeholder="请选择" clearable>
              <el-option label="性别识别" value="gender" />
              <el-option label="WS验证" value="wsValid" />
              <el-option label="iOS验证" value="iosValid" />
              <el-option label="RCS验证" value="rcsValid" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="第二任务类型">
            <el-select v-model="mergeForm.secondTaskType" placeholder="请选择" clearable>
              <el-option label="RCS验证" value="rcsValid" />
              <el-option label="WS验证" value="wsValid" />
              <el-option label="iOS验证" value="iosValid" />
              <el-option label="性别识别" value="gender" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="限制数量">
            <el-input-number 
              v-model="mergeForm.limit" 
              :min="1" 
              :max="100000"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          
          <el-form-item label="最小年龄">
            <el-input-number 
              v-model="mergeForm.minAge" 
              :min="1" 
              :max="150"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          
          <el-form-item label="最大年龄">
            <el-input-number 
              v-model="mergeForm.maxAge" 
              :min="1" 
              :max="150"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          
          <el-form-item label="性别">
            <el-select v-model="mergeForm.sex" placeholder="请选择性别" clearable>
              <el-option label="男" :value="1" />
              <el-option label="女" :value="2" />
              <el-option label="未知" :value="0" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="排除皮肤">
            <el-select v-model="mergeForm.excludeSkin" placeholder="请选择" clearable>
              <el-option label="是" :value="1" />
              <el-option label="否" :value="2" />
            </el-select>
          </el-form-item>
        </div>
        
        <div class="button-group">
          <el-button @click="resetMergeForm">重置</el-button>
          <el-button 
            type="warning" 
            :loading="mergeLoading"
            @click="handleMergeDownload"
          >
            <el-icon><FolderOpened /></el-icon>
            组合下载
          </el-button>
        </div>
      </el-form>
      
      <div v-if="mergeResult" class="result-section">
        <div class="result-title">下载结果</div>
        <div class="result-content">
          <pre>{{ JSON.stringify(mergeResult, null, 2) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download, FolderOpened } from '@element-plus/icons-vue'
import { queryDownload, generateDownloadUrl, mergeDownload } from '../api/download'

// 查询表单
const queryForm = reactive({
  countryCode: 'US',
  downloadType: 'txt',
  limit: 1000,
  taskType: 'wsValid'
})

const queryLoading = ref(false)
const queryResult = ref(null)

// 单类型下载表单
const singleForm = reactive({
  countryCode: 'US',
  downloadType: 'excel',
  limit: 1000,
  taskType: 'iosValid'
})

const singleLoading = ref(false)
const singleResult = ref(null)

// 多条件下载表单
const mergeForm = reactive({
  countryCode: 'US',
  excludeSkin: 2,
  firstTaskType: 'gender',
  limit: 20000,
  maxAge: 71,
  minAge: 34,
  secondTaskType: 'rcsValid',
  sex: 1
})

const mergeLoading = ref(false)
const mergeResult = ref(null)

// 查询处理
const handleQuery = async () => {
  if (!queryForm.countryCode || !queryForm.downloadType || !queryForm.taskType) {
    ElMessage.warning('请填写完整的查询条件')
    return
  }
  
  queryLoading.value = true
  queryResult.value = null
  
  try {
    const result = await queryDownload(queryForm)
    queryResult.value = result
    ElMessage.success('查询成功')
  } catch (error) {
    console.error('查询失败:', error)
  } finally {
    queryLoading.value = false
  }
}

// 单类型下载处理
const handleSingleDownload = async () => {
  if (!singleForm.countryCode || !singleForm.downloadType || !singleForm.taskType) {
    ElMessage.warning('请填写完整的下载参数')
    return
  }
  
  singleLoading.value = true
  singleResult.value = null
  
  try {
    const result = await generateDownloadUrl(singleForm)
    singleResult.value = result
    ElMessage.success('下载链接生成成功')
  } catch (error) {
    console.error('下载失败:', error)
  } finally {
    singleLoading.value = false
  }
}

// 多条件下载处理
const handleMergeDownload = async () => {
  if (!mergeForm.countryCode || !mergeForm.firstTaskType || !mergeForm.secondTaskType) {
    ElMessage.warning('请填写完整的下载参数')
    return
  }
  
  if (mergeForm.minAge > mergeForm.maxAge) {
    ElMessage.warning('最小年龄不能大于最大年龄')
    return
  }
  
  mergeLoading.value = true
  mergeResult.value = null
  
  try {
    const result = await mergeDownload(mergeForm)
    mergeResult.value = result
    ElMessage.success('组合下载成功')
  } catch (error) {
    console.error('下载失败:', error)
  } finally {
    mergeLoading.value = false
  }
}

// 重置表单
const resetQueryForm = () => {
  queryForm.countryCode = 'US'
  queryForm.downloadType = 'txt'
  queryForm.limit = 1000
  queryForm.taskType = 'wsValid'
  queryResult.value = null
}

const resetSingleForm = () => {
  singleForm.countryCode = 'US'
  singleForm.downloadType = 'excel'
  singleForm.limit = 1000
  singleForm.taskType = 'iosValid'
  singleResult.value = null
}

const resetMergeForm = () => {
  mergeForm.countryCode = 'US'
  mergeForm.excludeSkin = 2
  mergeForm.firstTaskType = 'gender'
  mergeForm.limit = 20000
  mergeForm.maxAge = 71
  mergeForm.minAge = 34
  mergeForm.secondTaskType = 'rcsValid'
  mergeForm.sex = 1
  mergeResult.value = null
}
</script>

<style scoped>
.download-page {
  width: 100%;
}
</style>
