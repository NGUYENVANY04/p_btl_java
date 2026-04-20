// Cấu hình Biểu đồ bằng Chart.js
document.addEventListener('DOMContentLoaded', function () {
    initChart();

    if (CONFIG.USE_MOCK_DATA) {
        initSimulatedData(); // Chạy đồ họa ảo
    } else {
        // Sau này Backend Java viết xong API sẽ gọi dòng này
        startRealtimeAPI();
    }

    renderLogs();
    initConnectionToggle();
});

function initConnectionToggle() {
    document.getElementById('btn-toggle-connection')?.addEventListener('click', function () {
        if (isOnline) {
            // Tắt kết nối
            clearInterval(simulationInterval);
            isOnline = false;
            this.innerHTML = '<i class="fa-solid fa-power-off mr-1"></i> Bật lại (Test)';
            this.classList.replace('bg-slate-700', 'bg-red-500/20');
            this.classList.replace('hover:bg-slate-600', 'hover:bg-red-500/40');
            this.classList.replace('text-slate-300', 'text-red-400');
            this.classList.replace('border-slate-600', 'border-red-500/50');

            const statusDot = document.getElementById('status-dot');
            const statusText = document.getElementById('status-text');
            if (statusDot) statusDot.className = 'h-2.5 w-2.5 rounded-full bg-slate-500';
            if (statusText) {
                statusText.className = 'text-xs font-semibold text-slate-400';
                statusText.innerText = 'Offline';
            }
            document.getElementById('last-update').innerText = 'Mất kết nối server';
            document.getElementById('last-update').style.color = '#ef4444'; // Đỏ
        } else {
            // Bật kết nối
            isOnline = true;
            this.innerHTML = '<i class="fa-solid fa-power-off mr-1"></i> Ngắt kết nối (Test)';
            this.classList.replace('bg-red-500/20', 'bg-slate-700');
            this.classList.replace('hover:bg-red-500/40', 'hover:bg-slate-600');
            this.classList.replace('text-red-400', 'text-slate-300');
            this.classList.replace('border-red-500/50', 'border-slate-600');

            const statusDot = document.getElementById('status-dot');
            const statusText = document.getElementById('status-text');
            if (statusDot) statusDot.className = 'h-2.5 w-2.5 rounded-full bg-green-500 pulse-green';
            if (statusText) {
                statusText.className = 'text-xs font-semibold text-green-400';
                statusText.innerText = 'Online';
            }

            if (CONFIG.USE_MOCK_DATA) {
                startSimulation();
            } else {
                startRealtimeAPI();
            }
        }
    });
}

// ================= CẤU HÌNH KẾT NỐI API =================
const CONFIG = {
    USE_MOCK_DATA: false,
    API_BASE_URL: 'http://localhost:8080/api/quochoc'
};

async function startRealtimeAPI() {
    // Gọi API mỗi 3 giây thay vì random
    simulationInterval = setInterval(async () => {
        if (!isOnline) return;
        try {
            // Lấy ngày từ ô Bộ Lọc trên màn hình (Mặc định 2026-04-17 hoặc ngày user chọn)
            const today = document.getElementById('filter-date').value;

            // ĐÂY LÀ DÒNG GỌI XUỐNG JAVA API của nhánh quochoc
            const response = await fetch(`${CONFIG.API_BASE_URL}/data/day?day=${today}`);
            if (!response.ok) throw new Error("Lỗi mạng");

            // Java trả về Mảng JSON chứa tất cả data trong ngày
            const jsonArray = await response.json();
            if (!jsonArray || jsonArray.length === 0) return;

            // Lấy phần tử mới nhất ở cuối mảng làm dòng Realtime
            const latestData = jsonArray[jsonArray.length - 1];
            const data = {
                u: latestData.voltage || 0.0,
                i: latestData.current || 0.0,
                p: latestData.power || 0.0
            };

            // 1. Đổ dữ liệu vào HTML
            document.getElementById('rt-voltage').innerText = data.u.toFixed(1);
            document.getElementById('rt-current').innerText = data.i.toFixed(2);
            document.getElementById('rt-power').innerText = data.p.toFixed(1);

            // 2. Cập nhật thanh màu (Load Progress)
            const percent = Math.min((data.p / 1200) * 100, 100);
            const loadProgress = document.getElementById('load-progress');
            if (loadProgress) {
                loadProgress.style.width = percent + '%';
                if (percent > 66) {
                    loadProgress.className = 'h-1.5 rounded-full bg-red-500';
                    // Kích hoạt Toast Cảnh Báo Quá Tải
                    if (!window.hasAlertedSpike) {
                        showToast('Cảnh báo quá tải thiết bị!', `Công suất hiện tại là <b class="text-white">${data.p.toFixed(1)}W</b>, vượt ngưỡng an toàn (800W). Giảm tải ngay!`, true);
                        window.hasAlertedSpike = true;
                        setTimeout(() => window.hasAlertedSpike = false, 10000); // Không spam liên tục trong 10s
                    }
                }
                else if (percent > 40) loadProgress.className = 'h-1.5 rounded-full bg-yellow-400';
                else loadProgress.className = 'h-1.5 rounded-full bg-green-400';
            }

            // 3. Cập nhật nhãn "Vừa xong"
            const lastUpdate = document.getElementById('last-update');
            if (lastUpdate) {
                lastUpdate.innerText = 'Vừa xong';
                lastUpdate.style.color = '#38bdf8';
                setTimeout(() => { if (isOnline) lastUpdate.style.color = '#cbd5e1'; }, 500);
            }

        } catch (error) {
            console.error("Không thể kết nối Backend Java:", error);
            // Nếu Java sập, có thể tự động bóp cò Offline UI luôn.
        }
    }, 3000);
}
// =======================================================

let consumptionChart;
let simulationInterval;
let isOnline = true;

function initChart() {
    const ctx = document.getElementById('consumptionChart').getContext('2d');

    // Gradient cho vùng fill biểu đồ
    let gradientFill = ctx.createLinearGradient(0, 0, 0, 400);
    gradientFill.addColorStop(0, 'rgba(45, 212, 191, 0.5)'); // Teal-400
    gradientFill.addColorStop(1, 'rgba(45, 212, 191, 0)');

    consumptionChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['00:00', '03:00', '06:00', '09:00', '12:00', '15:00', '18:00', '21:00'],
            datasets: [{
                label: 'Điện năng tiêu thụ (kWh)',
                data: [0.5, 0.3, 1.2, 2.5, 2.0, 3.5, 4.8, 2.1],
                borderColor: '#2dd4bf', // Teal-400
                backgroundColor: gradientFill,
                borderWidth: 2,
                pointBackgroundColor: '#111827',
                pointBorderColor: '#2dd4bf',
                pointBorderWidth: 2,
                pointRadius: 4,
                pointHoverRadius: 6,
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(15, 23, 42, 0.9)',
                    titleColor: '#e2e8f0',
                    bodyColor: '#2dd4bf',
                    borderColor: 'rgba(255,255,255,0.1)',
                    borderWidth: 1,
                    padding: 10,
                    displayColors: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: 'rgba(255, 255, 255, 0.05)', borderDash: [5, 5] },
                    ticks: { color: '#94a3b8', font: { size: 11 } }
                },
                x: {
                    grid: { display: false },
                    ticks: { color: '#94a3b8', font: { size: 11 } }
                }
            },
            interaction: {
                intersect: false,
                mode: 'index',
            },
        }
    });
}

function initSimulatedData() {
    startSimulation();
}

function startSimulation() {
    // Mô phỏng cập nhật số liệu thời gian thực
    simulationInterval = setInterval(() => {
        if (!isOnline) return;
        // Dao động điện áp khoảng 220V - 225V
        const u = parseFloat((220 + Math.random() * 5).toFixed(1));
        // Dao động dòng điện. Ở Bước 3 ta cho thêm tỷ lệ 10% dòng điện tăng vọt gây quá tải!
        let i = parseFloat((2.0 + Math.random() * 1.5).toFixed(2));
        if (Math.random() < 0.1) {
            i = parseFloat((i + 2.5).toFixed(2)); // Dòng điện vọt lên bất thường
        }

        // Công suất (P = U * I)
        const p = parseFloat((u * i).toFixed(1));

        document.getElementById('rt-voltage').innerText = u;
        document.getElementById('rt-current').innerText = i;
        document.getElementById('rt-power').innerText = p;

        // Cập nhật thanh progress công suất (Giả sử Max ngưỡng 1200W, Threshold Cảnh báo 800W)
        const percent = Math.min((p / 1200) * 100, 100);
        const loadProgress = document.getElementById('load-progress');
        if (loadProgress) {
            loadProgress.style.width = percent + '%';
            if (percent > 66) { // ~800W
                loadProgress.className = 'h-1.5 rounded-full bg-red-500';

                // Kích hoạt Toast Cảnh Báo Quá Tải
                if (!window.hasAlertedSpike) {
                    showToast('Cảnh báo quá tải thiết bị!', `Công suất hiện tại là <b class="text-white">${p}W</b>, vượt ngưỡng an toàn (800W). Giảm tải ngay!`, true);
                    window.hasAlertedSpike = true;
                    setTimeout(() => window.hasAlertedSpike = false, 10000); // Không spam liên tục trong 10s
                }
            }
            else if (percent > 40) loadProgress.className = 'h-1.5 rounded-full bg-yellow-400';
            else loadProgress.className = 'h-1.5 rounded-full bg-green-400';
        }

        // Cập nhật flash label "Vừa xong"
        const lastUpdate = document.getElementById('last-update');
        if (lastUpdate) {
            lastUpdate.innerText = 'Vừa xong';
            lastUpdate.style.color = '#38bdf8'; // Blue sáng lên
            setTimeout(() => {
                if (isOnline) lastUpdate.style.color = '#cbd5e1'; // Trở lại màu slate
            }, 500);
        }

    }, 3000); // Cập nhật mỗi 3 giây
}

// Helper: Hiển thị Toast Notification (Bước 3)
function showToast(title, message, isError = true) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    const borderColor = isError ? 'border-red-500/50' : 'border-blue-500/50';
    const bgColor = isError ? 'bg-red-950/80' : 'bg-blue-950/80';
    const iconColor = isError ? 'text-red-400' : 'text-blue-400';
    const icon = isError ? 'fa-triangle-exclamation' : 'fa-bell';

    // UI Toast
    toast.className = `glass-panel ${bgColor} border ${borderColor} p-4 rounded-xl shadow-2xl flex items-start gap-3 w-80 transform transition-all duration-300 translate-x-full opacity-0`;

    toast.innerHTML = `
        <i class="fa-solid ${icon} ${iconColor} text-2xl mt-0.5"></i>
        <div class="flex-1">
            <h4 class="text-white font-bold text-sm tracking-wide leading-tight">${title}</h4>
            <p class="text-slate-300 text-xs mt-1 leading-relaxed">${message}</p>
        </div>
        <button class="text-slate-400 hover:text-white transition-colors" onclick="this.parentElement.style.opacity=0; setTimeout(()=>this.parentElement.remove(), 300)">
            <i class="fa-solid fa-xmark"></i>
        </button>
    `;

    container.appendChild(toast);

    // Bật animation (slide in)
    requestAnimationFrame(() => {
        toast.classList.remove('translate-x-full', 'opacity-0');
    });

    // Tự động tắt sau 6 giây
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 6000);
}

// Hàm render dữ liệu bảng log tĩnh (thay vì code cứng trong html)
async function renderLogs() {
    try {
        const tbody = document.getElementById('log-table-body');
        if (!tbody) return;

        if (CONFIG.USE_MOCK_DATA) {
            // Chế độ giả lập vì API chưa có data thật
            const logs = [
                { time: "16:30:00 17/04/2026", device: "Nguyễn Văn A (Nhà 101)", u: 224.5, i: 2.45, p: 550.0 },
                { time: "16:15:00 17/04/2026", device: "Trần Thị B (Nhà 102)", u: 225.1, i: 2.50, p: 562.7 },
                { time: "16:00:00 17/04/2026", device: "Trần Thị B (Nhà 102)", u: 223.8, i: 1.10, p: 246.1 },
                { time: "15:45:00 17/04/2026", device: "Lê Văn C (Nhà 103)", u: 224.0, i: 1.05, p: 235.2 },
                { time: "15:30:00 17/04/2026", device: "Nguyễn Văn A (Nhà 101)", u: 222.5, i: 0.95, p: 211.3 }
            ];
            tbody.innerHTML = '';

            // Lấy id filter user hiện tại
            const filterUser = document.getElementById('filter-user').value;
            let displayLogs = logs;
            if (filterUser !== "all") {
                const userTextMap = { "101": "Nguyễn Văn A", "102": "Trần Thị B", "103": "Lê Văn C" };
                displayLogs = logs.filter(l => l.device.includes(userTextMap[filterUser]));
            }

            displayLogs.forEach(log => {
                const tr = document.createElement('tr');
                tr.className = 'hover:bg-slate-800/30 transition-colors';
                tr.innerHTML = `
                    <td class="px-4 py-3">${log.time}</td>
                    <td class="px-4 py-3 font-semibold text-indigo-300 border-l border-indigo-900/30">${log.device}</td>
                    <td class="px-4 py-3 text-blue-300">${log.u}</td>
                    <td class="px-4 py-3 text-orange-300">${log.i}</td>
                    <td class="px-4 py-3 text-yellow-300">${log.p}</td>
                `;
                tbody.appendChild(tr);
            });
            return;
        }

        // Chế độ API thật
        const today = document.getElementById('filter-date').value;
        const filterUserAPI = document.getElementById('filter-user').value;

        const response = await fetch(`${CONFIG.API_BASE_URL}/data/day?day=${today}`);
        if (!response.ok) return;

        const logsData = await response.json();
        tbody.innerHTML = '';

        // Cắt bớt và lật ngược
        let displayLogsAPI = logsData.reverse().slice(0, 50);

        // Giả sử API chưa trả về tên khách hàng, ta gán tên ảo để demo Admin View
        const demoNames = ["Nguyễn Văn A (Nhà 101)", "Trần Thị B (Nhà 102)", "Lê Văn C (Nhà 103)"];
        let counter = 0;

        displayLogsAPI.forEach(log => {
            // Fake assigned name cho trường hợp data thật thiếu device_id
            let assignedName = demoNames[counter % demoNames.length];
            if (filterUserAPI !== "all") {
                const sel = document.getElementById('filter-user');
                assignedName = sel.options[sel.selectedIndex].text;
            } else {
                counter++;
            }

            const tr = document.createElement('tr');
            tr.className = 'hover:bg-slate-800/30 transition-colors';
            // Ghép time và ngày hôm nay
            const displayTime = `${log.time} ${today.split('-').reverse().join('/')}`;
            tr.innerHTML = `
                <td class="px-4 py-3">${displayTime}</td>
                <td class="px-4 py-3 font-semibold text-indigo-300 border-l border-indigo-900/30">${assignedName}</td>
                <td class="px-4 py-3 text-blue-300">${log.voltage || 0.0}</td>
                <td class="px-4 py-3 text-orange-300">${log.current || 0.0}</td>
                <td class="px-4 py-3 text-yellow-300">${log.power || 0.0}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error("Lỗi khi load Logs", e);
    }
}

// Xử lý sự kiện click lọc dữ liệu
document.getElementById('btn-filter')?.addEventListener('click', function () {
    const filterType = document.getElementById('filter-type').value;
    const filterDate = document.getElementById('filter-date').value;

    // Giả lập loading và thay đổi dữ liệu
    const btn = this;
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang tải...';
    btn.disabled = true;

    setTimeout(() => {
        // Sinh mảng dữ liệu ngẫu nhiên để demo biểu đồ
        let newData = Array.from({ length: 8 }, () => (Math.random() * 5).toFixed(1));
        let sum = newData.reduce((a, b) => parseFloat(a) + parseFloat(b), 0).toFixed(1);

        // Cập nhật biểu đồ
        consumptionChart.data.datasets[0].data = newData;
        consumptionChart.update();

        // Cập nhật số liệu tổng quan
        document.getElementById('total-consumption').innerHTML = sum + ' <span class="text-xs text-teal-200">kWh</span>';
        document.getElementById('est-cost').innerHTML = (sum * 2500).toLocaleString('vi-VN') + ' <span class="text-xs text-pink-200">VNĐ</span>';

        // Gọi lại hàm render bảng dữ liệu để load data theo Filter Khách Hàng và Filter Ngày
        renderLogs();

        btn.innerHTML = originalText;
        btn.disabled = false;
    }, 800);
});

// Xử lý sự kiện click xuất dữ liệu Excel
document.getElementById('btn-export')?.addEventListener('click', function () {
    const table = document.getElementById('log-table');
    if (!table) {
        alert("Không tìm thấy bảng dữ liệu!");
        return;
    }

    // Chuyển HTML Table thành Workbook của SheetJS
    const wb = XLSX.utils.table_to_book(table, { sheet: "NhatKyTieuThu" });

    // Tải file về máy (định dạng xlsx)
    XLSX.writeFile(wb, 'LichSu_TieuThuDien.xlsx');
});
