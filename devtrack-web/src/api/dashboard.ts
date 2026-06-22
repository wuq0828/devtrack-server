import { request } from '@/utils/request'
import type { DashboardOverviewRequest, DashboardOverviewResult } from '@/types'

export function getDashboardOverview(
  payload: DashboardOverviewRequest,
): Promise<DashboardOverviewResult> {
  return request<DashboardOverviewResult>({
    url: '/dashboard/overview',
    method: 'post',
    data: payload,
  })
}
