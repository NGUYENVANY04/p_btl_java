let currentMode = 'today';
let energyChart;

/**
 * 1. Cập nhật đồng hồ hệ thống theo giây
 */
function updateDateTime() {
    const now = new Date();
    const fullDateEl = document.getElementById('full-date');
    const calendarDateEl = document.getElementById('calendar-date');
    if (fullDateEl) fullDateEl.innerText = now.toLocaleTimeString('vi-VN');
    if (calendarDateEl) {
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: '2-digit' };
        calendarDateEl.innerText = now.toLocaleDateString('vi-VN', options);
    }
}

/**
 * 2. Khởi tạo biểu đồ Chart.js
 */
function initChart() {
    const ctx = document.getElementById('energyChart').getContext('2d');
    const gradient = ctx.createLinearGradient(0, 0, 0, 350);
    gradient.addColorStop(0, 'rgba(16, 185, 129, 0.3)');
    gradient.addColorStop(1, 'rgba(16, 185, 129, 0)');

    energyChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Năng lượng (kWh)',
                data: [],
                borderColor: '#10b981',
                borderWidth: 3,
                fill: true,
                backgroundColor: gradient,
                tension: 0.3,
                pointRadius: 6,
                pointBackgroundColor: '#10b981',
                pointBorderColor: '#fff',
                pointBorderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { display: false }, ticks: { color: '#94a3b8', font: { size: 11 } } },
                y: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: '#94a3b8' }, beginAtZero: true }
            }
        }
    });
}

/**
 * 3. Hàm gọi API và tính TỔNG (SUM) năng lượng tiêu thụ
 */
async function loadHistoryData() {
    const titleEl = document.getElementById('energy-title');
    const mainValueEl = document.getElementById('energy-main');
    const datePicker = document.getElementById('date-picker');
    const yearPicker = document.getElementById('year-picker');

    if (!mainValueEl || !energyChart) return;
    mainValueEl.innerText = "---";

    try {
        let apiUrl = '';
        if (currentMode === 'today') {
            apiUrl = `http://localhost:8080/api/quochoc/data/day?day=${datePicker.value}`;
        } else if (currentMode === 'month') {
            apiUrl = `http://localhost:8080/api/quochoc/energy/monthly?month=${datePicker.value}`;
        } else if (currentMode === 'year') {
            apiUrl = `http://localhost:8080/api/quochoc/energy/yearly?year=${yearPicker.value}`;
        }

        const response = await fetch(apiUrl);
        if (!response.ok) throw new Error("Lỗi kết nối API");
        const result = await response.json();

        if (result && Array.isArray(result) && result.length > 0) {
            let labels = [];
            let chartData = [];
            let displayTotal = 0;

            if (currentMode === 'today') {
                // Sắp xếp theo thời gian tăng dần
                result.sort((a, b) => a.time.localeCompare(b.time));

                labels = result.map(item => item.time.substring(0, 5));
                chartData = result.map(item => item.energy);

                // LOGIC SUM: Cộng tất cả các giá trị energy của mỗi bản ghi
                displayTotal = result.reduce((sum, item) => sum + (Number(item.energy) || 0), 0);
                
                titleEl.innerText = `TIÊU THỤ NGÀY ${datePicker.value}`;
            } 
            else if (currentMode === 'month') {
                result.sort((a, b) => a.ngay.localeCompare(b.ngay));
                labels = result.map(item => "Ngày " + item.ngay.substring(8, 10));
                chartData = result.map(item => item.tong_nang_luong);
                
                // LOGIC SUM: Cộng tất cả các ngày trong tháng
                displayTotal = result.reduce((sum, item) => sum + (Number(item.tong_nang_luong) || 0), 0);
                titleEl.innerText = `TIÊU THỤ THÁNG ${datePicker.value}`;
            } 
            else if (currentMode === 'year') {
                result.sort((a, b) => a.thang.localeCompare(b.thang));
                labels = result.map(item => "Tháng " + item.thang.substring(5, 7));
                chartData = result.map(item => item.tong_nang_luong);
                
                // LOGIC SUM: Cộng tất cả các tháng trong năm
                displayTotal = result.reduce((sum, item) => sum + (Number(item.tong_nang_luong) || 0), 0);
                titleEl.innerText = `TIÊU THỤ NĂM ${yearPicker.value}`;
            }

            // Cập nhật giá trị tổng hiển thị (kWh)
            mainValueEl.innerText = displayTotal.toFixed(1);
            
            // Cập nhật biểu đồ đường
            energyChart.data.labels = labels;
            energyChart.data.datasets[0].data = chartData;
            energyChart.update();

        } else {
            mainValueEl.innerText = "0.0";
            titleEl.innerText = "KHÔNG CÓ DỮ LIỆU";
            energyChart.data.labels = [];
            energyChart.data.datasets[0].data = [];
            energyChart.update();
        }
    } catch (error) {
        console.error("Lỗi API:", error);
        mainValueEl.innerText = "ERR";
        titleEl.innerText = "LỖI KẾT NỐI SERVER";
    }
}

/**
 * 4. Chuyển đổi Tab (Daily/Monthly/Yearly)
 */
function switchTab(mode, btn) {
    currentMode = mode;
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    const datePicker = document.getElementById('date-picker');
    const yearPicker = document.getElementById('year-picker');
    const now = new Date();

    if (mode === 'year') {
        datePicker.classList.add('hidden');
        yearPicker.classList.remove('hidden');
    } else {
        datePicker.classList.remove('hidden');
        yearPicker.classList.add('hidden');
        datePicker.type = (mode === 'month') ? "month" : "date";
        
        const y = now.getFullYear();
        const m = String(now.getMonth() + 1).padStart(2, '0');
        const d = String(now.getDate()).padStart(2, '0');
        datePicker.value = (mode === 'month') ? `${y}-${m}` : `${y}-${m}-${d}`;
    }
    loadHistoryData();
}

/**
 * 5. Khởi tạo khi trang tải xong
 */
window.onload = () => {
    updateDateTime();
    setInterval(updateDateTime, 1000);
    initChart();
    
    // Thiết lập ngày mặc định (Hôm nay)
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    const datePicker = document.getElementById('date-picker');
    if (datePicker) datePicker.value = `${y}-${m}-${d}`;
    
    loadHistoryData();
};