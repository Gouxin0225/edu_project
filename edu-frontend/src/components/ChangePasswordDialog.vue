<template>
  <el-dialog
    v-model="visible"
    title="修改密码"
    width="400px"
    :show-close="!force"
    :close-on-click-modal="false"
    :close-on-press-escape="!force"
    @closed="handleClosed"
    class="cyberpunk-dialog"
  >
    <el-alert
      v-if="force"
      title="首次登录或密码重置后必须修改密码"
      type="warning"
      show-icon
      :closable="false"
      class="force-alert"
    />
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="90px"
    >
      <el-form-item label="原密码" prop="oldPassword" class="cyberpunk-form-item">
        <el-input
          v-model="form.oldPassword"
          type="password"
          placeholder="请输入原密码"
          show-password
          class="cyberpunk-input"
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword" class="cyberpunk-form-item">
        <el-input
          v-model="form.newPassword"
          type="password"
          placeholder="请输入新密码（至少8位，含字母和数字）"
          show-password
          class="cyberpunk-input"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword" class="cyberpunk-form-item">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
          show-password
          class="cyberpunk-input"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button v-if="!force" @click="visible = false" class="cancel-btn">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit" class="submit-btn">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const props = withDefaults(defineProps<{ visible: boolean; force?: boolean }>(), {
  force: false
})
const emit = defineEmits<{ 'update:visible': [value: boolean] }>()
const router = useRouter()
const userStore = useUserStore()

const visible = ref(props.visible)
watch(() => props.visible, (v) => { visible.value = v })
watch(visible, (v) => emit('update:visible', v))

const formRef = ref<FormInstance>()
const loading = ref(false)
const force = computed(() => props.force)

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirm = (_rule: any, value: string, callback: Function) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度必须为8-64位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码必须同时包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await request.put('/user/password', {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    visible.value = false
    userStore.clear()
    router.push('/login')
  } finally {
    loading.value = false
  }
}

function handleClosed() {
  formRef.value?.resetFields()
}
</script>

<style scoped>
.cyberpunk-dialog {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
}

.force-alert {
  margin-bottom: 16px;
}

:deep(.el-dialog) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 30px rgba(255, 16, 240, 0.15);
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border) !important;
}

:deep(.el-dialog__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

:deep(.el-dialog__body) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid var(--border) !important;
}

.cyberpunk-form-item :deep(.el-form-item__label) {
  color: var(--text-secondary) !important;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-input :deep(.el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  border-color: var(--border) !important;
  box-shadow: none !important;
}

.cyberpunk-input :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-input :deep(.el-input__inner::placeholder) {
  color: var(--text-muted) !important;
}

.cyberpunk-input :deep(.el-input__wrapper:focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

:deep(.el-input__suffix) {
  color: var(--text-secondary) !important;
}

.cancel-btn {
  background: transparent !important;
  border: 1px solid var(--text-muted) !important;
  color: var(--text-secondary) !important;
  font-family: 'JetBrains Mono', monospace;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px);
}

.cancel-btn:hover {
  border-color: #00ffff !important;
  color: #00ffff !important;
}

.submit-btn {
  background: transparent !important;
  border: 1px solid #ff10f0 !important;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px);
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.2);
}

.submit-btn:hover {
  background: #ff10f0 !important;
  color: #000 !important;
  box-shadow: 0 0 20px rgba(255, 16, 240, 0.5);
}
</style>
