document.addEventListener('DOMContentLoaded', function () {
    loadDashboardStats();
});

async function loadDashboardStats() {
    const res = await apiFetch('/page/admin/api/dashboard/stats');
    if (!res) return;

    const body = await res.json();
    if (!body.success) return;

    const data = body.data;

    // PENDING 匯入任務
    document.getElementById('statPendingJobs').textContent = data.pendingJobCount;

    // 近 24h error log（-1 表示尚未接上，顯示 N/A）
    const errorEl = document.getElementById('statErrorLogs');
    errorEl.textContent = data.recentErrorLogCount === -1 ? 'N/A' : data.recentErrorLogCount;

    // DB 連線狀態
    const dbEl = document.getElementById('statDb');
    const dbIcon = document.getElementById('statDbIcon');
    if (data.dbHealthy) {
        dbEl.textContent = '正常';
        dbIcon.classList.add('text-success');
    } else {
        dbEl.textContent = '異常';
        dbIcon.classList.add('text-danger');
    }
}
