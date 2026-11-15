const state = { products: [], activeView: 'dashboard' };
const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

function money(value) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value || 0));
}
function number(value) { return new Intl.NumberFormat('en-IN').format(Number(value || 0)); }
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[character]));
}
function dateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? escapeHtml(value) : date.toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}
function toast(message, type = 'success') {
  const element = document.createElement('div');
  element.className = `toast ${type}`;
  element.textContent = message;
  $('#toast-region').append(element);
  setTimeout(() => element.remove(), 3600);
}
function showError(error) { toast(error.message || 'Something went wrong.', 'error'); }

function navigate(view) {
  if (!['dashboard', 'products', 'purchases', 'sales', 'reports'].includes(view)) return;
  state.activeView = view;
  $$('.view').forEach(section => section.classList.toggle('active', section.id === `view-${view}`));
  $$('.nav-link').forEach(link => link.classList.toggle('active', link.dataset.view === view));
  const titles = { dashboard: 'Overview', products: 'Products', purchases: 'Purchases', sales: 'Sales', reports: 'Reports' };
  $('#breadcrumb-current').textContent = titles[view];
  $('#sidebar').classList.remove('open');
  if (view === 'dashboard') loadDashboard();
  if (view === 'products') loadProducts();
  if (view === 'purchases') { loadProducts().then(fillProductSelects); loadPurchases(); }
  if (view === 'sales') { loadProducts().then(fillProductSelects); loadSales(); }
  if (view === 'reports') loadReports();
}

async function checkServer() {
  try {
    await API.get('/api/health');
    $('#server-status').textContent = 'System online';
    $('#server-status').previousElementSibling.style.background = '#37c99d';
  } catch {
    $('#server-status').textContent = 'Server unavailable';
    $('#server-status').previousElementSibling.style.background = '#df6268';
  }
}

async function loadDashboard() {
  try {
    const data = await API.get('/api/dashboard');
    $('#stat-products').textContent = number(data.productCount);
    $('#stat-units').textContent = number(data.totalUnits);
    $('#stat-stock-value').textContent = money(data.stockValue);
    $('#stat-low-stock').textContent = number(data.lowStockCount);
    $('#nav-product-count').textContent = number(data.productCount);
    renderRecentSales(data.recentSales || []);
    renderStockWatch(data.lowStockProducts || []);
  } catch (error) { showError(error); }
}

function renderRecentSales(sales) {
  if (!sales.length) {
    $('#recent-sales').innerHTML = '<div class="empty-state"><strong>No sales recorded yet</strong><span>Your completed transactions will appear here.</span></div>';
    return;
  }
  $('#recent-sales').innerHTML = `<table class="data-table"><thead><tr><th>Invoice</th><th>Date</th><th>Items</th><th>Total</th></tr></thead><tbody>${sales.map(sale => `<tr><td><strong>${escapeHtml(sale.invoiceNumber)}</strong></td><td>${dateTime(sale.saleDate)}</td><td>${number((sale.items || []).reduce((sum, item) => sum + item.quantity, 0))} units</td><td class="amount">${money(sale.totalAmount)}</td></tr>`).join('')}</tbody></table>`;
}
function renderStockWatch(products) {
  if (!products.length) {
    $('#dashboard-low-stock').innerHTML = '<div class="empty-state"><strong>Stock looks healthy</strong><span>No products currently need attention.</span></div>';
    return;
  }
  $('#dashboard-low-stock').innerHTML = products.map(product => `<div class="stock-watch-item"><div class="product-avatar">${escapeHtml(product.name.slice(0, 1).toUpperCase())}</div><div class="stock-watch-info"><strong>${escapeHtml(product.name)}</strong><small>${escapeHtml(product.sku)}</small></div><div class="stock-qty"><strong>${number(product.currentStock)} left</strong><small>Reorder at ${number(product.reorderLevel)}</small></div></div>`).join('');
}

async function loadProducts(search = '') {
  try {
    const path = search ? `/api/products?search=${encodeURIComponent(search)}` : '/api/products';
    state.products = await API.get(path);
    $('#nav-product-count').textContent = number(state.products.length);
    renderProductsTable(state.products);
    fillProductSelects();
    return state.products;
  } catch (error) { showError(error); throw error; }
}
function stockBadge(product) {
  if (product.currentStock === 0) return '<span class="status-pill out">Out of stock</span>';
  if (product.currentStock <= product.reorderLevel) return '<span class="status-pill low">Low stock</span>';
  return '<span class="status-pill good">In stock</span>';
}
function renderProductsTable(products) {
  $('#product-table-caption').textContent = `${number(products.length)} active product${products.length === 1 ? '' : 's'}`;
  if (!products.length) {
    $('#products-table').innerHTML = '<div class="empty-state"><strong>No products found</strong><span>Add a product or try another search term.</span></div>';
    return;
  }
  $('#products-table').innerHTML = `<table class="data-table"><thead><tr><th>Product</th><th>Category</th><th>Selling price</th><th>Avg. cost</th><th>Stock</th><th>Status</th><th>Actions</th></tr></thead><tbody>${products.map(p => `<tr><td><strong>${escapeHtml(p.name)}</strong><div class="sku-text">${escapeHtml(p.sku)}</div></td><td>${escapeHtml(p.category)}</td><td class="amount">${money(p.sellingPrice)}</td><td>${money(p.averageCost)}</td><td><strong>${number(p.currentStock)}</strong><div class="sku-text">Reorder at ${number(p.reorderLevel)}</div></td><td>${stockBadge(p)}</td><td><div class="actions-cell"><button class="table-action" data-edit-product="${p.id}">Edit</button><button class="table-action danger" data-delete-product="${p.id}">Remove</button></div></td></tr>`).join('')}</tbody></table>`;
}

function fillProductSelects() {
  const options = state.products.map(p => `<option value="${p.id}" data-price="${p.sellingPrice}" data-stock="${p.currentStock}">${escapeHtml(p.name)} · ${escapeHtml(p.sku)} (${number(p.currentStock)} available)</option>`).join('');
  ['#purchase-product', '#sale-product'].forEach(selector => {
    const select = $(selector);
    if (!select) return;
    const previous = select.value;
    select.innerHTML = `<option value="">Choose a product</option>${options}`;
    if (previous) select.value = previous;
  });
  updateTransactionEstimate('purchase');
  updateTransactionEstimate('sale');
}

function openProductModal(product = null) {
  const form = $('#product-form');
  form.reset();
  form.elements.id.value = product?.id || '';
  form.elements.sku.value = product?.sku || '';
  form.elements.name.value = product?.name || '';
  form.elements.category.value = product?.category || '';
  form.elements.sellingPrice.value = product?.sellingPrice ?? '';
  form.elements.averageCost.value = product?.averageCost ?? '';
  form.elements.reorderLevel.value = product?.reorderLevel ?? 5;
  form.elements.currentStock.value = product?.currentStock ?? 0;
  const editing = Boolean(product);
  $('#product-modal-title').textContent = editing ? 'Edit product' : 'Add a product';
  $('#save-product-btn').textContent = editing ? 'Save changes' : 'Save product';
  $('#opening-stock-label').style.display = editing ? 'none' : 'grid';
  $('#product-edit-hint').textContent = editing ? 'To change available quantity, record a purchase or sale.' : 'Set the initial quantity currently available in your store.';
  $('#product-modal').classList.add('open');
  $('#product-modal').setAttribute('aria-hidden', 'false');
  form.elements.sku.focus();
}
function closeProductModal() {
  $('#product-modal').classList.remove('open');
  $('#product-modal').setAttribute('aria-hidden', 'true');
}

async function loadPurchases() {
  try {
    const purchases = await API.get('/api/purchases');
    if (!purchases.length) { $('#purchases-table').innerHTML = '<div class="empty-state"><strong>No purchase receipts yet</strong><span>Record an incoming purchase to increase stock.</span></div>'; return; }
    $('#purchases-table').innerHTML = `<table class="data-table"><thead><tr><th>Receipt</th><th>Supplier</th><th>Date</th><th>Items</th><th>Total</th></tr></thead><tbody>${purchases.map(p => `<tr><td><strong>${escapeHtml(p.receiptNumber)}</strong></td><td>${escapeHtml(p.supplierName)}</td><td>${dateTime(p.purchaseDate)}</td><td>${(p.items || []).length}</td><td class="amount">${money(p.totalAmount)}</td></tr>`).join('')}</tbody></table>`;
  } catch (error) { showError(error); }
}
async function loadSales() {
  try {
    const sales = await API.get('/api/sales');
    if (!sales.length) { $('#sales-table').innerHTML = '<div class="empty-state"><strong>No sales recorded yet</strong><span>Completed sales will appear here.</span></div>'; return; }
    $('#sales-table').innerHTML = `<table class="data-table"><thead><tr><th>Invoice</th><th>Date</th><th>Items</th><th>Total</th></tr></thead><tbody>${sales.map(s => `<tr><td><strong>${escapeHtml(s.invoiceNumber)}</strong></td><td>${dateTime(s.saleDate)}</td><td>${(s.items || []).reduce((n, item) => n + item.quantity, 0)} units</td><td class="amount">${money(s.totalAmount)}</td></tr>`).join('')}</tbody></table>`;
  } catch (error) { showError(error); }
}
function updateTransactionEstimate(type) {
  const form = $(`#${type}-form`);
  if (!form) return;
  const select = form.elements.productId;
  const selected = select?.selectedOptions?.[0];
  const quantity = Math.max(0, Number(form.elements.quantity?.value || 0));
  if (type === 'purchase') {
    const cost = Math.max(0, Number(form.elements.unitCost?.value || 0));
    $('#purchase-estimate').textContent = money(quantity * cost);
  } else {
    const price = Number(selected?.dataset.price || 0);
    const stock = Number(selected?.dataset.stock || 0);
    $('#sale-estimate').textContent = money(quantity * price);
    $('#sale-availability').innerHTML = selected?.value
      ? `<span class="availability-dot" style="background:${stock > 0 ? '#1fa987' : '#df6268'}"></span>${number(stock)} units currently available.`
      : '<span class="availability-dot"></span>Select a product to see availability.';
    form.elements.quantity.max = stock || '';
  }
}

async function loadReports() {
  try {
    const [valuation, fastMoving, sales] = await Promise.all([
      API.get('/api/reports/valuation'), API.get('/api/reports/fast-moving'), API.get('/api/reports/sales')
    ]);
    $('#valuation-table').innerHTML = valuation.length ? `<table class="data-table"><thead><tr><th>Category</th><th>Products</th><th>Units</th><th>Stock value</th><th>Retail value</th></tr></thead><tbody>${valuation.map(row => `<tr><td><strong>${escapeHtml(row.category)}</strong></td><td>${number(row.productCount)}</td><td>${number(row.units)}</td><td class="amount">${money(row.stockValue)}</td><td>${money(row.retailValue)}</td></tr>`).join('')}</tbody></table>` : '<div class="empty-state">No valuation data yet.</div>';
    $('#fast-moving-table').innerHTML = fastMoving.length ? `<table class="data-table"><thead><tr><th>#</th><th>Product</th><th>Sold</th><th>Remaining</th></tr></thead><tbody>${fastMoving.slice(0, 8).map((row, index) => `<tr><td><span class="report-rank">${index + 1}</span></td><td><strong>${escapeHtml(row.name)}</strong><div class="sku-text">${escapeHtml(row.sku)}</div></td><td><strong>${number(row.unitsSold)}</strong></td><td>${number(row.remainingStock)}</td></tr>`).join('')}</tbody></table>` : '<div class="empty-state"><strong>No sales data yet</strong><span>Products will rank here after sales are recorded.</span></div>';
    $('#sales-summary').innerHTML = `<div class="summary-block"><span>Total sales</span><strong>${money(sales.totalSales)}</strong></div><div class="summary-block"><span>Transactions</span><strong>${number(sales.transactionCount)}</strong></div><div class="summary-block"><span>Average sale</span><strong>${money(sales.averageSale)}</strong></div>`;
  } catch (error) { showError(error); }
}

function bindEvents() {
  $$('.nav-link').forEach(button => button.addEventListener('click', () => navigate(button.dataset.view)));
  $$('[data-go]').forEach(button => button.addEventListener('click', () => navigate(button.dataset.go)));
  $('#mobile-menu').addEventListener('click', () => $('#sidebar').classList.toggle('open'));
  $('#add-product-btn').addEventListener('click', () => openProductModal());
  $('#close-product-modal').addEventListener('click', closeProductModal);
  $('#cancel-product-modal').addEventListener('click', closeProductModal);
  $('#product-modal').addEventListener('click', event => { if (event.target === $('#product-modal')) closeProductModal(); });
  $('#refresh-products').addEventListener('click', () => loadProducts($('#product-search').value));
  $('#refresh-reports').addEventListener('click', loadReports);
  let searchTimer;
  $('#product-search').addEventListener('input', event => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => loadProducts(event.target.value), 220);
  });
  $('#products-table').addEventListener('click', async event => {
    const edit = event.target.closest('[data-edit-product]');
    const remove = event.target.closest('[data-delete-product]');
    if (edit) {
      const product = state.products.find(p => p.id === Number(edit.dataset.editProduct));
      if (product) openProductModal(product);
    }
    if (remove) {
      const id = Number(remove.dataset.deleteProduct);
      const product = state.products.find(p => p.id === id);
      if (!product || !confirm(`Remove ${product.name} from the active catalogue? Historical transactions will remain.`)) return;
      try { await API.delete(`/api/products/${id}`); toast('Product removed from the active catalogue.'); await loadProducts($('#product-search').value); await loadDashboard(); }
      catch (error) { showError(error); }
    }
  });
  $('#product-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const id = form.elements.id.value;
    const body = {
      sku: form.elements.sku.value.trim(), name: form.elements.name.value.trim(), category: form.elements.category.value.trim(),
      sellingPrice: Number(form.elements.sellingPrice.value), averageCost: Number(form.elements.averageCost.value || 0),
      reorderLevel: Number(form.elements.reorderLevel.value)
    };
    if (!id) body.currentStock = Number(form.elements.currentStock.value || 0);
    try {
      if (id) await API.put(`/api/products/${id}`, body); else await API.post('/api/products', body);
      closeProductModal(); toast(id ? 'Product details updated.' : 'Product added successfully.');
      await loadProducts($('#product-search').value); await loadDashboard();
    } catch (error) { showError(error); }
  });
  ['productId', 'quantity', 'unitCost'].forEach(name => {
    $('#purchase-form').elements[name].addEventListener('input', () => updateTransactionEstimate('purchase'));
    $('#purchase-form').elements[name].addEventListener('change', () => updateTransactionEstimate('purchase'));
  });
  ['productId', 'quantity'].forEach(name => {
    $('#sale-form').elements[name].addEventListener('input', () => updateTransactionEstimate('sale'));
    $('#sale-form').elements[name].addEventListener('change', () => updateTransactionEstimate('sale'));
  });
  $('#purchase-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const body = { supplierName: form.elements.supplierName.value.trim(), items: [{ productId: Number(form.elements.productId.value), quantity: Number(form.elements.quantity.value), unitCost: Number(form.elements.unitCost.value) }] };
    try {
      const result = await API.post('/api/purchases', body);
      toast(`Purchase ${result.receiptNumber} recorded.`); form.reset();
      await loadProducts(); await loadPurchases(); await loadDashboard(); updateTransactionEstimate('purchase');
    } catch (error) { showError(error); }
  });
  $('#sale-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const body = { items: [{ productId: Number(form.elements.productId.value), quantity: Number(form.elements.quantity.value) }] };
    try {
      const result = await API.post('/api/sales', body);
      toast(`Sale ${result.invoiceNumber} completed.`); form.reset();
      await loadProducts(); await loadSales(); await loadDashboard(); updateTransactionEstimate('sale');
    } catch (error) { showError(error); }
  });
  document.addEventListener('keydown', event => { if (event.key === 'Escape') closeProductModal(); });
}

document.addEventListener('DOMContentLoaded', async () => {
  $('#today-date').textContent = new Date().toLocaleDateString('en-IN', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' });
  bindEvents();
  await checkServer();
  await loadProducts();
  await loadDashboard();
});
