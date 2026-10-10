/**
 * StockGuard Reports & Analytics Controller
 */
const Reports = {
  topProductsChart: null,

  async init() {
    await Promise.all([
      this.loadTopProducts(),
      this.loadExpiryWastage(),
    ]);
    this.setupListeners();
  },

  async loadTopProducts() {
    try {
      const topProducts = await API.get('/reports/top-products');
      const tbody = document.getElementById('top-products-tbody');
      if (tbody) {
        if (!topProducts || topProducts.length === 0) {
          tbody.innerHTML = `<tr><td colspan="4" class="table-empty-state"><i class="fa-solid fa-trophy"></i><p>No sales recorded yet</p></td></tr>`;
        } else {
          tbody.innerHTML = topProducts.map((p, idx) => `
            <tr>
              <td><span style="font-weight:800; color:${idx === 0 ? '#F59E0B' : '#64748B'};">#${idx + 1}</span></td>
              <td><strong>${p.productName}</strong></td>
              <td>${p.unitsSold} units</td>
              <td><strong>₹${Number(p.totalRevenue).toFixed(2)}</strong></td>
            </tr>
          `).join('');
        }
      }

      // Top Products Bar Chart
      const ctx = document.getElementById('topProductsBarChart');
      if (ctx && typeof Chart !== 'undefined' && topProducts.length > 0) {
        if (this.topProductsChart) {
          this.topProductsChart.destroy();
        }

        const labels = topProducts.slice(0, 6).map(p => p.productName);
        const data = topProducts.slice(0, 6).map(p => p.unitsSold);

        this.topProductsChart = new Chart(ctx, {
          type: 'bar',
          data: {
            labels: labels,
            datasets: [{
              label: 'Units Sold',
              data: data,
              backgroundColor: '#1677FF',
              borderRadius: 6,
            }],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            indexAxis: 'y',
            plugins: {
              legend: { display: false },
            },
            scales: {
              x: { beginAtZero: true, grid: { color: '#F1F5F9' } },
              y: { grid: { display: false } },
            },
          },
        });
      }
    } catch (err) {
      console.error('Failed to load top products:', err);
    }
  },

  async loadExpiryWastage() {
    try {
      const data = await API.get('/reports/expiry-wastage');
      document.getElementById('report-expired-batches').textContent = data.totalExpiredBatches;
      document.getElementById('report-expired-units').textContent = data.totalExpiredUnits;
      document.getElementById('report-loss-amount').textContent = `₹${Number(data.estimatedLossAmount).toFixed(2)}`;

      const tbody = document.getElementById('report-wastage-tbody');
      if (tbody) {
        if (!data.items || data.items.length === 0) {
          tbody.innerHTML = `<tr><td colspan="5" class="table-empty-state"><i class="fa-solid fa-circle-check" style="color:#10B981;"></i><p>Zero expired stock waste! All inventory batches are safe.</p></td></tr>`;
        } else {
          tbody.innerHTML = data.items.map(item => `
            <tr>
              <td><strong>${item.productName}</strong></td>
              <td><code>${item.batchNumber}</code></td>
              <td>${item.expiredQuantity} units</td>
              <td style="color:#EF4444; font-weight:700;">-₹${Number(item.lossAmount).toFixed(2)}</td>
              <td>${item.expiryDate}</td>
            </tr>
          `).join('');
        }
      }
    } catch (err) {
      console.error('Failed to load expiry wastage report:', err);
    }
  },

  async exportCsv() {
    try {
      const blob = await API.get('/reports/export-csv');
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `StockGuard-Sales-Report-${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      a.remove();
      App.toast('Sales CSV report exported successfully', 'success');
    } catch (err) {
      App.toast(err.message, 'danger');
    }
  },

  setupListeners() {
    const exportBtn = document.getElementById('btn-export-csv');
    if (exportBtn) {
      exportBtn.addEventListener('click', () => this.exportCsv());
    }
  }
};

window.Reports = Reports;
