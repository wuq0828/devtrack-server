import service, { request } from '@/utils/request'
import type { AttachmentDto, AttachmentListRequest } from '@/types'

export function listAttachments(payload: AttachmentListRequest): Promise<AttachmentDto[]> {
  return request<AttachmentDto[]>({
    url: '/attachment/list',
    method: 'post',
    data: payload,
  })
}

// Download an attachment as a blob. The shared axios instance attaches the
// Authorization header via its request interceptor; the response interceptor
// passes binary responses through untouched (no envelope `code` field).
export function downloadAttachment(id: number): Promise<Blob> {
  return service
    .get(`/attachment/${id}/download`, { responseType: 'blob' })
    .then((res) => res.data as Blob)
}
