/**
 * GoalTracker Pro — Minimal JavaScript
 * Bootstrap modal tetikleme, form submit yardımcıları
 */

// ─── Frontend Hata Loglama ───────────────────────────────────────────────────
// Yakalanmayan JS hataları sunucu console'una iletilir (yalnızca dev'de anlamlı)
function sendClientError(payload) {
    try {
        var body = JSON.stringify(Object.assign({ url: window.location.pathname }, payload));
        var blob = new Blob([body], { type: 'application/json' });
        navigator.sendBeacon('/api/client-error', blob);
    } catch (_) { /* sessiz kal — loglama hatası uygulamayı bozmasın */ }
}

window.onerror = function (message, source, line, col, error) {
    sendClientError({
        message: message,
        source:  source,
        line:    line,
        col:     col,
        stack:   error ? error.stack : ''
    });
};

window.addEventListener('unhandledrejection', function (event) {
    sendClientError({
        message: 'Unhandled Promise Rejection: ' + (event.reason ? event.reason.toString() : 'unknown'),
        stack:   event.reason && event.reason.stack ? event.reason.stack : ''
    });
});
// ────────────────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', function () {

    // Flash mesajlarını 5 saniye sonra otomatik kapat
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            bsAlert.close();
        }, 5000);
    });

    // Form submit butonlarını çift tıklamaya karşı koru
    const forms = document.querySelectorAll('form[data-disable-on-submit]');
    forms.forEach(function (form) {
        form.addEventListener('submit', function () {
            const btn = form.querySelector('button[type="submit"]');
            if (btn) {
                btn.disabled = true;
                btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Kaydediliyor...';
            }
        });
    });

    // Sidebar aktif link vurgulama (URL bazlı)
    const currentPath = window.location.pathname;
    const sidebarLinks = document.querySelectorAll('.sidebar .nav-link');
    sidebarLinks.forEach(function (link) {
        const href = link.getAttribute('href');
        if (href && currentPath.startsWith(href) && href !== '/') {
            link.classList.add('active');
        }
    });
});

