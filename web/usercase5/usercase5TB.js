const user = JSON.parse(sessionStorage.getItem("currentUser")) || { id: 1 };
const BASE_URL = "http://localhost:8080/api";

let allAlerts = []; 
let currentFilter = 'all'; 
let userDeviceIds = []; // Chứa danh sách [1, 2, 3]

// =====================
// 1. LẤY LIST DEVICE ID TỪ USER ID
// =====================
async function getDeviceIds() {
    if (userDeviceIds.length > 0) return userDeviceIds;

    try {
        const res = await fetch(`${BASE_URL}/device?userId=${user.id}`);
        const data = await res.json();
        
        if (data.deviceIds) {
            userDeviceIds = data.deviceIds;
        } else {
            userDeviceIds = [data.deviceId || data];
        }
        return userDeviceIds;
    } catch (e) {
        console.error("❌ Lỗi lấy Device ID:", e);
        return [];
    }
}

async function fetchAlerts() {
    const listContainer = document.getElementById('alert-list');
    const countText = document.getElementById('alert-count');

    try {
        await getDeviceIds(); // Lấy tất cả thiết bị của user

        const response = await fetch(`${BASE_URL}/alerts`);
        if (!response.ok) throw new Error("Không thể kết nối API");
        
        allAlerts = await response.json();
        renderAlerts();

    } catch (error) {
        console.error("Lỗi fetch alerts:", error);
        countText.innerText = "Lỗi kết nối API";
        listContainer.innerHTML = `
            <div class="text-center p-10 text-red-400 bg-red-400/5 rounded-xl border border-red-400/20">
                <i class="fas fa-exclamation-triangle mb-3 text-2xl"></i>
                <p>Không thể lấy dữ liệu từ máy chủ. Vui lòng kiểm tra lại Backend.</p>
            </div>`;
    }
}

function filterAlerts(type) {
    currentFilter = type;
    
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    if (type === 'all') document.getElementById('tab-all').classList.add('active');
    else if (type === 'hệ thống') document.getElementById('tab-system').classList.add('active');
    else if (type === 'thiết bị') document.getElementById('tab-device').classList.add('active');

    renderAlerts();
}

function renderAlerts() {
    const listContainer = document.getElementById('alert-list');
    const countText = document.getElementById('alert-count');
    const filterDeviceId = document.getElementById('device-filter')?.value.trim();

    const filtered = allAlerts.filter(alert => {
        const alertType = alert.type ? alert.type.toLowerCase() : "";
        const matchTab = currentFilter === 'all' || alertType === currentFilter;

        // Đa thiết bị: Kiểm tra xem ID của cảnh báo có nằm trong danh sách thiết bị của user không
        let matchUserOwnership = true;
        if (alertType !== "hệ thống") {
            // Xem số thiết bị có nằm trong mảng userDeviceIds không
            matchUserOwnership = userDeviceIds.includes(Number(alert.device_id));
        }

        // Lọc theo thanh tìm kiếm ID trên màn hình
        let matchSearchInput = true;
        if (filterDeviceId && filterDeviceId !== "" && alertType !== "hệ thống") {
            matchSearchInput = String(alert.device_id) === filterDeviceId;
        }

        return matchTab && matchUserOwnership && matchSearchInput;
    });

    countText.innerText = `Hiện có ${filtered.length} thông báo ${currentFilter !== 'all' ? currentFilter : ''}`;
    listContainer.innerHTML = "";

    if (filtered.length === 0) {
        listContainer.innerHTML = `<div class="text-center p-10 text-slate-500 italic">Không có thông báo nào phù hợp cho thiết bị của bạn.</div>`;
        return;
    }

    filtered.forEach(alert => {
        const card = document.createElement('div');
        card.className = `alert-card ${alert.is_read ? '' : 'unread'}`;
        
        let iconClass = "fas fa-bell";
        let colorClass = "type-default";
        
        const typeKey = alert.type ? alert.type.toLowerCase() : "";
        if (typeKey === "hệ thống") {
            iconClass = "fas fa-shield-halved";
            colorClass = "type-system";
        } else if (typeKey === "thiết bị") {
            iconClass = "fas fa-microchip";
            colorClass = "type-device";
        } else if (typeKey === "offline") {
            iconClass = "fas fa-plug-circle-xmark";
            colorClass = "type-offline";
        }

        const dateObj = new Date(alert.created_at);
        const timeStr = dateObj.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
        const dateStr = dateObj.toLocaleDateString('vi-VN');

        card.innerHTML = `
            <div class="icon-box ${colorClass}">
                <i class="${iconClass}"></i>
            </div>
            <div class="flex-1">
                <div class="flex items-center gap-2">
                    <span class="device-badge">${typeKey === 'hệ thống' ? 'Toàn hệ thống' : 'ID thiết bị: ' + alert.device_id}</span>
                    <span class="text-[10px] opacity-40">#${alert.id}</span>
                </div>
                <h3 class="font-bold text-slate-200 capitalize">${alert.type || 'Thông báo'}</h3>
                <p class="text-sm text-slate-400 mt-0.5">${alert.message}</p>
            </div>
            <div class="text-right flex flex-col justify-center border-l border-white/5 pl-4 ml-2">
                <span class="text-xs font-bold text-slate-300">${timeStr}</span>
                <span class="text-[10px] text-slate-500 uppercase font-semibold">${dateStr}</span>
            </div>
        `;
        listContainer.appendChild(card);
    });
}

window.onload = fetchAlerts;
setInterval(fetchAlerts, 30000);