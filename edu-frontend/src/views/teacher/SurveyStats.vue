<template>
  <div class="stats-container">
    <div class="stats-header">
      <h2 class="cyber-title">{{ statistics.title }}</h2>
      <div class="total-count">
        <span class="label">填写人数</span>
        <span class="value">{{ statistics.totalCount || 0 }}</span>
      </div>
    </div>

    <div class="stats-content">
      <!-- NPS和雷达图 -->
      <el-row :gutter="20" class="top-row">
        <el-col :span="12">
          <el-card shadow="never" class="nps-card cyber-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">NPS 推荐指数</span>
              </div>
            </template>
            <div class="nps-content">
              <div class="nps-score">{{ npsScore }}</div>
              <div class="nps-desc">NPS Score</div>
              <div class="nps-details">
                <div class="nps-item promoter">
                  <span class="label">推荐者</span>
                  <span class="rate">{{ ((statistics.nps?.promoterRate || 0) * 100).toFixed(1) }}%</span>
                </div>
                <div class="nps-item passive">
                  <span class="label">被动者</span>
                  <span class="rate">{{ ((1 - (statistics.nps?.promoterRate || 0) - (statistics.nps?.detractorRate || 0)) * 100).toFixed(1) }}%</span>
                </div>
                <div class="nps-item detractor">
                  <span class="label">贬损者</span>
                  <span class="rate">{{ ((statistics.nps?.detractorRate || 0) * 100).toFixed(1) }}%</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never" class="radar-card cyber-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">多维度评分雷达图</span>
              </div>
            </template>
            <div ref="radarChartRef" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 题目统计 -->
      <el-row :gutter="20" class="questions-row">
        <el-col
          v-for="q in statistics.questions"
          :key="q.questionId"
          :span="12"
        >
          <el-card shadow="never" class="question-card cyber-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">{{ q.title }}</span>
                <el-tag size="small" class="cyber-tag">{{ getTypeName(q.type) }}</el-tag>
              </div>
            </template>

            <!-- 评分题显示柱状图 -->
            <div v-if="q.type !== 'TEXT' && q.type !== 'NPS'" class="question-content">
              <div class="avg-score">
                平均分：<span class="score-value">{{ q.avgScore?.toFixed(2) || '-' }}</span>
              </div>
              <div ref="barChartRefs" class="bar-chart-container" :data-qid="q.questionId"></div>
            </div>

            <!-- NPS显示特殊样式 -->
            <div v-if="q.type === 'NPS'" class="question-content">
              <div class="avg-score">
                平均分：<span class="score-value">{{ q.avgScore?.toFixed(2) || '-' }}</span>
              </div>
              <div class="nps-bar">
                <div
                  v-for="d in q.distribution"
                  :key="d.value"
                  class="nps-bar-item"
                  :style="{ width: (d.rate * 100) + '%' }"
                >
                  <span class="nps-value">{{ d.value }}</span>
                  <span class="nps-rate">{{ (d.rate * 100).toFixed(1) }}%</span>
                </div>
              </div>
            </div>

            <!-- 文本题显示回答列表 -->
            <div v-if="q.type === 'TEXT'" class="text-answers">
              <div v-if="q.textAnswers && q.textAnswers.length > 0" class="answer-list">
                <el-tag
                  v-for="(ans, idx) in q.textAnswers"
                  :key="idx"
                  class="answer-tag cyber-tag"
                >
                  {{ ans }}
                </el-tag>
              </div>
              <div v-else class="no-data">暂无回答</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { BarChart, RadarChart, type BarSeriesOption, type RadarSeriesOption } from 'echarts/charts'
import {
  GridComponent,
  RadarComponent,
  TooltipComponent,
  type GridComponentOption,
  type RadarComponentOption,
  type TooltipComponentOption
} from 'echarts/components'
import { graphic, init, use, type ComposeOption, type ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { getSurveyStatistics, type SurveyStatistics } from '@/api/survey'

use([BarChart, RadarChart, GridComponent, RadarComponent, TooltipComponent, CanvasRenderer])

type ECOption = ComposeOption<
  BarSeriesOption | RadarSeriesOption | GridComponentOption | RadarComponentOption | TooltipComponentOption
>

const props = defineProps<{
  surveyId: number
}>()

const loading = ref(false)
const statistics = reactive<SurveyStatistics>({
  surveyId: 0,
  title: '',
  totalCount: 0,
  nps: { score: 0, promoterRate: 0, detractorRate: 0 },
  radarData: [],
  questions: []
})

const radarChartRef = ref<HTMLElement>()
const barChartRefs = ref<HTMLElement[]>([])
let radarChart: ECharts | null = null
const barCharts: Map<number, ECharts> = new Map()

const npsScore = computed(() => {
  if (!statistics.nps) return 0
  return statistics.nps.score || 0
})

function getTypeName(type: string): string {
  const map: Record<string, string> = {
    STAR: '星级评分',
    NPS: 'NPS评分',
    SCALE: '量表评分',
    TEXT: '文本回答'
  }
  return map[type] || type
}

async function fetchStatistics() {
  loading.value = true
  try {
    const res = await getSurveyStatistics(props.surveyId)
    Object.assign(statistics, res.data)
    await nextTick()
    initRadarChart()
    initBarCharts()
  } catch (error: any) {
    ElMessage.error(error.message || '加载统计数据失败')
  } finally {
    loading.value = false
  }
}

function initRadarChart() {
  if (!radarChartRef.value || !statistics.radarData?.length) return

  if (radarChart) {
    radarChart.dispose()
  }

  radarChart = init(radarChartRef.value)

  const indicator = statistics.radarData.map(item => ({
    name: item.questionTitle.length > 10 ? item.questionTitle.substring(0, 10) + '...' : item.questionTitle,
    max: 5
  }))

  const values = statistics.radarData.map(item => item.avgScore || 0)

  const option: ECOption = {
    backgroundColor: '#0a0a0a',
    radar: {
      indicator,
      radius: '65%',
      axisName: {
        color: '#00ffff',
        fontFamily: 'JetBrains Mono'
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(0, 255, 255, 0.05)', 'rgba(0, 255, 255, 0.1)']
        }
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(0, 255, 255, 0.2)'
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(0, 255, 255, 0.3)'
        }
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '评分',
        areaStyle: {
          color: new graphic.RadialGradient(0.5, 0.5, 1, [
            { offset: 0, color: 'rgba(255, 16, 240, 0.6)' },
            { offset: 1, color: 'rgba(0, 255, 255, 0.1)' }
          ])
        },
        lineStyle: {
          color: '#ff10f0',
          width: 2
        },
        itemStyle: {
          color: '#ff10f0'
        }
      }]
    }]
  }

  radarChart.setOption(option)
}

function initBarCharts() {
  statistics.questions.forEach(q => {
    if (q.type === 'TEXT' || q.type === 'NPS') return

    const container = document.querySelector(`[data-qid="${q.questionId}"]`) as HTMLElement
    if (!container) return

    if (barCharts.has(q.questionId)) {
      barCharts.get(q.questionId)?.dispose()
    }

    const chart = init(container)
    barCharts.set(q.questionId, chart)

    const gradientColors = ['#00ffff', '#ff10f0', '#39ff14']

    const option: ECOption = {
      backgroundColor: '#0a0a0a',
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#0a0a0a',
        borderColor: '#1a1a2e',
        textStyle: {
          color: '#e0e0e0',
          fontFamily: 'JetBrains Mono'
        }
      },
      xAxis: {
        type: 'category',
        data: q.distribution?.map(d => d.value.toString()) || [],
        axisLabel: { color: '#00ffff', fontFamily: 'JetBrains Mono' },
        axisLine: { lineStyle: { color: '#1a1a2e' } }
      },
      yAxis: {
        type: 'value',
        max: Math.max(...(q.distribution?.map(d => d.rate * 100) || [100])),
        axisLabel: {
          formatter: '{value}%',
          color: '#00ffff',
          fontFamily: 'JetBrains Mono'
        },
        axisLine: { lineStyle: { color: '#1a1a2e' } },
        splitLine: { lineStyle: { color: 'rgba(26, 26, 46, 0.5)' } }
      },
      series: [{
        type: 'bar',
        data: q.distribution?.map((d, i) => ({
          value: (d.rate * 100).toFixed(1),
          itemStyle: {
            color: new graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: gradientColors[i % gradientColors.length] },
              { offset: 1, color: 'rgba(10, 10, 10, 0.8)' }
            ])
          }
        })) || [],
        barWidth: '50%',
        itemStyle: {
          borderRadius: [5, 5, 0, 0]
        }
      }],
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      }
    }

    chart.setOption(option)
  })
}

onMounted(() => {
  fetchStatistics()

  window.addEventListener('resize', () => {
    radarChart?.resize()
    barCharts.forEach(chart => chart.resize())
  })
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.stats-container {
  background: var(--bg-surface);
  min-height: 100%;
  padding: 20px;
  font-family: 'JetBrains Mono', monospace;
}

.stats-header {
  background: var(--bg-surface);
  padding: 20px;
  border: 1px solid var(--border);
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.1);
}

.cyber-title {
  margin: 0;
  color: #00ffff !important;
  font-weight: 700 !important;
  text-shadow: 0 0 15px rgba(0, 255, 255, 0.6) !important;
  letter-spacing: 2px !important;
}

.total-count {
  text-align: right;
}

.total-count .label {
  display: block;
  color: var(--text-secondary) !important;
  font-size: 14px;
}

.total-count .value {
  display: block;
  font-size: 32px;
  font-weight: 700;
  color: #ff10f0 !important;
  text-shadow: 0 0 15px rgba(255, 16, 240, 0.6) !important;
}

.stats-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.top-row, .questions-row {
  width: 100%;
}

.cyber-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px));
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.08), inset 0 0 40px rgba(0, 0, 0, 0.5) !important;
  height: 100%;
}

.cyber-card :deep(.el-card__header) {
  background: rgba(26, 26, 46, 0.3) !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 12px 15px !important;
}

.cyber-card :deep(.el-card__body) {
  background: var(--bg-surface) !important;
  padding: 15px !important;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  color: #00ffff !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  text-shadow: 0 0 8px rgba(0, 255, 255, 0.4) !important;
}

.nps-card, .radar-card, .question-card {
  height: 100%;
}

.nps-content {
  text-align: center;
  padding: 20px 0;
}

.nps-score {
  font-size: 72px;
  font-weight: 700;
  color: #ff10f0 !important;
  line-height: 1;
  text-shadow: 0 0 30px rgba(255, 16, 240, 0.6) !important;
}

.nps-desc {
  color: var(--text-secondary) !important;
  font-size: 14px;
  margin-top: 10px;
}

.nps-details {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-top: 30px;
}

.nps-item {
  text-align: center;
}

.nps-item .label {
  display: block;
  color: var(--text-secondary) !important;
  font-size: 13px;
  margin-bottom: 5px;
}

.nps-item .rate {
  display: block;
  font-size: 20px;
  font-weight: 600;
}

.nps-item.promoter .rate {
  color: #39ff14 !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.5) !important;
}

.nps-item.passive .rate {
  color: #ff9800 !important;
  text-shadow: 0 0 10px rgba(255, 152, 0, 0.5) !important;
}

.nps-item.detractor .rate {
  color: #ff4444 !important;
  text-shadow: 0 0 10px rgba(255, 68, 68, 0.5) !important;
}

.chart-container {
  height: 280px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
}

.bar-chart-container {
  height: 200px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
}

.question-content {
  padding: 10px 0;
}

.avg-score {
  font-size: 16px;
  color: var(--text-primary) !important;
  margin-bottom: 15px;
  font-family: 'JetBrains Mono', monospace;
}

.score-value {
  font-size: 28px;
  font-weight: 600;
  color: #00ffff !important;
  text-shadow: 0 0 15px rgba(0, 255, 255, 0.5) !important;
}

.nps-bar {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.nps-bar-item {
  display: flex;
  align-items: center;
  background: linear-gradient(90deg, rgba(0, 255, 255, 0.8), rgba(255, 16, 240, 0.8)) !important;
  color: var(--text-primary);
  padding: 8px 12px;
  min-width: 40px;
  clip-path: polygon(5px 0, 100% 0, 100% calc(100% - 5px), calc(100% - 5px) 100%, 0 100%, 0 5px) !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
}

.nps-value {
  font-weight: 600;
  margin-right: 10px;
}

.nps-rate {
  font-size: 13px;
}

.text-answers {
  padding: 10px 0;
}

.answer-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.answer-tag {
  margin: 0;
}

.no-data {
  color: var(--text-secondary) !important;
  text-align: center;
  padding: 20px;
  font-family: 'JetBrains Mono', monospace;
}

.cyber-tag {
  font-family: 'JetBrains Mono', monospace !important;
  border: 1px solid currentColor !important;
  background: transparent !important;
  clip-path: polygon(5px 0, 100% 0, 100% calc(100% - 5px), calc(100% - 5px) 100%, 0 100%, 0 5px) !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.3) !important;
}
</style>
