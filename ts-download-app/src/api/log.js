import request from './request'

export function getDownloadLogs(data) {
  return request.post('/log/list', data)
}

export function getLoginLogs(data) {
  return request.post('/loginLog/list', data)
}
