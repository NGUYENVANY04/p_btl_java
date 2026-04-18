// =====================
// API CONFIGURATION
// =====================
// URL
const API_BASE_URL = 'http://localhost:8080/api'; 

const API_ENDPOINTS = {
    getBudget: `${API_BASE_URL}/budget/get`,
    setBudget: `${API_BASE_URL}/budget/set`,
    getUsage: `${API_BASE_URL}/usage/current`,
    getHistory: `${API_BASE_URL}/usage/history`,
    addUsage: `${API_BASE_URL}/usage/add`,
    updatePrice: `${API_BASE_URL}/settings/price`,
};

// =====================
// LOCAL STORAGE CONFIG
// =====================
const STORAGE_KEYS = {
    budget: 'electricity_budget',
    usage: 'electricity_usage',
    history: 'electricity_history',
    price: 'electricity_price',
    warningLevel: 'warning_level',
    month: 'current_month',
};

// =====================
// STATE MANAGEMENT
// =====================
let appState = {
    budget: 0,
    currentUsage: 0,
    monthlyHistory: [],
    pricePerKwh: 3000,
    warningLevel: 80,
    currentMonth: getCurrentMonth(),
    dailyUsageData: [],
};

let dailyChart = null;
let costChart = null;

// =====================
// INITIALIZATION
// =====================
document.addEventListener('DOMContentLoaded', function () {
    initializeApp();
    loadDataFromStorage();
    updateAllUI();
    initializeCharts();
    setCurrentDate();
});

function initializeApp() {
    // Load data from local storage
    loadDataFromStorage();
    
    // Set up event listeners
    setupEventListeners();
    
    // Initialize date input with today
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('dataDate').value = today;
}

function setupEventListeners() {
    // Add data button
    window.addData = addData;
    window.saveBudget = saveBudget;
    window.toggleEditBudget = toggleEditBudget;
    window.updatePrice = updatePrice;
    window.changeMonth = changeMonth;
    window.updateWarningLevel = updateWarningLevel;
    window.refreshData = refreshData;
}

// =====================
// DATA LOADING & SAVING
// =====================
function loadDataFromStorage() {
    const storedBudget = localStorage.getItem(STORAGE_KEYS.budget);
    const storedUsage = localStorage.getItem(STORAGE_KEYS.usage);
    const storedHistory = localStorage.getItem(STORAGE_KEYS.history);
    const storedPrice = localStorage.getItem(STORAGE_KEYS.price);
    const storedWarning = localStorage.getItem(STORAGE_KEYS.warningLevel);
    const storedMonth = localStorage.getItem(STORAGE_KEYS.month);

    appState.budget = storedBudget ? parseFloat(storedBudget) : 0;
    appState.currentUsage = storedUsage ? parseFloat(storedUsage) : 0;
    appState.monthlyHistory = storedHistory ? JSON.parse(storedHistory) : [];
    appState.pricePerKwh = storedPrice ? parseFloat(storedPrice) : 3000;
    appState.warningLevel = storedWarning ? parseFloat(storedWarning) : 80;
    appState.currentMonth = storedMonth || getCurrentMonth();

    // Filter history by current month
    filterHistoryByMonth();
}

function saveDataToStorage() {
    localStorage.setItem(STORAGE_KEYS.budget, appState.budget.toString());
    localStorage.setItem(STORAGE_KEYS.usage, appState.currentUsage.toString());
    localStorage.setItem(STORAGE_KEYS.history, JSON.stringify(appState.monthlyHistory));
    localStorage.setItem(STORAGE_KEYS.price, appState.pricePerKwh.toString());
    localStorage.setItem(STORAGE_KEYS.warningLevel, appState.warningLevel.toString());
    localStorage.setItem(STORAGE_KEYS.month, appState.currentMonth);
}

// =====================
// FETCH DATA FROM API
// =====================
async function fetchBudgetFromAPI() {
    try {
        const response = await fetch(API_ENDPOINTS.getBudget, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });
        
        if (response.ok) {
            const data = await response.json();
            appState.budget = data.budgetLimit || 0;
            saveDataToStorage();
            return true;
        }
    } catch (error) {
        console.log('API not available, using local storage:', error);
    }
    return false;
}

async function fetchUsageFromAPI() {
    try {
        const response = await fetch(API_ENDPOINTS.getUsage, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });
        
        if (response.ok) {
            const data = await response.json();
            appState.currentUsage = data.currentUsage || 0;
            saveDataToStorage();
            return true;
        }
    } catch (error) {
        console.log('API not available, using local storage:', error);
    }
    return false;
}

async function fetchHistoryFromAPI() {
    try {
        const response = await fetch(API_ENDPOINTS.getHistory, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });
        
        if (response.ok) {
            const data = await response.json();
            appState.monthlyHistory = data.history || [];
            filterHistoryByMonth();
            saveDataToStorage();
            return true;
        }
    } catch (error) {
        console.log('API not available, using local storage:', error);
    }
    return false;
}

async function sendDataToAPI(endpoint, data) {
    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            return true;
        }
    } catch (error) {
        console.log('API not available, data saved locally:', error);
    }
    return false;
}

// =====================
// BUDGET MANAGEMENT
// =====================
function toggleEditBudget() {
    const display = document.getElementById('budgetDisplay');
    const form = document.getElementById('budgetEditForm');
    const input = document.getElementById('budgetInput');

    if (form.style.display === 'none') {
        display.style.display = 'none';
        form.style.display = 'block';
        input.value = appState.budget;
        input.focus();
    } else {
        display.style.display = 'block';
        form.style.display = 'none';
    }
}

function saveBudget() {
    const input = document.getElementById('budgetInput');
    const value = parseFloat(input.value);

    if (isNaN(value) || value < 0) {
        alert('Vui lòng nhập hạn mức hợp lệ');
        return;
    }

    appState.budget = value;
    saveDataToStorage();
    
    // Send to API
    sendDataToAPI(API_ENDPOINTS.setBudget, {
        budgetLimit: value,
        month: appState.currentMonth
    });

    toggleEditBudget();
    updateAllUI();
}

// =====================
// USAGE DATA MANAGEMENT
// =====================
function addData() {
    const dateInput = document.getElementById('dataDate');
    const usageInput = document.getElementById('dataUsage');
    const notesInput = document.getElementById('dataNotes');

    const date = dateInput.value;
    const usage = parseFloat(usageInput.value);
    const notes = notesInput.value;

    if (!date || isNaN(usage) || usage < 0) {
        alert('Vui lòng nhập ngày và tiêu thụ hợp lệ');
        return;
    }

    // Add to history
    const entry = {
        date: date,
        usage: usage,
        cost: usage * appState.pricePerKwh,
        notes: notes,
        timestamp: new Date().toISOString()
    };

    appState.monthlyHistory.push(entry);
    
    // Update current usage (sum of all usage in current month)
    recalculateCurrentUsage();
    saveDataToStorage();

    // Send to API
    sendDataToAPI(API_ENDPOINTS.addUsage, entry);

    // Clear inputs
    usageInput.value = '';
    notesInput.value = '';
    dateInput.value = new Date().toISOString().split('T')[0];

    updateAllUI();
}

function recalculateCurrentUsage() {
    const [year, month] = appState.currentMonth.split('-');
    appState.currentUsage = appState.monthlyHistory
        .filter(entry => entry.date.startsWith(appState.currentMonth))
        .reduce((sum, entry) => sum + entry.usage, 0);
}

// =====================
// PRICE MANAGEMENT
// =====================
function updatePrice() {
    const input = document.getElementById('priceInput');
    const value = parseFloat(input.value);

    if (isNaN(value) || value < 0) {
        alert('Vui lòng nhập giá hợp lệ');
        return;
    }

    appState.pricePerKwh = value;
    saveDataToStorage();
    
    // Send to API
    sendDataToAPI(API_ENDPOINTS.updatePrice, {
        pricePerKwh: value
    });

    updateAllUI();
}

// =====================
// WARNING LEVEL
// =====================
function updateWarningLevel() {
    const input = document.getElementById('warningLevel');
    const value = parseFloat(input.value);

    if (isNaN(value) || value < 0 || value > 100) {
        alert('Vui lòng nhập phần trăm từ 0 đến 100');
        return;
    }

    appState.warningLevel = value;
    saveDataToStorage();
    updateAllUI();
}

// =====================
// MONTH MANAGEMENT
// =====================
function changeMonth() {
    const monthInput = document.getElementById('monthInput');
    const newMonth = monthInput.value;

    if (!newMonth) {
        alert('Vui lòng chọn tháng');
        return;
    }

    appState.currentMonth = newMonth;
    saveDataToStorage();
    filterHistoryByMonth();
    updateAllUI();
}

function getCurrentMonth() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    return `${year}-${month}`;
}

function filterHistoryByMonth() {
    appState.monthlyHistory = appState.monthlyHistory.filter(entry => 
        entry.date.startsWith(appState.currentMonth)
    );
}

function setCurrentDate() {
    const monthInput = document.getElementById('monthInput');
    monthInput.value = appState.currentMonth;
}

// =====================
// UI UPDATES
// =====================
function updateAllUI() {
    updateBudgetDisplay();
    updateUsageDisplay();
    updatePercentage();
    updateCostDisplay();
    updateProgressBar();
    updateHistoryTable();
    updateCharts();
    updateStatusText();
}

function updateBudgetDisplay() {
    const budgetDisplay = document.getElementById('budgetDisplay');
    budgetDisplay.querySelector('.amount').textContent = appState.budget.toFixed(1);
    document.getElementById('budgetInput').value = appState.budget;
}

function updateUsageDisplay() {
    const now = new Date();
    const timeString = String(now.getHours()).padStart(2, '0') + ':' + 
                       String(now.getMinutes()).padStart(2, '0');
    
    document.getElementById('currentUsage').textContent = appState.currentUsage.toFixed(2);
    document.getElementById('lastUpdate').textContent = timeString;
}

function updatePercentage() {
    const percentage = appState.budget > 0 
        ? (appState.currentUsage / appState.budget * 100) 
        : 0;
    
    document.getElementById('percentageValue').textContent = Math.min(percentage, 100).toFixed(1) + '%';
}

function updateCostDisplay() {
    const estimatedCost = appState.currentUsage * appState.pricePerKwh;
    document.getElementById('estimatedCost').textContent = Math.round(estimatedCost).toLocaleString('vi-VN');
    document.getElementById('pricePerKwh').textContent = appState.pricePerKwh.toLocaleString('vi-VN');
    document.getElementById('priceInput').value = appState.pricePerKwh;
}

function updateProgressBar() {
    const percentage = appState.budget > 0 
        ? (appState.currentUsage / appState.budget * 100) 
        : 0;
    
    const clampedPercentage = Math.min(percentage, 100);
    const progressBar = document.getElementById('progressBar');
    const progressLabel = document.getElementById('progressLabel');
    const remainingKwh = appState.budget - appState.currentUsage;

    progressBar.style.width = clampedPercentage + '%';
    progressLabel.textContent = clampedPercentage.toFixed(1) + '%';
    document.getElementById('remainingKwh').textContent = Math.max(remainingKwh, 0).toFixed(2);
}

function updateStatusText() {
    const percentage = appState.budget > 0 
        ? (appState.currentUsage / appState.budget * 100) 
        : 0;
    
    const statusElement = document.getElementById('statusText');
    let status = '';
    let bgColor = '';

    if (percentage >= 100) {
        status = ' Đã vượt quá hạn mức!';
        bgColor = '#fee2e2';
        statusElement.style.color = '#dc2626';
    } else if (percentage >= appState.warningLevel) {
        status = ' Cảnh báo: đang tiếp cận hạn mức';
        bgColor = '#fef3c7';
        statusElement.style.color = '#d97706';
    } else {
        status = ' Sử dụng bình thường';
        bgColor = '#d1fae5';
        statusElement.style.color = '#059669';
    }

    statusElement.textContent = status;
    statusElement.style.background = bgColor;
}

// =====================
// HISTORY TABLE
// =====================
function updateHistoryTable() {
    const tbody = document.getElementById('historyTableBody');
    tbody.innerHTML = '';

    if (appState.monthlyHistory.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: #999;">Chưa có dữ liệu</td></tr>';
        return;
    }

    // Sort by date (newest first)
    const sortedHistory = [...appState.monthlyHistory].sort((a, b) => 
        new Date(b.date) - new Date(a.date)
    );

    sortedHistory.forEach(entry => {
        const row = document.createElement('tr');
        const dateObj = new Date(entry.date);
        const dateString = dateObj.toLocaleDateString('vi-VN');

        row.innerHTML = `
            <td>${dateString}</td>
            <td>${entry.usage.toFixed(2)} kWh</td>
            <td>${Math.round(entry.cost).toLocaleString('vi-VN')} ₫</td>
            <td>${entry.notes || '-'}</td>
        `;

        tbody.appendChild(row);
    });
}

// =====================
// CHARTS
// =====================
function initializeCharts() {
    // Daily usage chart
    const dailyCtx = document.getElementById('dailyChart');
    if (dailyCtx) {
        dailyChart = new Chart(dailyCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: 'Tiêu Thụ (kWh)',
                    data: [],
                    borderColor: '#2563eb',
                    backgroundColor: 'rgba(37, 99, 235, 0.1)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4,
                    pointBackgroundColor: '#2563eb',
                    pointBorderColor: '#fff',
                    pointBorderWidth: 2,
                    pointRadius: 5,
                    pointHoverRadius: 7,
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        display: true,
                        position: 'top',
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: 'kWh'
                        }
                    }
                }
            }
        });
    }

    // Cost distribution chart
    const costCtx = document.getElementById('costChart');
    if (costCtx) {
        costChart = new Chart(costCtx, {
            type: 'doughnut',
            data: {
                labels: ['Đã sử dụng', 'Còn lại'],
                datasets: [{
                    data: [],
                    backgroundColor: ['#2563eb', '#e5e7eb'],
                    borderColor: ['#fff', '#fff'],
                    borderWidth: 2,
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        display: true,
                        position: 'bottom',
                    }
                }
            }
        });
    }
}

function updateCharts() {
    if (!dailyChart || !costChart) return;

    // Prepare daily data
    const dailyData = {};
    appState.monthlyHistory.forEach(entry => {
        if (!dailyData[entry.date]) {
            dailyData[entry.date] = 0;
        }
        dailyData[entry.date] += entry.usage;
    });

    const sortedDates = Object.keys(dailyData).sort();
    const usageValues = sortedDates.map(date => dailyData[date]);

    // Update daily chart
    dailyChart.data.labels = sortedDates.map(date => {
        const dateObj = new Date(date);
        return dateObj.toLocaleDateString('vi-VN', { month: 'short', day: 'numeric' });
    });
    dailyChart.data.datasets[0].data = usageValues;
    dailyChart.update();

    // Update cost chart
    const remaining = Math.max(appState.budget - appState.currentUsage, 0);
    costChart.data.datasets[0].data = [appState.currentUsage, remaining];
    costChart.update();
}

// =====================
// REFRESH DATA
// =====================
async function refreshData() {
    try {
        // Try to fetch from API
        await Promise.all([
            fetchBudgetFromAPI(),
            fetchUsageFromAPI(),
            fetchHistoryFromAPI()
        ]);

        updateAllUI();
        showNotification('Dữ liệu đã được cập nhật', 'success');
    } catch (error) {
        showNotification('Không thể cập nhật từ API, sử dụng dữ liệu cục bộ', 'info');
    }
}

// =====================
// NOTIFICATIONS
// =====================
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        background: ${type === 'success' ? '#10b981' : '#3b82f6'};
        color: white;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
        z-index: 9999;
        animation: slideIn 0.3s ease;
    `;
    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Add CSS for notifications
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// =====================
// UTILITIES
// =====================
function formatCurrency(value) {
    return value.toLocaleString('vi-VN', {
        style: 'currency',
        currency: 'VND'
    });
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
}
