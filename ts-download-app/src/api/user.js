import request from './request'

export function getUserList() {
  return request.get('/user/list')
}

export function addUser(data) {
  return request.post('/user/add', data)
}

export function updateUser(data) {
  return request.post('/user/update', data)
}

export function deleteUser(id) {
  return request.post('/user/delete?id=' + id)
}

export function changePassword(data) {
  return request.post('/user/changePassword', data)
}

export function resetPassword(data) {
  return request.post('/user/resetPassword', data)
}
