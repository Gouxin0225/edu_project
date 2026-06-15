<template>
  <div class="login-page">
    <section class="hero-pane">
      <header class="brand-header">
        <div class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 64 64" role="img">
            <defs>
              <linearGradient id="brandMarkGradient" x1="8" y1="8" x2="56" y2="56">
                <stop offset="0" stop-color="#2563eb" />
                <stop offset="0.55" stop-color="#2f80ed" />
                <stop offset="1" stop-color="#18c6b8" />
              </linearGradient>
            </defs>
            <path d="M15 48 27 17c2-5 9-5 11 0l11 31h-9l-4-10H24l-4 10h-5Z" fill="url(#brandMarkGradient)" />
            <path d="M25 31h14l-4-10c-1-3-5-3-6 0l-4 10Z" fill="#fff" opacity=".92" />
            <path d="M18 48c7-12 17-18 31-18" fill="none" stroke="#14b8a6" stroke-width="6" stroke-linecap="round" />
          </svg>
        </div>
        <div>
          <h1>慧学助手</h1>
          <p>INTELLIGENT EDUCATION SYSTEM</p>
        </div>
      </header>

      <div class="hero-copy">
        <h2>智慧教育 · 赋能未来</h2>
        <p>科技驱动教育进步，数据成就教学智慧</p>
      </div>

      <div class="education-scene" aria-hidden="true">
        <div class="scene-grid"></div>
        <div class="dashboard-screen">
          <div class="screen-bar"></div>
          <div class="screen-content">
            <div class="chart-ring"></div>
            <div class="chart-lines">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
        <div class="book-stack">
          <div class="book book-top"></div>
          <div class="book book-middle"></div>
          <div class="book book-bottom"></div>
          <div class="cap">
            <div class="cap-top"></div>
            <div class="cap-base"></div>
            <div class="cap-string"></div>
          </div>
        </div>
        <div class="floating-icon icon-chart"><el-icon><TrendCharts /></el-icon></div>
        <div class="floating-icon icon-school"><el-icon><School /></el-icon></div>
        <div class="floating-icon icon-data"><el-icon><DataAnalysis /></el-icon></div>
      </div>

      <div class="feature-strip">
        <div class="feature-item">
          <el-icon><Lock /></el-icon>
          <div>
            <strong>安全可靠</strong>
            <span>多重防护体系</span>
          </div>
        </div>
        <div class="feature-item">
          <el-icon><TrendCharts /></el-icon>
          <div>
            <strong>智能高效</strong>
            <span>数据驱动决策</span>
          </div>
        </div>
        <div class="feature-item">
          <el-icon><CircleCheck /></el-icon>
          <div>
            <strong>便捷易用</strong>
            <span>简化教务管理</span>
          </div>
        </div>
      </div>
    </section>

    <main class="login-pane">
      <section class="login-card">
        <div class="card-accent"></div>
        <div class="card-brand">
          <div class="card-logo" aria-hidden="true">
            <svg viewBox="0 0 64 64">
              <path d="M15 48 27 17c2-5 9-5 11 0l11 31h-9l-4-10H24l-4 10h-5Z" fill="url(#brandMarkGradient)" />
              <path d="M25 31h14l-4-10c-1-3-5-3-6 0l-4 10Z" fill="#fff" opacity=".92" />
              <path d="M18 48c7-12 17-18 31-18" fill="none" stroke="#14b8a6" stroke-width="6" stroke-linecap="round" />
            </svg>
          </div>
          <div>
            <h2>慧学助手</h2>
            <p>INTELLIGENT EDUCATION SYSTEM</p>
          </div>
        </div>

        <div class="platform-label">
          <span></span>
          <em>智慧教务管理平台</em>
          <span></span>
        </div>

        <div class="welcome-block">
          <h3>欢迎回来</h3>
          <p>请登录以继续使用系统</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名 / 工号"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住登录状态</el-checkbox>
          </div>

          <button
            class="login-button"
            :class="{ loading }"
            :disabled="loading"
            @click.prevent="handleLogin"
          >
            <span v-if="loading" class="loader"></span>
            <span>{{ loading ? '登录中...' : '登录' }}</span>
          </button>
        </el-form>

        <div class="register-row">
          <span>学生还没有账号？</span>
          <el-button plain type="primary" @click="router.push('/register')">自主注册</el-button>
        </div>

        <footer>© 2025 慧学助手</footer>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { CircleCheck, DataAnalysis, Lock, School, TrendCharts, User } from '@element-plus/icons-vue'
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
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
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
  if (loading.value) return
  loading.value = true
  try {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return
    await userStore.login(form.value.username, form.value.password)
    if (rememberMe.value) {
      localStorage.setItem('rememberedUser', JSON.stringify({ username: form.value.username }))
    } else {
      localStorage.removeItem('rememberedUser')
    }
    const role = userStore.userInfo!.role
    await router.push(getRoleHome(role))
    if (userStore.userInfo?.mustChangePassword) {
      window.setTimeout(() => {
        window.dispatchEvent(new CustomEvent('password-change-required'))
      }, 0)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(430px, .88fr);
  background:
    linear-gradient(116deg, rgba(230, 240, 255, .96) 0%, rgba(247, 251, 255, .98) 48%, #f9fbff 100%);
  color: #0b1f44;
  overflow: hidden;
}

.hero-pane {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 58px 68px 56px;
  overflow: hidden;
}

.hero-pane::before,
.hero-pane::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

.hero-pane::before {
  width: 980px;
  height: 520px;
  left: 60px;
  top: 92px;
  border: 1px solid rgba(130, 166, 220, .26);
  border-color: rgba(130, 166, 220, .24) transparent transparent rgba(130, 166, 220, .16);
  border-radius: 50%;
  transform: rotate(-15deg);
}

.hero-pane::after {
  inset: auto 0 0 0;
  height: 42%;
  background: linear-gradient(0deg, rgba(255, 255, 255, .72), rgba(255, 255, 255, 0));
}

.brand-header,
.hero-copy,
.education-scene,
.feature-strip {
  position: relative;
  z-index: 1;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 18px;
}

.brand-mark {
  width: 64px;
  height: 64px;
  flex: 0 0 auto;
}

.brand-mark svg,
.card-logo svg {
  width: 100%;
  height: 100%;
  display: block;
}

.brand-header h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.15;
  font-weight: 800;
  color: #061a3d;
}

.brand-header p {
  margin: 9px 0 0;
  font-size: 14px;
  color: #2e4d7d;
  font-weight: 700;
  letter-spacing: 2px;
}

.hero-copy {
  margin-top: 78px;
}

.hero-copy h2 {
  margin: 0;
  font-size: 42px;
  line-height: 1.2;
  font-weight: 800;
  color: #071b42;
}

.hero-copy p {
  margin: 22px 0 0;
  font-size: 20px;
  line-height: 1.5;
  color: #48638c;
}

.education-scene {
  flex: 1;
  min-height: 390px;
  margin-top: 18px;
}

.scene-grid {
  position: absolute;
  left: 0;
  right: 40px;
  bottom: 18px;
  height: 170px;
  background:
    linear-gradient(90deg, rgba(109, 154, 221, .15) 1px, transparent 1px),
    linear-gradient(0deg, rgba(109, 154, 221, .14) 1px, transparent 1px);
  background-size: 42px 42px;
  transform: perspective(620px) rotateX(63deg);
  transform-origin: bottom;
  opacity: .58;
}

.dashboard-screen {
  position: absolute;
  left: 140px;
  bottom: 88px;
  width: 300px;
  height: 210px;
  border: 13px solid rgba(184, 211, 249, .88);
  border-radius: 20px;
  background: rgba(255, 255, 255, .86);
  box-shadow: 0 28px 70px rgba(61, 111, 189, .24);
  transform: rotate(-5deg);
}

.screen-bar {
  height: 36px;
  border-bottom: 1px solid rgba(173, 195, 226, .58);
}

.screen-bar::before {
  content: '';
  display: block;
  width: 46px;
  height: 8px;
  margin: 14px 0 0 22px;
  border-radius: 999px;
  background: #3b82f6;
}

.screen-content {
  display: grid;
  grid-template-columns: 112px 1fr;
  gap: 24px;
  align-items: center;
  padding: 24px 22px;
}

.chart-ring {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  background:
    conic-gradient(#14c8bd 0 32%, #2e7cf6 32% 72%, #dce9ff 72% 100%);
  position: relative;
}

.chart-ring::after {
  content: '';
  position: absolute;
  inset: 23px;
  border-radius: 50%;
  background: #fff;
}

.chart-lines {
  display: flex;
  align-items: end;
  gap: 12px;
  height: 92px;
}

.chart-lines span {
  width: 22px;
  border-radius: 10px 10px 4px 4px;
  background: linear-gradient(180deg, #2f80ed, #86d9ff);
}

.chart-lines span:nth-child(1) { height: 42px; }
.chart-lines span:nth-child(2) { height: 72px; }
.chart-lines span:nth-child(3) { height: 58px; }

.book-stack {
  position: absolute;
  left: 362px;
  bottom: 48px;
  width: 300px;
  height: 230px;
}

.book {
  position: absolute;
  left: 18px;
  width: 260px;
  height: 42px;
  border-radius: 8px;
  box-shadow: 0 18px 36px rgba(44, 92, 178, .18);
}

.book-top {
  bottom: 116px;
  background: linear-gradient(180deg, #86f1e9, #35c7bd);
}

.book-middle {
  bottom: 72px;
  background: linear-gradient(180deg, #ffffff, #d9e9ff);
}

.book-bottom {
  bottom: 28px;
  background: linear-gradient(180deg, #4f97ff, #2363d8);
}

.cap {
  position: absolute;
  left: 42px;
  bottom: 135px;
  width: 230px;
  height: 120px;
}

.cap-top {
  position: absolute;
  left: 16px;
  top: 18px;
  width: 210px;
  height: 72px;
  background: linear-gradient(145deg, #448cff, #155ac9);
  clip-path: polygon(50% 0, 100% 30%, 50% 60%, 0 30%);
  box-shadow: 0 18px 30px rgba(24, 91, 208, .28);
}

.cap-base {
  position: absolute;
  left: 84px;
  top: 66px;
  width: 82px;
  height: 32px;
  border-radius: 0 0 36px 36px;
  background: linear-gradient(180deg, #2563eb, #0f46b6);
}

.cap-string {
  position: absolute;
  left: 164px;
  top: 62px;
  width: 2px;
  height: 58px;
  background: #1c55bb;
}

.cap-string::after {
  content: '';
  position: absolute;
  left: -5px;
  bottom: -8px;
  width: 12px;
  height: 18px;
  border-radius: 999px;
  background: #1c55bb;
}

.floating-icon {
  position: absolute;
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  color: #fff;
  font-size: 28px;
  background: linear-gradient(145deg, #2f80ed, #16c7bb);
  box-shadow: 0 18px 36px rgba(42, 122, 218, .24);
}

.icon-chart {
  left: 58px;
  bottom: 142px;
}

.icon-school {
  left: 574px;
  bottom: 286px;
}

.icon-data {
  left: 716px;
  bottom: 124px;
  width: 38px;
  height: 38px;
  font-size: 20px;
  border-radius: 10px;
}

.feature-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  max-width: 820px;
  min-height: 88px;
  margin-top: auto;
  padding: 14px 22px;
  border: 1px solid rgba(214, 225, 241, .76);
  border-radius: 20px;
  background: rgba(255, 255, 255, .72);
  box-shadow: 0 16px 48px rgba(50, 95, 155, .11);
}

.feature-item {
  display: grid;
  grid-template-columns: 48px 1fr;
  align-items: center;
  gap: 14px;
  padding: 0 18px;
}

.feature-item + .feature-item {
  border-left: 1px solid rgba(190, 207, 231, .72);
}

.feature-item .el-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  font-size: 27px;
  color: #2474ef;
  background: #f4f8ff;
}

.feature-item strong {
  display: block;
  font-size: 16px;
  color: #08204a;
}

.feature-item span {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: #607393;
}

.login-pane {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 62px 56px 24px;
}

.login-card {
  position: relative;
  width: min(100%, 560px);
  min-height: 720px;
  padding: 58px 66px 34px;
  border: 1px solid rgba(207, 219, 236, .8);
  border-radius: 20px;
  background: rgba(255, 255, 255, .9);
  box-shadow: 0 28px 70px rgba(40, 73, 124, .18);
  overflow: hidden;
}

.card-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 6px;
  background: linear-gradient(90deg, #2563eb, #21c8bb);
}

.card-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
}

.card-logo {
  width: 72px;
  height: 72px;
  padding: 10px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 16px 34px rgba(29, 88, 178, .13);
}

.card-brand h2 {
  margin: 0;
  font-size: 30px;
  line-height: 1.1;
  color: #061a3d;
}

.card-brand p {
  margin: 10px 0 0;
  color: #2563eb;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 1px;
}

.platform-label {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 18px;
  margin: 42px 0 44px;
  color: #6c7f9f;
  font-size: 17px;
}

.platform-label span {
  height: 1px;
  background: linear-gradient(90deg, transparent, #d4deed);
}

.platform-label span:last-child {
  background: linear-gradient(90deg, #d4deed, transparent);
}

.platform-label em {
  font-style: normal;
}

.welcome-block {
  margin-bottom: 28px;
}

.welcome-block h3 {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
  color: #071b42;
}

.welcome-block p {
  margin: 10px 0 0;
  font-size: 16px;
  color: #687b9e;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-input__wrapper) {
  height: 62px;
  padding: 0 18px;
  border-radius: 8px;
  background: rgba(255, 255, 255, .95);
  box-shadow: 0 0 0 1px #d4deed inset;
  transition: box-shadow .18s ease, background .18s ease;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 2px #2f80ed inset, 0 12px 28px rgba(47, 128, 237, .12) !important;
}

.login-form :deep(.el-input__inner) {
  color: #172b4d;
  font-size: 16px;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #8798b5;
}

.login-form :deep(.el-input__prefix),
.login-form :deep(.el-input__suffix) {
  color: #7890b7;
  font-size: 21px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin: 2px 0 28px;
  font-size: 15px;
}

.form-options :deep(.el-checkbox__label) {
  color: #10264e;
  font-weight: 600;
}

.form-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #2563eb;
  border-color: #2563eb;
}

.login-button {
  width: 100%;
  height: 64px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(90deg, #2563eb 0%, #2f80ed 48%, #12bfb5 100%);
  box-shadow: 0 18px 34px rgba(37, 99, 235, .22);
  cursor: pointer;
  transition: transform .18s ease, box-shadow .18s ease, opacity .18s ease;
}

.login-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 22px 40px rgba(37, 99, 235, .28);
}

.login-button:active:not(:disabled) {
  transform: translateY(0);
}

.login-button:disabled {
  opacity: .72;
  cursor: not-allowed;
}

.loader {
  width: 18px;
  height: 18px;
  border: 2px solid #ffffff;
  border-bottom-color: transparent;
  border-radius: 50%;
  display: inline-block;
  animation: rotation 1s linear infinite;
}

@keyframes rotation {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.register-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 26px;
  color: #0e244a;
  font-size: 15px;
  font-weight: 700;
}

.register-row :deep(.el-button) {
  height: 34px;
  padding: 0 16px;
  border-radius: 8px;
  font-weight: 700;
}

.login-card footer {
  margin-top: 38px;
  text-align: center;
  color: #7183a4;
  font-size: 15px;
}

@media (max-width: 1180px) {
  .login-page {
    grid-template-columns: 1fr;
    overflow: auto;
  }

  .hero-pane {
    min-height: 620px;
    padding: 42px 42px 26px;
  }

  .login-pane {
    padding: 28px 32px 42px;
  }

  .login-card {
    min-height: 0;
  }
}

@media (max-width: 760px) {
  .login-page {
    background: #f7fbff;
  }

  .hero-pane {
    min-height: auto;
    padding: 28px 22px 16px;
  }

  .brand-header {
    gap: 12px;
  }

  .brand-mark {
    width: 52px;
    height: 52px;
  }

  .brand-header h1 {
    font-size: 24px;
  }

  .brand-header p {
    font-size: 11px;
    letter-spacing: 1px;
  }

  .hero-copy {
    margin-top: 34px;
  }

  .hero-copy h2 {
    font-size: 31px;
  }

  .hero-copy p {
    margin-top: 14px;
    font-size: 16px;
  }

  .education-scene {
    min-height: 260px;
    margin-top: 0;
    transform: scale(.74);
    transform-origin: left bottom;
    width: 135%;
  }

  .feature-strip {
    grid-template-columns: 1fr;
    gap: 12px;
    min-height: auto;
    padding: 16px;
    border-radius: 14px;
  }

  .feature-item {
    padding: 0;
  }

  .feature-item + .feature-item {
    border-left: 0;
  }

  .login-pane {
    padding: 18px 16px 28px;
  }

  .login-card {
    width: 100%;
    padding: 34px 20px 24px;
    border-radius: 16px;
  }

  .card-brand {
    justify-content: flex-start;
  }

  .card-logo {
    width: 58px;
    height: 58px;
  }

  .card-brand h2 {
    font-size: 24px;
  }

  .card-brand p {
    font-size: 11px;
    letter-spacing: 0;
  }

  .platform-label {
    margin: 28px 0;
    gap: 12px;
    font-size: 14px;
  }

  .welcome-block h3 {
    font-size: 25px;
  }

  .login-form :deep(.el-input__wrapper),
  .login-button {
    height: 56px;
  }

  .register-row {
    flex-wrap: wrap;
  }
}
</style>
