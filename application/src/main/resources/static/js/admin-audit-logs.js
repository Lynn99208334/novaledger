let currentPage = 0;
const PAGE_SIZE = 20;

let filterUsername = '';
let filterAction = '';
let filterDateFrom = '';
let filterDateTo = '';
let sortCol = 'id';
let sortDir = 'desc';

let allLogs = [];
let modalInstance = null;
let debounceTimer = null;

document.addEventListener('DOMContentLoaded', function () {
    loadLogs(0);

    // Select2 初始化
    Select2Helper.init('#filterAction');

    // Select2 change 事件
    Select2Helper.onChange('#filterAction', function () {
        filterAction = Select2Helper.getValue('#filterAction');
        loadLogs(0);
    });

    // Username 即時 debounce 搜尋
    document.getElementById('filterUsername').addEventListener('input', function () {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(function () {
            filterUsername = document.getElementById('filterUsername').value.trim();
            loadLogs(0);
        }, 400);
    });

    document.getElementById('searchBtn').addEventListener('click', function () {
        applyFilters();
        loadLogs(0);
    });

    document.getElementById('resetBtn').addEventListener('click', function () {
        document.getElementById('filterUsername').value = '';
        Select2Helper.clear('#filterAction');
        document.getElementById('filterDateFrom').value = '';
        document.getElementById('filterDateTo').value = '';
        filterUsername = '';
        filterAction = '';
        filterDateFrom = '';
        filterDateTo = '';
        loadLogs(0);
    });

    // Event delegation — 展開詳情
    document.getElementById('auditLogTableBody').addEventListener('click', function (e) {
        const btn = e.target.closest('button[data-action="detail"]');
        if (!btn) return;
        const before = btn.getAttribute('data-before') || '（無）';
        const after = btn.getAttribute('data-after') || '（無）';
        showDetailModal(before, after);
    });

    // 欄位排序
    document.querySelectorAll('th.sortable').forEach(function (th) {
        th.addEventListener('click', function () {
            const col = th.getAttribute('data-col');
            if (sortCol === col) {
                sortDir = sortDir === 'asc' ? 'desc' : 'asc';
            } else {
                sortCol = col;
                sortDir = 'asc';
            }
            document.querySelectorAll('th.sortable .sort-icon').forEach(el => el.textContent = '↕');
            th.querySelector('.sort-icon').textContent = sortDir === 'asc' ? '↑' : '↓';
            renderTable(sortLogs(allLogs));
        });
    });
});

function applyFilters() {
    filterUsername = document.getElementById('filterUsername').value.trim();
    filterAction = Select2Helper.getValue('#filterAction');
    filterDateFrom = document.getElementById('filterDateFrom').value;
    filterDateTo = document.getElementById('filterDateTo').value;
}

async function loadLogs(page) {
    currentPage = page;
    const params = new URLSearchParams({ page, size: PAGE_SIZE });
    if (filterUsername) params.set('username', filterUsername);
    if (filterAction) params.set('action', filterAction);
    if (filterDateFrom) params.set('dateFrom', filterDateFrom);
    if (filterDateTo) params.set('dateTo', filterDateTo);

    const res = await apiFetch('/api/admin/audit-logs?' + params.toString());
    if (!res) return;

    const body = await res.json();
    if (!body.success) return;

    const data = body.data;
    allLogs = data.content;
    renderTable(sortLogs(allLogs));
    renderPagination(data.totalPages, data.number);

    const totalEl = document.getElementById('totalCountLabel');
    totalEl.textContent = '共 ' + data.totalElements + ' 筆';
}

function sortLogs(logs) {
    if (!logs) return [];
    return [...logs].sort(function (a, b) {
        let aVal = a[sortCol];
        let bVal = b[sortCol];
        if (aVal == null) return 1;
        if (bVal == null) return -1;
        if (typeof aVal === 'string') aVal = aVal.toLowerCase();
        if (typeof bVal === 'string') bVal = bVal.toLowerCase();
        if (aVal < bVal) return sortDir === 'asc' ? -1 : 1;
        if (aVal > bVal) return sortDir === 'asc' ? 1 : -1;
        return 0;
    });
}

function renderTable(logs) {
    const tbody = document.getElementById('auditLogTableBody');
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4">無資料</td></tr>';
        return;
    }

    tbody.innerHTML = logs.map(function (log) {
        const before = escapeHtml(log.beforeValue || '');
        const after = escapeHtml(log.afterValue || '');
        const hasDetail = log.beforeValue || log.afterValue;

        const detailBtn = hasDetail
            ? '<button class="btn btn-outline-secondary btn-sm" data-action="detail" ' +
              'data-before="' + before + '" data-after="' + after + '">展開</button>'
            : '<span class="text-muted small">－</span>';

        return '<tr>' +
            '<td>' + log.id + '</td>' +
            '<td>' + (log.userId || '－') + '</td>' +
            '<td>' + (log.username ? escapeHtml(log.username) : '－') + '</td>' +
            '<td><span class="badge bg-secondary">' + escapeHtml(log.action) + '</span></td>' +
            '<td>' + escapeHtml(log.targetType) + '</td>' +
            '<td>' + escapeHtml(log.targetId || '－') + '</td>' +
            '<td>' + escapeHtml(log.ipAddress || '－') + '</td>' +
            '<td>' + formatDatetime(log.createdAt) + '</td>' +
            '<td class="text-center">' + detailBtn + '</td>' +
            '</tr>';
    }).join('');
}

function renderPagination(totalPages, currentPageNum) {
    const ul = document.getElementById('pagination');
    if (totalPages <= 1) { ul.innerHTML = ''; return; }

    let html = '';
    html += '<li class="page-item' + (currentPageNum === 0 ? ' disabled' : '') + '">' +
        '<a class="page-link" href="#" data-page="' + (currentPageNum - 1) + '">«</a></li>';

    const start = Math.max(0, currentPageNum - 2);
    const end = Math.min(totalPages - 1, currentPageNum + 2);
    for (let i = start; i <= end; i++) {
        html += '<li class="page-item' + (i === currentPageNum ? ' active' : '') + '">' +
            '<a class="page-link" href="#" data-page="' + i + '">' + (i + 1) + '</a></li>';
    }

    html += '<li class="page-item' + (currentPageNum === totalPages - 1 ? ' disabled' : '') + '">' +
        '<a class="page-link" href="#" data-page="' + (currentPageNum + 1) + '">»</a></li>';

    ul.innerHTML = html;

    ul.addEventListener('click', function (e) {
        e.preventDefault();
        const a = e.target.closest('a[data-page]');
        if (!a) return;
        const p = Number(a.getAttribute('data-page'));
        if (p >= 0 && p < totalPages) loadLogs(p);
    });
}

function showDetailModal(before, after) {
    document.getElementById('detailBefore').textContent = before || '（無）';
    document.getElementById('detailAfter').textContent = after || '（無）';

    if (!modalInstance) {
        modalInstance = new bootstrap.Modal(document.getElementById('detailModal'));
    }
    modalInstance.show();
}

function formatDatetime(isoStr) {
    if (!isoStr) return '－';
    const d = new Date(isoStr);
    return d.getFullYear() + '-' +
        String(d.getMonth() + 1).padStart(2, '0') + '-' +
        String(d.getDate()).padStart(2, '0') + ' ' +
        String(d.getHours()).padStart(2, '0') + ':' +
        String(d.getMinutes()).padStart(2, '0');
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
