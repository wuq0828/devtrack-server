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
      <el-card class="table-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>测试用例管理</span>
            <el-button type="success" :icon="Plus" @click="openCreateDialog">
              新建用例
            </el-button>
          </div>
        </template>

        <el-table v-loading="loading" :data="rows" stripe border style="width: 100%">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="expand-detail">
                <div class="detail-item">
                  <span class="detail-label">前置条件：</span>
                  <span class="detail-text">{{ (row as TestCaseDto).preconditions || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">步骤：</span>
                  <span class="detail-text">{{ (row as TestCaseDto).steps || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">预期结果：</span>
                  <span class="detail-text">{{ (row as TestCaseDto).expected || '-' }}</span>
                </div>
                <div v-if="(row as TestCaseDto).automationKey" class="detail-item">
                  <span class="detail-label">自动化标识：</span>
                  <span class="detail-text">{{ (row as TestCaseDto).automationKey }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag
                :type="TESTCASE_STATUS_TAG_TYPE[(row as TestCaseDto).status]"
                effect="light"
              >
                {{ TESTCASE_STATUS_LABEL[(row as TestCaseDto).status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180" align="center">
            <template #default="{ row }">
              {{ formatTime((row as TestCaseDto).createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="修改状态" width="160" align="center">
            <template #default="{ row }">
              <el-select
                :model-value="(row as TestCaseDto).status"
                size="small"
                style="width: 120px"
                @change="(v: TestCaseStatus) => handleStatusChange(row as TestCaseDto, v)"
              >
                <el-option
                  v-for="opt in TESTCASE_STATUS_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && rows.length === 0" description="暂无用例" :image-size="100" />
      </el-card>
    </main>

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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import { User, ArrowDown, SwitchButton, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { createTestCase, listTestCases, updateTestCaseStatus } from '@/api/testcase'
import NotificationBell from '@/components/NotificationBell.vue'
import AppNav from '@/components/AppNav.vue'
import type { TestCaseCreateRequest, TestCaseDto, TestCaseStatus } from '@/types'
import {
  TESTCASE_STATUS_LABEL,
  TESTCASE_STATUS_TAG_TYPE,
  TESTCASE_STATUS_OPTIONS,
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

// ---- Status update ----
async function handleStatusChange(row: TestCaseDto, status: TestCaseStatus) {
  if (status === row.status) return
  try {
    const updated = await updateTestCaseStatus({ testCaseId: row.id, status })
    ElMessage.success('状态已更新')
    const idx = rows.value.findIndex((r) => r.id === updated.id)
    if (idx >= 0) {
      rows.value[idx] = updated
    } else {
      fetchList()
    }
  } catch {
    // Interceptor already showed an error; nothing was committed locally.
  }
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
  background: #409eff;
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

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.expand-detail {
  padding: 8px 48px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  gap: 8px;
}

.detail-label {
  font-weight: 600;
  color: #606266;
  flex: 0 0 84px;
}

.detail-text {
  color: #303133;
  white-space: pre-wrap;
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

  .table-card :deep(.el-table) {
    overflow-x: auto;
  }
}
</style>
