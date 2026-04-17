
async function renderThresholdManager() {
    const container = document.getElementById("thresholdSection");

    // 1. Trạng thái Loading Cyber (Đồng bộ với DeviceManager)
    container.innerHTML = `
        <div class="loader-container">
            <div class="spinner"></div>
            <p style="color: var(--accent-blue); margin-top:15px; letter-spacing:2px">Đang hiệu chỉnh hệ thống...</p>
        </div>
    `;

    try {
        // Chạy song song để tối ưu tốc độ load
        const [devices, limits] = await Promise.all([getDevices(), getLimits()]);

        const htmlTemplate = `
            <div class="threshold-grid">
                ${devices.map(device => {
            // Tìm giới hạn tương ứng với thiết bị hiện tại
            const limit = limits.find(l => l.device_id === device.id) || {};

            return `
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

                            <div class="inputs-group">
                                <div class="form-group">
                                    <label>Ngưỡng Công Suất</label>
                                    <div class="input-wrapper">
                                        <input type="number" 
                                               id="power-${device.id}" 
                                               value="${limit.max_power || ''}" 
                                               placeholder="0000"
                                               step="1">
                                        <span class="unit">W</span>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label>Ngưỡng Dòng Điện</label>
                                    <div class="input-wrapper">
                                        <input type="number" 
                                               id="current-${device.id}" 
                                               value="${limit.max_current || ''}" 
                                               placeholder="0.0"
                                               step="0.1">
                                        <span class="unit">A</span>
                                    </div>
                                </div>
                            </div>

                            <div class="card-footer">
                                <button class="btn-update" onclick="uiSaveThreshold(${device.id}, this)">
                                    <i class="fas fa-shield-alt"></i>
                                    <span>Ghi dữ liệu</span>
                                </button>
                            </div>
                        </div>
                    `;
        }).join('')}
            </div>
        `;

        container.innerHTML = htmlTemplate;
    } catch (error) {
        console.error("Threshold Load Error:", error);
        container.innerHTML = `<div class="error-msg">⚠️ CRITICAL: FAILED TO CONNECT TO LIMITS_DB</div>`;
    }
}


/**
 * Xử lý lưu ngưỡng với hiệu ứng phản hồi cao cấp
 */
async function uiSaveThreshold(deviceId, btn) {
    // 1. Khởi tạo trạng thái Loading cho nút
    btn.classList.add('is-loading');
    btn.disabled = true;
    const originalContent = btn.innerHTML;

    // Đổi icon sang spinner quay tròn khi đang xử lý
    btn.innerHTML = `<i class="fas fa-circle-notch fa-spin"></i> <span>Đang ghi...</span>`;

    try {
        // Lấy giá trị và ép kiểu Number để tránh lỗi backend
        const powerValue = Number(document.getElementById(`power-${deviceId}`).value);
        const currentValue = Number(document.getElementById(`current-${deviceId}`).value);

        if (isNaN(powerValue) || isNaN(currentValue)) {
            throw new Error("Invalid Input");
        }

        // 2. Gọi API thực tế
        await saveLimit(deviceId, powerValue, currentValue);

        // 3. Hiệu ứng thành công (Màu xanh Teal phát sáng)
        btn.innerHTML = `<i class="fas fa-check-circle"></i> <span>Thành công</span>`;
        btn.style.background = "rgba(0, 188, 212, 0.2)";
        btn.style.borderColor = "var(--accent-teal)";
        btn.style.color = "var(--accent-teal)";
        btn.style.boxShadow = "0 0 15px rgba(0, 188, 212, 0.4)";

        // Reset về trạng thái cũ sau 2 giây
        setTimeout(() => {
            btn.innerHTML = originalContent;
            btn.classList.remove('is-loading');
            btn.disabled = false;
            btn.style = "";
        }, 2000);

    } catch (err) {
        // 4. Hiệu ứng thất bại (Màu đỏ phát sáng)
        btn.innerHTML = `<i class="fas fa-times-circle"></i> <span>Lỗi ghép nối</span>`;
        btn.style.background = "rgba(239, 68, 68, 0.2)";
        btn.style.borderColor = "var(--accent-red)";
        btn.style.color = "var(--accent-red)";
        btn.style.boxShadow = "0 0 15px rgba(239, 68, 68, 0.4)";

        console.error("Save Threshold Error:", err);

        setTimeout(() => {
            btn.innerHTML = originalContent;
            btn.classList.remove('is-loading');
            btn.disabled = false;
            btn.style = "";
        }, 2000);
    }
}