#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';

const ROOT = process.cwd();
const SRC_DIR = path.join(ROOT, 'src');
const ALLOWLIST_PATH = path.join(ROOT, 'config', 'tailwind-arbitrary-allowlist.json');

const CLASS_ARBITRARY_REGEX = /\b(?:[a-z]+[a-z0-9-]*:)*[a-z0-9-]+-\[([^\]\n]+)\]/g;
const SOURCE_EXTENSIONS = new Set(['.ts', '.tsx', '.css']);

function loadAllowlist() {
  if (!fs.existsSync(ALLOWLIST_PATH)) {
    throw new Error(`Arquivo de allowlist não encontrado: ${ALLOWLIST_PATH}`);
  }

  const raw = fs.readFileSync(ALLOWLIST_PATH, 'utf8').replace(/^\uFEFF/, '');
  const parsed = JSON.parse(raw);

  const literalValues = new Set(Array.isArray(parsed.literalValues) ? parsed.literalValues : []);
  const patterns = (Array.isArray(parsed.patterns) ? parsed.patterns : []).map((value) => new RegExp(value));

  return { literalValues, patterns };
}

function listSourceFiles(dir, results = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const absolutePath = path.join(dir, entry.name);

    if (entry.isDirectory()) {
      if (entry.name === 'node_modules' || entry.name === '.next') continue;
      listSourceFiles(absolutePath, results);
      continue;
    }

    if (SOURCE_EXTENSIONS.has(path.extname(entry.name))) {
      results.push(absolutePath);
    }
  }

  return results;
}

function getLineNumber(content, index) {
  let line = 1;
  for (let i = 0; i < index; i += 1) {
    if (content[i] === '\n') line += 1;
  }
  return line;
}

function isAllowedValue(value, allowlist) {
  if (allowlist.literalValues.has(value)) return true;
  return allowlist.patterns.some((pattern) => pattern.test(value));
}

function validateFile(filePath, allowlist) {
  const content = fs.readFileSync(filePath, 'utf8');
  const failures = [];

  for (const match of content.matchAll(CLASS_ARBITRARY_REGEX)) {
    const rawValue = match[1]?.trim() ?? '';
    if (!rawValue) continue;

    if (isAllowedValue(rawValue, allowlist)) continue;

    failures.push({
      filePath,
      line: getLineNumber(content, match.index ?? 0),
      classToken: match[0],
      value: rawValue,
    });
  }

  return failures;
}

function main() {
  const allowlist = loadAllowlist();
  const files = listSourceFiles(SRC_DIR);
  const failures = files.flatMap((filePath) => validateFile(filePath, allowlist));

  if (failures.length === 0) {
    console.log('Tailwind arbitrary values: OK');
    return;
  }

  console.error('Tailwind arbitrary values fora da allowlist:');
  for (const failure of failures) {
    const relative = path.relative(ROOT, failure.filePath).replace(/\\/g, '/');
    console.error(`- ${relative}:${failure.line} -> ${failure.classToken}`);
  }
  console.error('\nAjuste os valores para tokens/padrões permitidos ou atualize config/tailwind-arbitrary-allowlist.json com justificativa.');
  process.exit(1);
}

main();

