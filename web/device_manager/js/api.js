const API = "http://localhost:8080/api/devices";
const API_Threshold = "http://localhost:8080/api/device_limits";
const SB_URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1/user_devices";
const ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpuZnhoYnJrYWJ4ZW51enJjb2dkIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NTQ0Mjk4NSwiZXhwIjoyMDkxMDE4OTg1fQ.qNtUEq0HebqEs6tHrWT6Ghj94-UOb5dshIWAvWB8r6Y';

async function getDevices() {
    // 1. Lấy User ID đang đăng nhập
    const user = JSON.parse(sessionStorage.getItem("currentUser"));
    if (!user || !user.id) throw new Error("Chưa đăng nhập");


    try {
        const res = await fetch(`${SB_URL}?user_id=eq.${user.id}`, {
            method: "GET",
            headers: {
                "apikey": ANON_KEY,
                "Authorization": `Bearer ${ANON_KEY}`
            }
        });

        const myDeviceLinks = await res.json();
        if (myDeviceLinks.length === 0) return [];
        const allDevicesRes = await fetch(API);
        const allDevices = await allDevicesRes.json();
        const myDeviceIds = myDeviceLinks.map(link => link.device_id);
        const myDevices = allDevices.filter(device => myDeviceIds.includes(device.id));
        return myDevices;

    } catch (error) {
        console.error("Lỗi đổ dữ liệu:", error);
        return [];
    }
}
async function createDevice(data) {
    // 1. Lấy thông tin User ID đã lưu từ lúc đăng nhập
    const user = JSON.parse(sessionStorage.getItem("currentUser"));
    if (!user || !user.id) {
        throw new Error("Không tìm thấy User ID. Vui lòng đăng nhập lại.");
    }

    // Thông tin Supabase để gán quyền

    try {
        // --- API 1: TẠO THIẾT BỊ (API hiện tại của bạn) ---
        const res = await fetch(API, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                name: data.name,
                location: data.location,
                status: "true"
            })
        });

        if (!res.ok) {
            const errText = await res.text();
            throw new Error("Lỗi API tạo thiết bị: " + errText);
        }

        const newDevice = await res.json();
        const newDeviceId = newDevice.id; // Lấy ID vừa tạo (ví dụ: 56)
        console.log("Bước 1: Đã tạo thiết bị ID", newDeviceId);

        // --- API 2: GÁN QUYỀN (Gọi trực tiếp Supabase) ---
        const res2 = await fetch(SB_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "apikey": ANON_KEY,
                "Authorization": `Bearer ${ANON_KEY}`
            },
            body: JSON.stringify({
                user_id: user.id,      // ID của bạn (ví dụ: 3)
                device_id: newDeviceId // ID thiết bị vừa tạo (ví dụ: 56)
            })
        });

        if (!res2.ok) {
            const errorLog = await res2.json();
            throw new Error("Bước 2: Gán quyền thất bại - " + JSON.stringify(errorLog));
        }

        console.log("Bước 2: Đã gán thiết bị cho User", user.id);

        // Trả về dữ liệu thiết bị ban đầu để các hàm giao diện sử dụng
        return newDevice;

    } catch (error) {
        console.error("Lỗi luồng tạo & gán:", error.message);
        throw error; // Đẩy lỗi ra ngoài để xử lý alert nếu cần
    }
}
async function deleteDevice(id) {
    try {
        const response = await fetch(`${API}/${id}`,
            { method: "DELETE" });
        if (!response.ok) throw new Error("Failed to delete");
    } catch (err) {
        console.error("Delete error:", err);
    }
}

async function updateDevice(id, data) {
    const res = await fetch(`${API}/${id}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            id: id,
            name: data.name,
            location: data.location
        })
    });

    if (!res.ok) {
        const errText = await res.text();
        throw new Error(errText);
    }

    return await res.json();
}

async function getLimits() {
    const res = await fetch(API_Threshold);
    if (!res.ok) throw new Error("Failed to fetch devices");
    return await res.json();
}

async function setLimit(data) {
    try {
        const response = await fetch(`${API_Threshold}/${data.device_id}`, {

            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                id: data.device_id,
                device_id: data.device_id,
                max_power: data.max_power,
                max_current: data.max_current
            })
        });

        if (!response.ok) {
            throw new Error("API error: " + response.status);
        }

        const result = await response.json();
        console.log("Saved:", result);

    } catch (error) {
        console.error("Error saving limit:", error);
    }
}

async function saveLimit(deviceId, maxPower, maxCurrent) {
    console.log("meo chay toi");
    return setLimit({
        device_id: deviceId,
        max_power: maxPower,
        max_current: maxCurrent
    });
}

// ================= ALERTS API =================
async function checkOfflineDevices(seconds = 4) {
    const user = JSON.parse(sessionStorage.getItem("currentUser"));
    if (!user || !user.id) throw new Error("Chưa đăng nhập");

    const res = await fetch(`${API.replace('/devices', '')}/daocuong/devices/offline/check?seconds=${seconds}&userId=${user.id}`);
    if (!res.ok) throw new Error("Failed to check offline devices");
    return await res.json();
}

async function checkThresholdAlerts() {
    const user = JSON.parse(sessionStorage.getItem("currentUser"));
    if (!user || !user.id) throw new Error("Chưa đăng nhập");

    const res = await fetch(`${API.replace('/devices', '')}/daocuong/threshold/check?userId=${user.id}`);
    if (!res.ok) throw new Error("Failed to check threshold alerts");
    return await res.json();
}

async function getAlerts(limit = 50) {
    const user = JSON.parse(sessionStorage.getItem("currentUser"));
    if (!user || !user.id) throw new Error("Chưa đăng nhập");

    const res = await fetch(`${API.replace('/devices', '')}/daocuong/alerts?limit=${limit}&userId=${user.id}`);
    if (!res.ok) throw new Error("Failed to fetch alerts");
    return await res.json();
}