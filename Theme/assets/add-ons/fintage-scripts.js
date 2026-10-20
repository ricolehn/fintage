/*! Fintage Scripts for Jellyfin: the loader */

/* Paste this once into JavaScript Injector: every device then loads the newest version of all optional
Fintage scripts from GitHub, the way the import does for the theme, so new and improved scripts arrive by themselves.
Each device fetches them again once a day, and jsDelivr, which delivers them, can keep an old version for up to 12 hours.
It runs whatever is on the main branch of ricolehn/fintage; to stay on a version, replace @main with a release, e.g. @v1.1.1. */
(function () {
    "use strict";

    // to leave out a script, name it here, e.g. ["short-track-names"]; the names are in options 16 to 18 of the README
    window.fintageScriptsOff = [];

    const script = document.createElement("script");
    script.src =
        "https://cdn.jsdelivr.net/gh/ricolehn/fintage@main/Theme/assets/add-ons/fintage-scripts-latest.js?" +
        new Date().toISOString().slice(0, 10);
    document.head.append(script);
})();
