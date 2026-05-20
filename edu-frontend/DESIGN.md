# 智能教育管理系统 · 设计规范
# DESIGN SPEC — TECH GLASS THEME

---

## 1. 设计哲学

**简约 · 透明 · 科技 · 炫酷**

以"精密仪器感"为核心，所有视觉元素都应传达出高端、克制、精密的质感。界面如同航天舱仪表盘 — 信息密度高但不拥挤，视觉震撼但不花哨。

---

## 2. 色彩系统

### 主背景
- **Deep Navy**: `#080c14` — 深邃的海军蓝，近黑但不压抑
- **Surface**: `#0d1424` — 卡片/面板背景
- **Surface Elevated**: `#111827` — 悬浮元素背景

### 玻璃系统 (Glassmorphism)
- **Glass BG**: `rgba(255, 255, 255, 0.04)` — 基础毛玻璃背景
- **Glass BG Hover**: `rgba(255, 255, 255, 0.07)` — 悬停态玻璃
- **Glass BG Strong**: `rgba(255, 255, 255, 0.08)` — 强化玻璃
- **Glass Border**: `rgba(255, 255, 255, 0.08)` — 1px 细线边框
- **Glass Border Hover**: `rgba(0, 166, 255, 0.35)` — 悬停边框微微发亮

### 主色调 — 科技蓝 #00A6FF
- **Primary**: `#00A6FF` — 科技蓝（唯一的点缀色）
- **Primary Dim**: `rgba(0, 166, 255, 0.15)` — 淡蓝背景
- **Primary Glow**: `rgba(0, 166, 255, 0.25)` — 光晕效果
- **Primary Text**: `#7DD3FC` — 浅蓝文字

### 中性色
- **Text Primary**: `rgba(255, 255, 255, 0.95)` — 主要文字
- **Text Secondary**: `rgba(255, 255, 255, 0.60)` — 次要文字
- **Text Muted**: `rgba(255, 255, 255, 0.35)` — 辅助文字

### 语义色（克制版本）
- **Success**: `#22d3a5` — 青绿（非高饱和绿）
- **Warning**: `#f59e0b` —琥珀（不改色）
- **Danger**: `#f87171` — 珊瑚红（不改色）
- **Info**: `#7dd3fc` — 浅蓝（同主色系）

---

## 3. 排版

- **主字体**: `'Inter', 'Space Grotesk', -apple-system, sans-serif`
- **标题**: 700 weight, letter-spacing: -0.02em
- **正文**: 400 weight, line-height: 1.6
- **辅助文字**: 300-400 weight, opacity 0.6
- **大小比例**: 12 / 13 / 14 / 16 / 20 / 24 / 32 / 48px

---

## 4. 背景系统

### 全局背景 (`App.vue`)
- 基础色: `#080c14`
- **网格暗纹**: 1px 极细线条，40px 间距，`rgba(255,255,255,0.015)`
- **渐变光晕**: 2-3个极淡的蓝/青光晕，位置随机，动画极慢（60s+）

### 卡片扫光
- 每张卡片顶部有一道极淡（opacity 0.06）的水平渐变光线
- 从左到右: transparent → `rgba(0,166,255,0.08)` → transparent

---

## 5. 毛玻璃质感

```css
background: rgba(255, 255, 255, 0.04);
backdrop-filter: blur(20px) saturate(150%);
-webkit-backdrop-filter: blur(20px) saturate(150%);
border: 1px solid rgba(255, 255, 255, 0.08);
border-radius: 16px;
box-shadow: 0 8px 32px rgba(0, 0, 0, 0.20);
```

---

## 6. 动效规范

### 入场动画
```css
animation: fadeInUp 0.4s ease-out forwards;
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}
```
错落延迟: `animation-delay: calc(index * 0.06s)`

### Hover 效果
- 卡片: `transform: translateY(-2px)` + 边框变亮 + 淡淡蓝色光晕
- 按钮: `transform: translateY(-1px)` + 微弱发光
- 过渡时长: `0.2s ease-out`

### 背景动画
- 光晕漂移: 60s ease-in-out infinite alternate
- 粒子漂浮: 20-40s，opacity极低

---

## 7. Element Plus 覆盖策略

所有组件覆盖遵循:
- 背景: 毛玻璃 + 细边框
- 文字: 中性白
- 主按钮: 纯 `#00A6FF` 背景
- 边框: 细线半透明白
- 圆角: 8-12px（克制）
- focus态: 蓝色光晕 `box-shadow`

---

## 8. 实现文件清单

| 文件 | 修改内容 |
|------|----------|
| `src/assets/tech-theme.css` | 完整重写，设计系统变量 + 全局组件样式 |
| `src/App.vue` | 背景改为网格+光晕动效 |
| `src/layouts/MainLayout.vue` | 头部/侧边栏/毛玻璃质感重设计 |
| `src/views/Login.vue` | 登录页全面重新设计 |
| `src/views/student/*.vue` | 所有学生视图重设计 |
| `src/views/teacher/*.vue` | 所有教师视图重设计 |
| `src/views/admin/*.vue` | 所有管理员视图重设计 |
| `src/views/assistant/*.vue` | 所有助教视图重设计 |
| `src/components/*.vue` | 共享组件重设计 |
