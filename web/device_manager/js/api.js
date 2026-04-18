const API = "http://localhost:8080/api/devices";
const API_Threshold = "http://localhost:8080/api/device_limits";

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
    console.log("DEVICE RESPONSE:");

    return await res.json();
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