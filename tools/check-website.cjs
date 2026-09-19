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
// The product convention, not another hand-maintained website example, is the oracle.
const convention = fs.readFileSync(path.join(root, 'core/imposition/src/main/kotlin/com/aritr/zinely/core/imposition/Convention.kt'), 'utf8');
const cells = [...convention.matchAll(/(\d+) to GridCell\((\d+), (\d+)\)/g)]
  .map(([, number, row, column]) => ({ number: Number(number), row: Number(row), column: Number(column) }))
  .sort((a, b) => a.row - b.row || a.column - b.column);
const rotations = new Map([...convention.matchAll(/(\d+) to Rotation\.(NONE|HALF)/g)].map(([, n, r]) => [Number(n), r === 'HALF']));
assert.equal(cells.length, 8, 'Canonical eight-page cell table parsed');
assert.equal(rotations.size, 8, 'Canonical eight-page rotation table parsed');
const out = process.argv[4];
fs.mkdirSync(out, { recursive: true });
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
    page.on('console', message => { if (message.type() === 'error') failures.push(message.text()); });
    page.on('response', response => { if (response.url().startsWith(url) && response.status() >= 400) failures.push(`${response.status()} ${response.url()}`); });
    await page.goto(url);
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: path.join(out, 'home-desktop.png'), fullPage: true });
    for (const selector of ['.hero', '.gallery-section', '.about-card', '.download-card']) {
      await page.locator(selector).screenshot({ path: path.join(out, selector.slice(1) + '-desktop.png') });
    }
    await page.locator('[data-open-zine="market"]').click();
    const addButton = page.locator('[data-open-add]');
    assert.equal(await addButton.evaluate(el => el.scrollWidth <= el.clientWidth + 1), true, 'Add to page does not overflow');
    assert.equal(await addButton.locator('span').evaluate(el => el.getClientRects().length), 1, 'Add to page stays on one line');
    for (const selector of ['[data-demo-undo]', '[data-open-add]', '[data-open-proof]']) {
      const box = await page.locator(selector).boundingBox();
      assert.ok(box.width >= 44 && box.height >= 44, `${selector} keeps a 44px touch target`);
    }
    await addButton.click();
    assert.equal(await page.locator('#demo-add-tray').getAttribute('open'), '');
    await page.keyboard.press('Escape');
    await page.waitForTimeout(220);
    assert.equal(await addButton.evaluate(el => document.activeElement === el), true, 'Closing supplies returns focus to Add to page');
    await page.locator('[data-demo-screen="bench"]').screenshot({ path: path.join(out, 'desk-bench-desktop.png') });
    const orderToggle = page.locator('#page-order-toggle');
    await orderToggle.focus();
    await page.keyboard.press('Enter');
    assert.equal(await orderToggle.getAttribute('aria-pressed'), 'true');
    await page.waitForTimeout(550);
    // Read visual positions rather than copying the CSS mapping into the test.
    const printed = await page.locator('#page-order-sheet > span').evaluateAll(nodes => nodes.map(n => {
      const rect = n.getBoundingClientRect();
      const matrix = new DOMMatrix(getComputedStyle(n).transform);
      return { number: Number(n.querySelector('b').textContent), x: rect.x, y: rect.y, inverted: matrix.a < 0 };
    }).sort((a, b) => Math.abs(a.y - b.y) > 2 ? a.y - b.y : a.x - b.x));
    assert.deepEqual(printed.map(p => p.number), cells.map(p => p.number));
    assert.deepEqual(printed.map(p => p.inverted), cells.map(p => rotations.get(p.number)));
    const hero = await page.locator('.paper-story-sheet > span').evaluateAll(nodes => nodes.map(n => ({
      number: Number(n.textContent), inverted: getComputedStyle(n).transform !== 'none' && new DOMMatrix(getComputedStyle(n).transform).a < 0
    })));
    assert.deepEqual(hero.map(p => p.number), cells.map(p => p.number), 'Hero matches product convention too');
    assert.deepEqual(hero.map(p => p.inverted), cells.map(p => rotations.get(p.number)));
    assert.ok((await page.locator('#page-order-note').textContent()).includes(cells.slice(0, 4).map(p => p.number).join(', ')), 'Text alternative matches top row');
    assert.ok((await page.locator('#page-order-note').textContent()).includes(cells.slice(4).map(p => p.number).join(', ')), 'Text alternative matches bottom row');
    await page.locator('.page-lab').screenshot({ path: path.join(out, 'page-lab-print.png') });
    await page.keyboard.press('Space');
    assert.equal(await orderToggle.getAttribute('aria-pressed'), 'false', 'Keyboard reverses comparison');
    const idea = await page.locator('#zine-idea').textContent();
    await page.locator('#another-idea').focus();
    await page.keyboard.press('Enter');
    assert.notEqual(await page.locator('#zine-idea').textContent(), idea);
    for (const selector of ['.paper-note', '.footer-secret']) {
      await page.locator(`${selector} summary`).focus();
      await page.keyboard.press('Enter');
      assert.equal(await page.locator(selector).getAttribute('open'), '', 'Native disclosure opens by keyboard');
      await page.keyboard.press('Space');
      assert.equal(await page.locator(selector).getAttribute('open'), null);
    }
    await page.locator('.paper-story').hover({ position: { x: 20, y: 20 } });
    await page.waitForTimeout(100);
    assert.notEqual(await page.locator('.paper-story').evaluate(el => el.style.getPropertyValue('--paper-x')), '');
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.waitForTimeout(100);
    assert.equal(await page.locator('.paper-story').evaluate(el => el.style.getPropertyValue('--paper-x')), '');
    await orderToggle.click();
    assert.equal(await page.locator('#page-order-sheet > span').first().evaluate(el => getComputedStyle(el).transitionDuration), '0s');
    assert.equal(await orderToggle.getAttribute('aria-pressed'), 'true', 'Reduced motion retains the interaction');
    await orderToggle.click();
    await page.emulateMedia({ reducedMotion: 'no-preference' });
    const picker = page.locator('#fold-step');
    const play = page.locator('#fold-play');
    const livePicture = page.locator('#fold-live-picture');
    const setFoldStep = async i => picker.evaluate((element, value) => {
      element.value = String(value);
      element.dispatchEvent(new Event('input', { bubbles: true }));
      element.dispatchEvent(new Event('change', { bubbles: true }));
    }, i);
    await page.locator('#fold-guide').scrollIntoViewIfNeeded();
    assert.equal(await page.locator('.fold-card:visible').count(), 0, 'Enhanced guide uses one persistent live sheet');
    assert.equal(await livePicture.isVisible(), true);
    assert.equal(await page.locator('#fold-previous').isDisabled(), true);
    const skip = await page.locator('.skip-link').boundingBox();
    assert.ok(skip.y + skip.height < 0, 'Skip link hidden offscreen until focused');
    const first = await livePicture.innerHTML();
    await page.waitForTimeout(300);
    assert.equal(await livePicture.innerHTML(), first, 'No autoplay');
    await play.focus();
    await page.keyboard.press('Enter');
    await page.waitForTimeout(250);
    assert.equal(await play.textContent(), 'Pause fold');
    await page.keyboard.press('Space');
    const paused = await livePicture.innerHTML();
    await page.waitForTimeout(200);
    assert.equal(await livePicture.innerHTML(), paused, 'Pause stays paused');
    await play.click();
    await page.waitForFunction(() => document.querySelector('#fold-play').textContent === 'Replay fold');
    await page.locator('#fold-reset').click();
    assert.equal(await livePicture.innerHTML(), first);
    const controlYs = [];
    for (let i = 0; i < 10; i++) {
      await setFoldStep(i);
      assert.equal(await page.locator('.fold-card:visible').count(), 0);
      const card = page.locator(`#fold-${i + 1}`);
      assert.equal(await card.getAttribute('aria-hidden'), 'true');
      assert.equal(await card.locator('p span').count(), 3);
      assert.equal(await page.locator('#fold-live-copy br').count(), 2, `Desktop step ${i + 1}: three instruction lines`);
      assert.ok((await livePicture.boundingBox()).width > 250, `Desktop step ${i + 1}: live picture is visible`);
      controlYs.push((await play.boundingBox()).y);
    }
    assert.ok(Math.max(...controlYs) - Math.min(...controlYs) < 2, 'Playback alignment stable');
    assert.equal(await page.locator('#fold-next').isDisabled(), true);
    assert.equal(await page.locator('#fold-position-label').textContent(), 'Step 10 of 10');
    assert.match(await page.locator('.fold-finish').textContent(), /If yours looks like a little book/);
    const foldCollision = await page.evaluate(() => {
      const ticket = document.querySelector('.fold-stage__proof').getBoundingClientRect();
      const paper = document.querySelector('.fold-stage__paper').getBoundingClientRect();
      return !(ticket.right <= paper.left || ticket.left >= paper.right || ticket.bottom <= paper.top || ticket.top >= paper.bottom);
    });
    assert.equal(foldCollision, false, 'Proof ticket stays outside the working sheet');
    const bookletParts = await livePicture.evaluate(svg => {
      const box = selector => { const value = svg.querySelector(selector).getBBox(); return { y: value.y, width: value.width, height: value.height }; };
      return { title: box('.book-title-label'), art: box('.book-mark'), cover: box('.book-cover'), edge: box('.book-page-edge') };
    });
    assert.ok(bookletParts.title.y + bookletParts.title.height < bookletParts.art.y, 'Final booklet title and artwork do not overlap');
    assert.ok(bookletParts.cover.width > 85 && bookletParts.cover.height > 95 && bookletParts.edge.width > 8, 'Final booklet has cover and page-block proportions');
    await page.locator('#fold-guide').screenshot({ path: path.join(out, 'fold-desktop-final.png') });
    await setFoldStep(5);
    await play.click();
    await page.waitForFunction(() => document.querySelector('#fold-play').textContent === 'Replay fold');
    assert.equal(await livePicture.locator('.cut-mark').getAttribute('d'), 'M155 85H110', 'Cut stops at midpoint');
    await setFoldStep(8);
    await play.click();
    await page.waitForTimeout(1100);
    await play.click();
    await page.locator('#fold-guide').screenshot({ path: path.join(out, 'fold-diamond-paused.png') });
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.waitForFunction(() => document.querySelector('#fold-play').disabled);
    assert.equal(await play.isDisabled(), true);
    assert.ok(await livePicture.locator('.crease-line').count() > 0, 'Static picture restored');
    await page.emulateMedia({ reducedMotion: 'no-preference' });
    await page.waitForFunction(() => !document.querySelector('#fold-play').disabled);
    await page.locator('#fold-view').click();
    assert.equal(await page.locator('.fold-card:visible').count(), 10);
    assert.equal(await page.locator('.fold-position').isVisible(), false, 'Position hidden in all-steps view');
    const final = await page.locator('#fold-10').boundingBox();
    const list = await page.locator('.fold-instructions').boundingBox();
    assert.ok(Math.abs(final.x + final.width / 2 - list.x - list.width / 2) < 2, 'Final step centered');
    let axe = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
    assert.deepEqual(axe.violations.map(v => ({ id: v.id, nodes: v.nodes.map(n => n.target) })), [], 'All-steps accessibility');
    await page.locator('#fold-view').click();
    for (const [width, scale] of [[390, 1], [320, 1], [320, 2]]) {
      await page.setViewportSize({ width, height: 900 });
      await page.evaluate(scale => { document.documentElement.style.fontSize = `${16 * scale}px`; }, scale);
      // Open optional content too: hidden disclosures must not conceal reflow defects.
      await page.locator('.paper-note').evaluate(el => { el.open = true; });
      await page.locator('.footer-secret').evaluate(el => { el.open = true; });
      await orderToggle.click();
      const ys = [];
      for (let i = 0; i < 10; i++) {
        await setFoldStep(i);
        const overflow = await page.locator('main').evaluate(el => [...el.querySelectorAll('*')].filter(n => {
          if (!n.getClientRects().length || getComputedStyle(n).visibility === 'hidden' || n.closest('svg') || n.closest('.demo-page-nav')) return false;
          const r = n.getBoundingClientRect();
          return r.width > 0 && (r.right > innerWidth + 2 || r.left < -2) && !n.classList.contains('visually-hidden');
        }).map(n => n.id || n.className || n.tagName));
        assert.deepEqual(overflow, [], `${width}px/${scale}x text step ${i + 1} overflow`);
        ys.push(await play.evaluate(el => el.getBoundingClientRect().top + scrollY));
      }
      assert.ok(ys.every(Number.isFinite), 'Playback controls remain rendered with wrapped text');
      const buttonLineCount = await page.locator('#fold-previous').evaluate(el => {
        const range = document.createRange();
        range.selectNodeContents(el);
        return range.getClientRects().length;
      });
      assert.equal(buttonLineCount, 1, 'Previous label does not split at enlarged text');
      await setFoldStep(5);
      await page.locator('#fold-guide').screenshot({ path: path.join(out, `fold-${width}-${scale}x.png`) });
      await page.screenshot({ path: path.join(out, `home-${width}-${scale}x.png`), fullPage: true });
      await page.evaluate(() => scrollTo(0, 0));
      assert.ok(await page.locator('.skip-link').evaluate(el => el.getBoundingClientRect().bottom < 0), 'Unfocused skip link stays offscreen on mobile');
      await page.screenshot({ path: path.join(out, `viewport-${width}-${scale}x.png`) });
      for (const selector of ['.hero', '.page-lab', '.download-card']) {
        await page.locator(selector).screenshot({ path: path.join(out, `${selector.slice(1)}-${width}-${scale}x.png`) });
      }
      axe = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
      assert.deepEqual(axe.violations.map(v => ({ id: v.id, nodes: v.nodes.map(n => n.target) })), [], 'Focused accessibility');
    }
    await page.setViewportSize({ width: 1280, height: 900 });
    await page.evaluate(() => { document.documentElement.style.fontSize = ''; });
    for (const route of ['roadmap/', 'changelog/', 'privacy/', '404.html']) {
      await page.goto(url + route);
      assert.equal((await page.locator('body').innerText()).includes('Directory listing'), false, `${route} is a rendered page`);
      axe = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
      assert.deepEqual(axe.violations.map(v => v.id), [], route);
      await page.screenshot({ path: path.join(out, route.replaceAll('/', '') + '.png'), fullPage: true });
    }
    const fallback = await browser.newContext({ javaScriptEnabled: false, viewport: { width: 390, height: 900 } });
    const staticPage = await fallback.newPage();
    await staticPage.goto(url);
    assert.equal(await staticPage.locator('.fold-card:visible').count(), 10, 'No-JS complete fallback');
    assert.equal(await staticPage.locator('.fold-controls').isVisible(), false);
    assert.equal(await staticPage.locator('#page-order-toggle').isVisible(), false);
    assert.equal(await staticPage.locator('#another-idea').isVisible(), false);
    await staticPage.locator('.footer-secret summary').click();
    assert.equal(await staticPage.locator('.footer-secret p').isVisible(), true, 'Disclosure works without JS');
    await staticPage.close();
    const touch = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true });
    const touchPage = await touch.newPage();
    touchPage.on('pageerror', error => failures.push(error.message));
    await touchPage.goto(url);
    await touchPage.locator('#page-order-toggle').tap();
    assert.equal(await touchPage.locator('#page-order-toggle').getAttribute('aria-pressed'), 'true');
    await touchPage.locator('.paper-note summary').tap();
    assert.equal(await touchPage.locator('.paper-note p').isVisible(), true);
    await touchPage.locator('#another-idea').tap();
    assert.notEqual(await touchPage.locator('#zine-idea').textContent(), idea);
    await touch.close();
    await page.goto(url);
    await page.emulateMedia({ media: 'print' });
    assert.equal(await page.locator('.fold-card:visible').count(), 10, 'Print all steps');
    assert.deepEqual(failures, [], 'No JavaScript errors');
    console.log('PASS: paper comparison geometry; keyboard and touch discoveries; reduced motion; no autoplay; fold controls and cut endpoint; mobile/200% reflow; no-JS/print fallback; axe; zero page errors.');
    console.log(`Screenshots: ${out}`);
  } finally { await browser.close(); server.close(); }
})().catch(error => { console.error(error); server.close(); process.exitCode = 1; });
