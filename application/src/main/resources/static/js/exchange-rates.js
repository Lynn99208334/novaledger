document.addEventListener('DOMContentLoaded', loadRates);

function loadRates() {
    fetch('/api/exchange-rates')
        .then(res => res.json())
        .then(data => {
            if (!data.success) {
                document.getElementById('ratesBody').innerHTML =
                    '<tr><td colspan="5" class="text-center text-danger">載入失敗</td></tr>';
                return;
            }
            const rates = data.data;
            if (!rates || rates.length === 0) {
                document.getElementById('ratesBody').innerHTML =
                    '<tr><td colspan="5" class="text-center text-muted">尚無匯率資料</td></tr>';
                return;
            }
            const tbody = document.getElementById('ratesBody');
            tbody.innerHTML = '';
            rates.forEach(r => {
                const tr = document.createElement('tr');
                const editBtn = document.createElement('button');
                editBtn.className = 'btn btn-outline-primary btn-sm';
                editBtn.innerHTML = '<i class="fas fa-edit"></i>';
                editBtn.addEventListener('click', () => prefillForm(r.baseCurrency, r.rate));

                tr.innerHTML = `
                    <td><strong>${r.baseCurrency}</strong></td>
                    <td class="text-end">${r.rate}</td>
                    <td>${r.rateDate}</td>
                    <td>${r.source || '-'}</td>
                    <td class="text-center"></td>
                `;
                tr.querySelector('td:last-child').appendChild(editBtn);
                tbody.appendChild(tr);
            });
        })
        .catch(() => {
            document.getElementById('ratesBody').innerHTML =
                '<tr><td colspan="5" class="text-center text-danger">網路錯誤</td></tr>';
        });
}

function prefillForm(baseCurrency, rate) {
    document.getElementById('baseCurrency').value = baseCurrency;
    document.getElementById('rateValue').value = rate;
    document.getElementById('rateValue').focus();
}

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('submitRateBtn').addEventListener('click', submitUpdateRate);
});

function submitUpdateRate() {
    const baseCurrency = document.getElementById('baseCurrency').value;
    const rate = parseFloat(document.getElementById('rateValue').value);
    if (!rate || rate <= 0) { Swal.fire({ icon: 'warning', title: '請輸入有效匯率' }); return; }
    fetch('/api/exchange-rates', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ baseCurrency, quoteCurrency: 'TWD', rate })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            Swal.fire({ icon: 'success', title: '更新成功', timer: 1200, showConfirmButton: false })
                .then(() => loadRates());
        } else {
            Swal.fire({ icon: 'error', title: '更新失敗', text: data.error || '請稍後再試' });
        }
    })
    .catch(() => Swal.fire({ icon: 'error', title: '網路錯誤', text: '請稍後再試' }));
}
