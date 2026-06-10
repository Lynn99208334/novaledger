document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-action="edit-card"]').forEach(btn => {
        btn.addEventListener('click', () => openEditModal(btn));
    });
    document.querySelectorAll('[data-action="delete-card"]').forEach(btn => {
        btn.addEventListener('click', () => confirmDelete(btn));
    });
    document.getElementById('submitEditBtn').addEventListener('click', submitEdit);
});

function openEditModal(btn) {
    document.getElementById('editCardId').value          = btn.dataset.cardId;
    document.getElementById('editName').value            = btn.dataset.cardName;
    document.getElementById('editBankCode').value        = btn.dataset.bankCode || '';
    document.getElementById('editCardType').value        = btn.dataset.cardType || '';
    document.getElementById('editCardNumberLast4').value = btn.dataset.cardNumberLast4 || '';
    document.getElementById('editCurrencyCode').value    = btn.dataset.currencyCode;
    document.getElementById('editBillingDate').value     = btn.dataset.billingDate || '';
    document.getElementById('editPaymentDate').value     = btn.dataset.paymentDate || '';
    document.getElementById('editCreditLimit').value     = btn.dataset.creditLimit || '';
    document.getElementById('editNotes').value           = btn.dataset.notes || '';
    new bootstrap.Modal(document.getElementById('editCardModal')).show();
}

function submitEdit() {
    const cardId          = document.getElementById('editCardId').value;
    const name            = document.getElementById('editName').value.trim();
    const bankCode        = document.getElementById('editBankCode').value.trim();
    const cardType        = document.getElementById('editCardType').value.trim();
    const cardNumberLast4 = document.getElementById('editCardNumberLast4').value.trim();
    const currencyCode    = document.getElementById('editCurrencyCode').value;
    const billingDate     = document.getElementById('editBillingDate').value;
    const paymentDate     = document.getElementById('editPaymentDate').value;
    const creditLimit     = document.getElementById('editCreditLimit').value;
    const notes           = document.getElementById('editNotes').value.trim();

    if (!name) { Swal.fire({ icon: 'warning', title: '請填寫卡片名稱' }); return; }

    fetch(`/api/cards/${cardId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            name, currencyCode,
            bankCode: bankCode || null,
            cardType: cardType || null,
            cardNumberLast4: cardNumberLast4 || null,
            billingDate: billingDate ? parseInt(billingDate) : null,
            paymentDate: paymentDate ? parseInt(paymentDate) : null,
            creditLimit: creditLimit ? parseFloat(creditLimit) : null,
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
    const cardId   = btn.dataset.cardId;
    const cardName = btn.dataset.cardName;

    Swal.fire({
        title: '確定要刪除？',
        html: `信用卡「<strong>${cardName}</strong>」刪除後無法復原。`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '確定刪除',
        cancelButtonText: '取消'
    }).then(result => {
        if (result.isConfirmed) {
            fetch(`/api/cards/${cardId}`, { method: 'DELETE' })
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
