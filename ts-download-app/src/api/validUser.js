import request from './request'

export function downloadValidUsers(data) {
  return request.post('/validUser/download', data)
}

export function countValidUsers(data) {
  return request.post('/validUser/count', data)
}
