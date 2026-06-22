import { request } from '@/utils/request'
import type { FeishuAuthorizeUrlResult, LoginRequest, LoginResult } from '@/types'

export function login(payload: LoginRequest): Promise<LoginResult> {
  return request<LoginResult>({
    url: '/auth/login',
    method: 'post',
    data: payload,
  })
}

/** 取飞书扫码授权 URL;dev 模式(后端未配 appId)时返回 devMode 提示。 */
export function getFeishuAuthorizeUrl(): Promise<FeishuAuthorizeUrlResult> {
  return request<FeishuAuthorizeUrlResult>({
    url: '/auth/feishu/authorize-url',
    method: 'get',
  })
}

/** 用飞书回调拿到的 code 换取登录态(code 作为 query 参数)。 */
export function feishuLogin(code: string): Promise<LoginResult> {
  return request<LoginResult>({
    url: '/auth/feishu/login',
    method: 'post',
    params: { code },
  })
}
