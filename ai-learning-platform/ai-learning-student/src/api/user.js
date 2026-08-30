import request from '../utils/request'

// 认证接口
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)

// 用户接口
export const getMe = () => request.get('/user/me')
export const updateProfile = (data) => request.put('/user/profile', data)
export const changePassword = (data) => request.put('/user/password', data)
export const logout = () => request.post('/user/logout')
