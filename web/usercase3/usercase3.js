// ================= CẤU HÌNH KẾT NỐI API =================
const CONFIG = {
    USE_MOCK_DATA: false,
    API_BASE_URL: 'http://localhost:8080/api/xuandat'
};

// device-select giờ có value = device_id thật trong Supabase (101, 90, 87)
// Không cần map nữa, lấy trực tiếp

let consumptionChart;
let simulationInterval;
let isOnline = true;

// ===============================================================
// BOOT
// ===============================================================
document.addEventListener('DOMContentLoaded', function () {
    initChart();
    loadDevices(); // Fetch danh sách thiết bị thật từ API

    if (CONFIG.USE_MOCK_DATA) {
        initSimulatedData();
    } else {
        startRealtimeAPI();
    }

    renderLogs();
    initConnectionToggle();
});

// ===============================================================
// LOAD DEVICES: Fetch từ /api/xuandat/devices rồi fill dropdown
// ===============================================================
async function loadDevices() {
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/devices`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const devices = await response.json();
        if (!devices || devices.length === 0) return;

        // Fill dropdown thiết bị (góc trái - realtime)
        const deviceSelect = document.getElementById('device-select');
        if (deviceSelect) {
            deviceSelect.innerHTML = '';
            devices.forEach(d => {
                const opt = document.createElement('option');
                opt.value = d.id;
                opt.textContent = `${d.name || 'Thiết bị ' + d.id} (ID: ${d.id})`;
                deviceSelect.appendChild(opt);
            });
        }

        // Fill dropdown filter khách hàng (góc phải - lịch sử)
        const filterUser = document.getElementById('filter-user');
        if (filterUser) {
            // Giữ lại option "Tất cả"
            filterUser.innerHTML = '<option value="all">Tất cả Khách hàng</option>';
            devices.forEach(d => {
                const opt = document.createElement('option');
                opt.value = d.id;
                opt.textContent = `${d.name || 'Thiết bị ' + d.id} (ID: ${d.id})`;
                filterUser.appendChild(opt);
            });
        }

    } catch (err) {
        console.warn('[loadDevices] Không thể fetch devices, dùng options mặc định:', err.message);
        // Giữ nguyên options hardcode trong HTML làm fallback
    }
}

// ===============================================================
// CONNECTION TOGGLE (Bật/Tắt giả lập kết nối để demo)
// ===============================================================
function initConnectionToggle() {
    document.getElementById('btn-toggle-connection')?.addEventListener('click', function () {
        if (isOnline) {
            clearInterval(simulationInterval);
            isOnline = false;
            this.innerHTML = '<i class="fa-solid fa-power-off mr-1"></i> Bật lại (Test)';
            this.classList.replace('bg-slate-700', 'bg-red-500/20');
            this.classList.replace('hover:bg-slate-600', 'hover:bg-red-500/40');
            this.classList.replace('text-slate-300', 'text-red-400');
            this.classList.replace('border-slate-600', 'border-red-500/50');

            const statusDot = document.getElementById('status-dot');
            const statusText = document.getElementById('status-text');
            if (statusDot) statusDot.className = 'h-2.5 w-2.5 rounded-full bg-slate-500';
            if (statusText) {
                statusText.className = 'text-xs font-semibold text-slate-400';
                statusText.innerText = 'Offline';
            }
            const lu = document.getElementById('last-update');
            if (lu) { lu.innerText = 'Mất kết nối server'; lu.style.color = '#ef4444'; }
        } else {
            isOnline = true;
            this.innerHTML = '<i class="fa-solid fa-power-off mr-1"></i> Ngắt kết nối (Test)';
            this.classList.replace('bg-red-500/20', 'bg-slate-700');
            this.classList.replace('hover:bg-red-500/40', 'hover:bg-slate-600');
            this.classList.replace('text-red-400', 'text-slate-300');
            this.classList.replace('border-red-500/50', 'border-slate-600');

            const statusDot = document.getElementById('status-dot');
            const statusText = document.getElementById('status-text');
            if (statusDot) statusDot.className = 'h-2.5 w-2.5 rounded-full bg-green-500 pulse-green';
            if (statusText) {
                statusText.className = 'text-xs font-semibold text-green-400';
                statusText.innerText = 'Online';
            }

            if (CONFIG.USE_MOCK_DATA) {
                startSimulation();
            } else {
                startRealtimeAPI();
            }
        }
    });
}

// ===============================================================
// REALTIME: Gọi API /api/xuandat/realtime mỗi 3 giây
// ===============================================================
async function startRealtimeAPI() {
    // Gọi ngay lần đầu
    await fetchAndRenderRealtime();

    simulationInterval = setInterval(async () => {
        if (!isOnline) return;
        await fetchAndRenderRealtime();
    }, 3000);
}

async function fetchAndRenderRealtime() {
    try {
        const deviceSelect = document.getElementById('device-select');
        const deviceId = deviceSelect ? parseInt(deviceSelect.value) : 101;

        const response = await fetch(`${CONFIG.API_BASE_URL}/realtime?deviceId=${deviceId}`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const data = await response.json();
        if (!data || Object.keys(data).length === 0) return;

        const u = parseFloat(data.voltage || 0);
        const i = parseFloat(data.current || 0);
        const p = parseFloat(data.power || 0);

        // Hiển thị chỉ số
        document.getElementById('rt-voltage').innerText = u.toFixed(1);
        document.getElementById('rt-current').innerText = i.toFixed(2);
        document.getElementById('rt-power').innerText = p.toFixed(1);

        // Thanh load
        updateLoadBar(p);

        // Label "Vừa xong"
        flashLastUpdate();

    } catch (error) {
        console.warn('[Realtime] Không thể kết nối Backend Java:', error.message);
        // Fallback: dùng mock nếu API lỗi
        runMockTick();
    }
}

function updateLoadBar(p) {
    const percent = Math.min((p / 1200) * 100, 100);
    const loadProgress = document.getElementById('load-progress');
    if (!loadProgress) return;

    loadProgress.style.width = percent + '%';
    if (percent > 66) {
        loadProgress.className = 'h-1.5 rounded-full bg-red-500';
        if (!window.hasAlertedSpike) {
            showToast('Cảnh báo quá tải thiết bị!',
                `Công suất hiện tại là <b class="text-white">${p.toFixed(1)}W</b>, vượt ngưỡng an toàn (800W). Giảm tải ngay!`, true);
            window.hasAlertedSpike = true;
            setTimeout(() => window.hasAlertedSpike = false, 10000);
        }
    } else if (percent > 40) {
        loadProgress.className = 'h-1.5 rounded-full bg-yellow-400';
    } else {
        loadProgress.className = 'h-1.5 rounded-full bg-green-400';
    }
}

function flashLastUpdate() {
    const lastUpdate = document.getElementById('last-update');
    if (!lastUpdate) return;
    lastUpdate.innerText = 'Vừa xong';
    lastUpdate.style.color = '#38bdf8';
    setTimeout(() => { if (isOnline) lastUpdate.style.color = '#cbd5e1'; }, 500);
}

// ===============================================================
// MOCK: Dùng khi USE_MOCK_DATA = true HOẶC API lỗi
// ===============================================================
function initSimulatedData() {
    startSimulation();
}

function startSimulation() {
    simulationInterval = setInterval(runMockTick, 3000);
    runMockTick();
}

function runMockTick() {
    if (!isOnline) return;
    const u = parseFloat((220 + Math.random() * 5).toFixed(1));
    let i = parseFloat((2.0 + Math.random() * 1.5).toFixed(2));
    if (Math.random() < 0.1) i = parseFloat((i + 2.5).toFixed(2));
    const p = parseFloat((u * i).toFixed(1));

    document.getElementById('rt-voltage').innerText = u;
    document.getElementById('rt-current').innerText = i;
    document.getElementById('rt-power').innerText = p;

    updateLoadBar(p);
    flashLastUpdate();
}

// ===============================================================
// CHART
// ===============================================================
function initChart() {
    const ctx = document.getElementById('consumptionChart').getContext('2d');

    let gradientFill = ctx.createLinearGradient(0, 0, 0, 400);
    gradientFill.addColorStop(0, 'rgba(45, 212, 191, 0.5)');
    gradientFill.addColorStop(1, 'rgba(45, 212, 191, 0)');

    consumptionChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['00:00', '03:00', '06:00', '09:00', '12:00', '15:00', '18:00', '21:00'],
            datasets: [{
                label: 'Điện năng tiêu thụ (kWh)',
                data: [0.5, 0.3, 1.2, 2.5, 2.0, 3.5, 4.8, 2.1],
                borderColor: '#2dd4bf',
                backgroundColor: gradientFill,
                borderWidth: 2,
                pointBackgroundColor: '#111827',
                pointBorderColor: '#2dd4bf',
                pointBorderWidth: 2,
                pointRadius: 4,
                pointHoverRadius: 6,
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(15, 23, 42, 0.9)',
                    titleColor: '#e2e8f0',
                    bodyColor: '#2dd4bf',
                    borderColor: 'rgba(255,255,255,0.1)',
                    borderWidth: 1,
                    padding: 10,
                    displayColors: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: 'rgba(255, 255, 255, 0.05)', borderDash: [5, 5] },
                    ticks: { color: '#94a3b8', font: { size: 11 } }
                },
                x: {
                    grid: { display: false },
                    ticks: { color: '#94a3b8', font: { size: 11 } }
                }
            },
            interaction: { intersect: false, mode: 'index' }
        }
    });
}

// ===============================================================
// RENDER LOGS: Gọi API /api/xuandat/history/day (hoặc month/year)
// ===============================================================
async function renderLogs() {
    try {
        const tbody = document.getElementById('log-table-body');
        if (!tbody) return;

        // Dữ liệu mock sẵn (dùng khi API lỗi hoặc USE_MOCK_DATA = true)
        const MOCK_LOGS = [
            { time: "16:30:00 17/04/2026", device: "Nguyễn Văn A (Nhà 101)", voltage: 224.5, current: 2.45, power: 550.0 },
            { time: "16:15:00 17/04/2026", device: "Trần Thị B (Nhà 102)", voltage: 225.1, current: 2.50, power: 562.7 },
            { time: "16:00:00 17/04/2026", device: "Trần Thị B (Nhà 102)", voltage: 223.8, current: 1.10, power: 246.1 },
            { time: "15:45:00 17/04/2026", device: "Lê Văn C (Nhà 103)", voltage: 224.0, current: 1.05, power: 235.2 },
            { time: "15:30:00 17/04/2026", device: "Nguyễn Văn A (Nhà 101)", voltage: 222.5, current: 0.95, power: 211.3 },
            { time: "15:00:00 17/04/2026", device: "Nguyễn Văn A (Nhà 101)", voltage: 221.8, current: 1.20, power: 266.1 },
            { time: "14:30:00 17/04/2026", device: "Lê Văn C (Nhà 103)", voltage: 223.0, current: 0.88, power: 196.2 },
            { time: "14:00:00 17/04/2026", device: "Trần Thị B (Nhà 102)", voltage: 224.3, current: 3.10, power: 695.3 }
        ];

        if (CONFIG.USE_MOCK_DATA) {
            renderLogsToTable(tbody, filterMockLogs(MOCK_LOGS));
            return;
        }

        // Gọi API thật
        const filterType = document.getElementById('filter-type').value;
        const filterDate = document.getElementById('filter-date').value;
        const filterUser = document.getElementById('filter-user').value;
        const deviceId = filterUser !== 'all' ? parseInt(filterUser) : null;

        let apiUrl;
        if (filterType === 'month') {
            const month = filterDate.substring(0, 7); // "2026-04"
            apiUrl = `${CONFIG.API_BASE_URL}/history/month?month=${month}`;
            if (deviceId) apiUrl += `&deviceId=${deviceId}`;
        } else if (filterType === 'year') {
            const year = filterDate.substring(0, 4); // "2026"
            apiUrl = `${CONFIG.API_BASE_URL}/history/year?year=${year}`;
            if (deviceId) apiUrl += `&deviceId=${deviceId}`;
        } else {
            // Mặc định: theo ngày
            apiUrl = `${CONFIG.API_BASE_URL}/history/day?day=${filterDate}`;
            if (deviceId) apiUrl += `&deviceId=${deviceId}`;
        }

        const response = await fetch(apiUrl);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const logsData = await response.json();

        if (!logsData || logsData.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-6 text-slate-500">Không có dữ liệu cho khoảng thời gian này.</td></tr>';
            return;
        }

        // Dữ liệu summary (month/year) khác cấu trúc với day
        if (filterType === 'month' || filterType === 'year') {
            renderSummaryToTable(tbody, logsData, filterType);
        } else {
            renderLogsToTable(tbody, logsData);
        }

    } catch (e) {
        console.warn('[renderLogs] Lỗi, fallback mock:', e.message);
        // Fallback về mock
        const tbody = document.getElementById('log-table-body');
        if (tbody) renderLogsToTable(tbody, filterMockLogs(FAKE_LOGS_FALLBACK()));
    }
}

function FAKE_LOGS_FALLBACK() {
    return [
        { time: "16:30:00 20/04/2026", device: "Nguyễn Văn A (Nhà 101)", voltage: 224.5, current: 2.45, power: 550.0 },
        { time: "16:00:00 20/04/2026", device: "Trần Thị B (Nhà 102)", voltage: 225.1, current: 2.50, power: 562.7 },
        { time: "15:30:00 20/04/2026", device: "Lê Văn C (Nhà 103)", voltage: 223.8, current: 1.10, power: 246.1 },
        { time: "15:00:00 20/04/2026", device: "Nguyễn Văn A (Nhà 101)", voltage: 224.0, current: 1.05, power: 235.2 },
        { time: "14:30:00 20/04/2026", device: "Trần Thị B (Nhà 102)", voltage: 222.5, current: 0.95, power: 211.3 }
    ];
}

function filterMockLogs(logs) {
    const filterUser = document.getElementById('filter-user')?.value || 'all';
    if (filterUser === 'all') return logs;
    const nameMap = { "101": "Nguyễn Văn A", "102": "Trần Thị B", "103": "Lê Văn C" };
    return logs.filter(l => l.device.includes(nameMap[filterUser]));
}

function renderLogsToTable(tbody, logs) {
    tbody.innerHTML = '';
    if (logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-6 text-slate-500">Không có dữ liệu.</td></tr>';
        return;
    }
    logs.forEach(log => {
        const tr = document.createElement('tr');
        tr.className = 'hover:bg-slate-800/30 transition-colors';
        tr.innerHTML = `
            <td class="px-4 py-3">${log.time}</td>
            <td class="px-4 py-3 font-semibold text-indigo-300 border-l border-indigo-900/30">${log.device}</td>
            <td class="px-4 py-3 text-blue-300">${parseFloat(log.voltage || 0).toFixed(1)}</td>
            <td class="px-4 py-3 text-orange-300">${parseFloat(log.current || 0).toFixed(2)}</td>
            <td class="px-4 py-3 text-yellow-300">${parseFloat(log.power || 0).toFixed(1)}</td>
        `;
        tbody.appendChild(tr);
    });
}

function renderSummaryToTable(tbody, logs, type) {
    tbody.innerHTML = '';
    // Thay đổi header cho phù hợp loại hiển thị
    const headers = type === 'month'
        ? ['Ngày', 'Tổng điện năng (kWh)', '-', '-', '-']
        : ['Tháng', 'Tổng điện năng (kWh)', '-', '-', '-'];

    logs.forEach(log => {
        const label = log.date || log.month || '-';
        const kwh = parseFloat(log.energy_kwh || 0).toFixed(2);
        const tr = document.createElement('tr');
        tr.className = 'hover:bg-slate-800/30 transition-colors';
        tr.innerHTML = `
            <td class="px-4 py-3">${label}</td>
            <td class="px-4 py-3 font-semibold text-indigo-300 border-l border-indigo-900/30" colspan="4" class="text-teal-300">${kwh} kWh</td>
        `;
        tbody.appendChild(tr);
    });
}

// ===============================================================
// BỘ LỌC: Click "Lọc" → gọi API phù hợp
// ===============================================================
document.getElementById('btn-filter')?.addEventListener('click', async function () {
    const btn = this;
    const originalHtml = btn.innerHTML;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang tải...';
    btn.disabled = true;

    try {
        const filterType = document.getElementById('filter-type').value;
        const filterDate = document.getElementById('filter-date').value;
        const filterUser = document.getElementById('filter-user').value;
        const deviceId = filterUser !== 'all' ? parseInt(filterUser) : null;

        if (!CONFIG.USE_MOCK_DATA) {
            // Гọи API lấy dữ liệu biểu đồ
            let chartApiUrl, chartData = [], chartLabels = [];

            if (filterType === 'month') {
                const month = filterDate.substring(0, 7);
                chartApiUrl = `${CONFIG.API_BASE_URL}/history/month?month=${month}`;
                if (deviceId) chartApiUrl += `&deviceId=${deviceId}`;

                const res = await fetch(chartApiUrl);
                if (res.ok) {
                    const json = await res.json();
                    chartLabels = json.map(d => d.date || d.month || '');
                    chartData = json.map(d => parseFloat(d.energy_kwh || 0));
                }
            } else if (filterType === 'year') {
                const year = filterDate.substring(0, 4);
                chartApiUrl = `${CONFIG.API_BASE_URL}/history/year?year=${year}`;
                if (deviceId) chartApiUrl += `&deviceId=${deviceId}`;

                const res = await fetch(chartApiUrl);
                if (res.ok) {
                    const json = await res.json();
                    chartLabels = json.map(d => d.month || '');
                    chartData = json.map(d => parseFloat(d.energy_kwh || 0));
                }
            } else {
                // DAY: dùng data từ history/day để tính tổng theo giờ
                chartApiUrl = `${CONFIG.API_BASE_URL}/history/day?day=${filterDate}`;
                if (deviceId) chartApiUrl += `&deviceId=${deviceId}`;

                const res = await fetch(chartApiUrl);
                if (res.ok) {
                    const json = await res.json();
                    // Group theo giờ
                    const hourMap = {};
                    json.forEach(d => {
                        const hour = d.time ? d.time.substring(0, 2) + ':00' : '00:00';
                        const p = parseFloat(d.power || 0);
                        hourMap[hour] = (hourMap[hour] || 0) + (p / 1000); // W → kWh approx
                    });
                    chartLabels = Object.keys(hourMap).sort();
                    chartData = chartLabels.map(h => Math.round(hourMap[h] * 100) / 100);
                }
            }

            if (chartData.length > 0) {
                consumptionChart.data.labels = chartLabels;
                consumptionChart.data.datasets[0].data = chartData;
                consumptionChart.update();
            } else {
                // Không có data từ API → random demo
                updateChartWithMock();
            }
        } else {
            updateChartWithMock();
        }

        // Cập nhật tổng quan
        const currentData = consumptionChart.data.datasets[0].data;
        const sum = currentData.reduce((a, b) => parseFloat(a) + parseFloat(b), 0).toFixed(1);
        document.getElementById('total-consumption').innerHTML = sum + ' <span class="text-xs text-teal-200">kWh</span>';
        document.getElementById('est-cost').innerHTML = Math.round(sum * 2800).toLocaleString('vi-VN') + ' <span class="text-xs text-pink-200">VNĐ</span>';

        // Render lại bảng logs
        await renderLogs();

    } catch (err) {
        console.warn('[Filter] Lỗi khi lọc:', err.message);
        updateChartWithMock();
        await renderLogs();
    }

    btn.innerHTML = originalHtml;
    btn.disabled = false;
});

function updateChartWithMock() {
    const newData = Array.from({ length: 8 }, () => parseFloat((Math.random() * 5).toFixed(1)));
    consumptionChart.data.datasets[0].data = newData;
    consumptionChart.update();
}

// ===============================================================
// XUẤT EXCEL
// ===============================================================
document.getElementById('btn-export')?.addEventListener('click', function () {
    const table = document.getElementById('log-table');
    if (!table) { alert('Không tìm thấy bảng dữ liệu!'); return; }

    const wb = XLSX.utils.table_to_book(table, { sheet: "NhatKyTieuThu" });
    XLSX.writeFile(wb, 'LichSu_TieuThuDien.xlsx');
});

// ===============================================================
// TOAST NOTIFICATION
// ===============================================================
function showToast(title, message, isError = true) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    const borderColor = isError ? 'border-red-500/50' : 'border-blue-500/50';
    const bgColor = isError ? 'bg-red-950/80' : 'bg-blue-950/80';
    const iconColor = isError ? 'text-red-400' : 'text-blue-400';
    const icon = isError ? 'fa-triangle-exclamation' : 'fa-bell';

    toast.className = `glass-panel ${bgColor} border ${borderColor} p-4 rounded-xl shadow-2xl flex items-start gap-3 w-80 transform transition-all duration-300 translate-x-full opacity-0`;
    toast.innerHTML = `
        <i class="fa-solid ${icon} ${iconColor} text-2xl mt-0.5"></i>
        <div class="flex-1">
            <h4 class="text-white font-bold text-sm tracking-wide leading-tight">${title}</h4>
            <p class="text-slate-300 text-xs mt-1 leading-relaxed">${message}</p>
        </div>
        <button class="text-slate-400 hover:text-white transition-colors" onclick="this.parentElement.style.opacity=0; setTimeout(()=>this.parentElement.remove(), 300)">
            <i class="fa-solid fa-xmark"></i>
        </button>
    `;

    container.appendChild(toast);
    requestAnimationFrame(() => { toast.classList.remove('translate-x-full', 'opacity-0'); });
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 6000);
}
