// Source contracts only. This does not replace rendered, keyboard or screen-reader testing.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
const read = name => fs.readFileSync(path.join(root, name), 'utf8');
const home = read('website/index.html');
const roadmap = read('website/roadmap/index.html');
const notes = read('website/changelog/index.html');
const proposal = read('docs/design/experiments/v21-about-story.html');
assert.match(home, /Aastra, a two-person team/);
assert.match(home, /Android app for making little eight-page books/);
assert.doesNotMatch(home, /WCAG/);
assert.match(home, /id="accessibility"/);
assert.match(home, /reduced-motion setting/);
assert.match(home, /releases\/download\/v0\.9\.0-beta\.4-r3\/zinely-0\.9\.0-beta\.4-r3-release\.apk/);
assert.match(roadmap, /still being tested, not in the download/);
assert.match(roadmap, /not announced features/);
assert.doesNotMatch(roadmap, /restore its regression test|Measure cold/);
for (const id of ['website-september-9', 'beta-4-r3', 'beta-4-r2', 'beta-4', 'beta-3']) {
  assert.ok(notes.includes(`id="${id}"`), `Preserve release bookmark ${id}`);
}
for (const [name, html] of [['index.html', home], ['roadmap/index.html', roadmap], ['changelog/index.html', notes]]) {
  assert.match(html, /<html lang="en">/);
  assert.equal((html.match(/<h1(?:\s|>)/g) || []).length, 1, `${name}: one main heading`);
  const ids = [...html.matchAll(/\bid="([^"]+)"/g)].map(m => m[1]);
  assert.equal(ids.length, new Set(ids).size, `${name}: unique IDs`);
  for (const [, href] of html.matchAll(/\bhref="([^"]+)"/g)) {
    if (/^(https:|mailto:)/.test(href)) continue;
    const [url, fragment] = href.split('#');
    const target = url ? path.resolve(root, 'website', path.dirname(name), url) : path.resolve(root, 'website', name);
    // Privacy is generated from the authoritative policy by pages.yml.
    if (target === path.resolve(root, 'website/privacy')) continue;
    const file = fs.existsSync(target) && fs.statSync(target).isDirectory() ? path.join(target, 'index.html') : target;
    // The logo is copied into the Pages artifact from the Android resources.
    if (file === path.resolve(root, 'website/assets/logo.webp')) continue;
    assert.ok(fs.existsSync(file), `${name}: local target ${href}`);
    if (fragment) assert.ok(fs.readFileSync(file, 'utf8').includes(`id="${fragment}"`), `${name}: anchor ${href}`);
  }
}
for (const html of [roadmap, notes, proposal]) assert.ok(!html.includes('\u2014'), 'No em dashes in rewritten pages/proposal');
for (const html of [home, proposal]) {
  for (const [, script] of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/g)) new vm.Script(script);
}
assert.match(proposal, /Proposal, not a design freeze/);
assert.match(proposal, /Aastra, a two-person team/);
console.log('PASS: public copy, local links/anchors, release bookmarks, stable download, and proposal script syntax. Source-only checks.');
