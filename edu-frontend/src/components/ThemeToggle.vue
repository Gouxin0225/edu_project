<template>
  <div class="theme-toggle" :class="`theme-toggle--${variant}`" role="group" aria-label="主题切换">
    <button
      type="button"
      class="theme-option"
      :class="{ 'is-active': theme === 'light' }"
      :aria-pressed="theme === 'light'"
      title="切换到明亮主题"
      @click="setTheme('light')"
    >
      <el-icon><Sunny /></el-icon>
      <span>明亮</span>
    </button>
    <button
      type="button"
      class="theme-option"
      :class="{ 'is-active': theme === 'dark' }"
      :aria-pressed="theme === 'dark'"
      title="切换到暗色主题"
      @click="setTheme('dark')"
    >
      <el-icon><Moon /></el-icon>
      <span>暗色</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'

withDefaults(defineProps<{ variant?: 'header' | 'floating' }>(), {
  variant: 'header'
})

const { theme, setTheme } = useTheme()
</script>

<style scoped>
.theme-toggle {
  display: inline-grid;
  grid-template-columns: repeat(2, minmax(58px, 1fr));
  align-items: center;
  gap: 2px;
  width: 124px;
  height: 32px;
  padding: 3px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--toggle-bg);
  box-shadow: var(--shadow-sm);
}

.theme-toggle--floating {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 50;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.theme-option {
  height: 24px;
  border: 0;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  transition: color 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
}

.theme-option:hover {
  color: var(--primary-light);
  background: var(--surface-hover);
}

.theme-option.is-active {
  color: var(--toggle-active-text);
  background: var(--toggle-active-bg);
  box-shadow: var(--toggle-active-shadow);
}

.theme-option .el-icon {
  font-size: 14px;
}
</style>
