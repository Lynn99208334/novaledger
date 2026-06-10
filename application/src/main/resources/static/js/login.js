function fillLogin(email, password) {
    document.getElementById("email").value = email;
    document.getElementById("password").value = password;
}

document.addEventListener("DOMContentLoaded", function () {

    // 快捷填入按鈕
    document.querySelectorAll("[data-fill-email]").forEach(function (btn) {
        btn.addEventListener("click", function () {
            fillLogin(btn.dataset.fillEmail, btn.dataset.fillPassword);
        });
    });

    // 登入表單送出
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
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ email: email, password: password })
            });

            if (!response.ok) {
                const data = await response.json();
                const errorCode = data?.error?.errorCode;

                if (errorCode === "AUTH_033") {
                    loginError.textContent = "登入失敗次數過多，請 15 分鐘後再試。";
                } else if (errorCode === "AUTH_001") {
                    loginError.textContent = "Email 尚未驗證，請先完成驗證。";
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

            window.location.href = "/page/dashboard";

        } catch (error) {
            console.error("Login failed", error);
            loginError.textContent = "發生錯誤，請稍後再試。";
            loginError.classList.remove("d-none");
        }
    });
});
