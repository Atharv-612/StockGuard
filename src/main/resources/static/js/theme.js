/**
 * StockGuard Global Theme Manager (Light & Black / Dark Mode)
 * Provides seamless theme toggling, persistence, and instant visual synchronization across the app.
 */
(function() {
  const THEME_KEY = 'stockguard_theme';
  const NEO_KEY = 'neo_theme';

  function getPreferredTheme() {
    const saved = localStorage.getItem(THEME_KEY) || localStorage.getItem(NEO_KEY);
    if (saved) return saved;
    return 'light';
  }

  function applyTheme(theme) {
    if (theme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
    } else {
      document.documentElement.removeAttribute('data-theme');
    }
    updateAllButtons(theme);
  }

  function updateAllButtons(theme) {
    const isDark = theme === 'dark';
    const buttons = document.querySelectorAll('#theme-toggle-btn, #landing-theme-toggle, .theme-toggle-nav-btn');
    buttons.forEach(btn => {
      const icon = btn.querySelector('i');
      const text = btn.querySelector('.theme-toggle-label') || btn.querySelector('span');

      if (icon) {
        icon.className = isDark ? 'fa-solid fa-sun' : 'fa-solid fa-moon';
      }
      if (text) {
        text.textContent = isDark ? 'Light' : 'Black';
      }
      btn.setAttribute('aria-label', isDark ? 'Switch to Light Theme' : 'Switch to Black Theme');
      btn.setAttribute('title', isDark ? 'Switch to Light Theme' : 'Switch to Black Theme');
    });
  }

  window.StockGuardTheme = {
    getTheme() {
      return document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
    },

    setTheme(theme) {
      localStorage.setItem(THEME_KEY, theme);
      localStorage.setItem(NEO_KEY, theme);
      applyTheme(theme);
      window.dispatchEvent(new CustomEvent('themechange', { detail: { theme } }));
    },

    toggle() {
      const nextTheme = this.getTheme() === 'dark' ? 'light' : 'dark';
      this.setTheme(nextTheme);
      return nextTheme;
    },

    init() {
      const initialTheme = getPreferredTheme();
      applyTheme(initialTheme);

      // Bind all theme toggle clicks dynamically
      document.addEventListener('click', (e) => {
        const btn = e.target.closest('#theme-toggle-btn, #landing-theme-toggle, .theme-toggle-nav-btn');
        if (btn) {
          e.preventDefault();
          StockGuardTheme.toggle();
        }
      });

      // Synchronize across multiple open browser tabs
      window.addEventListener('storage', (e) => {
        if (e.key === THEME_KEY || e.key === NEO_KEY) {
          if (e.newValue) applyTheme(e.newValue);
        }
      });
    }
  };

  // Immediate execution on DOM readiness
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => StockGuardTheme.init());
  } else {
    StockGuardTheme.init();
  }
})();
