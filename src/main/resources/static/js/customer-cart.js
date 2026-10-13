(() => {
    const cart = document.getElementById('cartSelection');
    if (!cart) return;
    const items = Array.from(cart.querySelectorAll('.cart-item-select:not(:disabled)'));
    const selectAll = document.getElementById('selectAllItems');
    const storageKey = 'betafruit-cart-selection-' + cart.dataset.customerId;
    try {
        const saved = JSON.parse(sessionStorage.getItem(storageKey));
        if (Array.isArray(saved)) items.forEach(item => item.checked = saved.includes(item.value));
    } catch (_) { /* Selection works even if browser storage is unavailable. */ }

    function updateSummary() {
        const selected = items.filter(item => item.checked);
        const quantity = selected.reduce((sum, item) => sum + Number(item.dataset.quantity), 0);
        // Work in integer cents to preserve the server's two-decimal money values.
        const cents = selected.reduce((sum, item) => sum + Math.round(Number(item.dataset.subtotal) * 100), 0);
        document.getElementById('selectedQuantity').textContent = quantity;
        document.getElementById('selectedTotal').textContent = new Intl.NumberFormat('vi-VN', {
            maximumFractionDigits: 2
        }).format(cents / 100) + 'đ';
        document.getElementById('noSelection').hidden = selected.length > 0;
        selectAll.disabled = items.length === 0;
        selectAll.checked = items.length > 0 && selected.length === items.length;
        selectAll.indeterminate = selected.length > 0 && selected.length < items.length;
        try {
            sessionStorage.setItem(storageKey, JSON.stringify(selected.map(item => item.value)));
        } catch (_) { /* No persistence is required to select items. */ }
    }
    items.forEach(item => item.addEventListener('change', updateSummary));
    selectAll.addEventListener('change', () => {
        items.forEach(item => item.checked = selectAll.checked);
        updateSummary();
    });
    updateSummary();
})();
