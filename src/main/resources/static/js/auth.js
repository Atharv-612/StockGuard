/**
 * StockGuard Authentication Management
 */
const Auth = {
  getUser() {
    const raw = localStorage.getItem('stockguard_user');
    return raw ? JSON.parse(raw) : null;
  },

  setUser(user) {
    if (user) {
      localStorage.setItem('stockguard_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('stockguard_user');
    }
  },

  isAuthenticated() {
    const token = API.getToken();
    const user = this.getUser();
    if (!token || !user) return false;
    try {
      const parts = token.split('.');
      if (parts.length === 3) {
        const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        const payload = JSON.parse(jsonPayload);
        if (payload.exp && Date.now() >= payload.exp * 1000) {
          this.logout(false);
          return false;
        }
      }
    } catch (e) {
      this.logout(false);
      return false;
    }
    return true;
  },

  async login(email, password) {
    try {
      const resp = await API.post('/auth/login', { email, password });
      API.setToken(resp.token);
      this.setUser({
        id: resp.userId,
        email: resp.email,
        fullName: resp.fullName,
        role: resp.role,
        shopId: resp.shopId,
        shopName: resp.shopName,
        currency: resp.currency || 'INR',
      });
      return resp;
    } catch (err) {
      throw err;
    }
  },

  async register(shopName, ownerName, email, phone, password) {
    try {
      const resp = await API.post('/auth/register', {
        shopName,
        ownerName,
        email,
        phone,
        password,
      });
      API.setToken(resp.token);
      this.setUser({
        id: resp.userId,
        email: resp.email,
        fullName: resp.fullName,
        role: resp.role,
        shopId: resp.shopId,
        shopName: resp.shopName,
        currency: resp.currency || 'INR',
      });
      return resp;
    } catch (err) {
      throw err;
    }
  },

  async logout(redirect = true) {
    try {
      if (API.getToken()) {
        await API.post('/auth/logout', {});
      }
    } catch (e) {
      // Ignore network errors on logout
    } finally {
      API.setToken(null);
      this.setUser(null);
      if (redirect) {
        window.location.href = '/app.html?view=login';
      }
    }
  },

  updateShopName(newName) {
    const u = this.getUser();
    if (u) {
      u.shopName = newName;
      this.setUser(u);
    }
  }
};

window.Auth = Auth;
