<template>
  <div class="ai-cases-page">
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
      <!-- PRD input -->
      <el-card class="prd-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>AI 生成测试用例</span>
          </div>
        </template>
        <el-input
          v-model="prd"
          type="textarea"
          :rows="8"
          maxlength="5000"
          show-word-limit
          placeholder="在此粘贴 PRD / 需求描述，AI 将据此生成测试用例…"
        />
        <div class="prd-actions">
          <el-button
            type="primary"
            :icon="MagicStick"
            :loading="generating"
            :disabled="!prd.trim()"
            @click="handleGenerate"
          >
            AI 生成
          </el-button>
        </div>
      </el-card>

      <!-- Generated diagrams: flowchart + test-point mind map -->
      <el-card v-if="hasGenerated && (flowchart || mindmap)" class="diagram-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="result-title">流程图 & 测试点脑图</span>
            <div class="diagram-actions">
              <el-button text :icon="Picture" :loading="exporting" @click="exportActivePng">
                导出 PNG
              </el-button>
              <el-button text :icon="CopyDocument" @click="copyActiveDiagram">复制源码</el-button>
              <el-button text :icon="Download" @click="downloadActiveDiagram">下载 .mmd</el-button>
            </div>
          </div>
        </template>
        <el-tabs v-model="activeDiagram">
          <el-tab-pane label="业务流程图" name="flowchart">
            <MermaidDiagram v-if="flowchart" ref="flowchartRef" :code="flowchart" />
            <el-empty v-else description="未生成流程图" :image-size="80" />
          </el-tab-pane>
          <el-tab-pane label="测试点脑图" name="mindmap">
            <MermaidDiagram v-if="mindmap" ref="mindmapRef" :code="mindmap" />
            <el-empty v-else description="未生成脑图" :image-size="80" />
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <!-- Generated cases -->
      <el-card v-if="hasGenerated" class="result-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div class="result-title">
              <span>生成结果（{{ generatedCases.length }} 条）</span>
              <el-tag
                v-if="engine"
                :type="engine === 'claude' ? 'success' : 'info'"
                effect="dark"
                size="small"
              >
                {{ engineLabel }}
              </el-tag>
            </div>
            <el-button
              type="success"
              :icon="DocumentAdd"
              :loading="saving"
              :disabled="selectedRows.length === 0"
              @click="handleSave"
            >
              保存选中用例（{{ selectedRows.length }}）
            </el-button>
          </div>
        </template>

        <el-table
          ref="tableRef"
          :data="generatedCases"
          stripe
          border
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="preconditions" label="前置条件" min-width="180">
            <template #default="{ row }">
              <span class="cell-text">{{ (row as GenCase).preconditions || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="steps" label="步骤" min-width="240">
            <template #default="{ row }">
              <span class="cell-text">{{ (row as GenCase).steps || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="expected" label="预期结果" min-width="200">
            <template #default="{ row }">
              <span class="cell-text">{{ (row as GenCase).expected || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-if="generatedCases.length === 0"
          description="未生成任何用例"
          :image-size="100"
        />
      </el-card>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type TableInstance } from 'element-plus'
import {
  User,
  ArrowDown,
  SwitchButton,
  MagicStick,
  DocumentAdd,
  CopyDocument,
  Download,
  Picture,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { genCases, saveCases } from '@/api/ai'
import NotificationBell from '@/components/NotificationBell.vue'
import AppNav from '@/components/AppNav.vue'
import MermaidDiagram from '@/components/MermaidDiagram.vue'
import type { AiEngine, GenCase } from '@/types'

const PROJECT_ID = 1

const router = useRouter()
const userStore = useUserStore()

// ---- Generate ----
const prd = ref('')
const generating = ref(false)
const engine = ref<AiEngine | null>(null)
const generatedCases = ref<GenCase[]>([])
const hasGenerated = ref(false)

// ---- Generated diagrams (Mermaid source) ----
const flowchart = ref<string>('')
const mindmap = ref<string>('')
const activeDiagram = ref<'flowchart' | 'mindmap'>('flowchart')

const engineLabel = computed(() =>
  engine.value === 'claude' ? 'claude · AI 模型' : 'heuristic · 启发式兜底',
)

async function handleGenerate() {
  const text = prd.value.trim()
  if (!text) return
  generating.value = true
  try {
    const result = await genCases({ projectId: PROJECT_ID, prd: text })
    engine.value = result.engine
    generatedCases.value = result.cases ?? []
    flowchart.value = result.flowchart ?? ''
    mindmap.value = result.mindmap ?? ''
    activeDiagram.value = flowchart.value ? 'flowchart' : 'mindmap'
    hasGenerated.value = true
    // Default to selecting every generated case.
    await nextTick()
    toggleAllSelection(true)
  } catch {
    // Interceptor already showed an error.
  } finally {
    generating.value = false
  }
}

// ---- Diagram actions ----
const flowchartRef = ref<InstanceType<typeof MermaidDiagram>>()
const mindmapRef = ref<InstanceType<typeof MermaidDiagram>>()
const exporting = ref(false)

function currentDiagram(): { code: string; name: string } {
  return activeDiagram.value === 'flowchart'
    ? { code: flowchart.value, name: 'flowchart' }
    : { code: mindmap.value, name: 'mindmap' }
}

async function exportActivePng() {
  const { name } = currentDiagram()
  const target = activeDiagram.value === 'flowchart' ? flowchartRef.value : mindmapRef.value
  if (!target) return
  exporting.value = true
  try {
    const ok = await target.exportPng(name)
    if (ok) ElMessage.success('已导出 PNG')
    else ElMessage.warning('当前没有可导出的图')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

async function copyActiveDiagram() {
  const { code } = currentDiagram()
  if (!code) return
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('已复制 Mermaid 源码')
  } catch {
    ElMessage.warning('复制失败')
  }
}

function downloadActiveDiagram() {
  const { code, name } = currentDiagram()
  if (!code) return
  const blob = new Blob([code], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${name}.mmd`
  a.click()
  URL.revokeObjectURL(url)
}

// ---- Selection ----
const tableRef = ref<TableInstance>()
const selectedRows = ref<GenCase[]>([])

function handleSelectionChange(rows: GenCase[]) {
  selectedRows.value = rows
}

function toggleAllSelection(select: boolean) {
  for (const row of generatedCases.value) {
    tableRef.value?.toggleRowSelection(row, select)
  }
}

// ---- Save ----
const saving = ref(false)

async function handleSave() {
  if (selectedRows.value.length === 0) return
  saving.value = true
  try {
    const result = await saveCases({
      projectId: PROJECT_ID,
      cases: selectedRows.value.map((c) => ({
        title: c.title,
        preconditions: c.preconditions,
        steps: c.steps,
        expected: c.expected,
      })),
    })
    const created = result?.created ?? 0
    try {
      await ElMessageBox.confirm(`已入库 ${created} 条用例，是否前往用例页查看？`, '保存成功', {
        confirmButtonText: '前往用例页',
        cancelButtonText: '留在本页',
        type: 'success',
      })
      router.push('/testcases')
    } catch {
      // User chose to stay on the current page.
      ElMessage.success(`已入库 ${created} 条`)
    }
  } catch {
    // Interceptor already showed an error.
  } finally {
    saving.value = false
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
</script>

<style scoped>
.ai-cases-page {
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

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
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

.result-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  color: #e7e9f3;
}

.prd-card {
  margin-bottom: 16px;
}

.diagram-card {
  margin-bottom: 16px;
}

.diagram-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.prd-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.cell-text {
  white-space: pre-wrap;
  color: #e7e9f3;
  font-size: 13px;
  line-height: 1.5;
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
}
</style>
