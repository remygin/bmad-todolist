import {
  ApiError,
  apiRequest,
  clearToken,
  getStoredToken,
  storeToken,
} from './client'

export { ApiError, clearToken, getStoredToken, storeToken }

export type MeResponse = {
  id: number
  username: string
  roles: string[]
}

export type LoginResponse = {
  accessToken: string
  tokenType: string
  user: MeResponse
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
    authenticated: false,
  })
}

export async function fetchMe(token: string): Promise<MeResponse> {
  return apiRequest<MeResponse>('/auth/me', {
    token,
  })
}
