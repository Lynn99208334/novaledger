(() => {
    let seconds = 5;
    const el = document.getElementById('countdown');
    if (!el) return;

    const timer = setInterval(() => {
        seconds--;
        el.textContent = seconds;
        if (seconds <= 0) {
            clearInterval(timer);
            window.location.href = '/page/login';
        }
    }, 1000);
})();
