import { request } from '@/utils/request'
import type {
  ActivityDto,
  BatchTransitionRequest,
  BatchTransitionResult,
  CommentItem,
  DefectActivityRequest,
  DefectBoardRequest,
  DefectBoardResult,
  DefectCommentRequest,
  DefectCreateRequest,
  DefectDetailRequest,
  DefectDetailResult,
  DefectDto,
  DefectListRequest,
  DefectListResult,
  DefectStatsRequest,
  DefectStatsResult,
  DefectTransitionRequest,
} from '@/types'

export function listDefects(payload: DefectListRequest): Promise<DefectListResult> {
  return request<DefectListResult>({
    url: '/defect/list',
    method: 'post',
    data: payload,
  })
}

export function createDefect(payload: DefectCreateRequest): Promise<DefectDto> {
  return request<DefectDto>({
    url: '/defect/create',
    method: 'post',
    data: payload,
  })
}

export function transitionDefect(payload: DefectTransitionRequest): Promise<DefectDto> {
  return request<DefectDto>({
    url: '/defect/transition',
    method: 'post',
    data: payload,
  })
}

export function getDefectDetail(payload: DefectDetailRequest): Promise<DefectDetailResult> {
  return request<DefectDetailResult>({
    url: '/defect/detail',
    method: 'post',
    data: payload,
  })
}

export function addDefectComment(payload: DefectCommentRequest): Promise<CommentItem> {
  return request<CommentItem>({
    url: '/defect/comment',
    method: 'post',
    data: payload,
  })
}

export function getDefectBoard(payload: DefectBoardRequest): Promise<DefectBoardResult> {
  return request<DefectBoardResult>({
    url: '/defect/board',
    method: 'post',
    data: payload,
  })
}

export function getDefectStats(payload: DefectStatsRequest): Promise<DefectStatsResult> {
  return request<DefectStatsResult>({
    url: '/defect/stats',
    method: 'post',
    data: payload,
  })
}

export function getDefectActivity(payload: DefectActivityRequest): Promise<ActivityDto[]> {
  return request<ActivityDto[]>({
    url: '/defect/activity',
    method: 'post',
    data: payload,
  })
}

export function batchTransitionDefects(
  payload: BatchTransitionRequest,
): Promise<BatchTransitionResult> {
  return request<BatchTransitionResult>({
    url: '/defect/batch-transition',
    method: 'post',
    data: payload,
  })
}
