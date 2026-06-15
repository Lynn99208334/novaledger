$(function () {
    // Select2 初始化
    $('#bankSelect').select2({
        placeholder: '請選擇銀行...',
        allowClear: true,
        width: '100%',
        language: {
            noResults: function () { return '找不到符合的銀行'; }
        }
    });

    // 銀行選擇連動：自動帶入 bankCode 和 bankName（hidden）
    $('#bankSelect').on('change', function () {
        const selected = $(this).find(':selected');
        const code = $(this).val();
        const name = selected.data('name') || '';

        document.getElementById('bankCode').value = code || '';
        document.getElementById('bankName').value = name || '';
    });

    // 初始餘額預設 0
    const balanceInput = document.getElementById('initialBalance');
    if (balanceInput && balanceInput.value === '') {
        balanceInput.value = '0';
    }
});
