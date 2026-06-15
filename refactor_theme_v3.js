const fs = require('fs');
const path = require('path');

const rootDir = '/edu-platform/edu-frontend/src/views';

const replacements = [
  // 1. Specific property-based replacements for ambiguous colors
  
  // #333
  {
    regex: /(border(?:-top|-bottom|-left|-right|-color)?:\s*[^;]*?)#333/gi,
    replacement: '$1var(--border)'
  },
  {
    regex: /(color:\s*[^;]*?)#333/gi,
    replacement: '$1var(--text-secondary)'
  },

  // #1a1a2e
  {
    regex: /(border-color:\s*[^;]*?)#1a1a2e/gi,
    replacement: '$1var(--border-subtle)'
  },
  {
    regex: /(border(?:-top|-bottom|-left|-right)?:\s*[^;]*?)#1a1a2e/gi,
    replacement: '$1var(--border)'
  },
  {
    regex: /(box-shadow:\s*[^;]*?)#1a1a2e/gi,
    replacement: '$1var(--border)'
  },
  {
    regex: /(background(?:-color)?:\s*[^;]*?)#1a1a2e/gi,
    replacement: '$1var(--bg-surface)'
  },
  {
    regex: /(linear-gradient\([^;]*?)#1a1a2e/gi,
    replacement: '$1var(--bg-surface)'
  },
  {
    regex: /(--el-[a-z0-9-]+-color:\s*[^;]*?)#1a1a2e/gi,
    replacement: '$1var(--border)'
  },

  // #0a0a0a, #030303 (Backgrounds/Variables)
  {
    regex: /(background(?:-color)?:\s*[^;]*?)(#0a0a0a|#030303)/gi,
    replacement: '$1var(--bg-surface)'
  },
  {
    regex: /(linear-gradient\([^;]*?)(#0a0a0a|#030303)/gi,
    replacement: '$1var(--bg-surface)'
  },
  {
    regex: /(--el-[a-z0-9-]+-color:\s*[^;]*?)(#0a0a0a|#030303)/gi,
    replacement: '$1var(--bg-surface)'
  },

  // 2. Global-ish replacements for unambiguous colors (within style tag)
  
  // Text secondary
  {
    regex: /(color:\s*[^;]*?)(#666|#999|#909090|#444|#606266)/gi,
    replacement: '$1var(--text-secondary)'
  },
  // Text primary
  {
    regex: /(color:\s*[^;]*?)(#e0e0e0|#e2e8f0|#f8fafc)/gi,
    replacement: '$1var(--text-primary)'
  },
  // Other properties for these colors
  {
    regex: /(border(?:-top|-bottom|-left|-right|-color)?:\s*[^;]*?)(#666|#999|#909090|#444|#606266)/gi,
    replacement: '$1var(--text-secondary)'
  },
  {
    regex: /(background(?:-color)?:\s*[^;]*?)(#e0e0e0|#e2e8f0|#f8fafc)/gi,
    replacement: '$1var(--text-primary)'
  },
  {
    regex: /(linear-gradient\([^;]*?)(#e0e0e0|#e2e8f0|#f8fafc)/gi,
    replacement: '$1var(--text-primary)'
  },

  // 3. rgba(255, 255, 255, 0.03)
  {
    regex: /rgba\(255,\s*255,\s*255,\s*0\.03\)/gi,
    replacement: 'var(--surface-muted)'
  },

  // 4. #000 and #fff (General text/bg)
  {
    // color: #000
    regex: /color:\s*#000(\s*!important)?/gi,
    replacement: (match, p1, offset, string) => {
      const context = string.substring(Math.max(0, offset - 50), Math.min(string.length, offset + 50));
      if (context.toLowerCase().includes('hover') || /#00ffff|#ff10f0|#39ff14|#ff9800|#fff/i.test(context)) {
        return match;
      }
      return `color: var(--text-primary)${p1 || ''}`;
    }
  },
  {
    regex: /color:\s*#fff(\s*!important)?/gi,
    replacement: 'color: var(--text-primary)$1'
  },
  {
    regex: /background(?:-color)?:\s*#000(\s*!important)?/gi,
    replacement: (match, p1) => match.replace('#000', 'var(--bg-base)')
  },
  {
    regex: /background(?:-color)?:\s*#fff(\s*!important)?/gi,
    replacement: (match, p1) => match.replace('#fff', 'var(--bg-surface)')
  }
];

function processFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf8');
  
  const styleRegex = /<style[^>]*>([\s\S]*?)<\/style>/gi;
  let newContent = content.replace(styleRegex, (match, styleBody) => {
    let newStyleBody = styleBody;
    replacements.forEach(({ regex, replacement }) => {
      newStyleBody = newStyleBody.replace(regex, replacement);
    });
    return match.replace(styleBody, newStyleBody);
  });

  if (content !== newContent) {
    fs.writeFileSync(filePath, newContent, 'utf8');
    console.log(`Updated: ${filePath}`);
  }
}

function walkDir(dir) {
  const files = fs.readdirSync(dir);
  files.forEach(file => {
    const fullPath = path.join(dir, file);
    if (fs.statSync(fullPath).isDirectory()) {
      walkDir(fullPath);
    } else if (file.endsWith('.vue')) {
      processFile(fullPath);
    }
  });
}

walkDir(rootDir);
console.log('Refactoring complete.');
