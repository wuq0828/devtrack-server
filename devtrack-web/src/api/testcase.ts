import { request } from '@/utils/request'
import type {
  TestCaseCreateRequest,
  TestCaseDeleteRequest,
  TestCaseDto,
  TestCaseListRequest,
  TestCaseSetRegressionRequest,
  TestCaseUpdateRequest,
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

export function updateTestCase(payload: TestCaseUpdateRequest): Promise<TestCaseDto> {
  return request<TestCaseDto>({
    url: '/testcase/update',
    method: 'post',
    data: payload,
  })
}

export function setTestCaseRegression(
  payload: TestCaseSetRegressionRequest,
): Promise<TestCaseDto> {
  return request<TestCaseDto>({
    url: '/testcase/set-regression',
    method: 'post',
    data: payload,
  })
}

export function deleteTestCase(payload: TestCaseDeleteRequest): Promise<void> {
  return request<void>({
    url: '/testcase/delete',
    method: 'post',
    data: payload,
  })
}
