(() => {
    const form = document.getElementById("forgotForm");
    if (!form) return;

    form.addEventListener("submit", async function (e) {
        e.preventDefault();

        const email = document.getElementById("email").value;
        const successMsg = document.getElementById("successMsg");
        const errorMsg = document.getElementById("errorMsg");
        const submitBtn = document.getElementById("submitBtn");

        successMsg.classList.add("d-none");
        errorMsg.classList.add("d-none");
        submitBtn.disabled = true;
        submitBtn.textContent = "寄送中...";

        try {
            await fetch("/api/auth/forgot-password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email })
            });

            // 不管 email 是否存在，都顯示成功（避免 email 枚舉攻擊）
            successMsg.classList.remove("d-none");
            form.classList.add("d-none");

        } catch (err) {
            errorMsg.classList.remove("d-none");
            submitBtn.disabled = false;
            submitBtn.textContent = "寄送重設連結";
        }
    });
})();
