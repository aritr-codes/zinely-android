(() => {
  const demo = document.querySelector('[data-fold-demo]');
  if (!demo) return;

  const steps = [
    {
      className: 'step-fold',
      instruction: 'Fold the right half over the left half.',
      status: 'Fold shown: the right half moves over the left half.'
    },
    {
      className: 'step-cut',
      instruction: 'Cut along the middle crease, from the folded edge to the centre.',
      status: 'Cut shown: the centre slit runs from the folded edge to the middle.'
    },
    {
      className: 'step-pop',
      instruction: 'Push both ends toward the middle so the centre opens into a diamond.',
      status: 'Push shown: both ends move inward and the centre opens.'
    }
  ];

  const label = demo.querySelector('.fold-step');
  const instruction = demo.querySelector('.fold-instruction');
  const show = demo.querySelector('.fold-show');
  const next = demo.querySelector('.fold-next');
  const status = demo.querySelector('.fold-status');
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  let index = 0;
  let finishTimer;

  function resetClasses() {
    demo.classList.remove('step-fold', 'step-cut', 'step-pop', 'is-playing', 'is-shown');
  }

  function renderStep() {
    clearTimeout(finishTimer);
    resetClasses();
    const current = steps[index];
    demo.classList.add(current.className);
    label.textContent = `Example ${index + 1} of ${steps.length}`;
    instruction.textContent = current.instruction;
    show.textContent = 'Show this fold';
    status.textContent = '';
  }

  function showStep() {
    clearTimeout(finishTimer);
    demo.classList.remove('is-playing', 'is-shown');
    void demo.offsetWidth;
    const current = steps[index];
    show.textContent = 'Show again';

    if (reducedMotion.matches) {
      demo.classList.add('is-shown');
      status.textContent = `${current.status} Motion was reduced to match your device setting.`;
      return;
    }

    demo.classList.add('is-playing');
    finishTimer = setTimeout(() => {
      demo.classList.remove('is-playing');
      demo.classList.add('is-shown');
      status.textContent = current.status;
    }, 900);
  }

  show.addEventListener('click', showStep);
  next.addEventListener('click', () => {
    index = (index + 1) % steps.length;
    renderStep();
    status.textContent = `Example ${index + 1} of ${steps.length}. ${steps[index].instruction}`;
  });
  reducedMotion.addEventListener?.('change', renderStep);
  renderStep();
})();
