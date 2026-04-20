const user = JSON.parse(sessionStorage.getItem("currentUser")) || { id: 1 }; // Fallback id đề phòng chưa đăng nhập
let autoEnabled = true;
let timer = null;

function $(id) {
  return document.getElementById(id);
}

function setStatus(state, text) {
  const dot = $("status-dot");
  const label = $("status-text");
  if (!dot || !label) return;

  const colors = {
    idle: "rgba(160, 179, 209, 0.55)",
    loading: "rgba(255, 204, 0, 0.85)",
    ok: "rgba(0, 245, 255, 0.9)",
    error: "rgba(255, 107, 107, 0.9)",
  };

  dot.style.background = colors[state] || colors.idle;
  label.innerText = text || "";
}

function showError(message) {
  const box = $("error-box");
  const text = $("error-text");
  if (!box || !text) return;
  if (message) {
    box.classList.remove("hidden");
    text.innerText = message;
  } else {
    box.classList.add("hidden");
    text.innerText = "";
  }
}

function getApiBase() {
  const base = $("api-base")?.value?.trim() || "http://localhost:8080";
  return base.replace(/\/+$/, "");
}

async function fetchJson(url) {
  const res = await fetch(url, { method: "GET" });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} ${res.statusText}${body ? ` - ${body}` : ""}`);
  }
  return res.json();
}

function renderOffline(data) {
  const tbody = $("offline-tbody");
  const count = $("offline-count");
  if (!tbody || !count) return;

  const list = Array.isArray(data?.offline_devices) ? data.offline_devices : [];
  count.innerText = String(list.length);

  if (list.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td class="px-4 py-4 text-slate-500" colspan="3">Không có thiết bị offline.</td>
      </tr>
    `;
    return;
  }

  const rows = list
    .sort((a, b) => (Number(b.minutes_since_last_seen) || 0) - (Number(a.minutes_since_last_seen) || 0))
    .map((x) => {
      const deviceId = x.device_id ?? "--";
      const lastSeen = x.last_seen ?? "--";
      const mins = x.minutes_since_last_seen ?? "--";
      return `
        <tr class="hover:bg-white/5">
          <td class="px-4 py-3 font-semibold text-slate-100">${deviceId}</td>
          <td class="px-4 py-3 text-slate-300">${String(lastSeen)}</td>
          <td class="px-4 py-3 text-rose-200 font-semibold">${String(mins)}</td>
        </tr>
      `;
    })
    .join("");

  tbody.innerHTML = rows;
}

function badgeForType(type) {
  const t = String(type || "").toUpperCase();
  const base = "inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-bold tracking-widest border";
  if (t === "OFFLINE") return `<span class="${base} border-rose-500/30 bg-rose-500/10 text-rose-200">OFFLINE</span>`;
  if (t === "POWER_OVER") return `<span class="${base} border-amber-500/30 bg-amber-500/10 text-amber-200">POWER</span>`;
  if (t === "CURRENT_OVER") return `<span class="${base} border-indigo-500/30 bg-indigo-500/10 text-indigo-200">CURRENT</span>`;
  return `<span class="${base} border-white/10 bg-white/5 text-slate-200">${t || "--"}</span>`;
}

function renderAlerts(list) {
  const tbody = $("alerts-tbody");
  const count = $("alerts-count");
  if (!tbody || !count) return;

  const arr = Array.isArray(list) ? list : [];
  count.innerText = String(arr.length);

  if (arr.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td class="px-4 py-4 text-slate-500" colspan="5">Không có alert.</td>
      </tr>
    `;
    return;
  }

  const rows = arr
    .map((a) => {
      const id = a.id ?? "--";
      const device = a.device_id ?? "--";
      const type = a.type ?? "--";
      const msg = a.message ?? "";
      const isRead = !!a.is_read;
      return `
        <tr class="hover:bg-white/5 ${isRead ? "opacity-70" : ""}">
          <td class="px-4 py-3 text-slate-300">${id}</td>
          <td class="px-4 py-3 font-semibold text-slate-100">${device}</td>
          <td class="px-4 py-3">${badgeForType(type)}</td>
          <td class="px-4 py-3 text-slate-200">${String(msg)}</td>
          <td class="px-4 py-3 ${isRead ? "text-slate-400" : "text-emerald-200 font-semibold"}">
            ${isRead ? "YES" : "NO"}
          </td>
        </tr>
      `;
    })
    .join("");

  tbody.innerHTML = rows;
}

function renderOverLimit(data) {
  const tbody = $("over-tbody");
  const count = $("over-count");
  if (!tbody || !count) return;

  const list = Array.isArray(data?.over_limit_devices) ? data.over_limit_devices : [];
  count.innerText = String(list.length);

  if (list.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td class="px-4 py-4 text-slate-500" colspan="5">Không có thiết bị vượt ngưỡng.</td>
      </tr>
    `;
    return;
  }

  const rows = list.map((x) => {
    const deviceId = x.device_id ?? "--";
    const latest = x.latest || {};
    const lim = x.limit || {};
    const power = latest.power ?? "--";
    const maxPower = lim.max_power ?? "--";
    const current = latest.current ?? "--";
    const maxCurrent = lim.max_current ?? "--";

    const powerCls = x.power_over ? "text-amber-200 font-semibold" : "text-slate-200";
    const curCls = x.current_over ? "text-indigo-200 font-semibold" : "text-slate-200";

    return `
      <tr class="hover:bg-white/5">
        <td class="px-4 py-3 font-semibold text-slate-100">${deviceId}</td>
        <td class="px-4 py-3 ${powerCls}">${String(power)}</td>
        <td class="px-4 py-3 text-slate-300">${String(maxPower)}</td>
        <td class="px-4 py-3 ${curCls}">${String(current)}</td>
        <td class="px-4 py-3 text-slate-300">${String(maxCurrent)}</td>
      </tr>
    `;
  }).join("");

  tbody.innerHTML = rows;
}

async function runChecks() {
  showError("");
  setStatus("loading", "Đang kiểm tra...");

  const user = JSON.parse(sessionStorage.getItem("currentUser"));
  if (!user || !user.id) {
    setStatus("error", "Chưa đăng nhập");
    showError("Vui lòng đăng nhập để xem alerts của bạn");
    return;
  }

  const base = getApiBase();
  const seconds = Number($("offline-seconds")?.value || 4) || 4;
  const limit = Number($("alerts-limit")?.value || 50) || 50;

  const offlineUrl = `${base}/api/daocuong/devices/offline/check?seconds=${encodeURIComponent(seconds)}&userId=${user.id}`;
  const thresholdUrl = `${base}/api/daocuong/threshold/check?userId=${user.id}`;
  const alertsUrl = `${base}/api/daocuong/alerts?limit=${encodeURIComponent(limit)}&userId=${user.id}`;

  try {
    const [offline, threshold, alerts] = await Promise.all([
      fetchJson(offlineUrl),
      fetchJson(thresholdUrl),
      fetchJson(alertsUrl),
    ]);
    renderOffline(offline);
    renderOverLimit(threshold);
    renderAlerts(alerts);
    setStatus("ok", "OK • Đã cập nhật");
  } catch (e) {
    setStatus("error", "Lỗi");
    showError(e?.message || String(e));
  }
}

async function loadAlertsOnly() {
  showError("");
  setStatus("loading", "Đang load alerts...");

  const user = JSON.parse(sessionStorage.getItem("currentUser"));
  if (!user || !user.id) {
    setStatus("error", "Chưa đăng nhập");
    showError("Vui lòng đăng nhập để xem alerts của bạn");
    return;
  }

  const base = getApiBase();
  const limit = Number($("alerts-limit")?.value || 50) || 50;
  const alertsUrl = `${base}/api/daocuong/alerts?limit=${encodeURIComponent(limit)}&userId=${user.id}`;

  try {
    const alerts = await fetchJson(alertsUrl);
    renderAlerts(alerts);
    setStatus("ok", "OK • Alerts updated");
  } catch (e) {
    setStatus("error", "Lỗi");
    showError(e?.message || String(e));
  }
}

function updateClock() {
  const now = new Date();
  const timeEl = $("clock-time");
  const dateEl = $("clock-date");
  if (timeEl) timeEl.innerText = now.toLocaleTimeString("vi-VN");
  if (dateEl) dateEl.innerText = now.toLocaleDateString("vi-VN", {
    weekday: "long",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

function setupAutoRefresh() {
  if (timer) clearInterval(timer);
  timer = null;

  const seconds = Number($("refresh-seconds")?.value || 10) || 0;
  if (!autoEnabled || seconds <= 0) return;

  timer = setInterval(() => {
    runChecks();
  }, seconds * 1000);
}

function bindEvents() {
  $("btn-check-now")?.addEventListener("click", () => runChecks());
  $("btn-refresh-alerts")?.addEventListener("click", () => loadAlertsOnly());
  $("btn-toggle-auto")?.addEventListener("click", () => {
    autoEnabled = !autoEnabled;
    $("btn-toggle-auto").innerText = `Auto: ${autoEnabled ? "ON" : "OFF"}`;
    setupAutoRefresh();
  });

  ["api-base", "offline-minutes", "alerts-limit", "refresh-seconds"].forEach((id) => {
    $(id)?.addEventListener("change", () => setupAutoRefresh());
  });
}

window.onload = () => {
  bindEvents();
  updateClock();
  setInterval(updateClock, 1000);
  setStatus("idle", "Sẵn sàng");
  setupAutoRefresh();
  runChecks();
};
