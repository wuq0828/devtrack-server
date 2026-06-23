<template>
  <div class="defect-page">
    <!-- Header bar -->
    <header class="topbar">
      <div class="topbar-left">
        <span class="logo">DevTrack</span>
        <AppNav />
      </div>
      <div class="topbar-right">
        <NotificationBell />
        <el-dropdown>
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ userStore.username || '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="content">
      <!-- Filter bar -->
      <el-card class="filter-card" shadow="never">
        <el-form :inline="true" :model="filters" @submit.prevent>
          <el-form-item label="状态">
            <el-select
              v-model="filters.statusCode"
              placeholder="全部状态"
              clearable
              style="width: 150px"
            >
              <el-option
                v-for="opt in STATUS_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="优先级">
            <el-select
              v-model="filters.priority"
              placeholder="全部优先级"
              clearable
              style="width: 150px"
            >
              <el-option
                v-for="opt in PRIORITY_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="关键字">
            <el-input
              v-model="filters.keyword"
              placeholder="标题/编号"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
          </el-form-item>

          <el-form-item class="create-btn">
            <el-button type="success" :icon="Plus" @click="openCreateDialog">
              新建缺陷
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- Table -->
      <el-card class="table-card" shadow="never">
        <!-- Batch action bar -->
        <div v-if="selectedRows.length" class="batch-bar">
          <span class="batch-info">已选 {{ selectedRows.length }} 项</span>
          <el-select
            v-model="batchTransition"
            placeholder="选择流转动作"
            style="width: 180px"
          >
            <el-option
              v-for="opt in BATCH_TRANSITION_OPTIONS"
              :key="opt.code"
              :label="opt.label"
              :value="opt.code"
            />
          </el-select>
          <el-button
            type="primary"
            :loading="batchRunning"
            :disabled="!batchTransition"
            @click="handleBatchTransition"
          >
            批量执行
          </el-button>
          <el-button link @click="clearSelection">清空选择</el-button>
        </div>

        <el-table
          ref="tableRef"
          v-loading="loading"
          :data="rows"
          stripe
          border
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="displayKey" label="编号" width="130" />
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="STATUS_TAG_TYPE[(row as DefectDto).statusCode]" effect="light">
                {{ STATUS_LABEL[(row as DefectDto).statusCode] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="严重程度" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="SEVERITY_TAG_TYPE[(row as DefectDto).severity]" effect="plain">
                {{ SEVERITY_LABEL[(row as DefectDto).severity] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="PRIORITY_TAG_TYPE[(row as DefectDto).priority]" effect="dark">
                {{ (row as DefectDto).priority }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处理人" width="110" align="center">
            <template #default="{ row }">
              {{ userName((row as DefectDto).assigneeId) }}
            </template>
          </el-table-column>
          <el-table-column label="创建人" width="110" align="center">
            <template #default="{ row }">
              {{ userName((row as DefectDto).reporterId) }}
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170" align="center">
            <template #default="{ row }">
              {{ formatTime((row as DefectDto).createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="{ row }">
              <el-space wrap :size="4">
                <el-button
                  type="primary"
                  size="small"
                  plain
                  :icon="View"
                  class="detail-btn"
                  @click="openDetail((row as DefectDto).id)"
                >
                  详情
                </el-button>
                <el-button
                  v-for="action in getTransitions((row as DefectDto).statusCode, {
                    assigneeId: (row as DefectDto).assigneeId,
                    reporterId: (row as DefectDto).reporterId,
                    currentUserId: userStore.user?.userId ?? null,
                  })"
                  :key="action.code"
                  :type="action.type"
                  size="small"
                  :link="action.type !== 'primary'"
                  :plain="action.type === 'primary'"
                  :class="{ 'detail-btn': action.type === 'primary' }"
                  @click="handleTransition(row as DefectDto, action)"
                >
                  {{ action.label }}
                </el-button>
              </el-space>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="pagination">
          <el-pagination
            v-model:current-page="page.pn"
            v-model:page-size="page.ps"
            :total="page.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="fetchList"
            @size-change="handleSizeChange"
          />
        </div>
      </el-card>
    </main>

    <!-- Create dialog -->
    <el-dialog v-model="createVisible" title="新建缺陷" width="520px" @closed="resetCreateForm">
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="90px"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入缺陷标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入缺陷描述"
          />
        </el-form-item>
        <el-form-item label="严重程度" prop="severity">
          <el-select v-model="createForm.severity" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="opt in SEVERITY_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="createForm.priority" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="opt in PRIORITY_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人" prop="assigneeId">
          <el-select
            v-model="createForm.assigneeId"
            placeholder="指派给处理人(选填)"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option v-for="u in users" :key="u.userId" :label="u.name" :value="u.userId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">提交</el-button>
      </template>
    </el-dialog>

    <!-- Shared detail drawer -->
    <DefectDetailDrawer
      v-model="drawerVisible"
      :defect-id="activeDefectId"
      @refresh="fetchList"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type TableInstance,
} from 'element-plus'
import {
  User,
  ArrowDown,
  SwitchButton,
  Search,
  RefreshLeft,
  Plus,
  View,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  listDefects,
  createDefect,
  transitionDefect,
  batchTransitionDefects,
} from '@/api/defect'
import { listUsers, type UserBrief } from '@/api/user'
import type {
  DefectDto,
  DefectCreateRequest,
  Priority,
  Severity,
  StatusCode,
  TransitionCode,
} from '@/types'
import {
  STATUS_OPTIONS,
  PRIORITY_OPTIONS,
  SEVERITY_OPTIONS,
  STATUS_LABEL,
  STATUS_TAG_TYPE,
  SEVERITY_LABEL,
  SEVERITY_TAG_TYPE,
  PRIORITY_TAG_TYPE,
  BATCH_TRANSITION_OPTIONS,
  getTransitions,
  formatTime,
  type TransitionAction,
} from '@/utils/enums'
import DefectDetailDrawer from '@/components/DefectDetailDrawer.vue'
import NotificationBell from '@/components/NotificationBell.vue'
import AppNav from '@/components/AppNav.vue'

const PROJECT_ID = 1

const router = useRouter()
const userStore = useUserStore()

// ---- Filters & list state ----
const filters = reactive<{
  statusCode?: StatusCode
  priority?: Priority
  keyword?: string
}>({
  statusCode: undefined,
  priority: undefined,
  keyword: '',
})

const page = reactive({ pn: 1, ps: 10, total: 0 })
const rows = ref<DefectDto[]>([])
const loading = ref(false)

// ---- Detail drawer ----
const drawerVisible = ref(false)
const activeDefectId = ref<number | null>(null)

function openDetail(defectId: number) {
  activeDefectId.value = defectId
  drawerVisible.value = true
}

async function fetchList() {
  loading.value = true
  try {
    const result = await listDefects({
      projectId: PROJECT_ID,
      statusCode: filters.statusCode,
      priority: filters.priority,
      keyword: filters.keyword || undefined,
      pn: page.pn,
      ps: page.ps,
    })
    rows.value = result.list ?? []
    page.total = result.total ?? 0
    // Keep server-authoritative pagination in sync.
    page.pn = result.pn ?? page.pn
    page.ps = result.ps ?? page.ps
  } catch {
    // Interceptor already showed an error.
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.pn = 1
  fetchList()
}

function handleReset() {
  filters.statusCode = undefined
  filters.priority = undefined
  filters.keyword = ''
  page.pn = 1
  fetchList()
}

function handleSizeChange() {
  page.pn = 1
  fetchList()
}

// ---- Transitions ----
async function handleTransition(row: DefectDto, action: TransitionAction) {
  let comment: string | undefined
  if (action.requireComment) {
    try {
      const { value } = await ElMessageBox.prompt(
        `请填写「${action.label}」的说明`,
        action.label,
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
      // User cancelled.
      return
    }
  }

  try {
    const updated = await transitionDefect({
      defectId: row.id,
      transitionCode: action.code,
      comment,
    })
    ElMessage.success(`${action.label}成功`)
    // Update the row in place if returned, else refetch.
    const idx = rows.value.findIndex((r) => r.id === updated.id)
    if (idx >= 0) {
      rows.value[idx] = updated
    } else {
      fetchList()
    }
  } catch {
    // Interceptor already showed an error.
  }
}

// ---- Batch transition ----
const tableRef = ref<TableInstance>()
const selectedRows = ref<DefectDto[]>([])
const batchTransition = ref<TransitionCode | ''>('')
const batchRunning = ref(false)

function handleSelectionChange(rows: DefectDto[]) {
  selectedRows.value = rows
}

function clearSelection() {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

async function handleBatchTransition() {
  if (!batchTransition.value || selectedRows.value.length === 0) return
  batchRunning.value = true
  try {
    const { results } = await batchTransitionDefects({
      defectIds: selectedRows.value.map((r) => r.id),
      transitionCode: batchTransition.value,
    })
    const success = results.filter((r) => r.success).length
    const failed = results.length - success
    if (failed === 0) {
      ElMessage.success(`批量执行完成：成功 ${success}`)
    } else {
      ElMessage.warning(`批量执行完成：成功 ${success}，失败 ${failed}`)
    }
    clearSelection()
    batchTransition.value = ''
    await fetchList()
  } catch {
    // Interceptor already showed an error.
  } finally {
    batchRunning.value = false
  }
}

// ---- Create dialog ----
const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()

function defaultCreateForm(): DefectCreateRequest {
  return {
    projectId: PROJECT_ID,
    title: '',
    description: '',
    severity: 'MAJOR' as Severity,
    priority: 'P2' as Priority,
    assigneeId: undefined,
  }
}

const createForm = reactive<DefectCreateRequest>(defaultCreateForm())

const createRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  severity: [{ required: true, message: '请选择严重程度', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
}

function openCreateDialog() {
  createVisible.value = true
}

function resetCreateForm() {
  Object.assign(createForm, defaultCreateForm())
  createFormRef.value?.clearValidate()
}

async function handleCreate() {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    await createDefect({ ...createForm })
    ElMessage.success('创建成功')
    createVisible.value = false
    page.pn = 1
    fetchList()
  } catch {
    // Interceptor already showed an error.
  } finally {
    creating.value = false
  }
}

// ---- Logout ----
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  userStore.logout()
  router.replace('/login')
}

// ---- Users: assignee dropdown + id→name display ----
const users = ref<UserBrief[]>([])
const userMap = ref<Record<number, string>>({})
function userName(id: number | null | undefined): string {
  if (id == null) return '—'
  return userMap.value[id] ?? '—'
}
async function fetchUsers() {
  try {
    users.value = await listUsers()
    userMap.value = Object.fromEntries(users.value.map((u) => [u.userId, u.name]))
  } catch {
    // non-fatal — columns/dropdown just show placeholders
  }
}

onMounted(() => {
  fetchList()
  fetchUsers()
})
</script>

<style scoped>
.defect-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}


.topbar {
  height: 56px;
  background: #818cf8;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
}

.nav-menu {
  border-bottom: none !important;
  height: 56px;
}

.nav-menu :deep(.el-menu-item) {
  height: 56px;
  line-height: 56px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #fff;
  outline: none;
}

.content {
  flex: 1;
  padding: 16px 24px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.create-btn {
  margin-left: auto;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 12px;
  background: rgba(99,102,241,0.16);
  border: 1px solid #d9ecff;
  border-radius: 4px;
}

.batch-info {
  color: #818cf8;
  font-size: 13px;
  font-weight: 600;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.no-action {
  color: #c0c4cc;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* ---- Mobile responsive (lightweight) ---- */
@media (max-width: 768px) {
  .topbar {
    height: auto;
    flex-wrap: wrap;
    gap: 8px;
    padding: 8px 12px;
  }

  .topbar-left {
    gap: 8px;
    flex-wrap: wrap;
  }

  .nav-menu {
    height: auto;
    flex-wrap: wrap;
  }

  .nav-menu :deep(.el-menu-item) {
    height: 40px;
    line-height: 40px;
    padding: 0 10px;
  }

  .content {
    padding: 12px 10px;
  }

  .filter-card :deep(.el-form--inline .el-form-item) {
    margin-right: 8px;
  }

  .create-btn {
    margin-left: 0;
  }

  .table-card :deep(.el-table) {
    overflow-x: auto;
  }
}
</style>
