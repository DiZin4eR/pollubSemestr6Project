(function () {
    const loader = document.querySelector("[data-page-loader]");

    if (!loader) {
        return;
    }

    let showTimer;
    let elapsedTimer;
    let startedAt;

    document.addEventListener("submit", (event) => {
        if (!event.defaultPrevented) {
            showLoader();
        }
    });

    document.addEventListener("click", (event) => {
        const link = event.target.closest("a[href]");

        if (!link || event.defaultPrevented || shouldIgnoreLink(event, link)) {
            return;
        }

        showLoader();
    });

    window.addEventListener("pageshow", hideLoader);

    function shouldIgnoreLink(event, link) {
        if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
            return true;
        }

        if (link.target && link.target !== "_self") {
            return true;
        }

        if (link.hasAttribute("download")) {
            return true;
        }

        const href = link.getAttribute("href");

        if (!href || href.startsWith("#") || href.startsWith("mailto:") || href.startsWith("tel:")) {
            return true;
        }

        const url = new URL(link.href, window.location.href);

        return url.origin !== window.location.origin;
    }

    function showLoader() {
        window.clearTimeout(showTimer);

        showTimer = window.setTimeout(() => {
            startedAt = Date.now();
            loader.removeAttribute("hidden");
            loader.setAttribute("aria-hidden", "false");
            document.documentElement.classList.add("is-loading");
            updateLoaderText();
            elapsedTimer = window.setInterval(updateLoaderText, 1000);
        }, 120);
    }

    function hideLoader() {
        window.clearTimeout(showTimer);
        window.clearInterval(elapsedTimer);
        loader.setAttribute("hidden", "");
        loader.setAttribute("aria-hidden", "true");
        document.documentElement.classList.remove("is-loading");
    }

    function updateLoaderText() {
        const text = loader.querySelector("[data-loader-text]");

        if (!text || !startedAt) {
            return;
        }

        const elapsedSeconds = Math.floor((Date.now() - startedAt) / 1000);

        if (elapsedSeconds < 3) {
            text.textContent = "Processing request";
            return;
        }

        text.textContent = `Still working... ${elapsedSeconds}s`;
    }
})();
