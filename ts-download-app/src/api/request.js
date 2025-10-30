import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 0, // 不设置超时时间,因为下载可能很慢
  withCredentials: true // 允许携带cookie
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 可以在这里添加token等认证信息
    // const token = localStorage.getItem('token')
    // if (token) {
    //   config.headers['Authorization'] = `Bearer ${token}`
    // }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('响应错误:', error)
    
    let message = '请求失败'
    if (error.response) {
      switch (error.response.status) {
        case 401:
          message = '未授权,请重新登录'
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求错误,未找到该资源'
          break
        case 500:
          message = '服务器错误'
          break
        default:
          message = error.response.data?.message || '请求失败'
      }
    } else if (error.request) {
      message = '网络错误,请检查网络连接'
    }
    
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
