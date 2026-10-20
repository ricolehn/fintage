/*! Add-on: Episode Titles for the Fintage Theme for Jellyfin */

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
        new MutationObserver(scan).observe(document.body, { childList: true, subtree: true });
        scan();
    }

    if (document.body) {
        start();
    } else {
        document.addEventListener("DOMContentLoaded", start);
    }
})();
