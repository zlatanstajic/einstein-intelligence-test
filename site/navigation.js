/* The anchor remains usable when JavaScript is unavailable. */
(() => {
  const backToTop = document.querySelector('.back-to-top');
  const updateVisibility = () => {
    backToTop.hidden = window.scrollY < 400;
  };
  window.addEventListener('scroll', updateVisibility, { passive: true });
  updateVisibility();
})();
