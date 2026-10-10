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
 * Fully compatible with backend DTOs and handles real-time data persistence.
 */
const StockGuardMockDB = {
  DB_KEY: 'stockguard_mock_db_v2',

  getDB() {
    let raw = localStorage.getItem(this.DB_KEY);
    let db;
    if (!raw) {
      // Check if older version exists to migrate or seed fresh
      const oldRaw = localStorage.getItem('stockguard_mock_db_v1');
      if (oldRaw) {
        try {
          db = JSON.parse(oldRaw);
        } catch (e) {
          db = null;
        }
      }
      if (!db) {
        db = this.seedInitial();
      }
    } else {
      try {
        db = JSON.parse(raw);
      } catch (e) {
        db = this.seedInitial();
      }
    }

    // Self-healing / normalization pass to ensure all fields strictly conform to DTO contracts
    db = this.normalizeDB(db);
    this.saveDB(db);
    return db;
  },

  saveDB(db) {
    try {
      localStorage.setItem(this.DB_KEY, JSON.stringify(db));
    } catch (e) {
      console.warn('LocalStorage save failed:', e);
    }
  },

  normalizeDB(db) {
    if (!db) return this.seedInitial();

    if (!Array.isArray(db.categories)) db.categories = [];
    if (!Array.isArray(db.products)) db.products = [];
    if (!Array.isArray(db.batches)) db.batches = [];
    if (!Array.isArray(db.movements)) db.movements = [];
    if (!Array.isArray(db.sales)) db.sales = [];
    if (!db.shop) db.shop = { id: 1, name: 'StockGuard Demo Grocery & Dairy', currency: 'INR', minStockDefault: 10, expiryWarningDays: 7 };

    const today = new Date();

    // Normalize Batches
    db.batches.forEach(b => {
      if (b.currentQuantity === undefined) {
        b.currentQuantity = b.remainingQuantity !== undefined ? b.remainingQuantity : (b.initialQuantity || 0);
      }
      if (b.costPrice === undefined) {
        b.costPrice = b.purchaseCost !== undefined ? b.purchaseCost : 0;
      }
      if (b.sellingPrice === undefined) {
        const prod = db.products.find(p => p.id === b.productId);
        b.sellingPrice = prod ? prod.sellingPrice : 0;
      }
      if (b.expiryDate) {
        b.daysUntilExpiry = Math.ceil((new Date(b.expiryDate) - today) / (1000 * 60 * 60 * 24));
        b.expiryState = b.daysUntilExpiry <= 0 ? 'EXPIRED' : (b.daysUntilExpiry <= 7 ? 'EXPIRING_SOON' : 'SAFE');
      } else {
        b.daysUntilExpiry = 30;
        b.expiryState = 'SAFE';
      }
      if (!b.categoryName) {
        const prod = db.products.find(p => p.id === b.productId);
        b.categoryName = prod ? prod.categoryName : 'General';
      }
      if (!b.status) {
        b.status = b.daysUntilExpiry <= 0 ? 'EXPIRED' : 'ACTIVE';
      }
    });

    // Normalize Products
    db.products.forEach(p => {
      if (p.currentStock === undefined) p.currentStock = 0;
      if (p.minStockThreshold === undefined) p.minStockThreshold = 10;
      p.isLowStock = p.currentStock <= p.minStockThreshold;
      p.activeBatchesCount = db.batches.filter(b => b.productId === p.id && b.currentQuantity > 0).length;
    });

    // Normalize Sales
    db.sales.forEach(s => {
      s.saleDate = s.saleDate || s.createdAt || new Date().toISOString();
      s.subtotal = s.subtotal !== undefined ? s.subtotal : (s.totalAmount || 0);
      s.discount = s.discount !== undefined ? s.discount : (s.discountAmount || 0);
      s.tax = s.tax || 0;
      s.totalAmount = s.totalAmount !== undefined ? s.totalAmount : (s.finalAmount || (s.subtotal - s.discount));
      if (!Array.isArray(s.items)) s.items = [];
      s.items.forEach(it => {
        if (it.totalPrice === undefined) {
          it.totalPrice = (it.quantity || 1) * (it.unitPrice || 0);
        }
      });
    });

    return db;
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
        { id: 1, productId: 1, productName: 'Amul Taaza Homogenised Toned Milk 1L', categoryName: 'Dairy & Eggs', batchNumber: 'BATCH-MLK-01', initialQuantity: 30, currentQuantity: 24, costPrice: 54.00, sellingPrice: 60.00, mfgDate: addDays(today, -5), expiryDate: addDays(today, 4), daysUntilExpiry: 4, expiryState: 'EXPIRING_SOON', status: 'ACTIVE' },
        { id: 2, productId: 1, productName: 'Amul Taaza Homogenised Toned Milk 1L', categoryName: 'Dairy & Eggs', batchNumber: 'BATCH-MLK-02', initialQuantity: 40, currentQuantity: 40, costPrice: 54.00, sellingPrice: 60.00, mfgDate: addDays(today, -1), expiryDate: addDays(today, 10), daysUntilExpiry: 10, expiryState: 'SAFE', status: 'ACTIVE' },
        { id: 3, productId: 2, productName: 'Nestle A+ Nourish Dahi / Curd 400g', categoryName: 'Dairy & Eggs', batchNumber: 'BATCH-CRD-01', initialQuantity: 15, currentQuantity: 6, costPrice: 35.00, sellingPrice: 42.00, mfgDate: addDays(today, -12), expiryDate: addDays(today, -2), daysUntilExpiry: -2, expiryState: 'EXPIRED', status: 'EXPIRED' },
        { id: 4, productId: 2, productName: 'Nestle A+ Nourish Dahi / Curd 400g', categoryName: 'Dairy & Eggs', batchNumber: 'BATCH-CRD-02', initialQuantity: 20, currentQuantity: 18, costPrice: 35.00, sellingPrice: 42.00, mfgDate: addDays(today, -2), expiryDate: addDays(today, 5), daysUntilExpiry: 5, expiryState: 'EXPIRING_SOON', status: 'ACTIVE' },
        { id: 5, productId: 3, productName: 'Amul Pasteurized Butter 500g', categoryName: 'Dairy & Eggs', batchNumber: 'BATCH-BTR-01', initialQuantity: 20, currentQuantity: 16, costPrice: 245.00, sellingPrice: 275.00, mfgDate: addDays(today, -20), expiryDate: addDays(today, 90), daysUntilExpiry: 90, expiryState: 'SAFE', status: 'ACTIVE' },
        { id: 6, productId: 4, productName: 'Britannia Processed Cheese Slices 200g', categoryName: 'Dairy & Eggs', batchNumber: 'BATCH-CHS-01', initialQuantity: 15, currentQuantity: 12, costPrice: 130.00, sellingPrice: 150.00, mfgDate: addDays(today, -30), expiryDate: addDays(today, 60), daysUntilExpiry: 60, expiryState: 'SAFE', status: 'ACTIVE' },
        { id: 7, productId: 5, productName: 'Harvest Gold White Bread 400g', categoryName: 'Bakery & Bread', batchNumber: 'BATCH-BRD-01', initialQuantity: 20, currentQuantity: 8, costPrice: 32.00, sellingPrice: 40.00, mfgDate: addDays(today, -4), expiryDate: addDays(today, 1), daysUntilExpiry: 1, expiryState: 'EXPIRING_SOON', status: 'ACTIVE' },
        { id: 8, productId: 5, productName: 'Harvest Gold White Bread 400g', categoryName: 'Bakery & Bread', batchNumber: 'BATCH-BRD-02', initialQuantity: 25, currentQuantity: 25, costPrice: 32.00, sellingPrice: 40.00, mfgDate: addDays(today, -1), expiryDate: addDays(today, 4), daysUntilExpiry: 4, expiryState: 'EXPIRING_SOON', status: 'ACTIVE' },
      ],
      movements: [
        { id: 1, productName: 'Amul Taaza Homogenised Toned Milk 1L', batchNumber: 'BATCH-MLK-02', movementType: 'PURCHASE', quantityChanged: 40, quantityAfter: 64, reference: 'BATCH-MLK-02', notes: 'Supplier delivery', createdAt: new Date(Date.now() - 3600000).toISOString() },
        { id: 2, productName: 'Harvest Gold White Bread 400g', batchNumber: 'BATCH-BRD-01', movementType: 'SALE', quantityChanged: -2, quantityAfter: 33, reference: 'POS-8821', notes: 'Walk-in sale', createdAt: new Date(Date.now() - 7200000).toISOString() },
      ],
      sales: [
        {
          id: 1,
          invoiceNumber: 'POS-8821',
          customerName: 'Pooja Sharma',
          customerPhone: '+919876543210',
          subtotal: 310.00,
          discount: 0,
          tax: 0,
          totalAmount: 310.00,
          paymentMethod: 'UPI',
          saleDate: new Date(Date.now() - 7200000).toISOString(),
          items: [
            { id: 1, productId: 1, productName: 'Amul Taaza Homogenised Toned Milk 1L', batchId: 1, batchNumber: 'BATCH-MLK-01', quantity: 2, unitPrice: 60.00, totalPrice: 120.00 },
            { id: 2, productId: 5, productName: 'Harvest Gold White Bread 400g', batchId: 7, batchNumber: 'BATCH-BRD-01', quantity: 2, unitPrice: 40.00, totalPrice: 80.00 },
            { id: 3, productId: 7, productName: 'India Gate Basmati Rice Feast Rozzana 1kg', batchId: null, batchNumber: 'FIFO', quantity: 1, unitPrice: 110.00, totalPrice: 110.00 },
          ],
        },
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
    const [pathPart, queryPart] = endpoint.split('?');
    const cleanEp = pathPart;
    const searchParams = new URLSearchParams(queryPart || '');

    // Auth Login
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

    // Auth Register
    if (cleanEp === '/auth/register') {
      if (body?.shopName) db.shop.name = body.shopName;
      if (body?.ownerName) db.shop.ownerName = body.ownerName;
      if (body?.email) db.shop.email = body.email;
      if (body?.phone) db.shop.phone = body.phone;
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

    // Reports: Dashboard Metrics
    if (cleanEp === '/reports/dashboard') {
      const lowStock = db.products.filter(p => p.currentStock <= p.minStockThreshold).length;
      const expired = db.batches.filter(b => b.daysUntilExpiry <= 0 && b.currentQuantity > 0).length;
      const expiringSoon = db.batches.filter(b => b.daysUntilExpiry > 0 && b.daysUntilExpiry <= 7 && b.currentQuantity > 0).length;
      const totalUnits = db.products.reduce((acc, p) => acc + (p.currentStock || 0), 0);
      const todayTotal = db.sales.reduce((acc, s) => acc + (s.totalAmount || 0), 0);

      return {
        totalProducts: db.products.length,
        totalStockQuantity: totalUnits,
        lowStockCount: lowStock,
        expiringSoonCount: expiringSoon,
        expiredCount: expired,
        todaySales: todayTotal,
        monthlySales: todayTotal + 38450.00,
        todayOrdersCount: db.sales.length,
        currency: db.shop.currency || 'INR',
      };
    }

    // Reports: Monthly Sales Trend (MUST RETURN ARRAY of objects to support monthlyData.map)
    if (cleanEp === '/reports/monthly-sales') {
      return [
        { label: 'May', revenue: 28500, orderCount: 95 },
        { label: 'Jun', revenue: 31200, orderCount: 108 },
        { label: 'Jul', revenue: 29800, orderCount: 102 },
        { label: 'Aug', revenue: 34500, orderCount: 118 },
        { label: 'Sep', revenue: 38200, orderCount: 130 },
        { label: 'Oct', revenue: 42150, orderCount: 142 },
      ];
    }

    // Reports: Top Products (Array of { productId, productName, unitsSold, totalRevenue })
    if (cleanEp === '/reports/top-products') {
      return [
        { productId: 1, productName: 'Amul Taaza Homogenised Toned Milk 1L', unitsSold: 142, totalRevenue: 8520 },
        { productId: 5, productName: 'Harvest Gold White Bread 400g', unitsSold: 98, totalRevenue: 3920 },
        { productId: 2, productName: 'Nestle A+ Nourish Dahi / Curd 400g', unitsSold: 84, totalRevenue: 3528 },
        { productId: 7, productName: 'India Gate Basmati Rice Feast Rozzana 1kg', unitsSold: 45, totalRevenue: 5175 },
        { productId: 9, productName: 'Tata Salt Vacuum Evaporated Iodised 1kg', unitsSold: 72, totalRevenue: 2016 },
      ];
    }

    // Reports: Expiry Wastage Report
    if (cleanEp === '/reports/expiry-wastage') {
      const expiredBatches = db.batches.filter(b => b.daysUntilExpiry <= 0 && b.currentQuantity > 0);
      let totalUnits = 0;
      let totalLoss = 0;
      const items = expiredBatches.map(b => {
        const itemLoss = (b.costPrice || 0) * b.currentQuantity;
        totalUnits += b.currentQuantity;
        totalLoss += itemLoss;
        return {
          productName: b.productName,
          batchNumber: b.batchNumber,
          expiredQuantity: b.currentQuantity,
          costPrice: b.costPrice,
          lossAmount: itemLoss,
          expiryDate: b.expiryDate,
        };
      });

      return {
        totalExpiredBatches: expiredBatches.length,
        totalExpiredUnits: totalUnits,
        estimatedLossAmount: totalLoss,
        currency: db.shop.currency || 'INR',
        items: items,
      };
    }

    // Reports: Export CSV
    if (cleanEp === '/reports/export-csv') {
      let csv = 'Invoice Number,Date,Customer,Payment Method,Subtotal,Discount,Total Amount\n';
      db.sales.forEach(s => {
        csv += `"${s.invoiceNumber}","${s.saleDate}","${s.customerName}","${s.paymentMethod}",${s.subtotal},${s.discount},${s.totalAmount}\n`;
      });
      return new Blob([csv], { type: 'text/csv' });
    }

    // Alerts: Combined Active Alerts
    if (cleanEp === '/alerts') {
      const lowStockItems = db.products.filter(p => p.currentStock <= p.minStockThreshold);
      const expiringSoonItems = db.batches.filter(b => b.daysUntilExpiry > 0 && b.daysUntilExpiry <= 7 && b.currentQuantity > 0);
      const expiredItems = db.batches.filter(b => b.daysUntilExpiry <= 0 && b.currentQuantity > 0);

      return {
        lowStockCount: lowStockItems.length,
        lowStockItems: lowStockItems,
        expiringSoonCount: expiringSoonItems.length,
        expiringSoonItems: expiringSoonItems,
        expiredCount: expiredItems.length,
        expiredItems: expiredItems,
      };
    }

    // Categories
    if (cleanEp === '/categories') {
      return db.categories;
    }

    // Products Catalog
    if (cleanEp === '/products') {
      if (method === 'POST') {
        const catId = parseInt(body.categoryId) || 1;
        const cat = db.categories.find(c => c.id === catId);
        const minThresh = parseInt(body.minStockThreshold || body.minStockAlert || 10);
        const newP = {
          id: db.products.length ? Math.max(...db.products.map(p => p.id)) + 1 : 1,
          shopId: 1,
          categoryId: catId,
          categoryName: cat ? cat.name : 'General',
          name: body.name,
          sku: body.sku || 'SKU-' + Math.floor(1000 + Math.random() * 9000),
          barcode: body.barcode || null,
          description: body.description || '',
          costPrice: parseFloat(body.costPrice || 0),
          sellingPrice: parseFloat(body.sellingPrice || 0),
          currentStock: 0,
          minStockThreshold: minThresh,
          unit: body.unit || 'pcs',
          isLowStock: true,
          activeBatchesCount: 0,
          createdAt: new Date().toISOString(),
        };
        db.products.unshift(newP);
        this.saveDB(db);
        return newP;
      }

      // GET: Support filtering by search query and categoryId
      let list = [...db.products];
      const search = (searchParams.get('search') || '').toLowerCase().trim();
      const catFilter = searchParams.get('categoryId');

      if (catFilter) {
        list = list.filter(p => p.categoryId === parseInt(catFilter));
      }
      if (search) {
        list = list.filter(p =>
          (p.name && p.name.toLowerCase().includes(search)) ||
          (p.sku && p.sku.toLowerCase().includes(search)) ||
          (p.barcode && p.barcode.includes(search))
        );
      }
      return list;
    }

    // Product Single Item Operations
    if (cleanEp.startsWith('/products/')) {
      const id = parseInt(cleanEp.replace('/products/', ''));
      if (method === 'DELETE') {
        db.products = db.products.filter(p => p.id !== id);
        db.batches = db.batches.filter(b => b.productId !== id);
        this.saveDB(db);
        return null;
      }
      if (method === 'PUT') {
        const idx = db.products.findIndex(p => p.id === id);
        if (idx !== -1) {
          const p = db.products[idx];
          if (body.name !== undefined) p.name = body.name;
          if (body.sku !== undefined) p.sku = body.sku;
          if (body.barcode !== undefined) p.barcode = body.barcode;
          if (body.description !== undefined) p.description = body.description;
          if (body.costPrice !== undefined) p.costPrice = parseFloat(body.costPrice);
          if (body.sellingPrice !== undefined) p.sellingPrice = parseFloat(body.sellingPrice);
          if (body.minStockThreshold !== undefined) p.minStockThreshold = parseInt(body.minStockThreshold);
          if (body.unit !== undefined) p.unit = body.unit;
          if (body.categoryId !== undefined) {
            p.categoryId = parseInt(body.categoryId);
            const cat = db.categories.find(c => c.id === p.categoryId);
            if (cat) p.categoryName = cat.name;
          }
          p.isLowStock = (p.currentStock || 0) <= p.minStockThreshold;
          this.saveDB(db);
          return p;
        }
      }
      return db.products.find(p => p.id === id) || null;
    }

    // Batches & Expiry Management
    if (cleanEp === '/batches') {
      if (method === 'POST') {
        const prodId = parseInt(body.productId);
        const prod = db.products.find(p => p.id === prodId);
        const qty = parseInt(body.quantity || 0);
        const cost = parseFloat(body.costPrice !== undefined ? body.costPrice : (body.purchaseCost || (prod ? prod.costPrice : 0)));
        const sell = parseFloat(body.sellingPrice !== undefined ? body.sellingPrice : (prod ? prod.sellingPrice : 0));
        const expDate = body.expiryDate;
        const daysUntil = Math.ceil((new Date(expDate) - new Date()) / (1000 * 60 * 60 * 24));
        const state = daysUntil <= 0 ? 'EXPIRED' : (daysUntil <= 7 ? 'EXPIRING_SOON' : 'SAFE');

        const newB = {
          id: db.batches.length ? Math.max(...db.batches.map(b => b.id)) + 1 : 1,
          productId: prodId,
          productName: prod ? prod.name : 'Product',
          categoryName: prod ? prod.categoryName : 'General',
          batchNumber: body.batchNumber || 'BATCH-' + Math.floor(1000 + Math.random() * 9000),
          initialQuantity: qty,
          currentQuantity: qty,
          costPrice: cost,
          sellingPrice: sell,
          mfgDate: body.mfgDate || null,
          expiryDate: expDate,
          daysUntilExpiry: daysUntil,
          expiryState: state,
          status: daysUntil <= 0 ? 'EXPIRED' : 'ACTIVE',
          createdAt: new Date().toISOString(),
        };

        db.batches.unshift(newB);

        // Update product stock and re-evaluate low stock
        if (prod) {
          prod.currentStock = (prod.currentStock || 0) + qty;
          prod.isLowStock = prod.currentStock <= prod.minStockThreshold;
          prod.activeBatchesCount = db.batches.filter(b => b.productId === prod.id && b.currentQuantity > 0).length;
        }

        // Add movement log
        db.movements.unshift({
          id: db.movements.length + 1,
          productName: prod ? prod.name : newB.productName,
          batchNumber: newB.batchNumber,
          movementType: 'PURCHASE',
          quantityChanged: qty,
          quantityAfter: prod ? prod.currentStock : qty,
          reference: newB.batchNumber,
          notes: `Batch intake for ${newB.batchNumber}`,
          createdAt: new Date().toISOString(),
        });

        this.saveDB(db);
        return newB;
      }
      return db.batches;
    }

    // Discard Expired Batch
    if (cleanEp.includes('/discard')) {
      const parts = cleanEp.split('/');
      const batchId = parseInt(parts[2]);
      const b = db.batches.find(x => x.id === batchId);
      if (b) {
        const discardedQty = b.currentQuantity || 0;
        const prod = db.products.find(p => p.id === b.productId);
        if (prod && discardedQty > 0) {
          prod.currentStock = Math.max(0, (prod.currentStock || 0) - discardedQty);
          prod.isLowStock = prod.currentStock <= prod.minStockThreshold;
          prod.activeBatchesCount = db.batches.filter(x => x.productId === prod.id && x.id !== b.id && x.currentQuantity > 0).length;
        }
        b.currentQuantity = 0;
        b.status = 'DISCARDED';
        b.expiryState = 'EXPIRED';

        db.movements.unshift({
          id: db.movements.length + 1,
          productName: b.productName,
          batchNumber: b.batchNumber,
          movementType: 'EXPIRED_SCRAP',
          quantityChanged: -discardedQty,
          quantityAfter: prod ? prod.currentStock : 0,
          reference: 'SCRAP-' + b.batchNumber,
          notes: body?.reason || 'Expired batch quarantined and discarded',
          createdAt: new Date().toISOString(),
        });

        this.saveDB(db);
      }
      return { message: 'Batch quarantined and discarded' };
    }

    // Inventory Adjustments
    if (cleanEp === '/inventory/adjust') {
      const prodId = parseInt(body.productId);
      const p = db.products.find(x => x.id === prodId);
      if (p) {
        const diff = parseInt(body.quantityDifference || 0);
        p.currentStock = Math.max(0, (p.currentStock || 0) + diff);
        p.isLowStock = p.currentStock <= p.minStockThreshold;

        db.movements.unshift({
          id: db.movements.length + 1,
          productName: p.name,
          batchNumber: 'N/A',
          movementType: 'ADJUSTMENT',
          quantityChanged: diff,
          quantityAfter: p.currentStock,
          reference: 'ADJ-' + Date.now().toString().slice(-4),
          notes: body.reason || 'Manual count correction',
          createdAt: new Date().toISOString(),
        });

        this.saveDB(db);
      }
      return { message: 'Stock adjusted successfully' };
    }

    if (cleanEp === '/inventory/movements') {
      return db.movements;
    }

    // Sales POS Engine
    if (cleanEp === '/sales') {
      if (method === 'POST') {
        const invoiceNum = 'POS-' + Math.floor(1000 + Math.random() * 9000);
        const reqItems = body.items || [];
        let subtotal = 0;
        const saleItems = [];

        reqItems.forEach(it => {
          const prodId = parseInt(it.productId);
          const prod = db.products.find(p => p.id === prodId);
          const qty = parseInt(it.quantity || 1);
          const unitPrice = parseFloat(it.unitPrice !== undefined ? it.unitPrice : (prod ? prod.sellingPrice : 0));
          const lineTotal = qty * unitPrice;
          subtotal += lineTotal;

          if (prod) {
            prod.currentStock = Math.max(0, (prod.currentStock || 0) - qty);
            prod.isLowStock = prod.currentStock <= prod.minStockThreshold;

            // FIFO deduction from product batches
            let toDeduct = qty;
            const prodBatches = db.batches.filter(b => b.productId === prod.id && b.currentQuantity > 0);
            for (const b of prodBatches) {
              if (toDeduct <= 0) break;
              const take = Math.min(b.currentQuantity, toDeduct);
              b.currentQuantity -= take;
              toDeduct -= take;
            }
            prod.activeBatchesCount = db.batches.filter(b => b.productId === prod.id && b.currentQuantity > 0).length;

            db.movements.unshift({
              id: db.movements.length + 1,
              productName: prod.name,
              batchNumber: prodBatches[0]?.batchNumber || 'FIFO',
              movementType: 'SALE',
              quantityChanged: -qty,
              quantityAfter: prod.currentStock,
              reference: invoiceNum,
              notes: `POS sale to ${body.customerName || 'Walk-in Customer'}`,
              createdAt: new Date().toISOString(),
            });
          }

          saleItems.push({
            id: saleItems.length + 1,
            productId: prodId,
            productName: prod ? prod.name : 'Item',
            batchId: null,
            batchNumber: 'FIFO',
            quantity: qty,
            unitPrice: unitPrice,
            totalPrice: lineTotal,
          });
        });

        const discount = parseFloat(body.discount || body.discountAmount || 0);
        const totalAmount = Math.max(0, subtotal - discount);

        const newSale = {
          id: db.sales.length ? Math.max(...db.sales.map(s => s.id)) + 1 : 1,
          invoiceNumber: invoiceNum,
          customerName: body.customerName || 'Walk-in Customer',
          customerPhone: body.customerPhone || '',
          subtotal: subtotal,
          tax: 0,
          discount: discount,
          totalAmount: totalAmount,
          paymentMethod: body.paymentMethod || 'CASH',
          saleDate: new Date().toISOString(),
          items: saleItems,
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

    // Notifications & WhatsApp Alerts
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
