// ==================== Authentication ==================== 
let currentUser = null;
let charts = {};

function handleLogin(event) {
    event.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    // Simple demo authentication (username: admin, password: admin)
    if (username === 'admin' && password === 'admin') {
        currentUser = {
            id: 1,
            name: 'Admin',
            username: username,
            email: 'admin@iot-dashboard.com'
        };
        
        // Update UI
        document.getElementById('user-name').textContent = currentUser.name;
        document.querySelector('.user-avatar').textContent = currentUser.name.charAt(0).toUpperCase();
        
        // Hide auth modal and show app
        const authModal = document.getElementById('auth-modal');
        authModal.classList.add('hidden');
        
        setTimeout(() => {
            const mainApp = document.getElementById('main-app');
            mainApp.classList.remove('hidden');
            
            // Load home page by default
            setTimeout(() => {
                changePage('home', document.querySelector('.nav-button.active'));
            }, 100);
        }, 300);
    } else {
        alert('Tên đăng nhập hoặc mật khẩu không đúng!\nDùng: admin / admin');
        document.getElementById('password').value = '';
    }
}

function handleLogout() {
    if (confirm('Bạn có chắc chắn muốn đăng xuất?')) {
        currentUser = null;
        
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
    } else if (url.includes('usercase5')) {
        pageTitle.textContent = 'Tài khoản & Firmware';
        pageSubtitle.textContent = 'Quản lý tài khoản và cập nhật firmware';
    }
}

// ==================== Home Content ==================== 
function loadHomeContent() {
    const homeContent = document.getElementById('home-content');
    
    homeContent.innerHTML = `
        <!-- Welcome Card -->
        <div class="welcome-card">
            <h2> Chào mừng, ${currentUser.name}!</h2>
            <p>Đây là bảng điều khiển IoT của bạn. Từ đây bạn có thể quản lý toàn bộ các thiết bị, cảm biến, và cài đặt hệ thống.</p>
            
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
        
        <!-- Temperature Chart -->
        <div class="chart-card">
            <h3> Nhiệt độ (24h qua)</h3>
            <div class="chart-container">
                <canvas id="temperature-chart"></canvas>
            </div>
        </div>
        
        <!-- Humidity Chart -->
        <div class="chart-card">
            <h3>Độ ẩm (24h qua)</h3>
            <div class="chart-container">
                <canvas id="humidity-chart"></canvas>
            </div>
        </div>
        
        <!-- System Status -->
        <div class="info-card">
            <h3> Trạng thái hệ thống</h3>
            <div class="info-list">
                <div class="info-item">
                    <div class="info-icon"> </div>
                    <div class="info-text">
                        <h4>Kết nối MQTT</h4>
                        <p>Đã kết nối - 2 phút trước</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"> </div>
                    <div class="info-text">
                        <h4>WiFi Gateway</h4>
                        <p>Signal: 85% - Ổn định</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"> </div>
                    <div class="info-text">
                        <h4>Dung lượng lưu trữ</h4>
                        <p>Sử dụng: 45GB / 100GB</p>
                        <span class="status-badge">Normal</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"> </div>
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
            <h3> Thiết bị</h3>
            <div class="info-list">
                <div class="info-item">
                    <div class="info-icon"></div>
                    <div class="info-text">
                        <h4>Cảm biến tầng 1</h4>
                        <p>Nhiệt độ: 24.5°C - Độ ẩm: 65%</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"></div>
                    <div class="info-text">
                        <h4>Cảm biến tầng 2</h4>
                        <p>Nhiệt độ: 26.2°C - Độ ẩm: 72%</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"></div>
                    <div class="info-text">
                        <h4>Cảm biến tầng 3</h4>
                        <p>Nhiệt độ: 23.8°C - Độ ẩm: 58%</p>
                        <span class="status-badge">Online</span>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"> </div>
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
            <h3> Hoạt động gần đây</h3>
            <div class="info-list">
                <div class="info-item">
                    <div class="info-icon"></div>
                    <div class="info-text">
                        <h4>Cảnh báo vượt ngưỡng</h4>
                        <p>Tầng 2 - Độ ẩm vượt 75% - 15 phút trước</p>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"></div>
                    <div class="info-text">
                        <h4>Cập nhật firmware</h4>
                        <p>v2.1.0 - Hoàn tất thành công - 2 giờ trước</p>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"></div>
                    <div class="info-text">
                        <h4>Khởi động lại hệ thống</h4>
                        <p>Toàn bộ hệ thống khởi động thành công - 5 giờ trước</p>
                    </div>
                </div>
                <div class="info-item">
                    <div class="info-icon"></div>
                    <div class="info-text">
                        <h4>Kết nối thiết bị mới</h4>
                        <p>Sensor_05 đã kết nối - Hôm qua</p>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Initialize charts
    initCharts();
}

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