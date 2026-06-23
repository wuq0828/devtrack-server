import { request } from '@/utils/request'

export interface UserBrief {
  userId: number
  name: string
}

/** 全部用户(用于处理人下拉与 ID→名字 展示)。 */
export function listUsers(): Promise<UserBrief[]> {
  return request<UserBrief[]>({
    url: '/user/list',
    method: 'post',
    data: {},
  })
}
