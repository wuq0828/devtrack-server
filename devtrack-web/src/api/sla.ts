import { request } from '@/utils/request'
import type { SlaOverdueRequest, SlaOverdueResult } from '@/types'

export function getSlaOverdue(payload: SlaOverdueRequest): Promise<SlaOverdueResult> {
  return request<SlaOverdueResult>({
    url: '/sla/overdue',
    method: 'post',
    data: payload,
  })
}
