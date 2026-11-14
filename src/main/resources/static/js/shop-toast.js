(() => {
    const DEFAULT_TITLES = {
        success: 'Thành công',
        error: 'Thông tin chưa hợp lệ',
        warning: 'Lưu ý'
    };

    const ICON_CLASSES = {
        success: 'bi-check-circle-fill',
        error: 'bi-exclamation-triangle-fill',
        warning: 'bi-exclamation-circle-fill'
    };

    const MAX_VISIBLE_TOASTS = 4;

    function getOrCreateStack() {
        let stack = document.getElementById('shopToastStack');
        if (!stack && document.body) {
            stack = document.createElement('div');
            stack.id = 'shopToastStack';
            stack.className = 'shop-toast-stack';
            stack.setAttribute('aria-live', 'polite');
            stack.setAttribute('aria-atomic', 'false');
            document.body.appendChild(stack);
        }
        return stack;
    }

    function dismissToastElement(toastEl) {
        if (!toastEl || toastEl.dataset.dismissing === '1') return;
        toastEl.dataset.dismissing = '1';
        toastEl.classList.add('is-leaving');
        window.setTimeout(() => {
            if (toastEl.parentNode) toastEl.parentNode.removeChild(toastEl);
        }, 320);
    }

    function showShopToast(message, type = 'success', title = null, durationMs = 4500) {
        if (!message) return;
        const normalizedType = ['success', 'error', 'warning'].includes(type) ? type : 'success';
        const stack = getOrCreateStack();
        if (!stack) return;

        while (stack.children.length >= MAX_VISIBLE_TOASTS) {
            dismissToastElement(stack.firstElementChild);
        }

        const item = document.createElement('div');
        item.className = `shop-toast-item shop-toast--${normalizedType}`;
        item.setAttribute('role', normalizedType === 'error' ? 'alert' : 'status');

        const accent = document.createElement('div');
        accent.className = 'shop-toast-accent';

        const body = document.createElement('div');
        body.className = 'shop-toast-body';

        const iconWrap = document.createElement('div');
        iconWrap.className = 'shop-toast-icon';
        const icon = document.createElement('i');
        icon.className = `bi ${ICON_CLASSES[normalizedType]}`;
        icon.setAttribute('aria-hidden', 'true');
        iconWrap.appendChild(icon);

        const content = document.createElement('div');
        content.className = 'shop-toast-content';
        const titleEl = document.createElement('div');
        titleEl.className = 'shop-toast-title';
        titleEl.textContent = title || DEFAULT_TITLES[normalizedType];
        const msgEl = document.createElement('div');
        msgEl.className = 'shop-toast-message';
        msgEl.textContent = message;
        content.append(titleEl, msgEl);

        const closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'shop-toast-close';
        closeBtn.setAttribute('aria-label', 'Đóng thông báo');
        const closeIcon = document.createElement('i');
        closeIcon.className = 'bi bi-x-lg';
        closeIcon.setAttribute('aria-hidden', 'true');
        closeBtn.appendChild(closeIcon);
        closeBtn.addEventListener('click', () => dismissToastElement(item));

        body.append(iconWrap, content, closeBtn);

        const progress = document.createElement('div');
        progress.className = 'shop-toast-progress';
        progress.style.animationDuration = `${durationMs}ms`;

        item.append(accent, body, progress);
        stack.appendChild(item);

        // Hỗ trợ tạm dừng đếm ngược khi di chuột vào toast để đọc kỹ hơn
        let remainingMs = durationMs;
        let startedAt = Date.now();
        let timerId = window.setTimeout(() => dismissToastElement(item), remainingMs);

        item.addEventListener('mouseenter', () => {
            window.clearTimeout(timerId);
            remainingMs = Math.max(400, remainingMs - (Date.now() - startedAt));
        });

        item.addEventListener('mouseleave', () => {
            if (item.dataset.dismissing === '1') return;
            startedAt = Date.now();
            timerId = window.setTimeout(() => dismissToastElement(item), remainingMs);
        });
    }

    window.showShopToast = showShopToast;

    document.addEventListener('DOMContentLoaded', () => {
        const flashData = document.getElementById('shopFlashToastData');
        if (!flashData) return;

        const successMsg = flashData.dataset.success;
        const errorMsg = flashData.dataset.error;

        if (successMsg && successMsg.trim() !== '') {
            showShopToast(successMsg, 'success', 'Thành công', 4500);
        }
        if (errorMsg && errorMsg.trim() !== '') {
            showShopToast(errorMsg, 'error', 'Thông báo lỗi', 5000);
        }
    });
})();
