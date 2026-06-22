<template>
  <div class="iteration-page">
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
      <div class="layout">
        <!-- Left: iteration list -->
        <el-card class="list-card" shadow="never">
          <template #header>
            <div class="list-header">
              <span>迭代列表</span>
              <el-button type="success" size="small" :icon="Plus" @click="openCreateDialog">
                新建迭代
              </el-button>
            </div>
          </template>

          <div v-loading="listLoading" class="list-body">
            <div
              v-for="it in iterations"
              :key="it.id"
              class="iteration-item"
              :class="{ active: it.id === activeIterationId }"
              @click="selectIteration(it)"
            >
              <div class="item-top">
                <span class="item-name">{{ it.name }}</span>
                <el-tag :type="ITERATION_STATUS_TAG_TYPE[it.status]" size="small" effect="light">
                  {{ ITERATION_STATUS_LABEL[it.status] }}
                </el-tag>
              </div>
              <div class="item-dates">{{ it.startDate }} ~ {{ it.endDate }}</div>
              <div v-if="it.status !== 'CLOSED'" class="item-actions">
                <el-button
                  type="warning"
                  size="small"
                  link
                  @click.stop="handleClose(it)"
                >
                  关闭
                </el-button>
              </div>
            </div>

            <el-empty
              v-if="!listLoading && iterations.length === 0"
              description="暂无迭代"
              :image-size="80"
            />
          </div>
        </el-card>

        <!-- Right: burndown chart + report -->
        <div class="right-col">
          <el-card class="chart-card" shadow="never">
            <div v-if="activeIterationId === null" class="chart-placeholder">
              <el-empty description="请选择左侧迭代查看燃尽图" />
            </div>
            <div v-else v-loading="burndownLoading" class="chart-wrap">
              <BurndownChart :data="burndown" />
            </div>
          </el-card>

          <!-- Iteration report -->
          <el-card v-if="activeIterationId !== null" class="report-card" shadow="never">
            <template #header>
              <span class="report-title">迭代报表</span>
            </template>
            <div v-loading="reportLoading" class="report-body">
              <template v-if="report">
                <div class="stat-row">
                  <el-statistic title="总数" :value="report.total" />
                  <el-statistic title="已完成" :value="report.done" />
                  <el-statistic title="进行中" :value="report.inProgress" />
                  <el-statistic title="待处理" :value="report.todo" />
                </div>

                <div class="completion-block">
                  <div class="completion-label">
                    <span>完成率</span>
                    <strong>{{ report.completionRate }}%</strong>
                  </div>
                  <el-progress
                    :percentage="clampPercent(report.completionRate)"
                    :stroke-width="14"
                    :status="report.completionRate >= 100 ? 'success' : undefined"
                  />
                </div>

                <div class="breakdown">
                  <h5 class="breakdown-title">按状态</h5>
                  <div v-if="statusEntries.length" class="tag-list">
                    <el-tag
                      v-for="[key, count] in statusEntries"
                      :key="key"
                      size="large"
                      effect="light"
                    >
                      {{ statusLabel(key) }}: {{ count }}
                    </el-tag>
                  </div>
                  <span v-else class="empty-hint">暂无数据</span>
                </div>

                <div class="breakdown">
                  <h5 class="breakdown-title">按优先级</h5>
                  <div v-if="priorityEntries.length" class="tag-list">
                    <el-tag
                      v-for="[key, count] in priorityEntries"
                      :key="key"
                      size="large"
                      effect="light"
                      :type="priorityTagType(key)"
                    >
                      {{ priorityLabel(key) }}: {{ count }}
                    </el-tag>
                  </div>
                  <span v-else class="empty-hint">暂无数据</span>
                </div>
              </template>
              <el-empty v-else-if="!reportLoading" description="暂无报表数据" :image-size="60" />
            </div>
          </el-card>
        </div>
      </div>
    </main>

    <!-- Create dialog -->
    <el-dialog v-model="createVisible" title="新建迭代" width="480px" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入迭代名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="起止日期" prop="dateRange">
          <el-date-picker
            v-model="createForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import { User, ArrowDown, SwitchButton, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  createIteration,
  listIterations,
  closeIteration,
  getBurndown,
  getIterationReport,
} from '@/api/iteration'
import type { BurndownResult, IterationReportResult, IterationDto, Priority, StatusCode } from '@/types'
import {
  ITERATION_STATUS_LABEL,
  ITERATION_STATUS_TAG_TYPE,
  STATUS_LABEL,
  PRIORITY_LABEL,
  PRIORITY_TAG_TYPE,
} from '@/utils/enums'
import BurndownChart from '@/components/BurndownChart.vue'
import NotificationBell from '@/components/NotificationBell.vue'
import AppNav from '@/components/AppNav.vue'

const PROJECT_ID = 1

const router = useRouter()
const userStore = useUserStore()

// ---- Iteration list ----
const iterations = ref<IterationDto[]>([])
const listLoading = ref(false)
const activeIterationId = ref<number | null>(null)

async function fetchIterations() {
  listLoading.value = true
  try {
    const list = await listIterations({ projectId: PROJECT_ID })
    iterations.value = list ?? []
    // Keep the current selection if it still exists, otherwise auto-select the first.
    if (
      activeIterationId.value === null ||
      !iterations.value.some((it) => it.id === activeIterationId.value)
    ) {
      const first = iterations.value[0]
      if (first) {
        selectIteration(first)
      } else {
        activeIterationId.value = null
        burndown.value = null
        report.value = null
      }
    }
  } catch {
    // Interceptor already showed an error.
  } finally {
    listLoading.value = false
  }
}

// ---- Burndown ----
const burndown = ref<BurndownResult | null>(null)
const burndownLoading = ref(false)

function selectIteration(it: IterationDto) {
  activeIterationId.value = it.id
  fetchBurndown(it.id)
  fetchReport(it.id)
}

async function fetchBurndown(iterationId: number) {
  burndownLoading.value = true
  try {
    burndown.value = await getBurndown({ iterationId })
  } catch {
    burndown.value = null
  } finally {
    burndownLoading.value = false
  }
}

// ---- Iteration report ----
const report = ref<IterationReportResult | null>(null)
const reportLoading = ref(false)

async function fetchReport(iterationId: number) {
  reportLoading.value = true
  try {
    report.value = await getIterationReport({ iterationId })
  } catch {
    report.value = null
  } finally {
    reportLoading.value = false
  }
}

const statusEntries = computed(() => Object.entries(report.value?.byStatus ?? {}))
const priorityEntries = computed(() => Object.entries(report.value?.byPriority ?? {}))

function statusLabel(code: string): string {
  return STATUS_LABEL[code as StatusCode] ?? code
}

function priorityLabel(code: string): string {
  return PRIORITY_LABEL[code as Priority] ?? code
}

function priorityTagType(code: string) {
  return PRIORITY_TAG_TYPE[code as Priority] ?? 'info'
}

function clampPercent(v: number): number {
  if (Number.isNaN(v)) return 0
  return Math.min(100, Math.max(0, Math.round(v)))
}

// ---- Close iteration ----
async function handleClose(it: IterationDto) {
  try {
    await ElMessageBox.confirm(`确定要关闭迭代「${it.name}」吗？`, '关闭迭代', {
      confirmButtonText: '关闭',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await closeIteration({ iterationId: it.id })
    ElMessage.success('已关闭')
    fetchIterations()
  } catch {
    // Interceptor already showed an error.
  }
}

// ---- Create dialog ----
const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()

interface CreateForm {
  name: string
  dateRange: [string, string] | null
}

function defaultCreateForm(): CreateForm {
  return { name: '', dateRange: null }
}

const createForm = reactive<CreateForm>(defaultCreateForm())

const createRules: FormRules<CreateForm> = {
  name: [{ required: true, message: '请输入迭代名称', trigger: 'blur' }],
  dateRange: [
    {
      required: true,
      message: '请选择起止日期',
      trigger: 'change',
      validator: (_rule, value, callback) => {
        if (Array.isArray(value) && value[0] && value[1]) {
          callback()
        } else {
          callback(new Error('请选择起止日期'))
        }
      },
    },
  ],
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
  if (!valid || !createForm.dateRange) return

  creating.value = true
  try {
    const created = await createIteration({
      projectId: PROJECT_ID,
      name: createForm.name,
      startDate: createForm.dateRange[0],
      endDate: createForm.dateRange[1],
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    activeIterationId.value = created.id
    await fetchIterations()
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

onMounted(fetchIterations)
</script>

<style scoped>
.iteration-page {
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

.layout {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

.list-card {
  flex: 0 0 320px;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 200px;
}

.iteration-item {
  border: 1px solid rgba(130,140,200,0.18);
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.iteration-item:hover {
  border-color: #c6e2ff;
  background: rgba(99,102,241,0.08);
}

.iteration-item.active {
  border-color: #818cf8;
  background: rgba(99,102,241,0.16);
}

.item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.item-name {
  font-weight: 600;
  color: #e7e9f3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-dates {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.item-actions {
  margin-top: 4px;
  text-align: right;
}

.right-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-card :deep(.el-card__body) {
  height: 100%;
}

.chart-placeholder {
  height: 100%;
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-wrap {
  height: 100%;
  min-height: 360px;
}

.report-title {
  font-weight: 600;
  color: #e7e9f3;
}

.report-body {
  min-height: 120px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.completion-block {
  margin-bottom: 20px;
}

.completion-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: #9298b4;
}

.completion-label strong {
  color: #818cf8;
}

.breakdown {
  margin-bottom: 16px;
}

.breakdown:last-child {
  margin-bottom: 0;
}

.breakdown-title {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #9298b4;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-hint {
  color: #c0c4cc;
  font-size: 13px;
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

  .layout {
    flex-direction: column;
  }

  .list-card {
    flex: 1 1 auto;
  }

  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
