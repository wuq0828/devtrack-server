import { request } from '@/utils/request'
import type { GanttOverviewRequest, GanttOverviewResult } from '@/types'

export function getGanttOverview(payload: GanttOverviewRequest): Promise<GanttOverviewResult> {
  return request<GanttOverviewResult>({
    url: '/gantt/overview',
    method: 'post',
    data: payload,
  })
}
