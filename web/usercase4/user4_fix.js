// ========================== 1. CẤU HÌNH HỆ THỐNG ==========================
const CONFIG = {
    // URL project Supabase (database cloud realtime)
    SB_URL: 'https://znfxhbrkabxenuzrcogd.supabase.co',

    // API key để truy cập Supabase (anon key)
    SB_KEY: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',

    // URL backend Spring Boot (API điều khiển thiết bị)
    API_URL: 'http://localhost:8080'
};


// ========================== 2. KHỞI TẠO SUPABASE CLIENT ==========================
const supa = supabase.createClient(CONFIG.SB_URL, CONFIG.SB_KEY);
// 👉 Tạo kết nối giữa frontend và Supabase để truy vấn dữ liệu + realtime


// ========================== 3. KHỞI ĐỘNG ỨNG DỤNG ==========================
document.addEventListener("DOMContentLoaded", () => {
    console.log("🚀 Hệ thống IoT đang khởi động...");

    // Load toàn bộ thiết bị + dữ liệu ban đầu
    refreshAll();

    // Mở kết nối realtime để nhận dữ liệu mới liên tục
    startRealtime();
});


// ========================== 4. LÀM MỚI TOÀN BỘ DASHBOARD ==========================
async function refreshAll() {

    // Lấy element hiển thị trạng thái kết nối
    const statusEl = document.getElementById("conn-status");

    // Lấy giới hạn số dòng hiển thị trong bảng
    const rowLimitInput = document.getElementById('row-limit');
    const rowLimit = rowLimitInput ? parseInt(rowLimitInput.value) : 10;

    // Hiển thị trạng thái đang cập nhật
    if (statusEl) {
        statusEl.innerText = "🔄 Đang cập nhật...";
        statusEl.style.color = "#ffcc00";
    }

    try {
        // =================== GỌI BACKEND SPRING BOOT ===================
        const response = await fetch(`${CONFIG.API_URL}/api/devices`);

        if (!response.ok) throw new Error("Không thể kết nối Backend");

        // Danh sách thiết bị đang hoạt động
        const activeDevices = await response.json();

        // Container chứa dashboard
        const container = document.getElementById("dashboard-container");
        if (!container) return;

        // Xóa UI cũ trước khi render lại
        container.innerHTML = "";

        // =================== RENDER TẤT CẢ THIẾT BỊ ===================
        await Promise.all(activeDevices.map(async (device) => {

            // Tạo card UI cho từng thiết bị
            renderDevice(device);

            // Lấy dữ liệu lịch sử của từng thiết bị từ Supabase
            await fetchHistory(device.id, rowLimit);
        }));

        // Hiển thị trạng thái thành công
        if (statusEl) {
            statusEl.innerText = "🟢 Trực tuyến";
            statusEl.style.color = "#2ecc71";
        }

    } catch (err) {
        console.error("Lỗi Refresh:", err);

        // Hiển thị lỗi nếu backend không hoạt động
        if (statusEl) {
            statusEl.innerText = "❌ Lỗi kết nối Server";
            statusEl.style.color = "#e74c3c";
        }
    }
}


// ========================== 5. RENDER THIẾT BỊ (UI CARD) ==========================
function renderDevice(device) {

    const container = document.getElementById("dashboard-container");

    // Tạo card thiết bị mới
    const card = document.createElement("div");
    card.className = "device-card";
    card.id = `card-${device.id}`;

    // Nội dung HTML của card
    card.innerHTML = `
        <div class="device-header">

            <!-- Tên thiết bị -->
            <div class="device-name">
                THIẾT BỊ: ${device.name || 'Không tên'} (ID: ${device.id})
            </div>

            <!-- Công tắc ON/OFF điều khiển thiết bị -->
            <label class="switch">
                <input type="checkbox" ${device.status === 'ON' ? 'checked' : ''} 
                       onchange="control(${device.id}, this)">
                <span class="slider"></span>
            </label>

        </div>

        <!-- Trạng thái gửi lệnh -->
        <div id="msg-${device.id}" class="status-msg">
            Sẵn sàng
        </div>

        <!-- Bảng hiển thị dữ liệu sensor -->
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Mã tin</th>
                        <th>Điện áp (V)</th>
                        <th>Dòng điện (A)</th>
                        <th>Công suất (W)</th>
                        <th>Thời gian</th>
                    </tr>
                </thead>

                <tbody id="tb-${device.id}">
                    <tr>
                        <td colspan="5">Đang đợi dữ liệu...</td>
                    </tr>
                </tbody>
            </table>
        </div>
    `;

    // Thêm card vào dashboard
    container.appendChild(card);
}


// ========================== 6. LẤY DỮ LIỆU LỊCH SỬ SUPABASE ==========================
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

            // Đảo ngược để hiển thị theo thứ tự thời gian đúng
            [...data].reverse().forEach(row => updateTable(id, row, false));
        }
    }
}


// ========================== 7. CẬP NHẬT BẢNG DỮ LIỆU ==========================
function updateTable(id, d, isNew = false) {

    const tbody = document.getElementById(`tb-${id}`);
    if (!tbody) return;

    // Xóa dòng placeholder nếu còn tồn tại
    if (tbody.rows.length === 1 &&
        (tbody.rows[0].cells.length === 1 ||
            tbody.rows[0].innerText.includes("Đang đợi"))) {
        tbody.innerHTML = "";
    }

    // Thêm dòng mới lên đầu bảng
    const r = tbody.insertRow(0);
    if (isNew) r.classList.add("new-row");

    r.innerHTML = `
        <td>#${d.id}</td>
        <td>${d.voltage ?? 0}</td>
        <td>${d.current ?? 0}</td>
        <td>${d.power ?? 0}</td>
        <td>${new Date(d.created_at || Date.now()).toLocaleTimeString()}</td>
    `;

    // Giới hạn số dòng hiển thị
    const limitInput = document.getElementById('row-limit');
    const limit = limitInput ? parseInt(limitInput.value) : 10;

    while (tbody.rows.length > limit) {
        tbody.deleteRow(tbody.rows.length - 1);
    }
}


// ========================== 8. ĐIỀU KHIỂN THIẾT BỊ ==========================
async function control(id, el) {

    // Trạng thái ON/OFF
    const state = el.checked ? 'ON' : 'OFF';

    const msg = document.getElementById(`msg-${id}`);
    if (msg) msg.innerText = "⏳ Đang gửi lệnh...";

    try {
        // Gửi lệnh xuống backend Spring Boot
        const res = await fetch(
            `${CONFIG.API_URL}/api/device/control?deviceId=${id}&status=${state}`,
            { method: 'POST' }
        );

        if (!res.ok) throw new Error();

        // Thành công
        if (msg) {
            msg.innerText = `✅ Đã chuyển sang ${state}`;
            msg.style.color = "#2ecc71";
        }

    } catch (e) {

        // Lỗi thì rollback trạng thái UI
        if (msg) {
            msg.innerText = "❌ Lỗi: Gửi lệnh thất bại";
            msg.style.color = "#e74c3c";
        }

        el.checked = !el.checked;
    }

    // Reset trạng thái sau 2.5s
    setTimeout(() => {
        if (msg) {
            msg.innerText = "Sẵn sàng";
            msg.style.color = "#9bb3c9";
        }
    }, 2500);
}


// ========================== 9. REALTIME SUPABASE ==========================
function startRealtime() {

    supa.channel('iot-public-channel')

        // Lắng nghe khi có INSERT mới vào bảng sensor_data
        .on('postgres_changes',
            { event: 'INSERT', schema: 'public', table: 'sensor_data' },
            (payload) => {

                console.log("📡 Realtime data:", payload.new);

                // Cập nhật UI ngay lập tức
                updateTable(payload.new.device_id, payload.new, true);
            }
        )

        // Trạng thái kết nối realtime
        .subscribe((status) => {
            console.log("Realtime status:", status);
        });
}


// ========================== 10. WEBSOCKET NODEJS SERVER ==========================

const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080 });

// Khi client kết nối
wss.on('connection', (ws) => {
    console.log('Client đã kết nối');

    // Gửi dữ liệu giả lập mỗi 2 giây
    setInterval(() => {
        const data = JSON.stringify({
            price: Math.random().toFixed(2)
        });

        ws.send(data);
    }, 2000);
});