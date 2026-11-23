# Contributing to Fintage

Thank you for your interest in contributing to **Fintage**, a theme for the Jellyfin web client. To keep it clean, easy to maintain and working on every device, please follow these guidelines.

---

## 🧩 Pull Request Guidelines

- **Keep PRs focused.** Each pull request should address a single feature, bugfix, or improvement. Avoid bundling unrelated changes.

- **Write clear PR descriptions.** Explain what you changed and why. This helps reviewers and future contributors understand the purpose and reasoning behind the change.

- **Provide screenshots.** Include before/after screenshots for any visual changes, along with a short description of what each image shows.

- **Comment your code.** If you’re adding new CSS rules or modifying complex ones, include inline comments where needed to describe intent.

- **Don't forget responsiveness.** Fintage runs on phones, tablets, TVs, and desktops. Ensure your changes are tested across viewports and don't break layouts or interactions on other platforms.

- **Use consistent units.** Follow the theme’s existing use of relative units such as `em` instead of absolute units like `px`.

- **Avoid `!important`.** Only use it when it is absolutely necessary and no alternative exists.

- **Minimize media queries.** Only add new media queries if there is no simpler or existing solution that achieves the same result.

---

## 💬 Propose First for Major Changes

If you are planning to:
- change the overall layout or structure of key components,
- modify the core color scheme,
- or introduce new UI behavioral patterns,

Please open a GitHub issue first to discuss the proposal before submitting a PR. This helps avoid duplicate work and keeps Fintage consistent.

---

## 📁 Files and Releases

- The theme is written in the files in `src/`. `src/fintage.css` lists them in the order in which the browser reads them and says what each one holds; the add-ons are in `src/add-ons/`. Changes go into these files.
- The order matters: of two rules that are equally specific, the later one wins. A rule goes into the file of the part of Jellyfin it styles, and the colors, surfaces and motion of the look go into `src/look/`, which comes last.
- Run `npm ci` once, then `npm run build`. This joins the files into the minified files that the imports load: `Theme/Fintage-latest-minified.css` and the `-latest-min.css` files of the add-ons, and builds `Theme/assets/add-ons/fintage-scripts-latest.js`, the file with all optional scripts that the loader loads. Commit them together with the changed source files.
- The version is the one in `package.json`; the build writes it into the header of each file and into the footer of the settings. To start a new version, change it there, e.g. with `npm version 1.5.1 --no-git-tag-version`. Every release is tagged, e.g. `v1.5.0`, so the files of each version stay available.
- The optional scripts are `Theme/assets/add-ons/*.js`. A new one goes into the list of scripts in `build.js`, so that the loader brings it too.
- The `ElegantFin-*` files and the other dated versions of the add-ons are the releases of ElegantFin that Fintage is based on, and they stay as they are.
- Follow the existing CSS naming conventions and structure, and group related styles with a comment that says what they are for.

---

## 🤝 Communication

- Be respectful, constructive, and collaborative.
- Be open to feedback and willing to revise your work if needed.

---

We appreciate your help in making Fintage better for everyone. Thanks for contributing! 🙌
