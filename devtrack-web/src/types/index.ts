// ---- Unified API response envelope ----
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// ---- Auth ----
export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResult {
  userId: number
  username: string
  token: string
  admin: boolean
}

/**
 * 飞书授权 URL 接口返回。已配 appId 时返回 authorizeUrl;
 * 未配(dev 模式)时返回 devMode + hint。
 */
export interface FeishuAuthorizeUrlResult {
  authorizeUrl?: string
  devMode?: string
  hint?: string
}

// ---- Enums ----
export type Severity = 'BLOCKER' | 'CRITICAL' | 'MAJOR' | 'MINOR' | 'TRIVIAL'

export type Priority = 'P0' | 'P1' | 'P2' | 'P3'

export type StatusCode =
  | 'NEW'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'VERIFIED'
  | 'CLOSED'
  | 'REOPENED'
  | 'REJECTED'

export type TransitionCode =
  | 'confirm'
  | 'start'
  | 'resolve'
  | 'verify'
  | 'close'
  | 'reject'
  | 'reopen'

// ---- Defect ----
export interface DefectDto {
  id: number
  projectId: number
  displayKey: string
  title: string
  description: string
  statusCode: StatusCode
  severity: Severity
  priority: Priority
  reporterId: number | null
  assigneeId: number | null
  iterationId: number | null
  source: string | null
  createTime: number // epoch ms
  updateTime: number // epoch ms
  // Custom field values keyed by fieldKey. Optional; only present on some endpoints.
  customFields?: Record<string, any>
}

// ---- Defect list ----
export interface DefectListRequest {
  projectId: number
  statusCode?: StatusCode
  priority?: Priority
  assigneeId?: number
  keyword?: string
  pn: number
  ps: number
}

export interface DefectListResult {
  list: DefectDto[]
  pn: number
  ps: number
  total: number
}

// ---- Create defect ----
export interface DefectCreateRequest {
  projectId: number
  title: string
  description: string
  severity: Severity
  priority: Priority
  assigneeId?: number
  iterationId?: number
}

// ---- Transition ----
export interface DefectTransitionRequest {
  defectId: number
  transitionCode: TransitionCode
  comment?: string
}

// ---- Defect detail ----
export interface HistoryItem {
  fromState: string | null
  toState: string
  transitionName: string
  operatorName: string
  comment: string | null
  createTime: number // epoch ms
}

export interface CommentItem {
  id: number
  content: string
  authorName: string
  createTime: number // epoch ms
}

export interface TransitionOption {
  code: TransitionCode
  name: string
  requireComment: boolean
}

export interface DefectDetailRequest {
  defectId: number
}

export interface DefectDetailResult {
  defect: DefectDto
  history: HistoryItem[]
  comments: CommentItem[]
  availableTransitions: TransitionOption[]
}

// ---- Comment ----
export interface DefectCommentRequest {
  defectId: number
  content: string
}

// ---- Board ----
export interface DefectBoardRequest {
  projectId: number
}

export interface BoardColumn {
  statusCode: StatusCode
  statusName: string
  category: string
  count: number
  defects: DefectDto[]
}

export interface DefectBoardResult {
  columns: BoardColumn[]
}

// ---- Iteration ----
export type IterationStatus = 'PLANNING' | 'ACTIVE' | 'CLOSED'

export interface IterationDto {
  id: number
  projectId: number
  name: string
  status: IterationStatus
  startDate: string // "YYYY-MM-DD"
  endDate: string // "YYYY-MM-DD"
}

export interface IterationCreateRequest {
  projectId: number
  name: string
  startDate: string // "YYYY-MM-DD"
  endDate: string // "YYYY-MM-DD"
}

export interface IterationListRequest {
  projectId: number
}

export interface IterationCloseRequest {
  iterationId: number
}

export interface BurndownPoint {
  date: string // "YYYY-MM-DD"
  remaining: number // defects still unresolved at end of day
}

export interface BurndownRequest {
  iterationId: number
}

export interface BurndownResult {
  iterationName: string
  total: number
  points: BurndownPoint[]
}

export interface AssignIterationRequest {
  defectId: number
  iterationId: number
}

// ---- Requirement ----
export type RequirementStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE'

export interface RequirementDto {
  id: number
  projectId: number
  title: string
  description: string
  status: RequirementStatus
  priority: Priority
  createTime: number // epoch ms
}

export interface RequirementCreateRequest {
  projectId: number
  title: string
  description: string
  priority: Priority
}

export interface RequirementListRequest {
  projectId: number
}

// ---- Test case ----
export type TestCaseStatus = 'PENDING' | 'PASS' | 'FAIL' | 'BLOCKED'

export interface TestCaseDto {
  id: number
  projectId: number
  title: string
  preconditions: string
  steps: string
  expected: string
  status: TestCaseStatus
  automationKey: string | null
  createTime: number // epoch ms
  // Whether this case is part of the regression suite. Only present on some endpoints.
  regression?: boolean
}

export interface TestCaseCreateRequest {
  projectId: number
  title: string
  preconditions: string
  steps: string
  expected: string
}

export interface TestCaseListRequest {
  projectId: number
}

export interface TestCaseUpdateStatusRequest {
  testCaseId: number
  status: TestCaseStatus
}

export interface TestCaseUpdateRequest {
  testCaseId: number
  title: string
  preconditions: string
  steps: string
  expected: string
  automationKey?: string | null
}

export interface TestCaseDeleteRequest {
  testCaseId: number
}

// ---- Stats ----
export interface DefectStatsRequest {
  projectId: number
}

export interface DefectStatsResult {
  total: number
  byStatus: Record<string, number>
  byPriority: Record<string, number>
  bySeverity: Record<string, number>
}

// ---- Relations ----
export type RelationEntityType = 'DEFECT' | 'REQUIREMENT' | 'TEST_CASE'

export type RelationType =
  | 'VERIFIES'
  | 'COVERS'
  | 'CAUSED_BY'
  | 'RELATES'
  | 'BLOCKS'
  | 'DUPLICATES'

export interface RelationDto {
  id: number
  sourceType: RelationEntityType
  sourceId: number
  sourceTitle: string
  targetType: RelationEntityType
  targetId: number
  targetTitle: string
  relationType: RelationType
}

export interface RelationListRequest {
  entityType: RelationEntityType
  entityId: number
}

export interface RelationLinkRequest {
  sourceType: RelationEntityType
  sourceId: number
  targetType: RelationEntityType
  targetId: number
  relationType: RelationType
}

export interface RelationUnlinkRequest {
  relationId: number
}

// ---- Attachment ----
export interface AttachmentDto {
  id: number
  fileName: string
  sizeBytes: number
  uploaderName: string
  createTime: number // epoch ms
}

export interface AttachmentListRequest {
  defectId: number
}

// ---- Activity (operation records) ----
export type ActivityAction =
  | 'CREATE'
  | 'TRANSITION'
  | 'ASSIGN'
  | 'ASSIGN_ITERATION'
  | 'COMMENT'

export interface ActivityDto {
  action: ActivityAction | string
  detail: string
  operatorName: string
  createTime: number // epoch ms
}

export interface DefectActivityRequest {
  defectId: number
}

// ---- Batch transition ----
export interface BatchResult {
  defectId: number
  success: boolean
  message: string
}

export interface BatchTransitionRequest {
  defectIds: number[]
  transitionCode: TransitionCode
  comment?: string
}

export interface BatchTransitionResult {
  results: BatchResult[]
}

// ---- Iteration report ----
export interface IterationReportRequest {
  iterationId: number
}

export interface IterationReportResult {
  total: number
  done: number
  inProgress: number
  todo: number
  completionRate: number // 0-100
  byStatus: Record<string, number>
  byPriority: Record<string, number>
}

// ---- Notification ----
export type NotificationType = 'ASSIGN' | 'MENTION'

export interface NotificationDto {
  id: number
  type: NotificationType | string
  content: string
  refType: string
  refId: number
  read: boolean
  createTime: number // epoch ms
}

export interface NotificationListRequest {
  onlyUnread?: boolean
}

export interface NotificationUnreadCountResult {
  count: number
}

export interface NotificationMarkReadRequest {
  id: number
}

// ---- Custom fields ----
export type CustomFieldType = 'TEXT' | 'NUMBER' | 'SELECT'

export interface CustomFieldDefDto {
  id: number
  fieldKey: string
  label: string
  fieldType: CustomFieldType
  options: string[]
  required: boolean
}

export interface CustomFieldDefsRequest {
  projectId: number
}

export interface DefectSetFieldsRequest {
  defectId: number
  fields: Record<string, any>
}

// ---- AI generated cases ----
export type AiEngine = 'claude' | 'heuristic'

export interface GenCase {
  title: string
  preconditions: string
  steps: string
  expected: string
}

export interface AiGenCasesRequest {
  projectId: number
  prd: string
}

export interface AiGenCasesResult {
  engine: AiEngine
  cases: GenCase[]
  /** 业务流程图(Mermaid flowchart 文本) */
  flowchart?: string | null
  /** 测试点脑图(Mermaid mindmap 文本) */
  mindmap?: string | null
}

export interface AiSaveCasesRequest {
  projectId: number
  cases: GenCase[]
}

export interface AiSaveCasesResult {
  created: number
}

// ---- Tags ----
export interface TagDto {
  id: number
  name: string
  color: string
}

export interface TagListRequest {
  projectId: number
}

export interface TagCreateRequest {
  projectId: number
  name: string
  color: string
}

export interface DefectTagsRequest {
  defectId: number
}

export interface DefectSetTagsRequest {
  defectId: number
  tagIds: number[]
}

// ---- SLA ----
export interface SlaItem {
  defectId: number
  displayKey: string
  title: string
  priority: Priority
  statusCode: StatusCode
  overdueDays: number
  slaDays: number
}

export interface SlaOverdueRequest {
  projectId: number
}

export interface SlaOverdueResult {
  items: SlaItem[]
}

// ---- Dashboard ----
export interface DashboardOverviewRequest {
  projectId: number
}

export interface DashboardOverviewResult {
  total: number
  openCount: number
  byStatus: Record<string, number>
  byPriority: Record<string, number>
  recentActivities: ActivityDto[]
}

// ---- Test runs / regression ----
export type RunResultStatus = 'PASS' | 'FAIL' | 'BLOCKED' | 'PENDING'

export interface TestRunDto {
  id: number
  name: string
  status: string
  total: number
  passed: number
  failed: number
  blocked: number
  pending: number
}

export interface RunCase {
  testCaseId: number
  title: string
  status: RunResultStatus
}

export interface TestRunCreateRequest {
  projectId: number
  name: string
  caseIds: number[]
}

export interface TestRunListRequest {
  projectId: number
}

export interface TestRunDetailRequest {
  runId: number
}

export interface TestRunDetailResult {
  run: TestRunDto
  cases: RunCase[]
}

export interface TestRunSetResultRequest {
  runId: number
  testCaseId: number
  status: RunResultStatus
}

export interface TestRunRegressionRequest {
  projectId: number
  name: string
}

export interface TestCaseSetRegressionRequest {
  testCaseId: number
  regression: boolean
}

// ---- Coverage matrix ----
export interface CoverageCaseRef {
  testCaseId: number
  title: string
}

export interface CoverageRow {
  requirementId: number
  title: string
  status: RequirementStatus | string
  covered: boolean
  coveredBy: CoverageCaseRef[]
}

export interface CoverageMatrixRequest {
  projectId: number
}

export interface CoverageMatrixResult {
  total: number
  coveredCount: number
  coverageRate: number // 0-100
  rows: CoverageRow[]
}

// ---- Gantt ----
export type GanttItemType = 'iteration' | 'version'

export interface GanttItem {
  type: GanttItemType
  id: number
  name: string
  status: string
  startDate?: string // "YYYY-MM-DD"; version may only have endDate
  endDate?: string // "YYYY-MM-DD"
}

export interface GanttOverviewRequest {
  projectId: number
}

export interface GanttOverviewResult {
  items: GanttItem[]
}

// ---- Feishu doc reading ----
export type FeishuEngine = 'feishu' | 'stub'

export interface FeishuReadDocRequest {
  docUrl: string
}

export interface FeishuReadDocResult {
  engine: FeishuEngine
  content: string
}

// ---- Observability logs ----
export type ObservLogLevel = 'INFO' | 'WARN' | 'ERROR'

export interface ObservLog {
  id: number
  source: string
  level: ObservLogLevel | string
  message: string
  createTime: number // epoch ms
  defectId: number | null
}

export interface ObservListRequest {
  projectId: number
  keyword?: string
}

export interface ObservListResult {
  items: ObservLog[]
}

export interface ObservCreateDefectRequest {
  logId: number
}

// ---- Feishu approval ----
export type ApprovalAction = 'CLOSE' | 'REJECT'
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ApprovalDto {
  id: number
  defectId: number
  action: ApprovalAction | string
  status: ApprovalStatus | string
  applicantName: string
}

export interface ApprovalSubmitRequest {
  defectId: number
  action: ApprovalAction
}

export interface ApprovalListRequest {
  projectId: number
}

export interface ApprovalDecideRequest {
  approvalId: number
  approved: boolean
}
