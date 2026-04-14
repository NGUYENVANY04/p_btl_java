const API = "http://localhost:8000/api";

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

async function getDevices(){
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 100));
    return mockDevices;
}

async function createDevice(data){
    const newId = mockDevices.length > 0 ? Math.max(...mockDevices.map(d => d.id)) + 1 : 1;
    mockDevices.push({ id: newId, name: data.name, location: data.location, status: "online" });
    await new Promise(resolve => setTimeout(resolve, 100));
}

async function deleteDevice(id){
    mockDevices = mockDevices.filter(d => d.id !== id);
    await new Promise(resolve => setTimeout(resolve, 100));
}

async function updateDevice(id, data){
    const device = mockDevices.find(d => d.id === id);
    if (device) {
        device.name = data.name;
        device.location = data.location;
    }
    await new Promise(resolve => setTimeout(resolve, 100));
}

//// DEVICE LIMIT API ////

async function getLimits(){
    await new Promise(resolve => setTimeout(resolve, 100));
    return mockLimits;
}

async function setLimit(data){
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