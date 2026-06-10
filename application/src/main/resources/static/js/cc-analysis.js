document.addEventListener('DOMContentLoaded', () => {
    const area = document.querySelector('.cc-drop-inner');
    const btnChoose = document.querySelector('.cc-drop-button');
    const preview = document.getElementById('preview');
    const btnNext = document.getElementById('nextBtn');
    const dropWrap = document.getElementById('dropWrap');
    const previewSection = document.getElementById('previewSection');

    if (!area || !btnChoose || !preview) return;

    let pickedFile = null;

    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.pdf,.csv,.xlsx,.xls';
    input.style.display = 'none';
    document.body.appendChild(input);

    btnChoose.addEventListener('click', () => input.click());
    input.addEventListener('change', e => handleFiles(e.target.files));

    ['dragenter', 'dragover'].forEach(ev =>
        area.addEventListener(ev, e => { e.preventDefault(); area.classList.add('is-dragover'); })
    );
    ['dragleave', 'drop'].forEach(ev =>
        area.addEventListener(ev, e => { e.preventDefault(); area.classList.remove('is-dragover'); })
    );
    area.addEventListener('drop', e => handleFiles(e.dataTransfer.files));

    function handleFiles(files) {
        if (!files || !files.length) return;
        const f = files[0];
        if (!/\.pdf$/i.test(f.name)) { alert('請上傳 PDF'); return; }
        pickedFile = f;
        area.querySelector('.cc-drop-title').textContent = f.name;
        area.querySelector('.cc-drop-hint').textContent = '已選擇 1 個檔案（按「下一步」開始解析）';
        preview.textContent = `檔案：${f.name}\n\n尚未解析，請按「下一步」。`;
    }

    if (btnNext) {
        btnNext.addEventListener('click', async (e) => {
            e.preventDefault();
            if (!pickedFile) { alert('請先選擇檔案'); return; }
            preview.textContent = '解析中…';
            btnNext.disabled = true;
            try {
                const fd = new FormData();
                fd.append('file', pickedFile);
                const csrf = document.querySelector('meta[name="_csrf"]')?.content;
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
                const headers = csrf && csrfHeader ? { [csrfHeader]: csrf } : {};
                const res = await fetch('/api/cc/upload', { method: 'POST', body: fd, headers });
                if (!res.ok) {
                    preview.textContent = `上傳或解析失敗（${res.status}）`;
                    return;
                }
                const data = await res.json();
                renderPreview(data, pickedFile.name);
                document.querySelectorAll('.cc-step').forEach(el => el.classList.remove('cc-step--active'));
                document.querySelectorAll('.cc-step')[1]?.classList.add('cc-step--active');
                dropWrap?.classList.add('is-hidden');
                previewSection?.classList.remove('is-hidden');
            } catch (err) {
                preview.textContent = '上傳或解析發生例外：' + err.message;
            } finally {
                btnNext.disabled = false;
            }
        });
    }

    function renderPreview(data, filename) {
        const hdr = `${data.bank || ''} ${data.statementMonth || ''}`.trim();
        if (Array.isArray(data.items) && data.items.length) {
            const table = document.createElement('table');
            table.className = 'table table-sm table-striped';
            table.innerHTML = `<thead><tr><th>交易日期</th><th>入帳日期</th><th>交易項目</th><th>幣別</th><th class="text-end">金額</th><th class="text-end">新臺幣</th></tr></thead><tbody></tbody>`;
            const tbody = table.querySelector('tbody');
            data.items.forEach(it => {
                const tr = document.createElement('tr');
                tr.innerHTML = `<td>${it.txnDate ?? ''}</td><td>${it.postDate ?? ''}</td><td>${escapeHtml(it.description ?? '')}</td><td>${it.currency ?? ''}</td><td class="text-end">${it.amount ?? ''}</td><td class="text-end">${it.twd ?? ''}</td>`;
                tbody.appendChild(tr);
            });
            preview.innerHTML = `<div class="mb-2">檔案：${filename} ${hdr ? `| ${hdr}` : ''}</div>`;
            preview.appendChild(table);
        } else {
            const text = (data.rawHeader && data.rawHeader.trim()) ? data.rawHeader : (data.rawTableText || '');
            preview.textContent = `檔案：${filename}\n\n${text || '[無內容]'}`;
        }
    }

    function escapeHtml(s) {
        return s.replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
    }
});
