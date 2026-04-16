import axios from 'axios'
import { ElMessage } from 'element-plus'
import { fetchPublicKey, encryptRequest, decryptResponse, clearPublicKey } from '../utils/crypto'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 0,
  withCredentials: true
})

let cryptoEnabled = false

fetchPublicKey().then((key) => {
  cryptoEnabled = !!key
}).catch(() => {
  console.warn('获取公钥失败，API 加密未启用')
})

request.interceptors.request.use(
  async config => {
    if (cryptoEnabled && config.method !== 'get' && config.data) {
      if (!config._originalData) {
        config._originalData = config.data
      }
      try {
        await fetchPublicKey()
        const result = encryptRequest(config._originalData)
        if (result) {
          config._aesKeyBase64 = result.aesKeyBase64
          config._ivBase64 = result.iv
          config.headers['X-Encrypted'] = 'true'
          config.headers['X-Encrypted-Key'] = result.encryptedKey
          config.headers['X-Encrypted-IV'] = result.iv
          config.headers['Content-Type'] = 'text/plain'
          config.data = result.encryptedData
        }
      } catch (e) {
        console.warn('请求加密失败，使用明文', e)
      }
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const config = response.config
    if (config._aesKeyBase64 && typeof response.data === 'string') {
      try {
        const decrypted = decryptResponse(response.data, config._aesKeyBase64, config._ivBase64)
        return JSON.parse(decrypted)
      } catch (e) {
        console.warn('响应解密失败', e)
        return Promise.reject(new Error('响应解密失败'))
      }
    }
    return response.data
  },
  async error => {
    if (error.response) {
      const { status, data } = error.response
      const config = error.config

      if (status === 400 && data?.msg === '加解密失败' && !config._retried) {
        config._retried = true
        clearPublicKey()
        await fetchPublicKey(true)
        cryptoEnabled = true
        delete config.headers['X-Encrypted']
        delete config.headers['X-Encrypted-Key']
        delete config.headers['X-Encrypted-IV']
        config.data = config._originalData
        return request(config)
      }

      let message = '请求失败'
      switch (status) {
        case 401:
          message = '未登录或登录已过期'
          router.push('/login')
          break
        case 403:
          message = '权限不足'
          break
        case 404:
          message = '请求的资源不存在'
          break
        case 500:
          message = '服务器错误'
          break
        default:
          message = data?.msg || '请求失败'
      }
      ElMessage.error(message)
    } else if (error.request) {
      ElMessage.error('网络错误,请检查网络连接')
    } else {
      ElMessage.error('请求失败')
    }
    return Promise.reject(error)
  }
)

export default request
