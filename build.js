// npm run build: builds the files that the imports load from the files in src/,
// and the file with all optional scripts that the loader of option 15 loads.
// The version comes from package.json; to start a new one, change it there, e.g. npm version 1.5.1 --no-git-tag-version
const fs = require("fs");
const crypto = require("crypto");
const postcss = require("postcss");
const postcssImport = require("postcss-import");
const selectorParser = require("postcss-selector-parser");
const { plugins } = require("./postcss.config.js");

process.chdir(__dirname);
const pkg = JSON.parse(fs.readFileSync("package.json", "utf8"));
const addOns = "Theme/assets/add-ons/";

// the optional scripts, in the order in which the loader of option 15 runs them; page-state is built from the theme
const scripts = ["page-state", "full-resolution-episode-images", "short-track-names", "episode-titles"];

// the names of src/settings/page-types.css: the build writes the check of each name in its place, as if it were
// written there, so the built file is the same as if every rule spelled out its checks
const pageTypes = {
    postcssPlugin: "fintage-page-types",
    Once(root) {
        const checks = {};
        root.walkAtRules("custom-selector", (rule) => {
            const [, name, check] = rule.params.match(/^(:--[\w-]+)\s+([\s\S]+)$/);
            checks[name] = check.replace(/\s+/g, " ").trim();
            rule.remove();
        });
        const expand = (selector, seen = []) =>
            selector.replace(/:--[\w-]+/g, (name) => {
                if (!(name in checks)) throw new Error(`unknown page type ${name}`);
                if (seen.includes(name)) throw new Error(`the page type ${name} uses itself`);
                return expand(checks[name], [...seen, name]);
            });
        root.walkRules((rule) => {
            if (rule.selector.includes(":--")) rule.selector = expand(rule.selector);
        });
    },
};

// The faster version for the optional scripts: every :has() that asks what the page shows becomes a question that
// the page-state script answers, and the rule only reads the answer, an attribute like [data-fintage~="p1a2b3c"].
// A :has() of the page of an item, like #itemDetailPage:has(.subtitle), is answered on that page; a :has() that looks
// for a page from outside, like .backdropContainer:has(~ * #itemDetailPage:not(.hide) ...), is answered on <html>.
// The other :has(), e.g. of cards and menus, stay as they are.
function pageQuestions() {
    const questions = { page: new Map(), document: new Map() };
    const ask = (scope, selector) => {
        const map = questions[scope];
        if (!map.has(selector)) {
            const hash = crypto
                .createHash("sha1")
                .update(scope + selector)
                .digest("hex");
            map.set(selector, (scope === "page" ? "p" : "d") + hash.slice(0, 6));
        }
        return map.get(selector);
    };
    const answer = (token) =>
        selectorParser.attribute({ attribute: "data-fintage", operator: "~=", value: token, quoteMark: '"', raws: {} });
    const compound = (node) => {
        const nodes = node.parent.nodes;
        let start = nodes.indexOf(node);
        let end = start;
        while (start > 0 && nodes[start - 1].type !== "combinator") start--;
        while (end < nodes.length - 1 && nodes[end + 1].type !== "combinator") end++;
        return nodes.slice(start, end + 1);
    };
    // whether a :has() belongs to the page of an item, also inside :not(), :is() or :where() of that page
    const onPage = (node) => {
        if (compound(node).some((n) => n.type === "id" && n.value === "itemDetailPage")) return true;
        const selector = node.parent;
        const pseudo = selector.parent;
        const alone = !selector.nodes.some((n) => n.type === "combinator");
        return alone && pseudo && pseudo.type === "pseudo" && [":not", ":is", ":where"].includes(pseudo.value)
            ? onPage(pseudo)
            : false;
    };
    const onRoot = (node) =>
        compound(node).some(
            (n) =>
                (n.type === "pseudo" && n.value === ":root") ||
                (n.type === "tag" && n.value === "html") ||
                (n.type === "class" && n.value.startsWith("layout-")),
        );
    const convert = (container) =>
        container.each((selector) =>
            selector.each((node) => {
                if (node.type !== "pseudo") return;
                if (node.value !== ":has") return node.nodes.length && convert(node);
                const argument = node.nodes.map(String).join(",").replace(/\s+/g, " ").trim();
                const lead = node.nodes[0].nodes[0];
                const combinator = lead && lead.type === "combinator" ? lead.value.trim() : "";
                if (onPage(node) && !["~", "+"].includes(combinator)) {
                    node.replaceWith(answer(ask("page", combinator ? ":scope " + argument : argument)));
                } else if (
                    /#itemDetailPage|\.standalonePage|#nowPlayingPage/.test(argument) &&
                    node.nodes.length === 1
                ) {
                    // the elements that look for the page from outside come before it, so the page follows them
                    let selector = argument.replace(/^[~+]\s*/, "").replace(/^\*\s+/, "");
                    if (selector.startsWith(">")) selector = "body " + selector;
                    const token = ask("document", selector);
                    node.replaceWith(
                        onRoot(node)
                            ? answer(token)
                            : selectorParser.pseudo({
                                  value: ":is",
                                  nodes: [
                                      selectorParser.selector({
                                          nodes: [
                                              selectorParser.pseudo({ value: ":root" }),
                                              answer(token),
                                              selectorParser.combinator({ value: " " }),
                                              selectorParser.universal(),
                                          ],
                                      }),
                                  ],
                              }),
                    );
                } else convert(node);
            }),
        );
    const plugin = {
        postcssPlugin: "fintage-page-questions",
        Once(root) {
            root.walkRules((rule) => {
                if (rule.selector.includes(":has(")) rule.selector = selectorParser(convert).processSync(rule.selector);
            });
        },
    };
    return { plugin, questions };
}

// the source of each file that the imports load
const files = [
    ["src/fintage.css", "Theme/Fintage-latest-minified.css"],
    ["src/add-ons/custom-media-covers.css", `${addOns}custom-media-covers-latest-min.css`],
    ["src/add-ons/media-bar-plugin-support.css", `${addOns}media-bar-plugin-support-latest-min.css`],
];

async function css(from, to, extra = [], note = "") {
    const result = await postcss([postcssImport(), pageTypes, ...extra, ...plugins]).process(
        fs.readFileSync(from, "utf8"),
        { from, to },
    );
    const text = result.css.replace(/^(\/\*![^*]*?) \*\//, `$1${note} */`);
    fs.writeFileSync(to, text.split("{{version}}").join(pkg.version));
    console.log(`${from} -> ${to}`);
}

async function build() {
    for (const [from, to] of files) await css(from, to);

    // the faster version, and the page-state script with the questions of exactly this build
    const { plugin, questions } = pageQuestions();
    await css(
        "src/fintage.css",
        "Theme/Fintage-js-latest-minified.css",
        [plugin],
        "; the faster version for the optional scripts, which needs page-state.js",
    );
    // written the way Prettier writes it: one question per line, or three lines when it is too long
    const string = (s) => (s.includes('"') ? `'${s}'` : `"${s}"`);
    const entry = ([selector, token]) => {
        const line = `        [${string(token)}, ${string(selector)}],`;
        return line.length <= 120
            ? line
            : `        [\n            ${string(token)},\n            ${string(selector)},\n        ],`;
    };
    const list = (map) => "[\n" + [...map].map(entry).join("\n") + "\n    ]";
    const script = fs
        .readFileSync("src/scripts/page-state.js", "utf8")
        .replace("/* PAGE_QUESTIONS */ []", list(questions.page))
        .replace("/* DOCUMENT_QUESTIONS */ []", list(questions.document));
    fs.writeFileSync(`${addOns}page-state.js`, script);
    console.log(
        `src/scripts/page-state.js -> ${addOns}page-state.js (${questions.page.size} + ${questions.document.size} questions)`,
    );

    bundle();
}

// one watcher for all scripts of the loader: each script hands it the function to run when the page changes, with the
// changes it cares about, instead of watching the whole page itself; a script on its own still watches the page itself
const watcher = `// the scripts share one watcher of the page
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
`;

// the loader of option 15 loads all optional scripts as one file; each of them can be left out
// by naming it in window.fintageScriptsOff, and each runs only once, also when it is installed on its own
function bundle() {
    const to = `${addOns}fintage-scripts-latest.js`;
    const parts = scripts.map((name) => {
        const code = fs.readFileSync(`${addOns}${name}.js`, "utf8").replace(/^\/\*![\s\S]*?\*\/\s*/, "");
        return `// ${name}\nif (!off.includes("${name}")) {\n${code}}\n`;
    });
    const text = [
        `/*! Fintage Scripts v${pkg.version} for Jellyfin by ricolehn: ${scripts.join(", ")} */`,
        "(function () {",
        '"use strict";',
        "const off = window.fintageScriptsOff || [];",
        watcher,
        ...parts,
        `console.info("Fintage: scripts v${pkg.version} are loaded");`,
        "})();",
        "",
    ].join("\n");
    fs.writeFileSync(to, text);
    console.log(`${scripts.map((name) => name + ".js").join(", ")} -> ${to}`);
}

build();
