let currentPage = 0;
let currentKeyword = '';
const PAGE_SIZE = 20;
let debounceTimer = null;
let isDevEnv = false;

document.addEventListener('DOMContentLoaded', function () {
    checkDevEnv().then(() => {
        loadUsers(0, '');
    });

    // 即時搜尋（300ms debounce）
    document.getElementById('keywordInput').addEventListener('input', function () {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            currentKeyword = this.value.trim();
            loadUsers(0, currentKeyword);
        }, 300);
    });

    document.getElementById('resetBtn').addEventListener('click', function () {
        document.getElementById('keywordInput').value = '';
        currentKeyword = '';
        loadUsers(0, '');
    });

    // Event delegation
    document.getElementById('userTableBody').addEventListener('click', function (e) {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;
        const action = btn.getAttribute('data-action');
        const userId = btn.getAttribute('data-user-id');
        if (action === 'disable') disableUser(Number(userId));
        if (action === 'enable') enableUser(Number(userId));
        if (action === 'delete') deleteUser(Number(userId), btn.getAttribute('data-username'));
    });
});

async function checkDevEnv() {
    try {
        const res = await apiFetch('/api/dev/env');
        if (res && res.ok) {
            const body = await res.json();
            isDevEnv = body.success && body.data && body.data.env === 'dev';
        }
    } catch (e) {
        isDevEnv = false;
    }
}

async function loadUsers(page, keyword) {
    currentPage = page;
    const params = new URLSearchParams({ page, size: PAGE_SIZE, keyword });
    const res = await apiFetch('/api/admin/users?' + params.toString());
    if (!res) return;

    const body = await res.json();
    if (!body.success) return;

    const data = body.data;
    renderTable(data.content);
    renderPagination(data.totalPages, data.number);

    const totalEl = document.getElementById('totalCountLabel');
    totalEl.textContent = '共 ' + data.totalElements + ' 筆';
}

function renderTable(users) {
    const tbody = document.getElementById('userTableBody');
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4">無資料</td></tr>';
        return;
    }

    tbody.innerHTML = users.map(u => {
        const emailBadge = u.emailVerified
            ? '<span class="badge bg-success">已驗證</span>'
            : '<span class="badge bg-secondary">未驗證</span>';

        const statusBadge = u.enabled
            ? '<span class="badge bg-success">' + u.status + '</span>'
            : '<span class="badge bg-danger">' + u.status + '</span>';

        const adminBadge = u.isSystemAdmin
            ? '<span class="badge bg-danger">ADMIN</span>'
            : '<span class="text-muted">－</span>';

        const lastLogin = u.lastLoginAt ? formatDatetime(u.lastLoginAt) : '－';
        const createdAt = formatDatetime(u.createdAt);

        let actionBtns;
        if (u.isSystemAdmin) {
            actionBtns = '<span class="text-muted small">不可操作</span>';
        } else {
            const toggleBtn = u.enabled
                ? '<button class="btn btn-outline-danger btn-sm" data-action="disable" data-user-id="' + u.id + '">停用</button>'
                : '<button class="btn btn-outline-success btn-sm" data-action="enable" data-user-id="' + u.id + '">啟用</button>';

            const deleteBtn = isDevEnv
                ? '<button class="btn btn-danger btn-sm ms-1" data-action="delete" data-user-id="' + u.id + '" data-username="' + escapeHtml(u.username) + '">刪除</button>'
                : '';

            actionBtns = toggleBtn + deleteBtn;
        }

        return '<tr>' +
            '<td>' + u.id + '</td>' +
            '<td>' + escapeHtml(u.username) + '</td>' +
            '<td>' + escapeHtml(u.email) + '</td>' +
            '<td class="text-center">' + emailBadge + '</td>' +
            '<td class="text-center">' + statusBadge + '</td>' +
            '<td class="text-center">' + adminBadge + '</td>' +
            '<td>' + lastLogin + '</td>' +
            '<td>' + createdAt + '</td>' +
            '<td class="text-center">' + actionBtns + '</td>' +
            '</tr>';
    }).join('');
}

function renderPagination(totalPages, currentPageNum) {
    const ul = document.getElementById('pagination');
    if (totalPages <= 1) { ul.innerHTML = ''; return; }

    let html = '';
    html += '<li class="page-item' + (currentPageNum === 0 ? ' disabled' : '') + '">' +
        '<a class="page-link" href="#" data-page="' + (currentPageNum - 1) + '">«</a></li>';

    for (let i = 0; i < totalPages; i++) {
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
        if (p >= 0 && p < totalPages) loadUsers(p, currentKeyword);
    });
}

async function disableUser(userId) {
    const confirmed = await Swal.fire({
        title: '確認停用？',
        text: '停用後該使用者將無法登入',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: '確認停用',
        cancelButtonText: '取消',
        confirmButtonColor: '#dc3545'
    });
    if (!confirmed.isConfirmed) return;

    const res = await apiFetch('/api/admin/users/' + userId + '/disable', { method: 'PATCH' });
    if (!res) return;

    const body = await res.json();
    if (body.success) {
        Swal.fire({ icon: 'success', title: '已停用', timer: 1200, showConfirmButton: false });
        loadUsers(currentPage, currentKeyword);
    } else {
        Swal.fire({ icon: 'error', title: '操作失敗', text: body.message });
    }
}

async function enableUser(userId) {
    const confirmed = await Swal.fire({
        title: '確認啟用？',
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: '確認啟用',
        cancelButtonText: '取消'
    });
    if (!confirmed.isConfirmed) return;

    const res = await apiFetch('/api/admin/users/' + userId + '/enable', { method: 'PATCH' });
    if (!res) return;

    const body = await res.json();
    if (body.success) {
        Swal.fire({ icon: 'success', title: '已啟用', timer: 1200, showConfirmButton: false });
        loadUsers(currentPage, currentKeyword);
    } else {
        Swal.fire({ icon: 'error', title: '操作失敗', text: body.message });
    }
}

async function deleteUser(userId, username) {
    const confirmed = await Swal.fire({
        title: '[DEV] 確認硬刪除？',
        html: '將永久刪除 <strong>' + escapeHtml(username) + '</strong> 及其所有關聯資料，此操作無法復原。',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: '確認刪除',
        cancelButtonText: '取消',
        confirmButtonColor: '#dc3545'
    });
    if (!confirmed.isConfirmed) return;

    const res = await apiFetch('/api/dev/admin/users/' + userId, { method: 'DELETE' });
    if (!res) return;

    const body = await res.json();
    if (body.success) {
        Swal.fire({ icon: 'success', title: '已刪除', timer: 1200, showConfirmButton: false });
        loadUsers(currentPage, currentKeyword);
    } else {
        Swal.fire({ icon: 'error', title: '刪除失敗', text: body.message });
    }
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
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
