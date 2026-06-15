<template>
  <div class="classes-container">
    <!-- Hero Section -->
    <header class="page-hero">
      <div class="hero-content">
        <div class="hero-kicker">JOIN OUR COMMUNITY</div>
        <h1>加入班级</h1>
        <p>申请加入正在开课的班级，开启你的智能学习之旅。班主任审核通过后即可解锁课程与挑战。</p>
      </div>
      <div class="hero-actions">
        <el-button
          class="cyber-btn-refresh"
          :icon="Refresh"
          circle
          @click="loadData"
          :loading="loading"
        />
      </div>
      <div class="hero-visual">
        <el-icon><School /></el-icon>
      </div>
    </header>

    <!-- Class Grid -->
    <section class="class-grid" v-loading="loading">
      <div
        v-for="item in classes"
        :key="item.classId"
        class="class-card-wrapper"
      >
        <div class="class-card" :class="{ 'is-joined': item.joined }">
          <!-- Card Header -->
          <div class="card-header">
            <div class="class-info">
              <el-tag size="small" effect="plain" class="school-tag">
                {{ item.schoolName || '通用班级' }}
              </el-tag>
              <h3 class="class-name">{{ item.className }}</h3>
            </div>
            <div class="status-indicator">
              <span class="status-dot" :class="statusTagType(item)"></span>
              <span class="status-text">{{ statusText(item) }}</span>
            </div>
          </div>

          <!-- Card Body -->
          <div class="card-body">
            <div class="meta-item">
              <div class="meta-label">
                <el-icon><User /></el-icon>
                <span>主讲讲师</span>
              </div>
              <div class="meta-value">{{ item.teacherNames?.join('、') || '暂未分配' }}</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">
                <el-icon><Avatar /></el-icon>
                <span>负责班主任</span>
              </div>
              <div class="meta-value">{{ item.assistantNames?.join('、') || '暂未分配' }}</div>
            </div>

            <!-- Reject Message -->
            <div v-if="item.applicationStatus === 'REJECTED' && item.rejectReason" class="reject-alert">
              <el-icon><Warning /></el-icon>
              <span>{{ item.rejectReason }}</span>
            </div>
          </div>

          <!-- Card Footer -->
          <div class="card-footer">
            <el-button
              class="action-btn"
              :type="getBtnType(item)"
              :disabled="!canApply(item)"
              :loading="applyingId === item.classId"
              @click="apply(item)"
              round
            >
              {{ actionText(item) }}
            </el-button>
          </div>
        </div>
      </div>

      <el-empty
        v-if="!loading && classes.length === 0"
        description="暂无可申请班级"
        class="cyber-empty"
      >
        <template #image>
          <el-icon :size="80"><CircleClose /></el-icon>
        </template>
      </el-empty>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Refresh, School, User, Avatar, Warning, CircleClose } from '@element-plus/icons-vue'
import { applyJoinClass, getAvailableClasses, type StudentClassItem } from '@/api/student'

const loading = ref(false)
const applyingId = ref<number | null>(null)
const classes = ref<StudentClassItem[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await getAvailableClasses()
    classes.value = res.data || []
  } finally {
    loading.value = false
  }
}

function canApply(item: StudentClassItem) {
  return !item.joined && item.applicationStatus !== 'PENDING' && item.allowStudentApply
}

function actionText(item: StudentClassItem) {
  if (item.joined) return '已进入班级'
  if (item.applicationStatus === 'PENDING') return '审核处理中'
  if (item.applicationStatus === 'REJECTED') return '重新提交申请'
  return '立即申请加入'
}

function statusText(item: StudentClassItem) {
  if (item.joined) return '已加入'
  if (item.applicationStatus === 'PENDING') return '待审核'
  if (item.applicationStatus === 'REJECTED') return '未通过'
  return '开放中'
}

function statusTagType(item: StudentClassItem) {
  if (item.joined) return 'success'
  if (item.applicationStatus === 'PENDING') return 'warning'
  if (item.applicationStatus === 'REJECTED') return 'danger'
  return 'primary'
}

function getBtnType(item: StudentClassItem) {
  if (item.joined) return 'success'
  if (item.applicationStatus === 'PENDING') return 'info'
  if (item.applicationStatus === 'REJECTED') return 'warning'
  return 'primary'
}

async function apply(item: StudentClassItem) {
  applyingId.value = item.classId
  try {
    await applyJoinClass(item.classId)
    ElMessage.success('加入申请已提交，请耐心等待审核')
    await loadData()
  } finally {
    applyingId.value = null
  }
}

onMounted(loadData)
</script>

<style scoped>
.classes-container {
  display: grid;
  gap: 24px;
  padding: 18px 22px 32px;
  animation: slideUp 0.4s ease-out;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* Hero Section */
.page-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto 120px;
  align-items: center;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 12px;
  background: 
    radial-gradient(circle at 10% 20%, rgba(64, 128, 255, 0.15), transparent 30%),
    linear-gradient(135deg, var(--bg-surface), var(--bg-base));
  border: 1px solid var(--border);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.page-hero::after {
  content: '';
  position: absolute;
  top: 0; right: 0;
  width: 300px; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(0, 255, 255, 0.03));
  clip-path: polygon(20% 0, 100% 0, 100% 100%, 0% 100%);
  pointer-events: none;
}

.hero-kicker {
  font-size: 11px;
  font-weight: 800;
  color: #00ffff;
  letter-spacing: 1.5px;
  margin-bottom: 8px;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.4);
}

.hero-content h1 {
  margin: 0;
  font-size: 32px;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.hero-content p {
  margin: 12px 0 0;
  font-size: 14px;
  color: var(--text-secondary);
  max-width: 600px;
  line-height: 1.6;
}

.hero-visual {
  width: 100px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 255, 255, 0.05);
  border-radius: 10px;
  border: 1px solid rgba(0, 255, 255, 0.1);
  box-shadow: inset 0 0 20px rgba(0, 255, 255, 0.05);
}

.hero-visual .el-icon {
  font-size: 48px;
  color: #00ffff;
  filter: drop-shadow(0 0 8px rgba(0, 255, 255, 0.5));
}

.cyber-btn-refresh {
  background: transparent !important;
  border: 1px solid var(--border) !important;
  color: var(--text-secondary) !important;
}

.cyber-btn-refresh:hover {
  border-color: #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.2);
}

/* Grid Layout */
.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

/* Card Styling */
.class-card {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: hidden;
}

.class-card:hover {
  transform: translateY(-5px);
  border-color: rgba(0, 255, 255, 0.4);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.3), 0 0 20px rgba(0, 255, 255, 0.05);
}

.class-card.is-joined {
  border-color: rgba(57, 255, 20, 0.2);
}

.class-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; height: 3px;
  background: linear-gradient(90deg, #00ffff, #ff10f0);
  opacity: 0.6;
}

.card-header {
  padding: 22px 22px 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.school-tag {
  background: rgba(148, 163, 184, 0.1) !important;
  border-color: var(--border) !important;
  color: var(--text-muted) !important;
  margin-bottom: 10px;
}

.class-name {
  margin: 0;
  font-size: 20px;
  color: var(--text-primary);
  font-weight: 700;
  line-height: 1.3;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.2);
  padding: 4px 10px;
  border-radius: 20px;
  border: 1px solid var(--border);
}

.status-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  box-shadow: 0 0 6px currentColor;
}

.status-dot.success { color: #39ff14; background: #39ff14; }
.status-dot.warning { color: #ff9800; background: #ff9800; }
.status-dot.danger  { color: #ff10f0; background: #ff10f0; }
.status-dot.primary { color: #00ffff; background: #00ffff; }

.status-text {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.card-body {
  padding: 0 22px 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 12px;
}

.meta-label .el-icon {
  font-size: 14px;
  color: var(--text-secondary);
}

.meta-value {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}

.reject-alert {
  margin-top: 4px;
  padding: 10px 12px;
  background: rgba(255, 16, 240, 0.08);
  border: 1px solid rgba(255, 16, 240, 0.2);
  border-radius: 8px;
  display: flex;
  gap: 8px;
  color: #ff10f0;
  font-size: 12px;
  line-height: 1.5;
}

.reject-alert .el-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.card-footer {
  padding: 16px 22px 22px;
  border-top: 1px solid var(--border-subtle);
}

.action-btn {
  width: 100%;
  font-weight: 700;
  letter-spacing: 1px;
  transition: all 0.3s;
}

.action-btn:not(:disabled):hover {
  transform: scale(1.02);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
}

.cyber-empty {
  padding: 60px 0;
  grid-column: 1 / -1;
}

@media (max-width: 768px) {
  .page-hero {
    grid-template-columns: 1fr;
    padding: 24px;
  }
  .hero-visual, .hero-actions { display: none; }
}
</style>
