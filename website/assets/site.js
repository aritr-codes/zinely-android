(() => {
  const status = document.querySelector('#direction-status');
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  const buttons = document.querySelectorAll('.direction-button');
  const timers = new Map();

  function stopAnimations() {
    timers.forEach(clearTimeout);
    timers.clear();
    document.querySelectorAll('.tracing-direction').forEach(card => card.classList.remove('tracing-direction'));
  }

  buttons.forEach(button => {
    button.hidden = false;
    button.addEventListener('click', () => {
      stopAnimations();
      const card = button.closest('.fold-card');
      const number = card.querySelector('.fold-number').textContent;
      const instruction = card.querySelector('p').textContent;
      status.textContent = `Step ${number}. ${instruction}`;
      if (reducedMotion.matches) return;
      // Arrows remain visible at rest; replay only traces their direction.
      void card.offsetWidth;
      card.classList.add('tracing-direction');
      timers.set(card, setTimeout(() => {
        card.classList.remove('tracing-direction');
        timers.delete(card);
      }, 850));
    });
  });
  reducedMotion.addEventListener('change', stopAnimations);
})();
