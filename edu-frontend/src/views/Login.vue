<template>
  <div class="login-page">
    <div class="login-glow" aria-hidden="true"></div>
    <div class="login-glitch-lines" aria-hidden="true"></div>

    <div class="login-card">
      <div class="card-corner tl"></div>
      <div class="card-corner tr"></div>
      <div class="card-corner bl"></div>
      <div class="card-corner br"></div>

      <div class="brand-area">
        <div class="brand-logo">
          <svg width="48" height="48" viewBox="0 0 80 80" fill="none">
            <defs>
              <linearGradient id="loginLogoGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#4080FF"/>
                <stop offset="100%" stop-color="#8060E8"/>
              </linearGradient>
            </defs>
            <rect x="6" y="6" width="68" height="68" rx="18"
              fill="url(#loginLogoGrad)" opacity="0.15"/>
            <rect x="6" y="6" width="68" height="68" rx="18"
              fill="none" stroke="url(#loginLogoGrad)" stroke-width="1.5"/>
            <text x="50%" y="55%" dominant-baseline="middle" text-anchor="middle"
              fill="url(#loginLogoGrad)" font-size="26" font-weight="800" font-family="Outfit,sans-serif">OP</text>
          </svg>
        </div>
        <div class="brand-info">
          <div class="brand-zh">西安鸥鹏科技</div>
          <div class="brand-en">INTELLIGENT EDUCATION SYSTEM</div>
        </div>
      </div>

      <div class="card-divider">
        <span class="divider-line"></span>
        <span class="divider-text">智慧教务管理平台</span>
        <span class="divider-line"></span>
      </div>

      <div class="form-area">
        <h2 class="welcome-title">欢迎回来</h2>
        <p class="welcome-sub">请登录以继续使用系统</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="USERNAME / ID"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="PASSWORD"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item class="remember-item">
            <el-checkbox v-model="rememberMe">
              <span class="cyber-checkbox-label">记住登录状态</span>
            </el-checkbox>
          </el-form-item>

          <el-form-item>
            <button
              class="login-btn"
              :class="{ loading }"
              :disabled="loading"
              @click.prevent="handleLogin"
            >
              <span class="btn-text">{{ loading ? '登录中...' : '登录' }}</span>
            </button>
          </el-form-item>
          <div class="register-entry">
            <span>学生还没有账号？</span>
            <el-button link type="primary" @click="router.push('/register')">自主注册</el-button>
          </div>
        </el-form>
      </div>

      <p class="card-footer">© 2025 西安鸥鹏科技有限公司</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getRoleHome } from '@/router'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)
const form = ref({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码',  trigger: 'blur' }]
}

onMounted(() => {
  const saved = localStorage.getItem('rememberedUser')
  if (saved) {
    const { username } = JSON.parse(saved)
    form.value.username = username
    rememberMe.value = true
  }
})

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form.value.username, form.value.password)
    if (rememberMe.value) {
      localStorage.setItem('rememberedUser', JSON.stringify({ username: form.value.username }))
    } else {
      localStorage.removeItem('rememberedUser')
    }
    const role = userStore.userInfo!.role
    router.push(getRoleHome(role))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  padding: 20px;
  position: relative;
}

/* Remove unused decorative elements */
.login-glow,
.login-glitch-lines { display: none; }

/* ── Card ── */
.login-card {
  width: 420px;
  background: rgba(12, 20, 40, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 40px 36px 32px;
  position: relative;
  animation: cardIn 0.5s cubic-bezier(0.22, 1, 0.36, 1);
  box-shadow:
    0 0 0 1px rgba(64, 128, 255, 0.08) inset,
    0 8px 40px rgba(0, 0, 0, 0.6),
    0 0 80px rgba(64, 128, 255, 0.06);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
}

/* Top accent line */
.login-card::before {
  content: '';
  position: absolute;
  top: 0; left: 24px; right: 24px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(64, 128, 255, 0.6), rgba(120, 80, 240, 0.4), transparent);
  border-radius: 1px;
}

/* Corner decorations - remove old ones */
.card-corner { display: none; }

@keyframes cardIn {
  from { opacity: 0; transform: translateY(24px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* ── Brand ── */
.brand-area {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
}

.brand-logo {
  flex-shrink: 0;
}

.brand-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.brand-zh {
  font-size: 17px;
  font-weight: 700;
  color: #E2E8F0;
  line-height: 1;
  letter-spacing: 0.5px;
}

.brand-en {
  font-size: 10px;
  letter-spacing: 2px;
  color: rgba(64, 128, 255, 0.5);
  font-weight: 500;
  text-transform: uppercase;
}

/* ── Divider ── */
.card-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: rgba(255, 255, 255, 0.06);
}

.divider-text {
  font-size: 11px;
  letter-spacing: 1px;
  color: rgba(148, 163, 184, 0.4);
  white-space: nowrap;
  font-weight: 500;
}

/* ── Form ── */
.welcome-title {
  font-size: 24px;
  font-weight: 700;
  color: #E2E8F0;
  margin: 0 0 6px;
  letter-spacing: 0.3px;
}

.welcome-sub {
  font-size: 13px;
  color: rgba(148, 163, 184, 0.55);
  margin: 0 0 28px;
  letter-spacing: 0.2px;
}

.register-entry {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: rgba(226, 232, 240, 0.62);
  font-size: 13px;
}

/* ── Login Button ── */
.login-btn {
  width: 100%;
  height: 46px;
  border: none;
  background: linear-gradient(135deg, #4080FF 0%, #5560E8 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  border-radius: 10px;
  transition: all 0.2s ease;
  box-shadow: 0 4px 20px rgba(64, 128, 255, 0.3);
  font-family: 'Outfit', sans-serif;
}

.login-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.08) 0%, transparent 60%);
  pointer-events: none;
}

.login-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #5590FF 0%, #6670F0 100%);
  box-shadow: 0 6px 28px rgba(64, 128, 255, 0.45);
  transform: translateY(-1px);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 12px rgba(64, 128, 255, 0.3);
}

.login-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-text {
  position: relative;
  z-index: 1;
}

/* ── Checkbox ── */
.cyber-checkbox-label {
  color: rgba(148, 163, 184, 0.7);
  font-size: 13px;
}

/* ── Footer ── */
.card-footer {
  text-align: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.15);
  margin: 24px 0 0;
  letter-spacing: 0.5px;
}
</style>
