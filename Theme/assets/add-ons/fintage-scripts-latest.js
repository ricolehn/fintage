/*! Fintage Scripts v1.5.5 for Jellyfin by ricolehn: page-state, full-resolution-episode-images, short-track-names, episode-titles */
(function () {
"use strict";
const off = window.fintageScriptsOff || [];
// the scripts share one watcher of the page
if (!window.fintageWatch) {
    const watchers = [];
    const wants = (options, record) =>
        record.type === "childList" ? options.childList : (options.attributeFilter || []).includes(record.attributeName);
    const observer = new MutationObserver(function (records) {
        for (const w of watchers) if (records.some((record) => wants(w.options, record))) w.callback();
    });
    window.fintageWatch = function (callback, options) {
        watchers.push({ callback, options });
        // observing the page again replaces the changes the watcher listens to with those of all scripts
        const attributes = [...new Set(watchers.flatMap((w) => w.options.attributeFilter || []))];
        observer.observe(document.body, attributes.length
            ? { childList: true, subtree: true, attributes: true, attributeFilter: attributes }
            : { childList: true, subtree: true });
    };
}

// page-state
if (!off.includes("page-state")) {
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
        ["ddee130", '#itemDetailPage:not(.hide) .parentName [data-type="Series"]'],
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
}

// full-resolution-episode-images
if (!off.includes("full-resolution-episode-images")) {
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
}

// short-track-names
if (!off.includes("short-track-names")) {
/* On the pages of movies and episodes, Jellyfin names the audio tracks like "German - Dolby Digital+ - 5.1 - Default"
and the subtitles like "German - Forced - SUBRIP"; this shows only the language and the codec,
and whether the subtitles are forced, e.g. "German · EAC3" and "(Forced) German · SRT".
The full name stays as the tooltip, and tracks that would get the same name keep what tells them apart.
The names come from the server, so custom CSS cannot change them and this needs a plugin
that adds JavaScript to the web client, e.g. JavaScript Injector. */
(function () {
    "use strict";

    // the script runs only once, also when the loader of option 15 brings it too
    const active = (window.fintageScripts ??= new Set());
    if (active.has("short-track-names")) return;
    active.add("short-track-names");

    // the loader of option 15 brings one watcher for all its scripts; on its own, the script watches the page itself
    const watch =
        window.fintageWatch ||
        function (callback, options) {
            new MutationObserver(callback).observe(document.body, options);
        };

    const selector = "#itemDetailPage:not(.hide) .trackSelections :is(.selectAudio, .selectSubtitles)";

    const audioCodecs = {
        aac: "AAC",
        ac3: "AC3",
        alac: "ALAC",
        dts: "DTS",
        eac3: "EAC3",
        flac: "FLAC",
        mp2: "MP2",
        mp3: "MP3",
        opus: "Opus",
        truehd: "TrueHD",
        vorbis: "Vorbis",
        wmapro: "WMA Pro",
        wmav2: "WMA",
    };

    const subtitleCodecs = {
        ass: "ASS",
        dvb_subtitle: "DVB",
        dvbsub: "DVB",
        dvd_subtitle: "VobSub",
        dvdsub: "VobSub",
        hdmv_pgs_subtitle: "PGS",
        mov_text: "MOV_TEXT",
        pgssub: "PGS",
        srt: "SRT",
        ssa: "SSA",
        subrip: "SRT",
        ttml: "TTML",
        vtt: "VTT",
        webvtt: "VTT",
    };

    // Jellyfin uses the bibliographic language codes, the browser knows the terminological ones
    const languageCodes = {
        alb: "sqi",
        arm: "hye",
        baq: "eus",
        bur: "mya",
        chi: "zho",
        cze: "ces",
        dut: "nld",
        fre: "fra",
        geo: "kat",
        ger: "deu",
        gre: "ell",
        ice: "isl",
        mac: "mkd",
        mao: "mri",
        may: "msa",
        per: "fas",
        rum: "ron",
        slo: "slk",
        tib: "bod",
        wel: "cym",
    };

    // the names of the languages in the language of the web client, made when the first track is named,
    // since the page is not ready yet when the script starts
    let languageNames;

    function displayNames() {
        if (languageNames === undefined) {
            try {
                languageNames = new Intl.DisplayNames([document.documentElement.lang || navigator.language, "en"], {
                    type: "language",
                });
            } catch (e) {
                // older browsers fall back to the name the server gives the track
                languageNames = null;
            }
        }
        return languageNames;
    }

    function capitalize(text) {
        return text.charAt(0).toUpperCase() + text.slice(1);
    }

    function language(stream) {
        if (stream.LocalizedLanguage) return capitalize(stream.LocalizedLanguage);

        const code = (stream.Language || "").toLowerCase();
        if (!code || code === "und") return stream.LocalizedUndefined || "Undefined";

        try {
            const names = displayNames();
            const name = names && names.of(languageCodes[code] || code);
            if (name && name.toLowerCase() !== code) return capitalize(name);
        } catch (e) {
            // the code is not a valid language tag
        }

        // the server puts the name of the language first in the name of the track, unless the track has a title
        const first = !stream.Title && stream.DisplayTitle ? stream.DisplayTitle.split(" - ")[0].trim() : "";
        if (first && first.toLowerCase() !== code) return capitalize(first);
        return code.toUpperCase();
    }

    function audioCodec(stream) {
        const codec = (stream.Codec || "").toLowerCase();
        const profile = stream.Profile || "";
        let name = codec.startsWith("pcm") ? "PCM" : audioCodecs[codec] || codec.toUpperCase();

        if (codec === "dts") {
            if (/DTS:X/i.test(profile)) name = "DTS:X";
            else if (/\bMA\b/i.test(profile)) name = "DTS-HD MA";
            else if (/HRA/i.test(profile)) name = "DTS-HD HRA";
            else if (/Express/i.test(profile)) name = "DTS Express";
        }
        return name;
    }

    function subtitleCodec(stream) {
        const codec = (stream.Codec || "").toLowerCase();
        return subtitleCodecs[codec] || codec.toUpperCase();
    }

    function shortName(stream) {
        const codec = stream.Type === "Audio" ? audioCodec(stream) : subtitleCodec(stream);
        const name = [language(stream), codec].filter(Boolean).join(" · ");
        return stream.Type !== "Audio" && stream.IsForced ? `(${stream.LocalizedForced || "Forced"}) ${name}` : name;
    }

    // what tells two tracks with the same language and codec apart, e.g. a commentary or the subtitles for the deaf,
    // in the order it is tried
    const extras = [
        (stream) => stream.Title || "",
        (stream) => (stream.IsHearingImpaired ? stream.LocalizedHearingImpaired || "SDH" : ""),
        (stream) => (stream.Type === "Audio" && stream.ChannelLayout ? capitalize(stream.ChannelLayout) : ""),
    ];

    function withExtras(name, stream, kinds) {
        return [name]
            .concat(kinds.map((kind) => kind(stream)))
            .filter(Boolean)
            .join(" · ");
    }

    function names(streams) {
        const base = streams.map(shortName);

        return base.map(function (name, i) {
            const group = streams.filter((stream, j) => base[j] === name);
            if (group.length === 1) return name;

            // the first detail that tells the whole group apart, then all of them, then the number in the group
            for (const kinds of extras.map((kind) => [kind]).concat([extras])) {
                const named = group.map((stream) => withExtras(name, stream, kinds));
                if (new Set(named).size === group.length) return withExtras(name, streams[i], kinds);
            }
            return name + " · #" + (group.indexOf(streams[i]) + 1);
        });
    }

    const items = new Map();

    function getItem(id) {
        if (!items.has(id)) {
            const apiClient = window.ApiClient;
            const request = apiClient.getItem(apiClient.getCurrentUserId(), id).catch(function () {
                items.delete(id);
                return null;
            });
            items.set(id, request);
        }
        return items.get(id);
    }

    function currentItemId() {
        const query = location.hash.includes("?") ? location.hash.split("?")[1] : location.search;
        return new URLSearchParams(query).get("id");
    }

    function sameId(a, b) {
        return Boolean(a && b) && a.replace(/-/g, "").toLowerCase() === b.replace(/-/g, "").toLowerCase();
    }

    function rename(select, item) {
        const page = select.closest("#itemDetailPage");
        const sourceId = page.querySelector(".selectSource") && page.querySelector(".selectSource").value;
        const sources = item.MediaSources || [];
        const source = sources.find((s) => s.Id === sourceId) || sources[0];
        if (!source) return;

        const type = select.classList.contains("selectAudio") ? "Audio" : "Subtitle";
        const options = [...select.options].filter((option) => option.value !== "-1");
        const streams = options.map((option) =>
            (source.MediaStreams || []).find((s) => s.Type === type && String(s.Index) === option.value),
        );
        if (streams.some((stream) => !stream)) return;

        names(streams).forEach(function (name, i) {
            const option = options[i];
            if (option.textContent === name) return;
            option.title = option.textContent;
            option.textContent = name;
            option.dataset.shortName = name;
        });
    }

    let scheduled = false;

    function scan() {
        if (scheduled) return;
        scheduled = true;
        requestAnimationFrame(function () {
            scheduled = false;
            const selects = [...document.querySelectorAll(selector)].filter((select) =>
                [...select.options].some(
                    (option) => option.value !== "-1" && option.dataset.shortName !== option.textContent,
                ),
            );
            const id = currentItemId();
            if (!selects.length || !id || !window.ApiClient) return;

            getItem(id).then(function (item) {
                // the page may show another item by the time the answer arrives
                if (item && sameId(item.Id, currentItemId())) selects.forEach((select) => rename(select, item));
            });
        });
    }

    function start() {
        // this shows in the browser console whether the script is loaded
        console.info("Fintage: short track names are active");
        watch(scan, { childList: true, subtree: true });
        scan();
    }

    if (document.body) {
        start();
    } else {
        document.addEventListener("DOMContentLoaded", start);
    }
})();
}

// episode-titles
if (!off.includes("episode-titles")) {
/* On the page of an episode, Jellyfin writes the season, the number and the name of the episode in one line,
like "Season 2 - 4. Woe's Hollow"; this splits it into a smaller line with the season and the number,
like "Season 2 · Episode 4", and the name of the episode below it, which Fintage styles.
The number and the name come as one piece of text, so custom CSS cannot split them and this needs a plugin
that adds JavaScript to the web client, e.g. JavaScript Injector. */
(function () {
    "use strict";

    // the script runs only once, also when the loader of option 15 brings it too
    const active = (window.fintageScripts ??= new Set());
    if (active.has("episode-titles")) return;
    active.add("episode-titles");

    // the loader of option 15 brings one watcher for all its scripts; on its own, the script watches the page itself
    const watch =
        window.fintageWatch ||
        function (callback, options) {
            new MutationObserver(callback).observe(document.body, options);
        };

    const selector = '#itemDetailPage .itemName > bdi:has(> a[data-type="Season"])';

    // the word Jellyfin itself uses for "Episode" in each language, where it is not "Episode"
    const words = {
        ar: "حلقة",
        "be-by": "Серыя",
        "bg-bg": "Епизод",
        "bn-bd": "পর্ব",
        br: "Rann",
        bs: "Epizoda",
        ca: "Episodi",
        ckb: "ئەڵقە",
        cs: "Epizoda",
        cy: "Pennod",
        de: "Folge",
        el: "Επεισόδιο",
        eo: "Epizodo",
        es: "Episodio",
        "es-ar": "Capítulo",
        et: "Episood",
        eu: "Kapitulua",
        fa: "قسمت سریال",
        fi: "Jakso",
        fil: "Episodyo",
        fo: "Partur",
        fr: "Épisode",
        ga: "Eipeasóid",
        gl: "Episodio",
        he: "פרק",
        hr: "Epizoda",
        hu: "Epizód",
        id: "Episod",
        "is-is": "Þáttur",
        it: "Episodio",
        ja: "エピソード",
        ka: "ეპიზოდი",
        kk: "Telebölım",
        ko: "에피소드",
        "lt-lt": "Epizodas",
        lv: "Sērija",
        mk: "Епизода",
        ml: "എപ്പിസോഡ്",
        mn: "Анги",
        mr: "भाग",
        my: "အပိုင်း",
        nl: "Aflevering",
        pa: "TV ਦੀ ਕਿਸ਼ਤ",
        pl: "Odcinek",
        pt: "Episódio",
        ro: "Episod",
        ru: "Эпизод",
        sk: "Epizóda",
        "sl-si": "Epizoda",
        sq: "Episodi",
        sr: "Епизода",
        sv: "Avsnitt",
        ta: "அத்தியாயம்",
        te: "ఎపిసోడ్",
        th: "ตอน",
        tr: "Bölüm",
        uk: "Епізод",
        "ur-pk": "قسط",
        uz: "Epizod",
        vi: "Tập",
        "zh-cn": "剧集",
        "zh-hk": "劇集",
        "zh-tw": "劇集",
        zu: "Isiqephu",
    };

    function episodeWord() {
        const lang = (document.documentElement.lang || "en").toLowerCase();
        return words[lang] || words[lang.split("-")[0]] || "Episode";
    }

    function split(bdi) {
        const season = bdi.querySelector(':scope > a[data-type="Season"]');
        const text = season && season.nextSibling;
        if (!text || text.nodeType !== Node.TEXT_NODE || text.nextSibling) return;

        // " - 4. Woe's Hollow", " - 4-5. Two Episodes in One File" or, without a number, " - Woe's Hollow"
        const match = /^\s*-\s*(?:(\d+(?:-\d+)?)\.\s+)?([\s\S]*)$/.exec(text.textContent);
        if (!match) return;

        const numbers = document.createElement("span");
        numbers.className = "fintageEpisodeNumbers";
        numbers.append(season);
        if (match[1]) numbers.append(" · " + episodeWord() + " " + match[1].replace("-", "–"));

        const name = document.createElement("span");
        name.className = "fintageEpisodeName";
        name.textContent = match[2];

        text.remove();
        bdi.append(numbers, name);
    }

    let scheduled = false;

    function scan() {
        if (scheduled) return;
        scheduled = true;
        requestAnimationFrame(function () {
            scheduled = false;
            document.querySelectorAll(selector).forEach(split);
        });
    }

    function start() {
        // this shows in the browser console whether the script is loaded
        console.info("Fintage: episode titles are active");
        watch(scan, { childList: true, subtree: true });
        scan();
    }

    if (document.body) {
        start();
    } else {
        document.addEventListener("DOMContentLoaded", start);
    }
})();
}

console.info("Fintage: scripts v1.5.5 are loaded");
})();
