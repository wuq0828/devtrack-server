<template>
  <div class="board-page">
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

    <main class="content" v-loading="loading">
      <!-- Stats cards -->
      <div class="stats-row">
        <el-card class="stat-card" shadow="never">
          <el-statistic title="缺陷总数" :value="stats?.total ?? 0" />
        </el-card>
        <el-card
          v-for="opt in PRIORITY_OPTIONS"
          :key="opt.value"
          class="stat-card"
          shadow="never"
        >
          <el-statistic :title="opt.label" :value="stats?.byPriority?.[opt.value] ?? 0">
            <template #suffix>
              <el-tag
                :type="PRIORITY_TAG_TYPE[opt.value]"
                effect="dark"
                size="small"
                class="stat-tag"
              >
                {{ opt.value }}
              </el-tag>
            </template>
          </el-statistic>
        </el-card>
      </div>

      <!-- Board columns -->
      <div class="board-columns">
        <section
          v-for="col in columns"
          :key="col.statusCode"
          class="board-column"
        >
          <div class="column-header">
            <el-tag :type="STATUS_TAG_TYPE[col.statusCode]" effect="light">
              {{ col.statusName }}
            </el-tag>
            <span class="column-count">{{ col.count }}</span>
          </div>
          <div class="column-body">
            <el-card
              v-for="d in col.defects"
              :key="d.id"
              class="defect-card"
              shadow="hover"
              @click="openDetail(d.id)"
            >
              <div class="card-key">{{ d.displayKey }}</div>
              <div class="card-title">{{ d.title }}</div>
              <div class="card-tags">
                <el-tag :type="PRIORITY_TAG_TYPE[d.priority]" effect="dark" size="small">
                  {{ d.priority }}
                </el-tag>
                <el-tag :type="SEVERITY_TAG_TYPE[d.severity]" effect="plain" size="small">
                  {{ SEVERITY_LABEL[d.severity] }}
                </el-tag>
              </div>
            </el-card>
            <div v-if="col.defects.length === 0" class="column-empty">暂无缺陷</div>
          </div>
        </section>
      </div>
    </main>

    <DefectDetailDrawer
      v-model="drawerVisible"
      :defect-id="activeDefectId"
      @refresh="loadBoard"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { User, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getDefectBoard, getDefectStats } from '@/api/defect'
import type { BoardColumn, DefectStatsResult } from '@/types'
import {
  STATUS_TAG_TYPE,
  SEVERITY_LABEL,
  SEVERITY_TAG_TYPE,
  PRIORITY_TAG_TYPE,
  PRIORITY_OPTIONS,
} from '@/utils/enums'
import DefectDetailDrawer from '@/components/DefectDetailDrawer.vue'
import NotificationBell from '@/components/NotificationBell.vue'
import AppNav from '@/components/AppNav.vue'

const PROJECT_ID = 1

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const columns = ref<BoardColumn[]>([])
const stats = ref<DefectStatsResult | null>(null)

const drawerVisible = ref(false)
const activeDefectId = ref<number | null>(null)

async function loadBoard() {
  loading.value = true
  try {
    const [board, statsResult] = await Promise.all([
      getDefectBoard({ projectId: PROJECT_ID }),
      getDefectStats({ projectId: PROJECT_ID }),
    ])
    columns.value = board.columns ?? []
    stats.value = statsResult
  } catch {
    // Interceptor already showed an error.
  } finally {
    loading.value = false
  }
}

function openDetail(defectId: number) {
  activeDefectId.value = defectId
  drawerVisible.value = true
}

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

onMounted(loadBoard)
</script>

<style scoped>
.board-page {
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

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 150px;
}

.stat-tag {
  margin-left: 6px;
}

.board-columns {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
  align-items: flex-start;
}

.board-column {
  flex: 0 0 280px;
  background: rgba(22,28,46,0.5);
  border-radius: 8px;
  padding: 10px;
  max-height: calc(100vh - 220px);
  display: flex;
  flex-direction: column;
}

.column-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 0 2px;
}

.column-count {
  font-weight: 700;
  color: #c2c6da;
  background: rgba(99, 102, 241, 0.18);
  border-radius: 10px;
  padding: 0 8px;
  font-size: 12px;
  line-height: 20px;
}

.column-body {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.defect-card {
  cursor: pointer;
}

.defect-card :deep(.el-card__body) {
  padding: 10px 12px;
}

.card-key {
  font-family: monospace;
  font-size: 12px;
  color: #818cf8;
  font-weight: 700;
  margin-bottom: 4px;
}

.card-title {
  font-size: 13px;
  color: #e7e9f3;
  line-height: 1.5;
  margin-bottom: 8px;
}

.card-tags {
  display: flex;
  gap: 6px;
}

.column-empty {
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
  padding: 20px 0;
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

  .board-column {
    flex: 0 0 240px;
    max-height: none;
  }
}
</style>
