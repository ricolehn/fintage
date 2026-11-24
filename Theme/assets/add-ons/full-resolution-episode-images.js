/*! Add-on: Full Resolution Episode Images for the Fintage Theme for Jellyfin */

/* Jellyfin loads the image on the page of an episode at a quarter of the window width,
which is soft when Fintage shows it as the backdrop of the page;
this loads the image in the resolution of the screen instead, once it is shown.
Jellyfin loads the poster on the page of a season at a quarter of the window width as well, which is soft where
Fintage shows it as the card of the page; this loads it as large as it is shown, in the pixels of the screen.
Custom CSS cannot change the size Jellyfin asks for, so this needs a plugin
that adds JavaScript to the web client, e.g. JavaScript Injector. */
(function () {
    "use strict";

    // the script runs only once, also when the loader of option 15 brings it too
    const active = (window.fintageScripts ??= new Set());
    if (active.has("full-resolution-episode-images")) return;
    active.add("full-resolution-episode-images");

    // the loader of option 15 brings one watcher for all its scripts; on its own, the script watches the page itself
    const watch =
        window.fintageWatch ||
        function (callback, options) {
            new MutationObserver(callback).observe(document.body, options);
        };

    const selector = "#itemDetailPage:not(.hide) .detailImageContainer > .card .cardImageContainer";
    const pixels = window.devicePixelRatio || 1;
    const maxWidth = Math.min(Math.round(screen.width * pixels), 3840);

    // a poster is loaded as wide as it is shown, in the pixels of the screen, in steps of 200 pixels, so a poster that is
    // shown a little larger or smaller on another page or device comes from the same cache of the server
    function posterWidth(image) {
        return Math.min(Math.ceil((image.getBoundingClientRect().width * pixels) / 200) * 200, 2000);
    }

    // the width to load an image at, or nothing when it stays as it is
    function widthFor(image, page) {
        // only episode pages have a link to their season in the title; their own image is a landscape card
        if (page.querySelector('.itemName [data-type="Season"]')) {
            return image.closest(".backdropCard") ? maxWidth : 0;
        }
        // the pages of seasons have a link to their series above their title; Jellyfin draws their poster in two places,
        // one for phones and one for the other layouts, and only the one that the theme shows counts
        if (page.querySelector('.parentName [data-type="Series"]')) {
            return image.getClientRects().length ? posterWidth(image) : 0;
        }
        return 0;
    }

    function upgrade(image) {
        const match = /url\("?(.+?)"?\)/.exec(image.style.backgroundImage);
        if (!match || !match[1].includes("fillWidth=") || image.dataset.fullResolution === match[1]) return;

        // the TV layout keeps the small images
        if (document.documentElement.classList.contains("layout-tv")) return;

        const width = widthFor(image, image.closest("#itemDetailPage"));
        if (!width) return;

        image.dataset.fullResolution = match[1];
        const url = new URL(match[1], location.href);
        url.searchParams.delete("fillWidth");
        url.searchParams.delete("fillHeight");
        url.searchParams.set("maxWidth", width);

        // the small image stays until the large one has loaded, unless the page shows another episode or season by then
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
        watch(scan, {
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
