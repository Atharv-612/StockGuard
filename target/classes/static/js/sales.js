/**
 * StockGuard Point-of-Sale (POS) & Sales Management Controller
 */
const Sales = {
  products: [],
  cart: [],
  allSalesHistory: [],

  async init() {
    this.setupListeners();
    await Promise.all([
      this.loadPosProducts(),
      this.loadSalesHistory(),
    ]);
  },

  async loadPosProducts(search = '') {
    try {
      let endpoint = '/products';
      if (search) endpoint += `?search=${encodeURIComponent(search)}`;
      this.products = await API.get(endpoint);
      this.renderPosGrid();
    } catch (err) {
      console.error('Failed to load POS products:', err);
    }
  },

  renderPosGrid() {
    const grid = document.getElementById('pos-products-grid');
    if (!grid) return;

    if (!this.products || this.products.length === 0) {
      grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:2rem;color:#64748B;">No products found</div>';
      return;
    }

    grid.innerHTML = this.products.map(p => {
      const inStock = p.currentStock > 0;
      const stockBadge = inStock
        ? `<span class="pos-stock-tag" style="background:rgba(40,217,120,0.15);color:#0E7B42;">Stock: ${p.currentStock}</span>`
        : `<span class="pos-stock-tag" style="background:rgba(239,68,68,0.15);color:#EF4444;">Out of stock</span>`;

      return `
        <div class="pos-product-item ${!inStock ? 'disabled-item' : ''}" style="${!inStock ? 'opacity:0.6;cursor:not-allowed;' : ''}" onclick="${inStock ? `Sales.addToCart(${p.id})` : ''}">
          <div>
            <div class="pos-product-title">${p.name}</div>
            <div style="font-size:0.75rem; color:#64748B;">${p.categoryName || 'General'}</div>
          </div>
          <div class="pos-product-bottom">
            <span class="pos-product-price">₹${Number(p.sellingPrice).toFixed(2)}</span>
            ${stockBadge}
          </div>
        </div>
      `;
    }).join('');
  },

  addToCart(productId) {
    const product = this.products.find(p => p.id === productId);
    if (!product || product.currentStock <= 0) {
      App.toast('Item is out of stock', 'warning');
      return;
    }

    const existingIndex = this.cart.findIndex(i => i.productId === productId);
    if (existingIndex > -1) {
      if (this.cart[existingIndex].quantity >= product.currentStock) {
        App.toast(`Cannot add more. In-stock ceiling reached (${product.currentStock} ${product.unit})`, 'warning');
        return;
      }
      this.cart[existingIndex].quantity += 1;
    } else {
      this.cart.push({
        productId: product.id,
        productName: product.name,
        unitPrice: product.sellingPrice,
        maxStock: product.currentStock,
        quantity: 1,
      });
    }

    this.renderCart();
  },

  updateCartQty(productId, delta) {
    const index = this.cart.findIndex(i => i.productId === productId);
    if (index === -1) return;

    const item = this.cart[index];
    const newQty = item.quantity + delta;

    if (newQty <= 0) {
      this.cart.splice(index, 1);
    } else if (newQty > item.maxStock) {
      App.toast(`Cannot exceed available inventory (${item.maxStock})`, 'warning');
      return;
    } else {
      item.quantity = newQty;
    }

    this.renderCart();
  },

  renderCart() {
    const list = document.getElementById('pos-bill-items-list');
    if (!list) return;

    if (this.cart.length === 0) {
      list.innerHTML = `
        <div style="text-align:center;padding:3rem 1rem;color:#94A3B8;">
          <i class="fa-solid fa-cart-shopping" style="font-size:2.5rem;margin-bottom:0.75rem;display:block;"></i>
          <p>Cart is empty. Click items on the left to add.</p>
        </div>
      `;
      this.updateTotals(0);
      return;
    }

    let subtotal = 0;
    list.innerHTML = this.cart.map(item => {
      const lineTotal = item.unitPrice * item.quantity;
      subtotal += lineTotal;

      return `
        <div class="bill-item">
          <div>
            <div class="bill-item-name" title="${item.productName}">${item.productName}</div>
            <div style="font-size:0.75rem; color:#64748B;">₹${Number(item.unitPrice).toFixed(2)} each</div>
          </div>
          <div class="bill-qty-controls">
            <button class="bill-qty-btn" onclick="Sales.updateCartQty(${item.productId}, -1)">-</button>
            <span style="font-weight:700; min-width:20px; text-align:center;">${item.quantity}</span>
            <button class="bill-qty-btn" onclick="Sales.updateCartQty(${item.productId}, 1)">+</button>
          </div>
          <div style="font-weight:700; color:var(--color-navy); min-width:60px; text-align:right;">
            ₹${lineTotal.toFixed(2)}
          </div>
        </div>
      `;
    }).join('');

    this.updateTotals(subtotal);
  },

  updateTotals(subtotal) {
    const discountInput = document.getElementById('pos-discount-input');
    const discount = parseFloat(discountInput ? discountInput.value : 0) || 0;
    const finalTotal = Math.max(0, subtotal - discount);

    document.getElementById('pos-bill-subtotal').textContent = `₹${subtotal.toFixed(2)}`;
    document.getElementById('pos-bill-total').textContent = `₹${finalTotal.toFixed(2)}`;
  },

  clearCart() {
    this.cart = [];
    this.renderCart();
  },

  async checkout() {
    if (this.cart.length === 0) {
      App.toast('Cart is empty. Add products before checkout.', 'warning');
      return;
    }

    const customerName = document.getElementById('pos-customer-name').value || 'Walk-in Customer';
    const customerPhone = document.getElementById('pos-customer-phone').value || '';
    const paymentMethod = document.getElementById('pos-payment-method').value || 'CASH';
    const discount = parseFloat(document.getElementById('pos-discount-input').value) || 0;

    const payload = {
      customerName,
      customerPhone,
      paymentMethod,
      discount,
      items: this.cart.map(i => ({
        productId: i.productId,
        quantity: i.quantity,
      })),
    };

    try {
      const response = await API.post('/sales', payload);
      App.toast(`Sale recorded! Invoice #${response.invoiceNumber}`, 'success');

      // Show receipt modal
      this.showReceiptModal(response);

      // Reset POS cart
      this.clearCart();
      document.getElementById('pos-customer-name').value = '';
      document.getElementById('pos-customer-phone').value = '';
      document.getElementById('pos-discount-input').value = '0';

      // Refresh stock in product catalog
      await this.loadPosProducts();
      await this.loadSalesHistory();
      Dashboard.init();
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  showReceiptModal(sale) {
    const user = Auth.getUser();
    document.getElementById('receipt-shop-name').textContent = user ? user.shopName : 'StockGuard Grocery';
    document.getElementById('receipt-invoice-num').textContent = sale.invoiceNumber;
    document.getElementById('receipt-date').textContent = new Date(sale.saleDate).toLocaleString();
    document.getElementById('receipt-customer').textContent = sale.customerName;
    document.getElementById('receipt-payment').textContent = sale.paymentMethod;

    const itemsContainer = document.getElementById('receipt-items-tbody');
    itemsContainer.innerHTML = sale.items.map(it => `
      <tr>
        <td style="padding:4px 0;">${it.productName}</td>
        <td style="text-align:center;">${it.quantity}</td>
        <td style="text-align:right;">₹${Number(it.unitPrice).toFixed(2)}</td>
        <td style="text-align:right; font-weight:700;">₹${Number(it.totalPrice).toFixed(2)}</td>
      </tr>
    `).join('');

    document.getElementById('receipt-subtotal').textContent = `₹${Number(sale.subtotal).toFixed(2)}`;
    document.getElementById('receipt-discount').textContent = `₹${Number(sale.discount).toFixed(2)}`;
    document.getElementById('receipt-grand-total').textContent = `₹${Number(sale.totalAmount).toFixed(2)}`;

    App.openModal('receipt-modal');
  },

  async loadSalesHistory() {
    try {
      this.allSalesHistory = await API.get('/sales');
      const tbody = document.getElementById('sales-history-tbody');
      if (!tbody) return;

      if (!this.allSalesHistory || this.allSalesHistory.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="table-empty-state"><i class="fa-solid fa-receipt"></i><p>No recorded sales transactions yet</p></td></tr>`;
        return;
      }

      tbody.innerHTML = this.allSalesHistory.map(s => {
        const dateStr = new Date(s.saleDate).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
        return `
          <tr>
            <td><strong>${s.invoiceNumber}</strong></td>
            <td>${dateStr}</td>
            <td>${s.customerName}</td>
            <td><span class="badge badge-primary">${s.paymentMethod}</span></td>
            <td><strong>₹${Number(s.totalAmount).toFixed(2)}</strong></td>
            <td>
              <button class="btn btn-outline btn-sm" onclick="Sales.viewSavedReceipt(${s.id})">
                <i class="fa-solid fa-receipt"></i> Receipt
              </button>
            </td>
          </tr>
        `;
      }).join('');
    } catch (err) {
      console.error('Failed to load sales history:', err);
    }
  },

  async viewSavedReceipt(saleId) {
    try {
      const sale = await API.get(`/sales/${saleId}`);
      this.showReceiptModal(sale);
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  setupListeners() {
    const searchInput = document.getElementById('pos-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.loadPosProducts(e.target.value);
      });
    }

    const discountInput = document.getElementById('pos-discount-input');
    if (discountInput) {
      discountInput.addEventListener('input', () => {
        const subtotal = this.cart.reduce((acc, i) => acc + (i.unitPrice * i.quantity), 0);
        this.updateTotals(subtotal);
      });
    }
  }
};

window.Sales = Sales;
