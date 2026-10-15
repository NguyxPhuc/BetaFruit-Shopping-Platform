(() => {
    function showImageFallback(image) {
        image.hidden = true;
        const placeholder = image.parentElement.querySelector('.home-no-image');
        if (placeholder) placeholder.hidden = false;
    }
    document.querySelectorAll('[data-product-image]').forEach(image => {
        image.addEventListener('error', () => showImageFallback(image));
        if (image.complete && image.naturalWidth === 0) showImageFallback(image);
    });
})();
