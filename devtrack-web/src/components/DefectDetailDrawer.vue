<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="640px"
    direction="rtl"
    @closed="handleClosed"
  >
    <div v-loading="loading" class="drawer-body">
      <template v-if="detail">
        <!-- Header summary -->
        <section class="section header-section">
          <div class="defect-title">
            <span class="display-key">{{ detail.defect.displayKey }}</span>
            <span class="title-text">{{ detail.defect.title }}</span>
          </div>
          <div class="tag-row">
            <el-tag :type="STATUS_TAG_TYPE[detail.defect.statusCode]" effect="light">
              {{ STATUS_LABEL[detail.defect.statusCode] }}
            </el-tag>
            <el-tag :type="SEVERITY_TAG_TYPE[detail.defect.severity]" effect="plain">
              {{ SEVERITY_LABEL[detail.defect.severity] }}
            </el-tag>
            <el-tag :type="PRIORITY_TAG_TYPE[detail.defect.priority]" effect="dark">
              {{ detail.defect.priority }}
            </el-tag>
          </div>
          <p v-if="detail.defect.description" class="description">
            {{ detail.defect.description }}
          </p>

          <!-- Tags -->
          <div v-loading="tagsLoading" class="defect-tags">
            <el-tag
              v-for="tag in defectTags"
              :key="tag.id"
              :color="tag.color"
              class="defect-tag"
              effect="dark"
              closable
              :disable-transitions="false"
              @close="handleRemoveTag(tag)"
            >
              {{ tag.name }}
            </el-tag>

            <el-popover
              v-model:visible="tagPopoverVisible"
              placement="bottom-start"
              :width="280"
              trigger="click"
              @show="handleTagPopoverShow"
            >
              <template #reference>
                <el-button size="small" type="primary" plain :icon="Plus" round>
                  标签
                </el-button>
              </template>

              <div v-loading="allTagsLoading" class="tag-picker">
                <div class="tag-picker-section">
                  <div class="tag-picker-label">选择已有标签</div>
                  <div v-if="availableTags.length" class="tag-picker-list">
                    <el-tag
                      v-for="tag in availableTags"
                      :key="tag.id"
                      :color="tag.color"
                      class="defect-tag tag-pick-item"
                      effect="dark"
                      @click="handleAddExistingTag(tag)"
                    >
                      {{ tag.name }}
                    </el-tag>
                  </div>
                  <span v-else class="empty-hint">没有可添加的标签</span>
                </div>

                <el-divider class="tag-picker-divider" />

                <div class="tag-picker-section">
                  <div class="tag-picker-label">新建标签</div>
                  <div class="tag-create-row">
                    <el-input
                      v-model="newTagName"
                      size="small"
                      maxlength="20"
                      placeholder="标签名称"
                    />
                    <el-color-picker v-model="newTagColor" size="small" />
                  </div>
                  <div class="tag-create-actions">
                    <el-button
                      size="small"
                      type="primary"
                      :loading="creatingTag"
                      :disabled="!newTagName.trim()"
                      @click="handleCreateAndAddTag"
                    >
                      创建并添加
                    </el-button>
                  </div>
                </div>
              </div>
            </el-popover>
          </div>
        </section>

        <!-- Custom fields -->
        <section class="section">
          <h4 class="section-title">自定义字段</h4>
          <div v-loading="fieldsLoading">
            <template v-if="fieldDefs.length">
              <el-form label-width="120px" label-position="right">
                <el-form-item
                  v-for="def in fieldDefs"
                  :key="def.id"
                  :label="def.label"
                  :required="def.required"
                >
                  <el-input
                    v-if="def.fieldType === 'TEXT'"
                    v-model="fieldValues[def.fieldKey]"
                    :placeholder="`请输入${def.label}`"
                    clearable
                  />
                  <el-input-number
                    v-else-if="def.fieldType === 'NUMBER'"
                    v-model="fieldValues[def.fieldKey]"
                    :controls="false"
                    :placeholder="`请输入${def.label}`"
                    style="width: 100%"
                  />
                  <el-select
                    v-else-if="def.fieldType === 'SELECT'"
                    v-model="fieldValues[def.fieldKey]"
                    :placeholder="`请选择${def.label}`"
                    clearable
                    style="width: 100%"
                  >
                    <el-option
                      v-for="opt in def.options"
                      :key="opt"
                      :label="opt"
                      :value="opt"
                    />
                  </el-select>
                </el-form-item>
              </el-form>
              <div class="fields-actions">
                <el-button type="primary" :loading="savingFields" @click="handleSaveFields">
                  保存字段
                </el-button>
              </div>
            </template>
            <span v-else-if="!fieldsLoading" class="empty-hint">该项目未配置自定义字段</span>
          </div>
        </section>

        <!-- Transition actions -->
        <section class="section">
          <h4 class="section-title">流转操作</h4>
          <el-space v-if="detail.availableTransitions.length" wrap :size="8">
            <el-button
              v-for="t in detail.availableTransitions"
              :key="t.code"
              :type="transitionButtonType(t.code)"
              size="small"
              :loading="transitioning"
              @click="handleTransition(t)"
            >
              {{ t.name }}
            </el-button>
          </el-space>
          <span v-else class="empty-hint">当前状态没有可用的流转操作</span>
        </section>

        <!-- History timeline -->
        <section class="section">
          <h4 class="section-title">流转历史</h4>
          <el-timeline v-if="detail.history.length">
            <el-timeline-item
              v-for="(h, idx) in detail.history"
              :key="idx"
              :timestamp="formatTime(h.createTime)"
              placement="top"
            >
              <div class="history-line">
                <strong>{{ h.operatorName }}</strong>
                <span class="history-flow">
                  {{ stateLabel(h.fromState) }} → {{ stateLabel(h.toState) }}
                </span>
                <el-tag size="small" type="info" effect="plain">{{ h.transitionName }}</el-tag>
              </div>
              <div v-if="h.comment" class="history-comment">{{ h.comment }}</div>
            </el-timeline-item>
          </el-timeline>
          <span v-else class="empty-hint">暂无历史记录</span>
        </section>

        <!-- Relations -->
        <section class="section">
          <div class="section-head">
            <h4 class="section-title">关联 ({{ relations.length }})</h4>
            <el-button type="primary" size="small" :icon="Plus" @click="openLinkDialog">
              添加关联
            </el-button>
          </div>
          <div v-loading="relationsLoading">
            <ul v-if="relations.length" class="relation-list">
              <li v-for="rel in relations" :key="rel.id" class="relation-item">
                <div class="relation-main">
                  <el-tag size="small" type="info" effect="plain">
                    {{ RELATION_TYPE_LABEL[rel.relationType] ?? rel.relationType }}
                  </el-tag>
                  <el-tag
                    size="small"
                    :type="RELATION_ENTITY_TYPE_TAG_TYPE[counterpart(rel).type]"
                    effect="light"
                  >
                    {{ RELATION_ENTITY_TYPE_LABEL[counterpart(rel).type] ?? counterpart(rel).type }}
                  </el-tag>
                  <span class="relation-title">{{ counterpart(rel).title }}</span>
                </div>
                <el-button
                  type="danger"
                  size="small"
                  link
                  :loading="unlinkingId === rel.id"
                  @click="handleUnlink(rel)"
                >
                  解除
                </el-button>
              </li>
            </ul>
            <span v-else class="empty-hint">暂无关联</span>
          </div>
        </section>

        <!-- Attachments -->
        <section class="section">
          <h4 class="section-title">附件 ({{ attachments.length }})</h4>
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :data="uploadData"
            name="file"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
          >
            <el-button type="primary" size="small" :icon="UploadFilled">上传附件</el-button>
          </el-upload>

          <div v-loading="attachmentsLoading" class="attachment-wrap">
            <ul v-if="attachments.length" class="attachment-list">
              <li v-for="att in attachments" :key="att.id" class="attachment-item">
                <div class="attachment-main">
                  <el-icon class="attachment-icon"><Document /></el-icon>
                  <span class="attachment-name">{{ att.fileName }}</span>
                  <span class="attachment-meta">{{ formatFileSize(att.sizeBytes) }}</span>
                  <span class="attachment-meta">{{ att.uploaderName }}</span>
                  <span class="attachment-meta">{{ formatTime(att.createTime) }}</span>
                </div>
                <el-button
                  type="primary"
                  size="small"
                  link
                  :loading="downloadingId === att.id"
                  @click="handleDownload(att)"
                >
                  下载
                </el-button>
              </li>
            </ul>
            <span v-else class="empty-hint">暂无附件</span>
          </div>
        </section>

        <!-- Activity timeline -->
        <section class="section">
          <h4 class="section-title">操作记录</h4>
          <div v-loading="activitiesLoading">
            <el-timeline v-if="activities.length">
              <el-timeline-item
                v-for="(a, idx) in activities"
                :key="idx"
                :timestamp="formatTime(a.createTime)"
                placement="top"
              >
                <div class="activity-line">
                  <strong>{{ a.operatorName }}</strong>
                  <el-tag size="small" type="info" effect="plain">
                    {{ activityActionLabel(a.action) }}
                  </el-tag>
                </div>
                <div v-if="a.detail" class="activity-detail">{{ a.detail }}</div>
              </el-timeline-item>
            </el-timeline>
            <span v-else class="empty-hint">暂无操作记录</span>
          </div>
        </section>

        <!-- Comments -->
        <section class="section">
          <h4 class="section-title">评论 ({{ detail.comments.length }})</h4>
          <ul v-if="detail.comments.length" class="comment-list">
            <li v-for="c in detail.comments" :key="c.id" class="comment-item">
              <div class="comment-head">
                <strong>{{ c.authorName }}</strong>
                <span class="comment-time">{{ formatTime(c.createTime) }}</span>
              </div>
              <div class="comment-content">{{ c.content }}</div>
            </li>
          </ul>
          <span v-else class="empty-hint">暂无评论</span>

          <div class="comment-input">
            <el-input
              v-model="commentText"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="写下你的评论..."
            />
            <div class="comment-actions">
              <el-button
                type="primary"
                :loading="commenting"
                :disabled="!commentText.trim()"
                @click="handleAddComment"
              >
                发送评论
              </el-button>
            </div>
          </div>
        </section>
      </template>
    </div>

    <!-- Add relation dialog -->
    <el-dialog
      v-model="linkVisible"
      title="添加关联"
      width="480px"
      append-to-body
      @closed="resetLinkForm"
    >
      <el-form label-width="90px">
        <el-form-item label="关联类型">
          <el-select
            v-model="linkForm.relationType"
            placeholder="请选择关联类型"
            style="width: 100%"
            @change="handleRelationTypeChange"
          >
            <el-option
              v-for="opt in DEFECT_RELATION_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="targetEntityLabel">
          <el-select
            v-model="linkForm.targetId"
            placeholder="请先选择关联类型"
            filterable
            :disabled="!linkForm.relationType"
            :loading="targetOptionsLoading"
            style="width: 100%"
          >
            <el-option
              v-for="opt in targetOptions"
              :key="opt.id"
              :label="opt.title"
              :value="opt.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linkVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="linking"
          :disabled="!linkForm.relationType || linkForm.targetId == null"
          @click="handleLink"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, UploadFilled, Document } from '@element-plus/icons-vue'
import {
  addDefectComment,
  getDefectActivity,
  getDefectDetail,
  transitionDefect,
} from '@/api/defect'
import { downloadAttachment, listAttachments } from '@/api/attachment'
import { createTag, getDefectTags, listTags, setDefectTags } from '@/api/tag'
import { getCustomFieldDefs, setDefectFields } from '@/api/customfield'
import { linkRelation, listRelations, unlinkRelation } from '@/api/relation'
import { listRequirements } from '@/api/requirement'
import { listTestCases } from '@/api/testcase'
import { useUserStore } from '@/store/user'
import type {
  ActivityDto,
  AttachmentDto,
  CustomFieldDefDto,
  DefectDetailResult,
  RelationDto,
  RelationEntityType,
  RelationType,
  StatusCode,
  TagDto,
  TransitionOption,
} from '@/types'
import {
  STATUS_LABEL,
  STATUS_TAG_TYPE,
  SEVERITY_LABEL,
  SEVERITY_TAG_TYPE,
  PRIORITY_TAG_TYPE,
  RELATION_TYPE_LABEL,
  RELATION_ENTITY_TYPE_LABEL,
  RELATION_ENTITY_TYPE_TAG_TYPE,
  DEFECT_RELATION_TYPE_OPTIONS,
  transitionButtonType,
  activityActionLabel,
  formatFileSize,
  formatTime,
} from '@/utils/enums'

const props = defineProps<{
  modelValue: boolean
  defectId: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  // Emitted whenever the defect data changes (transition/comment) so the
  // parent list/board can refresh.
  refresh: []
}>()

const visible = ref(props.modelValue)
const loading = ref(false)
const detail = ref<DefectDetailResult | null>(null)
const commentText = ref('')
const commenting = ref(false)
const transitioning = ref(false)
// Track whether any mutation happened so we refresh the parent on close.
let mutated = false

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val && props.defectId != null) {
      mutated = false
      fetchDetail()
      fetchRelations()
      fetchAttachments()
      fetchActivities()
      fetchTags()
    }
  },
)

watch(visible, (val) => {
  if (val !== props.modelValue) emit('update:modelValue', val)
})

const drawerTitle = '缺陷详情'

function stateLabel(state: string | null): string {
  if (!state) return '—'
  return STATUS_LABEL[state as StatusCode] ?? state
}

async function fetchDetail() {
  if (props.defectId == null) return
  loading.value = true
  try {
    detail.value = await getDefectDetail({ defectId: props.defectId })
    // Custom field definitions depend on the project, which is only known
    // once the detail (and therefore the defect) is loaded.
    await fetchCustomFields()
  } catch {
    // Interceptor already showed an error.
  } finally {
    loading.value = false
  }
}

// ---- Custom fields ----
const fieldDefs = ref<CustomFieldDefDto[]>([])
const fieldValues = ref<Record<string, any>>({})
const fieldsLoading = ref(false)
const savingFields = ref(false)

async function fetchCustomFields() {
  const projectId = detail.value?.defect.projectId
  if (projectId == null) {
    fieldDefs.value = []
    fieldValues.value = {}
    return
  }
  fieldsLoading.value = true
  try {
    const defs = await getCustomFieldDefs({ projectId })
    fieldDefs.value = defs ?? []
    syncFieldValues()
  } catch {
    fieldDefs.value = []
    fieldValues.value = {}
  } finally {
    fieldsLoading.value = false
  }
}

// Seed editable values from the defect's stored custom field values.
function syncFieldValues() {
  const stored = detail.value?.defect.customFields ?? {}
  const next: Record<string, any> = {}
  for (const def of fieldDefs.value) {
    next[def.fieldKey] = stored[def.fieldKey] ?? (def.fieldType === 'NUMBER' ? undefined : '')
  }
  fieldValues.value = next
}

async function handleSaveFields() {
  if (props.defectId == null) return
  // Validate required fields.
  for (const def of fieldDefs.value) {
    if (def.required) {
      const v = fieldValues.value[def.fieldKey]
      if (v === '' || v === null || v === undefined) {
        ElMessage.warning(`请填写「${def.label}」`)
        return
      }
    }
  }
  savingFields.value = true
  try {
    const updated = await setDefectFields({
      defectId: props.defectId,
      fields: { ...fieldValues.value },
    })
    ElMessage.success('字段已保存')
    mutated = true
    // Reflect the returned values locally and re-seed the editable form.
    if (detail.value) {
      detail.value.defect = updated
      syncFieldValues()
    }
  } catch {
    // Interceptor already showed an error.
  } finally {
    savingFields.value = false
  }
}

// ---- Attachments ----
const userStore = useUserStore()
const attachments = ref<AttachmentDto[]>([])
const attachmentsLoading = ref(false)
const downloadingId = ref<number | null>(null)

// el-upload posts directly to the backend, so we attach the bearer token
// header manually (the axios interceptor does not run for el-upload requests).
const uploadAction = '/devtrack/attachment/upload'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`,
}))
const uploadData = computed(() => ({ defectId: props.defectId }))

async function fetchAttachments() {
  if (props.defectId == null) return
  attachmentsLoading.value = true
  try {
    const list = await listAttachments({ defectId: props.defectId })
    attachments.value = list ?? []
  } catch {
    // Interceptor already showed an error.
  } finally {
    attachmentsLoading.value = false
  }
}

function handleUploadSuccess() {
  ElMessage.success('上传成功')
  fetchAttachments()
}

function handleUploadError() {
  ElMessage.error('上传失败')
}

async function handleDownload(att: AttachmentDto) {
  downloadingId.value = att.id
  try {
    const blob = await downloadAttachment(att.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = att.fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  } finally {
    downloadingId.value = null
  }
}

// ---- Activity (operation records) ----
const activities = ref<ActivityDto[]>([])
const activitiesLoading = ref(false)

async function fetchActivities() {
  if (props.defectId == null) return
  activitiesLoading.value = true
  try {
    const list = await getDefectActivity({ defectId: props.defectId })
    // Show newest first.
    activities.value = (list ?? []).slice().sort((a, b) => b.createTime - a.createTime)
  } catch {
    // Interceptor already showed an error.
  } finally {
    activitiesLoading.value = false
  }
}

// ---- Tags ----
const defectTags = ref<TagDto[]>([])
const tagsLoading = ref(false)
const tagPopoverVisible = ref(false)
const allTags = ref<TagDto[]>([])
const allTagsLoading = ref(false)
const newTagName = ref('')
const newTagColor = ref('#409eff')
const creatingTag = ref(false)

// Existing tags not already applied to this defect.
const availableTags = computed(() => {
  const applied = new Set(defectTags.value.map((t) => t.id))
  return allTags.value.filter((t) => !applied.has(t.id))
})

async function fetchTags() {
  if (props.defectId == null) return
  tagsLoading.value = true
  try {
    const list = await getDefectTags({ defectId: props.defectId })
    defectTags.value = list ?? []
  } catch {
    // Interceptor already showed an error.
  } finally {
    tagsLoading.value = false
  }
}

async function handleTagPopoverShow() {
  const projectId = detail.value?.defect.projectId
  if (projectId == null) return
  allTagsLoading.value = true
  try {
    const list = await listTags({ projectId })
    allTags.value = list ?? []
  } catch {
    allTags.value = []
  } finally {
    allTagsLoading.value = false
  }
}

// Submit the full tag-id list and refresh from the returned tags.
async function commitTags(tagIds: number[]) {
  if (props.defectId == null) return
  try {
    const list = await setDefectTags({ defectId: props.defectId, tagIds })
    defectTags.value = list ?? []
    mutated = true
  } catch {
    // Interceptor already showed an error; restore by re-fetching.
    await fetchTags()
  }
}

async function handleAddExistingTag(tag: TagDto) {
  if (defectTags.value.some((t) => t.id === tag.id)) return
  const tagIds = [...defectTags.value.map((t) => t.id), tag.id]
  await commitTags(tagIds)
  tagPopoverVisible.value = false
}

async function handleRemoveTag(tag: TagDto) {
  const tagIds = defectTags.value.filter((t) => t.id !== tag.id).map((t) => t.id)
  await commitTags(tagIds)
}

async function handleCreateAndAddTag() {
  const name = newTagName.value.trim()
  const projectId = detail.value?.defect.projectId
  if (!name || projectId == null) return
  creatingTag.value = true
  try {
    const created = await createTag({ projectId, name, color: newTagColor.value })
    allTags.value = [...allTags.value, created]
    const tagIds = [...defectTags.value.map((t) => t.id), created.id]
    await commitTags(tagIds)
    newTagName.value = ''
    newTagColor.value = '#409eff'
    tagPopoverVisible.value = false
  } catch {
    // Interceptor already showed an error.
  } finally {
    creatingTag.value = false
  }
}

// ---- Relations ----
const relations = ref<RelationDto[]>([])
const relationsLoading = ref(false)
const unlinkingId = ref<number | null>(null)

// Resolve the "other side" of a relation (the side that is not the current defect).
function counterpart(rel: RelationDto): { type: RelationEntityType; id: number; title: string } {
  const isCurrentSource = rel.sourceType === 'DEFECT' && rel.sourceId === props.defectId
  if (isCurrentSource) {
    return { type: rel.targetType, id: rel.targetId, title: rel.targetTitle }
  }
  return { type: rel.sourceType, id: rel.sourceId, title: rel.sourceTitle }
}

async function fetchRelations() {
  if (props.defectId == null) return
  relationsLoading.value = true
  try {
    const list = await listRelations({ entityType: 'DEFECT', entityId: props.defectId })
    relations.value = list ?? []
  } catch {
    // Interceptor already showed an error.
  } finally {
    relationsLoading.value = false
  }
}

async function handleUnlink(rel: RelationDto) {
  try {
    await ElMessageBox.confirm('确定要解除该关联吗？', '解除关联', {
      confirmButtonText: '解除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  unlinkingId.value = rel.id
  try {
    await unlinkRelation({ relationId: rel.id })
    ElMessage.success('已解除关联')
    await fetchRelations()
  } catch {
    // Interceptor already showed an error.
  } finally {
    unlinkingId.value = null
  }
}

// ---- Add relation dialog ----
const linkVisible = ref(false)
const linking = ref(false)
const targetOptionsLoading = ref(false)
const targetOptions = ref<{ id: number; title: string }[]>([])

interface LinkForm {
  relationType: RelationType | ''
  targetId: number | null
}

const linkForm = reactive<LinkForm>({ relationType: '', targetId: null })

const selectedRelationOption = computed(() =>
  DEFECT_RELATION_TYPE_OPTIONS.find((o) => o.value === linkForm.relationType),
)

const targetEntityLabel = computed(() => {
  const t = selectedRelationOption.value?.targetType
  return t ? RELATION_ENTITY_TYPE_LABEL[t] : '关联对象'
})

function openLinkDialog() {
  linkVisible.value = true
}

function resetLinkForm() {
  linkForm.relationType = ''
  linkForm.targetId = null
  targetOptions.value = []
}

async function handleRelationTypeChange() {
  linkForm.targetId = null
  await loadTargetOptions()
}

async function loadTargetOptions() {
  const targetType = selectedRelationOption.value?.targetType
  const projectId = detail.value?.defect.projectId
  if (!targetType || projectId == null) {
    targetOptions.value = []
    return
  }
  targetOptionsLoading.value = true
  try {
    if (targetType === 'TEST_CASE') {
      const list = await listTestCases({ projectId })
      targetOptions.value = (list ?? []).map((tc) => ({ id: tc.id, title: tc.title }))
    } else if (targetType === 'REQUIREMENT') {
      const list = await listRequirements({ projectId })
      targetOptions.value = (list ?? []).map((r) => ({ id: r.id, title: r.title }))
    } else {
      targetOptions.value = []
    }
  } catch {
    targetOptions.value = []
  } finally {
    targetOptionsLoading.value = false
  }
}

async function handleLink() {
  const opt = selectedRelationOption.value
  if (!opt || linkForm.targetId == null || props.defectId == null) return
  linking.value = true
  try {
    await linkRelation({
      sourceType: 'DEFECT',
      sourceId: props.defectId,
      targetType: opt.targetType,
      targetId: linkForm.targetId,
      relationType: opt.value,
    })
    ElMessage.success('关联成功')
    linkVisible.value = false
    await fetchRelations()
  } catch {
    // Interceptor already showed an error.
  } finally {
    linking.value = false
  }
}

async function handleAddComment() {
  const content = commentText.value.trim()
  if (!content || props.defectId == null) return
  commenting.value = true
  try {
    await addDefectComment({ defectId: props.defectId, content })
    commentText.value = ''
    ElMessage.success('评论已发送')
    mutated = true
    await fetchDetail()
  } catch {
    // Interceptor already showed an error.
  } finally {
    commenting.value = false
  }
}

async function handleTransition(t: TransitionOption) {
  if (props.defectId == null) return
  let comment: string | undefined
  if (t.requireComment) {
    try {
      const { value } = await ElMessageBox.prompt(
        `请填写「${t.name}」的说明`,
        t.name,
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: '请输入说明（必填）',
          inputValidator: (v) => (v && v.trim().length > 0 ? true : '说明不能为空'),
        },
      )
      comment = value
    } catch {
      return
    }
  }

  transitioning.value = true
  try {
    await transitionDefect({
      defectId: props.defectId,
      transitionCode: t.code,
      comment,
    })
    ElMessage.success(`${t.name}成功`)
    mutated = true
    await fetchDetail()
  } catch {
    // Interceptor already showed an error.
  } finally {
    transitioning.value = false
  }
}

function handleClosed() {
  if (mutated) {
    emit('refresh')
    mutated = false
  }
  detail.value = null
  commentText.value = ''
  relations.value = []
  attachments.value = []
  activities.value = []
  fieldDefs.value = []
  fieldValues.value = {}
  defectTags.value = []
  allTags.value = []
  tagPopoverVisible.value = false
  newTagName.value = ''
  newTagColor.value = '#409eff'
}
</script>

<style scoped>
.drawer-body {
  min-height: 200px;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.header-section {
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.defect-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.display-key {
  font-family: monospace;
  font-weight: 700;
  color: #409eff;
}

.title-text {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.tag-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.defect-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.defect-tag {
  border: none;
  color: #fff;
}

.tag-picker-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tag-picker-label {
  font-size: 12px;
  color: #909399;
}

.tag-picker-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-pick-item {
  cursor: pointer;
}

.tag-picker-divider {
  margin: 12px 0;
}

.tag-create-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-create-actions {
  display: flex;
  justify-content: flex-end;
}

.description {
  margin: 8px 0 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.history-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.history-flow {
  color: #606266;
  font-size: 13px;
}

.history-comment {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
  background: #f5f7fa;
  padding: 6px 10px;
  border-radius: 4px;
}

.comment-list {
  list-style: none;
  margin: 0 0 16px;
  padding: 0;
}

.comment-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f2f5;
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.comment-time {
  color: #c0c4cc;
  font-size: 12px;
}

.comment-content {
  color: #303133;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.comment-input {
  margin-top: 8px;
}

.comment-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.empty-hint {
  color: #c0c4cc;
  font-size: 13px;
}

.fields-actions {
  display: flex;
  justify-content: flex-end;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-head .section-title {
  margin: 0;
}

.relation-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.relation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}

.relation-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}

.relation-title {
  color: #303133;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-wrap {
  margin-top: 12px;
}

.attachment-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}

.attachment-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}

.attachment-icon {
  color: #909399;
}

.attachment-name {
  color: #303133;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-meta {
  color: #909399;
  font-size: 12px;
}

.activity-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.activity-detail {
  margin-top: 4px;
  color: #606266;
  font-size: 13px;
}
</style>
