// npm run build: builds the minified files that the imports load from the readable files of the version in package.json
// npm run new-version -- 1.0.1: copies the readable files of the current version to a new version, which then gets the changes
const fs = require("fs");
const postcss = require("postcss");
const { plugins } = require("./postcss.config.js");

process.chdir(__dirname);
const pkg = JSON.parse(fs.readFileSync("package.json", "utf8"));
const theme = "Theme/";
const addOns = "Theme/assets/add-ons/";

// the readable files of a version and the minified files they are built into
const files = (version) => [
    [`${theme}Fintage_v${version}.css`, `${theme}Fintage-latest-minified.css`],
    [`${addOns}custom-media-covers-v${version}.css`, `${addOns}custom-media-covers-latest-min.css`],
    [`${addOns}media-bar-plugin-support-v${version}.css`, `${addOns}media-bar-plugin-support-latest-min.css`],
];

async function build() {
    for (const [from, to] of files(pkg.version)) {
        const result = await postcss(plugins).process(fs.readFileSync(from, "utf8"), { from, to });
        fs.writeFileSync(to, result.css);
        console.log(`${from} -> ${to}`);
    }
}

// the version is in the name of each file, in its header and in the footer of the settings
function newVersion(version) {
    if (!/^\d+\.\d+\.\d+$/.test(version || "")) throw new Error("usage: npm run new-version -- 1.0.1");
    const current = files(pkg.version);
    files(version).forEach(([to], i) => {
        const [from] = current[i];
        fs.writeFileSync(to, fs.readFileSync(from, "utf8").split(`v${pkg.version}`).join(`v${version}`));
        console.log(`${from} -> ${to}`);
    });
    pkg.version = version;
    fs.writeFileSync("package.json", JSON.stringify(pkg, null, 4) + "\n");
}

if (process.argv[2] === "new") newVersion(process.argv[3]);
else build();
