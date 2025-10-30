import request from './request'

/**
 * 查询接口
 * @param {Object} params - 查询参数
 * @param {string} params.countryCode - 国家代码
 * @param {string} params.downloadType - 下载类型 (txt/excel)
 * @param {number} params.limit - 限制数量
 * @param {string} params.taskType - 任务类型
 */
export const queryDownload = (params) => {
  return request({
    url: '/download/query',
    method: 'post',
    data: params
  })
}

/**
 * 单类型下载接口
 * @param {Object} params - 下载参数
 * @param {string} params.countryCode - 国家代码
 * @param {string} params.downloadType - 下载类型 (txt/excel)
 * @param {number} params.limit - 限制数量
 * @param {string} params.taskType - 任务类型
 */
export const generateDownloadUrl = (params) => {
  return request({
    url: '/download/generateDownloadUrl',
    method: 'post',
    data: params
  })
}

/**
 * 多条件下载接口
 * @param {Object} params - 下载参数
 * @param {string} params.countryCode - 国家代码
 * @param {number} params.excludeSkin - 排除皮肤
 * @param {string} params.firstTaskType - 第一任务类型
 * @param {number} params.limit - 限制数量
 * @param {number} params.maxAge - 最大年龄
 * @param {number} params.minAge - 最小年龄
 * @param {string} params.secondTaskType - 第二任务类型
 * @param {number} params.sex - 性别
 */
export const mergeDownload = (params) => {
  return request({
    url: '/download/mergeDownload',
    method: 'post',
    data: params
  })
}
