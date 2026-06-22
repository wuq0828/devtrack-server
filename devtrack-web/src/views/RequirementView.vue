<template>
  <div class="requirement-page">
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
            <span>需求管理</span>
            <el-button type="success" :icon="Plus" @click="openCreateDialog">
              新建需求
            </el-button>
          </div>
        </template>

        <el-table v-loading="loading" :data="rows" stripe border style="width: 100%">
          <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag
                :type="REQUIREMENT_STATUS_TAG_TYPE[(row as RequirementDto).status]"
                effect="light"
              >
                {{ REQUIREMENT_STATUS_LABEL[(row as RequirementDto).status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="PRIORITY_TAG_TYPE[(row as RequirementDto).priority]" effect="dark">
                {{ (row as RequirementDto).priority }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180" align="center">
            <template #default="{ row }">
              {{ formatTime((row as RequirementDto).createTime) }}
            </template>
          </el-table-column>
          <el-table-column type="expand" label="描述" width="80">
            <template #default="{ row }">
              <div class="expand-detail">
                <span class="detail-label">描述：</span>
                <span class="detail-text">{{ (row as RequirementDto).description || '-' }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && rows.length === 0" description="暂无需求" :image-size="100" />
      </el-card>
    </main>

    <!-- Create dialog -->
    <el-dialog v-model="createVisible" title="新建需求" width="520px" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="createForm.title"
            placeholder="请输入需求标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入需求描述"
          />
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
import { createRequirement, listRequirements } from '@/api/requirement'
import NotificationBell from '@/components/NotificationBell.vue'
import AppNav from '@/components/AppNav.vue'
import type { Priority, RequirementCreateRequest, RequirementDto } from '@/types'
import {
  REQUIREMENT_STATUS_LABEL,
  REQUIREMENT_STATUS_TAG_TYPE,
  PRIORITY_OPTIONS,
  PRIORITY_TAG_TYPE,
  formatTime,
} from '@/utils/enums'

const PROJECT_ID = 1

const router = useRouter()
const userStore = useUserStore()

// ---- List state ----
const rows = ref<RequirementDto[]>([])
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    rows.value = (await listRequirements({ projectId: PROJECT_ID })) ?? []
  } catch {
    // Interceptor already showed an error.
  } finally {
    loading.value = false
  }
}

// ---- Create dialog ----
const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()

function defaultCreateForm(): RequirementCreateRequest {
  return {
    projectId: PROJECT_ID,
    title: '',
    description: '',
    priority: 'P2' as Priority,
  }
}

const createForm = reactive<RequirementCreateRequest>(defaultCreateForm())

const createRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
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
    await createRequirement({ ...createForm })
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
.requirement-page {
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
  gap: 8px;
}

.detail-label {
  font-weight: 600;
  color: #9298b4;
  flex: 0 0 auto;
}

.detail-text {
  color: #e7e9f3;
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
