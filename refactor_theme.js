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
  if (!fs.existsSync(fullPath)) {
    console.log(`File not found: ${filePath}`);
    return;
  }

  let content = fs.readFileSync(fullPath, 'utf8');
  let originalContent = content;

  // Text Colors
  // Rule: #666, #999, #909090, #444, #606266 -> var(--text-secondary) or var(--text-muted)
  // We'll use secondary as a safer default for readability, unless it's clearly muted notes.
  // Actually, #909090 is quite light in dark theme, #666 is darker.
  // Let's map #909090, #999 to secondary and #666, #444 to muted? 
  // Wait, rule says "secondary for more important text, muted for very minor notes".
  // Let's use var(--text-muted) for these grays by default as they were mostly used for secondary/muted info.
  content = content.replace(/color:\s*(#909090|#999|#666|#444|#606266)(?!\w)/gi, 'color: var(--text-secondary)');
  content = content.replace(/color:\s*#333(?!\w)/gi, 'color: var(--text-secondary)');
  content = content.replace(/color:\s*(#e0e0e0|#e2e8f0|#f8fafc)(?!\w)/gi, 'color: var(--text-primary)');

  // Borders
  // Rule: border-color: #333, border: 1px solid #1a1a2e -> var(--border)
  content = content.replace(/border-color:\s*#333(?!\w)/gi, 'border-color: var(--border)');
  content = content.replace(/border(-[a-z]+)?:\s*1px\s+solid\s+#333(?!\w)/gi, 'border$1: 1px solid var(--border)');
  content = content.replace(/border(-[a-z]+)?:\s*1px\s+solid\s+#1a1a2e(?!\w)/gi, 'border$1: 1px solid var(--border)');
  // Rule: border-color: #1a1a2e -> var(--border-subtle)
  content = content.replace(/border-color:\s*#1a1a2e(?!\w)/gi, 'border-color: var(--border-subtle)');

  // Backgrounds
  // Rule: background: #0a0a0a, #030303, #1a1a2e -> var(--bg-surface) or var(--surface-card)
  content = content.replace(/background(-color)?:\s*(#0a0a0a|#030303|#1a1a2e)(?!\w)/gi, 'background$1: var(--bg-surface)');
  // Rule: background: rgba(255, 255, 255, 0.03) -> var(--surface-muted)
  content = content.replace(/background(-color)?:\s*rgba\(\s*255\s*,\s*255\s*,\s*255\s*,\s*0\.03\s*\)/gi, 'background$1: var(--surface-muted)');

  // Specific fallbacks
  content = content.replace(/var\(--bg-base,\s*#030303\)/gi, 'var(--bg-base)');

  if (content !== originalContent) {
    fs.writeFileSync(fullPath, content, 'utf8');
    console.log(`Updated ${filePath}`);
  }
}

files.forEach(processFile);
