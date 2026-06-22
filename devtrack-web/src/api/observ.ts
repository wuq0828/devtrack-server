import { request } from '@/utils/request'
import type {
  DefectDto,
  ObservCreateDefectRequest,
  ObservListRequest,
  ObservListResult,
} from '@/types'

export function listObservLogs(payload: ObservListRequest): Promise<ObservListResult> {
  return request<ObservListResult>({
    url: '/observ/list',
    method: 'post',
    data: payload,
  })
}

export function createDefectFromLog(payload: ObservCreateDefectRequest): Promise<DefectDto> {
  return request<DefectDto>({
    url: '/observ/create-defect',
    method: 'post',
    data: payload,
  })
}
