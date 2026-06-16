let currentPage = 0;
const PAGE_SIZE = 20;

let filterUserId = null;
let filterAction = '';
let filterDateFrom = '';
let filterDateTo = '';

let modalInstance = null;

document.addEventListener('DOMContentLoaded', function () {
    loadLogs(0);

    document.getElementById('searchBtn').addEventListener('click', function () {
        applyFilters();
        loadLogs(0);
    });

    document.getElementById('resetBtn').addEventListener('click', function () {
        document.getElementById('filterUserId').value = '';
        document.getElementById('filterAction').value = '';
        document.getElementById('filterDateFrom').value = '';
        document.getElementById('filterDateTo').value = '';
        filterUserId = null;
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
});

function applyFilters() {
    const userIdVal = document.getElementById('filterUserId').value.trim();
    filterUserId = userIdVal ? Number(userIdVal) : null;
    filterAction = document.getElementById('filterAction').value;
    filterDateFrom = document.getElementById('filterDateFrom').value;
    filterDateTo = document.getElementById('filterDateTo').value;
}

async function loadLogs(page) {
    currentPage = page;
    const params = new URLSearchParams({ page, size: PAGE_SIZE });
    if (filterUserId) params.set('userId', filterUserId);
    if (filterAction) params.set('action', filterAction);
    if (filterDateFrom) params.set('dateFrom', filterDateFrom);
    if (filterDateTo) params.set('dateTo', filterDateTo);

    const res = await apiFetch('/api/admin/audit-logs?' + params.toString());
    if (!res) return;

    const body = await res.json();
    if (!body.success) return;

    const data = body.data;
    renderTable(data.content);
    renderPagination(data.totalPages, data.number);

    const totalEl = document.getElementById('totalCountLabel');
    totalEl.textContent = '共 ' + data.totalElements + ' 筆';
}

function renderTable(logs) {
    const tbody = document.getElementById('auditLogTableBody');
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">無資料</td></tr>';
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
