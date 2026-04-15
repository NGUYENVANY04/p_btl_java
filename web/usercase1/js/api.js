const API = "http://localhost:8080/api/devices";

// Mock data for demo purposes
let mockDevices = [
    { id: 1, name: "Thiết bị 1", location: "Phòng A", status: "online" },
    { id: 2, name: "Thiết bị 2", location: "Phòng B", status: "online" },
    { id: 3, name: "Thiết bị 3", location: "Phòng C", status: "offline" }
];

let mockLimits = [
    { device_id: 1, max_power: 100, max_current: 10 },
    { device_id: 2, max_power: 150, max_current: 15 }
];

//// DEVICE API ////

async function getDevices() {
    const res = await fetch(API);
    if (!res.ok) throw new Error("Failed to fetch devices");
    return await res.json();
}

async function createDevice(data) {
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
        throw new Error(errText);
    }

    return await res.json(); // nhận device vừa tạo
}
/**
 * Xóa thiết bị
 */
async function deleteDevice(id) {
    try {
        const response = await fetch(`${API}/${id}`, { method: "DELETE" });
        if (!response.ok) throw new Error("Failed to delete");
    } catch (err) {
        console.error("Delete error:", err);
    }
}

async function updateDevice(id, data) {
    const res = await axios.put(`${API}/devices/${id}`, {
        name: data.name,
        location: data.location
    });
    return res.data;
}

//// DEVICE LIMIT API ////

async function getLimits() {
    await new Promise(resolve => setTimeout(resolve, 100));
    return mockLimits;
}

async function setLimit(data) {
    const existing = mockLimits.find(l => l.device_id === data.device_id);
    if (existing) {
        existing.max_power = data.max_power;
        existing.max_current = data.max_current;
    } else {
        mockLimits.push({
            device_id: data.device_id,
            max_power: data.max_power,
            max_current: data.max_current
        });
    }
    await new Promise(resolve => setTimeout(resolve, 100));
}

// Alias for compatibility
async function saveLimit(deviceId, maxPower, maxCurrent) {
    return setLimit({
        device_id: deviceId,
        max_power: maxPower,
        max_current: maxCurrent
    });
}