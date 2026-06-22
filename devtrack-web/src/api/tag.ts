import { request } from '@/utils/request'
import type {
  DefectSetTagsRequest,
  DefectTagsRequest,
  TagCreateRequest,
  TagDto,
  TagListRequest,
} from '@/types'

export function listTags(payload: TagListRequest): Promise<TagDto[]> {
  return request<TagDto[]>({
    url: '/tag/list',
    method: 'post',
    data: payload,
  })
}

export function createTag(payload: TagCreateRequest): Promise<TagDto> {
  return request<TagDto>({
    url: '/tag/create',
    method: 'post',
    data: payload,
  })
}

export function getDefectTags(payload: DefectTagsRequest): Promise<TagDto[]> {
  return request<TagDto[]>({
    url: '/defect/tags',
    method: 'post',
    data: payload,
  })
}

export function setDefectTags(payload: DefectSetTagsRequest): Promise<TagDto[]> {
  return request<TagDto[]>({
    url: '/defect/set-tags',
    method: 'post',
    data: payload,
  })
}
