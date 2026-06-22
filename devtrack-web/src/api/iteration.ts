import { request } from '@/utils/request'
import type {
  AssignIterationRequest,
  BurndownRequest,
  BurndownResult,
  DefectDto,
  IterationCloseRequest,
  IterationCreateRequest,
  IterationDto,
  IterationListRequest,
  IterationReportRequest,
  IterationReportResult,
} from '@/types'

export function createIteration(payload: IterationCreateRequest): Promise<IterationDto> {
  return request<IterationDto>({
    url: '/iteration/create',
    method: 'post',
    data: payload,
  })
}

export function listIterations(payload: IterationListRequest): Promise<IterationDto[]> {
  return request<IterationDto[]>({
    url: '/iteration/list',
    method: 'post',
    data: payload,
  })
}

export function closeIteration(payload: IterationCloseRequest): Promise<IterationDto> {
  return request<IterationDto>({
    url: '/iteration/close',
    method: 'post',
    data: payload,
  })
}

export function getBurndown(payload: BurndownRequest): Promise<BurndownResult> {
  return request<BurndownResult>({
    url: '/iteration/burndown',
    method: 'post',
    data: payload,
  })
}

export function assignDefectIteration(payload: AssignIterationRequest): Promise<DefectDto> {
  return request<DefectDto>({
    url: '/defect/assign-iteration',
    method: 'post',
    data: payload,
  })
}

export function getIterationReport(payload: IterationReportRequest): Promise<IterationReportResult> {
  return request<IterationReportResult>({
    url: '/iteration/report',
    method: 'post',
    data: payload,
  })
}
