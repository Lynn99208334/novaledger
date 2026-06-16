let editingKey = null;
let editingType = null;
let modalInstance = null;

document.addEventListener('DOMContentLoaded', function () {
    loadConfigs();

    // Event delegation — 開啟編輯 Modal
    document.getElementById('configTableBody').addEventListener('click', function (e) {
        const btn = e.target.closest('button[data-action="edit"]');
        if (!btn) return;
        editingKey = btn.getAttribute('data-key');
        editingType = btn.getAttribute('data-type');
        const currentValue = btn.getAttribute('data-value');
        const desc = btn.getAttribute('data-desc');
        openEditModal(editingKey, editingType, currentValue, desc);
    });

    document.getElementById('confirmEditBtn').addEventListener('click', function () {
        submitUpdate();
    });
});

async function loadConfigs() {
    const res = await apiFetch('/api/admin/system-configs');
    if (!res) return;

    const body = await res.json();
    if (!body.success) return;

    renderTable(body.data);
}

function renderTable(configs) {
    const tbody = document.getElementById('configTableBody');
    if (!configs || configs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">無資料</td></tr>';
        return;
    }

    tbody.innerHTML = configs.map(function (c) {
        const typeBadge = c.valueType === 'BOOLEAN'
            ? '<span class="badge bg-secondary">BOOLEAN</span>'
            : c.valueType === 'INTEGER'
                ? '<span class="badge bg-info text-dark">INTEGER</span>'
                : '<span class="badge bg-light text-dark border">STRING</span>';

        const valueBadge = c.valueType === 'BOOLEAN'
            ? (c.configValue === 'true'
                ? '<span class="badge bg-success">true</span>'
                : '<span class="badge bg-danger">false</span>')
            : '<code>' + escapeHtml(c.configValue) + '</code>';

        const updatedAt = c.updatedAt ? formatDatetime(c.updatedAt) : '－';

        return '<tr>' +
            '<td><code>' + escapeHtml(c.configKey) + '</code></td>' +
            '<td class="text-muted small">' + escapeHtml(c.description || '') + '</td>' +
            '<td>' + typeBadge + '</td>' +
            '<td>' + valueBadge + '</td>' +
            '<td class="small">' + updatedAt + '</td>' +
            '<td class="text-center">' +
            '<button class="btn btn-outline-primary btn-sm" data-action="edit" ' +
            'data-key="' + escapeHtml(c.configKey) + '" ' +
            'data-type="' + escapeHtml(c.valueType) + '" ' +
            'data-value="' + escapeHtml(c.configValue) + '" ' +
            'data-desc="' + escapeHtml(c.description || '') + '">編輯</button>' +
            '</td>' +
            '</tr>';
    }).join('');
}

function openEditModal(key, type, currentValue, desc) {
    document.getElementById('editModalKey').textContent = 'Key：' + key;
    document.getElementById('editModalDesc').textContent = desc || '';
    document.getElementById('editValueInput').value = currentValue;

    const hint = document.getElementById('editValueHint');
    if (type === 'BOOLEAN') {
        hint.textContent = '請輸入 true 或 false';
    } else if (type === 'INTEGER') {
        hint.textContent = '請輸入整數';
    } else {
        hint.textContent = '';
    }

    if (!modalInstance) {
        modalInstance = new bootstrap.Modal(document.getElementById('editModal'));
    }
    modalInstance.show();
}

async function submitUpdate() {
    const newValue = document.getElementById('editValueInput').value.trim();

    // 基本格式驗證
    if (editingType === 'BOOLEAN' && newValue !== 'true' && newValue !== 'false') {
        alert('BOOLEAN 類型只能輸入 true 或 false');
        return;
    }
    if (editingType === 'INTEGER' && isNaN(parseInt(newValue))) {
        alert('INTEGER 類型請輸入整數');
        return;
    }

    const res = await apiFetch('/api/admin/system-configs/' + encodeURIComponent(editingKey), {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ value: newValue })
    });
    if (!res) return;

    const body = await res.json();
    if (body.success) {
        modalInstance.hide();
        loadConfigs();
    } else {
        alert('更新失敗：' + body.message);
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
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
