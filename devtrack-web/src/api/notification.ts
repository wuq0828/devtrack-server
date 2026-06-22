import { request } from '@/utils/request'
import type {
  NotificationDto,
  NotificationListRequest,
  NotificationMarkReadRequest,
  NotificationUnreadCountResult,
} from '@/types'

export function listNotifications(
  payload: NotificationListRequest = {},
): Promise<NotificationDto[]> {
  return request<NotificationDto[]>({
    url: '/notification/list',
    method: 'post',
    data: payload,
  })
}

export function getUnreadCount(): Promise<NotificationUnreadCountResult> {
  return request<NotificationUnreadCountResult>({
    url: '/notification/unread-count',
    method: 'post',
    data: {},
  })
}

export function markNotificationRead(payload: NotificationMarkReadRequest): Promise<null> {
  return request<null>({
    url: '/notification/mark-read',
    method: 'post',
    data: payload,
  })
}

export function markAllNotificationsRead(): Promise<null> {
  return request<null>({
    url: '/notification/mark-all-read',
    method: 'post',
    data: {},
  })
}
