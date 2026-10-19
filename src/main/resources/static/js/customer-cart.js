(() => {
    const cart = document.getElementById('cartSelection');
    if (!cart) return;
    const clearDialog = document.getElementById('clearCartDialog');
    const confirmClear = document.getElementById('confirmClearCart');
    let clearForm = null;
    cart.querySelectorAll('.cart-clear-form').forEach(form => {
        form.addEventListener('submit', event => {
            event.preventDefault();
            clearForm = form;
            document.getElementById('clearCartShopName').textContent = form.dataset.shopName;
            confirmClear.disabled = false;
            confirmClear.textContent = 'Xóa giỏ';
            clearDialog.showModal();
        });
    });
    document.getElementById('cancelClearCart').addEventListener('click', () => clearDialog.close());
    clearDialog.addEventListener('close', () => { clearForm = null; });
    clearDialog.addEventListener('click', event => {
        const bounds = clearDialog.getBoundingClientRect();
        if (event.target === clearDialog && (event.clientX < bounds.left || event.clientX > bounds.right
                || event.clientY < bounds.top || event.clientY > bounds.bottom)) clearDialog.close();
    });
    confirmClear.addEventListener('click', () => {
        if (!clearForm || confirmClear.disabled) return;
        confirmClear.disabled = true;
        confirmClear.textContent = 'Đang xóa…';
        HTMLFormElement.prototype.submit.call(clearForm);
    });
    const money = amount => new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 2}).format(amount) + 'đ';
    let saveQueue = Promise.resolve();
    cart.querySelectorAll('.cart-shop-block').forEach(shop => {
        const availableItems = () => Array.from(shop.querySelectorAll('.cart-item-select:not(:disabled)'));
        const selectAll = shop.querySelector('.cart-shop-select-all');
        const orderButton = shop.querySelector('.cart-order-button');
        const storageKey = 'betafruit-shop-cart-' + cart.dataset.customerId + '-' + shop.dataset.shopId;
        try {
            const saved = JSON.parse(sessionStorage.getItem(storageKey));
            if (Array.isArray(saved)) availableItems().forEach(item => item.checked = saved.includes(item.value));
        } catch (_) { /* Selection works without browser storage. */ }

        function updateSummary() {
            const items = availableItems();
            const selected = items.filter(item => item.checked);
            const quantity = selected.reduce((sum, item) => sum + Number(item.dataset.quantity), 0);
            const cents = selected.reduce((sum, item) => sum + Math.round(Number(item.dataset.subtotal) * 100), 0);
            shop.querySelector('.cart-selected-quantity').textContent = quantity;
            shop.querySelector('.cart-selected-total').textContent = money(cents / 100);
            shop.querySelector('.cart-no-selection').hidden = selected.length > 0;
            shop.querySelector('.cart-shop-warning').hidden = !shop.querySelector('.cart-item-select:disabled');
            selectAll.disabled = items.length === 0;
            selectAll.checked = items.length > 0 && selected.length === items.length;
            selectAll.indeterminate = selected.length > 0 && selected.length < items.length;
            orderButton.disabled = selected.length === 0;
            try {
                sessionStorage.setItem(storageKey, JSON.stringify(selected.map(item => item.value)));
            } catch (_) { /* No persistence is required to select items. */ }
        }
        shop.querySelectorAll('.cart-item-select').forEach(item => item.addEventListener('change', updateSummary));
        selectAll.addEventListener('change', () => {
            availableItems().forEach(item => item.checked = selectAll.checked);
            updateSummary();
        });
        shop.querySelectorAll('.cart-quantity-form').forEach(form => {
            const input = form.querySelector('[name="quantity"]');
            const status = form.querySelector('.cart-save-status');
            const row = form.closest('.cart-item');
            const checkbox = row.querySelector('.cart-item-select');
            let timer;
            let queued = false;
            function setStatus(message, error = false) {
                status.textContent = message;
                status.classList.toggle('is-error', error);
            }
            async function save() {
                if (!input.checkValidity()) {
                    input.value = input.dataset.savedQuantity;
                    setStatus('Số lượng phải là số nguyên lớn hơn 0.', true);
                    return;
                }
                if (Number(input.value) === Number(input.dataset.savedQuantity)) return;
                const body = new URLSearchParams(new FormData(form));
                input.disabled = true;
                form.setAttribute('aria-busy', 'true');
                setStatus('Đang lưu…');
                try {
                    const response = await fetch(form.dataset.updateUrl, {
                        method: 'POST', headers: {'Accept': 'application/json'}, body
                    });
                    if (response.redirected || !response.headers.get('content-type')?.includes('application/json')) {
                        throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
                    }
                    const result = await response.json();
                    if (!response.ok) throw new Error(result.message || 'Không thể cập nhật số lượng.');
                    const item = result.item;
                    const wasDisabled = checkbox.disabled;
                    input.value = item.quantity;
                    input.dataset.savedQuantity = item.quantity;
                    checkbox.dataset.quantity = item.quantity;
                    checkbox.dataset.subtotal = item.subtotal;
                    checkbox.disabled = !item.available;
                    if (!item.available) checkbox.checked = false;
                    else if (wasDisabled) checkbox.checked = true;
                    row.classList.toggle('cart-item-unavailable', !item.available);
                    row.querySelector('.cart-unit-value').textContent = item.available ? money(item.unitPrice) : '—';
                    row.querySelector('.cart-subtotal-value').textContent = item.available ? money(item.subtotal) : '—';
                    const warning = row.querySelector('.cart-item-warning');
                    warning.textContent = item.warning || '';
                    warning.hidden = item.available;
                    document.querySelectorAll('.cart-count').forEach(badge => badge.textContent = result.cartCount);
                    updateSummary();
                    setStatus('Đã lưu');
                } catch (error) {
                    input.value = input.dataset.savedQuantity;
                    setStatus(error.message || 'Lưu chưa thành công. Vui lòng thử lại.', true);
                } finally {
                    input.disabled = false;
                    form.removeAttribute('aria-busy');
                }
            }
            function enqueueSave() {
                    if (queued) return;
                    queued = true;
                    // Serialize saves so older responses cannot overwrite newer badge counts.
                    saveQueue = saveQueue.then(async () => {
                        queued = false;
                        await save();
                    }).catch(() => { queued = false; });
            }
            function schedule(delay) {
                clearTimeout(timer);
                timer = setTimeout(enqueueSave, delay);
            }
            input.addEventListener('input', () => schedule(450));
            input.addEventListener('change', () => schedule(0));
            form.querySelectorAll('.cart-quantity-step').forEach(button => {
                button.addEventListener('click', () => {
                    if (input.disabled) return;
                    const value = Number(input.value) + Number(button.dataset.step);
                    if (!Number.isInteger(value) || value < 1 || value > 2147483647) return;
                    input.value = value;
                    schedule(300);
                });
            });
            form.addEventListener('submit', event => { event.preventDefault(); schedule(0); });
        });
        updateSummary();
    });
})();
