const chartAvailable = typeof Chart !== 'undefined';

if (chartAvailable && typeof ChartDataLabels !== 'undefined') {
    Chart.register(ChartDataLabels);
}

const PIE_COLORS = [
    '#4e73df', '#1cc88a', '#36b9cc', '#f6c23e', '#e74a3b',
    '#858796', '#5a5c69', '#fd7e14', '#20c997', '#6f42c1'
];

let currencyPieChartInstance = null;
let categoryPieChartInstance = null;
let incomeExpenseBarChartInstance = null;
let netBarChartInstance = null;

function fmt(num) {
    if (num == null) return '0';
    return Number(num).toLocaleString('zh-TW', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    });
}

function resetDashboardView() {
    document.getElementById('emptyState').style.display = 'none';
    document.getElementById('dashboardContent').style.display = '';

    document.getElementById('totalAssetsInTwd').textContent = '—';
    document.getElementById('assetsByCurrency').textContent = '';
    document.getElementById('monthlyIncome').textContent = '—';
    document.getElementById('monthlyExpense').textContent = '—';

    document.getElementById('noBreakdownMsg').style.display = 'none';
    document.getElementById('noBreakdownTable').style.display = 'none';
    document.getElementById('categoryPieChart').style.display = '';
    document.getElementById('breakdownTable').style.display = 'none';
    document.getElementById('breakdownTableBody').innerHTML = '';
    document.getElementById('breakdownTotal').textContent = '—';
    document.getElementById('netLabel').textContent = '';

    destroyCharts();
}

function destroyCharts() {
    if (currencyPieChartInstance) { currencyPieChartInstance.destroy(); currencyPieChartInstance = null; }
    if (categoryPieChartInstance) { categoryPieChartInstance.destroy(); categoryPieChartInstance = null; }
    if (incomeExpenseBarChartInstance) { incomeExpenseBarChartInstance.destroy(); incomeExpenseBarChartInstance = null; }
    if (netBarChartInstance) { netBarChartInstance.destroy(); netBarChartInstance = null; }
}

function pieOptions(amounts) {
    const total = amounts.reduce((a, b) => a + b, 0);
    return {
        responsive: true,
        plugins: {
            legend: {
                position: 'bottom',
                labels: { font: { size: 11 }, boxWidth: 12 }
            },
            tooltip: { enabled: false },
            datalabels: {
                color: '#fff',
                font: { size: 11, weight: 'bold' },
                formatter: (value) => {
                    if (total <= 0) return '';
                    const ratio = value / total;
                    if (ratio < 0.05) return '';
                    return `${fmt(value)}\n${(ratio * 100).toFixed(1)}%`;
                }
            }
        }
    };
}

function normalizeApiData(json) {
    if (Array.isArray(json)) return json;
    if (json && Object.prototype.hasOwnProperty.call(json, 'data')) return json.data;
    return json;
}

async function loadTenantSwitcher() {
    const switcher = document.getElementById('tenantSwitcher');
    try {
        const res = await apiFetch('/api/tenants/my');
        if (!res) return;

        const json = await res.json();
        if (!res.ok) {
            console.error('GET /api/tenants/my failed', json);
            switcher.innerHTML = '<option value="">帳本載入失敗</option>';
            return;
        }

        const tenants = normalizeApiData(json);
        if (!Array.isArray(tenants)) {
            console.error('Invalid tenants response', json);
            switcher.innerHTML = '<option value="">帳本資料格式錯誤</option>';
            return;
        }

        switcher.innerHTML = '';
        tenants.forEach(t => {
            const opt = document.createElement('option');
            opt.value = t.tenantId;
            opt.textContent = `${t.tenantName}（${t.tenantType}）`;
            if (t.current) opt.selected = true;
            switcher.appendChild(opt);
        });

        switcher.addEventListener('change', async () => {
            const selectedId = Number(switcher.value);
            if (!selectedId) return;
            await switchTenant(selectedId);
        });
    } catch (e) {
        console.error('loadTenantSwitcher error', e);
        switcher.innerHTML = '<option value="">帳本載入失敗</option>';
    }
}

async function switchTenant(tenantId) {
    try {
        const switchRes = await apiFetch('/api/tenants/switch', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tenantId: tenantId })
        });
        if (!switchRes) return;

        let switchJson = null;
        try { switchJson = await switchRes.json(); } catch (e) { switchJson = null; }

        if (!switchRes.ok) {
            console.error('POST /api/tenants/switch failed', switchJson);
            alert('切換帳本失敗，請重試');
            return;
        }

        const data = normalizeApiData(switchJson);
        if (data && data.accessToken) localStorage.setItem('accessToken', data.accessToken);
        if (data && data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken);

        location.reload();
    } catch (e) {
        console.error('Tenant switch error', e);
        alert('切換帳本時發生錯誤');
    }
}

async function loadDashboard() {
    resetDashboardView();
    try {
        const res = await apiFetch('/api/dashboard/summary');
        if (!res) return;

        const json = await res.json();
        if (!res.ok || json.success === false) {
            console.error('GET /api/dashboard/summary failed', json);
            return;
        }

        const data = normalizeApiData(json);
        if (!data) return;

        if (!data.hasAccounts) {
            document.getElementById('emptyState').style.display = 'block';
            document.getElementById('dashboardContent').style.display = 'none';
            return;
        }

        document.getElementById('totalAssetsInTwd').textContent = 'TWD ' + fmt(data.totalAssetsInTwd);
        const byCurrency = data.totalAssetsByCurrency || {};
        document.getElementById('assetsByCurrency').textContent =
            Object.entries(byCurrency).map(([c, a]) => `${c} ${fmt(a)}`).join('　');
        document.getElementById('monthlyIncome').textContent = 'TWD ' + fmt(data.monthlyIncome);
        document.getElementById('monthlyExpense').textContent = 'TWD ' + fmt(data.monthlyExpense);

        if (!chartAvailable) return;

        const assetsByTwd = data.assetsByTwd || {};
        const curLabels = Object.keys(assetsByTwd);
        const curAmounts = Object.values(assetsByTwd).map(Number);
        currencyPieChartInstance = new Chart(document.getElementById('currencyPieChart'), {
            type: 'doughnut',
            data: { labels: curLabels, datasets: [{ data: curAmounts, backgroundColor: PIE_COLORS.slice(0, curLabels.length), borderWidth: 2 }] },
            options: pieOptions(curAmounts)
        });

        const income = Number(data.monthlyIncome || 0);
        const expense = Number(data.monthlyExpense || 0);
        incomeExpenseBarChartInstance = new Chart(document.getElementById('incomeExpenseBarChart'), {
            type: 'bar',
            data: { labels: ['收入', '支出'], datasets: [{ data: [income, expense], backgroundColor: ['#1cc88a', '#e74a3b'], borderRadius: 4 }] },
            options: {
                responsive: true,
                plugins: {
                    legend: { display: false },
                    datalabels: { anchor: 'end', align: 'end', color: '#444', font: { size: 11, weight: 'bold' }, formatter: v => 'TWD ' + fmt(v) }
                },
                scales: { y: { beginAtZero: true, ticks: { callback: v => fmt(v) } } }
            }
        });

        const net = income - expense;
        const netColor = net >= 0 ? '#1cc88a' : '#e74a3b';
        document.getElementById('netLabel').textContent = (net >= 0 ? '+' : '') + 'TWD ' + fmt(net);
        document.getElementById('netLabel').style.color = netColor;
        netBarChartInstance = new Chart(document.getElementById('netBarChart'), {
            type: 'bar',
            data: { labels: ['淨額'], datasets: [{ data: [Math.abs(net)], backgroundColor: [netColor], borderRadius: 4 }] },
            options: {
                indexAxis: 'y',
                responsive: true,
                plugins: {
                    legend: { display: false },
                    datalabels: { anchor: 'end', align: 'end', color: '#444', font: { size: 11, weight: 'bold' }, formatter: v => 'TWD ' + fmt(v) }
                },
                scales: { x: { beginAtZero: true, ticks: { callback: v => fmt(v) } } }
            }
        });

        const breakdown = data.categoryBreakdown || [];
        if (breakdown.length === 0) {
            document.getElementById('noBreakdownMsg').style.display = 'block';
            document.getElementById('noBreakdownTable').style.display = 'block';
            document.getElementById('categoryPieChart').style.display = 'none';
            return;
        }

        const catLabels = breakdown.map(b => b.categoryName);
        const catAmounts = breakdown.map(b => Number(b.amount || 0));
        const catTotal = catAmounts.reduce((a, b) => a + b, 0);
        categoryPieChartInstance = new Chart(document.getElementById('categoryPieChart'), {
            type: 'pie',
            data: { labels: catLabels, datasets: [{ data: catAmounts, backgroundColor: PIE_COLORS.slice(0, catLabels.length), borderWidth: 2 }] },
            options: pieOptions(catAmounts)
        });

        document.getElementById('breakdownTable').style.display = '';
        document.getElementById('breakdownTotal').textContent = 'TWD ' + fmt(catTotal);
        const tbody = document.getElementById('breakdownTableBody');
        tbody.innerHTML = '';
        breakdown.forEach(b => {
            const amount = Number(b.amount || 0);
            const pct = catTotal > 0 ? (amount / catTotal * 100).toFixed(1) : '0.0';
            const tr = document.createElement('tr');
            tr.innerHTML = `<td>${b.categoryName}</td><td class="text-end">TWD ${fmt(amount)}</td><td class="text-end">${pct}%</td>`;
            tbody.appendChild(tr);
        });

    } catch (e) {
        console.error('Dashboard load failed', e);
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    await loadTenantSwitcher();
    await loadDashboard();
});
