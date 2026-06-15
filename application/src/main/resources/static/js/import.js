let currentJobId = null;
let pollTimer = null;

const SUPPORTED_BANKS = [
    { bankCode: '822', name: '中國信託銀行', hint: '支援 CSV 格式' },
    { bankCode: '808', name: '玉山銀行',     hint: '支援 CSV 格式（開發中）' },
    { bankCode: '807', name: '永豐銀行',     hint: '支援 CSV 格式（開發中）' },
    { bankCode: '806', name: '元大銀行',     hint: '支援 CSV 格式（開發中）' }
];

document.addEventListener('DOMContentLoaded', () => {
    const select = document.getElementById('bankCodeSelect');
    SUPPORTED_BANKS.forEach(bank => {
        const opt = document.createElement('option');
        opt.value = bank.bankCode;
        opt.textContent = `${bank.name}（${bank.bankCode}）`;
        opt.dataset.hint = bank.hint;
        select.appendChild(opt);
    });
    select.addEventListener('change', () => {
        const selected = SUPPORTED_BANKS.find(b => b.bankCode === select.value);
        document.getElementById('bankHint').textContent = selected ? selected.hint : '';
    });

    fetch('/api/accounts')
        .then(res => res.json())
        .then(data => {
            if (!data.success) return;
            const accountSelect = document.getElementById('accountSelect');
            (data.data || []).forEach(account => {
                const opt = document.createElement('option');
                opt.value = account.id;
                opt.textContent = `${account.name}（${account.currencyCode}）`;
                accountSelect.appendChild(opt);
            });
        });

    document.getElementById('uploadBtn').addEventListener('click', startUpload);
    document.getElementById('confirmImportBtn').addEventListener('click', confirmImport);
    document.getElementById('resetBtn').addEventListener('click', resetPage);
});

function startUpload() {
    const fileInput = document.getElementById('fileInput');
    const bankCode  = document.getElementById('bankCodeSelect').value;
    const accountId = document.getElementById('accountSelect').value;
    const jobType   = document.querySelector('input[name="jobType"]:checked').value;

    if (!bankCode)  { Swal.fire({ icon: 'warning', title: '請選擇銀行' }); return; }
    if (!accountId) { Swal.fire({ icon: 'warning', title: '請選擇帳戶' }); return; }
    if (!fileInput.files || fileInput.files.length === 0) { Swal.fire({ icon: 'warning', title: '請先選擇檔案' }); return; }

    const file = fileInput.files[0];
    if (file.size > 10 * 1024 * 1024) { Swal.fire({ icon: 'warning', title: '檔案過大', text: '上限 10MB' }); return; }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('jobType', jobType);
    formData.append('bankCode', bankCode);
    formData.append('accountId', accountId);

    document.getElementById('uploadBtn').disabled = true;
    document.getElementById('progressCard').classList.remove('d-none');

    fetch('/api/import/upload', { method: 'POST', body: formData })
        .then(res => res.json())
        .then(data => {
            if (data.success && data.data && data.data.jobId) {
                currentJobId = data.data.jobId;
                pollTimer = setInterval(() => pollStatus(currentJobId), 1500);
            } else {
                document.getElementById('uploadBtn').disabled = false;
                document.getElementById('progressCard').classList.add('d-none');
                Swal.fire({ icon: 'error', title: '上傳失敗', text: data.message || '請稍後再試' });
            }
        })
        .catch(() => {
            document.getElementById('uploadBtn').disabled = false;
            document.getElementById('progressCard').classList.add('d-none');
            Swal.fire({ icon: 'error', title: '網路錯誤', text: '請稍後再試' });
        });
}

function pollStatus(jobId) {
    fetch(`/api/import/jobs/${jobId}/status`, { headers: { 'X-Tenant-Id': '1' } })
        .then(res => res.json())
        .then(data => {
            if (!data.success) return;
            const job = data.data;
            const total = job.totalCount || 0;
            const done  = (job.successCount || 0) + (job.failCount || 0);
            const pct   = total > 0 ? Math.round((done / total) * 100) : 0;
            const bar   = document.getElementById('progressBar');
            bar.style.width = pct + '%';
            bar.textContent = pct + '%';
            document.getElementById('progressStatus').textContent = job.status;
            document.getElementById('progressDetail').textContent = `已處理 ${done} / ${total} 筆`;

            if (job.status === 'COMPLETED' || job.status === 'FAILED') {
                clearInterval(pollTimer);
                if (job.status === 'COMPLETED') {
                    Promise.all([
                        fetch(`/api/import/jobs/${jobId}/preview`, { headers: { 'X-Tenant-Id': '1' } }).then(r => r.json()),
                        fetch(`/api/import/jobs/${jobId}/errors`,  { headers: { 'X-Tenant-Id': '1' } }).then(r => r.json())
                    ]).then(([previewRes, errorRes]) => {
                        document.getElementById('progressCard').classList.add('d-none');
                        showResults(job, previewRes.data || [], errorRes.data || []);
                    });
                } else {
                    Swal.fire({ icon: 'error', title: '解析失敗', text: '請確認檔案格式是否正確。' });
                }
            }
        })
        .catch(() => clearInterval(pollTimer));
}

function showResults(job, previews, errors) {
    document.getElementById('statTotal').textContent   = job.totalCount   || 0;
    document.getElementById('statSuccess').textContent = job.successCount || 0;
    document.getElementById('statFail').textContent    = job.failCount    || 0;

    document.getElementById('previewTableBody').innerHTML = previews.map(row => {
        const amount = row.amount ?? 0;
        const expense = amount < 0 ? formatAmount(Math.abs(amount)) : '';
        const income  = amount >= 0 ? formatAmount(amount) : '';
        const isDuplicate = row.importStatus === 'DUPLICATE';
        const rowClass = isDuplicate ? 'class="table-warning"' : '';
        const note = isDuplicate ? '<span class="badge bg-warning text-dark">重複</span>' : '';
        return `
        <tr ${rowClass}>
            <td>${row.rowNumber ?? ''}</td>
            <td>${row.transactionDate ?? ''}</td>
            <td>${row.description ?? ''}</td>
            <td class="text-end text-danger">${expense}</td>
            <td class="text-end text-success">${income}</td>
            <td class="text-end">${row.balance != null ? formatAmount(row.balance) : ''}</td>
            <td>${row.currencyCode ?? ''}</td>
            <td>${note}</td>
        </tr>
        `;
    }).join('');
    document.getElementById('previewCount').textContent = previews.length + ' 筆';

    if (errors.length > 0) {
        document.getElementById('errorTableBody').innerHTML = errors.map(row => `
            <tr>
                <td>${row.rowNumber ?? ''}</td>
                <td class="text-muted small">${row.rawData ?? ''}</td>
                <td class="text-danger">${row.errorMessage ?? ''}</td>
            </tr>
        `).join('');
        document.getElementById('errorCount').textContent = errors.length + ' 筆';
        document.getElementById('errorCard').classList.remove('d-none');
    }

    if ((job.successCount || 0) > 0) document.getElementById('confirmImportBtn').disabled = false;
    document.getElementById('resultSection').classList.remove('d-none');
}

function confirmImport() {
    if (!currentJobId) return;
    Swal.fire({
        icon: 'question', title: '確認匯入？',
        text: '將把解析成功的資料寫入交易記錄，此操作無法復原。',
        showCancelButton: true, confirmButtonText: '確認匯入', cancelButtonText: '取消'
    }).then(result => {
        if (!result.isConfirmed) return;
        document.getElementById('confirmImportBtn').disabled = true;
        fetch(`/api/import/jobs/${currentJobId}/confirm`, { method: 'POST' })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    const imported = data.data?.importedCount ?? 0;
                    const skipped  = data.data?.skippedCount  ?? 0;
                    const msg = skipped > 0
                        ? `已寫入 ${imported} 筆交易記錄，${skipped} 筆重複跳過。`
                        : `已寫入 ${imported} 筆交易記錄。`;
                    Swal.fire({ icon: 'success', title: '匯入成功', text: msg });
                } else {
                    document.getElementById('confirmImportBtn').disabled = false;
                    Swal.fire({ icon: 'error', title: '匯入失敗', text: data.error?.message || '請稍後再試' });
                }
            })
            .catch(() => {
                document.getElementById('confirmImportBtn').disabled = false;
                Swal.fire({ icon: 'error', title: '網路錯誤', text: '請稍後再試' });
            });
    });
}

function resetPage() {
    currentJobId = null;
    if (pollTimer) clearInterval(pollTimer);
    document.getElementById('fileInput').value = '';
    document.getElementById('uploadBtn').disabled = false;
    document.getElementById('progressCard').classList.add('d-none');
    document.getElementById('resultSection').classList.add('d-none');
    document.getElementById('errorCard').classList.add('d-none');
    document.getElementById('confirmImportBtn').disabled = true;
    document.getElementById('previewTableBody').innerHTML = '';
    document.getElementById('errorTableBody').innerHTML = '';
    const bar = document.getElementById('progressBar');
    bar.style.width = '0%';
    bar.textContent = '0%';
}

function formatAmount(val) {
    if (val == null) return '';
    return Number(val).toLocaleString('zh-TW', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}
