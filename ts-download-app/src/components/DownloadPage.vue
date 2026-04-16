<template>
  <div class="download-page">
    <el-tabs v-model="activeTab" type="border-card" class="demo-tabs">
      <!-- 合并下载 -->
      <el-tab-pane label="合并下载" name="mergeDownload">
        <div class="tab-content">
          <div class="section-title">
            <el-icon><FolderOpened /></el-icon>
            <span>合并下载（支持分批下载）</span>
          </div>
          
          <el-form :model="mergeForm" label-width="120px" class="form-container">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="导出类型" required>
                  <el-select v-model="mergeForm.downloadType" placeholder="请选择导出类型">
                    <el-option label="Excel文件" value="excel" />
                    <el-option label="TXT文件（只含手机号）" value="txt" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="国家代码" required>
                  <el-select 
                    v-model="mergeForm.countryCode" 
                    placeholder="请选择国家"
                    filterable
                    clearable
                  >
                    <el-option-group label="常用国家">
                      <el-option
                        v-for="country in PopularCountries"
                        :key="country.code"
                        :label="`${country.name} (${country.code})`"
                        :value="country.code"
                      />
                    </el-option-group>
                    <el-option-group label="全部国家">
                      <el-option
                        v-for="country in CountryConstants"
                        :key="country.code"
                        :label="`${country.name} (${country.code})`"
                        :value="country.code"
                      />
                    </el-option-group>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="第一任务类型">
                  <el-select 
                    v-model="mergeForm.firstTaskType" 
                    placeholder="请选择第一任务类型（默认gender）"
                    filterable
                    clearable
                  >
                    <el-option-group label="常用任务">
                      <el-option
                        v-for="task in PopularTaskTypes"
                        :key="task.value"
                        :label="task.label"
                        :value="task.value"
                      />
                    </el-option-group>
                    <el-option-group 
                      v-for="category in taskCategories" 
                      :key="category.name"
                      :label="category.label"
                    >
                      <el-option
                        v-for="task in category.tasks"
                        :key="task.value"
                        :label="task.label"
                        :value="task.value"
                      />
                    </el-option-group>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="第二任务类型">
                  <el-select 
                    v-model="mergeForm.secondTaskType" 
                    placeholder="可选，不选则只使用第一任务类型"
                    filterable
                    clearable
                  >
                    <el-option-group label="常用任务">
                      <el-option
                        v-for="task in PopularTaskTypes"
                        :key="task.value"
                        :label="task.label"
                        :value="task.value"
                      />
                    </el-option-group>
                    <el-option-group 
                      v-for="category in taskCategories" 
                      :key="category.name"
                      :label="category.label"
                    >
                      <el-option
                        v-for="task in category.tasks"
                        :key="task.value"
                        :label="task.label"
                        :value="task.value"
                      />
                    </el-option-group>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="导出数量">
                  <el-input-number 
                    v-model="mergeForm.limit" 
                    :min="0" 
                    :max="1000000"
                    :placeholder="mergeForm.downloadType === 'txt' ? '不填导出全部' : '默认10000'"
                    style="width: 100%"
                  />
                  <div class="form-item-tip" v-if="mergeForm.downloadType === 'txt'">
                    TXT导出不填则导出全部（已去重手机号）
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="跳过数量">
                  <el-input-number 
                    v-model="mergeForm.skip" 
                    :min="0"
                    placeholder="分批下载时使用"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="检查用户名">
                  <el-select v-model="mergeForm.checkUserNameEmpty" placeholder="可选" clearable>
                    <el-option label="查询用户名为空的" :value="0" />
                    <el-option label="查询用户名不为空的" :value="1" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="最小年龄">
                  <el-input-number v-model="mergeForm.minAge" :min="1" :max="150" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最大年龄">
                  <el-input-number v-model="mergeForm.maxAge" :min="1" :max="150" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="性别">
                  <el-select v-model="mergeForm.sex" placeholder="可选" clearable>
                    <el-option label="女" :value="0" />
                    <el-option label="男" :value="1" />
                    <el-option label="未知" :value="-1" />
                    <el-option label="无图片" :value="-2" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="排除肤色">
                  <el-select v-model="mergeForm.excludeSkin" placeholder="可选（支持多选）" clearable multiple collapse-tags>
                    <el-option label="排除黄色皮肤" :value="0" />
                    <el-option label="排除棕色皮肤" :value="1" />
                    <el-option label="排除黑色皮肤" :value="2" />
                    <el-option label="排除白色皮肤" :value="3" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="指定肤色">
                  <el-select v-model="mergeForm.includeSkin" placeholder="可选（支持多选）" clearable multiple collapse-tags>
                    <el-option label="黄色皮肤" :value="0" />
                    <el-option label="棕色皮肤" :value="1" />
                    <el-option label="黑色皮肤" :value="2" />
                    <el-option label="白色皮肤" :value="3" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="有效天数">
                  <el-input-number 
                    v-model="mergeForm.activeDay" 
                    :min="0" 
                    :max="9999"
                    :value-on-clear="null"
                    placeholder="可选"
                    style="width: 100%"
                  />
                  <div class="form-item-tip">
                    不填则不筛选，填N表示筛选active_day在0~N范围内
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item>
              <el-button @click="resetMergeForm">重置</el-button>
              <el-button 
                type="warning" 
                :loading="mergeLoading"
                @click="handleMergeDownload"
              >
                <el-icon><FolderOpened /></el-icon>
                合并下载
              </el-button>
              <el-button 
                type="info" 
                :loading="batchLoading"
                @click="handleBatchDownload"
              >
                <el-icon><Download /></el-icon>
                分批下载
              </el-button>
            </el-form-item>
          </el-form>
          
          <div v-if="mergeResult" class="result-section">
            <div class="result-title">下载结果</div>
            <div class="result-content">
              <el-alert 
                v-if="mergeResult.code === 200"
                :title="mergeResult.msg"
                type="success"
                show-icon
              />
              <div class="download-link" v-if="mergeResult.data">
                <el-input :model-value="getFullUrl(mergeResult.data)" readonly class="link-input">
                  <template #append>
                    <el-button @click="copyToClipboard(mergeResult.data)">复制</el-button>
                  </template>
                </el-input>
                <el-button type="success" @click="downloadFile(mergeResult.data)" class="download-btn">
                  <el-icon><Download /></el-icon>
                  立即下载
                </el-button>
              </div>
            </div>
          </div>
          
          <div v-if="batchProgress.show" class="batch-progress">
            <div class="progress-title">分批下载进度</div>
            <el-progress :percentage="batchProgress.percentage" :status="batchProgress.status" :stroke-width="20" />
            <div class="progress-info">
              <p>当前批次: {{ batchProgress.current }} / {{ batchProgress.total }}</p>
              <p>已下载: {{ batchProgress.downloaded.toLocaleString() }} 条记录</p>
            </div>
            <div class="batch-files" v-if="batchProgress.files.length > 0">
              <div class="files-title">已生成文件:</div>
              <div v-for="(file, index) in batchProgress.files" :key="index" class="file-item">
                <el-link :href="getFullUrl(file)" target="_blank" type="primary">
                  批次 {{ index + 1 }}: {{ file.split('/').pop() }}
                </el-link>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 查询任务记录数量 -->
      <el-tab-pane label="查询记录数量" name="queryCount">
        <div class="tab-content">
          <div class="section-title">
            <el-icon><Search /></el-icon>
            <span>查询任务记录数量</span>
          </div>
          
          <el-form :model="countForm" label-width="120px" class="form-container">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="任务类型" required>
                  <el-select v-model="countForm.taskType" placeholder="请选择任务类型" filterable clearable>
                    <el-option-group label="常用任务">
                      <el-option v-for="task in PopularTaskTypes" :key="task.value" :label="task.label" :value="task.value" />
                    </el-option-group>
                    <el-option-group v-for="category in taskCategories" :key="category.name" :label="category.label">
                      <el-option v-for="task in category.tasks" :key="task.value" :label="task.label" :value="task.value" />
                    </el-option-group>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="国家代码" required>
                  <el-select v-model="countForm.countryCode" placeholder="请选择国家" filterable clearable>
                    <el-option-group label="常用国家">
                      <el-option v-for="country in PopularCountries" :key="country.code" :label="`${country.name} (${country.code})`" :value="country.code" />
                    </el-option-group>
                    <el-option-group label="全部国家">
                      <el-option v-for="country in CountryConstants" :key="country.code" :label="`${country.name} (${country.code})`" :value="country.code" />
                    </el-option-group>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="最小年龄">
                  <el-input-number v-model="countForm.minAge" :min="1" :max="150" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最大年龄">
                  <el-input-number v-model="countForm.maxAge" :min="1" :max="150" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="性别">
                  <el-select v-model="countForm.sex" placeholder="可选" clearable>
                    <el-option label="女" :value="0" />
                    <el-option label="男" :value="1" />
                    <el-option label="未知" :value="-1" />
                    <el-option label="无图片" :value="-2" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="排除肤色">
                  <el-select v-model="countForm.excludeSkin" placeholder="可选（支持多选）" clearable multiple collapse-tags>
                    <el-option label="排除黄色皮肤" :value="0" />
                    <el-option label="排除棕色皮肤" :value="1" />
                    <el-option label="排除黑色皮肤" :value="2" />
                    <el-option label="排除白色皮肤" :value="3" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="指定肤色">
                  <el-select v-model="countForm.includeSkin" placeholder="可选（支持多选）" clearable multiple collapse-tags>
                    <el-option label="黄色皮肤" :value="0" />
                    <el-option label="棕色皮肤" :value="1" />
                    <el-option label="黑色皮肤" :value="2" />
                    <el-option label="白色皮肤" :value="3" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="检查用户名">
                  <el-select v-model="countForm.checkUserNameEmpty" placeholder="可选" clearable>
                    <el-option label="查询用户名为空的" :value="0" />
                    <el-option label="查询用户名不为空的" :value="1" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="有效天数">
                  <el-input-number 
                    v-model="countForm.activeDay" 
                    :min="0" :max="9999" :value-on-clear="null"
                    placeholder="可选" style="width: 100%"
                  />
                  <div class="form-item-tip">不填则不筛选，填N表示筛选active_day在0~N范围内</div>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item>
              <el-button @click="resetCountForm">重置</el-button>
              <el-button type="primary" :loading="countLoading" @click="handleQueryCount">
                <el-icon><Search /></el-icon>
                查询数量
              </el-button>
            </el-form-item>
          </el-form>
          
          <div v-if="countResult" class="result-section">
            <div class="result-title">查询结果</div>
            <div class="result-content">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="任务类型">{{ countResult.data?.taskType }}</el-descriptions-item>
                <el-descriptions-item label="国家代码">{{ countResult.data?.countryCode }}</el-descriptions-item>
                <el-descriptions-item label="总记录数">{{ countResult.data?.totalCount?.toLocaleString() }}</el-descriptions-item>
                <el-descriptions-item label="有效记录数">{{ countResult.data?.validCount?.toLocaleString() }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download, FolderOpened } from '@element-plus/icons-vue'
import { CountryConstants, PopularCountries } from '../constants/CountryConstants'
import { TaskTypeOptions, PopularTaskTypes } from '../constants/TaskTypeConstants'
import { mergeDownload as apiMergeDownload, queryTaskCount as apiQueryTaskCount } from '../api/download'

const activeTab = ref('mergeDownload')

const mergeForm = reactive({
  countryCode: 'US',
  downloadType: 'excel',
  firstTaskType: 'gender',
  secondTaskType: null,
  limit: null,
  skip: 0,
  minAge: null,
  maxAge: null,
  sex: null,
  excludeSkin: [],
  includeSkin: [],
  checkUserNameEmpty: null,
  activeDay: null
})

const mergeLoading = ref(false)
const mergeResult = ref(null)
const batchLoading = ref(false)

const batchProgress = reactive({
  show: false,
  percentage: 0,
  status: '',
  current: 0,
  total: 0,
  downloaded: 0,
  files: []
})

const countForm = reactive({
  taskType: 'gender',
  countryCode: 'US',
  minAge: null,
  maxAge: null,
  sex: null,
  excludeSkin: [],
  includeSkin: [],
  checkUserNameEmpty: null,
  activeDay: null
})

const countLoading = ref(false)
const countResult = ref(null)

const taskCategories = [
  { name: 'whatsapp', label: 'WhatsApp相关', tasks: TaskTypeOptions.filter(t => t.category === 'whatsapp') },
  { name: 'ios', label: 'iOS相关', tasks: TaskTypeOptions.filter(t => t.category === 'ios') },
  { name: 'telegram', label: 'Telegram相关', tasks: TaskTypeOptions.filter(t => t.category === 'telegram') },
  { name: 'facebook', label: 'Facebook相关', tasks: TaskTypeOptions.filter(t => t.category === 'facebook') },
  { name: 'social', label: '社交媒体', tasks: TaskTypeOptions.filter(t => t.category === 'social') },
  { name: 'ecommerce', label: '电商平台', tasks: TaskTypeOptions.filter(t => t.category === 'ecommerce') },
  { name: 'finance', label: '金融交易', tasks: TaskTypeOptions.filter(t => t.category === 'finance') },
  { name: 'carrier', label: '运营商检测', tasks: TaskTypeOptions.filter(t => t.category === 'carrier') },
  { name: 'other', label: '其他服务', tasks: TaskTypeOptions.filter(t => t.category === 'other') }
]

const buildRequestData = (formData) => {
  return Object.fromEntries(
    Object.entries(formData).filter(([_, value]) => {
      if (typeof value === 'number') return true
      if (Array.isArray(value)) return value.length > 0
      return value !== null && value !== undefined && value !== ''
    })
  )
}

const handleMergeDownload = async () => {
  if (!mergeForm.firstTaskType || !mergeForm.countryCode) {
    ElMessage.warning('请填写第一任务类型和国家代码')
    return
  }
  if (mergeForm.minAge && mergeForm.maxAge && mergeForm.minAge > mergeForm.maxAge) {
    ElMessage.warning('最小年龄不能大于最大年龄')
    return
  }
  
  mergeLoading.value = true
  mergeResult.value = null
  
  try {
    const result = await apiMergeDownload(buildRequestData(mergeForm))
    mergeResult.value = result
    if (result.code === 200) {
      ElMessage.success(result.msg || '合并文件生成成功')
    } else {
      ElMessage.error(result.msg || '合并下载失败')
    }
  } catch (error) {
    console.error('合并下载失败:', error)
  } finally {
    mergeLoading.value = false
  }
}

const handleBatchDownload = async () => {
  if (!mergeForm.firstTaskType || !mergeForm.countryCode || !mergeForm.limit || mergeForm.limit <= 0) {
    ElMessage.warning('分批下载需要填写：第一任务类型、国家代码和导出数量（大于0）')
    return
  }
  
  batchLoading.value = true
  batchProgress.show = true
  batchProgress.percentage = 0
  batchProgress.status = 'active'
  batchProgress.current = 0
  batchProgress.downloaded = 0
  batchProgress.files = []
  
  const batchSize = mergeForm.limit
  let skip = 0
  let batchNumber = 1
  const totalBatches = 5
  batchProgress.total = totalBatches
  
  try {
    while (batchNumber <= totalBatches) {
      batchProgress.current = batchNumber
      const requestData = { ...buildRequestData(mergeForm), skip, limit: batchSize }
      const result = await apiMergeDownload(requestData)
      
      if (result.code === 200 && result.data) {
        batchProgress.files.push(result.data)
        batchProgress.downloaded += batchSize
        batchProgress.percentage = Math.round((batchNumber / totalBatches) * 100)
        ElMessage.success(`第${batchNumber}批下载成功`)
        skip += batchSize
        batchNumber++
        await new Promise(resolve => setTimeout(resolve, 1000))
      } else {
        ElMessage.warning(`第${batchNumber}批下载完成或出错: ${result.msg}`)
        break
      }
    }
    batchProgress.status = 'success'
    ElMessage.success('分批下载完成')
  } catch (error) {
    console.error('分批下载失败:', error)
    batchProgress.status = 'exception'
  } finally {
    batchLoading.value = false
  }
}

const handleQueryCount = async () => {
  if (!countForm.taskType || !countForm.countryCode) {
    ElMessage.warning('请填写完整的必填参数')
    return
  }
  
  countLoading.value = true
  countResult.value = null
  
  try {
    const result = await apiQueryTaskCount(buildRequestData(countForm))
    countResult.value = result
    if (result.code === 200) {
      ElMessage.success(result.msg || '查询成功')
    } else {
      ElMessage.error(result.msg || '查询失败')
    }
  } catch (error) {
    console.error('查询记录数量失败:', error)
  } finally {
    countLoading.value = false
  }
}

const getFullUrl = (path) => {
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  return window.location.origin + path
}

const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(getFullUrl(text))
    ElMessage.success('链接已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

const downloadFile = (url) => { window.open(url, '_blank') }

const resetMergeForm = () => {
  mergeForm.countryCode = 'US'
  mergeForm.downloadType = 'excel'
  mergeForm.firstTaskType = 'gender'
  mergeForm.secondTaskType = null
  mergeForm.limit = null
  mergeForm.skip = 0
  mergeForm.minAge = null
  mergeForm.maxAge = null
  mergeForm.sex = null
  mergeForm.excludeSkin = []
  mergeForm.includeSkin = []
  mergeForm.checkUserNameEmpty = null
  mergeForm.activeDay = null
  mergeResult.value = null
  batchProgress.show = false
}

const resetCountForm = () => {
  countForm.taskType = 'gender'
  countForm.countryCode = 'US'
  countForm.minAge = null
  countForm.maxAge = null
  countForm.sex = null
  countForm.excludeSkin = []
  countForm.includeSkin = []
  countForm.checkUserNameEmpty = null
  countForm.activeDay = null
  countResult.value = null
}
</script>

<style scoped>
.download-page {
  width: 100%;
}

.tab-content {
  padding: 20px 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #303133;
}

.form-container {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.result-section {
  margin-top: 20px;
  padding: 20px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.result-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 15px;
  color: #303133;
}

.result-content {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 6px;
}

.download-link { margin-top: 15px; }
.link-input { margin-bottom: 10px; }
.download-btn { width: 100%; }

.form-item-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

.batch-progress {
  margin-top: 20px;
  padding: 20px;
  background: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
}

.progress-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 15px;
  color: #1e40af;
}

.progress-info {
  margin-top: 10px;
  font-size: 14px;
  color: #6b7280;
}

.progress-info p { margin: 5px 0; }

.batch-files { margin-top: 15px; }
.files-title { font-weight: 600; margin-bottom: 10px; color: #374151; }

.file-item {
  margin: 8px 0;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
}

:deep(.el-select) { width: 100%; }
</style>
