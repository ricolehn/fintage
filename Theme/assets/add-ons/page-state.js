/*! Add-on: Page State for the Fintage Theme for Jellyfin */

/* The faster version of the theme, Theme/Fintage-js-latest-minified.css, leaves the questions about what a page shows,
like "is this the page of an episode?", to this script: it answers them whenever the page changes and writes the answers
onto the page, so that the browser does not have to ask them again for every element it styles.
npm run build writes the questions below from the theme in src/, so that they always fit the faster version of the
same build; change the theme there, not here. This needs a plugin that adds JavaScript to the web client,
e.g. JavaScript Injector, and it is part of the loader of option 15. */
(function () {
    "use strict";

    // the script runs only once, also when the loader of option 15 brings it too
    const active = (window.fintageScripts ??= new Set());
    if (active.has("page-state")) return;
    active.add("page-state");

    // the loader of option 15 brings one watcher for all its scripts; on its own, the script watches the page itself
    const watch =
        window.fintageWatch ||
        function (callback, options) {
            new MutationObserver(callback).observe(document.body, options);
        };

    // each question is a token for the theme and the selector that answers it: questions about the page of an item are
    // answered on that page, the others on <html>, both in the attribute data-fintage, e.g. data-fintage="p1a2b3c"
    const pageQuestions = [
        ["p1e9ef5", ".subtitle, .musicParentName"],
        ["p8d05e1", ".detailImageContainer :is(.backdropCard, .squareCard, .person, .queue, .book, .live_tv)"],
        ["p6b518a", ".detailImageContainer .cardPadder-square > .person"],
        ["p7565e5", ".subtitle"],
        ["p26cca4", ".detailImageContainer .person"],
        ["p43c719", '.parentName [data-type="Series"]'],
        ["p8b4ab4", '.itemName [data-type="Season"]'],
        ["p65d95c", ".detailLogo:not(.hide)"],
        ["p76dc82", ".detailImageContainer :is(.backdropCard, .album)"],
        ["p129a90", ".detailImageContainer .backdropCard"],
        ["pf94326", ".detailImageContainer > .backdropCard"],
        ["p8d1428", ".detailImageContainer .cardImageIcon:is(.movie, .tv, .video_library, .folder)"],
        ["p62f7d7", ".subtitle, .musicParentName, .detailImageContainer :is(.backdropCard, .squareCard)"],
        ["pdc1b65", ":scope > .detailLogo:not(.hide)"],
        ["p064c3d", ".infoWrapper > .detailImageContainer .cardImageIcon:is(.album, .queue)"],
    ];
    const documentQuestions = [
        ["d9ea610", "#itemDetailPage:not(.hide)"],
        ["db54897", '#itemDetailPage:not(.hide) .itemName [data-type="Season"]'],
        ["d11bd75", "#itemDetailPage:not(.hide) .detailImageContainer > .backdropCard"],
        [
            "d739c6a",
            "main #itemDetailPage:not(.hide) .detailImageContainer .cardImageIcon:is(.movie, .tv, .video_library, .folder)",
        ],
        [
            "d101bc3",
            "#itemDetailPage:not(.hide) .detailImageContainer .cardImageIcon:is(.movie, .tv, .video_library, .folder)",
        ],
        [
            "db7889d",
            "#itemDetailPage:not(.hide) :is(.subtitle, .musicParentName, .detailImageContainer :is(.backdropCard, .squareCard))",
        ],
        ["ddee130", '#itemDetailPage:not(.hide) .parentName [data-type="Series"]'],
        ["de8ccfb", "#nowPlayingPage:not(.hide)"],
        ["db64277", "main #nowPlayingPage:not(.hide)"],
        [
            "dc75653",
            "main #itemDetailPage:not(.hide) .infoWrapper > .detailImageContainer .cardImageIcon:is(.album, .queue)",
        ],
        [
            "df30985",
            "#itemDetailPage:not(.hide) .infoWrapper > .detailImageContainer .cardImageIcon:is(.album, .queue)",
        ],
        ["d4cbde0", "main #itemDetailPage:not(.hide)"],
        ["d2b4538", "main .standalonePage:not(.hide)"],
    ];

    function write(element, questions, root) {
        const answer = questions
            .filter(([, selector]) => root.querySelector(selector))
            .map(([token]) => token)
            .join(" ");
        if (element.getAttribute("data-fintage") !== answer) element.setAttribute("data-fintage", answer);
    }

    function answer() {
        // Jellyfin keeps the pages it showed before, hidden, so only the page that is shown is asked
        for (const page of document.querySelectorAll('[id="itemDetailPage"]:not(.hide)')) {
            write(page, pageQuestions, page);
        }
        write(document.documentElement, documentQuestions, document);
    }

    function start() {
        // this shows in the browser console whether the script is loaded
        console.info("Fintage: page state is active");
        // the answers change when elements come or go, or when a page is shown or hidden by its class
        watch(answer, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ["class"],
        });
        answer();
    }

    if (document.body) {
        start();
    } else {
        document.addEventListener("DOMContentLoaded", start);
    }
})();
