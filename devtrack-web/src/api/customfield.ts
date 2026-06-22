import { request } from '@/utils/request'
import type {
  CustomFieldDefDto,
  CustomFieldDefsRequest,
  DefectDto,
  DefectSetFieldsRequest,
} from '@/types'

export function getCustomFieldDefs(
  payload: CustomFieldDefsRequest,
): Promise<CustomFieldDefDto[]> {
  return request<CustomFieldDefDto[]>({
    url: '/customfield/defs',
    method: 'post',
    data: payload,
  })
}

export function setDefectFields(payload: DefectSetFieldsRequest): Promise<DefectDto> {
  return request<DefectDto>({
    url: '/defect/set-fields',
    method: 'post',
    data: payload,
  })
}
