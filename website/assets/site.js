(() => {
  const guide = document.querySelector('.fold-guide');
  if (!guide) return;
  const cards = [...guide.querySelectorAll('.fold-card')];
  const picker = guide.querySelector('#fold-step');
  const previous = guide.querySelector('#fold-previous');
  const next = guide.querySelector('#fold-next');
  const view = guide.querySelector('#fold-view');
  const play = guide.querySelector('#fold-play');
  const reset = guide.querySelector('#fold-reset');
  const note = guide.querySelector('#fold-motion-note');
  const status = guide.querySelector('#direction-status');
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

  cards.forEach((card, i) => {
    const option = document.createElement('option');
    option.value = i;
    option.textContent = `Step ${i + 1} of ${cards.length}`;
    picker.append(option);
  });

  function stop() {
    cancelAnimationFrame(frame);
    frame = 0;
    running = false;
    lastTime = null;
  }

  function restore() {
    stop();
    elapsed = 0;
    cards[index].querySelector('svg').replaceWith(originals[index].cloneNode(true));
  }

  function updateControls() {
    previous.disabled = index === 0;
    next.disabled = index === cards.length - 1;
    picker.value = index;
    play.disabled = !animated.has(index) || motion.matches;
    reset.disabled = elapsed === 0;
    play.textContent = running ? 'Pause direction' : elapsed >= duration ? 'Replay direction' : elapsed > 0 ? 'Resume direction' : 'Show direction';
    note.textContent = motion.matches ? 'Reduced motion is on. Follow the static picture and instructions.'
      : !animated.has(index) ? 'A static checkpoint: compare your paper with this picture.'
      : index === 8 ? 'Top view: watch the slit open into a diamond, then a cross. Pause at any point.'
      : 'Motion starts only when you ask. Pause at any point.';
  }

  function showStep(newIndex, announce = true) {
    restore();
    index = Math.max(0, Math.min(cards.length - 1, newIndex));
    cards.forEach((card, i) => {
      // Reserve the tallest card at every text size without clipping instructions.
      // Inactive cards stay in the same grid area but are invisible and inaccessible.
      const inactive = !allSteps && i !== index;
      card.classList.toggle('fold-inactive', inactive);
      card.inert = inactive;
      if (inactive) card.setAttribute('aria-hidden', 'true');
      else card.removeAttribute('aria-hidden');
    });
    updateControls();
    if (announce) status.textContent = `Step ${index + 1} of ${cards.length}. ${cards[index].querySelector('h3').textContent.replace(/^\d+/, '')}. ${[...cards[index].querySelectorAll('p span')].map(line => line.textContent).join(' ')}`;
  }

  function polygon(points, className = 'sheet') {
    return `<polygon class="${className}" points="${points.map(point => point.join(',')).join(' ')}"/>`;
  }

  function draw(progress) {
    const svg = cards[index].querySelector('svg');
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
    svg.innerHTML = drawing;
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

  play.addEventListener('click', () => {
    if (motion.matches || !animated.has(index)) return;
    if (running) stop();
    else {
      if (elapsed >= duration) elapsed = 0;
      running = true;
      lastTime = null;
      frame = requestAnimationFrame(tick);
    }
    updateControls();
  });
  reset.addEventListener('click', () => { restore(); updateControls(); });
  previous.addEventListener('click', () => showStep(index - 1));
  next.addEventListener('click', () => showStep(index + 1));
  picker.addEventListener('change', () => showStep(Number(picker.value)));
  view.addEventListener('click', () => {
    allSteps = !allSteps;
    guide.classList.toggle('fold-focused', !allSteps);
    guide.querySelector('.fold-playback').hidden = allSteps;
    guide.querySelector('.fold-navigation').hidden = allSteps;
    view.setAttribute('aria-pressed', String(allSteps));
    view.textContent = allSteps ? 'One step at a time' : 'All steps';
    showStep(index, false);
    status.textContent = allSteps ? 'All ten steps shown in order.' : `Step ${index + 1} of ${cards.length}.`;
  });
  motion.addEventListener('change', () => { restore(); updateControls(); });
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) { stop(); updateControls(); }
  });
  // Without JS the complete ordered paper guide remains readable.
  guide.classList.add('fold-focused');
  guide.querySelector('.fold-controls').hidden = false;
  guide.querySelector('.fold-playback').hidden = false;
  showStep(0, false);
})();
