/* Runner de toda la suite del motor. Ejecutar: node tests/run-all.js
   Lanza cada archivo *.test.js en proceso aparte y agrega el resultado. */
'use strict';
const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const dir = __dirname;
const files = fs.readdirSync(dir).filter(function (f) { return /\.test\.js$/.test(f); }).sort();
let fallos = 0;
files.forEach(function (f) {
  process.stdout.write('\n===== ' + f + ' =====\n');
  try {
    const out = execFileSync(process.execPath, [path.join(dir, f)], { encoding: 'utf8' });
    process.stdout.write(out.split('\n').slice(-3).join('\n') + '\n');
  } catch (e) {
    fallos++;
    process.stdout.write((e.stdout || '') + (e.stderr || '') + '\n');
  }
});
process.stdout.write('\n' + (fallos === 0 ? '✓ SUITE OK' : ('✗ ' + fallos + ' archivo(s) con fallos')) + '\n');
process.exit(fallos ? 1 : 0);
