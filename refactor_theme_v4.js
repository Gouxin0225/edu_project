const fs = require('fs');
const path = require('path');

const rootDir = '/edu-platform/edu-frontend/src/views';

const replacements = [
  {
    regex: /color:\s*#1a1a2e(!important)?/gi,
    replacement: 'color: var(--border)$1'
  },
  {
    regex: /fill:\s*#1a1a2e(!important)?/gi,
    replacement: 'fill: var(--border)$1'
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
