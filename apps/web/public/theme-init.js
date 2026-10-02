// Applies the remembered theme before the first paint to avoid a flash.
// Keep the key and values in sync with src/theme/theme.ts.
(function () {
  try {
    var theme = localStorage.getItem('ljbu.theme.v1');
    if (theme === 'light' || theme === 'dark') {
      document.documentElement.dataset.theme = theme;
    }
  } catch (e) {
    // Storage unavailable: follow the system preference.
  }
})();
