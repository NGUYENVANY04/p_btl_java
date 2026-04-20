// ==================== Authentication ====================
let currentUser = null;
let charts = {};

function normalizeRole(role) {
    return String(role || '').trim().toLowerCase();
}

function isAdminUser(user) {
    return normalizeRole(user && user.role) === 'admin';
}

function syncCurrentUserState() {
    if (!currentUser) {
        window.__CURRENT_USER__ = null;
        sessionStorage.removeItem("currentUser");
        return;
    }

    currentUser = {
        ...currentUser,
        role: normalizeRole(currentUser.role)
    };

    window.__CURRENT_USER__ = currentUser;
    sessionStorage.setItem("currentUser", JSON.stringify(currentUser));
}

// User database (in production, this would be on the server)
const userDatabase = [
    {
        id: 1,
        name: 'Admin',
        username: 'admin',
        password: 'admin',
        email: 'admin@iot-dashboard.com',
        role: 'admin',
        permissions: ['all']
    },
    {
        id: 2,
        name: 'Nguyễn Văn A',
        username: 'user1',
        password: 'user123',
        email: 'user1@example.com',
        role: 'user',
        permissions: ['view_dashboard', 'view_sensors', 'view_budget', 'view_notifications']
    },
    {
        id: 3,
        name: 'Trần Thị B',
        username: 'user2',
        password: 'user123',
        email: 'user2@example.com',
        role: 'user',
        permissions: ['view_dashboard', 'view_sensors', 'view_budget']
    },
    {
        id: 4,
        name: 'Lê Văn C',
        username: 'user3',
        password: 'user123',
        email: 'user3@example.com',
        role: 'user',
        permissions: ['view_dashboard', 'view_sensors']
    }
];


async function handleLogin(event) {
    event.preventDefault();

    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    // ===== 1. CHECK ADMIN (hardcode) =====
    if (email === 'admin' && password === 'admin') {
        currentUser = {
            id: 0,
            name: 'Admin',
            role: 'admin'
        };
        // Lưu vào bộ nhớ trình duyệt
        syncCurrentUserState();
        afterLoginSuccess();
        return;
    }

    // ===== 2. USER LOGIN QUA API =====
    try {
        const response = await fetch("http://localhost:8080/api/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: email,
                password: password
            })
        });

        if (!response.ok) {
            throw new Error("Sai tài khoản hoặc mật khẩu");
        }

        const data = await response.json();

        // Kiểm tra data có tồn tại không trước khi truy cập
        if (data) {
            currentUser = {
                id: data.id,            // Lấy ID = 3 từ JSON của bạn
                name: data.username,    // Đổi data.name thành data.username cho đúng JSON
                email: data.email,      // Lấy email từ JSON
                role: normalizeRole(data.role) || 'user',
                permissions: [
                    'view_dashboard',
                    'view_sensors',
                    'view_budget',
                    'view_notifications',
                    'manage_devices',
                    'manage_thresholds',
                    'manage_network',
                    'manage_accounts'
                ]
            };

            // LƯU QUAN TRỌNG: Cất vào sessionStorage
            syncCurrentUserState();

            console.log("Đăng nhập thành công, ID lưu lại là:", currentUser.id);
            afterLoginSuccess();
        } else {
            throw new Error("Dữ liệu phản hồi trống");
        }

    } catch (error) {
        console.error("Lỗi chi tiết:", error);
        alert("Đăng nhập thất bại: " + error.message);
        document.getElementById('password').value = '';
    }
}
function afterLoginSuccess() {
    // Update UI
    document.getElementById('user-name').textContent = currentUser.name;
    document.querySelector('.user-avatar').textContent = currentUser.name.charAt(0).toUpperCase();

    updateRoleDisplay();
    applyRolePermissions();

    const authModal = document.getElementById('auth-modal');
    authModal.classList.add('hidden');

    setTimeout(() => {
        const mainApp = document.getElementById('main-app');
        mainApp.classList.remove('hidden');

        setTimeout(() => {
            changePage('home', document.querySelector('.nav-button.active'));
        }, 100);
    }, 300);
}

function updateRoleDisplay() {
    const userInfo = document.querySelector('.user-info');
    const roleBadge = document.createElement('div');
    roleBadge.className = `role-badge role-${currentUser.role}`;
    roleBadge.textContent = isAdminUser(currentUser) ? 'Quản trị viên' : 'Người dùng';

    // Remove existing role badge if any
    const existingBadge = userInfo.querySelector('.role-badge');
    if (existingBadge) existingBadge.remove();

    userInfo.appendChild(roleBadge);
}

function applyRolePermissions() {
    const submenu = document.getElementById("system-submenu");

    if (!submenu) return;

    if (isAdminUser(currentUser)) {
        // ADMIN: ẩn 2 page con
        submenu.style.display = "none";
    } else {
        // USER: hiện full
        submenu.style.display = "block";
    }
}
function checkPermission(url) {
    if (!currentUser || isAdminUser(currentUser)) return true;

    const permissions = currentUser.permissions || [];

    // Map URLs to permissions
    const permissionMap = {
        'home': 'view_dashboard',
        'usercase2/usercase2.html': 'view_sensors',
        'budget_manager/budget.html': 'view_budget',
        'usercase4/usercase4.html': 'view_notifications',
        'device_manager/device.html': 'manage_devices',
        'device_manager/threshold.html': 'manage_thresholds',
        'usercase3/usercase3.html': 'manage_network',
        'usercase5/usercase5.html': 'manage_accounts'
    };

    const requiredPermission = permissionMap[url];
    return requiredPermission ? permissions.includes(requiredPermission) : false;
}

// ==================== Navigation ==================== 
function toggleSubmenu(el) {
    const parent = el.parentElement;
    // Close other submenus
    document.querySelectorAll('.nav-item').forEach(item => {
        if (item !== parent) item.classList.remove('active');
    });
    parent.classList.toggle('active');
}

function changePage(url, btn) {
    const contentFrame = document.getElementById('content-frame');
    const homeContent = document.getElementById('home-content');
    const pageTitle = document.getElementById('page-title');
    const pageSubtitle = document.getElementById('page-subtitle');
    const submenuBtn = btn && btn.closest('.submenu');

    // Remove active from all main buttons and submenu buttons
    document.querySelectorAll('.nav-button').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.submenu button').forEach(b => b.classList.remove('active'));

    if (url === 'home') {
        // Show home page
        homeContent.style.display = 'grid';
        contentFrame.style.display = 'none';
        pageTitle.textContent = 'Trang chủ';
        pageSubtitle.textContent = 'Chào mừng bạn đến với IoT Dashboard';

        if (btn) btn.classList.add('active');

        // Load home content
        loadHomeContent();
    } else {
        // Show iframe content
        homeContent.style.display = 'none';
        contentFrame.style.display = 'block';
        contentFrame.src = url;
        if (submenuBtn) {
            btn.classList.add('active');
            const parentNavItem = btn.closest('.nav-item');
            if (parentNavItem) parentNavItem.classList.add('active');
        } else if (btn) {
            btn.classList.add('active');
        }

        // Update page title based on URL
        updatePageTitle(url);
    }

    // Close submenus only when not clicking a submenu item
    if (!submenuBtn) {
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });
    }
}

function updatePageTitle(url) {
    const pageTitle = document.getElementById('page-title');
    const pageSubtitle = document.getElementById('page-subtitle');
    if (url.includes('device.html')) {
        pageTitle.textContent = 'Cấu hình thiết bị';
        pageSubtitle.textContent = 'Quản lý và cấu hình các thiết bị IoT';
    } else if (url.includes('threshold.html')) {
        pageTitle.textContent = 'Đặt ngưỡng cảnh báo';
        pageSubtitle.textContent = 'Cấu hình các mức cảnh báo cho cảm biến';
    } else if (url.includes('usercase2')) {
        pageTitle.textContent = 'Dữ liệu cảm biến';
        pageSubtitle.textContent = 'Xem và theo dõi dữ liệu từ các cảm biến';
    } else if (url.includes('usercase3')) {
        pageTitle.textContent = 'Kết nối WiFi & MQTT';
        pageSubtitle.textContent = 'Cấu hình kết nối mạng và MQTT';
    } else if (url.includes('usercase4')) {
        pageTitle.textContent = 'Thông báo & Logs';
        pageSubtitle.textContent = 'Xem các thông báo và nhật ký hệ thống';
    } else if (url.includes('budget_manager/budget.html')) {
        pageTitle.textContent = 'Quản lý ngân sách điện';
        pageSubtitle.textContent = 'Theo dõi và quản lý chi phí điện năng';
    }
}

function handleLogout() {
    if (confirm('Bạn có chắc chắn muốn đăng xuất?')) {
        currentUser = null;
        syncCurrentUserState();

        // Destroy charts
        Object.values(charts).forEach(chart => {
            if (chart) chart.destroy();
        });
        charts = {};

        // Show auth modal
        const mainApp = document.getElementById('main-app');
        mainApp.classList.add('hidden');

        const authModal = document.getElementById('auth-modal');
        authModal.classList.remove('hidden');

        // Reset form
        document.getElementById('login-form').reset();
    }
}

// ==================== Home Content ====================
function loadHomeContent() {
    const homeContent = document.getElementById('home-content');

    if (isAdminUser(currentUser)) {
        loadAdminDashboard(homeContent);
    } else {
        loadUserDashboard(homeContent);
    }
}

function loadAdminDashboard(container) {
    container.innerHTML = `
        <!-- Welcome Card -->
        <div class="welcome-card">
            <h2>👑 Chào mừng, ${currentUser.name}!</h2>
            <p>Bạn đang ở chế độ Quản trị viên. Từ đây bạn có thể quản lý toàn bộ hệ thống IoT, thiết bị, và người dùng.</p>

            <div class="stats">
                <div class="stat-item">
                    <div class="stat-number">12</div>
                    <div class="stat-label">Thiết bị hoạt động</div>
                </div>
                <div class="stat-item">
                    <div class="stat-number">48</div>
                    <div class="stat-label">Cảm biến kết nối</div>
                </div>
                <div class="stat-item">
                    <div class="stat-number">3</div>
                    <div class="stat-label">Cảnh báo mới</div>
                </div>
                <div class="stat-item">
                    <div class="stat-number">99.2%</div>
                    <div class="stat-label">Độ ổn định</div>
                </div>
            </div>
        </div>

        <!-- System Overview -->
        <div class="chart-card">
            <h3>📊 Tổng quan hệ thống</h3>
            <div class="admin-overview">
                <div class="overview-item">
                    <h4>👥 Người dùng</h4>
                    <p>4 người dùng đang hoạt động</p>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: 100%"></div>
                    </div>
                </div>
                <div class="overview-item">
                    <h4>🔧 Thiết bị</h4>
                    <p>12/12 thiết bị online</p>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: 100%"></div>
                    </div>
                </div>
                <div class="overview-item">
                    <h4>📡 MQTT</h4>
                    <p>Kết nối ổn định</p>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: 95%"></div>
                    </div>
                </div>
                <div class="overview-item">
                    <h4>💾 Dung lượng</h4>
                    <p>45GB / 100GB sử dụng</p>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: 45%"></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Temperature Chart -->
        <div class="chart-card">
            <h3>🌡️ Nhiệt độ (24h qua)</h3>
            <div class="chart-container">
                <canvas id="temperature-chart"></canvas>
            </div>
        </div>

        <!-- Humidity Chart -->
        <div class="chart-card">
            <h3>💧 Độ ẩm (24h qua)</h3>
            <div class="chart-container">
                <canvas id="humidity-chart"></canvas>
            </div>
        </div>

        <!-- System Status -->
        <div class="info-card">
            <h3>⚙️ Trạng thái hệ thống</h3>
            <div class="info-list">
                <div class="info-item">
                    <div class="info-icon">📡</div>
                    <div class="info-text">
                        <h4>Kết nối MQTT</h4>
                        <p>Đã kết nối - 2 phút trước</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon">📶</div>
                    <div class="info-text">
                        <h4>WiFi Gateway</h4>
                        <p>Signal: 85% - Ổn định</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon">💾</div>
                    <div class="info-text">
                        <h4>Dung lượng lưu trữ</h4>
                        <p>Sử dụng: 45GB / 100GB</p>
                        <span class="status-badge">Normal</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon">⚡</div>
                    <div class="info-text">
                        <h4>Nguồn điện</h4>
                        <p>Ổn định - 220V AC</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
            </div>
            <div class="quick-actions">
                <button class="action-btn" onclick="alert('Tính năng sẽ được cập nhật')">Khởi động lại</button>
                <button class="action-btn" onclick="alert('Tính năng sẽ được cập nhật')">Báo cáo</button>
                <button class="action-btn" onclick="alert('Tính năng sẽ được cập nhật')">Cài đặt</button>
            </div>
        </div>

        <!-- Device Status -->
        <div class="info-card">
            <h3>🔧 Thiết bị</h3>
            <div class="info-list">
                <div class="info-item">
                    <div class="info-icon">🏠</div>
                    <div class="info-text">
                        <h4>Cảm biến tầng 1</h4>
                        <p>Nhiệt độ: 24.5°C - Độ ẩm: 65%</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon">🏠</div>
                    <div class="info-text">
                        <h4>Cảm biến tầng 2</h4>
                        <p>Nhiệt độ: 26.2°C - Độ ẩm: 72%</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon">🏠</div>
                    <div class="info-text">
                        <h4>Cảm biến tầng 3</h4>
                        <p>Nhiệt độ: 23.8°C - Độ ẩm: 58%</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon">🌤️</div>
                    <div class="info-text">
                        <h4>Cảm biến ngoài trời</h4>
                        <p>Nhiệt độ: 28.3°C - Độ ẩm: 45%</p>
                        <span class="status-badge offline">Offline</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- Activity -->
        <div class="info-card">
            <h3>📋 Hoạt động gần đây</h3>
            <div class="info-list">
                <div class="info-item">
                    <div class="info-icon">⚠️</div>
                    <div class="info-text">
                        <h4>Cảnh báo vượt ngưỡng</h4>
                        <p>Tầng 2 - Độ ẩm vượt 75% - 15 phút trước</p>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon">🔄</div>
                    <div class="info-text">
                        <h4>Cập nhật firmware</h4>
                        <p>v2.1.0 - Hoàn tất thành công - 2 giờ trước</p>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon">👤</div>
                    <div class="info-text">
                        <h4>Đăng nhập hệ thống</h4>
                        <p>User: ${currentUser.name} - 3 giờ trước</p>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Load charts for admin
    setTimeout(() => {
        initCharts();
    }, 100);
}

function loadUserDashboard(container) {
    container.innerHTML = `
        <!-- Welcome Card -->
        <div class="welcome-card">
            <h2> Chào mừng, ${currentUser.name}!</h2>
            <p>Đây là bảng điều khiển cá nhân của bạn. Bạn có thể theo dõi dữ liệu cảm biến và quản lý ngân sách điện.</p>

            <div class="stats">
                <div class="stat-item">
                    <div class="stat-number">--</div>
                    <div class="stat-label">Thiết bị theo dõi</div>
                </div>
                <div class="stat-item">
                    <div class="stat-number">--</div>
                    <div class="stat-label">Cảm biến</div>
                </div>
                <div class="stat-item">
                    <div class="stat-number">--</div>
                    <div class="stat-label">Thông báo</div>
                </div>
                <div class="stat-item">
                    <div class="stat-number">--</div>
                    <div class="stat-label">Tiết kiệm</div>
                </div>
            </div>
        </div>

        <!-- Personal Budget Overview -->
        <div class="chart-card">
            <h3> Ngân sách điện tháng này</h3>
            <div class="budget-overview">
                <div class="budget-item">
                    <h4>Hạn mức: <span id="user-budget-limit">0 kWh</span></h4>
                    <p>Đã sử dụng: <span id="user-budget-used">0 kWh</span></p>
                    <div class="progress-bar">
                        <div class="progress-fill" id="user-budget-progress" style="width: 0%"></div>
                    </div>
                    <p class="budget-status" id="user-budget-status">Chưa thiết lập</p>
                </div>
            </div>
        </div>

        <!-- Temperature Chart -->
        <div class="chart-card">
            <h3> Nhiệt độ (24h qua)</h3>
            <div class="chart-container">
                <canvas id="temperature-chart"></canvas>
            </div>
        </div>

        <!-- Humidity Chart -->
        <div class="chart-card">
            <h3> Độ ẩm (24h qua)</h3>
            <div class="chart-container">
                <canvas id="humidity-chart"></canvas>
            </div>
        </div>

        <!-- Device Status -->
        <div class="info-card">
            <h3> Thiết bị theo dõi</h3>
            <div class="info-list">
                <div class="info-item">
                    <div class="info-icon"> </div>
                    <div class="info-text">
                        <h4>Cảm biến tầng 1</h4>
                        <p>Nhiệt độ: 24.5°C - Độ ẩm: 65%</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"> </div>
                    <div class="info-text">
                        <h4>Cảm biến tầng 2</h4>
                        <p>Nhiệt độ: 26.2°C - Độ ẩm: 72%</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"></div>
                    <div class="info-text">
                        <h4>Cảm biến ngoài trời</h4>
                        <p>Nhiệt độ: 28.3°C - Độ ẩm: 45%</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- Quick Actions -->
        <div class="info-card">
            <h3> Truy cập nhanh</h3>
            <div class="quick-actions">
                <button class="action-btn" onclick="changePage('usercase2/usercase2.html', this)">📊 Xem dữ liệu cảm biến</button>
                <button class="action-btn" onclick="changePage('budget_manager/budget.html', this)">⚡ Quản lý ngân sách</button>
                ${currentUser.permissions.includes('view_notifications') ?
            '<button class="action-btn" onclick="changePage(\'usercase4/usercase4.html\', this)">🔔 Thông báo</button>' : ''}
            </div>
        </div>

        <!-- Activity -->
        <div class="info-card">
            <h3> Hoạt động cá nhân</h3>
            <div class="info-list">
                <div class="info-item">
                    <div class="info-icon">👤</div>
                    <div class="info-text">
                        <h4>Đăng nhập hệ thống</h4>
                        <p>${currentUser.name} - Hôm nay</p>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"></div>
                    <div class="info-text">
                        <h4>Tiết kiệm điện</h4>
                        <p>Tiết kiệm được 15kWh so với tháng trước</p>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"></div>
                    <div class="info-text">
                        <h4>Báo cáo hàng tháng</h4>
                        <p>Đã tạo báo cáo tháng 3 - 5 ngày trước</p>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Load user-specific data
    loadUserData();

    // Load charts for user
    setTimeout(() => {
        initCharts();
    }, 100);
}

function loadUserData() {
    // Load budget data from localStorage (shared with budget manager)
    const budget = localStorage.getItem('electricity_budget');
    const usage = localStorage.getItem('electricity_usage');

    if (budget && usage) {
        const budgetLimit = parseFloat(budget);
        const currentUsage = parseFloat(usage);
        const percentage = budgetLimit > 0 ? (currentUsage / budgetLimit * 100) : 0;

        document.getElementById('user-budget-limit').textContent = budgetLimit.toFixed(1) + ' kWh';
        document.getElementById('user-budget-used').textContent = currentUsage.toFixed(2) + ' kWh';
        document.getElementById('user-budget-progress').style.width = Math.min(percentage, 100) + '%';

        const statusEl = document.getElementById('user-budget-status');
        if (percentage >= 100) {
            statusEl.textContent = ' Đã vượt quá hạn mức!';
            statusEl.style.color = '#ef4444';
        } else if (percentage >= 80) {
            statusEl.textContent = ' Cần tiết kiệm';
            statusEl.style.color = '#f59e0b';
        } else {
            statusEl.textContent = ' Trong hạn mức';
            statusEl.style.color = '#10b981';
        }
    }
}

// ==================== Charts ====================
function initCharts() {
    // Temperature Chart
    const tempCtx = document.getElementById('temperature-chart');
    if (tempCtx) {
        if (charts.temperature) charts.temperature.destroy();
        charts.temperature = new Chart(tempCtx, {
            type: 'line',
            data: {
                labels: ['00h', '02h', '04h', '06h', '08h', '10h', '12h', '14h', '16h', '18h', '20h', '22h', '24h'],
                datasets: [{
                    label: 'Nhiệt độ (°C)',
                    data: [20.5, 19.8, 19.2, 18.5, 20.1, 22.3, 24.5, 26.2, 25.8, 24.1, 22.5, 21.3, 20.8],
                    borderColor: '#ef4444',
                    backgroundColor: 'rgba(239, 68, 68, 0.1)',
                    tension: 0.4,
                    fill: true,
                    pointBackgroundColor: '#ef4444',
                    pointBorderColor: '#fff',
                    pointBorderWidth: 2,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        labels: {
                            color: '#cbd5e1',
                            font: {
                                size: 12,
                                weight: 600
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: false,
                        grid: {
                            color: 'rgba(255, 255, 255, 0.05)',
                            drawBorder: false
                        },
                        ticks: {
                            color: '#94a3b8',
                            font: {
                                size: 12
                            }
                        }
                    },
                    x: {
                        grid: {
                            color: 'rgba(255, 255, 255, 0.05)',
                            drawBorder: false
                        },
                        ticks: {
                            color: '#94a3b8',
                            font: {
                                size: 12
                            }
                        }
                    }
                }
            }
        });
    }
    // Humidity Chart
    const humidityCtx = document.getElementById('humidity-chart');
    if (humidityCtx) {
        if (charts.humidity) charts.humidity.destroy();
        charts.humidity = new Chart(humidityCtx, {
            type: 'line',
            data: {
                labels: ['00h', '02h', '04h', '06h', '08h', '10h', '12h', '14h', '16h', '18h', '20h', '22h', '24h'],
                datasets: [{
                    label: 'Độ ẩm (%)',
                    data: [65, 70, 75, 80, 72, 65, 58, 55, 60, 68, 72, 75, 70],
                    borderColor: '#3b82f6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    tension: 0.4,
                    fill: true,
                    pointBackgroundColor: '#3b82f6',
                    pointBorderColor: '#fff',
                    pointBorderWidth: 2,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        labels: {
                            color: '#cbd5e1',
                            font: {
                                size: 12,
                                weight: 600
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100,
                        grid: {
                            color: 'rgba(255, 255, 255, 0.05)',
                            drawBorder: false
                        },
                        ticks: {
                            color: '#94a3b8',
                            font: {
                                size: 12
                            }
                        }
                    },
                    x: {
                        grid: {
                            color: 'rgba(255, 255, 255, 0.05)',
                            drawBorder: false
                        },
                        ticks: {
                            color: '#94a3b8',
                            font: {
                                size: 12
                            }
                        }
                    }
                }
            }
        });
    }
}

// ==================== Time Display ==================== 
function updateTime() {
    const now = new Date();
    const timeString = now.toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
    document.getElementById('time-display').textContent = timeString;
}

setInterval(updateTime, 1000);
updateTime();

// ==================== Initialize ==================== 
// Focus on username field
window.addEventListener('load', () => {
    document.getElementById('username').focus();
});

// Hàm hiển thị màn hình Đăng ký
function showRegister() {
    document.getElementById('login-box').style.display = 'none';
    document.getElementById('register-box').style.display = 'block';
}

// Hàm hiển thị màn hình Đăng nhập
function showLogin() {
    document.getElementById('register-box').style.display = 'none';
    document.getElementById('login-box').style.display = 'block';
}

