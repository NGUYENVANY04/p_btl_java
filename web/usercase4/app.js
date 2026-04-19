// ================== SUPABASE CONFIG ==================
const SUPABASE_URL = "https://znfxhbrkabxenuzrcogd.supabase.co";
const ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpuZnhoYnJrYWJ4ZW51enJjb2dkIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NTQ0Mjk4NSwiZXhwIjoyMDkxMDE4OTg1fQ.qNtUEq0HebqEs6tHrWT6Ghj94-UOb5dshIWAvWB8r6Y';

const supa = supabase.createClient(SUPABASE_URL, ANON_KEY);

const statusDiv = document.getElementById("status");
const dashboard = document.getElementById("device-dashboard");

// ================== INIT ==================
async function initDashboard() {

    const user = JSON.parse(sessionStorage.getItem("currentUser"));
    if (!user || !user.id) {
        statusDiv.innerHTML = "❌ Chưa đăng nhập";
        return;
    }

    try {
        const { data: links, error: linkError } = await supa
            .from("user_devices")
            .select("*")
            .eq("user_id", user.id);

        if (linkError) {
            statusDiv.innerHTML = "❌ Lỗi Supabase";
            return;
        }

        const deviceIds = links.map(l => Number(l.device_id));

        const res = await fetch(`${document.getElementById("api").value}/api/devices`);
        const allDevices = await res.json();

        const devices = allDevices.filter(d => deviceIds.includes(Number(d.id)));

        dashboard.innerHTML = "";

        devices.forEach(device => {
            const id = device.id;
            const name = device.name || `Thiết bị ${id}`;

            const card = document.createElement("div");
            card.className = "card";

            // Tìm đoạn tạo innerHTML cho các nút BẬT/TẮT và sửa thành:
            // TRUYỀN 3 THAM SỐ: event, id, status
            // Tìm đoạn render Card và sửa phần btn-group thành:
            card.innerHTML = `
    <div style="display:flex;justify-content:space-between;align-items:center;">
        <div><strong>🖥️ ${name}</strong> (ID: ${id})</div>

        <div class="btn-group">
            <a href="javascript:void(0)" class="btn-on" 
               style="text-decoration:none; cursor:pointer; padding:5px 12px; background:green; color:white; border-radius:4px; display:inline-block;" 
               onclick="controlDevice(${id}, 'ON')">BẬT</a>
            
            <a href="javascript:void(0)" class="btn-off" 
               style="text-decoration:none; cursor:pointer; padding:5px 12px; background:red; color:white; border-radius:4px; margin-left:5px; display:inline-block;" 
               onclick="controlDevice(${id}, 'OFF')">TẮT</a>
        </div>
    </div>

    <div id="ctrl-status-${id}" class="status-box" style="margin-top:10px;">Trạng thái: Sẵn sàng</div>
    ...
`;
            dashboard.appendChild(card);
            loadData(id);
        });

        statusDiv.innerHTML = "🟢 Loaded OK";

    } catch (err) {
        statusDiv.innerHTML = "❌ Error init";
    }
}

// ================== LOAD DATA ==================
async function loadData(deviceId) {

    const limit = document.getElementById("limit").value;

    const { data } = await supa
        .from("sensor_data")
        .select("*")
        .eq("device_id", deviceId)
        .order("id", { ascending: false })
        .limit(limit);

    const body = document.getElementById(`table-body-${deviceId}`);
    body.innerHTML = "";

    data.reverse().forEach(d => {
        const row = body.insertRow(0);
        row.innerHTML = `
            <td>${d.id}</td>
            <td>${d.voltage}</td>
            <td>${d.current}</td>
            <td>${d.power}</td>
            <td>${d.energy}</td>
            <td>${new Date(d.created_at).toLocaleTimeString()}</td>
        `;
    });
}
// Sửa lại khai báo hàm controlDevice
// NHẬN 3 THAM SỐ: event, id, status
// Chỉ cần 2 tham số: id và status
window.controlDevice = async function (id, status) {
    const el = document.getElementById(`ctrl-status-${id}`);
    if (el) el.innerHTML = "⏳ Sending...";

    try {
        const res = await fetch(`http://localhost:8080/api/control`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                deviceId: id,
                status: status
            })
        });

        if (res.ok) {
            if (el) el.innerHTML = "✅ OK";
        } else {
            if (el) el.innerHTML = "❌ FAIL";
        }
    } catch (e) {
        if (el) el.innerHTML = "❌ ERROR";
        console.error("Lỗi điều khiển:", e);
    }

    return false; // Chặn mọi hành động mặc định của trình duyệt
};
// ================== REALTIME ==================
function startRealtime() {

    supa.channel("sensor")
        .on("postgres_changes", {
            event: "INSERT",
            schema: "public",
            table: "sensor_data"
        }, payload => {
            const d = payload.new;
            const body = document.getElementById(`table-body-${d.device_id}`);
            if (!body) return;

            const row = body.insertRow(0);
            row.innerHTML = `
                <td>${d.id}</td>
                <td>${d.voltage}</td>
                <td>${d.current}</td>
                <td>${d.power}</td>
                <td>${d.energy}</td>
                <td>${new Date(d.created_at).toLocaleTimeString()}</td>
            `;
        })
        .subscribe();
}
window.manualCheck = function () {
    console.log("Refreshing dashboard...");
    initDashboard();
};
// ================== INIT ==================
initDashboard();
startRealtime();