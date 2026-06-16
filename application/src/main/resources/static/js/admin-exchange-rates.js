let editingBase = null;
let editingQuote = null;
let modalInstance = null;

document.addEventListener('DOMContentLoaded', function () {
    loadRates();

    // Event delegation — 開啟編輯 Modal
    document.getElementById('rateTableBody').addEventListener('click', function (e) {
        const btn = e.target.closest('button[data-action="edit"]');
        if (!btn) return;
        editingBase = btn.getAttribute('data-base');
        editingQuote = btn.getAttribute('data-quote');
        const currentRate = btn.getAttribute('data-rate');
        openEditModal(editingBase, editingQuote, currentRate);
    });

    document.getElementById('confirmEditBtn').addEventListener('click', function () {
        submitUpdate();
    });
});

async function loadRates() {
    const res = await apiFetch('/api/exchange-rates');
    if (!res) return;

    const body = await res.json();
    if (!body.success) return;

    renderTable(body.data);
}

function renderTable(rates) {
    const tbody = document.getElementById('rateTableBody');
    if (!rates || rates.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">無資料</td></tr>';
        return;
    }

    tbody.innerHTML = rates.map(function (r) {
        const sourceBadge = r.source === 'MANUAL'
            ? '<span class="badge bg-warning text-dark">手動</span>'
            : '<span class="badge bg-info text-dark">' + escapeHtml(r.source) + '</span>';

        return '<tr>' +
            '<td><strong>' + escapeHtml(r.baseCurrency) + '</strong></td>' +
            '<td>' + escapeHtml(r.quoteCurrency) + '</td>' +
            '<td>' + r.rate + '</td>' +
            '<td>' + escapeHtml(r.rateDate || '') + '</td>' +
            '<td>' + sourceBadge + '</td>' +
            '<td class="text-center">' +
            '<button class="btn btn-outline-primary btn-sm" data-action="edit" ' +
            'data-base="' + escapeHtml(r.baseCurrency) + '" ' +
            'data-quote="' + escapeHtml(r.quoteCurrency) + '" ' +
            'data-rate="' + r.rate + '">編輯</button>' +
            '</td>' +
            '</tr>';
    }).join('');
}

function openEditModal(base, quote, currentRate) {
    document.getElementById('editModalSubtitle').textContent =
        base + ' / ' + quote + '　目前匯率：' + currentRate;
    document.getElementById('editRateInput').value = currentRate;

    if (!modalInstance) {
        modalInstance = new bootstrap.Modal(document.getElementById('editModal'));
    }
    modalInstance.show();
}

async function submitUpdate() {
    const rateVal = document.getElementById('editRateInput').value.trim();
    if (!rateVal || isNaN(Number(rateVal)) || Number(rateVal) <= 0) {
        alert('請輸入有效的匯率（大於 0 的數字）');
        return;
    }

    const res = await apiFetch('/api/exchange-rates', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            baseCurrency: editingBase,
            quoteCurrency: editingQuote,
            rate: Number(rateVal)
        })
    });
    if (!res) return;

    const body = await res.json();
    if (body.success) {
        modalInstance.hide();
        loadRates();
    } else {
        alert('更新失敗：' + body.message);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}
