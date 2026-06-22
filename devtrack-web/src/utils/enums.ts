import type {
  ActivityAction,
  ApprovalAction,
  ApprovalStatus,
  IterationStatus,
  NotificationType,
  ObservLogLevel,
  Priority,
  RelationEntityType,
  RelationType,
  RequirementStatus,
  RunResultStatus,
  Severity,
  StatusCode,
  TestCaseStatus,
  TransitionCode,
} from '@/types'

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

// ---- Iteration status ----
export const ITERATION_STATUS_LABEL: Record<IterationStatus, string> = {
  PLANNING: '规划中',
  ACTIVE: '进行中',
  CLOSED: '已关闭',
}

export const ITERATION_STATUS_TAG_TYPE: Record<IterationStatus, TagType> = {
  PLANNING: 'info',
  ACTIVE: 'primary',
  CLOSED: 'success',
}

// ---- Status ----
export const STATUS_LABEL: Record<StatusCode, string> = {
  NEW: '新建',
  CONFIRMED: '已确认',
  IN_PROGRESS: '处理中',
  RESOLVED: '已解决',
  VERIFIED: '已验证',
  CLOSED: '已关闭',
  REOPENED: '重新打开',
  REJECTED: '已拒绝',
}

export const STATUS_TAG_TYPE: Record<StatusCode, TagType> = {
  NEW: 'info',
  CONFIRMED: 'primary',
  IN_PROGRESS: 'warning',
  RESOLVED: 'success',
  VERIFIED: 'success',
  CLOSED: 'info',
  REOPENED: 'danger',
  REJECTED: 'danger',
}

export const STATUS_OPTIONS: { label: string; value: StatusCode }[] = (
  Object.keys(STATUS_LABEL) as StatusCode[]
).map((value) => ({ label: STATUS_LABEL[value], value }))

// ---- Severity ----
export const SEVERITY_LABEL: Record<Severity, string> = {
  BLOCKER: '阻塞',
  CRITICAL: '严重',
  MAJOR: '主要',
  MINOR: '次要',
  TRIVIAL: '轻微',
}

export const SEVERITY_TAG_TYPE: Record<Severity, TagType> = {
  BLOCKER: 'danger',
  CRITICAL: 'danger',
  MAJOR: 'warning',
  MINOR: 'info',
  TRIVIAL: 'info',
}

export const SEVERITY_OPTIONS: { label: string; value: Severity }[] = (
  Object.keys(SEVERITY_LABEL) as Severity[]
).map((value) => ({ label: SEVERITY_LABEL[value], value }))

// ---- Priority ----
export const PRIORITY_LABEL: Record<Priority, string> = {
  P0: 'P0 最高',
  P1: 'P1 高',
  P2: 'P2 中',
  P3: 'P3 低',
}

export const PRIORITY_TAG_TYPE: Record<Priority, TagType> = {
  P0: 'danger',
  P1: 'warning',
  P2: 'primary',
  P3: 'info',
}

export const PRIORITY_OPTIONS: { label: string; value: Priority }[] = (
  Object.keys(PRIORITY_LABEL) as Priority[]
).map((value) => ({ label: PRIORITY_LABEL[value], value }))

// ---- Requirement status ----
export const REQUIREMENT_STATUS_LABEL: Record<RequirementStatus, string> = {
  PENDING: '待处理',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
}

export const REQUIREMENT_STATUS_TAG_TYPE: Record<RequirementStatus, TagType> = {
  PENDING: 'info',
  IN_PROGRESS: 'warning',
  DONE: 'success',
}

// ---- Test case status ----
export const TESTCASE_STATUS_LABEL: Record<TestCaseStatus, string> = {
  PENDING: '待执行',
  PASS: '通过',
  FAIL: '失败',
  BLOCKED: '阻塞',
}

export const TESTCASE_STATUS_TAG_TYPE: Record<TestCaseStatus, TagType> = {
  PENDING: 'info',
  PASS: 'success',
  FAIL: 'danger',
  BLOCKED: 'warning',
}

export const TESTCASE_STATUS_OPTIONS: { label: string; value: TestCaseStatus }[] = (
  Object.keys(TESTCASE_STATUS_LABEL) as TestCaseStatus[]
).map((value) => ({ label: TESTCASE_STATUS_LABEL[value], value }))

// ---- Relations ----
export const RELATION_TYPE_LABEL: Record<RelationType, string> = {
  VERIFIES: '验证',
  COVERS: '覆盖',
  CAUSED_BY: '源于',
  RELATES: '相关',
  BLOCKS: '阻塞',
  DUPLICATES: '重复',
}

export const RELATION_ENTITY_TYPE_LABEL: Record<RelationEntityType, string> = {
  DEFECT: '缺陷',
  REQUIREMENT: '需求',
  TEST_CASE: '用例',
}

export const RELATION_ENTITY_TYPE_TAG_TYPE: Record<RelationEntityType, TagType> = {
  DEFECT: 'danger',
  REQUIREMENT: 'primary',
  TEST_CASE: 'success',
}

// Options for the "add relation" dialog: relation type semantics.
export interface RelationTypeOption {
  // The relation type value sent to the backend.
  value: RelationType
  // Human label shown in the select.
  label: string
  // The target entity type this relation links the defect to.
  targetType: RelationEntityType
}

export const DEFECT_RELATION_TYPE_OPTIONS: RelationTypeOption[] = [
  { value: 'VERIFIES', label: '用例验证缺陷', targetType: 'TEST_CASE' },
  { value: 'CAUSED_BY', label: '缺陷源于需求', targetType: 'REQUIREMENT' },
  { value: 'RELATES', label: '相关', targetType: 'REQUIREMENT' },
]

// ---- Transitions ----
export interface TransitionAction {
  code: TransitionCode
  label: string
  // Element button type for visual emphasis
  type: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  // Whether a comment is required (and therefore must prompt the user)
  requireComment: boolean
}

const CONFIRM: TransitionAction = { code: 'confirm', label: '确认', type: 'primary', requireComment: false }
const START: TransitionAction = { code: 'start', label: '开始处理', type: 'primary', requireComment: false }
const RESOLVE: TransitionAction = { code: 'resolve', label: '解决', type: 'success', requireComment: false }
const VERIFY: TransitionAction = { code: 'verify', label: '验证通过', type: 'success', requireComment: false }
const CLOSE: TransitionAction = { code: 'close', label: '关闭', type: 'info', requireComment: false }
const REJECT: TransitionAction = { code: 'reject', label: '拒绝', type: 'danger', requireComment: true }
const REOPEN: TransitionAction = { code: 'reopen', label: '重新打开', type: 'warning', requireComment: true }

// Available transitions keyed by current status.
export const STATUS_TRANSITIONS: Record<StatusCode, TransitionAction[]> = {
  NEW: [CONFIRM, REJECT],
  CONFIRMED: [START, REJECT],
  IN_PROGRESS: [RESOLVE, REJECT],
  RESOLVED: [VERIFY, REOPEN, REJECT],
  VERIFIED: [CLOSE, REJECT],
  REOPENED: [CONFIRM, REJECT],
  REJECTED: [REOPEN],
  CLOSED: [],
}

export function getTransitions(status: StatusCode): TransitionAction[] {
  return STATUS_TRANSITIONS[status] ?? []
}

// Button visual type keyed by transition code. Used when the backend returns
// available transitions (code/name/requireComment) without a button style.
export const TRANSITION_BUTTON_TYPE: Record<TransitionCode, TransitionAction['type']> = {
  confirm: 'primary',
  start: 'primary',
  resolve: 'success',
  verify: 'success',
  close: 'info',
  reject: 'danger',
  reopen: 'warning',
}

export function transitionButtonType(code: TransitionCode): TransitionAction['type'] {
  return TRANSITION_BUTTON_TYPE[code] ?? 'primary'
}

// Chinese label keyed by transition code (reuses the labels above).
export const TRANSITION_LABEL: Record<TransitionCode, string> = {
  confirm: CONFIRM.label,
  start: START.label,
  resolve: RESOLVE.label,
  verify: VERIFY.label,
  close: CLOSE.label,
  reject: REJECT.label,
  reopen: REOPEN.label,
}

// Options for the batch-transition action dropdown.
export const BATCH_TRANSITION_OPTIONS: { code: TransitionCode; label: string }[] = (
  ['confirm', 'start', 'resolve', 'verify', 'close', 'reject', 'reopen'] as TransitionCode[]
).map((code) => ({ code, label: TRANSITION_LABEL[code] }))

// ---- Activity action labels ----
export const ACTIVITY_ACTION_LABEL: Record<ActivityAction, string> = {
  CREATE: '创建',
  TRANSITION: '状态流转',
  ASSIGN: '指派',
  ASSIGN_ITERATION: '归入迭代',
  COMMENT: '评论',
}

export function activityActionLabel(action: string): string {
  return ACTIVITY_ACTION_LABEL[action as ActivityAction] ?? action
}

// ---- Notification type labels ----
export const NOTIFICATION_TYPE_LABEL: Record<NotificationType, string> = {
  ASSIGN: '指派给你',
  MENTION: '有人@你',
}

export const NOTIFICATION_TYPE_TAG_TYPE: Record<NotificationType, TagType> = {
  ASSIGN: 'primary',
  MENTION: 'warning',
}

export function notificationTypeLabel(type: string): string {
  return NOTIFICATION_TYPE_LABEL[type as NotificationType] ?? type
}

export function notificationTypeTagType(type: string): TagType {
  return NOTIFICATION_TYPE_TAG_TYPE[type as NotificationType] ?? 'info'
}

// ---- Test run result status (per-case execution result) ----
export const RUN_RESULT_STATUS_LABEL: Record<RunResultStatus, string> = {
  PENDING: '待执行',
  PASS: '通过',
  FAIL: '失败',
  BLOCKED: '阻塞',
}

export const RUN_RESULT_STATUS_TAG_TYPE: Record<RunResultStatus, TagType> = {
  PENDING: 'info',
  PASS: 'success',
  FAIL: 'danger',
  BLOCKED: 'warning',
}

export function runResultStatusLabel(status: string): string {
  return RUN_RESULT_STATUS_LABEL[status as RunResultStatus] ?? status
}

export function runResultStatusTagType(status: string): TagType {
  return RUN_RESULT_STATUS_TAG_TYPE[status as RunResultStatus] ?? 'info'
}

// ---- Observability log level ----
export const OBSERV_LEVEL_TAG_TYPE: Record<ObservLogLevel, TagType> = {
  INFO: 'info',
  WARN: 'warning',
  ERROR: 'danger',
}

export function observLevelTagType(level: string): TagType {
  return OBSERV_LEVEL_TAG_TYPE[level as ObservLogLevel] ?? 'info'
}

// ---- Approval ----
export const APPROVAL_ACTION_LABEL: Record<ApprovalAction, string> = {
  CLOSE: '关闭缺陷',
  REJECT: '拒绝缺陷',
}

export const APPROVAL_STATUS_LABEL: Record<ApprovalStatus, string> = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回',
}

export const APPROVAL_STATUS_TAG_TYPE: Record<ApprovalStatus, TagType> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
}

export function approvalActionLabel(action: string): string {
  return APPROVAL_ACTION_LABEL[action as ApprovalAction] ?? action
}

export function approvalStatusLabel(status: string): string {
  return APPROVAL_STATUS_LABEL[status as ApprovalStatus] ?? status
}

export function approvalStatusTagType(status: string): TagType {
  return APPROVAL_STATUS_TAG_TYPE[status as ApprovalStatus] ?? 'info'
}

// ---- File size format ----
export function formatFileSize(bytes: number): string {
  if (bytes == null || bytes < 0) return '-'
  if (bytes < 1024) return `${bytes} B`
  const kb = bytes / 1024
  if (kb < 1024) return `${kb.toFixed(1)} KB`
  const mb = kb / 1024
  if (mb < 1024) return `${mb.toFixed(1)} MB`
  return `${(mb / 1024).toFixed(1)} GB`
}

// ---- Time format ----
export function formatTime(epochMs: number): string {
  if (!epochMs) return '-'
  const d = new Date(epochMs)
  const pad = (n: number) => String(n).padStart(2, '0')
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}
