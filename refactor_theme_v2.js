const fs = require('fs');
const path = require('path');

const files = [
  'src/views/student/ExamTaking.vue',
  'src/views/student/Mistakes.vue',
  'src/views/student/Profile.vue',
  'src/views/student/Surveys.vue',
  'src/views/student/SurveyFill.vue',
  'src/views/student/ExamResult.vue',
  'src/views/admin/ClassManage.vue',
  'src/views/admin/UserManage.vue',
  'src/views/teacher/ExamCreate.vue',
  'src/views/teacher/ExamGrade.vue',
  'src/views/teacher/SurveyStats.vue',
  'src/views/teacher/Surveys.vue',
  'src/views/common/AiChat.vue'
];

const basePath = '/edu-platform/edu-frontend';

function processFile(filePath) {
  const fullPath = path.join(basePath, filePath);
  if (!fs.existsSync(fullPath)) return;

  let content = fs.readFileSync(fullPath, 'utf8');
  
  // First, revert any accidental replacements of border-color: var(--text-secondary)
  // that were originally border-color: #333 or similar.
  // Actually, let's just re-run with fixed regex on the CURRENT state,
  // but we need to know what to fix.
  // If border-color is var(--text-secondary), it's likely wrong.
  content = content.replace(/border-color:\s*var\(--text-secondary\)/gi, 'border-color: var(--border)');

  // Text Colors (using negative lookbehind to avoid matching border-color)
  content = content.replace(/(?<![a-zA-Z-])color:\s*(#909090|#999|#666|#444|#606266|var\(--text-secondary\))(?!\w)/gi, 'color: var(--text-secondary)');
  content = content.replace(/(?<![a-zA-Z-])color:\s*#333(?!\w)/gi, 'color: var(--text-secondary)');
  content = content.replace(/(?<![a-zA-Z-])color:\s*(#e0e0e0|#e2e8f0|#f8fafc)(?!\w)/gi, 'color: var(--text-primary)');

  // Borders
  content = content.replace(/border-color:\s*(#333|var\(--text-secondary\))(?!\w)/gi, 'border-color: var(--border)');
  content = content.replace(/border(-[a-z]+)?:\s*1px\s+solid\s+(#333|#1a1a2e)(?!\w)/gi, 'border$1: 1px solid var(--border)');
  content = content.replace(/border-color:\s*#1a1a2e(?!\w)/gi, 'border-color: var(--border-subtle)');

  // Backgrounds
  content = content.replace(/background(-color)?:\s*(#0a0a0a|#030303|#1a1a2e)(?!\w)/gi, 'background$1: var(--bg-surface)');
  content = content.replace(/background(-color)?:\s*rgba\(\s*255\s*,\s*255\s*,\s*255\s*,\s*0\.03\s*\)/gi, 'background$1: var(--surface-muted)');

  // Specific fallbacks
  content = content.replace(/var\(--bg-base,\s*#030303\)/gi, 'var(--bg-base)');

  fs.writeFileSync(fullPath, content, 'utf8');
}

files.forEach(processFile);
console.log('Refactor completed with fixed regex.');
