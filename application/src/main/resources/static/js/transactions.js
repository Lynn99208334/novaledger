let currentPage = 0;
const pageSize = 20;
let editModal = null;

document.addEventListener('DOMContentLoaded', () => {
    editModal = new bootstrap.Modal(document.getElementById('editModal'));
    loadAccounts();
    loadTransactions(0);
    document.getElementById('filterBtn').addEventListener('click', applyFilter);
    document.getElementById('submitEditBtn').addEventListener('click', submitEdit);
});

function loadAccounts() {
    fetch('/api/accounts')
        .then(res => res.json())
        .then(data => {
            if (!data.success) return;
            const select = document.getElementById('filterAccount');
            (data.data || []).forEach(acc => {
                const opt = document.createElement('option');
                opt.value = acc.id;
                opt.textContent = `${acc.name}（${acc.currencyCode}）`;
                select.appendChild(opt);
            });
        });
}

function applyFilter() { loadTransactions(0); }

function buildUrl(page) {
    const accountId = document.getElementById('filterAccount').value;
    const from = document.getElementById('filterFrom').value;
    const to = document.getElementById('filterTo').value;
    const params = new URLSearchParams({ page, size: pageSize });
    if (accountId) params.append('accountId', accountId);
    if (from) params.append('from', from);
    if (to) params.append('to', to);
    return `/api/transactions?${params.toString()}`;
}

function loadTransactions(page) {
    currentPage = page;
    fetch(buildUrl(page))
        .then(res => res.json())
        .then(data => {
            if (!data.success) return;
            const pageData = data.data;
            renderTable(pageData.content || []);
            renderPagination(pageData.number, pageData.totalPages, pageData.totalElements);
        })
        .catch(() => Swal.fire({ icon: 'error', title: '載入失敗', text: '請稍後再試' }));
}

function renderTable(transactions) {
    const tbody = document.getElementById('txTableBody');
    tbody.innerHTML = '';
    if (transactions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">暫無資料</td></tr>';
        return;
    }
    transactions.forEach(tx => {
        const tr = document.createElement('tr');
        const editBtn = document.createElement('button');
        editBtn.className = 'btn btn-outline-primary btn-sm me-1';
        editBtn.innerHTML = '<i class="fas fa-edit"></i>';
        editBtn.addEventListener('click', () => openEdit(tx));

        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-outline-danger btn-sm';
        deleteBtn.innerHTML = '<i class="fas fa-trash"></i>';
        deleteBtn.addEventListener('click', () => confirmDelete(tx.id));

        tr.innerHTML = `
            <td>${tx.transactionDate ?? ''}</td>
            <td>${formatTxType(tx.txTypeCode)}</td>
            <td class="text-end ${amountClass(tx.txTypeCode)}">${formatAmount(tx.totalAmount)}</td>
            <td>${tx.currencyCode ?? ''}</td>
            <td class="text-muted small">${tx.memo ?? ''}</td>
            <td class="text-center"></td>
        `;
        const actionCell = tr.querySelector('td:last-child');
        actionCell.appendChild(editBtn);
        actionCell.appendChild(deleteBtn);
        tbody.appendChild(tr);
    });
}

function renderPagination(currentPageNum, totalPages, totalElements) {
    document.getElementById('totalLabel').textContent = `共 ${totalElements} 筆`;
    const ul = document.getElementById('pagination');
    ul.innerHTML = '';
    if (totalPages <= 1) return;

    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${currentPageNum === 0 ? 'disabled' : ''}`;
    const prevLink = document.createElement('a');
    prevLink.className = 'page-link';
    prevLink.href = '#';
    prevLink.textContent = '«';
    prevLink.addEventListener('click', e => { e.preventDefault(); if (currentPageNum > 0) loadTransactions(currentPageNum - 1); });
    prevLi.appendChild(prevLink);
    ul.appendChild(prevLi);

    const start = Math.max(0, currentPageNum - 2);
    const end = Math.min(totalPages - 1, currentPageNum + 2);
    for (let i = start; i <= end; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === currentPageNum ? 'active' : ''}`;
        const link = document.createElement('a');
        link.className = 'page-link';
        link.href = '#';
        link.textContent = i + 1;
        const pageNum = i;
        link.addEventListener('click', e => { e.preventDefault(); loadTransactions(pageNum); });
        li.appendChild(link);
        ul.appendChild(li);
    }

    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${currentPageNum >= totalPages - 1 ? 'disabled' : ''}`;
    const nextLink = document.createElement('a');
    nextLink.className = 'page-link';
    nextLink.href = '#';
    nextLink.textContent = '»';
    nextLink.addEventListener('click', e => { e.preventDefault(); if (currentPageNum < totalPages - 1) loadTransactions(currentPageNum + 1); });
    nextLi.appendChild(nextLink);
    ul.appendChild(nextLi);
}

function openEdit(tx) {
    document.getElementById('editTxId').value = tx.id;
    document.getElementById('editTxType').value = tx.txTypeCode;
    document.getElementById('editDate').value = tx.transactionDate;
    document.getElementById('editAmount').value = tx.totalAmount;
    document.getElementById('editCurrency').value = tx.currencyCode;
    document.getElementById('editMemo').value = tx.memo || '';
    editModal.show();
}

function submitEdit() {
    const id = document.getElementById('editTxId').value;
    fetch(`/api/transactions/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            txTypeCode: document.getElementById('editTxType').value,
            transactionDate: document.getElementById('editDate').value,
            totalAmount: parseFloat(document.getElementById('editAmount').value),
            currencyCode: document.getElementById('editCurrency').value,
            memo: document.getElementById('editMemo').value,
            items: []
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) { editModal.hide(); loadTransactions(currentPage); }
        else { Swal.fire({ icon: 'error', title: '更新失敗', text: data.error?.message || '請稍後再試' }); }
    })
    .catch(() => Swal.fire({ icon: 'error', title: '網路錯誤' }));
}

function confirmDelete(txId) {
    Swal.fire({
        icon: 'warning', title: '確認刪除？', text: '此操作無法復原。',
        showCancelButton: true, confirmButtonText: '刪除', cancelButtonText: '取消', confirmButtonColor: '#d33'
    }).then(result => {
        if (!result.isConfirmed) return;
        fetch(`/api/transactions/${txId}`, { method: 'DELETE' })
            .then(res => res.json())
            .then(data => {
                if (data.success) { loadTransactions(currentPage); }
                else { Swal.fire({ icon: 'error', title: '刪除失敗', text: data.error?.message || '請稍後再試' }); }
            })
            .catch(() => Swal.fire({ icon: 'error', title: '網路錯誤' }));
    });
}

function formatAmount(val) {
    if (val == null) return '';
    return Number(val).toLocaleString('zh-TW', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

function formatTxType(code) {
    const map = { INCOME: '收入', EXPENSE: '支出', TRANSFER_IN: '轉入', TRANSFER_OUT: '轉出', ADJUSTMENT: '調整' };
    return map[code] || code;
}

function amountClass(code) {
    if (code === 'INCOME' || code === 'TRANSFER_IN') return 'text-success';
    if (code === 'EXPENSE' || code === 'TRANSFER_OUT') return 'text-danger';
    return '';
}
