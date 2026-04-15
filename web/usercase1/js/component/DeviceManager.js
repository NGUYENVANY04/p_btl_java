
async function renderDeviceManager() {
    // Hỗ trợ cả trang demo.html và device.html
    const container = document.getElementById("deviceSection") || document.getElementById("deviceCardGrid");

    if (!container) {
        console.error("Lỗi: Không tìm thấy vùng hiển thị thiết bị. Vui lòng kiểm tra id 'deviceSection' hoặc 'deviceCardGrid'.");
        return;
    }
    
    // 1. Hiệu ứng Loading "Scanning"
    container.innerHTML = `
        <div class="loader-container">
            <div class="spinner"></div>
            <p style="color: var(--accent-blue); margin-top:15px; letter-spacing:2px; font-weight:bold;">
                SYSTEM SCANNING...
            </p>
        </div>
    `;

    try {
        const devices = await getDevices(); // Hàm lấy dữ liệu từ api.js

        // Nếu là deviceCardGrid (device.html), chỉ render các card
        // Nếu là deviceSection (demo.html), render toàn bộ
        const isStandalone = document.getElementById("deviceCardGrid");
        
        let htmlTemplate;
        
        if (isStandalone) {
            // Chỉ render các device card cho device.html
            htmlTemplate = devices.map(device => `
                <div class="device-card">
                    <div class="card-header">
                        <div class="card-title-group">
                            <h3>${device.name}</h3>
                            <span class="device-tag">#ID-${device.id.toString().padStart(3, '0')}</span>
                        </div>
                        <div class="location">
                            <i class="fas fa-microchip"></i> <span>${device.location}</span>
                        </div>
                    </div>

                    <div class="info-status">
                        <div class="status-item">
                            <span class="status-label">Trạng thái</span>
                            <span class="status-value ${device.status === 'online' ? 'text-active' : 'text-inactive'}">
                                ${device.status === 'online' ? '● Trực tuyến' : '○ Ngoại tuyến'}
                            </span>
                        </div>
                    </div>

                    <div class="card-footer">
                        <button class="btn-update" onclick="editDevice(${device.id})">
                            <i class="fas fa-edit"></i>
                            <span>SỬA</span>
                        </button>
                        <button class="btn-delete" onclick="confirmDelete(${device.id})">
                            <i class="fas fa-trash-alt"></i>
                            <span>XÓA</span>
                        </button>
                    </div>
                </div>
            `).join('');
        } else {
            // Demo.html mode - render từng device theo cách cũ
            htmlTemplate = `
                <div class="device-card-grid">
                    ${devices.map(device => `
                        <div class="device-card">
                            <div class="card-top">
                                <span class="device-id">ID: #${device.id.toString().padStart(2, '0')}</span>
                                <div class="status-dot ${device.status === 'online' ? 'online' : 'offline'}"></div>
                            </div>

                            <h3 class="device-name">${device.name}</h3>

                            <div class="info-row">
                                <div class="info-group">
                                    <span class="info-label">Vị trí</span>
                                    <span class="info-value">${device.location}</span>
                                </div>
                                <div class="info-group">
                                    <span class="info-label">Trạng thái</span>
                                    <span class="info-value ${device.status === 'online' ? 'text-active' : 'text-inactive'}">
                                        ${device.status === 'online' ? 'Trực tuyến' : 'Ngoại tuyến'}
                                    </span>
                                </div>
                            </div>

                            <div class="card-actions">
                                <button class="action-btn edit" onclick="editDevice(${device.id})">
                                    SỬA
                                </button>
                                <button class="action-btn delete" onclick="confirmDelete(${device.id})">
                                    XÓA
                                </button>
                            </div>
                        </div>
                    `).join('')}
                </div>
            `;
        }

        container.innerHTML = htmlTemplate;

    } catch (error) {
        console.error("Lỗi render thiết bị:", error);
        container.innerHTML = `
            <div class="error-msg">
                <i class="fas fa-exclamation-triangle"></i>
                <p>KHÔNG THỂ KẾT NỐI VỚI HỆ THỐNG PHẦN CỨNG</p>
            </div>
        `;
    }
}

/**
 * Xác nhận xóa thiết bị
 */
async function confirmDelete(id) {
    if (!confirm("Bạn có chắc muốn xóa thiết bị này?")) {
        return;
    }
    await deleteDevice(id);
    renderDeviceManager();
}

/**
 * Xử lý thêm thiết bị mới từ thanh Input
 */
async function handleAddNewDevice() {
    const name = document.getElementById('newDeviceName').value.trim();
    const location = document.getElementById('newDeviceLocation').value.trim();

    if (!name || !location) {
        alert("Vui lòng điền đầy đủ thông tin Module!");
        return;
    }

    // Hiệu ứng nút bấm khi đang xử lý
    const btn = document.querySelector('.btn-add-submit');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>...';
    
    try {
        await createDevice({ name, location }); // Hàm trong api.js
        document.getElementById('newDeviceName').value = '';
        document.getElementById('newDeviceLocation').value = '';
        renderDeviceManager(); // Load lại danh sách
    } catch (err) {
        alert("Lỗi khi đăng ký thiết bị mới!");
        btn.innerHTML = '<i class="fas fa-plus"></i> Thêm';
    } finally {
        btn.innerHTML = '<i class="fas fa-plus"></i> Thêm';
    }
}

/**
 * Xử lý sửa thiết bị
 */
async function editDevice(id) {
    const device = await getDevices().then(devices => devices.find(d => d.id === id));
    const newName = prompt("Tên thiết bị:", device.name);
    if (newName === null) return;
    
    const newLocation = prompt("Vị trí:", device.location);
    if (newLocation === null) return;
    
    await updateDevice(id, { name: newName, location: newLocation });
    renderDeviceManager();
}

/**
 * Xóa thiết bị
 */
async function deleteDevice(id) {
    try {
        const response = await fetch(`${API}/devices/${id}`, { method: "DELETE" });
        if (!response.ok) throw new Error("Failed to delete");
    } catch (err) {
        console.error("Delete error:", err);
    }
}
