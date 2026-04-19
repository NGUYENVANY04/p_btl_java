const user = JSON.parse(sessionStorage.getItem("currentUser")) || { id: 1 }; 

// =====================
// STATE
// =====================
let currentMode = 'today';
let energyChart = null;
let totalEnergyGlobal = 0;
let userDeviceIds = []; // Mảng chứa nhiều thiết bị

const BASE_URL = "http://localhost:8080/api";

// =====================
// LẤY DANH SÁCH DEVICE IDS
// =====================
async function getDeviceIds() {
    if (userDeviceIds.length > 0) return userDeviceIds;
    try {
        const res = await fetch(`${BASE_URL}/device?userId=${user.id}`);
        const data = await res.json();
        
        if (data.deviceIds) {
            userDeviceIds = data.deviceIds; // Nhận mảng [1, 2, 3]
        } else {
            userDeviceIds = [data.deviceId || data];
        }
        return userDeviceIds;
    } catch (e) {
        console.error("❌ getDeviceIds error:", e);
        return [68]; // Fake ID
    }
}

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
                        line1: { type: 'line', yMin: 10, yMax: 10, borderColor: '#ef4444', borderWidth: 2, borderDash: [6, 6], label: { display: true, content: 'Cảnh báo', position: 'end', backgroundColor: 'rgba(239, 68, 68, 0.9)', font: { size: 10 } } }
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

function updateChartLine() {
    if (!energyChart) return;
    const val = parseFloat(document.getElementById('chart-threshold')?.value) || 0;
    if(energyChart.options.plugins.annotation.annotations.line1) {
        energyChart.options.plugins.annotation.annotations.line1.yMin = val;
        energyChart.options.plugins.annotation.annotations.line1.yMax = val;
        energyChart.update();
    }
}

function updateMetricsOnly() {
    const priceEl = document.getElementById('elec-price');
    const goalEl = document.getElementById('goal-total');
    if (!priceEl || !goalEl) return;

    const price = parseFloat(priceEl.value) || 0;
    const goal = parseFloat(goalEl.value) || 1;
    
    document.getElementById('money-display').innerText = (totalEnergyGlobal * price).toLocaleString('vi-VN') + " VNĐ";
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

async function fetchApi(mode, timeVal) {
    const idsArray = await getDeviceIds();
    if (!idsArray || idsArray.length === 0) return [];
    
    // Nối mảng thành chuỗi 1,2,3 để Backend nhận diện thành List
    const idsParam = idsArray.join(',');

    let url = "";
    if (mode === 'today') url = `${BASE_URL}/data/day?deviceIds=${idsParam}&day=${timeVal}`;
    if (mode === 'month') url = `${BASE_URL}/energy/monthly?deviceIds=${idsParam}&month=${timeVal}`;
    if (mode === 'year') url = `${BASE_URL}/energy/yearly?deviceIds=${idsParam}&year=${timeVal}`;
    if (mode === 'overview') url = `${BASE_URL}/energy/overview?deviceIds=${idsParam}`;

    try {
        const res = await fetch(url);
        return await res.json() || [];
    } catch (e) {
        console.error("API error:", e);
        return [];
    }
}

function generateLabels(mode, timeVal) {
    let labels = [];
    if (mode === 'today') {
        for (let i = 0; i < 24; i++) labels.push(String(i).padStart(2, '0') + ":00");
    }
    if (mode === 'month') {
        if(!timeVal) return labels;
        const [y, m] = timeVal.split('-');
        const days = new Date(y, m, 0).getDate();
        for (let i = 1; i <= days; i++) labels.push("N" + String(i).padStart(2, '0'));
    }
    if (mode === 'year') {
        for (let i = 1; i <= 12; i++) labels.push("T" + String(i).padStart(2, '0'));
    }
    return labels;
}

async function fetchAndRenderData() {
    const isCompare = document.getElementById('compare-enable')?.checked;

    const idMain = currentMode === 'year' ? 'main-year' : currentMode === 'month' ? 'main-month' : 'main-day';
    const idComp = currentMode === 'year' ? 'comp-year' : currentMode === 'month' ? 'comp-month' : 'comp-day';

    const timeMain = document.getElementById(idMain)?.value;
    const timeComp = document.getElementById(idComp)?.value;

    const rawMain = await fetchApi(currentMode, timeMain);
    const rawComp = isCompare ? await fetchApi(currentMode, timeComp) : [];

    let labels = generateLabels(currentMode, timeMain);
    let mainData = []; let compData = [];
    totalEnergyGlobal = 0;

    if (currentMode === 'overview') {
        labels = rawMain.map(i => "Năm " + (i.nam || ""));
        mainData = rawMain.map(i => parseFloat(i.tong_nang_luong || 0));
        totalEnergyGlobal = mainData.reduce((a, b) => a + (isNaN(b) ? 0 : b), 0);
    } 
    else {
        const mapValue = (raw, label) => {
            let found = null;
            if (currentMode === 'today') found = raw.find(i => i.time === label);
            else if (currentMode === 'month') found = raw.find(i => i.ngay && i.ngay.endsWith("-" + label.slice(1)));
            else if (currentMode === 'year') found = raw.find(i => i.thang && i.thang.endsWith("-" + label.slice(1)));
            
            const val = found ? parseFloat(found.tong_nang_luong) : 0;
            return isNaN(val) ? 0 : val;
        };

        mainData = labels.map(l => {
            const val = mapValue(rawMain, l);
            totalEnergyGlobal += val;
            return val;
        });

        if (isCompare) compData = labels.map(l => mapValue(rawComp, l));
    }

    document.getElementById('energy-main').innerText = totalEnergyGlobal.toLocaleString('vi-VN', { maximumFractionDigits: 2 });
    energyChart.data.labels = labels;
    energyChart.data.datasets[0].data = mainData;
    energyChart.data.datasets[1].data = compData;
    energyChart.data.datasets[1].hidden = !isCompare;
    energyChart.update();
    
    updateMetricsOnly();
}

function switchTab(mode, btn) {
    currentMode = mode;

    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    const allInputs = ['main-day', 'main-month', 'main-year', 'comp-day', 'comp-month', 'comp-year'];
    allInputs.forEach(id => {
        const el = document.getElementById(id);
        if(el) el.classList.add('hidden');
    });
    
    const compSection = document.getElementById('compare-section');
    if (compSection) {
        if (mode === 'overview') {
            compSection.classList.add('hidden');
        } else {
            compSection.classList.remove('hidden');
            if (mode === 'today') {
                document.getElementById('main-day')?.classList.remove('hidden');
                document.getElementById('comp-day')?.classList.remove('hidden');
            } else if (mode === 'month') {
                document.getElementById('main-month')?.classList.remove('hidden');
                document.getElementById('comp-month')?.classList.remove('hidden');
            } else if (mode === 'year') {
                document.getElementById('main-year')?.classList.remove('hidden');
                document.getElementById('comp-year')?.classList.remove('hidden');
            }
        }
    }
    fetchAndRenderData();
}

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
    
    document.getElementById('main-day').value = `${y}-${m}-${d}`;
    document.getElementById('main-month').value = `${y}-${m}`;
    document.getElementById('main-year').value = y;

    const yest = new Date(now); yest.setDate(yest.getDate() - 1);
    document.getElementById('comp-day').value = yest.toISOString().split('T')[0];
    
    let lastM = now.getMonth(); let lastY = y;
    if(lastM === 0) { lastM = 12; lastY--; }
    document.getElementById('comp-month').value = `${lastY}-${String(lastM).padStart(2, '0')}`;
    document.getElementById('comp-year').value = y - 1;
}

window.onload = async () => {
    startClock();
    setInitialDates();
    initChart();
    await getDeviceIds(); // Chờ lấy xong list IDs
    switchTab('today', document.querySelector('.tab-btn.active') || document.querySelectorAll('.tab-btn')[0]);
};