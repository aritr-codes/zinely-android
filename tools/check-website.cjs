// Run with an externally installed Playwright package directory; no app/runtime dependency.
// node tools/check-website.cjs <qa-node_modules> <browser-executable> <output-directory>
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const { chromium } = require(path.join(process.argv[2], 'playwright'));
const { default: AxeBuilder } = require(path.join(process.argv[2], '@axe-core/playwright'));
const root = path.resolve(__dirname, '..');
const site = path.join(root, 'website');
const out = process.argv[4];
const failures = [];
const server = http.createServer((req, res) => {
  let name = decodeURIComponent(new URL(req.url, 'http://localhost').pathname).replace(/^\/zinely-android\//, '');
  if (name.endsWith('/') || !name) name += 'index.html';
  let file = path.resolve(site, name);
  if (!file.startsWith(site + path.sep)) { res.writeHead(403); res.end(); return; }
  if (name === 'assets/logo.webp') file = path.join(root, 'app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp');
  if (!fs.existsSync(file) || fs.statSync(file).isDirectory()) { res.writeHead(404); res.end(); return; }
  const mime = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.webp': 'image/webp' };
  res.setHeader('Content-Type', (mime[path.extname(file)] || 'application/octet-stream') + '; charset=utf-8');
  res.end(fs.readFileSync(file));
});

(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const url = `http://127.0.0.1:${server.address().port}/zinely-android/`;
  const browser = await chromium.launch({ executablePath: process.argv[3], headless: true });
  try {
    const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
    const page = await context.newPage();
    page.on('pageerror', error => failures.push(error.message));
    await page.goto(url);
    const picker = page.locator('#fold-step');
    const play = page.locator('#fold-play');
    await page.locator('#fold-guide').scrollIntoViewIfNeeded();
    assert.equal(await page.locator('.fold-card:visible').count(), 1);
    assert.equal(await page.locator('#fold-previous').isDisabled(), true);
    const skip = await page.locator('.skip-link').boundingBox();
    assert.ok(skip.y + skip.height < 0, 'Skip link hidden offscreen until focused');
    const first = await page.locator('#fold-1 svg').innerHTML();
    await page.waitForTimeout(300);
    assert.equal(await page.locator('#fold-1 svg').innerHTML(), first, 'No autoplay');
    await play.focus();
    await page.keyboard.press('Enter');
    await page.waitForTimeout(250);
    assert.equal(await play.textContent(), 'Pause direction');
    await page.keyboard.press('Space');
    const paused = await page.locator('#fold-1 svg').innerHTML();
    await page.waitForTimeout(200);
    assert.equal(await page.locator('#fold-1 svg').innerHTML(), paused, 'Pause stays paused');
    await play.click();
    await page.waitForFunction(() => document.querySelector('#fold-play').textContent === 'Replay direction');
    await page.locator('#fold-reset').click();
    assert.equal(await page.locator('#fold-1 svg').innerHTML(), first);
    const controlYs = [];
    for (let i = 0; i < 10; i++) {
      await picker.selectOption(String(i));
      assert.equal(await page.locator('.fold-card:visible').count(), 1);
      const card = page.locator(`#fold-${i + 1}`);
      assert.equal(await card.getAttribute('aria-hidden'), null);
      assert.equal(await card.locator('p span').count(), 3);
      const lineCount = await card.locator('p').evaluate(el => el.getBoundingClientRect().height / parseFloat(getComputedStyle(el).lineHeight));
      assert.ok(Math.abs(lineCount - 3) < .1, `Desktop step ${i + 1}: ${lineCount} lines`);
      controlYs.push((await play.boundingBox()).y);
    }
    assert.ok(Math.max(...controlYs) - Math.min(...controlYs) < 2, 'Playback alignment stable');
    assert.equal(await page.locator('#fold-next').isDisabled(), true);
    await page.locator('#fold-guide').screenshot({ path: path.join(out, 'fold-desktop-final.png') });
    await picker.selectOption('5');
    await play.click();
    await page.waitForFunction(() => document.querySelector('#fold-play').textContent === 'Replay direction');
    assert.equal(await page.locator('#fold-6 .cut-mark').getAttribute('d'), 'M155 85H110', 'Cut stops at midpoint');
    await picker.selectOption('8');
    await play.click();
    await page.waitForTimeout(1100);
    await play.click();
    await page.locator('#fold-guide').screenshot({ path: path.join(out, 'fold-diamond-paused.png') });
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.waitForFunction(() => document.querySelector('#fold-play').disabled);
    assert.equal(await play.isDisabled(), true);
    assert.ok(await page.locator('#fold-9 svg .crease-line').count() > 0, 'Static picture restored');
    await page.emulateMedia({ reducedMotion: 'no-preference' });
    await page.waitForFunction(() => !document.querySelector('#fold-play').disabled);
    await page.locator('#fold-view').click();
    assert.equal(await page.locator('.fold-card:visible').count(), 10);
    const final = await page.locator('#fold-10').boundingBox();
    const list = await page.locator('.fold-instructions').boundingBox();
    assert.ok(Math.abs(final.x + final.width / 2 - list.x - list.width / 2) < 2, 'Final step centered');
    let axe = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
    assert.deepEqual(axe.violations.map(v => ({ id: v.id, nodes: v.nodes.map(n => n.target) })), [], 'All-steps accessibility');
    await page.locator('#fold-view').click();
    for (const [width, scale] of [[390, 1], [320, 1], [320, 2]]) {
      await page.setViewportSize({ width, height: 900 });
      await page.evaluate(scale => { document.documentElement.style.fontSize = `${16 * scale}px`; }, scale);
      const ys = [];
      for (let i = 0; i < 10; i++) {
        await picker.selectOption(String(i));
        const overflow = await page.locator('main').evaluate(el => [...el.querySelectorAll('*')].filter(n => {
          if (!n.getClientRects().length || getComputedStyle(n).visibility === 'hidden' || n.closest('svg')) return false;
          const r = n.getBoundingClientRect();
          return r.width > 0 && (r.right > innerWidth + 2 || r.left < -2) && !n.classList.contains('visually-hidden');
        }).map(n => n.id || n.className || n.tagName));
        assert.deepEqual(overflow, [], `${width}px/${scale}x text step ${i + 1} overflow`);
        ys.push(await play.evaluate(el => el.getBoundingClientRect().top + scrollY));
      }
      assert.ok(Math.max(...ys) - Math.min(...ys) < 2, 'Stable controls with wrapped text');
      const buttonLineCount = await page.locator('#fold-previous').evaluate(el => {
        const range = document.createRange();
        range.selectNodeContents(el);
        return range.getClientRects().length;
      });
      assert.equal(buttonLineCount, 1, 'Previous label does not split at enlarged text');
      await picker.selectOption('5');
      await page.locator('#fold-guide').screenshot({ path: path.join(out, `fold-${width}-${scale}x.png`) });
      axe = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
      assert.deepEqual(axe.violations.map(v => v.id), [], 'Focused accessibility');
    }
    await page.setViewportSize({ width: 1280, height: 900 });
    await page.evaluate(() => { document.documentElement.style.fontSize = ''; });
    for (const route of ['roadmap/', 'changelog/']) {
      await page.goto(url + route);
      axe = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
      assert.deepEqual(axe.violations.map(v => v.id), [], route);
      await page.screenshot({ path: path.join(out, route.replace('/', '') + '.png'), fullPage: true });
    }
    const fallback = await browser.newContext({ javaScriptEnabled: false, viewport: { width: 390, height: 900 } });
    const staticPage = await fallback.newPage();
    await staticPage.goto(url);
    assert.equal(await staticPage.locator('.fold-card:visible').count(), 10, 'No-JS complete fallback');
    assert.equal(await staticPage.locator('.fold-controls').isVisible(), false);
    await staticPage.close();
    await page.goto(url);
    await page.emulateMedia({ media: 'print' });
    assert.equal(await page.locator('.fold-card:visible').count(), 10, 'Print all steps');
    assert.deepEqual(failures, [], 'No JavaScript errors');
    console.log('PASS: no autoplay; keyboard play/pause/resume/reset; ten steps; cut endpoint; reduced motion; stable controls; centered final; mobile/200% reflow; no-JS/print fallback; axe checks; zero page errors.');
    console.log(`Screenshots: ${out}`);
  } finally { await browser.close(); server.close(); }
})().catch(error => { console.error(error); server.close(); process.exitCode = 1; });
