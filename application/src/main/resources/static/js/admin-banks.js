let createModal, editModal;

document.addEventListener('DOMContentLoaded', () => {
    createModal = new bootstrap.Modal(document.getElementById('createModal'));
    editModal   = new bootstrap.Modal(document.getElementById('editModal'));
    loadBanks();
    document.getElementById('openCreateModalBtn').addEventListener('click', openCreateModal);
    document.getElementById('submitCreateBtn').addEventListener('click', submitCreate);
    document.getElementById('submitEditBtn').addEventListener('click', submitEdit);
});

function loadBanks() {
    fetch('/api/admin/banks')
        .then(res => res.json())
        .then(data => {
            if (!data.success) {
                document.getElementById('bankTableBody').innerHTML =
                    '<tr><td colspan="6" class="text-center text-danger">載入失敗</td></tr>';
                return;
            }
            const banks = data.data || [];
            if (banks.length === 0) {
                document.getElementById('bankTableBody').innerHTML =
                    '<tr><td colspan="6" class="text-center text-muted py-4">尚無銀行資料</td></tr>';
                return;
            }
            const tbody = document.getElementById('bankTableBody');
            tbody.innerHTML = '';
            banks.forEach(b => {
                const tr = document.createElement('tr');
                const editBtn = document.createElement('button');
                editBtn.className = 'btn btn-outline-primary btn-sm';
                editBtn.innerHTML = '<i class="fas fa-edit"></i>';
                editBtn.addEventListener('click', () => openEditModal(b.bankCode, b.name, b.shortName || '', b.country, b.isActive));

                tr.innerHTML = `
                    <td><code>${b.bankCode}</code></td>
                    <td>${b.name}</td>
                    <td>${b.shortName || '-'}</td>
                    <td>${b.country}</td>
                    <td class="text-center">
                        <span class="badge ${b.isActive ? 'bg-success' : 'bg-secondary'}">
                            ${b.isActive ? '啟用' : '停用'}
                        </span>
                    </td>
                    <td class="text-center"></td>
                `;
                tr.querySelector('td:last-child').appendChild(editBtn);
                tbody.appendChild(tr);
            });
        })
        .catch(() => {
            document.getElementById('bankTableBody').innerHTML =
                '<tr><td colspan="6" class="text-center text-danger">網路錯誤</td></tr>';
        });
}

function openCreateModal() {
    document.getElementById('createBankCode').value = '';
    document.getElementById('createName').value = '';
    document.getElementById('createShortName').value = '';
    document.getElementById('createCountry').value = 'TW';
    document.getElementById('createIsActive').checked = true;
    createModal.show();
}

function submitCreate() {
    const bankCode  = document.getElementById('createBankCode').value.trim();
    const name      = document.getElementById('createName').value.trim();
    const shortName = document.getElementById('createShortName').value.trim();
    const country   = document.getElementById('createCountry').value.trim();
    const isActive  = document.getElementById('createIsActive').checked;

    if (!bankCode) { Swal.fire({ icon: 'warning', title: '請填寫銀行代碼' }); return; }
    if (!name)     { Swal.fire({ icon: 'warning', title: '請填寫銀行名稱' }); return; }
    if (!country)  { Swal.fire({ icon: 'warning', title: '請填寫國家' }); return; }

    fetch('/api/admin/banks', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ bankCode, name, shortName: shortName || null, country, isActive })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            createModal.hide();
            Swal.fire({ icon: 'success', title: '新增成功', timer: 1200, showConfirmButton: false })
                .then(() => loadBanks());
        } else {
            Swal.fire({ icon: 'error', title: '新增失敗', text: data.error?.message || '請稍後再試' });
        }
    })
    .catch(() => Swal.fire({ icon: 'error', title: '網路錯誤' }));
}

function openEditModal(bankCode, name, shortName, country, isActive) {
    document.getElementById('editBankCode').value        = bankCode;
    document.getElementById('editBankCodeDisplay').value = bankCode;
    document.getElementById('editName').value            = name;
    document.getElementById('editShortName').value       = shortName;
    document.getElementById('editCountry').value         = country;
    document.getElementById('editIsActive').checked      = isActive;
    editModal.show();
}

function submitEdit() {
    const bankCode  = document.getElementById('editBankCode').value;
    const name      = document.getElementById('editName').value.trim();
    const shortName = document.getElementById('editShortName').value.trim();
    const country   = document.getElementById('editCountry').value.trim();
    const isActive  = document.getElementById('editIsActive').checked;

    if (!name)    { Swal.fire({ icon: 'warning', title: '請填寫銀行名稱' }); return; }
    if (!country) { Swal.fire({ icon: 'warning', title: '請填寫國家' }); return; }

    fetch(`/api/admin/banks/${bankCode}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, shortName: shortName || null, country, isActive })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            editModal.hide();
            Swal.fire({ icon: 'success', title: '儲存成功', timer: 1200, showConfirmButton: false })
                .then(() => loadBanks());
        } else {
            Swal.fire({ icon: 'error', title: '儲存失敗', text: data.error?.message || '請稍後再試' });
        }
    })
    .catch(() => Swal.fire({ icon: 'error', title: '網路錯誤' }));
}
