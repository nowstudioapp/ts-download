import request from './request'

export const mergeDownload = (params) => {
  return request({
    url: '/download/mergeDownload',
    method: 'post',
    data: params
  })
}

export const queryTaskCount = (params) => {
  return request({
    url: '/download/queryTaskCount',
    method: 'post',
    data: params
  })
}
