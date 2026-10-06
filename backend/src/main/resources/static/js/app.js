/* Shared auth + API helpers for all Ledgerly pages */

function getToken() {
  return localStorage.getItem("ledgerly_token");
}

function getUser() {
  const raw = localStorage.getItem("ledgerly_user");
  return raw ? JSON.parse(raw) : null;
}

function setSession(token, user) {
  localStorage.setItem("ledgerly_token", token);
  localStorage.setItem("ledgerly_user", JSON.stringify(user));
}

function clearSession() {
  localStorage.removeItem("ledgerly_token");
  localStorage.removeItem("ledgerly_user");
}

function isLoggedIn() {
  return Boolean(getToken() && getUser());
}

function requireAuth() {
  if (!isLoggedIn()) {
    window.location.href = "login.html";
    return false;
  }
  return true;
}

function authHeaders() {
  return {
    "Content-Type": "application/json",
    Authorization: "Bearer " + getToken(),
  };
}

async function api(path, options = {}) {
  const res = await fetch(path, options);
  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }
  return { status: res.status, ok: res.ok, data };
}

function initials(name) {
  if (!name) return "?";
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join("");
}

function escapeHtml(str) {
  return String(str)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function formatCreatedAt(value) {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  return d.toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function showError(el, message) {
  if (!el) return;
  el.className = "form-error";
  el.textContent = message;
  el.style.display = "block";
}

function showSuccess(el, message) {
  if (!el) return;
  el.className = "form-success";
  el.textContent = message;
  el.style.display = "block";
}

function hideMessage(el) {
  if (!el) return;
  el.style.display = "none";
  el.textContent = "";
}

/* Update navbar based on login state. Call on every page load. */
function updateNav() {
  const user = getUser();
  const authed = isLoggedIn();

  const guestLinks = document.getElementById("nav-guest");
  const authedLinks = document.getElementById("nav-authed");
  const navName = document.getElementById("nav-user-name");
  const navAvatar = document.getElementById("nav-avatar");

  if (guestLinks) guestLinks.style.display = authed ? "none" : "flex";
  if (authedLinks) authedLinks.style.display = authed ? "flex" : "none";

  if (authed && user) {
    if (navName) navName.textContent = user.name || "User";
    if (navAvatar) navAvatar.textContent = initials(user.name);
  }
}

async function logout() {
  try {
    await api("/users/logout", { method: "POST", headers: authHeaders() });
  } catch {
    /* ignore network errors on logout */
  }
  clearSession();
  window.location.href = "index.html";
}

/* Boot helpers used by pages */
document.addEventListener("DOMContentLoaded", () => {
  updateNav();

  const logoutBtn = document.getElementById("btn-logout");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", (e) => {
      e.preventDefault();
      logout();
    });
  }
});
