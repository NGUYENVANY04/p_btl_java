const user = JSON.parse(sessionStorage.getItem("currentUser")) || { id: 1 };
const API_BASE_URL = 'http://localhost:8080/api'; 

const STORAGE_KEYS = { budget: 'electricity_budget', price: 'electricity_price', warningLevel: 'warning_level' };

let appState = {
    budget: 0, currentUsage: 0, monthlyHistory: [], pricePerKwh: 3000, warningLevel: 80,
    currentMonth: getCurrentMonth()
};

let dailyChart = null; let costChart = null;
let userDeviceIds = []; 
let currentPayingMonth = null; 

document.addEventListener('DOMContentLoaded', async function () {
    const now = new Date();
    const subtitle = document.getElementById('currentMonthSubtitle');
    if (subtitle) subtitle.textContent = `Theo dõi chi phí điện năng của bạn - Tháng ${now.getMonth() + 1}/${now.getFullYear()}`;

    loadSettingsFromStorage();
    setupEventListeners();
    
    await getDeviceIds();
    await fetchRealUsageHistory();
    
    updateAllUI();
    initializeCharts();
    fetchAndRenderBills(); 
});

function setupEventListeners() {
    window.saveBudget = saveBudget; window.toggleEditBudget = toggleEditBudget;
    window.updatePrice = updatePrice; window.updateWarningLevel = updateWarningLevel;
    window.refreshData = refreshData;
}

// =====================
// API: THIẾT BỊ & ĐỒNG BỘ ĐIỆN NĂNG
// =====================
async function getDeviceIds() {
    if (userDeviceIds.length > 0) return userDeviceIds;
    try {
        const res = await fetch(`${API_BASE_URL}/device?userId=${user.id}`);
        const data = await res.json();
        if (data.deviceIds) userDeviceIds = data.deviceIds;
        else userDeviceIds = [data.deviceId || data];
        return userDeviceIds;
    } catch (e) {
        console.error("❌ Lỗi lấy Device ID:", e);
        return []; 
    }
}

async function fetchRealUsageHistory() {
    if (!userDeviceIds || userDeviceIds.length === 0) return false;
    const idsParam = userDeviceIds.join(',');
    try {
        const res = await fetch(`${API_BASE_URL}/energy/monthly?deviceIds=${idsParam}&month=${appState.currentMonth}`);
        const data = await res.json();
        appState.monthlyHistory = data.map(item => ({
            date: item.ngay, usage: item.tong_nang_luong, cost: item.tong_nang_luong * appState.pricePerKwh
        }));
        recalculateCurrentUsage();
        return true;
    } catch (e) {
        return false;
    }
}

// =====================
// API: HÓA ĐƠN & THANH TOÁN
// =====================
async function fetchAndRenderBills() {
    const tbody = document.getElementById('billTableBody');
    if(!tbody) return;

    try {
        if (!userDeviceIds || userDeviceIds.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center p-4 text-slate-500">Không tìm thấy thiết bị nào</td></tr>';
            return;
        }
        
        const idsParam = userDeviceIds.join(',');
        const res = await fetch(`${API_BASE_URL}/bills?userId=${user.id}&deviceIds=${idsParam}`);
        const bills = await res.json();

        tbody.innerHTML = '';
        if (bills.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center p-4 text-slate-500">Chưa có dữ liệu hóa đơn</td></tr>';
            return;
        }

        bills.forEach(b => {
            const row = document.createElement('tr');
            row.className = "border-b border-white/5 hover:bg-white/5 transition-colors";

            let statusHtml = ''; let actionHtml = '';

            if (b.status === 'paid') {
                statusHtml = `<span style="color:#10b981; background:rgba(16,185,129,0.1); padding:4px 10px; border-radius:6px; font-weight:bold; font-size:12px">Đã thanh toán</span>`;
                actionHtml = `<span class="text-slate-500 italic text-sm">Hoàn tất</span>`;
            } 
            else if (b.status === 'unpaid' && b.can_pay === false) {
                statusHtml = `<span style="color:#f59e0b; background:rgba(245,158,11,0.1); padding:4px 10px; border-radius:6px; font-weight:bold; font-size:12px">Chưa chốt sổ</span>`;
                actionHtml = `<button disabled class="px-4 py-2 bg-slate-800 text-slate-500 rounded cursor-not-allowed font-bold text-xs">Chờ qua tháng</button>`;
            } 
            else {
                statusHtml = `<span style="color:#ef4444; background:rgba(239,68,68,0.1); padding:4px 10px; border-radius:6px; font-weight:bold; font-size:12px">Chưa thanh toán</span>`;
                actionHtml = `<button onclick="openQRModal('${b.month}', ${b.total_amount})" class="px-4 py-2 bg-blue-600 text-white rounded font-bold hover:bg-blue-700 transition shadow-[0_0_10px_rgba(37,99,235,0.3)] text-xs uppercase tracking-wider">Thanh Toán</button>`;
            }

            const devicesStr = b.devices ? b.devices.join(', ') : 'N/A';
            row.innerHTML = `
                <td class="p-3">
                    <div class="font-bold text-white">Tháng ${b.month}</div>
                    <div class="text-[10px] text-slate-500 mt-1">ID Máy: ${devicesStr}</div>
                </td>
                <td class="p-3"><strong class="text-emerald-400">${b.total_energy.toFixed(2)}</strong> kWh</td>
                <td class="p-3 text-orange-400 font-bold">${b.total_amount.toLocaleString('vi-VN')} ₫</td>
                <td class="p-3">${statusHtml}</td>
                <td class="p-3 text-right">${actionHtml}</td>
            `;
            tbody.appendChild(row);
        });
    } catch (e) { console.error("Lỗi lấy hóa đơn:", e); }
}

window.openQRModal = function(month, amount) {
    currentPayingMonth = month; 
    document.getElementById('qrMonthText').innerText = `Kỳ hóa đơn: Tháng ${month}`;
    document.getElementById('qrAmountText').innerText = `${amount.toLocaleString('vi-VN')} ₫`;
    document.getElementById('qrModal').classList.remove('hidden');
}

window.closeQRModal = function() {
    currentPayingMonth = null;
    document.getElementById('qrModal').classList.add('hidden');
}

window.confirmPaymentAction = async function() {
    if (!currentPayingMonth) return;
    try {
        const idsParam = userDeviceIds.join(',');
        const res = await fetch(`${API_BASE_URL}/bills/pay?userId=${user.id}&deviceIds=${idsParam}&month=${currentPayingMonth}`, {
            method: 'POST'
        });

        if (res.ok) {
            closeQRModal();
            showNotification('Thanh toán thành công! Hệ thống đã ghi nhận.', 'success');
            fetchAndRenderBills(); 
        } else {
            const errData = await res.json();
            alert('Lỗi: ' + (errData.message || 'Không thể cập nhật hóa đơn.'));
        }
    } catch (e) {
        alert('Không thể kết nối đến máy chủ.');
    }
}

// =====================
// CÀI ĐẶT LOCAL & TÍNH TOÁN NGÂN SÁCH
// =====================
function loadSettingsFromStorage() {
    appState.budget = parseFloat(localStorage.getItem(STORAGE_KEYS.budget)) || 0;
    appState.pricePerKwh = parseFloat(localStorage.getItem(STORAGE_KEYS.price)) || 3000;
    appState.warningLevel = parseFloat(localStorage.getItem(STORAGE_KEYS.warningLevel)) || 80;
}

function saveSettingsToStorage() {
    localStorage.setItem(STORAGE_KEYS.budget, appState.budget.toString());
    localStorage.setItem(STORAGE_KEYS.price, appState.pricePerKwh.toString());
    localStorage.setItem(STORAGE_KEYS.warningLevel, appState.warningLevel.toString());
}

function toggleEditBudget() {
    const display = document.getElementById('budgetDisplay'); const form = document.getElementById('budgetEditForm'); const input = document.getElementById('budgetInput');
    if (form.style.display === 'none') { display.style.display = 'none'; form.style.display = 'block'; input.value = appState.budget; input.focus(); } 
    else { display.style.display = 'block'; form.style.display = 'none'; }
}

function saveBudget() {
    const value = parseFloat(document.getElementById('budgetInput').value);
    if (isNaN(value) || value < 0) return alert('Vui lòng nhập hạn mức hợp lệ');
    appState.budget = value; saveSettingsToStorage(); toggleEditBudget(); updateAllUI();
    showNotification('Đã lưu hạn mức ngân sách', 'success');
}

function recalculateCurrentUsage() {
    appState.currentUsage = appState.monthlyHistory.reduce((sum, entry) => sum + entry.usage, 0);
}

function updatePrice() {
    const value = parseFloat(document.getElementById('priceInput').value);
    if (isNaN(value) || value <= 0) return alert('Vui lòng nhập giá tiền hợp lệ');
    appState.pricePerKwh = value; saveSettingsToStorage(); updateAllUI();
    showNotification('Đã cập nhật giá điện mới', 'success');
}

function updateWarningLevel() {
    const value = parseFloat(document.getElementById('warningLevel').value);
    if (isNaN(value) || value < 0 || value > 100) return alert('Vui lòng nhập phần trăm hợp lệ');
    appState.warningLevel = value; saveSettingsToStorage(); updateAllUI();
    showNotification('Đã cập nhật mốc cảnh báo', 'success');
}

function getCurrentMonth() {
    const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

// =====================
// UI UPDATES & BIỂU ĐỒ
// =====================
function updateAllUI() {
    document.getElementById('budgetDisplay').querySelector('.amount').textContent = appState.budget.toLocaleString('vi-VN');
    document.getElementById('budgetInput').value = appState.budget;

    const now = new Date();
    document.getElementById('currentUsage').textContent = appState.currentUsage.toFixed(1);
    document.getElementById('lastUpdate').textContent = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');

    const percentage = appState.budget > 0 ? (appState.currentUsage / appState.budget * 100) : 0;
    document.getElementById('percentageValue').textContent = Math.min(percentage, 100).toFixed(1) + '%';

    document.getElementById('estimatedCost').textContent = Math.round(appState.currentUsage * appState.pricePerKwh).toLocaleString('vi-VN');
    document.getElementById('pricePerKwh').textContent = appState.pricePerKwh.toLocaleString('vi-VN');
    document.getElementById('priceInput').value = appState.pricePerKwh;

    document.getElementById('progressBar').style.width = Math.min(percentage, 100) + '%';
    document.getElementById('progressLabel').textContent = Math.min(percentage, 100).toFixed(1) + '%';
    document.getElementById('remainingKwh').textContent = Math.max(appState.budget - appState.currentUsage, 0).toFixed(1);

    const statusElement = document.getElementById('statusText');
    if (percentage >= 100) { statusElement.textContent = 'Đã vượt quá hạn mức!'; statusElement.style.background = '#fee2e2'; statusElement.style.color = '#dc2626'; } 
    else if (percentage >= appState.warningLevel) { statusElement.textContent = `Cảnh báo: Đã dùng ${percentage.toFixed(0)}% hạn mức`; statusElement.style.background = '#fef3c7'; statusElement.style.color = '#d97706'; } 
    else { statusElement.textContent = 'Sử dụng bình thường'; statusElement.style.background = '#d1fae5'; statusElement.style.color = '#059669'; }
    
    updateCharts();
}

function initializeCharts() {
    if(typeof Chart === 'undefined') return;
    Chart.defaults.color = '#94a3b8'; Chart.defaults.font.family = "'Plus Jakarta Sans', sans-serif";

    const dailyCtx = document.getElementById('dailyChart');
    if (dailyCtx) dailyChart = new Chart(dailyCtx, { type: 'line', data: { labels: [], datasets: [{ label: 'Tiêu Thụ (kWh)', data: [], borderColor: '#10b981', backgroundColor: 'rgba(16, 185, 129, 0.1)', borderWidth: 3, fill: true, tension: 0.4 }] }, options: { responsive: true, maintainAspectRatio: true, plugins: { legend: { display: true, position: 'top' } }, scales: { x: { grid: { display: false } }, y: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, beginAtZero: true } } } });

    const costCtx = document.getElementById('costChart');
    if (costCtx) costChart = new Chart(costCtx, { type: 'doughnut', data: { labels: ['Đã sử dụng', 'Còn lại'], datasets: [{ data: [], backgroundColor: ['#10b981', '#1e293b'], borderColor: ['#162641', '#162641'], borderWidth: 2 }] }, options: { responsive: true, maintainAspectRatio: true } });
}

function updateCharts() {
    if (!dailyChart || !costChart) return;
    const dailyData = {}; appState.monthlyHistory.forEach(entry => dailyData[entry.date] = entry.usage);
    const sortedDates = Object.keys(dailyData).sort();
    
    dailyChart.data.labels = sortedDates.map(date => new Date(date).toLocaleDateString('vi-VN', { month: 'short', day: 'numeric' }));
    dailyChart.data.datasets[0].data = sortedDates.map(date => dailyData[date]);
    dailyChart.update();

    const remaining = Math.max(appState.budget - appState.currentUsage, 0);
    costChart.data.datasets[0].data = [appState.currentUsage, remaining];
    const pct = appState.budget > 0 ? (appState.currentUsage / appState.budget * 100) : 0;
    costChart.data.datasets[0].backgroundColor[0] = pct >= 100 ? '#ef4444' : pct >= appState.warningLevel ? '#f59e0b' : '#10b981';
    costChart.update();
}

async function refreshData() {
    document.getElementById('billTableBody').innerHTML = '<tr><td colspan="5" class="text-center p-4 text-slate-500">Đang đồng bộ...</td></tr>';
    await fetchRealUsageHistory(); fetchAndRenderBills(); updateAllUI();
    showNotification('Đã đồng bộ dữ liệu với máy chủ', 'success');
}

function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.style.cssText = `position: fixed; top: 20px; right: 20px; padding: 15px 20px; background: ${type === 'success' ? '#10b981' : '#3b82f6'}; color: white; border-radius: 8px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2); z-index: 9999; animation: slideIn 0.3s ease;`;
    notification.textContent = message; document.body.appendChild(notification);
    setTimeout(() => { notification.style.animation = 'slideOut 0.3s ease'; setTimeout(() => notification.remove(), 300); }, 3000);
}