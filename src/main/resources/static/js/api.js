/**
 * StockGuard Centralized API Client & Cloud Demo Engine
 * Connects to live Spring Boot backend when available, or seamlessly
 * operates in Cloud Demo mode when deployed on static hosts like Vercel.
 */
const API = {
  // Allow custom backend URL override (e.g. Render / Railway / local)
  get baseUrl() {
    const custom = localStorage.getItem('stockguard_backend_url');
    return custom ? custom.replace(/\/$/, '') : '/api';
  },

  getToken() {
    return localStorage.getItem('stockguard_token');
  },

  setToken(token) {
    if (token) {
      localStorage.setItem('stockguard_token', token);
    } else {
      localStorage.removeItem('stockguard_token');
    }
  },

  isCloudDemoMode() {
    return localStorage.getItem('stockguard_cloud_demo') === 'true' ||
           (window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1' && !localStorage.getItem('stockguard_backend_url'));
  },

  async request(endpoint, options = {}) {
    const url = `${this.baseUrl}${endpoint}`;
    const token = this.getToken();

    const headers = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const config = {
      ...options,
      headers,
    };

    // If already in Cloud Demo mode (on Vercel without custom backend), handle directly
    if (this.isCloudDemoMode()) {
      return StockGuardMockDB.handle(endpoint, options.method || 'GET', options.body ? JSON.parse(options.body) : null);
    }

    try {
      const response = await fetch(url, config);

      // If backend returns 404 for API endpoints (e.g. on Vercel where Spring Boot is not running)
      if (response.status === 404 && endpoint.startsWith('/')) {
        console.warn(`[StockGuard] Backend not found at ${url}. Activating Cloud Demo Mode.`);
        localStorage.setItem('stockguard_cloud_demo', 'true');
        return StockGuardMockDB.handle(endpoint, options.method || 'GET', options.body ? JSON.parse(options.body) : null);
      }

      // Handle 401 Unauthorized or 403 Forbidden (Session expired / invalid token)
      if ((response.status === 401 || response.status === 403) && !endpoint.startsWith('/auth/')) {
        this.setToken(null);
        localStorage.removeItem('stockguard_user');
        window.location.href = '/app.html?view=login';
        throw new Error('Session expired or access denied. Please log in again.');
      }

      // Handle No Content
      if (response.status === 204) {
        return null;
      }

      // Check if CSV or Binary
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('text/csv')) {
        return response.blob();
      }

      const data = await response.json();

      if (!response.ok) {
        const errorMsg = data.message || data.error || 'Request failed';
        throw new Error(errorMsg);
      }

      return data;
    } catch (err) {
      // If network fetch failed entirely (e.g. static host without server), fallback to Cloud Demo
      if (err.name === 'TypeError' || err.message.includes('fetch')) {
        console.warn(`[StockGuard] Network failed to reach ${url}. Activating Cloud Demo Mode.`);
        localStorage.setItem('stockguard_cloud_demo', 'true');
        return StockGuardMockDB.handle(endpoint, options.method || 'GET', options.body ? JSON.parse(options.body) : null);
      }
      console.error(`API Error on [${options.method || 'GET'} ${endpoint}]:`, err);
      throw err;
    }
  },

  get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  },

  post(endpoint, body) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  put(endpoint, body) {
    return this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body),
    });
  },

  delete(endpoint) {
    return this.request(endpoint, { method: 'DELETE' });
  },
};

/**
 * In-Browser Cloud Demo Simulator Database
 * Allows StockGuard to run 100% interactively on Vercel, Netlify, GitHub Pages, etc.
 */
const StockGuardMockDB = {
  getDB() {
    let raw = localStorage.getItem('stockguard_mock_db_v1');
    if (!raw) {
      const initial = this.seedInitial();
      localStorage.setItem('stockguard_mock_db_v1', JSON.stringify(initial));
      return initial;
    }
    return JSON.parse(raw);
  },

  saveDB(db) {
    localStorage.setItem('stockguard_mock_db_v1', JSON.stringify(db));
  },

  seedInitial() {
    const today = new Date();
    const addDays = (d, n) => {
      const r = new Date(d);
      r.setDate(r.getDate() + n);
      return r.toISOString().split('T')[0];
    };

    return {
      shop: {
        id: 1,
        name: 'StockGuard Demo Grocery & Dairy',
        ownerName: 'Atharv Raut',
        phone: '+91 9876543210',
        email: 'demo@stockguard.com',
        address: 'Shop #4, Shivaji Nagar Market, Pune, Maharashtra',
        currency: 'INR',
        minStockDefault: 10,
        expiryWarningDays: 7,
      },
      categories: [
        { id: 1, name: 'Dairy & Eggs', iconClass: 'fa-cow', description: 'Milk, Curd, Butter, Cheese, Eggs' },
        { id: 2, name: 'Bakery & Bread', iconClass: 'fa-bread-slice', description: 'Fresh bread, Buns, Rusk' },
        { id: 3, name: 'Grains & Staples', iconClass: 'fa-wheat-awn', description: 'Rice, Wheat Flour, Sugar, Dal' },
        { id: 4, name: 'Beverages', iconClass: 'fa-mug-hot', description: 'Tea, Coffee, Soft Drinks, Juices' },
        { id: 5, name: 'Packaged Snacks', iconClass: 'fa-cookie', description: 'Biscuits, Chips, Namkeen' },
        { id: 6, name: 'Cooking Oils', iconClass: 'fa-bottle-droplet', description: 'Sunflower Oil, Mustard Oil, Ghee' },
        { id: 7, name: 'Household & Detergents', iconClass: 'fa-pump-soap', description: 'Detergent powder, Dishwash bars' },
      ],
      products: [
        { id: 1, categoryId: 1, categoryName: 'Dairy & Eggs', name: 'Amul Taaza Homogenised Toned Milk 1L', sku: 'MILK-001', barcode: '890126201001', costPrice: 54.00, sellingPrice: 60.00, currentStock: 64, minStockThreshold: 10, unit: 'pack', isLowStock: false, activeBatchesCount: 2 },
        { id: 2, categoryId: 1, categoryName: 'Dairy & Eggs', name: 'Nestle A+ Nourish Dahi / Curd 400g', sku: 'CURD-001', barcode: '890105885231', costPrice: 35.00, sellingPrice: 42.00, currentStock: 24, minStockThreshold: 8, unit: 'cup', isLowStock: false, activeBatchesCount: 2 },
        { id: 3, categoryId: 1, categoryName: 'Dairy & Eggs', name: 'Amul Pasteurized Butter 500g', sku: 'BTR-001', barcode: '890126202002', costPrice: 245.00, sellingPrice: 275.00, currentStock: 16, minStockThreshold: 6, unit: 'pack', isLowStock: false, activeBatchesCount: 1 },
        { id: 4, categoryId: 1, categoryName: 'Dairy & Eggs', name: 'Britannia Processed Cheese Slices 200g', sku: 'CHS-001', barcode: '890106312456', costPrice: 130.00, sellingPrice: 150.00, currentStock: 12, minStockThreshold: 8, unit: 'pack', isLowStock: false, activeBatchesCount: 1 },
        { id: 5, categoryId: 2, categoryName: 'Bakery & Bread', name: 'Harvest Gold White Bread 400g', sku: 'BRD-001', barcode: '890204300101', costPrice: 32.00, sellingPrice: 40.00, currentStock: 33, minStockThreshold: 10, unit: 'loaf', isLowStock: false, activeBatchesCount: 2 },
        { id: 6, categoryId: 1, categoryName: 'Dairy & Eggs', name: 'Farm Fresh White Eggs (Pack of 6)', sku: 'EGG-006', barcode: '890800100606', costPrice: 45.00, sellingPrice: 54.00, currentStock: 40, minStockThreshold: 12, unit: 'tray', isLowStock: false, activeBatchesCount: 1 },
        { id: 7, categoryId: 3, categoryName: 'Grains & Staples', name: 'India Gate Basmati Rice Feast Rozzana 1kg', sku: 'RICE-001', barcode: '890112345678', costPrice: 90.00, sellingPrice: 115.00, currentStock: 20, minStockThreshold: 5, unit: 'pack', isLowStock: false, activeBatchesCount: 1 },
        { id: 8, categoryId: 3, categoryName: 'Grains & Staples', name: 'Aashirvaad Shvaas Whole Wheat Atta 5kg', sku: 'ATTA-005', barcode: '890103001010', costPrice: 270.00, sellingPrice: 315.00, currentStock: 15, minStockThreshold: 5, unit: 'bag', isLowStock: false, activeBatchesCount: 1 },
        { id: 9, categoryId: 3, categoryName: 'Grains & Staples', name: 'Tata Salt Vacuum Evaporated Iodised 1kg', sku: 'SALT-001', barcode: '890123456789', costPrice: 22.00, sellingPrice: 28.00, currentStock: 45, minStockThreshold: 10, unit: 'pack', isLowStock: false, activeBatchesCount: 1 },
        { id: 10, categoryId: 3, categoryName: 'Grains & Staples', name: 'Madhur Pure Refined Sugar 1kg', sku: 'SUG-001', barcode: '890234567890', costPrice: 42.00, sellingPrice: 50.00, currentStock: 30, minStockThreshold: 8, unit: 'pack', isLowStock: false, activeBatchesCount: 1 },
        { id: 11, categoryId: 4, categoryName: 'Beverages', name: 'Tata Tea Gold Leaf Tea 500g', sku: 'TEA-001', barcode: '890103038841', costPrice: 280.00, sellingPrice: 325.00, currentStock: 18, minStockThreshold: 6, unit: 'pack', isLowStock: false, activeBatchesCount: 1 },
        { id: 12, categoryId: 4, categoryName: 'Beverages', name: 'Nescafe Classic Instant Coffee Jar 50g', sku: 'COF-001', barcode: '890105800001', costPrice: 165.00, sellingPrice: 195.00, currentStock: 14, minStockThreshold: 5, unit: 'jar', isLowStock: false, activeBatchesCount: 1 },
        { id: 13, categoryId: 4, categoryName: 'Beverages', name: 'Coca-Cola Original Taste Bottle 750ml', sku: 'COKE-750', barcode: '890176401223', costPrice: 32.00, sellingPrice: 40.00, currentStock: 28, minStockThreshold: 10, unit: 'bottle', isLowStock: false, activeBatchesCount: 1 },
        { id: 14, categoryId: 5, categoryName: 'Packaged Snacks', name: "Lay's India's Magic Masala Chips 50g", sku: 'CHIP-001', barcode: '890149110188', costPrice: 16.00, sellingPrice: 20.00, currentStock: 50, minStockThreshold: 15, unit: 'pack', isLowStock: false, activeBatchesCount: 1 },
        { id: 15, categoryId: 5, categoryName: 'Packaged Snacks', name: 'Parle-G Original Glucose Biscuits 250g', sku: 'PRLG-250', barcode: '890171910001', costPrice: 22.00, sellingPrice: 28.00, currentStock: 4, minStockThreshold: 15, unit: 'pack', isLowStock: true, activeBatchesCount: 1 },
        { id: 16, categoryId: 6, categoryName: 'Cooking Oils', name: 'Fortune Sunlite Refined Sunflower Oil 1L', sku: 'OIL-001', barcode: '890600728001', costPrice: 125.00, sellingPrice: 148.00, currentStock: 2, minStockThreshold: 10, unit: 'pouch', isLowStock: true, activeBatchesCount: 1 },
      ],
      batches: [
        { id: 1, productId: 1, productName: 'Amul Taaza Homogenised Toned Milk 1L', batchNumber: 'BATCH-MLK-01', initialQuantity: 30, remainingQuantity: 24, purchaseCost: 54.00, sellingPrice: 60.00, mfgDate: addDays(today, -5), expiryDate: addDays(today, 4), daysUntilExpiry: 4, status: 'EXPIRING_SOON' },
        { id: 2, productId: 1, productName: 'Amul Taaza Homogenised Toned Milk 1L', batchNumber: 'BATCH-MLK-02', initialQuantity: 40, remainingQuantity: 40, purchaseCost: 54.00, sellingPrice: 60.00, mfgDate: addDays(today, -1), expiryDate: addDays(today, 10), daysUntilExpiry: 10, status: 'SAFE' },
        { id: 3, productId: 2, productName: 'Nestle A+ Nourish Dahi / Curd 400g', batchNumber: 'BATCH-CRD-01', initialQuantity: 15, remainingQuantity: 6, purchaseCost: 35.00, sellingPrice: 42.00, mfgDate: addDays(today, -12), expiryDate: addDays(today, -2), daysUntilExpiry: -2, status: 'EXPIRED' },
        { id: 4, productId: 2, productName: 'Nestle A+ Nourish Dahi / Curd 400g', batchNumber: 'BATCH-CRD-02', initialQuantity: 20, remainingQuantity: 18, purchaseCost: 35.00, sellingPrice: 42.00, mfgDate: addDays(today, -2), expiryDate: addDays(today, 5), daysUntilExpiry: 5, status: 'EXPIRING_SOON' },
        { id: 5, productId: 3, productName: 'Amul Pasteurized Butter 500g', batchNumber: 'BATCH-BTR-01', initialQuantity: 20, remainingQuantity: 16, purchaseCost: 245.00, sellingPrice: 275.00, mfgDate: addDays(today, -20), expiryDate: addDays(today, 90), daysUntilExpiry: 90, status: 'SAFE' },
        { id: 6, productId: 4, productName: 'Britannia Processed Cheese Slices 200g', batchNumber: 'BATCH-CHS-01', initialQuantity: 15, remainingQuantity: 12, purchaseCost: 130.00, sellingPrice: 150.00, mfgDate: addDays(today, -30), expiryDate: addDays(today, 60), daysUntilExpiry: 60, status: 'SAFE' },
        { id: 7, productId: 5, productName: 'Harvest Gold White Bread 400g', batchNumber: 'BATCH-BRD-01', initialQuantity: 20, remainingQuantity: 8, purchaseCost: 32.00, sellingPrice: 40.00, mfgDate: addDays(today, -4), expiryDate: addDays(today, 1), daysUntilExpiry: 1, status: 'EXPIRING_SOON' },
        { id: 8, productId: 5, productName: 'Harvest Gold White Bread 400g', batchNumber: 'BATCH-BRD-02', initialQuantity: 25, remainingQuantity: 25, purchaseCost: 32.00, sellingPrice: 40.00, mfgDate: addDays(today, -1), expiryDate: addDays(today, 4), daysUntilExpiry: 4, status: 'EXPIRING_SOON' },
      ],
      movements: [
        { id: 1, productName: 'Amul Taaza Milk 1L', movementType: 'PURCHASE', quantityChanged: 40, previousQuantity: 24, newQuantity: 64, reason: 'Supplier stock delivery BATCH-MLK-02', referenceCode: 'BATCH-MLK-02', createdAt: new Date(Date.now() - 3600000).toISOString() },
        { id: 2, productName: 'Parle-G Biscuits 250g', movementType: 'SALE', quantityChanged: -6, previousQuantity: 10, newQuantity: 4, reason: 'POS Bill #POS-8821', referenceCode: 'SALE-8821', createdAt: new Date(Date.now() - 7200000).toISOString() },
      ],
      sales: [
        { id: 1, invoiceNumber: 'POS-8821', customerName: 'Walk-in Customer', customerPhone: '+919876543210', totalAmount: 310.00, discountAmount: 0, finalAmount: 310.00, paymentMethod: 'UPI', createdAt: new Date().toISOString(), itemsCount: 4 },
      ],
      notifications: {
        preferences: {
          whatsappPhone: '+919876543210',
          expiryWarningDays: 7,
          enableLowStock: true,
          enableExpiry: true,
          enableDailySales: true,
          metaApiToken: '',
          phoneNumberId: '',
          businessAccountId: '',
          useLiveApi: false,
        },
        logs: [
          { id: 1, recipientPhone: '+919876543210', templateName: 'EXPIRY_WARNING', messageContent: '⚠️ [StockGuard Expiry Warning] Products nearing expiry within 7 days: Milk, Bread.', status: 'SIMULATED', providerResponse: 'Simulated locally via StockGuard Dev Mock Engine.', createdAt: new Date().toISOString() },
        ],
      },
    };
  },

  handle(endpoint, method, body) {
    const db = this.getDB();
    const cleanEp = endpoint.split('?')[0];

    // Auth
    if (cleanEp === '/auth/login') {
      const email = body?.email || 'demo@stockguard.com';
      return {
        token: 'eyJhbGciOiJIUzM4NCJ9.eyJyb2xlIjoiT1dORVIiLCJzaG9wSWQiOjEsInN1YiI6ImRlbW9Ac3RvY2tndWFyZC5jb20iLCJpYXQiOjE3OTE2MDY4ODYsImV4cCI6MTc5NTIwMDAwMH0.mock_token_for_vercel',
        userId: 1,
        email: email,
        fullName: db.shop.ownerName || 'Atharv Raut',
        role: 'OWNER',
        shopId: 1,
        shopName: db.shop.name,
        currency: db.shop.currency || 'INR',
      };
    }

    if (cleanEp === '/auth/register') {
      db.shop.name = body?.shopName || db.shop.name;
      db.shop.ownerName = body?.ownerName || db.shop.ownerName;
      db.shop.email = body?.email || db.shop.email;
      db.shop.phone = body?.phone || db.shop.phone;
      this.saveDB(db);
      return {
        token: 'eyJhbGciOiJIUzM4NCJ9.eyJyb2xlIjoiT1dORVIiLCJzaG9wSWQiOjEsInN1YiI6ImRlbW9Ac3RvY2tndWFyZC5jb20iLCJpYXQiOjE3OTE2MDY4ODYsImV4cCI6MTc5NTIwMDAwMH0.mock_token_for_vercel',
        userId: 1,
        email: db.shop.email,
        fullName: db.shop.ownerName,
        role: 'OWNER',
        shopId: 1,
        shopName: db.shop.name,
        currency: 'INR',
      };
    }

    // Reports / Dashboard
    if (cleanEp === '/reports/dashboard') {
      const lowStock = db.products.filter(p => p.currentStock <= p.minStockThreshold).length;
      const expired = db.batches.filter(b => b.daysUntilExpiry <= 0).length;
      const expiringSoon = db.batches.filter(b => b.daysUntilExpiry > 0 && b.daysUntilExpiry <= 7).length;
      const totalUnits = db.products.reduce((acc, p) => acc + p.currentStock, 0);
      const todayTotal = db.sales.reduce((acc, s) => acc + s.finalAmount, 0);

      return {
        totalProducts: db.products.length,
        totalStockQuantity: totalUnits,
        lowStockCount: lowStock,
        expiringSoonCount: expiringSoon,
        expiredCount: expired,
        todaySales: todayTotal,
        monthlySales: todayTotal + 3116.00,
        todayOrdersCount: db.sales.length,
        currency: db.shop.currency || 'INR',
      };
    }

    if (cleanEp === '/reports/monthly-sales') {
      return {
        months: ['May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct'],
        sales: [28500, 31200, 29800, 34500, 38200, 42150],
        orderCounts: [95, 108, 102, 118, 130, 142],
        categoryBreakdown: {
          'Dairy & Eggs': 15200,
          'Bakery & Bread': 7400,
          'Grains & Staples': 11800,
          'Beverages': 4500,
          'Packaged Snacks': 3250,
        },
      };
    }

    if (cleanEp === '/reports/top-products') {
      return [
        { id: 1, name: 'Amul Taaza Milk 1L', unitsSold: 142, revenue: 8520 },
        { id: 5, name: 'Harvest Gold Bread 400g', unitsSold: 98, revenue: 3920 },
        { id: 2, name: 'Nestle A+ Curd 400g', unitsSold: 84, revenue: 3528 },
        { id: 7, name: 'India Gate Basmati 1kg', unitsSold: 45, revenue: 5175 },
        { id: 9, name: 'Tata Salt 1kg', unitsSold: 72, revenue: 2016 },
      ];
    }

    if (cleanEp === '/reports/expiry-wastage') {
      return {
        totalLossValue: 210.00,
        expiredBatchesCount: 1,
        quarantinedUnitsCount: 6,
        currency: db.shop.currency || 'INR',
        lossesByCategory: { 'Dairy & Eggs': 210.00 },
      };
    }

    if (cleanEp === '/reports/export-csv') {
      const csv = 'Date,Invoice,Customer,Amount,PaymentMethod\n2026-10-10,POS-8821,Walk-in,310.00,UPI\n';
      return new Blob([csv], { type: 'text/csv' });
    }

    // Alerts
    if (cleanEp === '/alerts') {
      const lowStock = db.products.filter(p => p.currentStock <= p.minStockThreshold);
      const expiring = db.batches.filter(b => b.daysUntilExpiry <= 7);
      return {
        lowStockProducts: lowStock,
        expiringBatches: expiring,
      };
    }

    // Categories
    if (cleanEp === '/categories') {
      return db.categories;
    }

    // Products
    if (cleanEp === '/products') {
      if (method === 'POST') {
        const newP = {
          id: db.products.length ? Math.max(...db.products.map(p => p.id)) + 1 : 1,
          shopId: 1,
          categoryId: body.categoryId,
          categoryName: db.categories.find(c => c.id === body.categoryId)?.name || 'General',
          name: body.name,
          sku: body.sku || 'SKU-' + Math.floor(Math.random() * 10000),
          barcode: body.barcode || null,
          costPrice: parseFloat(body.costPrice || 0),
          sellingPrice: parseFloat(body.sellingPrice || 0),
          currentStock: 0,
          minStockThreshold: parseInt(body.minStockAlert || db.shop.minStockDefault || 10),
          unit: body.unit || 'pcs',
          isLowStock: true,
          activeBatchesCount: 0,
          createdAt: new Date().toISOString(),
        };
        db.products.push(newP);
        this.saveDB(db);
        return newP;
      }
      return db.products;
    }

    if (cleanEp.startsWith('/products/')) {
      const id = parseInt(cleanEp.replace('/products/', ''));
      if (method === 'DELETE') {
        db.products = db.products.filter(p => p.id !== id);
        this.saveDB(db);
        return null;
      }
      if (method === 'PUT') {
        const idx = db.products.findIndex(p => p.id === id);
        if (idx !== -1) {
          db.products[idx] = { ...db.products[idx], ...body };
          this.saveDB(db);
          return db.products[idx];
        }
      }
      return db.products.find(p => p.id === id) || null;
    }

    // Batches
    if (cleanEp === '/batches') {
      if (method === 'POST') {
        const prod = db.products.find(p => p.id === parseInt(body.productId));
        const newB = {
          id: db.batches.length ? Math.max(...db.batches.map(b => b.id)) + 1 : 1,
          productId: body.productId,
          productName: prod ? prod.name : 'Product',
          batchNumber: body.batchNumber || 'BATCH-' + Math.floor(Math.random() * 1000),
          initialQuantity: parseInt(body.quantity),
          remainingQuantity: parseInt(body.quantity),
          purchaseCost: parseFloat(body.purchaseCost || (prod ? prod.costPrice : 0)),
          sellingPrice: parseFloat(body.sellingPrice || (prod ? prod.sellingPrice : 0)),
          mfgDate: body.mfgDate,
          expiryDate: body.expiryDate,
          daysUntilExpiry: Math.ceil((new Date(body.expiryDate) - new Date()) / (1000 * 60 * 60 * 24)),
          status: 'SAFE',
        };
        db.batches.push(newB);
        if (prod) prod.currentStock += newB.remainingQuantity;
        this.saveDB(db);
        return newB;
      }
      return db.batches;
    }

    if (cleanEp.includes('/discard')) {
      const batchId = parseInt(cleanEp.split('/')[2]);
      const b = db.batches.find(x => x.id === batchId);
      if (b) {
        const prod = db.products.find(p => p.id === b.productId);
        if (prod) prod.currentStock = Math.max(0, prod.currentStock - b.remainingQuantity);
        b.remainingQuantity = 0;
        b.status = 'DISCARDED';
        this.saveDB(db);
      }
      return { message: 'Batch quarantined and discarded' };
    }

    // Inventory Adjust & Movements
    if (cleanEp === '/inventory/adjust') {
      const p = db.products.find(x => x.id === parseInt(body.productId));
      if (p) {
        const diff = parseInt(body.quantityDifference);
        const oldQty = p.currentStock;
        p.currentStock = Math.max(0, p.currentStock + diff);
        db.movements.unshift({
          id: db.movements.length + 1,
          productName: p.name,
          movementType: diff >= 0 ? 'ADJUST_ADD' : 'ADJUST_SUB',
          quantityChanged: diff,
          previousQuantity: oldQty,
          newQuantity: p.currentStock,
          reason: body.reason || 'Manual count correction',
          referenceCode: 'ADJ-' + Date.now().toString().slice(-4),
          createdAt: new Date().toISOString(),
        });
        this.saveDB(db);
      }
      return { message: 'Stock adjusted successfully' };
    }

    if (cleanEp === '/inventory/movements') {
      return db.movements;
    }

    // Sales POS
    if (cleanEp === '/sales') {
      if (method === 'POST') {
        const invoiceNum = 'POS-' + Math.floor(1000 + Math.random() * 9000);
        const items = body.items || [];
        let total = 0;
        items.forEach(it => {
          total += (it.quantity * it.unitPrice);
          const p = db.products.find(prod => prod.id === it.productId);
          if (p) p.currentStock = Math.max(0, p.currentStock - it.quantity);
        });

        const newSale = {
          id: db.sales.length + 1,
          invoiceNumber: invoiceNum,
          customerName: body.customerName || 'Walk-in Customer',
          customerPhone: body.customerPhone || '',
          totalAmount: total,
          discountAmount: body.discountAmount || 0,
          finalAmount: Math.max(0, total - (body.discountAmount || 0)),
          paymentMethod: body.paymentMethod || 'CASH',
          createdAt: new Date().toISOString(),
          items: items.map(it => {
            const pr = db.products.find(p => p.id === it.productId);
            return {
              productName: pr ? pr.name : 'Item',
              quantity: it.quantity,
              unitPrice: it.unitPrice,
              subtotal: it.quantity * it.unitPrice,
            };
          }),
        };
        db.sales.unshift(newSale);
        this.saveDB(db);
        return newSale;
      }
      return db.sales;
    }

    if (cleanEp.startsWith('/sales/')) {
      const id = parseInt(cleanEp.replace('/sales/', ''));
      return db.sales.find(s => s.id === id) || db.sales[0];
    }

    // Notifications
    if (cleanEp === '/notifications/preferences') {
      if (method === 'PUT') {
        db.notifications.preferences = { ...db.notifications.preferences, ...body };
        this.saveDB(db);
        return db.notifications.preferences;
      }
      return db.notifications.preferences;
    }

    if (cleanEp === '/notifications/logs') {
      return db.notifications.logs;
    }

    if (cleanEp === '/notifications/send') {
      const isLive = Boolean(db.notifications.preferences.useLiveApi);
      const logItem = {
        id: db.notifications.logs.length + 1,
        recipientPhone: body.recipientPhone,
        templateName: body.templateName || 'EXPIRY_WARNING',
        messageContent: `📢 [StockGuard ${body.templateName}] ${body.customMessage || 'Alert dispatched to store owner.'}`,
        status: isLive ? 'SENT' : 'SIMULATED',
        providerResponse: isLive
          ? 'Meta Cloud API Response (HTTP 200): Message queued for delivery.'
          : 'Simulated locally via StockGuard Dev Mock Engine (Vercel Cloud Demo).',
        createdAt: new Date().toISOString(),
      };
      db.notifications.logs.unshift(logItem);
      this.saveDB(db);
      return logItem;
    }

    // Shop Settings & DB Status
    if (cleanEp === '/settings/shop') {
      if (method === 'PUT') {
        db.shop = { ...db.shop, ...body };
        this.saveDB(db);
        return db.shop;
      }
      return db.shop;
    }

    if (cleanEp === '/settings/db-status') {
      return {
        connected: true,
        isH2: true,
        shopsCount: 1,
        databaseProduct: 'Browser Local Storage',
        engineName: 'Cloud Demo Engine (Vercel / LocalStorage)',
        status: 'HEALTHY',
        message: 'Active & healthy. Running in Vercel Cloud Demo mode. All changes persist in your browser!',
        databaseVersion: 'StockGuard v1.0.0',
        databaseUrl: 'browser://localStorage',
      };
    }

    return { message: 'OK' };
  },
};

window.API = API;
window.StockGuardMockDB = StockGuardMockDB;
