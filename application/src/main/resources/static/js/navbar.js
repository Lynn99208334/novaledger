function getTokenExpiration(token) {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.exp ? payload.exp * 1000 : null;
    } catch (e) {
        return null;
    }
}

function formatCountdown(ms) {
    if (ms <= 0) return '已過期';
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes} 分 ${seconds} 秒後登出`;
}

let countdownInterval = null;

function initNavbar() {
    const token = localStorage.getItem('accessToken');
    const username = localStorage.getItem('username');

    const userDropdownItem = document.getElementById('userDropdownItem');
    const loginLinkItem = document.getElementById('loginLinkItem');

    if (!userDropdownItem || !loginLinkItem) return;

    if (token && username) {
        userDropdownItem.classList.remove('d-none-init');
        loginLinkItem.classList.add('d-none-init');

        const navUsername = document.getElementById('navUsername');
        const navUsernameDetail = document.getElementById('navUsernameDetail');
        if (navUsername) navUsername.textContent = username;
        if (navUsernameDetail) navUsernameDetail.textContent = username;

        const expMs = getTokenExpiration(token);
        if (expMs) {
            const expiryEl = document.getElementById('navTokenExpiry');
            if (countdownInterval) clearInterval(countdownInterval);

            countdownInterval = setInterval(() => {
                const remaining = expMs - Date.now();
                if (expiryEl) expiryEl.textContent = formatCountdown(remaining);
                if (remaining <= 0) {
                    clearInterval(countdownInterval);
                    handleLogout();
                }
            }, 1000);
        }
    } else {
        userDropdownItem.classList.add('d-none-init');
        loginLinkItem.classList.remove('d-none-init');
    }
}

async function handleLogout() {
    const token = localStorage.getItem('accessToken');

    if (token) {
        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: { 'Authorization': 'Bearer ' + token }
            });
        } catch (e) {
            console.warn('Logout API call failed, proceeding with local cleanup', e);
        }
    }

    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('username');
    window.location.href = '/page/login';
}

function redirectToLogin() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/page/login';
}

async function apiFetch(url, options = {}) {
    const accessToken = localStorage.getItem('accessToken');

    if (!accessToken) {
        redirectToLogin();
        return null;
    }

    const headers = options.headers || {};

    const response = await fetch(url, {
        ...options,
        headers: {
            ...headers,
            'Authorization': 'Bearer ' + accessToken
        }
    });

    if (response.status === 401 || response.status === 403) {
        redirectToLogin();
        return null;
    }

    return response;
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        initNavbar();
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) logoutBtn.addEventListener('click', handleLogout);
    });
} else {
    initNavbar();
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) logoutBtn.addEventListener('click', handleLogout);
}
