/**
 * StockGuard Inventory Management Controller
 */
const Inventory = {
  products: [],
  categories: [],

  async init() {
    await this.loadCategories();
    await this.loadProducts();
    this.setupListeners();
  },

  async loadCategories() {
    try {
      this.categories = await API.get('/categories');
      const selectFilter = document.getElementById('inventory-category-filter');
      const selectModal = document.getElementById('product-category-input');

      if (selectFilter) {
        selectFilter.innerHTML = '<option value="">All Categories</option>' +
          this.categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
      }

      if (selectModal) {
        selectModal.innerHTML = '<option value="">Select Category</option>' +
          this.categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
      }
    } catch (err) {
      console.error('Failed to load categories:', err);
    }
  },

  async loadProducts(search = '', categoryId = '') {
    try {
      let endpoint = '/products';
      const params = new URLSearchParams();
      if (search) params.append('search', search);
      if (categoryId) params.append('categoryId', categoryId);
      if (params.toString()) endpoint += '?' + params.toString();

      this.products = await API.get(endpoint);
      this.renderTable();
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  renderTable() {
    const tbody = document.getElementById('inventory-table-tbody');
    if (!tbody) return;

    if (!this.products || this.products.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" class="table-empty-state"><i class="fa-solid fa-box-open"></i><p>No products match your search or filter</p></td></tr>`;
      return;
    }

    tbody.innerHTML = this.products.map(p => {
      let stockBadge = `<span class="badge badge-safe"><i class="fa-solid fa-check"></i> In Stock (${p.currentStock})</span>`;
      if (p.currentStock <= 0) {
        stockBadge = `<span class="badge badge-danger"><i class="fa-solid fa-triangle-exclamation"></i> Out of Stock</span>`;
      } else if (p.isLowStock) {
        stockBadge = `<span class="badge badge-warning"><i class="fa-solid fa-triangle-exclamation"></i> Low Stock (${p.currentStock})</span>`;
      }

      return `
        <tr>
          <td>
            <div style="font-weight:700; color:var(--color-navy);">${p.name}</div>
            <div style="font-size:0.775rem; color:var(--text-muted);">${p.description || ''}</div>
          </td>
          <td><span class="badge badge-primary">${p.categoryName || 'General'}</span></td>
          <td><code>${p.sku || '-'}</code></td>
          <td>₹${Number(p.costPrice).toFixed(2)}</td>
          <td><strong>₹${Number(p.sellingPrice).toFixed(2)}</strong></td>
          <td>${stockBadge}</td>
          <td>${p.minStockThreshold} ${p.unit}</td>
          <td>
            <div style="display:flex; gap:0.4rem;">
              <button class="btn btn-outline btn-sm" title="Quick Add Batch" onclick="Inventory.openAddBatchModal(${p.id}, '${p.name.replace(/'/g, "\\'")}')">
                <i class="fa-solid fa-plus"></i> Batch
              </button>
              <button class="btn btn-outline btn-sm" title="Adjust Stock" onclick="Inventory.openAdjustStockModal(${p.id}, '${p.name.replace(/'/g, "\\'")}', ${p.currentStock})">
                <i class="fa-solid fa-sliders"></i>
              </button>
              <button class="btn btn-outline btn-sm" title="Edit" onclick="Inventory.openEditModal(${p.id})">
                <i class="fa-solid fa-pen"></i>
              </button>
              <button class="btn btn-outline btn-sm" title="Delete" style="color:#EF4444;" onclick="Inventory.deleteProduct(${p.id}, '${p.name.replace(/'/g, "\\'")}')">
                <i class="fa-solid fa-trash-can"></i>
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  },

  setupListeners() {
    const searchInput = document.getElementById('inventory-search-input');
    const catFilter = document.getElementById('inventory-category-filter');

    if (searchInput) {
      let debounceTimer;
      searchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
          this.loadProducts(e.target.value, catFilter ? catFilter.value : '');
        }, 300);
      });
    }

    if (catFilter) {
      catFilter.addEventListener('change', (e) => {
        this.loadProducts(searchInput ? searchInput.value : '', e.target.value);
      });
    }

    // Product Form Submit
    const productForm = document.getElementById('product-form');
    if (productForm) {
      productForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        await this.handleSaveProduct();
      });
    }

    // Stock Adjust Form Submit
    const adjustForm = document.getElementById('adjust-stock-form');
    if (adjustForm) {
      adjustForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        await this.handleSaveAdjustment();
      });
    }
  },

  openAddModal() {
    document.getElementById('product-modal-title').textContent = 'Add New Product';
    document.getElementById('product-id-input').value = '';
    document.getElementById('product-name-input').value = '';
    document.getElementById('product-sku-input').value = '';
    document.getElementById('product-barcode-input').value = '';
    document.getElementById('product-cost-input').value = '';
    document.getElementById('product-selling-input').value = '';
    document.getElementById('product-min-stock-input').value = '10';
    document.getElementById('product-unit-input').value = 'pcs';
    document.getElementById('product-desc-input').value = '';
    if (this.categories.length > 0) {
      document.getElementById('product-category-input').value = this.categories[0].id;
    }
    App.openModal('product-modal');
  },

  async openEditModal(productId) {
    try {
      const p = await API.get(`/products/${productId}`);
      document.getElementById('product-modal-title').textContent = 'Edit Product Details';
      document.getElementById('product-id-input').value = p.id;
      document.getElementById('product-name-input').value = p.name;
      document.getElementById('product-category-input').value = p.categoryId || '';
      document.getElementById('product-sku-input').value = p.sku || '';
      document.getElementById('product-barcode-input').value = p.barcode || '';
      document.getElementById('product-cost-input').value = p.costPrice;
      document.getElementById('product-selling-input').value = p.sellingPrice;
      document.getElementById('product-min-stock-input').value = p.minStockThreshold;
      document.getElementById('product-unit-input').value = p.unit || 'pcs';
      document.getElementById('product-desc-input').value = p.description || '';
      App.openModal('product-modal');
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  async handleSaveProduct() {
    const id = document.getElementById('product-id-input').value;
    const payload = {
      name: document.getElementById('product-name-input').value,
      categoryId: document.getElementById('product-category-input').value || null,
      sku: document.getElementById('product-sku-input').value || null,
      barcode: document.getElementById('product-barcode-input').value || null,
      costPrice: parseFloat(document.getElementById('product-cost-input').value),
      sellingPrice: parseFloat(document.getElementById('product-selling-input').value),
      minStockThreshold: parseInt(document.getElementById('product-min-stock-input').value) || 10,
      unit: document.getElementById('product-unit-input').value || 'pcs',
      description: document.getElementById('product-desc-input').value || '',
    };

    try {
      if (id) {
        await API.put(`/products/${id}`, payload);
        App.toast('Product updated successfully', 'success');
      } else {
        await API.post('/products', payload);
        App.toast('New product added to inventory', 'success');
      }
      App.closeModal('product-modal');
      await this.loadProducts();
      Dashboard.init();
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  async deleteProduct(id, name) {
    if (!confirm(`Are you sure you want to remove "${name}" from inventory?`)) return;
    try {
      await API.delete(`/products/${id}`);
      App.toast(`Product "${name}" deleted`, 'success');
      await this.loadProducts();
      Dashboard.init();
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  openAdjustStockModal(productId, productName, currentStock) {
    document.getElementById('adjust-prod-id').value = productId;
    document.getElementById('adjust-prod-name').textContent = productName;
    document.getElementById('adjust-current-stock').textContent = currentStock;
    document.getElementById('adjust-diff-input').value = '0';
    document.getElementById('adjust-reason-input').value = 'Physical inventory audit count';
    App.openModal('adjust-stock-modal');
  },

  async handleSaveAdjustment() {
    const prodId = document.getElementById('adjust-prod-id').value;
    const diff = parseInt(document.getElementById('adjust-diff-input').value);
    const reason = document.getElementById('adjust-reason-input').value;

    if (diff === 0) {
      App.toast('Adjustment difference cannot be 0', 'warning');
      return;
    }

    try {
      await API.post('/inventory/adjust', {
        productId: prodId,
        quantityDifference: diff,
        reason: reason,
      });
      App.toast('Stock adjusted successfully', 'success');
      App.closeModal('adjust-stock-modal');
      await this.loadProducts();
      Dashboard.init();
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  openAddBatchModal(productId, productName) {
    Expiry.openNewBatchModalWithProduct(productId, productName);
  }
};

window.Inventory = Inventory;
