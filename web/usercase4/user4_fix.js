// 1. Cấu hình kết nối
const CONFIG = {
    SB_URL: 'https://znfxhbrkabxenuzrcogd.supabase.co',
    SB_KEY: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpuZnhoYnJrYWJ4ZW51enJjb2dkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU0NDI5ODUsImV4cCI6MjA5MTAxODk4NX0.oi6EOu7_IhJeC0jo9ACUIZaQKfsC3tupUVpF9w0DQiQ',
    API_URL: 'http://localhost:8080'
};

// 2. Khởi tạo Supabase Client
const supa = supabase.createClient(CONFIG.SB_URL, CONFIG.SB_KEY);

/**
 * Khởi tạo ứng dụng khi trang web tải xong
 */
document.addEventListener("DOMContentLoaded", () => {
    console.log("🚀 Hệ thống IoT đang khởi động...");
    refreshAll();    // Lấy danh sách thiết bị & dữ liệu cũ
    startRealtime(); // Mở kết nối WebSocket lắng nghe dữ liệu mới
});

/**
 * Làm mới danh sách thiết bị từ Backend Spring Boot
 */
async function refreshAll() {
    const statusEl = document.getElementById("conn-status");
    const rowLimitInput = document.getElementById('row-limit');
    const rowLimit = rowLimitInput ? parseInt(rowLimitInput.value) : 10;

    if (statusEl) {
        statusEl.innerText = "🔄 Đang cập nhật...";
        statusEl.style.color = "#ffcc00";
    }

    try {
        const response = await fetch(`${CONFIG.API_URL}/api/devices`);
        if (!response.ok) throw new Error("Không thể kết nối Backend");

        const activeDevices = await response.json();
        const container = document.getElementById("dashboard-container");
        if (!container) return;

        container.innerHTML = ""; // Xóa các card cũ

        // Load tất cả card thiết bị đồng thời
        await Promise.all(activeDevices.map(async (device) => {
            renderDevice(device);
            await fetchHistory(device.id, rowLimit);
        }));

        if (statusEl) {
            statusEl.innerText = "🟢 Trực tuyến";
            statusEl.style.color = "#2ecc71";
        }
    } catch (err) {
        console.error("Lỗi Refresh:", err);
        if (statusEl) {
            statusEl.innerText = "❌ Lỗi kết nối Server";
            statusEl.style.color = "#e74c3c";
        }
    }
}

/**
 * Vẽ giao diện Card cho từng thiết bị
 */
function renderDevice(device) {
    const container = document.getElementById("dashboard-container");
    const card = document.createElement("div");
    card.className = "device-card";
    card.id = `card-${device.id}`;
    card.innerHTML = `
        <div class="device-header">
            <div class="device-name">THIẾT BỊ: ${device.name || 'Không tên'} (ID: ${device.id})</div>
            <label class="switch">
                <input type="checkbox" ${device.status === 'ON' ? 'checked' : ''} 
                       onchange="control(${device.id}, this)">
                <span class="slider"></span>
            </label>
        </div>
<div id="msg-${device.id}" class="status-msg" style="font-size: 11px; color: #9bb3c9; margin-bottom: 10px;">Sẵn sàng</div>
        <div class="table-container">
            <table>
                <thead>
                    <tr><th>Mã tin</th><th>Điện áp (V)</th><th>Dòng điện (A)</th><th>Công suất (W)</th><th>Thời gian</th></tr>
                </thead>
                <tbody id="tb-${device.id}">
                    <tr><td colspan="5">Đang đợi dữ liệu...</td></tr>
                </tbody>
            </table>
        </div>
    `;
    container.appendChild(card);
}

/**
 * Lấy lịch sử dữ liệu từ Supabase Database
 */
async function fetchHistory(id, limit) {
    const { data, error } = await supa
        .from("sensor_data")
        .select("*")
        .eq("device_id", id)
        .order("created_at", { ascending: false })
        .limit(limit);

    if (error) {
        console.error(`Lỗi tải lịch sử cho ID ${id}:`, error);
        return;
    }

    if (data) {
        const tbody = document.getElementById(`tb-${id}`);
        if (tbody) {
            tbody.innerHTML = "";
            // Đảo ngược mảng để chèn vào đầu bảng theo đúng thứ tự thời gian
            [...data].reverse().forEach(row => updateTable(id, row, false));
        }
    }
}

/**
 * Cập nhật bảng dữ liệu (Dùng cho cả Lịch sử và Realtime)
 */
function updateTable(id, d, isNew = false) {
    const tbody = document.getElementById(`tb-${id}`);
    if (!tbody) return;

    // Xóa dòng thông báo "Đang đợi" nếu có
    if (tbody.rows.length === 1 && (tbody.rows[0].cells.length === 1 || tbody.rows[0].innerText.includes("Đang đợi"))) {
        tbody.innerHTML = "";
    }

    // Chèn dòng mới vào vị trí đầu tiên (index 0)
    const r = tbody.insertRow(0);
    if (isNew) r.classList.add("new-row");

    r.innerHTML = `
        <td>#${d.id}</td>
        <td>${d.voltage ?? 0}</td>
        <td>${d.current ?? 0}</td>
        <td>${d.power ?? 0}</td>
        <td>${new Date(d.created_at || Date.now()).toLocaleTimeString()}</td>
    `;

    // Giới hạn số dòng hiển thị dựa trên input row-limit
    const limitInput = document.getElementById('row-limit');
    const limit = limitInput ? parseInt(limitInput.value) : 10;
    while (tbody.rows.length > limit) {
        tbody.deleteRow(tbody.rows.length - 1);
    }
}

/**
 * Gửi lệnh điều khiển thiết bị (POST tới Spring Boot)
 */
async function control(id, el) {
    const state = el.checked ? 'ON' : 'OFF';
    const msg = document.getElementById(`msg-${id}`);
    if (msg) msg.innerText = "⏳ Đang gửi lệnh...";

    try {
        // Lưu ý: Dùng POST để đúng chuẩn thay đổi trạng thái
        const res = await fetch(`${CONFIG.API_URL}/api/device/control?deviceId=${id}&status=${state}`, {
            method: 'POST'
        });

        if (!res.ok) throw new Error();

        if (msg) {
msg.innerText = `✅ Đã chuyển sang ${state}`;
            msg.style.color = "#2ecc71";
        }
    } catch (e) {
        if (msg) {
            msg.innerText = "❌ Lỗi: Gửi lệnh thất bại";
            msg.style.color = "#e74c3c";
        }
        el.checked = !el.checked; // Revert nút gạt nếu lỗi
    } finally {
        setTimeout(() => {
            if (msg) {
                msg.innerText = "Sẵn sàng";
                msg.style.color = "#9bb3c9";
            }
        }, 2500);
    }
}

/**
 * Lắng nghe dữ liệu WebSocket (Realtime)
 */
function startRealtime() {
    supa.channel('iot-public-channel')
        .on('postgres_changes',
            { event: 'INSERT', schema: 'public', table: 'sensor_data' },
            (payload) => {
                console.log("📡 Nhận dữ liệu Realtime:", payload.new);
                updateTable(payload.new.device_id, payload.new, true);
            }
        )
        .subscribe((status) => {
            console.log("Kênh Realtime:", status);
        });
}
const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080 });

wss.on('connection', (ws) => {
    console.log('Có người kết nối rồi nè!');

    // Gửi dữ liệu định kỳ cho client mà không cần client hỏi
    setInterval(() => {
        const data = JSON.stringify({ price: Math.random().toFixed(2) });
        ws.send(data);
    }, 2000);
});