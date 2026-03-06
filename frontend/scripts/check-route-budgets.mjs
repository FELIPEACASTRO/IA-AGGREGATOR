#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';

const ROOT = process.cwd();
const MANIFEST_PATH = path.join(ROOT, '.next', 'app-build-manifest.json');

const ROUTE_BUDGETS_KB = {
  '/page': 620,
  '/home/page': 640,
  '/chat/page': 670,
  '/library/page': 650,
  '/settings/analytics/page': 640,
};

if (!fs.existsSync(MANIFEST_PATH)) {
  console.error('Arquivo .next/app-build-manifest.json não encontrado. Rode `npm run build` antes do budget check.');
  process.exit(1);
}

const manifest = JSON.parse(fs.readFileSync(MANIFEST_PATH, 'utf8'));
const pages = manifest?.pages ?? {};

const failures = [];

console.log('Route JS bundle budgets');
console.log('-----------------------');

for (const [routeKey, maxKb] of Object.entries(ROUTE_BUDGETS_KB)) {
  const files = Array.isArray(pages[routeKey]) ? pages[routeKey] : [];
  const jsFiles = [...new Set(files.filter((file) => typeof file === 'string' && file.endsWith('.js')))] ;

  const bytes = jsFiles.reduce((acc, relativeFile) => {
    const absoluteFile = path.join(ROOT, '.next', relativeFile);
    if (!fs.existsSync(absoluteFile)) return acc;
    return acc + fs.statSync(absoluteFile).size;
  }, 0);

  const kb = bytes / 1024;
  const status = kb <= maxKb ? 'OK' : 'FAIL';
  console.log(`${status.padEnd(4)} ${routeKey.padEnd(30)} ${kb.toFixed(1)}KB / ${maxKb}KB`);

  if (kb > maxKb) {
    failures.push({ routeKey, kb, maxKb });
  }
}

if (failures.length > 0) {
  console.error('\nBudget estourado em uma ou mais rotas.');
  process.exit(1);
}

console.log('\nBudgets atendidos.');

