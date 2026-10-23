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
  elements.accountButton.textContent = account.role === "SELLER"
    ? "Mi tienda" : account.fullName?.split(" ")[0] || "Mi cuenta";
}

function clearAccount() {
  state.account = null;
  state.auth = null;
  sessionStorage.removeItem("nexus-account");
  sessionStorage.removeItem("nexus-auth");
  elements.accountButton.textContent = "Ingresar";
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
  try {
    await api(`/api/customers/${encodeURIComponent(state.account.personId)}/cart/items`, {
      method: "POST",
      body: JSON.stringify({ productCode, quantity: 1 }),
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
    showToast(`Pedido confirmado: ${order.orderCode}`);
  } catch (error) {
    elements.cartError.textContent = error.message;
    elements.cartError.hidden = false;
  } finally {
    elements.checkoutButton.disabled = false;
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
      return;
    }
    clearAccount();
    state.cart.clear();
    renderCart();
    showToast("Sesión cerrada");
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
document.querySelector("#seller-logout").addEventListener("click", () => {
  elements.sellerDialog.close();
  clearAccount();
  showToast("Sesión cerrada");
});
document.querySelector("#login-tab").addEventListener("click", () => setRegisterMode(false));
document.querySelector("#register-tab").addEventListener("click", () => setRegisterMode(true));
document.querySelector("#account-type").addEventListener("change", (event) => updateAccountType(event.target.value));
document.querySelector("#retry-button").addEventListener("click", loadProducts);

if (state.account && state.auth) elements.accountButton.textContent = state.account.fullName?.split(" ")[0] || "Mi cuenta";
if (state.account?.role === "SELLER") elements.accountButton.textContent = "Mi tienda";
updateAccountType("CUSTOMER");
loadProducts();
refreshCart();