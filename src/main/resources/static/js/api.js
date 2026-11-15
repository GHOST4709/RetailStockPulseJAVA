const API = {
  async request(path, options = {}) {
    const config = { method: options.method || 'GET', headers: { 'Accept': 'application/json', ...(options.headers || {}) } };
    if (options.body !== undefined) {
      config.headers['Content-Type'] = 'application/json';
      config.body = JSON.stringify(options.body);
    }
    const response = await fetch(path, config);
    const text = await response.text();
    let payload = null;
    if (text) {
      try { payload = JSON.parse(text); }
      catch { throw new Error('The server returned an unreadable response.'); }
    }
    if (!response.ok) throw new Error(payload?.error || `Request failed (${response.status}).`);
    return payload;
  },
  get(path) { return this.request(path); },
  post(path, body) { return this.request(path, { method: 'POST', body }); },
  put(path, body) { return this.request(path, { method: 'PUT', body }); },
  delete(path) { return this.request(path, { method: 'DELETE' }); }
};
