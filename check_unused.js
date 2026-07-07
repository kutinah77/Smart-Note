const fs = require('fs');
const path = require('path');

const stringsXmlPath = 'app/src/main/res/values/strings.xml';
const stringsXml = fs.readFileSync(stringsXmlPath, 'utf8');

const regex = /<string name="([^"]+)">/g;
const stringNames = [];
let match;
while ((match = regex.exec(stringsXml)) !== null) {
  stringNames.push(match[1]);
}

console.log(`Found ${stringNames.length} strings.`);

function searchInDir(dir, stringName) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const fullPath = path.join(dir, file);
    const stat = fs.statSync(fullPath);
    if (stat.isDirectory()) {
      if (file !== 'build' && file !== '.gradle') {
        if (searchInDir(fullPath, stringName)) return true;
      }
    } else if (file.endsWith('.kt') || file.endsWith('.xml') || file.endsWith('.java')) {
      if (fullPath === 'app/src/main/res/values/strings.xml') continue;
      const content = fs.readFileSync(fullPath, 'utf8');
      if (content.includes(`R.string.${stringName}`) || content.includes(`@string/${stringName}`)) {
        return true;
      }
    }
  }
  return false;
}

const unused = [];
for (let i = 0; i < stringNames.length; i++) {
  const name = stringNames[i];
  if (!searchInDir('app/src', name)) {
    unused.push(name);
  }
}

console.log(`Found ${unused.length} unused strings.`);
console.log(unused.join('\n'));
