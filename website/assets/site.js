(() => {
  const journey = document.querySelector('[data-desk-journey]');
  if (!journey) return;
  const demo = journey.querySelector('[data-desk-demo]');
  const fallback = journey.querySelector('.journey-fallback');
  const screens = [...demo.querySelectorAll('[data-demo-screen]')];
  const pageTabs = demo.querySelector('[data-page-tabs]');
  const pageCanvas = demo.querySelector('[data-page-canvas]');
  const proofCanvas = demo.querySelector('[data-proof-canvas]');
  const dialog = demo.querySelector('[data-demo-add]');
  const status = demo.querySelector('[data-demo-status]');
  const undoButton = demo.querySelector('[data-demo-undo]');
  const pagePosition = demo.querySelector('[data-page-position]');
  const proofPosition = demo.querySelector('[data-proof-position]');
  const zineTitle = demo.querySelector('[data-zine-title]');
  const addButton = demo.querySelector('[data-open-add]');
  const benchScreen = demo.querySelector('[data-demo-screen="bench"]');
  const traySiblings = [...benchScreen.children].filter(child => child !== dialog && !child.matches('[data-page-position]'));
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  const itemLabels = {
    text: ['A small thought with somewhere to go.', 'Changed my mind. Kept the good bit.', 'Notes from the last stall on the left.'],
    photo: ['A sample photo of flowers at a market', 'A sample photo of afternoon shadows', 'A sample photo of tomatoes on a table'],
    art: ['A wonky little star', 'A tiny paper flower', 'A very determined sprout']
  };
  const itemMarks = { art: ['\u2726', '\u273f', '\u219f'] };
  const zines = {
    market: {
      title: 'Sunday market',
      pages: [
        [{ type: 'art', label: 'Two overlapping circles', mark: '\u25cb\u25cb' }, { type: 'text', value: 'Sunday market' }],
        [{ type: 'text', value: 'Go early. Bring a bag.' }, { type: 'photo', label: 'A sample photo of flowers at a market' }, { type: 'text', value: 'Saturday, just after the rain.' }],
        [{ type: 'text', value: 'Things overheard near the bread stall' }, { type: 'text', value: '\u201cTwo, please. The wonky ones.\u201d' }],
        [{ type: 'art', label: 'A tiny paper flower', mark: '\u273f' }, { type: 'text', value: 'Flowers that survived the bus home.' }],
        [{ type: 'photo', label: 'A sample photo of tomatoes on a table' }, { type: 'text', value: 'Red, orange, almost suspiciously perfect.' }],
        [{ type: 'text', value: 'One excellent peach. No notes.' }],
        [{ type: 'art', label: 'A wonky little star', mark: '\u2726' }],
        [{ type: 'text', value: 'See you next Sunday.' }]
      ]
    },
    garden: {
      title: 'Mum\u2019s garden',
      openAt: 2,
      pages: [
        [{ type: 'art', label: 'A very determined sprout', mark: '\u219f' }, { type: 'text', value: 'Mum\u2019s garden' }],
        [{ type: 'photo', label: 'A sample photo of tomatoes on a table' }, { type: 'text', value: 'The tomatoes came up in July.' }],
        [{ type: 'text', value: 'Mum\u2019s garden' }, { type: 'photo', label: 'A sample photo of tomatoes on a table' }, { type: 'text', value: 'The tomatoes came up in July, all at once, and nobody in this house wanted to eat another tomato by August.' }, { type: 'art', label: 'A very determined sprout', mark: '\u219f' }],
        [{ type: 'art', label: 'A tiny paper flower', mark: '\u273f' }, { type: 'text', value: 'Things that grew despite our planning.' }],
        [{ type: 'photo', label: 'A sample photo of afternoon shadows' }, { type: 'text', value: 'Four o\u2019clock, behind the shed.' }],
        [{ type: 'text', value: 'Water, wait, look again.' }],
        [{ type: 'art', label: 'A wonky little star', mark: '\u2726' }],
        [{ type: 'text', value: 'Made with dirt under the fingernails.' }]
      ]
    },
    riso: {
      title: 'Riso tests',
      pages: [
        [{ type: 'art', label: 'A wonky little star', mark: '\u2726' }, { type: 'text', value: 'Riso tests' }],
        [{ type: 'text', value: 'Pass one: strawberry' }, { type: 'photo', label: 'A sample block of pink ink' }],
        [{ type: 'text', value: 'The misregistration is doing its best.' }],
        [{ type: 'art', label: 'Two overlapping circles', mark: '\u25cb\u25cb' }],
        [{ type: 'text', value: 'Pass two: forest' }, { type: 'photo', label: 'A sample block of green ink' }],
        [{ type: 'text', value: 'Pink over green. Try it twice.' }],
        [{ type: 'art', label: 'A very determined sprout', mark: '\u219f' }],
        [{ type: 'text', value: 'Test complete. Probably.' }]
      ]
    },
    letters: {
      title: 'Letters home',
      pages: [
        [{ type: 'art', label: 'A small envelope', mark: '\u2709' }, { type: 'text', value: 'Letters home' }],
        [{ type: 'text', value: 'Dear you,' }, { type: 'text', value: 'The kettle is loud and the house is otherwise quiet.' }],
        [{ type: 'photo', label: 'A sample photo of afternoon shadows' }, { type: 'text', value: 'The light reached the kitchen at four.' }],
        [{ type: 'text', value: 'I wrote this down before I forgot.' }],
        [{ type: 'art', label: 'A wonky little star', mark: '\u2726' }],
        [{ type: 'text', value: 'More soon.' }],
        [{ type: 'photo', label: 'A sample photo of flowers at a market' }],
        [{ type: 'text', value: 'With love, from here.' }]
      ]
    }
  };
  const safeZones = {
    text: [
      { x: 8, y: 7, w: 84, h: 18 }, { x: 8, y: 73, w: 84, h: 18 },
      { x: 8, y: 39, w: 58, h: 21 }, { x: 34, y: 37, w: 58, h: 21 },
      { x: 8, y: 23, w: 62, h: 18 }, { x: 8, y: 57, w: 62, h: 18 }
    ],
    photo: [
      { x: 10, y: 27, w: 80, h: 37 }, { x: 10, y: 8, w: 80, h: 38 },
      { x: 10, y: 54, w: 80, h: 34 }, { x: 24, y: 24, w: 66, h: 45 },
      { x: 8, y: 31, w: 67, h: 41 }
    ],
    art: [
      { x: 72, y: 9, w: 18, h: 18 }, { x: 9, y: 10, w: 18, h: 18 },
      { x: 72, y: 70, w: 18, h: 18 }, { x: 9, y: 70, w: 18, h: 18 },
      { x: 70, y: 39, w: 18, h: 18 }, { x: 12, y: 42, w: 18, h: 18 },
      { x: 42, y: 14, w: 18, h: 18 }, { x: 42, y: 68, w: 18, h: 18 }
    ]
  };
  const compactZones = {
    text: [
      { x: 7, y: 78, w: 40, h: 14 }, { x: 53, y: 78, w: 40, h: 14 },
      { x: 7, y: 43, w: 46, h: 15 }, { x: 47, y: 42, w: 46, h: 15 }
    ],
    photo: [
      { x: 7, y: 8, w: 48, h: 30 }, { x: 45, y: 8, w: 48, h: 30 },
      { x: 7, y: 61, w: 42, h: 29 }, { x: 50, y: 59, w: 42, h: 29 },
      { x: 26, y: 34, w: 48, h: 31 }
    ],
    art: [
      { x: 74, y: 8, w: 16, h: 16 }, { x: 10, y: 8, w: 16, h: 16 },
      { x: 74, y: 76, w: 16, h: 16 }, { x: 10, y: 76, w: 16, h: 16 }
    ]
  };
  const recipes = {
    market: { shift: 1, rotations: [-2, -1, .6, 1.5], ink: 'warm' },
    letters: { shift: 3, rotations: [-1.2, -.4, .4, 1], ink: 'quiet' },
    riso: { shift: 5, rotations: [-2.4, -1.2, 1.2, 2.4], ink: 'overprint' },
    garden: { shift: 2, rotations: [-1.8, -.7, .8, 1.6], ink: 'botanical' },
    blank: { shift: 0, rotations: [-1.5, -.5, .5, 1.5], ink: 'paper' }
  };
  const artPaths = {
    sprout: 'M12 21V9M12 9c0-4 3-6 6-6 0 4-2.5 6-6 6ZM12 13c0-3-2.5-5-5-5 0 3 2 5 5 5Z',
    flower: 'M12 8c-5-6-9 2-3 4-6 2-2 10 3 4 5 6 9-2 3-4 6-2 2-10-3-4Z',
    envelope: 'M3 6h18v12H3zM3 7l9 6 9-6',
    rings: 'M10 17a6 6 0 1 1 0-12 6 6 0 0 1 0 12Zm4-10a6 6 0 1 1 0 12 6 6 0 0 1 0-12Z',
    star: 'M12 3l2.6 6.2 6.4.6-4.9 4.2 1.5 6.3L12 17l-5.6 3.3 1.5-6.3L3 9.8l6.4-.6Z'
  };
  let current = null;
  let pageIndex = 0;
  let proofIndex = 0;
  let undo = null;
  let lastPlacedId = null;
  let trayCloseTimer = 0;

  function hashString(value) {
    let hash = 2166136261;
    for (const character of value) {
      hash ^= character.charCodeAt(0);
      hash = Math.imul(hash, 16777619);
    }
    return hash >>> 0;
  }

  function cloneItem(item) {
    return { ...item, placement: item.placement ? { ...item.placement } : undefined };
  }

  function overlaps(a, b, gap = 2.5) {
    return !(a.x + a.w + gap <= b.x || b.x + b.w + gap <= a.x
      || a.y + a.h + gap <= b.y || b.y + b.h + gap <= a.y);
  }

  function overlapArea(a, b) {
    const width = Math.max(0, Math.min(a.x + a.w, b.x + b.w) - Math.max(a.x, b.x));
    const height = Math.max(0, Math.min(a.y + a.h, b.y + b.h) - Math.max(a.y, b.y));
    return width * height;
  }

  function choosePlacement(zineId, page, item, ordinal, occupied, allowCompact = false) {
    const recipe = recipes[zineId] || recipes.blank;
    const seed = hashString(`${zineId}:${page}:${item.type}:${ordinal}`);
    const estimatedHeight = item.type === 'text'
      ? Math.min(34, 13 + Math.ceil((item.value || '').length / 38) * 5)
      : null;
    const fitText = zone => estimatedHeight
      ? { ...zone, h: Math.max(zone.h, estimatedHeight), y: Math.min(zone.y, 94 - Math.max(zone.h, estimatedHeight)) }
      : zone;
    const zones = safeZones[item.type].map(fitText);
    const compact = compactZones[item.type].map(fitText);
    const start = (seed + recipe.shift) % zones.length;
    const compactStart = (seed + recipe.shift) % compact.length;
    const ordered = zones.map((_, offset) => zones[(start + offset) % zones.length]);
    if (allowCompact) ordered.push(...compact.map((_, offset) => compact[(compactStart + offset) % compact.length]));
    const open = ordered.find(zone => occupied.every(other => !overlaps(zone, other)));
    const zone = open || ordered.reduce((best, candidate) => {
      const score = occupied.reduce((total, other) => {
        const sameSpot = candidate.x === other.x && candidate.y === other.y
          && candidate.w === other.w && candidate.h === other.h;
        const weight = other.kind === 'text' ? (item.type === 'text' ? 6 : 4)
          : other.kind === 'photo' && item.type === 'photo' ? 3
          : 1;
        return total + overlapArea(candidate, other) * weight + (sameSpot ? 10000 : 0);
      }, 0);
      return score < best.score ? { zone: candidate, score } : best;
    }, { zone: ordered[0], score: Infinity }).zone;
    const rotations = recipe.rotations;
    return { ...zone, rotate: rotations[(seed >>> 5) % rotations.length], kind: item.type, layered: allowCompact && !open };
  }

  function composePage(zineId, page, pageNumber) {
    const occupied = [];
    const counts = { text: 0, photo: 0, art: 0 };
    page.forEach(item => {
      const ordinal = counts[item.type]++;
      item.id ||= `${zineId}-${pageNumber}-${item.type}-${ordinal}`;
      item.placement ||= choosePlacement(zineId, pageNumber, item, ordinal, occupied);
      occupied.push(item.placement);
    });
  }

  function composePages(zine) {
    zine.pages.forEach((page, index) => composePage(zine.id, page, index));
  }

  function clonePages(pages) {
    return pages.map(page => page.map(cloneItem));
  }

  function announce(message) {
    status.textContent = '';
    requestAnimationFrame(() => { status.textContent = message; });
  }

  function showScreen(name, shouldFocus = true) {
    if (name !== 'bench' && dialog.open) closeTray(false, true);
    screens.forEach(screen => {
      const active = screen.dataset.demoScreen === name;
      screen.hidden = !active;
      screen.inert = !active;
    });
    if (shouldFocus) screens.find(screen => screen.dataset.demoScreen === name).querySelector('[tabindex="-1"]')?.focus();
  }

  function renderPage(target, index, proof = false) {
    target.replaceChildren();
    target.dataset.zine = current.id;
    target.dataset.page = String(index + 1);
    target.classList.toggle('is-empty', current.pages[index].length === 0);
    if (current.pages[index].length === 0) {
      const hint = document.createElement('p');
      hint.className = 'demo-empty-page';
      hint.textContent = 'A blank page. Suspiciously full of potential.';
      target.append(hint);
    }
    let textIndex = 0;
    current.pages[index].forEach(item => {
      let element;
      if (item.type === 'text') {
        const text = document.createElement('p');
        text.className = `demo-page-text demo-composed-item${textIndex === 0 ? ' is-heading' : ' is-body'}`;
        text.textContent = item.value;
        textIndex += 1;
        element = text;
      } else if (item.type === 'photo') {
        const photo = document.createElement('div');
        photo.className = 'demo-page-photo demo-composed-item';
        photo.dataset.photo = item.label.includes('flowers') ? 'flowers'
          : item.label.includes('tomatoes') ? 'tomatoes'
          : item.label.includes('shadows') ? 'shadows'
          : item.label.includes('pink ink') ? 'pink-ink'
          : item.label.includes('green ink') ? 'green-ink'
          : 'paper';
        photo.setAttribute('role', 'img');
        photo.setAttribute('aria-label', item.label);
        element = photo;
      } else {
        const art = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        art.setAttribute('class', 'demo-page-art demo-composed-item');
        art.setAttribute('role', 'img');
        art.setAttribute('aria-label', item.label);
        art.setAttribute('viewBox', '0 0 24 24');
        const shape = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        const artKind = item.label.includes('sprout') ? 'sprout'
          : item.label.includes('flower') ? 'flower'
          : item.label.includes('envelope') ? 'envelope'
          : item.label.includes('overlapping') ? 'rings'
          : 'star';
        art.dataset.art = artKind;
        shape.setAttribute('d', artPaths[artKind]);
        art.append(shape);
        element = art;
      }
      const placement = item.placement;
      element.dataset.itemId = item.id;
      element.classList.toggle('is-layered', Boolean(placement.layered));
      element.style.setProperty('--item-x', `${placement.x}%`);
      element.style.setProperty('--item-y', `${placement.y}%`);
      element.style.setProperty('--item-w', `${placement.w}%`);
      element.style.setProperty('--item-h', `${placement.h}%`);
      element.style.setProperty('--item-rotate', `${placement.rotate}deg`);
      if (!proof && item.id === lastPlacedId) element.classList.add('is-set-down');
      target.append(element);
    });
    const number = document.createElement('small');
    number.className = 'demo-page-number';
    number.textContent = String(index + 1);
    target.append(number);
    if (proof) target.setAttribute('aria-label', `Page ${index + 1} preview`);
  }

  function selectPage(index, focusTab = false) {
    pageIndex = Math.max(0, Math.min(7, index));
    [...pageTabs.children].forEach((tab, tabIndex) => {
      const active = tabIndex === pageIndex;
      tab.setAttribute('aria-selected', String(active));
      tab.tabIndex = active ? 0 : -1;
    });
    pageCanvas.setAttribute('aria-labelledby', `demo-page-tab-${pageIndex + 1}`);
    pagePosition.textContent = `Page ${pageIndex + 1} of 8`;
    renderPage(pageCanvas, pageIndex);
    undoButton.disabled = !undo || undo.page !== pageIndex;
    if (focusTab) pageTabs.children[pageIndex].focus();
  }

  function buildPageTabs() {
    pageTabs.replaceChildren();
    for (let index = 0; index < 8; index += 1) {
      const tab = document.createElement('button');
      tab.type = 'button';
      tab.id = `demo-page-tab-${index + 1}`;
      tab.setAttribute('role', 'tab');
      tab.setAttribute('aria-controls', 'demo-page-canvas');
      tab.setAttribute('aria-label', `Page ${index + 1}`);
      tab.dataset.hasContent = String(current.pages[index].length > 0);
      tab.textContent = String(index + 1);
      tab.addEventListener('click', () => selectPage(index));
      tab.addEventListener('keydown', event => {
        const target = event.key === 'ArrowRight' ? (index + 1) % 8
          : event.key === 'ArrowLeft' ? (index + 7) % 8
          : event.key === 'Home' ? 0
          : event.key === 'End' ? 7
          : null;
        if (target === null) return;
        event.preventDefault();
        selectPage(target, true);
      });
      pageTabs.append(tab);
    }
  }

  function openZine(key) {
    const source = key === 'blank'
      ? { id: 'blank', title: 'Untitled little zine', pages: Array.from({ length: 8 }, () => []) }
      : zines[key];
    current = { id: source.id || key, title: source.title, pages: clonePages(source.pages) };
    composePages(current);
    pageIndex = source.openAt || 0;
    proofIndex = 0;
    undo = null;
    lastPlacedId = null;
    zineTitle.textContent = current.title;
    buildPageTabs();
    selectPage(pageIndex);
    showScreen('bench');
    announce(`${current.title} opened on the Bench. Page ${pageIndex + 1} of 8.`);
  }

  function showProof() {
    proofIndex = pageIndex;
    renderPage(proofCanvas, proofIndex, true);
    updateProofControls();
    showScreen('proof');
    announce(`Proof opened at page ${proofIndex + 1} of 8.`);
  }

  function updateProofControls() {
    const pageName = proofIndex === 0 ? 'Cover' : proofIndex === 7 ? 'Back' : `Page ${proofIndex + 1}`;
    proofPosition.textContent = `${pageName} \u00b7 ${proofIndex + 1} of 8`;
    demo.querySelector('[data-proof-previous]').disabled = proofIndex === 0;
    demo.querySelector('[data-proof-next]').disabled = proofIndex === 7;
  }

  function moveProof(amount) {
    proofIndex = Math.max(0, Math.min(7, proofIndex + amount));
    renderPage(proofCanvas, proofIndex, true);
    updateProofControls();
    announce(`Page ${proofIndex + 1} of 8.`);
  }

  function setBenchInert(value) {
    traySiblings.forEach(element => { element.inert = value; });
  }

  function openTray() {
    if (dialog.open) return;
    clearTimeout(trayCloseTimer);
    dialog.classList.remove('is-closing');
    dialog.show();
    benchScreen.classList.add('is-tray-open');
    addButton.setAttribute('aria-expanded', 'true');
    setBenchInert(true);
    requestAnimationFrame(() => dialog.querySelector('[data-add-element]').focus());
    announce('Supplies opened. Choose Text, Photo, or Art.');
  }

  function finishClosingTray(restoreFocus) {
    if (!dialog.open) return;
    dialog.close();
    dialog.classList.remove('is-closing');
    benchScreen.classList.remove('is-tray-open');
    addButton.setAttribute('aria-expanded', 'false');
    setBenchInert(false);
    if (restoreFocus && !benchScreen.hidden) addButton.focus();
  }

  function closeTray(restoreFocus = true, immediate = false) {
    if (!dialog.open) return;
    clearTimeout(trayCloseTimer);
    if (immediate || reducedMotion.matches) {
      finishClosingTray(restoreFocus);
      return;
    }
    dialog.classList.add('is-closing');
    trayCloseTimer = window.setTimeout(() => finishClosingTray(restoreFocus), 170);
  }

  function createAddedItem(type) {
    const page = current.pages[pageIndex];
    const ordinal = page.filter(item => item.type === type).length;
    const id = `${current.id}-${pageIndex}-${type}-${ordinal}`;
    const seed = hashString(id);
    const labels = itemLabels[type];
    const label = labels[seed % labels.length];
    const item = type === 'text' ? { id, type, value: label }
      : type === 'photo' ? { id, type, label }
      : { id, type, label, mark: itemMarks.art[seed % itemMarks.art.length] };
    item.placement = choosePlacement(current.id, pageIndex, item, ordinal, page.map(existing => existing.placement), true);
    return item;
  }

  function trapTrayFocus(event) {
    if (event.key === 'Escape') {
      event.preventDefault();
      closeTray();
      return;
    }
    if (event.key !== 'Tab') return;
    const focusable = [...dialog.querySelectorAll('button:not(:disabled)')];
    const first = focusable[0];
    const last = focusable.at(-1);
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  demo.querySelectorAll('[data-open-zine]').forEach(button => button.addEventListener('click', () => openZine(button.dataset.openZine)));
  demo.querySelector('[data-make-zine]').addEventListener('click', () => openZine('blank'));
  demo.querySelector('[data-back-to-shelf]').addEventListener('click', () => {
    showScreen('shelf');
    announce('Back on My Shelf. Demo changes were cleared.');
  });
  demo.querySelector('[data-open-proof]').addEventListener('click', showProof);
  demo.querySelector('[data-back-to-bench]').addEventListener('click', () => {
    pageIndex = proofIndex;
    selectPage(pageIndex);
    showScreen('bench');
    announce(`Back on the Bench. Page ${pageIndex + 1} of 8.`);
  });
  demo.querySelector('[data-proof-previous]').addEventListener('click', () => moveProof(-1));
  demo.querySelector('[data-proof-next]').addEventListener('click', () => moveProof(1));
  addButton.addEventListener('click', openTray);
  dialog.querySelector('[data-close-add]').addEventListener('click', () => closeTray());
  dialog.addEventListener('keydown', trapTrayFocus);
  demo.querySelectorAll('[data-add-element]').forEach(button => button.addEventListener('click', () => {
    const type = button.dataset.addElement;
    undo = { page: pageIndex, items: current.pages[pageIndex].map(cloneItem) };
    const item = createAddedItem(type);
    current.pages[pageIndex].push(item);
    lastPlacedId = item.id;
    selectPage(pageIndex);
    closeTray(true);
    announce(`${type[0].toUpperCase()}${type.slice(1)} added to page ${pageIndex + 1}.`);
    window.setTimeout(() => {
      pageCanvas.querySelector(`[data-item-id="${item.id}"]`)?.classList.remove('is-set-down');
      if (lastPlacedId === item.id) lastPlacedId = null;
    }, reducedMotion.matches ? 0 : 520);
  }));
  undoButton.addEventListener('click', () => {
    if (!undo || undo.page !== pageIndex) return;
    current.pages[pageIndex] = undo.items;
    undo = null;
    lastPlacedId = null;
    selectPage(pageIndex);
    announce(`Last addition removed from page ${pageIndex + 1}.`);
  });
  demo.querySelector('[data-demo-save]').addEventListener('click', () => {
    announce('Demo complete. Nothing was downloaded. The Android app would make the one-sheet PDF here.');
    const button = demo.querySelector('[data-demo-save]');
    button.textContent = 'Demo complete';
    button.disabled = true;
    const note = document.createElement('p');
    note.className = 'demo-save-note';
    note.textContent = 'Nothing was downloaded. In the Android app, this is where your one-sheet PDF appears.';
    if (!demo.querySelector('.demo-save-note')) button.closest('.demo-export-card').append(note);
  });
  demo.querySelector('[data-demo-share]').addEventListener('click', () => {
    announce('This browser demo does not create a file to share. The Android app opens your share sheet here.');
    const note = document.createElement('p');
    note.className = 'demo-save-note';
    note.textContent = 'Nothing was shared. The Android app would open your share sheet with the same PDF.';
    const card = demo.querySelector('.demo-export-card');
    const existing = card.querySelector('.demo-save-note');
    if (existing) existing.replaceWith(note);
    else card.append(note);
  });
  window.addEventListener('beforeprint', () => closeTray(false, true));

  fallback.hidden = true;
  demo.hidden = false;
  journey.classList.add('is-enhanced');
  showScreen('shelf', false);
})();

(() => {
  const guide = document.querySelector('.fold-guide');
  if (!guide) return;
  const cards = [...guide.querySelectorAll('.fold-card')];
  const studio = guide.querySelector('.fold-studio');
  const stage = guide.querySelector('.fold-stage');
  const livePicture = guide.querySelector('#fold-live-picture');
  const liveTitle = guide.querySelector('#fold-live-title');
  const liveNumber = guide.querySelector('#fold-live-number');
  const liveCopy = guide.querySelector('#fold-live-copy');
  const picker = guide.querySelector('#fold-step');
  const rangeLabel = guide.querySelector('#fold-range-label');
  const previous = guide.querySelector('#fold-previous');
  const next = guide.querySelector('#fold-next');
  const view = guide.querySelector('#fold-view');
  const play = guide.querySelector('#fold-play');
  const reset = guide.querySelector('#fold-reset');
  const note = guide.querySelector('#fold-motion-note');
  const status = guide.querySelector('#direction-status');
  const position = guide.querySelector('.fold-position');
  const motion = matchMedia('(prefers-reduced-motion: reduce)');
  const originals = cards.map(card => card.querySelector('svg').cloneNode(true));
  // Unfold/checkpoint and final book pictures stay static until physical-paper
  // validation justifies a more complex simulation or camera change.
  const animated = new Set([0, 1, 2, 4, 5, 8]);
  let index = 0;
  let allSteps = false;
  let frame = 0;
  let elapsed = 0;
  let running = false;
  let lastTime = null;
  const duration = 2400;

  function stop() {
    cancelAnimationFrame(frame);
    frame = 0;
    running = false;
    lastTime = null;
  }

  function restore() {
    stop();
    elapsed = 0;
    const original = originals[index];
    livePicture.setAttribute('viewBox', original.getAttribute('viewBox'));
    livePicture.innerHTML = original.innerHTML;
  }

  function updateControls() {
    previous.disabled = index === 0;
    next.disabled = index === cards.length - 1;
    picker.value = index;
    picker.style.setProperty('--fold-progress', `${index / (cards.length - 1) * 100}%`);
    picker.setAttribute('aria-valuetext', `Step ${index + 1} of ${cards.length}: ${cards[index].querySelector('h3').textContent.replace(/^\d+/, '').trim()}`);
    rangeLabel.textContent = `Step ${index + 1} of ${cards.length}`;
    play.disabled = !animated.has(index) || motion.matches;
    reset.disabled = elapsed === 0;
    play.textContent = running ? 'Pause fold' : elapsed >= duration ? 'Replay fold' : elapsed > 0 ? 'Resume fold' : 'Fold this step';
    note.textContent = motion.matches ? 'Reduced motion is on. Follow the static picture and instructions.'
      : !animated.has(index) ? 'A static checkpoint: compare your paper with this picture.'
      : index === 8 ? 'Top view: watch the slit open into a diamond, then a cross. Pause at any point.'
      : 'Motion starts only when you ask. Pause at any point.';
  }

  function showStep(newIndex, announce = true) {
    stop();
    index = Math.max(0, Math.min(cards.length - 1, newIndex));
    elapsed = 0;
    restore();
    const title = cards[index].querySelector('h3').textContent.replace(/^\d+/, '').trim();
    const lines = [...cards[index].querySelectorAll('p span')].map(line => line.textContent);
    liveNumber.textContent = index + 1;
    liveTitle.textContent = title;
    liveCopy.replaceChildren(...lines.flatMap((line, lineIndex) => {
      const nodes = [document.createTextNode(line)];
      if (lineIndex < lines.length - 1) nodes.push(document.createElement('br'));
      return nodes;
    }));
    stage.dataset.step = String(index + 1);
    cards.forEach((card, i) => {
      card.inert = !allSteps;
      card.setAttribute('aria-hidden', String(!allSteps));
    });
    livePicture.removeAttribute('role');
    livePicture.removeAttribute('tabindex');
    livePicture.removeAttribute('aria-label');
    livePicture.setAttribute('aria-hidden', 'true');
    if (!allSteps && animated.has(index) && !motion.matches) {
      livePicture.removeAttribute('aria-hidden');
      livePicture.setAttribute('role', 'button');
      livePicture.setAttribute('tabindex', '0');
      livePicture.setAttribute('aria-label', `Animate fold for step ${index + 1}`);
    }
    updateControls();
    if (position) {
      position.hidden = allSteps;
      position.style.setProperty('--position', (index + 1) / cards.length);
      position.querySelector('#fold-position-label').textContent = `Step ${index + 1} of ${cards.length}`;
      guide.querySelector('.fold-finish').textContent = index === cards.length - 1 && !allSteps
        ? 'Last fold. If yours looks like a little book, you are now a publisher. Very small circulation. Still counts.'
        : 'Made by you. Ready to pass on.';
    }
    if (announce) status.textContent = `Step ${index + 1} of ${cards.length}. ${title}. ${lines.join(' ')}`;
  }

  function polygon(points, className = 'sheet') {
    return `<polygon class="${className}" points="${points.map(point => point.join(',')).join(' ')}"/>`;
  }

  function draw(progress) {
    const t = (1 - Math.cos(Math.PI * progress)) / 2;
    let drawing = '';
    if (index === 0) {
      const y = 85 + 61 * Math.cos(Math.PI * t);
      const lift = 18 * Math.sin(Math.PI * t);
      drawing = polygon([[30, 24], [210, 24], [210, 85], [30, 85]])
        + polygon([[30, 85], [210, 85], [210 - lift, y], [30 - lift, y]], 'sheet moving-paper')
        + '<path class="crease-line" d="M30 85H210"/>';
    } else if ([1, 2, 4].includes(index)) {
      const [left, right, top, bottom] = index === 2 ? [60, 180, 43, 127] : index === 4 ? [30, 210, 24, 146] : [30, 210, 56, 120];
      const middle = (left + right) / 2;
      const x = middle + (right - middle) * Math.cos(Math.PI * t);
      const lift = 18 * Math.sin(Math.PI * t);
      drawing = polygon([[left, top], [middle, top], [middle, bottom], [left, bottom]])
        + polygon([[middle, top], [x, top - lift], [x, bottom - lift], [middle, bottom]], 'sheet moving-paper')
        + `<path class="crease-line" d="M${middle} ${top}V${bottom}"/>`;
    } else if (index === 5) {
      // From the folded edge through both layers, only to the midpoint.
      drawing = originals[index].innerHTML.replace('d="M155 85H110"', `d="M155 85H${155 - 45 * t}"`);
    } else if (index === 8) {
      // Plan view: middle panel lengths remain 45 units, outer panels stay joined.
      const w = 45 * Math.cos(Math.PI * t / 2);
      const h = 45 * Math.sin(Math.PI * t / 2);
      drawing = polygon([[75 - w, 79], [120 - w, 79], [120 - w, 91], [75 - w, 91]])
        + polygon([[120 + w, 79], [165 + w, 79], [165 + w, 91], [120 + w, 91]])
        + `<path class="fold-edge" d="M${120 - w} 85L120 ${85 - h}L${120 + w} 85L120 ${85 + h}Z"/>`
        + '<path class="direction" d="M30 145H65m0 0-8-5m8 5-8 5M210 145H175m0 0 8-5m-8 5 8 5"/>'
        + '<text x="91" y="20">top view</text>';
    }
    livePicture.innerHTML = drawing;
  }

  function tick(time) {
    if (!running) return;
    if (lastTime !== null) elapsed = Math.min(duration, elapsed + time - lastTime);
    lastTime = time;
    draw(elapsed / duration);
    if (elapsed >= duration) {
      stop();
      updateControls();
      status.textContent = `Direction for step ${index + 1} finished. Follow the written instruction before moving on.`;
    } else frame = requestAnimationFrame(tick);
  }

  function toggleFold() {
    if (motion.matches || !animated.has(index)) return;
    if (running) stop();
    else {
      if (elapsed >= duration) elapsed = 0;
      running = true;
      lastTime = null;
      frame = requestAnimationFrame(tick);
    }
    updateControls();
  }

  play.addEventListener('click', toggleFold);
  livePicture.addEventListener('click', toggleFold);
  livePicture.addEventListener('keydown', event => {
    if (!['Enter', ' '].includes(event.key)) return;
    event.preventDefault();
    toggleFold();
  });
  reset.addEventListener('click', () => showStep(index, false));
  previous.addEventListener('click', () => showStep(index - 1));
  next.addEventListener('click', () => showStep(index + 1));
  picker.addEventListener('input', () => showStep(Number(picker.value), false));
  picker.addEventListener('change', () => showStep(Number(picker.value)));
  view.addEventListener('click', () => {
    allSteps = !allSteps;
    guide.classList.toggle('fold-focused', !allSteps);
    stage.hidden = allSteps;
    guide.querySelector('.fold-playback').hidden = allSteps;
    guide.querySelector('.fold-navigation').hidden = allSteps;
    guide.querySelector('.fold-position').hidden = allSteps;
    guide.querySelector('.fold-finish').hidden = allSteps;
    view.setAttribute('aria-pressed', String(allSteps));
    view.textContent = allSteps ? 'Back to folding desk' : 'Read all steps';
    showStep(index, false);
    status.textContent = allSteps ? 'All ten steps shown in order.' : `Step ${index + 1} of ${cards.length}.`;
  });
  motion.addEventListener('change', () => showStep(index, false));
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) { stop(); updateControls(); }
  });
  // Without JS the complete ordered paper guide remains readable.
  studio.hidden = false;
  guide.classList.add('fold-focused');
  showStep(0, false);
})();

// Small, independent enhancements. The page and native disclosures also work without JS.
(() => {
  const toggle = document.querySelector('#page-order-toggle');
  if (toggle) {
    const sheet = document.querySelector('#page-order-sheet');
    const label = document.querySelector('#page-order-label');
    const note = document.querySelector('#page-order-note');
    toggle.hidden = false;
    toggle.addEventListener('click', () => {
      const print = toggle.getAttribute('aria-pressed') !== 'true';
      toggle.setAttribute('aria-pressed', String(print));
      sheet.classList.toggle('is-print', print);
      label.textContent = print ? 'Print layout' : 'Reading order';
      note.textContent = print
        ? 'Print layout: 5, 4, 3, 2 upside down; 6, 7, 8, 1 upright. Looks wrong. Folds right. Press again for reading order.'
        : 'Reading order: 1 to 8, front cover to back cover. A layout comparison, not a folding simulation.';
    });
  }

  const ideas = [
    'A tiny field guide to the cats on your street.',
    'Eight pages defending your most unreasonable sandwich opinion.',
    'A museum of things you found in your pockets.',
    'A photo essay about a walk that was supposed to take ten minutes.',
    'A very small cookbook for one very specific friend.',
    'The holiday photos, including the one nobody posed for.'
  ];
  const another = document.querySelector('#another-idea');
  if (another) {
    let idea = 0;
    another.hidden = false;
    another.addEventListener('click', () => {
      idea = (idea + 1) % ideas.length;
      document.querySelector('#zine-idea').textContent = ideas[idea];
    });
  }

  const paper = document.querySelector('.paper-story');
  if (!paper) return;
  const allowed = matchMedia('(hover: hover) and (pointer: fine) and (min-width: 821px) and (prefers-reduced-motion: no-preference)');
  let frame = 0;
  let point = null;
  function rest() {
    cancelAnimationFrame(frame);
    frame = 0;
    point = null;
    paper.style.removeProperty('--paper-x');
    paper.style.removeProperty('--paper-y');
  }
  paper.addEventListener('pointermove', event => {
    if (!allowed.matches || event.pointerType !== 'mouse') return;
    point = { x: event.clientX, y: event.clientY };
    if (frame) return;
    frame = requestAnimationFrame(() => {
      frame = 0;
      const box = paper.getBoundingClientRect();
      const x = Math.max(-.5, Math.min(.5, (point.x - box.left) / box.width - .5));
      const y = Math.max(-.5, Math.min(.5, (point.y - box.top) / box.height - .5));
      paper.style.setProperty('--paper-x', `${-y * 5}deg`);
      paper.style.setProperty('--paper-y', `${x * 5}deg`);
    });
  });
  paper.addEventListener('pointerleave', rest);
  allowed.addEventListener('change', rest);
  document.addEventListener('visibilitychange', () => { if (document.hidden) rest(); });
})();
