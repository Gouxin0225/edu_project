<template>
  <div class="page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="header-bar">
          <span class="page-title">审计日志</span>
          <el-button :icon="Refresh" @click="fetchLogs">刷新</el-button>
        </div>
      </template>

      <div class="toolbar">
        <el-select v-model="query.action" clearable placeholder="全部操作" style="width: 180px" @change="handleSearch">
          <el-option label="创建用户" value="USER_CREATE" />
          <el-option label="批量导入用户" value="USER_IMPORT" />
          <el-option label="重置密码" value="USER_RESET_PASSWORD" />
          <el-option label="删除用户" value="USER_DELETE" />
          <el-option label="创建班级" value="CLASS_CREATE" />
          <el-option label="分配班级人员" value="CLASS_ASSIGN_USER" />
          <el-option label="移除班级人员" value="CLASS_REMOVE_USER" />
        </el-select>
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索操作人或详情"
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      </div>

      <el-table :data="records" v-loading="loading" stripe border class="audit-table">
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作人" width="160">
          <template #default="{ row }">
            <div>{{ row.operatorRealName || '-' }}</div>
            <small class="muted">{{ row.operatorUsername || '-' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="90">
          <template #default="{ row }">{{ roleLabel(row.operatorRole) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="对象类型" width="120" />
        <el-table-column prop="targetId" label="对象ID" width="100" />
        <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
        <el-table-column prop="clientIp" label="IP" width="140" />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchLogs"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { getAuditLogs, type AuditLogRecord } from '@/api/admin/audit'

const loading = ref(false)
const records = ref<AuditLogRecord[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 20,
  action: '',
  keyword: ''
})

async function fetchLogs() {
  loading.value = true
  try {
    const res = await getAuditLogs({
      page: query.page,
      size: query.size,
      action: query.action || undefined,
      keyword: query.keyword || undefined
    })
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载审计日志失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  fetchLogs()
}

function handleSizeChange() {
  query.page = 1
  fetchLogs()
}

function formatTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function roleLabel(role?: string | null) {
  const map: Record<string, string> = {
    ADMIN: '管理员',
    TEACHER: '教师',
    ASSISTANT: '班主任',
    STUDENT: '学生'
  }
  return role ? map[role] || role : '-'
}

function actionLabel(action: string) {
  const map: Record<string, string> = {
    USER_CREATE: '创建用户',
    USER_IMPORT: '批量导入用户',
    USER_RESET_PASSWORD: '重置密码',
    USER_DELETE: '删除用户',
    CLASS_CREATE: '创建班级',
    CLASS_ASSIGN_USER: '分配班级人员',
    CLASS_REMOVE_USER: '移除班级人员'
  }
  return map[action] || action
}

onMounted(fetchLogs)
</script>

<style scoped>
.page {
  padding: 0;
}

.main-card {
  border: 1px solid var(--border);
}

.header-bar,
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar {
  justify-content: flex-start;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
}

.muted {
  color: var(--text-secondary);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
