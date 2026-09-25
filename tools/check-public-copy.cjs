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
const siteStyles = read('website/assets/styles.css');
const siteScript = read('website/assets/site.js');
const roadmapStyles = read('website/assets/roadmap.css');
const journalStyles = read('website/assets/journal.css');
const quietStyles = read('website/assets/quiet-pages.css');
const policyLayout = read('website/_layouts/policy.html');
const privacyFallback = read('website/privacy/index.html');
const notFound = read('website/404.html');
const download = read('website/download/index.html');
const sitemap = read('website/sitemap.xml');
const releaseNotes = read('docs/releases/0.9.0-beta.5.md');
const proposal = read('docs/design/experiments/v21-about-story.html');
const appAbout = read('docs/design/mockups/v21-colophon.html');
const copy = read('core/copy/src/main/kotlin/com/aritr/zinely/core/copy/Copy.kt');
const makerConstants = [...copy.matchAll(/public const val (MAKER_\w+): String =\s*("[^"\r\n]*"(?:\s*\+\s*"[^"\r\n]*")*)/g)];
assert.equal(makerConstants.length, 4, 'Four shared maker-note strings');
for (const [, name, expression] of makerConstants) {
  const value = [...expression.matchAll(/"([^"\r\n]*)"/g)].map(m => m[1]).join('');
  assert.ok(appAbout.includes(`>${value}</`), `${name}: shared Android copy matches frozen HTML`);
  assert.ok(!value.includes('\u2014'), `${name}: no em dash`);
}
assert.match(home, /Aastra, a two-person team/);
assert.match(home, /Sometimes you want to make something you can hand to someone/);
assert.match(home, /An Android app with a paper habit/);
assert.match(home, /eight-page booklet you can print, cut, fold, and hand to someone/);
assert.match(home, /data-desk-journey/);
assert.equal((home.match(/class="workbench-step /g) || []).length, 3, 'Gather, Arrange, and Print workbench scenes');
assert.match(home, /id="fold-step" type="range" min="0" max="9"/);
assert.match(home, /Fold this step/);
assert.match(siteScript, /picker\.addEventListener\('input'/);
assert.match(siteStyles, /Phase 2 shared workbench story/);
assert.match(roadmap, /assets\/roadmap\.css/);
for (const horizon of ['Foundation', 'Craft', 'Proof']) assert.ok(roadmap.includes(`class="press-zone">${horizon}</p>`), `Roadmap horizon ${horizon}`);
assert.match(roadmap, /Not on our list/);
assert.match(notes, /assets\/journal\.css/);
assert.match(notes, /studio-journal/);
assert.match(policyLayout, /assets\/quiet-pages\.css/);
assert.match(privacyFallback, /id="feedback-and-email"/);
assert.match(privacyFallback, /Zinely does not request the <code>INTERNET<\/code> permission/);
assert.doesNotMatch(privacyFallback, /Directory listing/);
assert.doesNotMatch(notFound, /relative_url/);
// GitHub Pages serves 404.html at any depth, so every local reference must be root-absolute.
for (const [, ref] of notFound.matchAll(/\b(?:href|src)="([^"]+)"/g)) assert.match(ref, /^(\/zinely-android\/|#|https:)/, `404 reference ${ref} works at any depth`);
assert.match(roadmapStyles, /\.roadmap-board/);
assert.match(journalStyles, /\.studio-journal/);
assert.match(quietStyles, /\.quiet-scene/);
assert.equal((home.match(/data-journey-panel=/g) || []).length, 3, 'Three truthful journey stages');
assert.match(home, /data-desk-demo hidden/);
assert.equal((home.match(/data-demo-screen=/g) || []).length, 3, 'Shelf, Bench, and Proof demo screens');
assert.equal((home.match(/data-open-zine=/g) || []).length, 4, 'Four sample zines open on the Bench');
assert.match(home, /data-demo-add aria-labelledby="demo-add-title"/);
assert.match(home, /data-add-element="text"/);
assert.match(home, /data-add-element="photo"/);
assert.match(home, /data-add-element="art"/);
assert.match(home, /class="visually-hidden" aria-live="polite" data-demo-status/);
assert.match(home, /It does not save or export anything/);
assert.match(siteScript, /Nothing was downloaded/);
assert.match(siteScript, /for \(let index = 0; index < 8; index \+= 1\)/);
assert.match(siteScript, /event\.key === 'ArrowRight'/);
assert.match(siteScript, /dialog\.show\(\)/);
assert.doesNotMatch(siteScript, /showModal|Math\.random/);
// Privacy promise: no third-party fonts, scripts or styles on any page. Fonts ship with their licences.
for (const [name, text] of [['home', home], ['roadmap', roadmap], ['changelog', notes], ['download', download], ['404', notFound], ['policy layout', policyLayout], ['privacy fallback', privacyFallback],
  ['styles', siteStyles], ['roadmap styles', roadmapStyles], ['journal styles', journalStyles], ['quiet styles', quietStyles], ['script', siteScript]]) {
  assert.doesNotMatch(text, /fonts\.(googleapis|gstatic)\.com/, `${name}: no Google Fonts request`);
  // Anything the browser fetches by itself: embedded elements, CSS url() and @import. Plain <a> links are fine.
  assert.doesNotMatch(text, /<(?:script|link|img|iframe|source|video|audio|embed|object)\b[^>]*\b(?:src|href|srcset)=["']?(?:https?:)?\/\/(?!aritr-codes\.github\.io\/)/, `${name}: no third-party embedded resource`);
  assert.doesNotMatch(text, /url\(\s*["']?(?:https?:)?\/\/|@import/, `${name}: no remote CSS resource`);
}
for (const [, font] of siteStyles.matchAll(/url\("(fonts\/[^"]+)"\)/g)) assert.ok(fs.existsSync(path.join(root, 'website/assets', font)), `Self-hosted ${font} exists`);
for (const licence of ['OFL-AveriaSansLibre.txt', 'OFL-Fraunces.txt', 'OFL-Inter.txt']) {
  assert.ok(fs.existsSync(path.join(root, 'website/assets/fonts', licence)), `${licence} ships beside the fonts`);
}
for (const weight of ['regular', 'bold']) {
  const web = fs.readFileSync(path.join(root, `website/assets/fonts/averia-sans-libre-${weight}.ttf`));
  const app = fs.readFileSync(path.join(root, `core/ui/src/main/res/font/averia_sans_libre_${weight}.ttf`));
  assert.ok(web.equals(app), `Averia ${weight} is the unmodified app file (Reserved Font Name)`);
}
assert.doesNotMatch(home, /things kept|\+ keep/, 'The demo shows only shipped Bench UI');
assert.match(home, /data-demo-undo disabled aria-label="Undo"/);
assert.match(home, /data-open-proof aria-label="Done and preview"/);
assert.match(home, /data-demo-share/);
assert.match(siteStyles, /aspect-ratio: 390 \/ 812/);
assert.match(home, /class="demo-desk-props" aria-hidden="true"/);
assert.match(siteStyles, /@media \(min-width: 900px\)/);
assert.match(siteStyles, /@media \(min-width: 900px\) and \(max-height: 820px\)/);
assert.match(siteStyles, /--demo-phone-width: 320px/);
assert.match(siteStyles, /gallery-section:has\(\.desk-journey\.is-enhanced\)/);
assert.match(siteStyles, /\.demo-add-dialog \{ position: absolute/);
assert.match(siteScript, /function choosePlacement\(/);
assert.match(siteScript, /function hashString\(/);
assert.match(siteScript, /function trapTrayFocus\(/);
assert.match(siteScript, /setBenchInert\(true\)/);
assert.match(siteScript, /addButton\.focus\(\)/);
assert.match(siteScript, /beforeprint/);
assert.match(siteScript, /current\.pages\[pageIndex\]\.map\(cloneItem\)/);
assert.match(siteScript, /item\.placement \|\|= choosePlacement/);
assert.match(home, /Add to page/);
assert.match(home, /id="demo-add-tray"/);
assert.match(home, /aria-expanded="false" aria-controls="demo-add-tray"/);
assert.match(home, /aria-label="Close supplies"/);
assert.match(siteStyles, /z-slide-tray-in/);
assert.match(siteStyles, /z-set-down/);
assert.match(siteStyles, /min-height: 62px/);
assert.doesNotMatch(siteStyles, /\.demo-add-dialog::backdrop/);
assert.match(siteScript, /photo\.dataset\.photo/);
const compositionDefinitions = siteScript.slice(siteScript.indexOf('const safeZones'), siteScript.indexOf('let current'));
const compositionFunctions = siteScript.slice(siteScript.indexOf('function hashString'), siteScript.indexOf('function composePage'));
assert.ok(compositionDefinitions.startsWith('const safeZones'), 'Composition constants are inspectable');
assert.ok(compositionFunctions.startsWith('function hashString'), 'Composition functions are inspectable');
new vm.Script(`${compositionDefinitions}\n${compositionFunctions}\n
  const fingerprints = [];
  for (const zineId of ['market','letters','riso','garden','blank']) {
    const occupied = [];
    let fingerprint = '';
    for (const type of ['text','photo','art']) {
      for (let ordinal = 0; ordinal < 5; ordinal += 1) {
        const item = { type, value: 'A deterministic line with enough words to exercise text sizing.' };
        const first = choosePlacement(zineId, 2, item, ordinal, occupied);
        const second = choosePlacement(zineId, 2, item, ordinal, occupied);
        if (JSON.stringify(first) !== JSON.stringify(second)) throw new Error('Composition is not deterministic');
        if (first.x < 0 || first.y < 0 || first.x + first.w > 100 || first.y + first.h > 100) throw new Error('Placement escaped the safe page');
        if (Math.abs(first.rotate) > 2.5) throw new Error('Rotation exceeded the controlled range');
        fingerprint += JSON.stringify(first);
        occupied.push(first);
      }
    }
    fingerprints.push(fingerprint);
  }
  if (new Set(fingerprints).size < 4) throw new Error('Zine recipes are not visually distinct');
`).runInNewContext();
for (const colour of ['#fff6e8', '#e9e29b', '#f2cfbb', '#f1b4af', '#8e9546', '#f28892']) {
  assert.ok(siteStyles.includes(colour), `Interactive desk keeps frozen app colour ${colour}`);
}
const deskDemoCopy = home.slice(home.indexOf('<div class="desk-demo"'), home.indexOf('<div class="journey-panels journey-fallback">'));
assert.ok(!deskDemoCopy.includes('\u2014'), 'No em dashes in interactive desk copy');
assert.doesNotMatch(home, /WCAG/);
assert.match(home, /id="accessibility"/);
assert.match(home, /reduced-motion setting/);
for (const html of [home, download]) assert.match(html, /releases\/download\/v0\.9\.0-beta\.5\/zinely-0\.9\.0-beta\.5-release\.apk/);
for (const html of [home, download, notes]) assert.doesNotMatch(html, /releases\/download\/v0\.9\.0-beta\.4/, 'No superseded APK is offered as the download');
assert.match(releaseNotes, /\| `versionCode` \| 10 \|/);
assert.match(download, /Android version code 10/, 'Download page states the released versionCode');
const sha = releaseNotes.match(/SHA-256 \| `([0-9a-f]{64})`/)[1];
assert.ok(download.includes(`<code>${sha}</code>`), 'Download checksum matches the release record');
assert.match(download, /id="install"/);
assert.match(download, /Google Play<\/dt><dd>Not yet\./);
assert.match(download, /There is no iPhone version/);
assert.match(download, /id="known-limits"[\s\S]*On Android 7, 8 and 9, Save PDF asks for storage access[\s\S]*not yet on a real Android 7, 8 or 9 phone/, 'Download page states the published Save PDF limit and its test coverage');
assert.match(download, /id="known-limits"[\s\S]*TalkBack/, 'Download page keeps the TalkBack focus limitation');
assert.doesNotMatch(notes, /TalkBack focus (?:is )?fixed|fixed TalkBack focus/i, 'Never claim the TalkBack focus limitation is fixed');
assert.match(sitemap, /zinely-android\/download\//);
for (const [name, html] of [['home', home], ['changelog', notes], ['download', download], ['roadmap', roadmap]]) {
  assert.doesNotMatch(html, /beta\.6|App Store|available on (?:Google )?Play/i, `${name}: no unreleased build, iOS or Play claim`);
}
assert.match(roadmap, /still being tested, not in the download/);
assert.match(roadmap, /not announced features/);
for (const [status, label] of [['available', 'Available'], ['development', 'In development'], ['planned', 'Planned'], ['exploring', 'Exploring']]) {
  assert.ok(roadmap.includes(`class="status-chip ${status}">${label}</span>`), `Keep commitment levels distinct: ${label}`);
}
assert.doesNotMatch(roadmap, /restore its regression test|Measure cold/);
for (const id of ['website-september-10', 'website-september-9', 'beta-5', 'beta-4-r3', 'beta-4-r2', 'beta-4', 'beta-3', 'beta-2', 'beta-1', 'early-builds']) {
  assert.ok(notes.includes(`id="${id}"`), `Preserve release bookmark ${id}`);
}
for (const [name, html] of [['index.html', home], ['roadmap/index.html', roadmap], ['changelog/index.html', notes], ['download/index.html', download]]) {
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
for (const html of [home, roadmap, notes, download, notFound, proposal]) assert.ok(!html.includes('\u2014'), 'No em dashes in public website copy');
for (const html of [home, proposal]) {
  for (const [, script] of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/g)) new vm.Script(script);
}
for (const [, script] of appAbout.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/g)) new vm.Script(script);
assert.match(proposal, /Design approved, 12 September 2026/);
assert.match(proposal, /src="\.\.\/mockups\/v21-colophon.html"/);
assert.doesNotMatch(proposal, /<script/); // One canonical copy, no injected duplicate.
assert.match(appAbout, /Aastra, the two people behind Zinely/);
assert.match(appAbout, /A little about this little app/);
assert.match(appAbout, /little paper books/);
assert.match(appAbout, /making an Android app/);
assert.match(appAbout, /The page order became our problem\. What goes on the pages is entirely yours\./);
assert.match(appAbout, /Thanks for making something with it\./);
assert.match(appAbout, /Licences &amp; credits/);
assert.match(appAbout, /Open-source notices/);
assert.match(appAbout, /Zinely uses a few open-source typefaces\. Their licence notices live here\./);
assert.doesNotMatch(appAbout, /Fonts we use|Warm, handmade lettering|A little bookish flair|Clear, everyday text/);
for (const family of ['Averia Sans Libre', 'Fraunces', 'Inter']) {
  assert.match(appAbout, new RegExp(`data-name="${family}"`));
}
assert.doesNotMatch(appAbout, /Sometimes you want to make something you can hand to someone/);
assert.ok(!appAbout.match(/<section class="section maker-note"[\s\S]*?<\/section>/)[0].includes('\u2014'));
assert.match(home, /We handle the page order\. You decide what deserves eight pages\./);
for (const html of [home, proposal]) {
  assert.doesNotMatch(html, /This got slightly out of hand/);
}
const about = home.match(/<section class="about-card"[\s\S]*?<\/section>/);
assert.ok(about, 'Homepage About section is present');
assert.ok(!about[0].includes('\u2014'), 'No em dashes in About copy');
assert.equal((about[0].match(/mailto:/g) || []).length, 0, 'About leaves the single email route in the footer');
assert.equal((about[0].match(/href="privacy\//g) || []).length, 1, 'About links the policy only where feedback handling is explained');
console.log('PASS: public copy, local links/anchors, release bookmarks, stable download, and proposal script syntax. Source-only checks.');
