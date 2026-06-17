(() => {
    // 從 URL 取得 token
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get("token");
    const resetForm = document.getElementById("resetForm");
    const invalidTokenMsg = document.getElementById("invalidTokenMsg");

    if (!token) {
        invalidTokenMsg.classList.remove("d-none");
        resetForm.classList.add("d-none");
        return;
    }

    resetForm.addEventListener("submit", async function (e) {
        e.preventDefault();

        const newPassword = document.getElementById("newPassword").value;
        const confirmPassword = document.getElementById("confirmPassword").value;
        const successMsg = document.getElementById("successMsg");
        const errorMsg = document.getElementById("errorMsg");
        const passwordMismatch = document.getElementById("passwordMismatch");
        const submitBtn = document.getElementById("submitBtn");

        errorMsg.classList.add("d-none");
        passwordMismatch.classList.add("d-none");

        if (newPassword !== confirmPassword) {
            passwordMismatch.classList.remove("d-none");
            return;
        }

        submitBtn.disabled = true;
        submitBtn.textContent = "重設中...";

        try {
            const response = await fetch("/api/auth/reset-password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ token, newPassword })
            });

            if (!response.ok) {
                const data = await response.json();
                errorMsg.textContent = data.error?.message || "連結無效或已過期，請重新申請。";
                errorMsg.classList.remove("d-none");
                submitBtn.disabled = false;
                submitBtn.textContent = "確認重設";
                return;
            }

            successMsg.classList.remove("d-none");
            resetForm.classList.add("d-none");

            setTimeout(() => {
                window.location.href = "/page/login";
            }, 3000);

        } catch (err) {
            errorMsg.textContent = "發生錯誤，請稍後再試。";
            errorMsg.classList.remove("d-none");
            submitBtn.disabled = false;
            submitBtn.textContent = "確認重設";
        }
    });
})();
