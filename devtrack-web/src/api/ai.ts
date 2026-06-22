import { request } from '@/utils/request'
import type {
  AiGenCasesRequest,
  AiGenCasesResult,
  AiSaveCasesRequest,
  AiSaveCasesResult,
  FeishuReadDocRequest,
  FeishuReadDocResult,
} from '@/types'

export function genCases(payload: AiGenCasesRequest): Promise<AiGenCasesResult> {
  return request<AiGenCasesResult>({
    url: '/ai/gen-cases',
    method: 'post',
    data: payload,
  })
}

export function saveCases(payload: AiSaveCasesRequest): Promise<AiSaveCasesResult> {
  return request<AiSaveCasesResult>({
    url: '/ai/save-cases',
    method: 'post',
    data: payload,
  })
}

export function readFeishuDoc(payload: FeishuReadDocRequest): Promise<FeishuReadDocResult> {
  return request<FeishuReadDocResult>({
    url: '/feishu/read-doc',
    method: 'post',
    data: payload,
  })
}
