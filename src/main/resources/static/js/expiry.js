/**
 * StockGuard Multi-Batch Expiry Tracking Controller
 */
const Expiry = {
  batches: [],
  currentFilter: 'all',

  async init() {
    this.setupListeners();
    await this.loadBatches();
  },

  async loadBatches(filter = this.currentFilter) {
    try {
      this.currentFilter = filter;
      let endpoint = '/batches';
      if (filter && filter !== 'all') {
        endpoint += `?filter=${filter}`;
      }

      this.batches = await API.get(endpoint);
      this.renderTable();
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  renderTable() {
    const tbody = document.getElementById('expiry-table-tbody');
    if (!tbody) return;

    let displayBatches = this.batches;
    if (this.currentFilter === 'safe') {
      displayBatches = this.batches.filter(b => b.expiryState === 'SAFE');
    }

    if (!displayBatches || displayBatches.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" class="table-empty-state"><i class="fa-solid fa-calendar-xmark"></i><p>No inventory batches match "${this.currentFilter}" filter</p></td></tr>`;
      return;
    }

    tbody.innerHTML = displayBatches.map(b => {
      let statusBadge = '';
      let actionBtn = '';

      if (b.expiryState === 'EXPIRED') {
        statusBadge = `<span class="badge badge-danger"><i class="fa-solid fa-skull-crossbones"></i> EXPIRED (${Math.abs(b.daysUntilExpiry)}d ago)</span>`;
        if (b.currentQuantity > 0) {
          actionBtn = `<button class="btn btn-danger btn-sm" onclick="Expiry.discardBatch(${b.id}, '${b.batchNumber}', ${b.currentQuantity})"><i class="fa-solid fa-trash-can"></i> Quarantine / Scrap</button>`;
        } else {
          actionBtn = `<span style="color:#64748B; font-size:0.8rem;">Discarded / 0 Qty</span>`;
        }
      } else if (b.expiryState === 'EXPIRING_SOON') {
        statusBadge = `<span class="badge badge-warning"><i class="fa-solid fa-clock"></i> Expiring Soon (${b.daysUntilExpiry} days left)</span>`;
        actionBtn = `<button class="btn btn-outline btn-sm" onclick="Expiry.quickWhatsAppAlert('${b.productName.replace(/'/g, "\\'")}', '${b.batchNumber}', ${b.daysUntilExpiry})"><i class="fa-brands fa-whatsapp" style="color:#28D978;"></i> Alert Owner</button>`;
      } else {
        statusBadge = `<span class="badge badge-safe"><i class="fa-solid fa-shield-halved"></i> Safe (${b.daysUntilExpiry} days)</span>`;
        actionBtn = `<span style="color:#10B981; font-size:0.8rem; font-weight:600;"><i class="fa-solid fa-check"></i> Safe to Sell</span>`;
      }

      return `
        <tr>
          <td>
            <div style="font-weight:700; color:var(--color-navy);">${b.productName}</div>
            <div style="font-size:0.775rem; color:var(--text-muted);">${b.categoryName || 'General'}</div>
          </td>
          <td><code style="font-weight:700;">${b.batchNumber}</code></td>
          <td><strong>${b.currentQuantity}</strong> / ${b.initialQuantity}</td>
          <td>₹${Number(b.costPrice).toFixed(2)}</td>
          <td><strong>₹${Number(b.sellingPrice).toFixed(2)}</strong></td>
          <td>${b.expiryDate}</td>
          <td>${statusBadge}</td>
          <td>${actionBtn}</td>
        </tr>
      `;
    }).join('');
  },

  setupListeners() {
    // Tab filters
    const filterTabs = document.querySelectorAll('.expiry-tab-btn');
    filterTabs.forEach(tab => {
      tab.addEventListener('click', (e) => {
        filterTabs.forEach(t => t.classList.remove('active'));
        e.currentTarget.classList.add('active');
        const filter = e.currentTarget.getAttribute('data-filter');
        this.loadBatches(filter);
      });
    });

    // Batch Intake Form
    const batchForm = document.getElementById('new-batch-form');
    if (batchForm) {
      batchForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        await this.handleSaveBatch();
      });
    }
  },

  async openNewBatchModal() {
    const products = await API.get('/products');
    const select = document.getElementById('batch-product-select');
    select.innerHTML = '<option value="">Select Target Product</option>' +
      products.map(p => `<option value="${p.id}" data-cost="${p.costPrice}" data-sell="${p.sellingPrice}">${p.name} (Stock: ${p.currentStock})</option>`).join('');

    select.addEventListener('change', (e) => {
      const opt = e.target.selectedOptions[0];
      if (opt) {
        document.getElementById('batch-cost-input').value = opt.getAttribute('data-cost') || '';
        document.getElementById('batch-selling-input').value = opt.getAttribute('data-sell') || '';
      }
    });

    document.getElementById('batch-number-input').value = 'BATCH-' + Math.floor(1000 + Math.random() * 9000);
    document.getElementById('batch-qty-input').value = '20';
    document.getElementById('batch-mfg-input').value = new Date().toISOString().split('T')[0];
    
    // Set expiry default 30 days ahead
    const nextMonth = new Date();
    nextMonth.setDate(nextMonth.getDate() + 30);
    document.getElementById('batch-exp-input').value = nextMonth.toISOString().split('T')[0];

    App.openModal('new-batch-modal');
  },

  async openNewBatchModalWithProduct(productId, productName) {
    await this.openNewBatchModal();
    const select = document.getElementById('batch-product-select');
    select.value = productId;
    select.dispatchEvent(new Event('change'));
  },

  async handleSaveBatch() {
    const payload = {
      productId: parseInt(document.getElementById('batch-product-select').value),
      batchNumber: document.getElementById('batch-number-input').value,
      quantity: parseInt(document.getElementById('batch-qty-input').value),
      costPrice: parseFloat(document.getElementById('batch-cost-input').value),
      sellingPrice: parseFloat(document.getElementById('batch-selling-input').value),
      mfgDate: document.getElementById('batch-mfg-input').value || null,
      expiryDate: document.getElementById('batch-exp-input').value,
    };

    if (!payload.productId) {
      App.toast('Please select a product for the batch', 'warning');
      return;
    }

    try {
      await API.post('/batches', payload);
      App.toast(`Batch ${payload.batchNumber} received successfully!`, 'success');
      App.closeModal('new-batch-modal');
      await this.loadBatches();
      Dashboard.init();
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  async discardBatch(batchId, batchNumber, qty) {
    if (!confirm(`Confirm discarding expired batch ${batchNumber}? This will deduct ${qty} units from inventory and record an audit scrap log.`)) {
      return;
    }

    try {
      await API.post(`/batches/${batchId}/discard`, { reason: 'Expired stock quarantined and discarded' });
      App.toast(`Expired batch ${batchNumber} quarantined & discarded`, 'success');
      await this.loadBatches();
      Dashboard.init();
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  async quickWhatsAppAlert(productName, batchNumber, daysLeft) {
    try {
      await API.post('/notifications/send', {
        templateName: 'EXPIRY_WARNING',
        customMessage: `Product "${productName}" (Batch: ${batchNumber}) will expire in ${daysLeft} days!`,
      });
      App.toast(`WhatsApp expiry warning triggered for ${productName}!`, 'success');
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  }
};

window.Expiry = Expiry;
