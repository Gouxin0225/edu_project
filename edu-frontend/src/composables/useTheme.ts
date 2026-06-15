import { computed, ref } from 'vue'

export type ThemeMode = 'light' | 'dark'

const STORAGE_KEY = 'edu-platform-theme'
const STORAGE_VERSION_KEY = 'edu-platform-theme-version'
const THEME_VERSION = 'light-default-20260603'
const DEFAULT_THEME: ThemeMode = 'light'
const theme = ref<ThemeMode>(DEFAULT_THEME)

function isThemeMode(value: string | null): value is ThemeMode {
  return value === 'light' || value === 'dark'
}

function readTheme(): ThemeMode {
  if (typeof window === 'undefined') {
    return DEFAULT_THEME
  }
  if (window.localStorage.getItem(STORAGE_VERSION_KEY) !== THEME_VERSION) {
    window.localStorage.removeItem(STORAGE_KEY)
    window.localStorage.setItem(STORAGE_VERSION_KEY, THEME_VERSION)
    return DEFAULT_THEME
  }
  const saved = window.localStorage.getItem(STORAGE_KEY)
  return isThemeMode(saved) ? saved : DEFAULT_THEME
}

function applyTheme(value: ThemeMode) {
  if (typeof document === 'undefined') {
    return
  }
  document.documentElement.dataset.theme = value
  document.documentElement.style.colorScheme = value
}

export function initTheme() {
  theme.value = readTheme()
  applyTheme(theme.value)
}

export function setTheme(value: ThemeMode) {
  theme.value = value
  applyTheme(value)
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_KEY, value)
    window.localStorage.setItem(STORAGE_VERSION_KEY, THEME_VERSION)
  }
}

export function toggleTheme() {
  setTheme(theme.value === 'dark' ? 'light' : 'dark')
}

export function useTheme() {
  return {
    theme,
    isDark: computed(() => theme.value === 'dark'),
    setTheme,
    toggleTheme
  }
}
