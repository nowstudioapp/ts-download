<template>
  <div class="valid-user-page">
    <div class="page-header">
      <el-icon><UserFilled /></el-icon>
      <span>有效用户下载</span>
    </div>

    <el-form :model="form" label-width="120px" class="form-container">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="用户类型" required>
            <el-select v-model="form.type" placeholder="请选择类型">
              <el-option label="Telegram (TG)" value="tg" />
              <el-option label="WhatsApp (WS)" value="ws" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="国家" required>
            <el-select
              v-model="form.countryCode"
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

      <el-form-item>
        <el-button type="primary" :loading="countLoading" @click="handleCount">
          <el-icon><Search /></el-icon>
          查询数量
        </el-button>
        <el-button type="warning" :loading="downloadLoading" @click="handleDownload">
          <el-icon><Download /></el-icon>
          下载TXT
        </el-button>
      </el-form-item>
    </el-form>

    <div v-if="countResult !== null" class="result-section">
      <div class="result-title">查询结果</div>
      <div class="result-content">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="类型">
            <el-tag>{{ form.type === 'tg' ? 'Telegram' : 'WhatsApp' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="国家">{{ form.countryCode }}</el-descriptions-item>
          <el-descriptions-item label="有效用户数">
            <span class="count-number">{{ countResult.toLocaleString() }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <div v-if="downloadResult" class="result-section">
      <div class="result-title">下载结果</div>
      <div class="result-content">
        <el-alert title="文件生成成功" type="success" show-icon />
        <div class="download-link">
          <el-input :model-value="getFullUrl(downloadResult)" readonly class="link-input">
            <template #append>
              <el-button @click="copyToClipboard(downloadResult)">复制</el-button>
            </template>
          </el-input>
          <el-button type="success" @click="openFile(downloadResult)" class="download-btn">
            <el-icon><Download /></el-icon>
            立即下载
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download, UserFilled } from '@element-plus/icons-vue'
import { CountryConstants, PopularCountries } from '../constants/CountryConstants'
import { downloadValidUsers, countValidUsers } from '../api/validUser'

const form = reactive({
  type: 'tg',
  countryCode: 'US'
})

const countLoading = ref(false)
const downloadLoading = ref(false)
const countResult = ref(null)
const downloadResult = ref(null)

const validate = () => {
  if (!form.type || !form.countryCode) {
    ElMessage.warning('请选择用户类型和国家')
    return false
  }
  return true
}

const handleCount = async () => {
  if (!validate()) return
  countLoading.value = true
  countResult.value = null
  try {
    const res = await countValidUsers({ type: form.type, countryCode: form.countryCode })
    if (res.code === 200) {
      countResult.value = res.data.count
      ElMessage.success('查询成功')
    } else {
      ElMessage.error(res.msg || '查询失败')
    }
  } catch (e) {
    console.error('查询失败:', e)
  } finally {
    countLoading.value = false
  }
}

const handleDownload = async () => {
  if (!validate()) return
  downloadLoading.value = true
  downloadResult.value = null
  try {
    const res = await downloadValidUsers({ type: form.type, countryCode: form.countryCode })
    if (res.code === 200) {
      downloadResult.value = res.data
      ElMessage.success(res.msg || '导出成功')
    } else {
      ElMessage.error(res.msg || '下载失败')
    }
  } catch (e) {
    console.error('下载失败:', e)
  } finally {
    downloadLoading.value = false
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

const openFile = (url) => {
  window.open(getFullUrl(url), '_blank')
}
</script>

<style scoped>
.valid-user-page {
  width: 100%;
}

.page-header {
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

.count-number {
  font-size: 18px;
  font-weight: 700;
  color: #409EFF;
}

.download-link {
  margin-top: 15px;
}

.link-input {
  margin-bottom: 10px;
}

.download-btn {
  width: 100%;
}

:deep(.el-select) {
  width: 100%;
}
</style>
