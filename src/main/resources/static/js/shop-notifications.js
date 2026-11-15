(() => {
    const root = document.getElementById('shop-live-notifications');
    if (!root) return;
    const badge = root.querySelector('[data-notification-count]');
    const toggle = document.getElementById('shop-notification-toggle');
    const list = root.querySelector('[data-notification-list]');
    const announcement = document.getElementById('shop-notification-announcement');
    let lastCount = Number(badge.textContent) || 0;
    let inFlight = false;
    let expired = false;

    function render(notifications) {
        const content = document.createDocumentFragment();
        if (notifications.length === 0) {
            const empty = document.createElement('p');
            empty.className = 'text-muted p-3 mb-0';
            empty.textContent = 'Bạn chưa có thông báo nào.';
            content.append(empty);
        }
        for (const notification of notifications) {
            const row = document.createElement('div');
            row.className = 'border-bottom p-3' + (notification.isRead ? '' : ' bg-light');
            const form = document.createElement('form');
            form.method = 'post';
            form.action = root.dataset.readBase + notification.notificationId;
            const redirect = document.createElement('input');
            redirect.type = 'hidden';
            redirect.name = 'redirect';
            redirect.value = notification.actionUrl || '/shop/orders';
            const button = document.createElement('button');
            button.type = 'submit';
            button.className = 'btn btn-link text-start text-decoration-none p-0 w-100';
            const title = document.createElement('span');
            title.className = 'd-block fw-semibold text-dark';
            title.textContent = notification.title;
            const message = document.createElement('span');
            message.className = 'd-block small text-secondary text-break';
            message.textContent = notification.message;
            const time = document.createElement('span');
            time.className = 'd-block small text-muted mt-1';
            time.textContent = notification.createdAt
                ? new Date(notification.createdAt).toLocaleString('vi-VN') : '';
            button.append(title, message, time);
            form.append(redirect, button);
            row.append(form);
            content.append(row);
        }
        list.replaceChildren(content);
    }

    async function refresh() {
        if (inFlight || expired || document.hidden) return;
        inFlight = true;
        try {
            const response = await fetch(root.dataset.feedUrl, {
                credentials: 'same-origin', cache: 'no-store', headers: {Accept: 'application/json'}
            });
            if (response.status === 401) {
                expired = true;
                return;
            }
            if (!response.ok) return;
            const feed = await response.json();
            badge.textContent = feed.unreadCount > 99 ? '99+' : String(feed.unreadCount);
            badge.classList.toggle('d-none', feed.unreadCount === 0);
            toggle.setAttribute('aria-label', `Thông báo: ${feed.unreadCount} chưa đọc`);
            if (feed.unreadCount > lastCount) {
                announcement.textContent = `Bạn có ${feed.unreadCount} thông báo chưa đọc. Vui lòng kiểm tra đơn hàng mới.`;
            }
            lastCount = feed.unreadCount;
            // Preserve keyboard focus and active form controls while the dropdown is open.
            if (!list.contains(document.activeElement)) render(feed.notifications);
        } catch (_) {
            // Keep the last successful result; retry on the next polling interval.
        } finally {
            inFlight = false;
        }
    }
    root.addEventListener('show.bs.dropdown', refresh);
    document.addEventListener('visibilitychange', refresh);
    window.setInterval(refresh, 15000);
    refresh();
})();
