/*! Add-on: Full Resolution Episode Images for the Fintage Theme for Jellyfin */

/* Jellyfin loads the image on the page of an episode at a quarter of the window width,
which is soft when Fintage shows it as the backdrop of the page;
this loads the image in the resolution of the screen instead, once it is shown.
Custom CSS cannot change the size Jellyfin asks for, so this needs a plugin
that adds JavaScript to the web client, e.g. JavaScript Injector. */
(function () {
    "use strict";

    // the script runs only once, also when the loader of option 15 brings it too
    const active = (window.fintageScripts ??= new Set());
    if (active.has("full-resolution-episode-images")) return;
    active.add("full-resolution-episode-images");

    const selector = "#itemDetailPage:not(.hide) .detailImageContainer > .backdropCard .cardImageContainer";
    const maxWidth = Math.min(Math.round(screen.width * (window.devicePixelRatio || 1)), 3840);

    function upgrade(image) {
        const match = /url\("?(.+?)"?\)/.exec(image.style.backgroundImage);
        if (!match || !match[1].includes("fillWidth=") || image.dataset.fullResolution === match[1]) return;

        // only episode pages have a link to their season in the title, and the TV layout keeps the small image
        const page = image.closest("#itemDetailPage");
        if (!page.querySelector('.itemName [data-type="Season"]')) return;
        if (document.documentElement.classList.contains("layout-tv")) return;

        image.dataset.fullResolution = match[1];
        const url = new URL(match[1], location.href);
        url.searchParams.delete("fillWidth");
        url.searchParams.delete("fillHeight");
        url.searchParams.set("maxWidth", maxWidth);

        // the small image stays until the large one has loaded, unless the page shows another episode by then
        const preload = new Image();
        preload.onload = function () {
            if (image.style.backgroundImage.includes(match[1])) {
                image.style.backgroundImage = 'url("' + url.href + '")';
            }
        };
        preload.src = url.href;
    }

    let scheduled = false;

    function scan() {
        if (scheduled) return;
        scheduled = true;
        requestAnimationFrame(function () {
            scheduled = false;
            document.querySelectorAll(selector).forEach(upgrade);
        });
    }

    function start() {
        // this shows in the browser console whether the script is loaded
        console.info("Fintage: full resolution episode images are active");
        new MutationObserver(scan).observe(document.body, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ["style"],
        });
        scan();
    }

    if (document.body) {
        start();
    } else {
        document.addEventListener("DOMContentLoaded", start);
    }
})();
