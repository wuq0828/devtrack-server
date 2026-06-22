<template>
  <div class="burndown-chart">
    <div v-if="!hasData" class="chart-empty">
      <el-empty :description="emptyText" />
    </div>
    <VChart v-else class="chart" :option="chartOption" autoresize />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsOption } from 'echarts'
import VChart from 'vue-echarts'
import type { BurndownResult } from '@/types'

use([
  CanvasRenderer,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
])

const props = defineProps<{
  data: BurndownResult | null
  emptyText?: string
}>()

const emptyText = computed(() => props.emptyText || '暂无燃尽数据')

const hasData = computed(() => (props.data?.points?.length ?? 0) > 0)

// Ideal line: linear decline from `total` down to 0 across the available points.
const idealSeries = computed<number[]>(() => {
  const points = props.data?.points ?? []
  const total = props.data?.total ?? 0
  const n = points.length
  if (n === 0) return []
  if (n === 1) return [total]
  const step = total / (n - 1)
  return points.map((_, i) => Number(Math.max(total - step * i, 0).toFixed(2)))
})

const chartOption = computed<EChartsOption>(() => {
  const points = props.data?.points ?? []
  return {
    title: {
      text: props.data?.iterationName ?? '燃尽图',
      left: 'center',
    },
    tooltip: {
      trigger: 'axis',
    },
    legend: {
      data: ['实际剩余', '理想线'],
      bottom: 0,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: 40,
      top: 50,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((p) => p.date),
    },
    yAxis: {
      type: 'value',
      name: '剩余缺陷数',
      minInterval: 1,
    },
    series: [
      {
        name: '实际剩余',
        type: 'line',
        smooth: false,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2 },
        itemStyle: { color: '#f56c6c' },
        data: points.map((p) => p.remaining),
      },
      {
        name: '理想线',
        type: 'line',
        smooth: false,
        symbol: 'none',
        lineStyle: { width: 2, type: 'dashed' },
        itemStyle: { color: '#909399' },
        data: idealSeries.value,
      },
    ],
  }
})
</script>

<style scoped>
.burndown-chart {
  width: 100%;
  height: 100%;
  min-height: 360px;
  display: flex;
}

.chart {
  width: 100%;
  height: 100%;
  min-height: 360px;
}

.chart-empty {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
