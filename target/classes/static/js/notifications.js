/**
 * StockGuard WhatsApp Business Platform Cloud API Integration Controller
 */
const Notifications = {
  preferences: null,

  async init() {
    this.setupListeners();
    await Promise.all([
      this.loadPreferences(),
      this.loadLogs(),
    ]);
  },

  async loadPreferences() {
    try {
      this.preferences = await API.get('/notifications/preferences');
      document.getElementById('pref-phone-input').value = this.preferences.whatsappPhone || '';
      document.getElementById('pref-warning-days').value = this.preferences.expiryWarningDays || 7;
      document.getElementById('pref-low-stock-toggle').checked = !!this.preferences.enableLowStock;
      document.getElementById('pref-expiry-toggle').checked = !!this.preferences.enableExpiry;
      document.getElementById('pref-daily-sales-toggle').checked = !!this.preferences.enableDailySales;

      // Meta Cloud Credentials
      const tokenInput = document.getElementById('meta-token-input');
      if (tokenInput) tokenInput.value = this.preferences.metaApiToken || '';

      const phoneIdInput = document.getElementById('meta-phone-id-input');
      if (phoneIdInput) phoneIdInput.value = this.preferences.phoneNumberId || '';

      const acctIdInput = document.getElementById('meta-account-id-input');
      if (acctIdInput) acctIdInput.value = this.preferences.businessAccountId || '';

      const liveToggle = document.getElementById('live-api-toggle');
      if (liveToggle) liveToggle.checked = !!this.preferences.useLiveApi;

      this.updateModeBadge(!!this.preferences.useLiveApi);

      // Also prefill test recipient
      document.getElementById('test-whatsapp-phone').value = this.preferences.whatsappPhone || '';
    } catch (err) {
      console.error('Failed to load notification preferences:', err);
    }
  },

  updateModeBadge(isLive) {
    const badge = document.getElementById('badge-whatsapp-mode');
    if (badge) {
      if (isLive) {
        badge.className = 'badge badge-safe';
        badge.innerHTML = '<i class="fa-solid fa-cloud"></i> Live Meta Cloud API';
      } else {
        badge.className = 'badge badge-warning';
        badge.innerHTML = '<i class="fa-solid fa-flask"></i> Local Dev Mock Mode';
      }
    }
  },

  async savePreferences() {
    const liveToggle = document.getElementById('live-api-toggle');
    const isLive = liveToggle ? liveToggle.checked : false;

    const payload = {
      whatsappPhone: document.getElementById('pref-phone-input').value,
      expiryWarningDays: parseInt(document.getElementById('pref-warning-days').value) || 7,
      enableLowStock: document.getElementById('pref-low-stock-toggle').checked,
      enableExpiry: document.getElementById('pref-expiry-toggle').checked,
      enableDailySales: document.getElementById('pref-daily-sales-toggle').checked,
      metaApiToken: document.getElementById('meta-token-input') ? document.getElementById('meta-token-input').value.trim() : null,
      phoneNumberId: document.getElementById('meta-phone-id-input') ? document.getElementById('meta-phone-id-input').value.trim() : null,
      businessAccountId: document.getElementById('meta-account-id-input') ? document.getElementById('meta-account-id-input').value.trim() : null,
      useLiveApi: isLive,
    };

    try {
      this.preferences = await API.put('/notifications/preferences', payload);
      this.updateModeBadge(isLive);
      App.toast(isLive ? 'Live WhatsApp Cloud API credentials updated & active!' : 'WhatsApp preferences saved in Dev Mock Mode!', 'success');
      // Refresh warning period across app
      Dashboard.init();
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  async loadLogs() {
    try {
      const logs = await API.get('/notifications/logs');
      const tbody = document.getElementById('notification-logs-tbody');
      const simConsole = document.getElementById('whatsapp-sim-console');

      if (tbody) {
        if (!logs || logs.length === 0) {
          tbody.innerHTML = `<tr><td colspan="5" class="table-empty-state"><i class="fa-brands fa-whatsapp"></i><p>No WhatsApp notifications sent yet</p></td></tr>`;
        } else {
          tbody.innerHTML = logs.map(l => {
            let statusBadge = '<span class="badge badge-safe">SENT</span>';
            if (l.status === 'SIMULATED') {
              statusBadge = '<span class="badge badge-warning"><i class="fa-solid fa-flask"></i> DEV SIMULATED</span>';
            } else if (l.status === 'FAILED') {
              statusBadge = '<span class="badge badge-danger">FAILED</span>';
            }

            const dateStr = new Date(l.createdAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });

            return `
              <tr>
                <td><strong>${l.templateName}</strong></td>
                <td><code>${l.recipientPhone}</code></td>
                <td>${statusBadge}</td>
                <td style="font-size:0.8rem; color:#64748B; max-width:260px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${l.providerResponse || ''}">${l.providerResponse || ''}</td>
                <td style="font-size:0.775rem; color:#94A3B8;">${dateStr}</td>
              </tr>
            `;
          }).join('');
        }
      }

      // Update simulation message preview bubble
      if (simConsole && logs.length > 0) {
        const latest = logs[0];
        simConsole.innerHTML = `
          <div style="font-size:0.75rem; color:#8696A0; margin-bottom:0.5rem;">
            [${latest.status}] Target: ${latest.recipientPhone} • Template: ${latest.templateName}
          </div>
          <div class="sim-message-bubble">${latest.messageContent}</div>
          <div style="font-size:0.7rem; color:#8696A0; text-align:right;">
            Gateway Response: ${latest.providerResponse}
          </div>
        `;
      }
    } catch (err) {
      console.error('Failed to load notification logs:', err);
    }
  },

  async sendTestNotification() {
    const template = document.getElementById('test-template-select').value;
    const phone = document.getElementById('test-whatsapp-phone').value;
    const customText = document.getElementById('test-custom-message').value;

    if (!phone) {
      App.toast('Please enter a destination phone number', 'warning');
      return;
    }

    try {
      const resp = await API.post('/notifications/send', {
        templateName: template,
        recipientPhone: phone,
        customMessage: customText,
      });

      if (resp.status === 'SIMULATED') {
        App.toast('WhatsApp message simulated locally (Dev Mock Mode). See console log below.', 'warning');
      } else if (resp.status === 'SENT') {
        App.toast('WhatsApp message successfully dispatched via Meta Cloud API!', 'success');
      } else {
        App.toast('WhatsApp dispatch failed. Check API credentials.', 'danger');
      }

      await this.loadLogs();
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  setupListeners() {
    const savePrefBtn = document.getElementById('btn-save-notification-pref');
    if (savePrefBtn) {
      savePrefBtn.addEventListener('click', () => this.savePreferences());
    }

    const testSendBtn = document.getElementById('btn-send-test-whatsapp');
    if (testSendBtn) {
      testSendBtn.addEventListener('click', () => this.sendTestNotification());
    }
  }
};

window.Notifications = Notifications;
