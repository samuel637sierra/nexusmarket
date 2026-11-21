const catalogImages = [
  "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=720&q=80",
  "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=720&q=80",
  "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=720&q=80",
  "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=720&q=80",
  "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=720&q=80",
];

const state = {
  account: JSON.parse(sessionStorage.getItem("nexus-account") || "null"),
  auth: sessionStorage.getItem("nexus-auth"),
  register: false,
  accountType: "CUSTOMER",
  products: [],
  cart: new Map(),
  search: "",
  type: "",
};

const elements = {
  grid: document.querySelector("#product-grid"),
  count: document.querySelector("#result-count"),
  empty: document.querySelector("#empty-state"),
  error: document.querySelector("#error-state"),
  errorMessage: document.querySelector("#error-message"),
  accountDialog: document.querySelector("#account-dialog"),
  cartDialog: document.querySelector("#cart-dialog"),
  sellerDialog: document.querySelector("#seller-dialog"),
  ordersDialog: document.querySelector("#orders-dialog"),
  adminDialog: document.querySelector("#admin-dialog"),
  ordersButton: document.querySelector("#orders-button"),
  accountButton: document.querySelector("#account-button"),
  cartButton: document.querySelector("#cart-button"),
  cartCount: document.querySelector("#cart-count"),
  accountForm: document.querySelector("#account-form"),
  accountError: document.querySelector("#account-error"),
  registerFields: document.querySelector("#register-fields"),
  sellerFields: document.querySelector("#seller-fields"),
  cartItems: document.querySelector("#cart-items"),
  cartTotal: document.querySelector("#cart-total"),
  cartError: document.querySelector("#cart-error"),
  checkoutButton: document.querySelector("#checkout-button"),
  toast: document.querySelector("#toast"),
};

function formatPrice(price) {
  return new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0,
  }).format(price);
}

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (state.auth) headers.set("Authorization", state.auth);
  if (options.body) headers.set("Content-Type", "application/json");
  const response = await fetch(path, { ...options, headers });
  const contentType = response.headers.get("content-type") || "";
  const result = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    const message = result?.message || (response.status === 401
      ? "Ingresa a tu cuenta para continuar."
      : "No se pudo completar la solicitud.");
    throw new Error(message);
  }
  return result;
}

function productImage(product, index) {
  return product.type === "DIGITAL" ? catalogImages[4] : catalogImages[index % 4];
}

function renderProducts() {
  const visible = state.products.filter((product) => {
    const matchesSearch = !state.search || `${product.name} ${product.description || ""}`
      .toLocaleLowerCase("es").includes(state.search.toLocaleLowerCase("es"));
    return matchesSearch && (!state.type || product.type === state.type);
  });

  elements.count.textContent = `${visible.length} ${visible.length === 1 ? "producto" : "productos"}`;
  elements.empty.hidden = visible.length > 0;
  elements.grid.hidden = visible.length === 0;
  elements.grid.innerHTML = visible.map((product) => {
    const index = state.products.indexOf(product);
    const label = product.type === "DIGITAL" ? "Digital" : "Envío disponible";
    return `<article class="product-card">
      <div class="product-photo-wrap">
        <img class="product-photo" src="${productImage(product, index)}" alt="${escapeHtml(product.name)}" loading="lazy">
        <span class="product-badge">${label}</span>
        <button class="quick-add" type="button" data-add="${escapeHtml(product.productCode)}">Agregar al carrito</button>
      </div>
      <div class="product-info">
        <div><p class="product-name">${escapeHtml(product.name)}</p><p class="product-type">${escapeHtml(product.description || label)}</p></div>
        <strong class="product-price">${formatPrice(product.price)}</strong>
      </div>
      <div class="qty-row">
        <input class="qty-input" type="number" min="1" value="1" data-qty="${escapeHtml(product.productCode)}" aria-label="Cantidad de ${escapeHtml(product.name)}">
        <button class="small-button" type="button" data-add="${escapeHtml(product.productCode)}">Agregar</button>
      </div>
    </article>`;
  }).join("");
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  })[character]);
}

async function loadProducts() {
  elements.error.hidden = true;
  elements.empty.hidden = true;
  elements.grid.hidden = false;
  elements.grid.innerHTML = "";
  elements.count.textContent = "Cargando catálogo...";
  const params = new URLSearchParams();
  if (state.search) params.set("search", state.search);
  if (state.type) params.set("type", state.type);
  try {
    state.products = await api(`/api/products?${params}`);
    renderProducts();
  } catch (error) {
    elements.grid.hidden = true;
    elements.error.hidden = false;
    elements.errorMessage.textContent = error.message;
    elements.count.textContent = "Catálogo no disponible";
  }
}

function setAccount(account, username, password) {
  state.account = account;
  state.auth = `Basic ${btoa(`${username}:${password}`)}`;
  sessionStorage.setItem("nexus-account", JSON.stringify(account));
  sessionStorage.setItem("nexus-auth", state.auth);
  updateAccountButtons();
}

const OPERATOR_ROLES = ["ADMINISTRATOR", "SUPERVISOR"];

function updateAccountButtons() {
  const role = state.account?.role;
  elements.accountButton.textContent = !role ? "Ingresar"
    : role === "SELLER" ? "Mi tienda"
    : OPERATOR_ROLES.includes(role) ? "Operación"
    : state.account.fullName?.split(" ")[0] || "Mi cuenta";
  elements.ordersButton.hidden = role !== "CUSTOMER";
}

function clearAccount() {
  state.account = null;
  state.auth = null;
  sessionStorage.removeItem("nexus-account");
  sessionStorage.removeItem("nexus-auth");
  updateAccountButtons();
}

function showToast(message) {
  elements.toast.textContent = message;
  elements.toast.classList.add("show");
  window.setTimeout(() => elements.toast.classList.remove("show"), 2600);
}

function requireAccount() {
  if (state.account && state.auth) return true;
  elements.accountDialog.showModal();
  return false;
}

async function refreshCart() {
  if (!state.account || !state.auth || state.account.role !== "CUSTOMER") {
    state.cart.clear();
    renderCart();
    return;
  }
  try {
    const cart = await api(`/api/customers/${encodeURIComponent(state.account.personId)}/cart`);
    state.cart = new Map(cart.items.map((item) => [item.productCode, item]));
    renderCart(cart.total);
  } catch (error) {
    if (error.message.includes("Ingresa")) clearAccount();
  }
}

function renderCart(totalFromApi) {
  const items = [...state.cart.values()];
  const count = items.reduce((sum, item) => sum + item.quantity, 0);
  elements.cartCount.textContent = count;
  elements.cartItems.innerHTML = items.length ? items.map((item) => `
    <div class="cart-row">
      <div><h3>${escapeHtml(item.name)}</h3><p>${item.quantity} × ${formatPrice(item.unitPrice)}</p></div>
      <button class="cart-remove" type="button" data-remove="${escapeHtml(item.productCode)}">Quitar</button>
    </div>`).join("") : "<p class='product-type'>Tu carrito está vacío.</p>";
  const total = totalFromApi ?? items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0);
  elements.cartTotal.textContent = formatPrice(total);
  elements.checkoutButton.disabled = items.length === 0;
}

async function addToCart(productCode) {
  if (!requireAccount()) return;
  if (state.account.role !== "CUSTOMER") {
    showToast("Solo los compradores pueden agregar al carrito.");
    return;
  }
  const qtyInput = [...document.querySelectorAll("[data-qty]")].find((input) => input.dataset.qty === productCode);
  const quantity = Math.max(1, Number(qtyInput?.value) || 1);
  try {
    await api(`/api/customers/${encodeURIComponent(state.account.personId)}/cart/items`, {
      method: "POST",
      body: JSON.stringify({ productCode, quantity }),
    });
    await refreshCart();
    showToast("Agregado a tu carrito");
  } catch (error) {
    showToast(error.message);
  }
}

function setRegisterMode(register) {
  state.register = register;
  document.querySelector("#login-tab").classList.toggle("active", !register);
  document.querySelector("#register-tab").classList.toggle("active", register);
  document.querySelector("#login-tab").setAttribute("aria-selected", String(!register));
  document.querySelector("#register-tab").setAttribute("aria-selected", String(register));
  elements.registerFields.hidden = !register;
  for (const fieldName of ["fullName", "email", "phoneNumber", "address"]) {
    elements.accountForm.elements[fieldName].required = register;
  }
  elements.accountForm.elements.password.autocomplete = register ? "new-password" : "current-password";
  document.querySelector("#account-title").textContent = register ? "Tu cuenta empieza aquí" : "Qué bueno verte";
  document.querySelector("#account-subtitle").textContent = register
    ? "Crea tu cuenta para comprar y seguir tus pedidos."
    : "Ingresa para comprar y seguir tus pedidos.";
  document.querySelector("#account-submit").textContent = register ? "Crear cuenta" : "Ingresar";
  elements.accountError.hidden = true;
}

function updateAccountType(accountType) {
  state.accountType = accountType;
  const isSeller = accountType === "SELLER";
  elements.sellerFields.hidden = !isSeller;
  elements.sellerFields.querySelectorAll("input").forEach((input) => {
    input.required = isSeller;
  });
}

async function submitAccount(event) {
  event.preventDefault();
  const form = new FormData(elements.accountForm);
  const username = String(form.get("username")).trim();
  const password = String(form.get("password"));
  const submit = document.querySelector("#account-submit");
  submit.disabled = true;
  elements.accountError.hidden = true;
  try {
    if (state.register) {
      const accountType = String(form.get("accountType"));
      const seller = accountType === "SELLER";
      await api(seller ? "/api/auth/register/seller" : "/api/auth/register/customer", {
        method: "POST",
        body: JSON.stringify({
          fullName: form.get("fullName"), email: form.get("email"),
          phoneNumber: form.get("phoneNumber"), address: form.get("address"),
          businessName: seller ? form.get("businessName") : undefined,
          taxIdentificationNumber: seller ? Number(form.get("taxIdentificationNumber")) : undefined,
          username, password,
        }),
      });
    }
    const account = await api("/api/auth/login", {
      method: "POST", body: JSON.stringify({ username, password }),
    });
    setAccount(account, username, password);
    elements.accountDialog.close();
    elements.accountForm.reset();
    setRegisterMode(false);
    updateAccountType("CUSTOMER");
    await refreshCart();
    showToast(`Hola, ${account.fullName.split(" ")[0]}`);
  } catch (error) {
    elements.accountError.textContent = error.message;
    elements.accountError.hidden = false;
  } finally {
    submit.disabled = false;
  }
}

async function submitSellerProduct(event) {
  event.preventDefault();
  if (!state.account || state.account.role !== "SELLER") return;
  const formElement = event.currentTarget;
  const form = new FormData(formElement);
  const submit = formElement.querySelector("button[type='submit']");
  submit.disabled = true;
  document.querySelector("#seller-error").hidden = true;
  try {
    const product = await api("/api/products", {
      method: "POST",
      body: JSON.stringify({
        name: form.get("name"), description: form.get("description"),
        price: Number(form.get("price")), type: form.get("type"),
        sellerCode: state.account.sellerCode,
      }),
    });
    await api(`/api/products/${encodeURIComponent(product.productCode)}/publish`, { method: "POST" });
    formElement.reset();
    elements.sellerDialog.close();
    await loadProducts();
    showToast("Tu producto ya está publicado");
  } catch (error) {
    const message = document.querySelector("#seller-error");
    message.textContent = error.message;
    message.hidden = false;
  } finally {
    submit.disabled = false;
  }
}

async function removeFromCart(productCode) {
  try {
    await api(`/api/customers/${encodeURIComponent(state.account.personId)}/cart/items/${encodeURIComponent(productCode)}`, {
      method: "DELETE",
    });
    await refreshCart();
  } catch (error) {
    elements.cartError.textContent = error.message;
    elements.cartError.hidden = false;
  }
}

async function checkout() {
  if (!requireAccount()) return;
  elements.checkoutButton.disabled = true;
  elements.cartError.hidden = true;
  try {
    const order = await api(`/api/customers/${encodeURIComponent(state.account.personId)}/checkout`, {
      method: "POST",
    });
    const payment = await api(`/api/orders/${encodeURIComponent(order.orderCode)}/payment`, {
      method: "POST",
    });
    if (!payment.paid) throw new Error("El pago no fue aprobado. El pedido sigue pendiente.");
    elements.cartDialog.close();
    await refreshCart();
    showToast(`Pedido pagado: ${order.orderCode}`);
  } catch (error) {
    elements.cartError.textContent = error.message;
    elements.cartError.hidden = false;
  } finally {
    elements.checkoutButton.disabled = false;
  }
}

const STATUS_LABELS = {
  PENDING_PAYMENT: "Pendiente de pago", PAID: "Pagado", SHIPPED: "Enviado", DELIVERED: "Entregado",
  COMPLETED: "Completado", CANCELLED: "Cancelado", RETURNED: "Devuelto y reembolsado",
  PARTIALLY_RETURNED: "Devuelto parcialmente", PREPARING: "Preparando", IN_TRANSIT: "En tránsito",
  SALE: "Venta", RETURN: "Devolución", PUBLISHED: "Publicado", SUSPENDED: "Suspendido",
  PHYSICAL: "Físico", DIGITAL: "Digital",
};

function pill(status) {
  const tone = ["RETURNED", "PARTIALLY_RETURNED", "RETURN", "CANCELLED"].includes(status) ? "warn"
    : ["DELIVERED", "COMPLETED", "PAID", "SALE", "PUBLISHED"].includes(status) ? "ok" : "";
  return `<span class="pill ${tone}">${escapeHtml(STATUS_LABELS[status] || status || "—")}</span>`;
}

function table(columns, rows) {
  if (!rows.length) return "<p class='muted'>Sin registros.</p>";
  return `<div class="table-wrap"><table class="data-table"><thead><tr>${columns.map((column) =>
    `<th class="${column.num ? "num" : ""}">${escapeHtml(column.label)}</th>`).join("")}</tr></thead><tbody>${rows.map((row) =>
    `<tr>${columns.map((column) => `<td class="${column.num ? "num" : ""}">${column.render(row)}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`;
}

function stat(label, value) {
  return `<div class="stat"><span>${escapeHtml(label)}</span><strong>${value}</strong></div>`;
}

const code = (value) => `<code>${escapeHtml(value ?? "—")}</code>`;

async function loadSellerReport(sellerCode, target) {
  if (!sellerCode) {
    target.innerHTML = "";
    return;
  }
  target.innerHTML = "<p class='muted'>Cargando reporte...</p>";
  try {
    const report = await api(`/api/sellers/${encodeURIComponent(sellerCode)}/report`);
    target.innerHTML = `<p class="muted">${escapeHtml(report.businessName)} · ${code(report.sellerCode)}</p>
      <div class="stat-row">${stat("Unidades vendidas", report.unitsSold)}${stat("Unidades devueltas", report.unitsReturned)}
      ${stat("Ventas brutas", formatPrice(report.grossSales))}${stat("Reembolsado", formatPrice(report.refundedAmount))}
      ${stat("Ventas netas", formatPrice(report.netSales))}</div>
      <h3>Productos e inventario</h3>
      ${table([
        { label: "Producto", render: (row) => `${escapeHtml(row.name)}<br>${code(row.productCode)}` },
        { label: "Tipo", render: (row) => pill(row.type) },
        { label: "Estado", render: (row) => pill(row.status) },
        { label: "Vendidas", num: true, render: (row) => row.unitsSold },
        { label: "Devueltas", num: true, render: (row) => row.unitsReturned },
        { label: "Bruto", num: true, render: (row) => formatPrice(row.grossSales) },
        { label: "Reembolso", num: true, render: (row) => formatPrice(row.refundedAmount) },
        { label: "Neto", num: true, render: (row) => formatPrice(row.netSales) },
        { label: "Stock", num: true, render: (row) => row.type === "DIGITAL" ? "—" : row.stockAvailable },
      ], report.products)}
      <h3 style="margin-top:16px">Movimientos (compras, devoluciones y reembolsos)</h3>
      ${table([
        { label: "Fecha", render: (row) => escapeHtml(row.date) },
        { label: "Tipo", render: (row) => pill(row.type) },
        { label: "Pedido", render: (row) => code(row.orderCode) },
        { label: "Producto", render: (row) => escapeHtml(row.productName) },
        { label: "Unid.", num: true, render: (row) => row.quantity },
        { label: "Monto", num: true, render: (row) => formatPrice(row.amount) },
        { label: "Efecto stock", num: true, render: (row) => row.stockChange > 0 ? `+${row.stockChange}` : row.stockChange },
      ], report.movements)}`;
  } catch (error) {
    target.innerHTML = `<p class="form-error">${escapeHtml(error.message)}</p>`;
  }
}

function openOrders() {
  elements.ordersDialog.showModal();
  loadOrders();
}

async function loadOrders() {
  const target = document.querySelector("#orders-list");
  try {
    const orders = await api(`/api/customers/${encodeURIComponent(state.account.personId)}/orders`);
    target.innerHTML = orders.length ? orders.map((order) => `<article class="order-card">
      <header><span>${code(order.orderCode)} · ${escapeHtml(order.orderDate)}</span>
        <span>${pill(order.status)} Total ${formatPrice(order.total)}${order.refundedAmount ? ` · Reembolsado ${formatPrice(order.refundedAmount)}` : ""}</span></header>
      ${table([
        { label: "Producto", render: (line) => escapeHtml(line.name) },
        { label: "Tipo", render: (line) => pill(line.type) },
        { label: "Vendedor", render: (line) => code(line.sellerCode) },
        { label: "Cant.", num: true, render: (line) => line.quantity },
        { label: "Devueltas", num: true, render: (line) => line.returnedQuantity },
        { label: "Estado", render: (line) => pill(line.status) },
        { label: "", render: (line) => line.type === "PHYSICAL" && ["DELIVERED", "COMPLETED"].includes(order.status)
          && line.returnedQuantity < line.quantity
          ? `<button class="small-button" type="button" data-order="${escapeHtml(order.orderCode)}" data-return="${escapeHtml(line.productCode)}">Devolver 1</button>` : "" },
      ], order.lines)}
    </article>`).join("") : "<p class='muted'>Aún no tienes pedidos.</p>";
  } catch (error) {
    target.innerHTML = `<p class="form-error">${escapeHtml(error.message)}</p>`;
  }
}

async function returnItem(button, orderCode, productCode) {
  const error = document.querySelector("#orders-error");
  error.hidden = true;
  button.disabled = true;
  try {
    const result = await api(`/api/orders/${encodeURIComponent(orderCode)}/returns`, {
      method: "POST", body: JSON.stringify({ productCode, quantity: 1 }),
    });
    showToast(`Devolución ${result.return.returnCode} reembolsada: ${formatPrice(result.return.amount)}`);
    await loadOrders();
  } catch (failure) {
    error.textContent = failure.message;
    error.hidden = false;
    button.disabled = false;
  }
}

function adminError(message) {
  const error = document.querySelector("#admin-error");
  error.textContent = message || "";
  error.hidden = !message;
}

async function openAdmin() {
  elements.adminDialog.showModal();
  adminError();
  try {
    const [products, sellers] = await Promise.all([api("/api/products"), api("/api/admin/sellers")]);
    document.querySelector("#stock-product").innerHTML = products.filter((product) => product.type === "PHYSICAL")
      .map((product) => `<option value="${escapeHtml(product.productCode)}">${escapeHtml(product.name)}</option>`).join("");
    document.querySelector("#admin-seller").innerHTML = `<option value="">Elige un vendedor</option>${sellers.map((seller) =>
      `<option value="${escapeHtml(seller.sellerCode)}">${escapeHtml(seller.businessName)}</option>`).join("")}`;
  } catch (error) {
    adminError(error.message);
  }
  loadAdminOrders();
  loadGlobalReport();
}

async function adminSubmit(event, path, body, message) {
  event.preventDefault();
  adminError();
  const form = event.currentTarget;
  try {
    await api(path, { method: "POST", body: JSON.stringify(body(new FormData(form))) });
    showToast(message);
    loadGlobalReport();
  } catch (error) {
    adminError(error.message);
  }
}

async function loadAdminOrders() {
  const target = document.querySelector("#admin-orders");
  try {
    const rows = await api("/api/admin/orders");
    target.innerHTML = table([
      { label: "Pedido", render: (row) => `${code(row.order.orderCode)}<br><span class="muted">${escapeHtml(row.order.orderDate)}</span>` },
      { label: "Estado", render: (row) => pill(row.order.status) },
      { label: "Productos", render: (row) => row.order.lines.map((line) =>
        `${escapeHtml(line.name)} ×${line.quantity} ${line.returnedQuantity ? pill(line.status) : ""}`).join("<br>") },
      { label: "Total", num: true, render: (row) => formatPrice(row.order.total) },
      { label: "Envío", render: (row) => row.shipment ? `${code(row.shipment.shipmentCode)} ${pill(row.shipment.status)}` : "—" },
      { label: "", render: (row) => {
        if (!row.shipment && row.order.status === "PAID" && row.order.requiresShipping) {
          return `<button class="small-button" data-ship-action="create" data-code="${escapeHtml(row.order.orderCode)}">Crear envío</button>`;
        }
        if (row.shipment?.status === "PREPARING") {
          return `<button class="small-button" data-ship-action="dispatch" data-code="${escapeHtml(row.shipment.shipmentCode)}">Despachar</button>`;
        }
        if (row.shipment?.status === "IN_TRANSIT") {
          return `<button class="small-button" data-ship-action="deliver" data-code="${escapeHtml(row.shipment.shipmentCode)}">Entregar</button>`;
        }
        return "";
      } },
    ], rows);
  } catch (error) {
    target.innerHTML = `<p class="form-error">${escapeHtml(error.message)}</p>`;
  }
}

async function shipmentAction(button, action, value) {
  adminError();
  button.disabled = true;
  try {
    if (action === "create") {
      await api("/api/shipments", { method: "POST", body: JSON.stringify({
        orderCode: value, warehouseCode: document.querySelector("#ship-warehouse").value.trim() }) });
    } else {
      await api(`/api/shipments/${encodeURIComponent(value)}/${action}`, { method: "POST" });
    }
    showToast("Envío actualizado");
    await loadAdminOrders();
    loadGlobalReport();
  } catch (error) {
    adminError(error.message);
    button.disabled = false;
  }
}

async function loadGlobalReport() {
  const type = document.querySelector("#global-report").value;
  const target = document.querySelector("#global-report-view");
  try {
    const response = await fetch(`/api/reports/${type}`, { headers: { Authorization: state.auth } });
    if (!response.ok) throw new Error("No tienes permiso para ver este reporte.");
    const [header, ...lines] = (await response.text()).trim().split("\n").map((line) => line.split(","));
    target.innerHTML = table(header.map((label, index) => ({ label, render: (row) => escapeHtml(row[index] ?? "") })),
      lines.filter((line) => line.length > 1));
  } catch (error) {
    target.innerHTML = `<p class="form-error">${escapeHtml(error.message)}</p>`;
  }
}

document.querySelector("#search-form").addEventListener("submit", (event) => {
  event.preventDefault();
  state.search = document.querySelector("#search-input").value.trim();
  loadProducts();
  document.querySelector("#catalog").scrollIntoView({ behavior: "smooth" });
});
document.querySelector("#type-filter").addEventListener("change", (event) => {
  state.type = event.target.value;
  renderProducts();
});
elements.grid.addEventListener("click", (event) => {
  const button = event.target.closest("[data-add]");
  if (button) addToCart(button.dataset.add);
});
elements.cartItems.addEventListener("click", (event) => {
  const button = event.target.closest("[data-remove]");
  if (button) removeFromCart(button.dataset.remove);
});
elements.accountButton.addEventListener("click", () => {
  if (state.account) {
    if (state.account.role === "SELLER") {
      elements.sellerDialog.showModal();
      loadSellerReport(state.account.sellerCode, document.querySelector("#seller-report"));
      return;
    }
    if (OPERATOR_ROLES.includes(state.account.role)) {
      openAdmin();
      return;
    }
    openOrders();
  } else {
    elements.accountDialog.showModal();
  }
});
elements.cartButton.addEventListener("click", async () => {
  elements.cartError.hidden = true;
  if (state.account) await refreshCart();
  elements.cartDialog.showModal();
});
elements.checkoutButton.addEventListener("click", checkout);
elements.accountForm.addEventListener("submit", submitAccount);
document.querySelector("#seller-form").addEventListener("submit", submitSellerProduct);
for (const [button, dialog] of [["#seller-logout", elements.sellerDialog],
  ["#customer-logout", elements.ordersDialog], ["#admin-logout", elements.adminDialog]]) {
  document.querySelector(button).addEventListener("click", () => {
    dialog.close();
    clearAccount();
    state.cart.clear();
    renderCart();
    showToast("Sesión cerrada");
  });
}
elements.ordersButton.addEventListener("click", openOrders);
document.querySelector("#orders-list").addEventListener("click", (event) => {
  const button = event.target.closest("[data-return]");
  if (button) returnItem(button, button.dataset.order, button.dataset.return);
});
document.querySelector("#seller-report-refresh").addEventListener("click",
  () => loadSellerReport(state.account.sellerCode, document.querySelector("#seller-report")));
document.querySelector("#admin-orders-refresh").addEventListener("click", loadAdminOrders);
document.querySelector("#admin-orders").addEventListener("click", (event) => {
  const button = event.target.closest("[data-ship-action]");
  if (button) shipmentAction(button, button.dataset.shipAction, button.dataset.code);
});
document.querySelector("#admin-seller").addEventListener("change", (event) =>
  loadSellerReport(event.target.value, document.querySelector("#admin-seller-report")));
document.querySelector("#global-report").addEventListener("change", loadGlobalReport);
document.querySelector("#warehouse-form").addEventListener("submit", (event) => adminSubmit(event, "/api/warehouses",
  (form) => ({ warehouseCode: form.get("warehouseCode"), name: form.get("name"), address: form.get("address"),
    capacity: Number(form.get("capacity")) }), "Bodega creada"));
document.querySelector("#stock-form").addEventListener("submit", (event) => adminSubmit(event, "/api/inventory/stock",
  (form) => ({ productCode: form.get("productCode"), warehouseCode: form.get("warehouseCode"),
    quantity: Number(form.get("quantity")) }), "Stock agregado"));
document.querySelector("#login-tab").addEventListener("click", () => setRegisterMode(false));
document.querySelector("#register-tab").addEventListener("click", () => setRegisterMode(true));
document.querySelector("#account-type").addEventListener("change", (event) => updateAccountType(event.target.value));
document.querySelector("#retry-button").addEventListener("click", loadProducts);

updateAccountButtons();
updateAccountType("CUSTOMER");
loadProducts();
refreshCart();