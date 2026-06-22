import { request } from '@/utils/request'
import type {
  TestCaseCreateRequest,
  TestCaseDto,
  TestCaseListRequest,
  TestCaseUpdateStatusRequest,
} from '@/types'

export function createTestCase(payload: TestCaseCreateRequest): Promise<TestCaseDto> {
  return request<TestCaseDto>({
    url: '/testcase/create',
    method: 'post',
    data: payload,
  })
}

export function listTestCases(payload: TestCaseListRequest): Promise<TestCaseDto[]> {
  return request<TestCaseDto[]>({
    url: '/testcase/list',
    method: 'post',
    data: payload,
  })
}

export function updateTestCaseStatus(payload: TestCaseUpdateStatusRequest): Promise<TestCaseDto> {
  return request<TestCaseDto>({
    url: '/testcase/update-status',
    method: 'post',
    data: payload,
  })
}
