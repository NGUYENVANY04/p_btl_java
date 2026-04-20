// ================== SUPABASE CONFIG ==================
const SUPABASE_URL = "https://znfxhbrkabxenuzrcogd.supabase.co";
const ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpuZnhoYnJrYWJ4ZW51enJjb2dkIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NTQ0Mjk4NSwiZXhwIjoyMDkxMDE4OTg1fQ.qNtUEq0HebqEs6tHrWT6Ghj94-UOb5dshIWAvWB8r6Y';


const supa = supabase.createClient(SUPABASE_URL, ANON_KEY);

const statusDiv = document.getElementById("status");
const dashboard = document.getElementById("device-dashboard");

// ================== GLOBAL STATE ==================
let realtimeChannel = null;

// ================== INIT ==================
document.addEventListener("DOMContentLoaded", async () => {
    console.log("🚀 Hệ thống IoT đang khởi động...");

    await initDashboard();
    startRealtime();
});

// ================== DASHBOARD ==================
async function initDashboard() {

    const statusDiv = document.getElementById("status");
    const dashboard = document.getElementById("device-dashboard");

    dashboard.innerHTML = "⏳ Đang tải...";

    const user = JSON.parse(sessionStorage.getItem("currentUser"));

    if (!user || !user.id) {
        statusDiv.innerHTML = "❌ Chưa đăng nhập";
        return;
    }

    try {
        const { data: links, error } = await supa
            .from("user_devices")
            .select("*")
            .eq("user_id", user.id);

        if (error) {
            statusDiv.innerHTML = "❌ Lỗi Supabase";
            return;
        }

        if (!links?.length) {
            dashboard.innerHTML = "⚠️ User chưa có thiết bị";
            return;
        }

        const deviceIds = links.map(l => Number(l.device_id));

        const api = document.getElementById("api")?.value;
        const res = await fetch(`${api}/api/devices`);
        const allDevices = await res.json();

        const devices = allDevices.filter(d =>
            deviceIds.includes(Number(d.id))
        );

        dashboard.innerHTML = "";

        devices.forEach(device => {
            renderDevice(device);
            fetchHistory(device.id, 10);
        });

        statusDiv.innerHTML = "🟢 Loaded OK";

    } catch (err) {
        console.error(err);
        statusDiv.innerHTML = "❌ Error init";
    }
}

// ================== RENDER DEVICE ==================
function renderDevice(device) {

    const container = document.getElementById("device-dashboard");

    const card = document.createElement("div");
    card.className = "device-card";
    card.id = `card-${device.id}`;

    card.innerHTML = `
        <div style="display:flex;justify-content:space-between;align-items:center;">
            <div>
                <strong>🖥️ ${device.name || 'Không tên'}</strong> (ID: ${device.id})
            </div>

            <div class="btn-group">
                <button class="btn-on" onclick="controlDevice(${device.id}, 'ON')">BẬT</button>
                <button class="btn-off" onclick="controlDevice(${device.id}, 'OFF')">TẮT</button>
            </div>
        </div>

        <div id="ctrl-status-${device.id}" class="status-box" style="margin-top:10px;">
            Trạng thái: Sẵn sàng
        </div>

        <div class="table-container" style="margin-top:10px;">
            <table>
                <thead>
                    <tr>
                        <th>Mã tin</th>
                        <th>Điện áp</th>
                        <th>Dòng</th>
                        <th>Công suất</th>
                        <th>Energy</th>
                        <th>Thời gian</th>
                    </tr>
                </thead>
                <tbody id="tb-${device.id}">
                    <tr><td colspan="6">Đang đợi dữ liệu...</td></tr>
                </tbody>
            </table>
        </div>
    `;

    container.appendChild(card);
}

// ================== HISTORY ==================
async function fetchHistory(id, limit) {

    const { data, error } = await supa
        .from("sensor_data")
        .select("*")
        .eq("device_id", id)
        .order("created_at", { ascending: false })
        .limit(limit);

    if (error) {
        console.error(error);
        return;
    }

    const tbody = document.getElementById(`tb-${id}`);
    if (!tbody) return;

    tbody.innerHTML = "";

    [...(data || [])].reverse().forEach(row => {
        updateTable(id, row, false);
    });
}

// ================== UPDATE TABLE ==================
function updateTable(id, d, isNew = false) {

    const tbody = document.getElementById(`tb-${id}`);
    if (!tbody) return;

    if (tbody.innerText.includes("Đang đợi")) {
        tbody.innerHTML = "";
    }

    const r = tbody.insertRow(0);
    if (isNew) r.classList.add("new-row");

    r.innerHTML = `
        <td>#${d.id}</td>
        <td>${d.voltage ?? 0}</td>
        <td>${d.current ?? 0}</td>
        <td>${d.power ?? 0}</td>
        <td>${d.energy ?? 0}</td>
        <td>${new Date().toLocaleTimeString()}</td>
    `;

    const limit = parseInt(document.getElementById("limit")?.value || 10);

    while (tbody.rows.length > limit) {
        tbody.deleteRow(-1);
    }
}

// ================== CONTROL DEVICE ==================
window.controlDevice = async function (id, status) {

    const el = document.getElementById(`ctrl-status-${id}`);
    if (el) el.innerHTML = "⏳ Sending...";

    try {
        const res = await fetch("http://localhost:8080/api/control", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ deviceId: id, status })
        });

        if (el) {
            el.innerHTML = res.ok ? "✅ OK" : "❌ FAIL";
        }

    } catch (e) {
        if (el) el.innerHTML = "❌ ERROR";
        console.error(e);
    }
};

// ================== REALTIME (FIXED) ==================
function startRealtime() {

    if (realtimeChannel) {
        supa.removeChannel(realtimeChannel);
    }

    realtimeChannel = supa.channel("iot-public-channel")
        .on(
            "postgres_changes",
            {
                event: "INSERT",
                schema: "public",
                table: "sensor_data"
            },
            (payload) => {
                console.log("📡 Realtime:", payload.new);
                updateTable(payload.new.device_id, payload.new, true);
            }
        )
        .subscribe((status) => {
            console.log("Realtime status:", status);
        });
}

// ================== MANUAL ==================
window.manualCheck = function () {
    initDashboard();
    startRealtime();
};