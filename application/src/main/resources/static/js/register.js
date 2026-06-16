document.addEventListener('DOMContentLoaded', function () {

    document.getElementById('registerForm').addEventListener('submit', async function (e) {
        e.preventDefault();

        const username = document.getElementById('username').value.trim();
        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;

        const errorEl = document.getElementById('registerError');
        const successEl = document.getElementById('registerSuccess');
        const submitBtn = document.getElementById('submitBtn');

        errorEl.classList.add('d-none');
        errorEl.textContent = '';
        successEl.classList.add('d-none');
        submitBtn.disabled = true;
        submitBtn.textContent = '處理中...';

        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, password })
            });

            const data = await res.json();

            if (res.ok) {
                successEl.textContent = '帳號建立成功！請前往信箱完成 Email 驗證。';
                successEl.classList.remove('d-none');
                document.getElementById('registerForm').reset();
            } else {
                const errorCode = data?.error?.errorCode;
                let message = '註冊失敗，請稍後再試。';

                if (errorCode === 'AUTH_022') {
                    message = '目前暫停開放新用戶註冊。';
                } else if (errorCode === 'AUTH_007') {
                    message = '此使用者名稱已被使用。';
                } else if (errorCode === 'AUTH_011') {
                    message = '此 Email 已被註冊。';
                } else if (errorCode === 'E40001') {
                    message = '請確認填寫資料格式正確。';
                }

                errorEl.textContent = message;
                errorEl.classList.remove('d-none');
            }
        } catch (err) {
            errorEl.textContent = '發生錯誤，請稍後再試。';
            errorEl.classList.remove('d-none');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = '建立帳號';
        }
    });
});
