// --- STATE QUẢN LÝ ---
let currentMode = 'today';
let energyChart = null;
let totalEnergyGlobal = 0;

// --- 1. KHỞI TẠO BIỂU ĐỒ ---
function initChart() {
    const ctx = document.getElementById('energyChart').getContext('2d');
    
    energyChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                { label: 'Dữ liệu chính', data: [], borderColor: '#10b981', borderWidth: 3, fill: true, backgroundColor: 'rgba(16, 185, 129, 0.1)', tension: 0.3, pointRadius: 4 },
                { label: 'So sánh', data: [], borderColor: '#f59e0b', borderWidth: 2, borderDash: [5, 5], fill: false, tension: 0.3, pointRadius: 3, hidden: true }
            ]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: {
                legend: { display: true, labels: { color: '#94a3b8', font: { size: 10 } } },
                annotation: {
                    annotations: {
                        line1: {
                            type: 'line', yMin: 10, yMax: 10, borderColor: '#ef4444', borderWidth: 2, borderDash: [6, 6],
                            label: { display: true, content: 'Cảnh báo', position: 'end', backgroundColor: 'rgba(239, 68, 68, 0.9)', font: { size: 10 } }
                        }
                    }
                }
            },
            scales: {
                x: { grid: { display: false }, ticks: { color: '#94a3b8', font: { size: 10 } } },
                y: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: '#94a3b8' }, beginAtZero: true }
            }
        }
    });
}

// --- 2. CẬP NHẬT GIAO DIỆN PHỤ ---
function updateChartLine() {
    if (!energyChart) return;
    const val = parseFloat(document.getElementById('chart-threshold').value) || 0;
    energyChart.options.plugins.annotation.annotations.line1.yMin = val;
    energyChart.options.plugins.annotation.annotations.line1.yMax = val;
    energyChart.update();
}

function updateMetricsOnly() {
    const price = parseFloat(document.getElementById('elec-price').value) || 0;
    const goal = parseFloat(document.getElementById('goal-total').value) || 1;
    
    // Tính tiền
    document.getElementById('money-display').innerText = (totalEnergyGlobal * price).toLocaleString('vi-VN') + " VNĐ";
    
    // Tính %
    const percent = (totalEnergyGlobal / goal) * 100;
    document.getElementById('percent-label').innerText = percent.toFixed(1) + "%";
    
    const bar = document.getElementById('percent-bar');
    bar.style.width = Math.min(percent, 100) + "%";
    
    if (percent > 100) {
        bar.style.backgroundColor = "#ef4444";
        document.getElementById('percent-label').style.color = "#ef4444";
    } else {
        bar.style.backgroundColor = "#10b981";
        document.getElementById('percent-label').style.color = "#10b981";
    }
}

// --- 3. LOGIC GỌI API & ĐỒNG BỘ DỮ LIỆU ---
async function fetchApi(mode, timeVal) {
    if (!timeVal && mode !== 'overview') return [];
    let url = `http://localhost:8080/api/quochoc/data/day?day=${timeVal}`;
    if (mode === 'month') url = `http://localhost:8080/api/quochoc/energy/monthly?month=${timeVal}`;
    if (mode === 'year') url = `http://localhost:8080/api/quochoc/energy/yearly?year=${timeVal}`;
    if (mode === 'overview') url = `http://localhost:8080/api/quochoc/energy/overview`;
    
    try {
        const res = await fetch(url);
        return await res.json() || [];
    } catch (e) {
        console.error("Lỗi API:", e);
        return [];
    }
}

function generateLabels(mode, timeVal) {
    let labels = [];
    if (mode === 'today') {
        for (let i = 0; i <= 23; i++) labels.push(String(i).padStart(2, '0') + ":00");
    } else if (mode === 'month') {
        if(!timeVal) return labels;
        const [y, m] = timeVal.split('-');
        const days = new Date(y, m, 0).getDate();
        for (let i = 1; i <= days; i++) labels.push("N" + String(i).padStart(2, '0'));
    } else if (mode === 'year') {
        for (let i = 1; i <= 12; i++) labels.push("T" + String(i).padStart(2, '0'));
    }
    return labels;
}

// Hàm lõi: Gọi API, map dữ liệu và vẽ biểu đồ
async function fetchAndRenderData() {
    const isCompare = document.getElementById('compare-enable').checked;
    
    const idMain = currentMode === 'year' ? 'main-year' : (currentMode === 'month' ? 'main-month' : 'main-day');
    const idComp = currentMode === 'year' ? 'comp-year' : (currentMode === 'month' ? 'comp-month' : 'comp-day');
    
    const timeMain = document.getElementById(idMain)?.value;
    const timeComp = document.getElementById(idComp)?.value;

    let rawMain = await fetchApi(currentMode, timeMain);
    let rawComp = isCompare ? await fetchApi(currentMode, timeComp) : [];

    let finalLabels = [], finalMain = [], finalComp = [];
    totalEnergyGlobal = 0;

    if (currentMode === 'overview') {
        // TỔNG QUAN: Trực tiếp lấy data từ API (API trả về nam và tong_nang_luong)
        finalLabels = rawMain.map(i => "Năm " + (i.nam || ""));
        finalMain = rawMain.map(i => parseFloat(i.tong_nang_luong || 0));
        
        totalEnergyGlobal = finalMain.reduce((a, b) => a + b, 0);
        document.getElementById('energy-title').innerText = "TỔNG QUAN TIÊU THỤ CÁC NĂM";
        
    } else {
        // CÁC CHẾ ĐỘ CÒN LẠI: Tạo trục X (labels) và map data vào
        finalLabels = generateLabels(currentMode, timeMain);
        
        // Hàm trợ giúp tìm giá trị tương ứng từ cục data API trả về
        const mapValueToLabel = (rawData, label) => {
            let foundItem;
            
            if (currentMode === 'today') {
                // Nhãn là "05:00", API trả về "time": "05:00"
                foundItem = rawData.find(item => item.time === label);
            } 
            else if (currentMode === 'month') {
                // Nhãn là "N04", API trả về "ngay": "2026-04-04" -> so sánh chuỗi "-04"
                const dayStr = "-" + label.substring(1); 
                foundItem = rawData.find(item => item.ngay && item.ngay.endsWith(dayStr));
            } 
            else if (currentMode === 'year') {
                // Nhãn là "T04", API trả về "thang": "2026-04" -> so sánh chuỗi "-04"
                const monthStr = "-" + label.substring(1);
                foundItem = rawData.find(item => item.thang && item.thang.endsWith(monthStr));
            }

            return foundItem ? parseFloat(foundItem.tong_nang_luong) : 0;
        };

        // Gắn data chính
        finalMain = finalLabels.map(label => {
            const val = mapValueToLabel(rawMain, label);
            totalEnergyGlobal += val; // Cộng dồn để hiển thị tổng số to đùng
            return val;
        });

        // Gắn data so sánh (nếu có bật)
        if (isCompare) {
            finalComp = finalLabels.map(label => mapValueToLabel(rawComp, label));
        }
        
        document.getElementById('energy-title').innerText = `DỮ LIỆU ${currentMode.toUpperCase()}: ${timeMain}`;
    }

    // Cập nhật DOM (Hiển thị 2 chữ số thập phân)
    document.getElementById('energy-main').innerText = totalEnergyGlobal.toLocaleString('vi-VN', { maximumFractionDigits: 2 });
    
    energyChart.data.labels = finalLabels;
    energyChart.data.datasets[0].data = finalMain;
    energyChart.data.datasets[1].data = finalComp;
    energyChart.data.datasets[1].hidden = !isCompare;
    energyChart.update();
    
    updateMetricsOnly();
}

// --- 4. ĐIỀU HƯỚNG TAB ---
function switchTab(mode, btn) {
    currentMode = mode;
    
    // Highlight nút
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    // Mảng chứa ID các input
    const allInputs = ['main-day', 'main-month', 'main-year', 'comp-day', 'comp-month', 'comp-year'];
    
    // Ẩn tất cả input
    allInputs.forEach(id => document.getElementById(id).classList.add('hidden'));
    
    // Xử lý ẩn/hiện tùy mode
    const compSection = document.getElementById('compare-section');
    if (mode === 'overview') {
        compSection.classList.add('hidden');
    } else {
        compSection.classList.remove('hidden');
        if (mode === 'today') {
            document.getElementById('main-day').classList.remove('hidden');
            document.getElementById('comp-day').classList.remove('hidden');
        } else if (mode === 'month') {
            document.getElementById('main-month').classList.remove('hidden');
            document.getElementById('comp-month').classList.remove('hidden');
        } else if (mode === 'year') {
            document.getElementById('main-year').classList.remove('hidden');
            document.getElementById('comp-year').classList.remove('hidden');
        }
    }
    
    fetchAndRenderData();
}

// --- 5. KHỞI ĐỘNG HỆ THỐNG ---
function startClock() {
    const update = () => {
        const now = new Date();
        document.getElementById('full-date').innerText = now.toLocaleTimeString('vi-VN');
        document.getElementById('calendar-date').innerText = now.toLocaleDateString('vi-VN', { weekday: 'long', year: 'numeric', month: 'long', day: '2-digit' });
    };
    update();
    setInterval(update, 1000);
}

function setInitialDates() {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    
    // Set Main Dates
    document.getElementById('main-day').value = `${y}-${m}-${d}`;
    document.getElementById('main-month').value = `${y}-${m}`;
    document.getElementById('main-year').value = y;

    // Set Compare Dates (Yesterday, Last Month, Last Year)
    const yest = new Date(now); yest.setDate(yest.getDate() - 1);
    document.getElementById('comp-day').value = yest.toISOString().split('T')[0];
    
    let lastM = now.getMonth(); let lastY = y;
    if(lastM === 0) { lastM = 12; lastY--; }
    document.getElementById('comp-month').value = `${lastY}-${String(lastM).padStart(2, '0')}`;
    
    document.getElementById('comp-year').value = y - 1;
}

window.onload = () => {
    startClock();
    setInitialDates();
    initChart();
    // Chạy mặc định chế độ ngày
    switchTab('today', document.querySelector('.tab-btn.active') || document.querySelectorAll('.tab-btn')[0]); 
};