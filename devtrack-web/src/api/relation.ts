import { request } from '@/utils/request'
import type {
  RelationDto,
  RelationLinkRequest,
  RelationListRequest,
  RelationUnlinkRequest,
} from '@/types'

export function listRelations(payload: RelationListRequest): Promise<RelationDto[]> {
  return request<RelationDto[]>({
    url: '/relation/list',
    method: 'post',
    data: payload,
  })
}

export function linkRelation(payload: RelationLinkRequest): Promise<RelationDto> {
  return request<RelationDto>({
    url: '/relation/link',
    method: 'post',
    data: payload,
  })
}

export function unlinkRelation(payload: RelationUnlinkRequest): Promise<null> {
  return request<null>({
    url: '/relation/unlink',
    method: 'post',
    data: payload,
  })
}
