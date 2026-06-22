import { request } from '@/utils/request'
import type {
  TestRunCreateRequest,
  TestRunDetailRequest,
  TestRunDetailResult,
  TestRunDto,
  TestRunListRequest,
  TestRunRegressionRequest,
  TestRunSetResultRequest,
} from '@/types'

export function createTestRun(payload: TestRunCreateRequest): Promise<TestRunDto> {
  return request<TestRunDto>({
    url: '/testrun/create',
    method: 'post',
    data: payload,
  })
}

export function listTestRuns(payload: TestRunListRequest): Promise<TestRunDto[]> {
  return request<TestRunDto[]>({
    url: '/testrun/list',
    method: 'post',
    data: payload,
  })
}

export function getTestRunDetail(payload: TestRunDetailRequest): Promise<TestRunDetailResult> {
  return request<TestRunDetailResult>({
    url: '/testrun/detail',
    method: 'post',
    data: payload,
  })
}

export function setTestRunResult(payload: TestRunSetResultRequest): Promise<TestRunDto> {
  return request<TestRunDto>({
    url: '/testrun/set-result',
    method: 'post',
    data: payload,
  })
}

export function createRegressionRun(payload: TestRunRegressionRequest): Promise<TestRunDto> {
  return request<TestRunDto>({
    url: '/testrun/regression',
    method: 'post',
    data: payload,
  })
}
