import request from '../utils/request'

// 认证接口
export const login = (data) => request.post('/auth/login', data)

// 用户接口
export const getMe = () => request.get('/user/me')
export const logout = () => request.post('/user/logout')
