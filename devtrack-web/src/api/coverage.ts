import { request } from '@/utils/request'
import type { CoverageMatrixRequest, CoverageMatrixResult } from '@/types'

export function getCoverageMatrix(payload: CoverageMatrixRequest): Promise<CoverageMatrixResult> {
  return request<CoverageMatrixResult>({
    url: '/coverage/matrix',
    method: 'post',
    data: payload,
  })
}
