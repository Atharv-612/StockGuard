/**
 * StockGuard Master Application Router & State Manager
 */
const App = {
  currentView: 'dashboard',

  init() {
    this.setupNavigation();
    this.setupModals();
    this.setupSettings();

    // Check query params for initial route
    const params = new URLSearchParams(window.location.search);
    const initialView = params.get('view') || 'dashboard';

    // Check auth
    if (!Auth.isAuthenticated()) {
      if (initialView === 'register') {
        this.switchView('register');
      } else {
        this.switchView('login');
      }
    } else {
      this.populateUserHeader();
      this.switchView(initialView === 'login' || initialView === 'register' ? 'dashboard' : initialView);
    }
  },

  populateUserHeader() {
    const user = Auth.getUser();
    if (!user) return;

    const nameEls = document.querySelectorAll('.user-name-display');
    nameEls.forEach(el => el.textContent = user.fullName || user.email);

    const storeEls = document.querySelectorAll('.user-shop-display');
    storeEls.forEach(el => el.textContent = user.shopName || 'Retail Store');

    const avatarEls = document.querySelectorAll('.user-avatar-initial');
    avatarEls.forEach(el => {
      const name = user.fullName || user.email || 'S';
      el.textContent = name.charAt(0).toUpperCase();
    });
  },

  setupNavigation() {
    const navLinks = document.querySelectorAll('[data-view-target]');
    navLinks.forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const targetView = link.getAttribute('data-view-target');
        this.switchView(targetView);

        // Close mobile sidebar if open
        const sidebar = document.querySelector('.app-sidebar');
        if (sidebar) sidebar.classList.remove('mobile-open');
      });
    });

    // Mobile sidebar toggle button
    const toggleBtn = document.getElementById('sidebar-toggle-btn');
    if (toggleBtn) {
      toggleBtn.addEventListener('click', () => {
        const sidebar = document.querySelector('.app-sidebar');
        if (sidebar) sidebar.classList.toggle('mobile-open');
      });
    }

    // Logout button
    const logoutBtns = document.querySelectorAll('.btn-logout-trigger');
    logoutBtns.forEach(btn => {
      btn.addEventListener('click', () => Auth.logout());
    });

    // Login Form Submit
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
      loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        try {
          await Auth.login(email, password);
          this.toast('Welcome back! Logged in successfully.', 'success');
          this.populateUserHeader();
          this.switchView('dashboard');
        } catch (err) {
          this.toast(err.message, 'danger');
        }
      });
    }

    // Register Form Submit
    const registerForm = document.getElementById('register-form');
    if (registerForm) {
      registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const shopName = document.getElementById('reg-shop-name').value;
        const ownerName = document.getElementById('reg-owner-name').value;
        const email = document.getElementById('reg-email').value;
        const phone = document.getElementById('reg-phone').value;
        const password = document.getElementById('reg-password').value;

        try {
          await Auth.register(shopName, ownerName, email, phone, password);
          this.toast('Shop registered! Welcome to StockGuard.', 'success');
          this.populateUserHeader();
          this.switchView('dashboard');
        } catch (err) {
          this.toast(err.message, 'danger');
        }
      });
    }
  },

  switchView(viewName) {
    this.currentView = viewName;

    // Handle authentication views vs app views
    const authWrapper = document.getElementById('auth-views-wrapper');
    const appWrapper = document.getElementById('app-views-wrapper');

    if (viewName === 'login' || viewName === 'register') {
      if (authWrapper) authWrapper.style.display = 'flex';
      if (appWrapper) appWrapper.style.display = 'none';

      document.getElementById('view-login').style.display = viewName === 'login' ? 'block' : 'none';
      document.getElementById('view-register').style.display = viewName === 'register' ? 'block' : 'none';
      return;
    }

    // Regular authenticated view
    if (!Auth.isAuthenticated()) {
      this.switchView('login');
      return;
    }

    if (authWrapper) authWrapper.style.display = 'none';
    if (appWrapper) appWrapper.style.display = 'flex';

    // Hide all view sections
    const sections = document.querySelectorAll('.view-section');
    sections.forEach(s => s.classList.remove('active'));

    // Show target section
    const targetEl = document.getElementById(`view-${viewName}`);
    if (targetEl) {
      targetEl.classList.add('active');
    }

    // Update active nav link
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
      const link = item.querySelector('[data-view-target]');
      if (link && link.getAttribute('data-view-target') === viewName) {
        item.classList.add('active');
      } else {
        item.classList.remove('active');
      }
    });

    // Update Topbar Title
    const titleEl = document.getElementById('topbar-page-title');
    if (titleEl) {
      const titles = {
        'dashboard': 'Dashboard Overview',
        'inventory': 'Inventory Management',
        'expiry': 'Multi-Batch Expiry Tracking',
        'sales': 'Sales Management & POS',
        'reports': 'Reports & Business Analytics',
        'notifications': 'WhatsApp Cloud Integration',
        'settings': 'Shop Profile & Preferences',
      };
      titleEl.textContent = titles[viewName] || 'Dashboard';
    }

    // Trigger module initializers
    if (viewName === 'dashboard') Dashboard.init();
    if (viewName === 'inventory') Inventory.init();
    if (viewName === 'expiry') Expiry.init();
    if (viewName === 'sales') Sales.init();
    if (viewName === 'reports') Reports.init();
    if (viewName === 'notifications') Notifications.init();
    if (viewName === 'settings') this.loadShopSettings();

    // Update URL query state without reload
    window.history.replaceState({}, '', `/app.html?view=${viewName}`);
  },

  setupModals() {
    // Backdrop click close
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
      overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
          overlay.classList.remove('active');
        }
      });
    });

    // Close buttons
    document.querySelectorAll('.modal-close, [data-modal-close]').forEach(btn => {
      btn.addEventListener('click', () => {
        const modal = btn.closest('.modal-overlay');
        if (modal) modal.classList.remove('active');
      });
    });

    // Escape key
    window.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay.active').forEach(m => m.classList.remove('active'));
      }
    });
  },

  openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('active');
  },

  closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('active');
  },

  toast(message, type = 'info') {
    let container = document.querySelector('.toast-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon = 'fa-circle-info';
    if (type === 'success') icon = 'fa-circle-check';
    if (type === 'danger') icon = 'fa-circle-exclamation';
    if (type === 'warning') icon = 'fa-triangle-exclamation';

    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(100%)';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  },

  async loadShopSettings() {
    try {
      const shop = await API.get('/settings/shop');
      document.getElementById('settings-shop-name').value = shop.name || '';
      document.getElementById('settings-owner-name').value = shop.ownerName || '';
      document.getElementById('settings-phone').value = shop.phone || '';
      document.getElementById('settings-email').value = shop.email || '';
      document.getElementById('settings-address').value = shop.address || '';
      document.getElementById('settings-currency').value = shop.currency || 'INR';
      document.getElementById('settings-min-stock').value = shop.minStockDefault || 10;
      document.getElementById('settings-warning-days').value = shop.expiryWarningDays || 7;
    } catch (err) {
      console.error('Failed to load shop settings:', err);
    }

    // Load Database Health & Status
    try {
      const dbInfo = await API.get('/settings/db-status');
      const dbEl = document.getElementById('db-status-badge');
      const dbDetails = document.getElementById('db-status-details');
      if (dbEl && dbInfo) {
        dbEl.className = dbInfo.connected ? 'badge badge-safe' : 'badge badge-danger';
        dbEl.innerHTML = dbInfo.connected
          ? `<i class="fa-solid fa-circle-check"></i> ${dbInfo.engineName || 'Database Active'}`
          : '<i class="fa-solid fa-circle-xmark"></i> Database Disconnected';
      }
      if (dbDetails && dbInfo) {
        dbDetails.textContent = dbInfo.message || '';
      }
    } catch (e) {
      console.error('Failed to load DB status:', e);
    }
  },

  setupSettings() {
    // Quick Demo Credentials Fill Button
    const fillDemoBtn = document.getElementById('btn-fill-demo');
    if (fillDemoBtn) {
      fillDemoBtn.addEventListener('click', (e) => {
        e.preventDefault();
        const emailInput = document.getElementById('login-email');
        const passInput = document.getElementById('login-password');
        if (emailInput && passInput) {
          emailInput.value = 'demo@stockguard.com';
          passInput.value = 'demo123';
          this.toast('Demo credentials populated!', 'info');
        }
      });
    }

    const form = document.getElementById('shop-settings-form');
    if (form) {
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
          name: document.getElementById('settings-shop-name').value,
          ownerName: document.getElementById('settings-owner-name').value,
          phone: document.getElementById('settings-phone').value,
          email: document.getElementById('settings-email').value,
          address: document.getElementById('settings-address').value,
          currency: document.getElementById('settings-currency').value,
          minStockDefault: parseInt(document.getElementById('settings-min-stock').value),
          expiryWarningDays: parseInt(document.getElementById('settings-warning-days').value),
        };

        try {
          const updated = await API.put('/settings/shop', payload);
          Auth.updateShopName(updated.name);
          this.populateUserHeader();
          this.toast('Shop settings saved successfully!', 'success');
        } catch (err) {
          this.toast(err.message, 'danger');
        }
      });
    }
  }
};

document.addEventListener('DOMContentLoaded', () => App.init());
window.App = App;
