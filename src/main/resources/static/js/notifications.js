/**
 * GoalTracker Pro — Bildirim WebSocket/STOMP İstemcisi
 * Navbar bildirim zili ve gerçek zamanlı bildirim yönetimi
 */

(function () {
    'use strict';

    let stompClient = null;
    let reconnectAttempts = 0;
    const MAX_RECONNECT_ATTEMPTS = 10;
    const BASE_RECONNECT_DELAY = 2000;

    // DOM elements
    const badgeEl = document.getElementById('notificationBadge');
    const countEl = document.getElementById('notificationCount');
    const listEl = document.getElementById('notificationList');
    const noNotificationsEl = document.getElementById('noNotifications');

    /**
     * Initialize notification system on page load.
     */
    function init() {
        loadUnreadCount();
        loadRecentNotifications();
        connectWebSocket();

        // Reconnect on visibility change
        document.addEventListener('visibilitychange', function () {
            if (!document.hidden && (!stompClient || !stompClient.connected)) {
                connectWebSocket();
            }
        });
    }

    /**
     * Fetch unread notification count from API.
     */
    function loadUnreadCount() {
        fetch('/api/notifications/unread-count', {
            headers: { 'Accept': 'application/json' }
        })
        .then(function (res) { return res.json(); })
        .then(function (data) {
            if (data.success && data.data) {
                updateBadge(data.data.count);
            }
        })
        .catch(function (err) {
            console.warn('Bildirim sayısı yüklenemedi:', err);
        });
    }

    /**
     * Fetch recent notifications from API and render in dropdown.
     */
    function loadRecentNotifications() {
        fetch('/api/notifications?page=0&size=10', {
            headers: { 'Accept': 'application/json' }
        })
        .then(function (res) { return res.json(); })
        .then(function (data) {
            if (data.success && data.data && data.data.content) {
                renderNotificationList(data.data.content);
            }
        })
        .catch(function (err) {
            console.warn('Bildirimler yüklenemedi:', err);
        });
    }

    /**
     * Render notifications in the dropdown list.
     */
    function renderNotificationList(notifications) {
        if (!listEl) return;

        if (notifications.length === 0) {
            listEl.innerHTML = '<div class="text-center py-3 text-muted"><i class="bi bi-bell-slash"></i> Bildirim yok</div>';
            return;
        }

        var html = '';
        notifications.forEach(function (n) {
            var readClass = n.read ? '' : 'bg-light';
            var iconClass = getIconClass(n.type);
            var colorClass = getColorClass(n.type);
            var timeStr = formatRelativeTime(n.createdAt);

            html += '<a href="/notifications" class="dropdown-item d-flex align-items-start gap-2 py-2 border-bottom ' + readClass + '" ' +
                    'onclick="markNotificationRead(' + n.id + ')">' +
                    '<i class="bi ' + iconClass + ' ' + colorClass + ' mt-1"></i>' +
                    '<div class="flex-grow-1">' +
                    '<div class="small fw-semibold">' + escapeHtml(n.title) + '</div>' +
                    '<div class="small text-muted text-truncate" style="max-width: 250px;">' + escapeHtml(n.message) + '</div>' +
                    '<div class="small text-muted">' + timeStr + '</div>' +
                    '</div>' +
                    '</a>';
        });

        listEl.innerHTML = html;
    }

    /**
     * Update the notification badge count.
     */
    function updateBadge(count) {
        if (!badgeEl || !countEl) return;
        if (count > 0) {
            countEl.textContent = count > 99 ? '99+' : count;
            badgeEl.style.display = 'inline-block';
        } else {
            badgeEl.style.display = 'none';
        }
    }

    /**
     * Connect to WebSocket using SockJS + STOMP.
     */
    function connectWebSocket() {
        if (typeof SockJS === 'undefined' || typeof StompJs === 'undefined') {
            // Libraries not loaded, skip WebSocket
            console.debug('SockJS/STOMP kütüphaneleri yüklenmemiş, WebSocket atlanıyor.');
            return;
        }

        try {
            var socket = new SockJS('/ws');
            stompClient = new StompJs.Client({
                webSocketFactory: function () { return socket; },
                reconnectDelay: 0, // We handle reconnection manually
                debug: function () {} // Suppress debug logs
            });

            stompClient.onConnect = function () {
                reconnectAttempts = 0;
                console.debug('WebSocket bağlantısı kuruldu.');

                stompClient.subscribe('/user/queue/notifications', function (message) {
                    try {
                        var notification = JSON.parse(message.body);
                        onNewNotification(notification);
                    } catch (e) {
                        console.warn('Bildirim parse hatası:', e);
                    }
                });
            };

            stompClient.onStompError = function (frame) {
                console.warn('STOMP hatası:', frame.headers['message']);
                scheduleReconnect();
            };

            stompClient.onWebSocketClose = function () {
                console.debug('WebSocket bağlantısı koptu.');
                scheduleReconnect();
            };

            stompClient.activate();
        } catch (e) {
            console.warn('WebSocket bağlantı hatası:', e);
            scheduleReconnect();
        }
    }

    /**
     * Schedule a reconnection with exponential backoff.
     */
    function scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            console.warn('Maksimum yeniden bağlanma denemesine ulaşıldı.');
            return;
        }

        var delay = BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts);
        reconnectAttempts++;

        console.debug('Yeniden bağlanma denemesi ' + reconnectAttempts + ', ' + delay + 'ms sonra...');
        setTimeout(function () {
            if (!document.hidden) {
                connectWebSocket();
            }
        }, delay);
    }

    /**
     * Handle a new real-time notification.
     */
    function onNewNotification(notification) {
        // Update badge count
        loadUnreadCount();
        // Reload notification list
        loadRecentNotifications();
        // Show toast
        showToast(notification);
    }

    /**
     * Show a Bootstrap toast for a new notification.
     */
    function showToast(notification) {
        var toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            toastContainer.style.zIndex = '9999';
            document.body.appendChild(toastContainer);
        }

        var iconClass = getIconClass(notification.type);
        var colorClass = getColorClass(notification.type);

        var toastHtml =
            '<div class="toast show" role="alert" aria-live="assertive" aria-atomic="true" data-bs-autohide="true" data-bs-delay="8000">' +
            '<div class="toast-header">' +
            '<i class="bi ' + iconClass + ' ' + colorClass + ' me-2"></i>' +
            '<strong class="me-auto">' + escapeHtml(notification.title) + '</strong>' +
            '<small class="text-muted">Şimdi</small>' +
            '<button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Kapat"></button>' +
            '</div>' +
            '<div class="toast-body">' + escapeHtml(notification.message) + '</div>' +
            '</div>';

        var wrapper = document.createElement('div');
        wrapper.innerHTML = toastHtml;
        var toastEl = wrapper.firstChild;
        toastContainer.appendChild(toastEl);

        // Initialize and show Bootstrap toast
        if (typeof bootstrap !== 'undefined') {
            var toast = new bootstrap.Toast(toastEl);
            toast.show();
        }

        // Remove from DOM after it hides
        toastEl.addEventListener('hidden.bs.toast', function () {
            toastEl.remove();
        });

        // Auto-remove after 10 seconds as fallback
        setTimeout(function () {
            if (toastEl.parentNode) {
                toastEl.remove();
            }
        }, 10000);
    }

    /**
     * Mark a single notification as read via API.
     */
    window.markNotificationRead = function (notificationId) {
        fetch('/api/notifications/' + notificationId + '/read', {
            method: 'PUT',
            headers: { 'Accept': 'application/json' }
        })
        .then(function () {
            loadUnreadCount();
        })
        .catch(function (err) {
            console.warn('Bildirim okundu işaretlenemedi:', err);
        });
    };

    /**
     * Mark all notifications as read via API.
     */
    window.markAllNotificationsRead = function () {
        fetch('/api/notifications/read-all', {
            method: 'PUT',
            headers: { 'Accept': 'application/json' }
        })
        .then(function () {
            loadUnreadCount();
            loadRecentNotifications();
        })
        .catch(function (err) {
            console.warn('Bildirimler okundu işaretlenemedi:', err);
        });
    };

    // --- Helper Functions ---

    function getIconClass(type) {
        var icons = {
            'DAILY_REMINDER': 'bi-clock',
            'STREAK_DANGER': 'bi-exclamation-triangle',
            'STREAK_LOST': 'bi-emoji-frown',
            'BADGE_EARNED': 'bi-trophy',
            'GOAL_COMPLETED': 'bi-check-circle',
            'WEEKLY_SUMMARY': 'bi-bar-chart',
            'FRIEND_ACTIVITY': 'bi-people'
        };
        return icons[type] || 'bi-bell';
    }

    function getColorClass(type) {
        var colors = {
            'DAILY_REMINDER': 'text-info',
            'STREAK_DANGER': 'text-warning',
            'STREAK_LOST': 'text-danger',
            'BADGE_EARNED': 'text-success',
            'GOAL_COMPLETED': 'text-success',
            'WEEKLY_SUMMARY': 'text-primary',
            'FRIEND_ACTIVITY': 'text-secondary'
        };
        return colors[type] || 'text-primary';
    }

    function formatRelativeTime(isoString) {
        if (!isoString) return '';
        var date = new Date(isoString);
        var now = new Date();
        var diffMs = now - date;
        var diffMin = Math.floor(diffMs / 60000);
        var diffHour = Math.floor(diffMin / 60);
        var diffDay = Math.floor(diffHour / 24);

        if (diffMin < 1) return 'Az önce';
        if (diffMin < 60) return diffMin + ' dk önce';
        if (diffHour < 24) return diffHour + ' saat önce';
        if (diffDay < 7) return diffDay + ' gün önce';
        return date.toLocaleDateString('tr-TR');
    }

    function escapeHtml(text) {
        if (!text) return '';
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

