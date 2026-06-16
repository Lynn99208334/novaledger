document.addEventListener('DOMContentLoaded', function () {
    loadParsers();
});

async function loadParsers() {
    const res = await apiFetch('/api/admin/parsers');
    if (!res) return;

    const body = await res.json();
    if (!body.success) return;

    const parsers = body.data;
    renderTable(parsers);

    document.getElementById('totalCountLabel').textContent = '共 ' + parsers.length + ' 個 parser';
}

function renderTable(parsers) {
    const tbody = document.getElementById('parserTableBody');
    if (!parsers || parsers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">無資料</td></tr>';
        return;
    }

    tbody.innerHTML = parsers.map(function (p) {
        const autoDetectBadge = p.supportsAutoDetect
            ? '<span class="badge bg-success">支援</span>'
            : '<span class="badge bg-secondary">不支援</span>';

        const accountBadge = p.supportsAccountExtraction
            ? '<span class="badge bg-success">支援</span>'
            : '<span class="badge bg-secondary">不支援</span>';

        const fileTypeBadge = p.fileType === 'CSV'
            ? '<span class="badge bg-info text-dark">CSV</span>'
            : '<span class="badge bg-warning text-dark">XLSX</span>';

        return '<tr>' +
            '<td>' + escapeHtml(p.bankCode) + '</td>' +
            '<td>' + escapeHtml(p.bankName) + '</td>' +
            '<td>' + fileTypeBadge + '</td>' +
            '<td><code>' + escapeHtml(p.parserKey) + '</code></td>' +
            '<td class="text-center">' + autoDetectBadge + '</td>' +
            '<td class="text-center">' + accountBadge + '</td>' +
            '</tr>';
    }).join('');
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}
