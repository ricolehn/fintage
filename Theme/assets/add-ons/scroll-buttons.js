/*! Add-on: Scroll Buttons for the Fintage Theme for Jellyfin */

/* On desktops, Jellyfin shows two arrows above each row that scrolls sideways, e.g. on the home page,
but only when the browser reports a mouse as the main pointer; on desktops with a touch screen,
it leaves them out and shows a scrollbar under each row instead, which Fintage hides.
This adds the same arrows there, and leaves the rows alone wherever Jellyfin shows its own.
Custom CSS cannot add buttons that scroll, so this needs a plugin
that adds JavaScript to the web client, e.g. JavaScript Injector. */
(function () {
    "use strict";

    const selector = '.emby-scroller:not([data-horizontal="false"], [data-scrollbuttons="false"])';

    // Jellyfin loads the layout of its arrows only together with them; :where() lets the theme's rules come first
    const css = `
        :where(.emby-scroller-container) { position: relative; }
        :where(.emby-scrollbuttons) {
            position: absolute; top: 0; z-index: 1; display: flex; align-items: center; justify-content: center;
            min-width: 104px; min-height: 24px; padding-top: 0.85em; color: #fff;
        }
        :where([dir="ltr"] .emby-scrollbuttons) { right: 0; }
        :where([dir="rtl"] .emby-scrollbuttons) { left: 0; }
        :where(.detailVerticalSection .emby-scrollbuttons) { padding-top: 0.4em; }
        :where(.emby-scrollbuttons-button > .material-icons) { display: block; min-width: 24px; min-height: 24px; }
    `;

    const done = new WeakSet();
    const refreshers = new WeakMap();
    const resizes = new ResizeObserver(function (entries) {
        entries.forEach(function (entry) {
            const refresh = refreshers.get(entry.target);
            if (refresh) refresh();
        });
    });

    // Jellyfin adds its own arrows on desktops whose main pointer is a mouse, and none on phones and TVs
    function wanted() {
        return document.documentElement.classList.contains("layout-desktop") && !matchMedia("(pointer: fine)").matches;
    }

    function button(direction, title) {
        const element = document.createElement("button");
        element.type = "button";
        element.title = title;
        element.className = "emby-scrollbuttons-button paper-icon-button-light";
        element.dataset.direction = direction;
        element.innerHTML = '<span class="material-icons chevron_' + direction + '" aria-hidden="true"></span>';
        return element;
    }

    // like Jellyfin, an arrow pages by the cards in view, so the card that was cut off at the edge comes first
    function distance(scroller, direction) {
        const frame = scroller.getBoundingClientRect();
        const style = getComputedStyle(scroller);
        const start = frame.left + parseFloat(style.paddingLeft);
        const end = frame.right - parseFloat(style.paddingRight);
        const cards = Array.from(scroller.querySelector(".scrollSlider")?.children || []).map(function (card) {
            return card.getBoundingClientRect();
        });
        if (direction === "right") {
            const hidden = cards.filter((card) => card.right > end + 1).sort((a, b) => a.left - b.left)[0];
            return hidden && hidden.left > start ? hidden.left - start : end - start;
        }
        const hidden = cards.filter((card) => card.left < start - 1).sort((a, b) => b.right - a.right)[0];
        return hidden && hidden.right < end ? hidden.right - end : start - end;
    }

    function update(buttons, scroller) {
        const max = scroller.scrollWidth - scroller.clientWidth;
        // in right-to-left languages, the position counts down from 0
        const rtl = getComputedStyle(scroller).direction === "rtl";
        const fromLeft = rtl ? scroller.scrollLeft + max : scroller.scrollLeft;
        buttons.classList.toggle("hide", max <= 20);
        buttons.firstChild.disabled = fromLeft < 1;
        buttons.lastChild.disabled = fromLeft > max - 1;
    }

    function add(scroller) {
        done.add(scroller);
        const before = scroller.previousElementSibling;
        if (before && before.classList.contains("emby-scrollbuttons")) return;

        const buttons = document.createElement("div");
        buttons.className = "emby-scrollbuttons padded-right";
        buttons.append(button("left", "Previous"), button("right", "Next"));
        scroller.before(buttons);
        scroller.parentNode.classList.add("emby-scroller-container");

        buttons.addEventListener("click", function (event) {
            const target = event.target.closest(".emby-scrollbuttons-button");
            if (target) scroller.scrollBy({ left: distance(scroller, target.dataset.direction), behavior: "smooth" });
        });

        const refresh = function () {
            update(buttons, scroller);
        };
        scroller.addEventListener("scroll", refresh, { passive: true });
        refreshers.set(scroller, refresh);
        resizes.observe(scroller);
        const slider = scroller.querySelector(".scrollSlider");
        if (slider) {
            refreshers.set(slider, refresh);
            resizes.observe(slider);
        }
        refresh();
    }

    let scheduled = false;

    function scan() {
        if (scheduled) return;
        scheduled = true;
        requestAnimationFrame(function () {
            scheduled = false;
            if (!wanted()) return;
            document.querySelectorAll(selector).forEach(function (scroller) {
                if (!done.has(scroller)) add(scroller);
            });
        });
    }

    function start() {
        // this shows in the browser console whether the script is loaded
        console.info("Fintage: scroll buttons are active");
        const style = document.createElement("style");
        style.textContent = css;
        document.head.prepend(style);
        new MutationObserver(scan).observe(document.body, { childList: true, subtree: true });
        scan();
    }

    if (document.body) {
        start();
    } else {
        document.addEventListener("DOMContentLoaded", start);
    }
})();
