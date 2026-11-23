// npm run build: builds the files that the imports load from the files in src/,
// and the file with all optional scripts that the loader of option 15 loads.
// The version comes from package.json; to start a new one, change it there, e.g. npm version 1.5.1 --no-git-tag-version
const fs = require("fs");
const postcss = require("postcss");
const postcssImport = require("postcss-import");
const { plugins } = require("./postcss.config.js");

process.chdir(__dirname);
const pkg = JSON.parse(fs.readFileSync("package.json", "utf8"));
const addOns = "Theme/assets/add-ons/";

// the optional scripts, in the order in which the loader of option 15 runs them
const scripts = ["full-resolution-episode-images", "short-track-names", "episode-titles"];

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

// the source of each file that the imports load
const files = [
    ["src/fintage.css", "Theme/Fintage-latest-minified.css"],
    ["src/add-ons/custom-media-covers.css", `${addOns}custom-media-covers-latest-min.css`],
    ["src/add-ons/media-bar-plugin-support.css", `${addOns}media-bar-plugin-support-latest-min.css`],
];

async function build() {
    for (const [from, to] of files) {
        const result = await postcss([postcssImport(), pageTypes, ...plugins]).process(fs.readFileSync(from, "utf8"), {
            from,
            to,
        });
        fs.writeFileSync(to, result.css.split("{{version}}").join(pkg.version));
        console.log(`${from} -> ${to}`);
    }
    bundle();
}

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
        ...parts,
        `console.info("Fintage: scripts v${pkg.version} are loaded");`,
        "})();",
        "",
    ].join("\n");
    fs.writeFileSync(to, text);
    console.log(`${scripts.map((name) => name + ".js").join(", ")} -> ${to}`);
}

build();
