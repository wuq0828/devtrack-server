import { request } from '@/utils/request'
import type {
  ApprovalDecideRequest,
  ApprovalDto,
  ApprovalListRequest,
  ApprovalSubmitRequest,
} from '@/types'

export function submitApproval(payload: ApprovalSubmitRequest): Promise<ApprovalDto> {
  return request<ApprovalDto>({
    url: '/approval/submit',
    method: 'post',
    data: payload,
  })
}

export function listApprovals(payload: ApprovalListRequest): Promise<ApprovalDto[]> {
  return request<ApprovalDto[]>({
    url: '/approval/list',
    method: 'post',
    data: payload,
  })
}

export function decideApproval(payload: ApprovalDecideRequest): Promise<ApprovalDto> {
  return request<ApprovalDto>({
    url: '/approval/decide',
    method: 'post',
    data: payload,
  })
}
