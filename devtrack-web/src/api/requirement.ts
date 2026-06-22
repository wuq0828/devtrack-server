import { request } from '@/utils/request'
import type {
  RequirementCreateRequest,
  RequirementDto,
  RequirementListRequest,
} from '@/types'

export function createRequirement(payload: RequirementCreateRequest): Promise<RequirementDto> {
  return request<RequirementDto>({
    url: '/requirement/create',
    method: 'post',
    data: payload,
  })
}

export function listRequirements(payload: RequirementListRequest): Promise<RequirementDto[]> {
  return request<RequirementDto[]>({
    url: '/requirement/list',
    method: 'post',
    data: payload,
  })
}
