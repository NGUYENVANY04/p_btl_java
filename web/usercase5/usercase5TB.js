const API_URL = "http://localhost:8080/api/quochoc/alerts";
let allAlerts = []; // Biến lưu trữ dữ liệu gốc
let currentFilter = 'all'; // Bộ lọc hiện tại

async function fetchAlerts() {
    const listContainer = document.getElementById('alert-list');
    const countText = document.getElementById('alert-count');

    try {
        const response = await fetch(API_URL);
        if (!response.ok) throw new Error("Không thể kết nối API");
        
        allAlerts = await response.json();
        
        // Render lại danh sách dựa trên bộ lọc đang chọn
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
    
    // Cập nhật giao diện nút Tab
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    if (type === 'all') document.getElementById('tab-all').classList.add('active');
    else if (type === 'hệ thống') document.getElementById('tab-system').classList.add('active');
    else if (type === 'thiết bị') document.getElementById('tab-device').classList.add('active');

    renderAlerts();
}

function renderAlerts() {
    const listContainer = document.getElementById('alert-list');
    const countText = document.getElementById('alert-count');

    // Thực hiện lọc mảng allAlerts
    const filtered = currentFilter === 'all' 
        ? allAlerts 
        : allAlerts.filter(item => item.type.toLowerCase() === currentFilter.toLowerCase());

    countText.innerText = `Hiện có ${filtered.length} thông báo ${currentFilter !== 'all' ? currentFilter : ''}`;
    listContainer.innerHTML = "";

    if (filtered.length === 0) {
        listContainer.innerHTML = `<div class="text-center p-10 text-slate-500 italic">Không có thông báo nào phù hợp.</div>`;
        return;
    }

    filtered.forEach(alert => {
        const card = document.createElement('div');
        // Sử dụng is_read (từ dữ liệu của bạn) để xác định trạng thái chưa đọc
        card.className = `alert-card ${alert.is_read ? '' : 'unread'}`;
        
        // Xác định Icon và Màu sắc dựa trên type
        let iconClass = "fas fa-bell";
        let colorClass = "type-default";
        
        const typeKey = alert.type.toLowerCase();
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

        // Định dạng thời gian (Dữ liệu của bạn: 2026-04-19 16:36:13)
        const dateObj = new Date(alert.created_at);
        const timeStr = dateObj.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
        const dateStr = dateObj.toLocaleDateString('vi-VN');

        card.innerHTML = `
            <div class="icon-box ${colorClass}">
                <i class="${iconClass}"></i>
            </div>
            <div class="flex-1">
                <div class="flex items-center gap-2">
                    <span class="device-badge">ID thiết bị: ${alert.device_id}</span>
                    <span class="text-[10px] opacity-40">#${alert.id}</span>
                </div>
                <h3 class="font-bold text-slate-200 capitalize">${alert.type}</h3>
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

// Khởi tạo
window.onload = fetchAlerts;

// Tự động làm mới mỗi 30 giây nhưng giữ nguyên bộ lọc đang chọn
setInterval(fetchAlerts, 30000);