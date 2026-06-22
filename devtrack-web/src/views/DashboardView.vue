<template>
  <div class="dashboard-page">
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
      <!-- Stat cards -->
      <div class="stats-row">
        <el-card class="stat-card" shadow="never">
          <el-statistic title="缺陷总数" :value="overview?.total ?? 0" />
        </el-card>
        <el-card class="stat-card" shadow="never">
          <el-statistic title="未关闭数" :value="overview?.openCount ?? 0" />
        </el-card>
        <el-card class="stat-card" shadow="never">
          <el-statistic title="完成率" :value="completionRate" suffix="%" />
        </el-card>
      </div>

      <!-- Charts -->
      <div class="charts-row">
        <el-card class="chart-card" shadow="never">
          <template #header><span class="card-title">状态分布</span></template>
          <div v-if="hasStatusData" class="chart-wrap">
            <VChart class="chart" :option="statusPieOption" autoresize />
          </div>
          <el-empty v-else description="暂无数据" :image-size="80" />
        </el-card>

        <el-card class="chart-card" shadow="never">
          <template #header><span class="card-title">优先级分布</span></template>
          <div v-if="hasPriorityData" class="chart-wrap">
            <VChart class="chart" :option="priorityBarOption" autoresize />
          </div>
          <el-empty v-else description="暂无数据" :image-size="80" />
        </el-card>
      </div>

      <!-- SLA overdue panel -->
      <el-card class="sla-card" shadow="never">
        <template #header>
          <div class="sla-header">
            <span class="card-title">SLA 超期预警</span>
            <el-tag v-if="slaItems.length" type="danger" effect="dark" size="small">
              {{ slaItems.length }} 项超期
            </el-tag>
          </div>
        </template>
        <el-table
          v-if="slaItems.length"
          v-loading="slaLoading"
          :data="slaItems"
          stripe
          border
          style="width: 100%"
        >
          <el-table-column prop="displayKey" label="编号" width="130">
            <template #default="{ row }">
              <span class="display-key">{{ (row as SlaItem).displayKey }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column label="优先级" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="PRIORITY_TAG_TYPE[(row as SlaItem).priority]" effect="dark">
                {{ PRIORITY_LABEL[(row as SlaItem).priority] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="STATUS_TAG_TYPE[(row as SlaItem).statusCode]" effect="light">
                {{ STATUS_LABEL[(row as SlaItem).statusCode] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            label="超期天数"
            width="120"
            align="center"
            sortable
            sort-by="overdueDays"
          >
            <template #default="{ row }">
              <span class="overdue-days">+{{ (row as SlaItem).overdueDays }} 天</span>
            </template>
          </el-table-column>
          <el-table-column prop="slaDays" label="SLA 天数" width="110" align="center">
            <template #default="{ row }">{{ (row as SlaItem).slaDays }} 天</template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无超期" :image-size="80" />
      </el-card>

      <!-- Recent activities -->
      <el-card class="activity-card" shadow="never">
        <template #header><span class="card-title">最近动态</span></template>
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
        <el-empty v-else description="暂无动态" :image-size="80" />
      </el-card>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { User, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsOption } from 'echarts'
import VChart from 'vue-echarts'
import { useUserStore } from '@/store/user'
import { getDashboardOverview } from '@/api/dashboard'
import { getSlaOverdue } from '@/api/sla'
import type {
  ActivityDto,
  DashboardOverviewResult,
  Priority,
  SlaItem,
  StatusCode,
} from '@/types'
import {
  STATUS_LABEL,
  STATUS_TAG_TYPE,
  PRIORITY_LABEL,
  PRIORITY_TAG_TYPE,
  activityActionLabel,
  formatTime,
} from '@/utils/enums'
import NotificationBell from '@/components/NotificationBell.vue'
import AppNav from '@/components/AppNav.vue'

use([
  CanvasRenderer,
  PieChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
])

const PROJECT_ID = 1

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const overview = ref<DashboardOverviewResult | null>(null)

const activities = computed<ActivityDto[]>(() =>
  (overview.value?.recentActivities ?? [])
    .slice()
    .sort((a, b) => b.createTime - a.createTime),
)

const completionRate = computed(() => {
  const total = overview.value?.total ?? 0
  const open = overview.value?.openCount ?? 0
  if (total <= 0) return 0
  return Math.round(((total - open) / total) * 100)
})

const statusEntries = computed(() => Object.entries(overview.value?.byStatus ?? {}))
const priorityEntries = computed(() => Object.entries(overview.value?.byPriority ?? {}))

const hasStatusData = computed(() => statusEntries.value.some(([, v]) => v > 0))
const hasPriorityData = computed(() => priorityEntries.value.some(([, v]) => v > 0))

function statusLabel(code: string): string {
  return STATUS_LABEL[code as StatusCode] ?? code
}

function priorityLabel(code: string): string {
  return PRIORITY_LABEL[code as Priority] ?? code
}

const statusPieOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0, type: 'scroll' },
  series: [
    {
      name: '状态分布',
      type: 'pie',
      radius: ['40%', '68%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#0b0f1a', borderWidth: 2 },
      label: { formatter: '{b}\n{c}' },
      data: statusEntries.value.map(([key, value]) => ({
        name: statusLabel(key),
        value,
      })),
    },
  ],
}))

const priorityBarOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '4%', bottom: 10, top: 30, containLabel: true },
  xAxis: {
    type: 'category',
    data: priorityEntries.value.map(([key]) => priorityLabel(key)),
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    {
      name: '数量',
      type: 'bar',
      barWidth: '46%',
      itemStyle: { color: '#818cf8', borderRadius: [4, 4, 0, 0] },
      data: priorityEntries.value.map(([, value]) => value),
    },
  ],
}))

async function fetchOverview() {
  loading.value = true
  try {
    overview.value = await getDashboardOverview({ projectId: PROJECT_ID })
  } catch {
    // Interceptor already showed an error.
  } finally {
    loading.value = false
  }
}

// ---- SLA overdue ----
const slaItems = ref<SlaItem[]>([])
const slaLoading = ref(false)

async function fetchSlaOverdue() {
  slaLoading.value = true
  try {
    const result = await getSlaOverdue({ projectId: PROJECT_ID })
    // Show the most overdue first.
    slaItems.value = (result?.items ?? []).slice().sort((a, b) => b.overdueDays - a.overdueDays)
  } catch {
    // Interceptor already showed an error.
  } finally {
    slaLoading.value = false
  }
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

onMounted(() => {
  fetchOverview()
  fetchSlaOverdue()
})
</script>

<style scoped>
.dashboard-page {
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

.card-title {
  font-weight: 600;
  color: #e7e9f3;
}

.sla-card {
  margin-bottom: 16px;
}

.sla-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.display-key {
  font-family: monospace;
  font-weight: 700;
  color: #818cf8;
}

.overdue-days {
  color: #f56c6c;
  font-weight: 700;
}

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 160px;
}

.charts-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.chart-card {
  flex: 1;
  min-width: 320px;
}

.chart-wrap {
  height: 320px;
}

.chart {
  width: 100%;
  height: 100%;
}

.activity-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.activity-detail {
  margin-top: 4px;
  color: #9298b4;
  font-size: 13px;
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

  .chart-card {
    min-width: 100%;
  }

  .chart-wrap {
    height: 260px;
  }
}
</style>
