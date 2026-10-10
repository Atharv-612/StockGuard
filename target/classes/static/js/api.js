/**
 * StockGuard Centralized API Client
 */
const API = {
  baseUrl: '/api',

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

    try {
      const response = await fetch(url, config);

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

window.API = API;
