const API_BASE = "http://localhost:8080/api/xuandat";

const elements = {
    deviceSelect: document.getElementById("device-select"),
    fromInput: document.getElementById("from-input"),
    toInput: document.getElementById("to-input"),
    bucketSelect: document.getElementById("bucket-select"),
    filterButton: document.getElementById("btn-filter"),
    exportButton: document.getElementById("btn-export"),
    feedback: document.getElementById("feedback"),
    totalRows: document.getElementById("total-rows"),
    totalEnergy: document.getElementById("total-energy"),
    alertCount: document.getElementById("alert-count"),
    latestTimestamp: document.getElementById("latest-timestamp"),
    chartDeviceName: document.getElementById("chart-device-name"),
    tableBucket: document.getElementById("table-bucket"),
    systemStatus: document.getElementById("system-status"),
    tableBody: document.getElementById("log-table-body"),
    rtVoltage: document.getElementById("rt-voltage"),
    rtCurrent: document.getElementById("rt-current"),
    rtPower: document.getElementById("rt-power"),
    loadProgress: document.getElementById("load-progress"),
    lastUpdate: document.getElementById("last-update"),
    statusChip: document.getElementById("status-chip"),
    breachBadge: document.getElementById("breach-badge"),
    latestAlert: document.getElementById("latest-alert"),
    alertsList: document.getElementById("alerts-list")
};

let consumptionChart;

document.addEventListener("DOMContentLoaded", async () => {
    setDefaultRange();
    initChart();
    bindEvents();
    await loadDevices();
    await loadDashboard();
});

function bindEvents() {
    elements.filterButton.addEventListener("click", loadDashboard);
    elements.exportButton.addEventListener("click", exportExcel);
}

function setDefaultRange() {
    const now = new Date();
    const before = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    elements.fromInput.value = toDatetimeLocalValue(before);
    elements.toInput.value = toDatetimeLocalValue(now);
}

async function loadDevices() {
    try {
        const response = await fetch(`${API_BASE}/devices`);
        if (!response.ok) {
            throw new Error("Không tải được danh sách thiết bị");
        }

        const devices = await response.json();
        elements.deviceSelect.innerHTML = [
            `<option value="">Tất cả thiết bị</option>`,
            ...devices.map((device) => `<option value="${device.id}">${escapeHtml(device.name)}${device.location ? ` - ${escapeHtml(device.location)}` : ""}</option>`)
        ].join("");

        if (devices.length > 0) {
            elements.deviceSelect.value = String(devices[0].id);
        }
    } catch (error) {
        setFeedback(error.message, true);
    }
}

async function loadDashboard() {
    setBusyState(true);
    const params = buildParams();

    try {
        const [historyResponse, alertsResponse] = await Promise.all([
            fetch(`${API_BASE}/history?${params.toString()}`),
            fetch(`${API_BASE}/alerts?${params.toString()}`)
        ]);

        if (!historyResponse.ok || !alertsResponse.ok) {
            throw new Error("Không lấy được dữ liệu từ backend");
        }

        const history = await historyResponse.json();
        const alerts = await alertsResponse.json();

        renderSummary(history.summary);
        renderChart(history.rows, history.device);
        renderTable(history.rows);
        renderLatestMeasurement(history.rows);
        renderAlerts(alerts);

        elements.tableBucket.textContent = history.filters.bucket;
        elements.systemStatus.textContent = "Đã kết nối";
        setFeedback(`Đã tải ${history.summary.totalRows} bản ghi từ service xuandat.`, false);
    } catch (error) {
        resetDashboard();
        elements.systemStatus.textContent = "Lỗi kết nối";
        setFeedback(error.message, true);
    } finally {
        setBusyState(false);
    }
}

function buildParams() {
    const params = new URLSearchParams();
    if (elements.deviceSelect.value) {
        params.set("deviceId", elements.deviceSelect.value);
    }
    params.set("from", elements.fromInput.value);
    params.set("to", elements.toInput.value);
    params.set("bucket", elements.bucketSelect.value);
    return params;
}

function renderSummary(summary) {
    elements.totalRows.textContent = summary.totalRows;
    elements.totalEnergy.textContent = Number(summary.totalEnergy || 0).toFixed(2);
    elements.alertCount.textContent = summary.alertCount;
    elements.latestTimestamp.textContent = summary.latestTimestamp || "--";
}

function renderChart(rows, device) {
    consumptionChart.data.labels = rows.map((row) => row.createdAt);
    consumptionChart.data.datasets[0].data = rows.map((row) => Number(row.power || 0));
    consumptionChart.data.datasets[1].data = rows.map((row) => Number(row.current || 0));
    consumptionChart.update();
    elements.chartDeviceName.textContent = device?.name || "Tất cả thiết bị";
}

function renderTable(rows) {
    if (!rows.length) {
        elements.tableBody.innerHTML = `<tr><td colspan="7" class="px-4 py-4 text-slate-400">Không có dữ liệu trong khoảng thời gian đã chọn.</td></tr>`;
        return;
    }

    elements.tableBody.innerHTML = rows.map((row) => {
        const badgeClass = row.isThresholdBreached
            ? "inline-flex items-center rounded-full bg-rose-500/10 text-rose-300 border border-rose-500/20 px-3 py-1 text-xs"
            : "inline-flex items-center rounded-full bg-emerald-500/10 text-emerald-300 border border-emerald-500/20 px-3 py-1 text-xs";
        const badgeText = row.isThresholdBreached ? row.breachTypes.join(", ") : "Ổn định";

        return `
            <tr class="hover:bg-slate-900/20 transition-colors">
                <td class="px-4 py-3">${escapeHtml(row.createdAt)}</td>
                <td class="px-4 py-3">${escapeHtml(row.deviceName || "--")}</td>
                <td class="px-4 py-3 text-blue-300">${formatNumber(row.voltage)}</td>
                <td class="px-4 py-3 text-orange-300">${formatNumber(row.current)}</td>
                <td class="px-4 py-3 text-yellow-300">${formatNumber(row.power)}</td>
                <td class="px-4 py-3 text-teal-300">${formatNumber(row.energy)}</td>
                <td class="px-4 py-3"><span class="${badgeClass}">${escapeHtml(badgeText)}</span></td>
            </tr>
        `;
    }).join("");
}

function renderLatestMeasurement(rows) {
    const latest = rows.length ? rows[rows.length - 1] : null;
    if (!latest) {
        elements.rtVoltage.textContent = "0.00";
        elements.rtCurrent.textContent = "0.00";
        elements.rtPower.textContent = "0.00";
        elements.loadProgress.style.width = "0%";
        elements.loadProgress.className = "h-full rounded-full bg-emerald-400";
        elements.statusChip.textContent = "Chưa có dữ liệu";
        elements.statusChip.className = "px-4 py-2 rounded-full bg-slate-500/10 text-slate-300 text-sm border border-slate-500/20";
        elements.lastUpdate.textContent = "Chưa có dữ liệu để hiển thị.";
        return;
    }

    elements.rtVoltage.textContent = formatNumber(latest.voltage);
    elements.rtCurrent.textContent = formatNumber(latest.current);
    elements.rtPower.textContent = formatNumber(latest.power);
    elements.lastUpdate.textContent = `Lần đo cuối: ${latest.createdAt}`;

    const percent = Math.min((Number(latest.power || 0) / 1200) * 100, 100);
    elements.loadProgress.style.width = `${percent}%`;

    if (latest.isThresholdBreached) {
        elements.loadProgress.className = "h-full rounded-full bg-rose-400";
        elements.statusChip.textContent = "Đang vượt ngưỡng";
        elements.statusChip.className = "px-4 py-2 rounded-full bg-rose-500/10 text-rose-300 text-sm border border-rose-500/20";
    } else {
        elements.loadProgress.className = "h-full rounded-full bg-emerald-400";
        elements.statusChip.textContent = "Ổn định";
        elements.statusChip.className = "px-4 py-2 rounded-full bg-emerald-500/10 text-emerald-300 text-sm border border-emerald-500/20";
    }
}

function renderAlerts(alerts) {
    elements.breachBadge.textContent = `${alerts.length} cảnh báo`;

    if (!alerts.length) {
        elements.latestAlert.innerHTML = `
            <h3 class="font-semibold text-white mb-2">Chưa có cảnh báo</h3>
            <p class="text-sm text-slate-400 leading-6">Dữ liệu hiện tại chưa vượt ngưỡng công suất hoặc dòng điện.</p>
        `;
        elements.alertsList.innerHTML = "";
        return;
    }

    const latest = alerts[0];
    elements.latestAlert.innerHTML = `
        <h3 class="font-semibold text-white mb-2">${escapeHtml(latest.type)}</h3>
        <p class="text-sm text-slate-300 leading-6">${escapeHtml(latest.message || "Không có nội dung")}<br>${escapeHtml(latest.createdAt || "")}</p>
    `;

    elements.alertsList.innerHTML = alerts.slice(0, 8).map((alert) => `
        <div class="rounded-2xl border border-rose-500/15 bg-slate-900/40 p-4">
            <p class="font-semibold text-white mb-1">${escapeHtml(alert.type)}</p>
            <p class="text-sm text-slate-400 leading-6">${escapeHtml(alert.message || "Không có nội dung")}</p>
            <p class="text-xs text-slate-500 mt-2">${escapeHtml(alert.createdAt || "")}</p>
        </div>
    `).join("");
}

async function exportExcel() {
    setBusyState(true);
    try {
        const response = await fetch(`${API_BASE}/export/excel?${buildParams().toString()}`);
        if (!response.ok) {
            throw new Error("Không thể xuất file Excel");
        }

        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = "xuandat-usecase3-report.xls";
        anchor.click();
        URL.revokeObjectURL(url);
        setFeedback("Đã xuất Excel từ dữ liệu usecase 3.", false);
    } catch (error) {
        setFeedback(error.message, true);
    } finally {
        setBusyState(false);
    }
}

function initChart() {
    const ctx = document.getElementById("consumptionChart").getContext("2d");
    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, "rgba(45, 212, 191, 0.32)");
    gradient.addColorStop(1, "rgba(45, 212, 191, 0)");

    consumptionChart = new Chart(ctx, {
        type: "line",
        data: {
            labels: [],
            datasets: [
                {
                    label: "Công suất (W)",
                    data: [],
                    borderColor: "#2dd4bf",
                    backgroundColor: gradient,
                    fill: true,
                    borderWidth: 3,
                    tension: 0.35
                },
                {
                    label: "Dòng điện (A)",
                    data: [],
                    borderColor: "#fb923c",
                    backgroundColor: "rgba(251, 146, 60, 0.12)",
                    fill: false,
                    borderWidth: 2,
                    tension: 0.25
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: {
                        color: "#e2e8f0"
                    }
                }
            },
            scales: {
                x: {
                    ticks: {
                        color: "#94a3b8",
                        maxRotation: 0,
                        autoSkip: true
                    },
                    grid: {
                        color: "rgba(148, 163, 184, 0.08)"
                    }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        color: "#94a3b8"
                    },
                    grid: {
                        color: "rgba(148, 163, 184, 0.08)"
                    }
                }
            }
        }
    });
}

function resetDashboard() {
    renderSummary({ totalRows: 0, totalEnergy: 0, alertCount: 0, latestTimestamp: null });
    renderTable([]);
    renderLatestMeasurement([]);
    renderAlerts([]);
    consumptionChart.data.labels = [];
    consumptionChart.data.datasets[0].data = [];
    consumptionChart.data.datasets[1].data = [];
    consumptionChart.update();
}

function setBusyState(isBusy) {
    elements.filterButton.disabled = isBusy;
    elements.exportButton.disabled = isBusy;
    elements.filterButton.innerHTML = isBusy
        ? '<i class="fa-solid fa-spinner fa-spin mr-2"></i>Đang tải...'
        : '<i class="fa-solid fa-filter mr-2"></i>Tải dữ liệu';
}

function setFeedback(message, isError) {
    elements.feedback.textContent = message;
    elements.feedback.style.color = isError ? "#fecdd3" : "#93c5fd";
}

function formatNumber(value) {
    return Number(value || 0).toFixed(2);
}

function toDatetimeLocalValue(date) {
    const shifted = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return shifted.toISOString().slice(0, 16);
}

function escapeHtml(value) {
    if (value === null || value === undefined) {
        return "";
    }
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}
