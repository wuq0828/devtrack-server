import { request } from '@/utils/request'
import type { LoginRequest, LoginResult } from '@/types'

export function login(payload: LoginRequest): Promise<LoginResult> {
  return request<LoginResult>({
    url: '/auth/login',
    method: 'post',
    data: payload,
  })
}
