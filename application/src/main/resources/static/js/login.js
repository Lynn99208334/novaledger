document.addEventListener("DOMContentLoaded", function () {

    // 快捷填入（密碼存在 JS 裡，不放 HTML attribute）
    const devAccounts = {
        "admin@novaledger.dev": "password123",
        "alice@novaledger.dev": "password123",
        "bob@novaledger.dev":   "password123",
        "son@novaledger.dev":   "password123"
    };

    document.querySelectorAll("[data-fill-email]").forEach(function (btn) {
        btn.addEventListener("click", function () {
            const email = btn.dataset.fillEmail;
            document.getElementById("email").value = email;
            document.getElementById("password").value = devAccounts[email] || "";
        });
    });

    document.getElementById("loginForm").addEventListener("submit", async function (event) {
        event.preventDefault();

        const email = document.getElementById("email").value;
        const password = document.getElementById("password").value;
        const loginError = document.getElementById("loginError");

        loginError.classList.add("d-none");
        loginError.textContent = "";

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: email, password: password })
            });

            if (!response.ok) {
                const data = await response.json();
                const errorCode = data?.error?.errorCode;

                if (errorCode === "AUTH_033") {
                    loginError.textContent = "登入失敗次數過多，請 15 分鐘後再試。";
                } else if (errorCode === "AUTH_001") {
                    loginError.textContent = "Email 尚未驗證，請先完成驗證。";
                } else if (errorCode === "AUTH_020") {
                    loginError.textContent = "此帳號已被停用，請聯繫管理員。";
                } else if (errorCode === "AUTH_021") {
                    loginError.textContent = "此帳號狀態異常，請聯繫管理員。";
                } else {
                    loginError.textContent = "帳號或密碼錯誤。";
                }

                loginError.classList.remove("d-none");
                return;
            }

            const data = await response.json();

            localStorage.setItem("accessToken", data.accessToken);
            localStorage.setItem("refreshToken", data.refreshToken);
            localStorage.setItem("username", data.username);

            const payload = JSON.parse(atob(data.accessToken.split('.')[1]));
            const roles = payload.roles || [];

            if (roles.includes("ROLE_ADMIN")) {
                window.location.href = "/page/admin/dashboard";
            } else {
                window.location.href = "/page/dashboard";
            }

        } catch (error) {
            console.error("Login failed", error);
            loginError.textContent = "發生錯誤，請稍後再試。";
            loginError.classList.remove("d-none");
        }
    });
});
