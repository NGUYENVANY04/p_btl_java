// ================== SUPABASE CONFIG ==================
const SUPABASE_URL = "https://znfxhbrkabxenuzrcogd.supabase.co";
const ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpuZnhoYnJrYWJ4ZW51enJjb2dkIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NTQ0Mjk4NSwiZXhwIjoyMDkxMDE4OTg1fQ.qNtUEq0HebqEs6tHrWT6Ghj94-UOb5dshIWAvWB8r6Y';

const supa = supabase.createClient(SUPABASE_URL, ANON_KEY);

const statusDiv = document.getElementById("status");
const dashboard = document.getElementById("device-dashboard");

document.addEventListener("DOMContentLoaded", () => {
    console.log("🚀 Hệ thống IoT đang khởi động...");

    // refreshAll();
    startRealtime();
});
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

        if (!links || links.length === 0) {
            dashboard.innerHTML = "⚠️ User chưa có thiết bị";
            return;
        }

        const deviceIds = links.map(l => Number(l.device_id));

        const api = document.getElementById("api").value;
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
function renderDevice(device) {

    const container = document.getElementById("device-dashboard");

    const card = document.createElement("div");
    card.className = "device-card";
    card.id = `card-${device.id}`;

    card.innerHTML = `
        <!-- HEADER -->
        <div style="display:flex;justify-content:space-between;align-items:center;">
            <div>
                <strong>🖥️ ${device.name || 'Không tên'}</strong> (ID: ${device.id})
            </div>

            <div class="btn-group">
                <button class="btn-on" onclick="controlDevice(${device.id}, 'ON')">BẬT</button>
                <button class="btn-off" onclick="controlDevice(${device.id}, 'OFF')">TẮT</button>
            </div>
        </div>

        <!-- STATUS -->
        <div id="ctrl-status-${device.id}" class="status-box" style="margin-top:10px;">
            Trạng thái: Sẵn sàng
        </div>

        <!-- TABLE -->
        <div class="table-container" style="margin-top:10px;">
            <table>
                <thead>
                    <tr>
                        <th>Mã tin</th>
                        <th>Điện áp (V)</th>
                        <th>Dòng điện (A)</th>
                        <th>Công suất (W)</th>
                        <th>Số Điện </th>
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

    container.appendChild(card);
}


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

            [...data].reverse().forEach(row => {
                updateTable(id, row, false);
            });
        }
    }
}


function updateTable(id, d, isNew = false) {

    const tbody = document.getElementById(`tb-${id}`);
    if (!tbody) return;

    if (
        tbody.rows.length === 1 &&
        (tbody.rows[0].cells.length === 1 ||
            tbody.innerText.includes("Đang đợi"))
    ) {
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

    const limitInput = document.getElementById('limit');
    const limit = limitInput ? parseInt(limitInput.value) : 10;

    while (tbody.rows.length > limit) {
        tbody.deleteRow(tbody.rows.length - 1);
    }
}
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

    return false;
};
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

window.manualCheck = function () {
    console.log("Refreshing dashboard...");
    initDashboard();
};

initDashboard();
startRealtime();