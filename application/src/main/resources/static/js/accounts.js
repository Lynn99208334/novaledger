document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-action="edit-account"]').forEach(btn => {
        btn.addEventListener('click', () => openEditModal(btn));
    });
    document.querySelectorAll('[data-action="delete-account"]').forEach(btn => {
        btn.addEventListener('click', () => confirmDelete(btn));
    });
    document.getElementById('submitEditBtn').addEventListener('click', submitEdit);
});

function openEditModal(btn) {
    document.getElementById('editAccountId').value     = btn.dataset.accountId;
    document.getElementById('editAccountType').value   = btn.dataset.accountType;
    document.getElementById('editName').value          = btn.dataset.accountName;
    document.getElementById('editAlias').value         = btn.dataset.accountAlias || '';
    document.getElementById('editCurrencyCode').value  = btn.dataset.currencyCode;
    document.getElementById('editBankCode').value      = btn.dataset.bankCode || '';
    document.getElementById('editAccountNumber').value = btn.dataset.accountNumber || '';
    document.getElementById('editNotes').value         = btn.dataset.notes || '';
    new bootstrap.Modal(document.getElementById('editAccountModal')).show();
}

function submitEdit() {
    const accountId     = document.getElementById('editAccountId').value;
    const accountType   = document.getElementById('editAccountType').value;
    const name          = document.getElementById('editName').value.trim();
    const alias         = document.getElementById('editAlias').value.trim();
    const currencyCode  = document.getElementById('editCurrencyCode').value;
    const bankCode      = document.getElementById('editBankCode').value.trim();
    const accountNumber = document.getElementById('editAccountNumber').value.trim();
    const notes         = document.getElementById('editNotes').value.trim();

    if (!name) {
        Swal.fire({ icon: 'warning', title: '請填寫帳戶名稱' });
        return;
    }

    fetch(`/api/accounts/${accountId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            accountType,
            name,
            alias: alias || null,
            currencyCode,
            initialBalance: 0,
            bankCode: bankCode || null,
            accountNumber: accountNumber || null,
            notes: notes || null
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            Swal.fire({ icon: 'success', title: '儲存成功', timer: 1200, showConfirmButton: false })
                .then(() => location.reload());
        } else {
            Swal.fire({ icon: 'error', title: '儲存失敗', text: data.message || '請稍後再試' });
        }
    })
    .catch(() => Swal.fire({ icon: 'error', title: '網路錯誤', text: '請稍後再試' }));
}

function confirmDelete(btn) {
    const accountId   = btn.dataset.accountId;
    const accountName = btn.dataset.accountName;

    Swal.fire({
        title: '確定要刪除？',
        html: `帳戶「<strong>${accountName}</strong>」刪除後無法復原。`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '確定刪除',
        cancelButtonText: '取消'
    }).then(result => {
        if (result.isConfirmed) {
            fetch(`/api/accounts/${accountId}`, { method: 'DELETE' })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        Swal.fire({ icon: 'success', title: '已刪除', timer: 1200, showConfirmButton: false })
                            .then(() => location.reload());
                    } else {
                        Swal.fire({ icon: 'error', title: '刪除失敗', text: data.message || '請稍後再試' });
                    }
                })
                .catch(() => Swal.fire({ icon: 'error', title: '網路錯誤', text: '請稍後再試' }));
        }
    });
}
