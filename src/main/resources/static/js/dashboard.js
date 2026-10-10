/**
 * StockGuard Main Dashboard Controller
 */
const Dashboard = {
  salesChartInstance: null,
  categoryChartInstance: null,

  async init() {
    try {
      await Promise.all([
        this.loadMetrics(),
        this.loadAlerts(),
        this.loadCharts(),
        this.loadRecentMovements(),
      ]);
    } catch (err) {
      console.error('Failed to initialize dashboard:', err);
      App.toast(err.message, 'danger');
    }
  },

  async loadMetrics() {
    const data = await API.get('/reports/dashboard');
    const currency = data.currency === 'INR' ? '₹' : data.currency || '₹';

    document.getElementById('metric-total-products').textContent = data.totalProducts;
    document.getElementById('metric-total-stock').textContent = data.totalStockQuantity.toLocaleString();
    document.getElementById('metric-low-stock').textContent = data.lowStockCount;
    document.getElementById('metric-expiring-soon').textContent = data.expiringSoonCount;
    document.getElementById('metric-expired').textContent = data.expiredCount;

    document.getElementById('metric-today-sales').textContent = `${currency}${Number(data.todaySales || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
    document.getElementById('metric-monthly-sales').textContent = `${currency}${Number(data.monthlySales || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;

    // Update sidebar alert badge
    const alertCount = data.lowStockCount + data.expiringSoonCount + data.expiredCount;
    const badge = document.getElementById('sidebar-alert-badge');
    if (badge) {
      badge.textContent = alertCount > 0 ? alertCount : '';
      badge.style.display = alertCount > 0 ? 'inline-block' : 'none';
    }
  },

  async loadAlerts() {
    const data = await API.get('/alerts');
    const banner = document.getElementById('dashboard-alert-banner');
    if (!banner) return;

    if (data.expiredCount > 0 || data.expiringSoonCount > 0) {
      banner.style.display = 'flex';
      let msg = '';
      if (data.expiredCount > 0) {
        msg += `⚠️ <strong>${data.expiredCount} product batches have EXPIRED!</strong> Quarantine and discard them immediately. `;
      }
      if (data.expiringSoonCount > 0) {
        msg += `⏳ <strong>${data.expiringSoonCount} batches are nearing expiry</strong> within your shop's warning window.`;
      }
      document.getElementById('alert-banner-text-content').innerHTML = msg;
    } else {
      banner.style.display = 'none';
    }
  },

  async loadCharts() {
    const monthlyData = await API.get('/reports/monthly-sales');

    // Monthly Sales Chart (Bar + Line)
    const ctx = document.getElementById('monthlySalesChart');
    if (ctx && typeof Chart !== 'undefined') {
      if (this.salesChartInstance) {
        this.salesChartInstance.destroy();
      }

      const labels = monthlyData.map(d => d.label);
      const revenues = monthlyData.map(d => d.revenue);
      const orders = monthlyData.map(d => d.orderCount);

      this.salesChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: labels,
          datasets: [
            {
              type: 'line',
              label: 'Orders Count',
              data: orders,
              borderColor: '#28D978',
              backgroundColor: 'rgba(40, 217, 120, 0.2)',
              borderWidth: 3,
              tension: 0.35,
              yAxisID: 'y1',
              pointRadius: 4,
            },
            {
              type: 'bar',
              label: 'Revenue (₹)',
              data: revenues,
              backgroundColor: 'rgba(22, 119, 255, 0.85)',
              borderRadius: 6,
              yAxisID: 'y',
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'top',
              labels: { font: { family: 'Plus Jakarta Sans', weight: '600' } },
            },
            tooltip: {
              backgroundColor: '#0B1530',
              padding: 12,
              cornerRadius: 8,
            },
          },
          scales: {
            y: {
              beginAtZero: true,
              grid: { color: '#F1F5F9' },
              ticks: { callback: (val) => '₹' + val },
            },
            y1: {
              position: 'right',
              beginAtZero: true,
              grid: { display: false },
              ticks: { precision: 0 },
            },
            x: {
              grid: { display: false },
            },
          },
        },
      });
    }

    // Category Distribution Donut Chart
    const donutCtx = document.getElementById('categoryDonutChart');
    if (donutCtx && typeof Chart !== 'undefined') {
      if (this.categoryChartInstance) {
        this.categoryChartInstance.destroy();
      }

      // Fetch categories & products to compute counts
      const products = await API.get('/products');
      const catCountMap = {};
      products.forEach(p => {
        const cat = p.categoryName || 'Uncategorized';
        catCountMap[cat] = (catCountMap[cat] || 0) + (p.currentStock || 0);
      });

      const catLabels = Object.keys(catCountMap);
      const catValues = Object.values(catCountMap);

      this.categoryChartInstance = new Chart(donutCtx, {
        type: 'doughnut',
        data: {
          labels: catLabels.length ? catLabels : ['Stock'],
          datasets: [{
            data: catValues.length ? catValues : [1],
            backgroundColor: [
              '#1677FF', '#28D978', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#06B6D4'
            ],
            borderWidth: 2,
            borderColor: '#fff',
          }],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'bottom',
              labels: { boxWidth: 12, font: { family: 'Plus Jakarta Sans', size: 11 } },
            },
          },
          cutout: '70%',
        },
      });
    }
  },

  async loadRecentMovements() {
    const movements = await API.get('/inventory/movements');
    const tbody = document.getElementById('dashboard-movements-tbody');
    if (!tbody) return;

    if (!movements || movements.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="table-empty-state"><i class="fa-solid fa-arrow-right-arrow-left"></i><p>No recent stock movements recorded yet</p></td></tr>`;
      return;
    }

    tbody.innerHTML = movements.slice(0, 7).map(m => {
      let badgeClass = 'badge-primary';
      let icon = 'fa-boxes-stacked';
      if (m.movementType === 'PURCHASE') {
        badgeClass = 'badge-safe';
        icon = 'fa-plus';
      } else if (m.movementType === 'SALE') {
        badgeClass = 'badge-primary';
        icon = 'fa-cart-shopping';
      } else if (m.movementType === 'EXPIRED_SCRAP') {
        badgeClass = 'badge-danger';
        icon = 'fa-trash-can';
      } else if (m.movementType === 'ADJUSTMENT') {
        badgeClass = 'badge-warning';
        icon = 'fa-sliders';
      }

      const dateStr = new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ', ' +
                      new Date(m.createdAt).toLocaleDateString([], { month: 'short', day: 'numeric' });

      return `
        <tr>
          <td><strong>${m.productName}</strong></td>
          <td><code>${m.batchNumber || 'N/A'}</code></td>
          <td><span class="badge ${badgeClass}"><i class="fa-solid ${icon}"></i> ${m.movementType}</span></td>
          <td style="font-weight:700; color:${m.quantityChanged > 0 ? '#10B981' : '#EF4444'}">${m.quantityChanged > 0 ? '+' : ''}${m.quantityChanged}</td>
          <td><strong>${m.quantityAfter}</strong></td>
          <td style="color:#64748B; font-size:0.8rem;">${dateStr}</td>
        </tr>
      `;
    }).join('');
  },
};

window.Dashboard = Dashboard;
