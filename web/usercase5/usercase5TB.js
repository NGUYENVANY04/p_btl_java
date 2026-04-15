const API_URL = "http://localhost:8080/api/quochoc/alerts";

async function fetchAlerts() {
    const listContainer = document.getElementById('alert-list');
    const countText = document.getElementById('alert-count');

    try {
        const response = await fetch(API_URL);
        if (!response.ok) throw new Error("Network response was not ok");
        
        const alerts = await response.json();
        
        // Cập nhật số lượng thông báo
        countText.innerText = `Hiện có ${alerts.length} thông báo`;
        
        // Xóa danh sách cũ
        listContainer.innerHTML = "";

        if (alerts.length === 0) {
            listContainer.innerHTML = `<div class="text-center p-10 text-slate-500">Không có thông báo nào.</div>`;
            return;
        }

        alerts.forEach(alert => {
            const card = document.createElement('div');
            card.className = `alert-card ${alert.is_read ? '' : 'unread'}`;
            
            // Xác định icon và màu sắc theo loại
            let iconClass = "fas fa-bell";
            let colorClass = "type-default";
            
            if (alert.type === "OFFLINE") {
                iconClass = "fas fa-plug-circle-xmark";
                colorClass = "type-offline";
            } else if (alert.type.includes("voltage") || alert.message.toLowerCase().includes("voltage")) {
                iconClass = "fas fa-bolt-lightning";
                colorClass = "type-voltage";
            }

            // Format thời gian (Vd: 2026-04-07T04:05 -> 04:05, 07/04/2026)
            const date = new Date(alert.created_at);
            const timeStr = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
            const dateStr = date.toLocaleDateString('vi-VN');

            card.innerHTML = `
                <div class="icon-box ${colorClass}">
                    <i class="${iconClass}"></i>
                </div>
                <div class="flex-1">
                    <span class="device-badge">Device ID: ${alert.device_id}</span>
                    <h3 class="font-bold text-slate-200">${alert.type.toUpperCase()}</h3>
                    <p class="text-sm text-slate-400 mt-1">${alert.message}</p>
                </div>
                <div class="text-right flex flex-col justify-center">
                    <span class="text-xs font-bold text-slate-300">${timeStr}</span>
                    <span class="text-[10px] text-slate-500 uppercase font-semibold">${dateStr}</span>
                </div>
            `;
            listContainer.appendChild(card);
        });

    } catch (error) {
        console.error("Lỗi fetch alerts:", error);
        countText.innerText = "Lỗi kết nối API";
        listContainer.innerHTML = `<div class="text-center p-10 text-red-400">Không thể lấy dữ liệu từ ${API_URL}. Vui lòng kiểm tra Server.</div>`;
    }
}

// Tự động load khi mở trang
window.onload = fetchAlerts;

// Tự động làm mới mỗi 30 giây
setInterval(fetchAlerts, 30000);