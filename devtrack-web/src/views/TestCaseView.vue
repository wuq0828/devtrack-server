<template>
  <div class="testcase-page">
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
      <!-- Stat summary -->
      <div class="stat-row">
        <button
          class="stat-chip"
          :class="{ active: statusFilter === 'ALL' }"
          @click="statusFilter = 'ALL'"
        >
          <span class="stat-num">{{ stats.total }}</span>
          <span class="stat-label">全部用例</span>
        </button>
        <div class="stat-chip rate">
          <span class="stat-num">{{ stats.passRate }}%</span>
          <span class="stat-label">通过率</span>
        </div>
        <button
          v-for="s in STATUS_FILTER_OPTIONS"
          :key="s.value"
          class="stat-chip"
          :class="{ active: statusFilter === s.value }"
          @click="statusFilter = s.value"
        >
          <span class="stat-num" :class="s.value.toLowerCase()">{{ stats[s.key] }}</span>
          <span class="stat-label">{{ s.label }}</span>
        </button>
      </div>

      <el-card class="table-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div class="card-header-left">
              <span class="card-title">测试用例管理</span>
              <el-tag size="small" type="info" effect="plain" round>
                {{ filteredRows.length }} / {{ rows.length }}
              </el-tag>
            </div>
            <div class="toolbar">
              <el-input
                v-model="keyword"
                class="search-input"
                placeholder="搜索标题 / 步骤 / 预期"
                :prefix-icon="Search"
                clearable
              />
              <el-tooltip content="只看回归用例" placement="top">
                <el-button
                  :type="regressionOnly ? 'warning' : 'default'"
                  :icon="Refresh"
                  plain
                  @click="regressionOnly = !regressionOnly"
                >
                  回归
                </el-button>
              </el-tooltip>
              <el-button :icon="RefreshRight" circle :loading="loading" @click="fetchList" />
              <el-button type="success" :icon="Plus" @click="openCreateDialog">新建用例</el-button>
            </div>
          </div>
        </template>

        <el-table
          v-loading="loading"
          :data="filteredRows"
          stripe
          border
          highlight-current-row
          style="width: 100%"
          row-key="id"
          @row-click="openDetail"
        >
          <el-table-column label="编号" width="90">
            <template #default="{ row }">
              <span class="cell-id">TC-{{ (row as TestCaseDto).id }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag
                :type="TESTCASE_STATUS_TAG_TYPE[(row as TestCaseDto).status]"
                effect="dark"
                size="small"
              >
                {{ TESTCASE_STATUS_LABEL[(row as TestCaseDto).status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="回归" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="(row as TestCaseDto).regression" type="warning" effect="plain" size="small">
                回归
              </el-tag>
              <span v-else class="cell-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="自动化" width="120" align="center">
            <template #default="{ row }">
              <el-tag v-if="(row as TestCaseDto).automationKey" type="success" effect="plain" size="small">
                已关联
              </el-tag>
              <span v-else class="cell-muted">手动</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170" align="center">
            <template #default="{ row }">
              <span class="cell-muted">{{ formatTime((row as TestCaseDto).createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click.stop="openDetail(row as TestCaseDto)">
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-if="!loading && filteredRows.length === 0"
          :description="rows.length === 0 ? '暂无用例' : '没有符合条件的用例'"
          :image-size="100"
        />
      </el-card>
    </main>

    <!-- Detail drawer -->
    <TestCaseDetailDrawer
      v-model="detailVisible"
      :test-case="activeCase"
      @saved="onCaseSaved"
      @deleted="onCaseDeleted"
    />

    <!-- Create dialog -->
    <el-dialog v-model="createVisible" title="新建用例" width="560px" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="createForm.title"
            placeholder="请输入用例标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="前置条件" prop="preconditions">
          <el-input
            v-model="createForm.preconditions"
            type="textarea"
            :rows="3"
            placeholder="请输入前置条件"
          />
        </el-form-item>
        <el-form-item label="步骤" prop="steps">
          <el-input
            v-model="createForm.steps"
            type="textarea"
            :rows="4"
            placeholder="请输入操作步骤"
          />
        </el-form-item>
        <el-form-item label="预期结果" prop="expected">
          <el-input
            v-model="createForm.expected"
            type="textarea"
            :rows="3"
            placeholder="请输入预期结果"
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
import {
  User,
  ArrowDown,
  SwitchButton,
  Plus,
  Search,
  Refresh,
  RefreshRight,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { createTestCase, listTestCases } from '@/api/testcase'
import NotificationBell from '@/components/NotificationBell.vue'
import AppNav from '@/components/AppNav.vue'
import TestCaseDetailDrawer from '@/components/TestCaseDetailDrawer.vue'
import type { TestCaseCreateRequest, TestCaseDto, TestCaseStatus } from '@/types'
import {
  TESTCASE_STATUS_LABEL,
  TESTCASE_STATUS_TAG_TYPE,
  formatTime,
} from '@/utils/enums'

const PROJECT_ID = 1

const router = useRouter()
const userStore = useUserStore()

// ---- List state ----
const rows = ref<TestCaseDto[]>([])
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    rows.value = (await listTestCases({ projectId: PROJECT_ID })) ?? []
  } catch {
    // Interceptor already showed an error.
  } finally {
    loading.value = false
  }
}

// ---- Filters ----
const keyword = ref('')
const statusFilter = ref<TestCaseStatus | 'ALL'>('ALL')
const regressionOnly = ref(false)

// Status filter chips — `key` indexes into the computed stats object.
const STATUS_FILTER_OPTIONS = [
  { value: 'PENDING' as TestCaseStatus, key: 'pending' as const, label: '待执行' },
  { value: 'PASS' as TestCaseStatus, key: 'pass' as const, label: '通过' },
  { value: 'FAIL' as TestCaseStatus, key: 'fail' as const, label: '失败' },
  { value: 'BLOCKED' as TestCaseStatus, key: 'blocked' as const, label: '阻塞' },
]

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return rows.value.filter((r) => {
    if (statusFilter.value !== 'ALL' && r.status !== statusFilter.value) return false
    if (regressionOnly.value && !r.regression) return false
    if (kw) {
      const haystack = `${r.title} ${r.steps} ${r.expected} ${r.preconditions}`.toLowerCase()
      if (!haystack.includes(kw)) return false
    }
    return true
  })
})

const stats = computed(() => {
  const total = rows.value.length
  let pass = 0
  let fail = 0
  let blocked = 0
  let pending = 0
  for (const r of rows.value) {
    if (r.status === 'PASS') pass++
    else if (r.status === 'FAIL') fail++
    else if (r.status === 'BLOCKED') blocked++
    else pending++
  }
  const executed = pass + fail + blocked
  const passRate = executed > 0 ? Math.round((pass / executed) * 100) : 0
  return { total, pass, fail, blocked, pending, passRate }
})

// ---- Detail drawer ----
const detailVisible = ref(false)
const activeCase = ref<TestCaseDto | null>(null)

function openDetail(row: TestCaseDto) {
  activeCase.value = row
  detailVisible.value = true
}

function onCaseSaved(updated: TestCaseDto) {
  const idx = rows.value.findIndex((r) => r.id === updated.id)
  if (idx >= 0) rows.value[idx] = updated
  // Keep the drawer's snapshot in sync for further edits.
  activeCase.value = updated
}

function onCaseDeleted(id: number) {
  rows.value = rows.value.filter((r) => r.id !== id)
}

// ---- Create dialog ----
const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()

function defaultCreateForm(): TestCaseCreateRequest {
  return {
    projectId: PROJECT_ID,
    title: '',
    preconditions: '',
    steps: '',
    expected: '',
  }
}

const createForm = reactive<TestCaseCreateRequest>(defaultCreateForm())

const createRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  steps: [{ required: true, message: '请输入步骤', trigger: 'blur' }],
  expected: [{ required: true, message: '请输入预期结果', trigger: 'blur' }],
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
    await createTestCase({ ...createForm })
    ElMessage.success('创建成功')
    createVisible.value = false
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

onMounted(fetchList)
</script>

<style scoped>
.testcase-page {
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

/* ---- Stat summary ---- */
.stat-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-chip {
  flex: 1;
  min-width: 110px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 14px 18px;
  border-radius: 14px;
  border: 1px solid rgba(130, 140, 200, 0.18);
  background: rgba(22, 28, 46, 0.55);
  backdrop-filter: saturate(140%) blur(12px);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  font: inherit;
  text-align: left;
}

.stat-chip:hover {
  transform: translateY(-2px);
  border-color: rgba(130, 140, 200, 0.32);
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.4);
}

.stat-chip.active {
  border-color: #6366f1;
  box-shadow: 0 0 0 1px #6366f1, 0 6px 22px rgba(99, 102, 241, 0.35);
}

.stat-chip.rate {
  cursor: default;
}

.stat-chip.rate:hover {
  transform: none;
  border-color: rgba(130, 140, 200, 0.18);
  box-shadow: none;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: #e7e9f3;
  line-height: 1;
}

.stat-num.pass {
  color: #34d399;
}
.stat-num.fail {
  color: #f87171;
}
.stat-num.blocked {
  color: #fbbf24;
}
.stat-num.pending {
  color: #818cf8;
}

.stat-label {
  font-size: 12px;
  color: #9298b4;
}

/* ---- Card header / toolbar ---- */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title {
  font-weight: 600;
  color: #e7e9f3;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-input {
  width: 240px;
}

/* ---- Table cells ---- */
.cell-id {
  font-family: monospace;
  font-weight: 700;
  color: #818cf8;
}

.cell-muted {
  color: #9298b4;
  font-size: 13px;
}

:deep(.el-table__row) {
  cursor: pointer;
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

  .search-input {
    width: 100%;
  }

  .table-card :deep(.el-table) {
    overflow-x: auto;
  }
}
</style>
