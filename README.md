<div align="center">

# 🎞️ Fintage

**A calm, cinematic theme for Jellyfin on phones, desktops and TVs, with a single import.**

<br>

<img src="Previews/fintage-hero.webp" width="100%" alt="Fintage on phones: the home screen, the page of The Super Mario Galaxy Movie and the page of Avatar: Seven Havens">

</div>

<hr>

### ✨ What Fintage brings

- a dark, cinematic look in desert tones: almost black with a faint film grain, glass panels with hairline edges, soft layered shadows, white play buttons and a restrained champagne accent
- a soft, warm light mode in sand tones with espresso and bronze accents, where logos keep their own colors
- slow, smooth motion: cards lift and catch a soft sheen
- made for Jellyfin 12.2: the new layout (`Auto`) as well as the `Desktop (Legacy)`, `Mobile (Legacy)` and `TV` display modes
- a frosted app bar like milk glass, with a clean bottom edge on every page
- soft, rounded icons everywhere, also in the app bar, the menus and the sidebar of the new layout
- movies, shows, collections and episodes show their image whole in a card with rounded corners and a soft glow in its colors, instead of across the top of the page
- on phones, the card sits below the app bar: below it, the logo is the title of the page, and small in its corner, the logo of the series marks an episode
- on desktops, the card almost fills the screen, with the logo, the details and the buttons on a tinted, frosted area in its lower left corner and the plot below it; episodes carry the small logo of the series above their title
- seasons show their own poster instead of the backdrop that all seasons of a show share: the poster is the card of the page and its colors wash over the top of the page, on phones above the title and on desktops next to the title, the buttons and the plot, with the name of the show as a small heading above the title of the season
- the episodes of a season as a clear list on every device, with 16:9 images and the first three lines of the plot, and on TVs about five episodes on the screen at once
- music on phones in the colors of its covers: albums and playlists show their cover as a card whose colors wash over the top of the page, their tracks as a calm list with the length of each track, and the albums of the library carry their title and artist below the cover
- a now playing screen on phones in a quiet color of the current cover, with the cover glowing in front of it and stepping back while the music is paused, and a small player that floats above the page as a bar of glass, in which the cover glows softly, with a thin gold line for the progress
- audio and subtitles as cards of their own below the plot, so the chosen tracks are clear at a glance
- the directors, writers, studios and genres at the very end of the page of a movie, show, season or episode, like the credits at the end of a film
- a compact sign-in card in front of the splash screen of the server, also on phones
- the results of the searches for subtitles and lyrics as glass cards under the names of their sources, with quiet pills and a round gold download button, which the search for images gets too
- a straight TV layout for series, seasons and episodes
- the Upcoming tab of TV shows as a timeline of release days, with the episodes of each day as a compact list
- smoother scrolling and animations, especially on older devices
- a light load: the fonts come with the theme, and the rounded icons from a small file of about 120 KB instead of the 5 MB of all icons
- optional scripts for episode backdrops and season posters in full resolution, short track names like `German · EAC3` and episode titles in two lines, all with one paste that keeps them up to date, and with them a faster version of the theme

<hr>

### 👇 Installation

Paste this line into the Custom CSS code box:

```css
@import url("https://cdn.jsdelivr.net/gh/ricolehn/fintage@main/Theme/Fintage-latest-minified.css");
```

The link updates by itself. jsDelivr keeps `@main` in its cache for up to 12 hours, so a new version can take a while to show up. To get it right away, open `https://purge.jsdelivr.net/gh/ricolehn/fintage@main/Theme/Fintage-latest-minified.css` once, or replace `@main` with the full id of the newest commit. A link with the id of a commit loads exactly that state right away, but it does not update by itself.

<details>
  <summary><i>For everyone on the server (Dashboard)</i></summary>

> 1. Open the Dashboard from the Administration section of the settings.
> 2. Open Branding in the side bar.
> 3. Paste the line into the Custom CSS code box.
> 4. Click Save and reload the page.

</details>

<details>
  <summary><i>Only for you (User Settings)</i></summary>

> 1. Open Display in your settings.
> 2. Paste the line into the Custom CSS code box.
> 3. Click Save and reload the page.

</details>

**Note:** Jellyfin 12 does not apply custom CSS to the Dashboard, so the Dashboard always keeps the stock Jellyfin look.

<hr>

### 🧩 Options

Each option goes into the Custom CSS code box, after the import. To undo one, remove it again.

<details>
  <summary><i>1. Light and Dark</i></summary>

> - Fintage is dark by default and also comes with a light mode in the same desert tones.
> - To switch, go to `User Settings > Display > Theme` and select **Dark** or **Light**. Jellyfin does not switch between them by itself.
> - This setting is not the same as the theme of the Dashboard, which Fintage does not change.

</details>

<details>
  <summary><i>2. App bar styles and buttons</i></summary>

> - There are four styles: frosted (the default), solid, seamless and fully transparent.
> - Frosted needs nothing: the content glows through the bar like through milk glass, and the bar keeps a clean bottom edge on every page.
> - Solid (fully opaque):
>   ```css
>   :root {
>       --appBarBlurStrength: none;
>       --appBarColor: var(--solidHeaderColor);
>   }
>   ```
> - Seamless (fades out at the bottom):
>   ```css
>   :root {
>       --appBarSeamlessBlur: initial;
>       --appBarBlurStrength: initial;
>       --appBarColor: initial;
>       --appBarBorder: initial;
>       --appBarShadow: initial;
>       --appBarTabsPadding: initial;
>   }
>   ```
> - Fully transparent:
>   ```css
>   :root {
>       --appBarSeamlessBlur: none;
>       --appBarBlurStrength: none;
>       --appBarColor: transparent;
>       --appBarBorder: none;
>       --appBarShadow: none;
>   }
>   ```
> - No SyncPlay button, e.g. if nobody watches together:
>   ```css
>   .headerSyncButton,
>   .MuiIconButton-root:has([data-testid="GroupsIcon"]) {
>       display: none;
>   }
>   ```
> - No Cast button, for playing on other devices:
>   ```css
>   .headerCastButton,
>   .MuiIconButton-root:has([data-testid="CastIcon"], [data-testid="CastConnectedIcon"]) {
>       display: none;
>   }
>   ```
> - Both work in every display mode and in every language.

</details>

<details>
  <summary><i>3. Extra buttons on cards (desktop)</i></summary>

> - Shows the Mark Played and Add to Favorites buttons at the bottom right of a card when the mouse is over it:
>   ```css
>   :root {
>       --extraCardButtonsVisibility: block;
>   }
>   ```
> - To also hide the three-dot button next to them, add `--menuButtonVisibility: none;` to the same block.

</details>

<details>
  <summary><i>4. Play button in the middle of cards (desktop)</i></summary>

> ```css
> :root {
>     --overlayPlayButtonPosition: 50%;
> }
> ```
> - The default is `2.8em`.

</details>

<details>
  <summary><i>5. No sheen on cards (desktop)</i></summary>

> - Turns off the sheen on a card when the mouse is over it:
>   ```css
>   :root {
>       --cardHoverEffect: none;
>   }
>   ```

</details>

<details>
  <summary><i>6. Labels below the library cards</i></summary>

> ```css
> :root {
>     --libraryLabelVisibility: block;
> }
> ```

</details>

<details>
  <summary><i>7. Bigger Continue Watching cards</i></summary>

> - Any value above `1` makes them bigger:
>   ```css
>   :root {
>       --resumeCardScale: 1.25;
>   }
>   ```

</details>

<details>
  <summary><i>8. Titles and logos on the pages of movies, shows and artists</i></summary>

> - These are the defaults; change only what you need:
>   ```css
>   :root {
>       /* movies and shows */
>       --itemOriginalTitleVisibility: block; /* block: show the original title if there is one; none: always hide it */
>       --clearLogoVisibility: block; /* block: show the logo if there is one; none: always hide it */
>       --itemTitleVisibility: none; /* none: hide the title when there is a logo; block: always show it */
>
>       /* music */
>       --artistClearLogoVisibility: block; /* block: show the logo of the artist if there is one; none: always hide it */
>       --artistTitleVisibility: none; /* none: hide the name of the artist; block: always show it */
>       --artistPageBackground: unset; /* unset: dim the background so the logo stands out on desktops; none: show it fully */
>   }
>   ```

</details>

<details>
  <summary><i>9. Small elements on phones</i></summary>

> - These are the defaults; change only what you need:
>   ```css
>   :root {
>       --miniOverlayButtonVisibility: block; /* block: show the small play or three-dot button on cards; none: hide them */
>       --endsAtVisibility: block; /* block: show when a video ends; none: hide it */
>       --criticsRatingVisibility: none; /* none: hide the critics rating; block: show it */
>   }
>   ```

</details>

<details>
  <summary><i>10. Corners and borders (advanced)</i></summary>

> - Other values can break the layout on some screens, so change them only if you are comfortable with CSS. These are the defaults:
>   ```css
>   :root {
>       /* corner radius */
>       --largerRadius: 1.25em;
>       --largeRadius: 1em;
>       --smallRadius: 0.5em;
>       --smallerRadius: 0.375em;
>
>       /* border thickness */
>       --borderWidth: 0.06em;
>       --borderWidthDouble: 0.12em;
>   }
>   ```

</details>

<details>
  <summary><i>11. IMDb colors for the star rating</i></summary>

> - The star uses the TMDB colors by default, since the rating comes from there. For the gold IMDb colors:
>   ```css
>   .starIcon {
>       --colors: var(--imdb);
>   }
>   ```

</details>

<details>
  <summary><i>12. Title of the login page</i></summary>

> - The title above the form is `Sign in to continue`. To change it:
>   ```css
>   :root {
>       --loginPageText: "Your text";
>   }
>   ```
> - The login page shows the splash screen of the server behind the form by itself.

</details>

<details>
  <summary><i>13. Custom covers for the media libraries (add-on)</i></summary>

> - Gives the libraries on the home screen colored covers; read more in [custom-media-covers.md](custom-media-covers.md).
>   ```css
>   @import url("https://cdn.jsdelivr.net/gh/ricolehn/fintage@main/Theme/assets/add-ons/custom-media-covers-latest-min.css");
>   ```

</details>

<details>
  <summary><i>14. Media Bar plugin (add-on)</i></summary>

> - Styles the [Media Bar plugin](https://github.com/IAmParadox27/jellyfin-plugin-media-bar) to match Fintage. Add it right after the main import:
>   ```css
>   @import url("https://cdn.jsdelivr.net/gh/ricolehn/fintage@main/Theme/assets/add-ons/media-bar-plugin-support-latest-min.css");
>   ```

</details>

<details>
  <summary><i>15. All optional scripts with one paste, always up to date (optional JavaScript)</i></summary>

> - Some things are beyond custom CSS, so Fintage comes with a few optional scripts, described in options 16 to 19. This loader brings all of them at once, in their newest version and on every device: you paste it once, and new and improved scripts arrive by themselves, the way the import does it for the theme.
> - It needs a plugin that adds JavaScript to the web client, such as [JavaScript Injector](https://github.com/n00bcodr/Jellyfin-JavaScript-Injector):
>   1. Open Dashboard > Plugins > Catalog, click the settings icon and add the repository of the plugin for your Jellyfin version: `https://raw.githubusercontent.com/n00bcodr/jellyfin-plugins/main/12/manifest.json` for Jellyfin 12, `https://raw.githubusercontent.com/n00bcodr/jellyfin-plugins/main/10.11/manifest.json` for Jellyfin 10.11.
>   2. Install JavaScript Injector from the Catalog and restart your Jellyfin server.
>   3. Open Dashboard > Plugins > JavaScript Injector, click Add Script, paste the content of [fintage-scripts.js](Theme/assets/add-ons/fintage-scripts.js) and save.
>   4. Reload the web page once without the cache: <kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>R</kbd> (<kbd>Cmd</kbd>+<kbd>Shift</kbd>+<kbd>R</kbd> on a Mac). In the Jellyfin apps, clear the cache of the app instead (on Android: Settings > Apps > Jellyfin > Storage > Clear cache). A normal reload is not enough, since the page from before the plugin was installed stays in the cache.
> - The browser console (<kbd>F12</kbd>) then shows the version of the scripts, like `Fintage: scripts v1.1.1 are loaded`, and a line for each script.
> - To leave out a script, name it in the loader, like `window.fintageScriptsOff = ["short-track-names"];`. The names are those of the files in options 16 to 19.
> - Each device fetches the scripts again once a day. jsDelivr, which delivers them, can keep an older version for up to 12 hours.
> - The loader runs whatever is on the main branch of this repository, on every device that opens Jellyfin. To stay on a version you have looked at, replace `@main` in it with a release, like `@v1.1.1`.
> - If you added single scripts of options 16 to 18 before, remove them: older copies of them would run a second time next to the loader.
> - With the loader, you can also use the faster version of the theme, see option 19.

</details>

<details>
  <summary><i>16. Episode images and season posters in full resolution (optional JavaScript)</i></summary>

> - On the page of an episode, Fintage shows the image of the episode as the backdrop, and on the page of a season, the poster of the season as the card of the page. Jellyfin only loads these images at a quarter of the window width, so they look soft when they are enlarged, and custom CSS cannot change the size Jellyfin asks for.
> - This script loads the image of an episode in the resolution of your screen instead, and the poster of a season as large as it is shown, in the pixels of your screen. It only changes the pages of episodes and seasons, and the TV layout keeps the small images.
> - It is part of the loader of option 15. To use it on its own instead, paste the content of [full-resolution-episode-images.js](Theme/assets/add-ons/full-resolution-episode-images.js) into JavaScript Injector, set up as in option 15, and reload the web page once without the cache.
> - The browser console (<kbd>F12</kbd>) then shows `Fintage: full resolution episode images are active`.

</details>

<details>
  <summary><i>17. Short names for audio and subtitle tracks (optional JavaScript)</i></summary>

> - Jellyfin names the tracks like `German - Dolby Digital+ - 5.1 - Default` and `German - Forced - SUBRIP`. These names come from the server, so custom CSS cannot change them.
> - This script shortens them to the language and the codec, like `German · EAC3`, and marks forced subtitles, like `(Forced) German · SRT`. The full name stays as the tooltip, and tracks that would get the same name keep what tells them apart, like the title of a commentary.
> - It is part of the loader of option 15. To use it on its own instead, paste the content of [short-track-names.js](Theme/assets/add-ons/short-track-names.js) into JavaScript Injector, set up as in option 15, and reload the web page once without the cache.
> - The browser console (<kbd>F12</kbd>) then shows `Fintage: short track names are active`.

</details>

<details>
  <summary><i>18. Episode titles in two lines (optional JavaScript)</i></summary>

> - On the page of an episode, Jellyfin writes the season, the number and the name in one line, like `Season 2 - 4. Woe's Hollow`. The number and the name come as one piece of text, so custom CSS cannot split them.
> - This script splits the title into a smaller, slightly darker line with the season and the number, like `Season 2 · Episode 4`, and the name of the episode below it. It uses the word for "Episode" from the language of Jellyfin, like `Staffel 2 · Folge 4` in German.
> - It is part of the loader of option 15. To use it on its own instead, paste the content of [episode-titles.js](Theme/assets/add-ons/episode-titles.js) into JavaScript Injector, set up as in option 15, and reload the web page once without the cache.
> - The browser console (<kbd>F12</kbd>) then shows `Fintage: episode titles are active`.

</details>

<details>
  <summary><i>19. The faster version of the theme (optional JavaScript)</i></summary>

> - Jellyfin does not tell its pages apart by a class, so the theme recognises the page of a movie, a season or an episode by what is on it, and the browser checks this again for every element it styles. With the loader of option 15, a small script answers these questions once whenever the page changes, and the faster version of the theme only reads the answers.
> - It looks exactly like the normal version, and the browser has less than half of the styling work on the pages of movies, shows, seasons, episodes and albums: on a phone with a slow processor about 0.25 instead of 0.6 seconds per page, on a desktop about 45 instead of 140 milliseconds. The script itself needs about 40 of these milliseconds per page on such a phone, and 15 on a desktop.
> - To use it, set up the loader of option 15, and replace the import of the theme with this one:
>   ```css
>   @import url("https://cdn.jsdelivr.net/gh/ricolehn/fintage@main/Theme/Fintage-js-latest-minified.css");
>   ```
> - The script is [page-state.js](Theme/assets/add-ons/page-state.js), and the loader brings it. The browser console (<kbd>F12</kbd>) then also shows `Fintage: page state is active`. Without this script, e.g. when `page-state` is named in `window.fintageScriptsOff`, the pages of movies, shows, seasons, episodes and albums of the faster version lose their card, so leave it on.
> - The plugin loads the scripts once Jellyfin has started, so when the page of a movie or a show is opened directly, e.g. by reloading it, its card can appear a moment after the page. Moving between pages is not affected.

</details>

<hr>

### 🖼️ Previews

<p align="center">
  <img src="Previews/mockups/phones.webp" width="100%" alt="Five phones: the page of a movie with its image whole in a card with a soft glow, an episode with a glow in its colors, the audio and subtitle cards, the Upcoming timeline and the credits at the end of a page">
</p>

<p align="center">
  <img src="Previews/mobile/01-home.webp" width="24%" alt="Home on a phone">
  <img src="Previews/mobile/04-movie.webp" width="24%" alt="Movie on a phone">
  <img src="Previews/mobile/08-series.webp" width="24%" alt="Series on a phone">
  <img src="Previews/mobile/10-episode.webp" width="24%" alt="Episode on a phone">
  <img src="Previews/mobile/07-collection.webp" width="24%" alt="Collection on a phone">
  <img src="Previews/mobile/09-season.webp" width="24%" alt="Season on a phone">
  <img src="Previews/mobile/11-upcoming.webp" width="24%" alt="Upcoming on a phone">
  <img src="Previews/mobile-light/02-movie.webp" width="24%" alt="Movie on a phone in the light mode">
</p>

[![A short demo video of Fintage](Previews/fintage-demo.webp)](Previews/fintage-demo.mp4)

<p align="center">
  <img src="Previews/mockups/devices.webp" width="49%" alt="Fintage on a TV, a laptop and a phone">
  <img src="Previews/mockups/dark-and-light.webp" width="49%" alt="The page of a series on two phones, in the dark and in the light mode">
  <img src="Previews/desktop/01-home.webp" width="49%" alt="Home">
  <img src="Previews/desktop/04-movie.webp" width="49%" alt="Movie">
  <img src="Previews/desktop/09-series.webp" width="49%" alt="Series">
  <img src="Previews/desktop/12-episode.webp" width="49%" alt="Episode">
</p>

<details>
  <summary><i>Phone</i></summary>
<br>

<img src="Previews/mobile/01-home.webp" width="24%" alt="Home"> <img src="Previews/mobile/02-movies.webp" width="24%" alt="Movies"> <img src="Previews/mobile/03-shows.webp" width="24%" alt="Shows"> <img src="Previews/mobile/04-movie.webp" width="24%" alt="Movie">
<img src="Previews/mobile/05-movie-details.webp" width="24%" alt="Details of a movie"> <img src="Previews/mobile/06-movie-credits.webp" width="24%" alt="Credits at the end of a movie"> <img src="Previews/mobile/07-collection.webp" width="24%" alt="Collection"> <img src="Previews/mobile/08-series.webp" width="24%" alt="Series">
<img src="Previews/mobile/09-season.webp" width="24%" alt="Season"> <img src="Previews/mobile/10-episode.webp" width="24%" alt="Episode"> <img src="Previews/mobile/11-upcoming.webp" width="24%" alt="Upcoming"> <img src="Previews/mobile/12-person.webp" width="24%" alt="Person">
<img src="Previews/mobile/13-search.webp" width="24%" alt="Search"> <img src="Previews/mobile/14-user-menu.webp" width="24%" alt="User menu"> <img src="Previews/mobile/15-settings.webp" width="24%" alt="Settings"> <img src="Previews/mobile/16-live-tv-guide.webp" width="24%" alt="Live TV guide">
<img src="Previews/mobile/17-login.webp" width="24%" alt="Login">
<img src="Previews/mobile/18-player.webp" width="98%" alt="Player">

</details>

<details>
  <summary><i>Phone in the light mode</i></summary>
<br>

<img src="Previews/mobile-light/01-home.webp" width="24%" alt="Home"> <img src="Previews/mobile-light/02-movie.webp" width="24%" alt="Movie"> <img src="Previews/mobile-light/03-movie-details.webp" width="24%" alt="Details of a movie"> <img src="Previews/mobile-light/04-series.webp" width="24%" alt="Series">
<img src="Previews/mobile-light/05-episode.webp" width="24%" alt="Episode"> <img src="Previews/mobile-light/06-upcoming.webp" width="24%" alt="Upcoming">

</details>

<details>
  <summary><i>Desktop</i></summary>
<br>

<img src="Previews/desktop/01-home.webp" width="49%" alt="Home"> <img src="Previews/desktop/02-movies.webp" width="49%" alt="Movies">
<img src="Previews/desktop/03-filters.webp" width="49%" alt="Filters"> <img src="Previews/desktop/04-movie.webp" width="49%" alt="Movie">
<img src="Previews/desktop/05-movie-details.webp" width="49%" alt="Details of a movie"> <img src="Previews/desktop/06-metadata-editor.webp" width="49%" alt="Metadata editor">
<img src="Previews/desktop/07-collection.webp" width="49%" alt="Collection"> <img src="Previews/desktop/08-shows.webp" width="49%" alt="Shows">
<img src="Previews/desktop/09-series.webp" width="49%" alt="Series"> <img src="Previews/desktop/10-series-details.webp" width="49%" alt="Details of a series">
<img src="Previews/desktop/11-season.webp" width="49%" alt="Season"> <img src="Previews/desktop/12-episode.webp" width="49%" alt="Episode">
<img src="Previews/desktop/13-upcoming.webp" width="49%" alt="Upcoming"> <img src="Previews/desktop/14-person.webp" width="49%" alt="Person">
<img src="Previews/desktop/15-search.webp" width="49%" alt="Search"> <img src="Previews/desktop/16-player.webp" width="49%" alt="Player">
<img src="Previews/desktop/17-live-tv-guide.webp" width="49%" alt="Live TV guide"> <img src="Previews/desktop/18-live-tv-channels.webp" width="49%" alt="Live TV channels">
<img src="Previews/desktop/19-user-menu.webp" width="49%" alt="User menu"> <img src="Previews/desktop/20-settings.webp" width="49%" alt="Settings">
<img src="Previews/desktop/21-login.webp" width="49%" alt="Login"> <img src="Previews/desktop/22-media-covers-add-on.webp" width="49%" alt="Custom media covers add-on">
<img src="Previews/desktop/23-legacy-layout.webp" width="49%" alt="Desktop (Legacy) layout">

</details>

<details>
  <summary><i>Desktop in the light mode</i></summary>
<br>

<img src="Previews/light/01-home.webp" width="49%" alt="Home"> <img src="Previews/light/02-movies.webp" width="49%" alt="Movies">
<img src="Previews/light/03-movie.webp" width="49%" alt="Movie"> <img src="Previews/light/04-movie-details.webp" width="49%" alt="Details of a movie">
<img src="Previews/light/05-series.webp" width="49%" alt="Series"> <img src="Previews/light/06-season.webp" width="49%" alt="Season">
<img src="Previews/light/07-episode.webp" width="49%" alt="Episode"> <img src="Previews/light/08-upcoming.webp" width="49%" alt="Upcoming">
<img src="Previews/light/09-settings.webp" width="49%" alt="Settings">

</details>

<details>
  <summary><i>TV</i></summary>
<br>

<img src="Previews/tv/01-home.webp" width="49%" alt="Home"> <img src="Previews/tv/02-movies.webp" width="49%" alt="Movies">
<img src="Previews/tv/03-shows.webp" width="49%" alt="Shows"> <img src="Previews/tv/04-movie.webp" width="49%" alt="Movie">
<img src="Previews/tv/05-movie-details.webp" width="49%" alt="Details of a movie"> <img src="Previews/tv/06-series.webp" width="49%" alt="Series">
<img src="Previews/tv/07-season.webp" width="49%" alt="Season"> <img src="Previews/tv/08-episode.webp" width="49%" alt="Episode">
<img src="Previews/tv/09-upcoming.webp" width="49%" alt="Upcoming"> <img src="Previews/tv/10-person.webp" width="49%" alt="Person">
<img src="Previews/tv/11-live-tv-guide.webp" width="49%" alt="Live TV guide"> <img src="Previews/tv/12-live-tv-channels.webp" width="49%" alt="Live TV channels">

</details>

The screenshots show Fintage v1.4.1 on Jellyfin 12.3 with the optional scripts of option 15, and the images at the top and in `Previews/mockups` put real screenshots into frames of devices. The library holds only placeholder files without real video; the titles, the artwork and the information come from [TMDB](https://www.themoviedb.org), the channel logos from [tv-logo/tv-logos](https://github.com/tv-logo/tv-logos) and the TV guide from [open-epg.com](https://www.open-epg.com), and all of them belong to their owners.

<hr>

### 📁 Files

| File | What it is |
| --- | --- |
| `Theme/Fintage-latest-minified.css` | the newest version of Fintage, minified; this is what the import loads |
| `Theme/Fintage-js-latest-minified.css` | the faster version for the optional scripts, see option 19 |
| `src/` | the readable source of Fintage and its add-ons, in files by topic; `src/fintage.css` lists them in order |
| `Theme/assets/add-ons/` | the add-ons (`*-latest-min.css` to import) and the optional scripts (`fintage-scripts.js` is the loader, `fintage-scripts-latest.js` the file it loads, `page-state.js` the script of the faster version, built from the theme) |
| `Theme/assets/fonts/` | the fonts of the theme, Inter and the rounded icons, with their licenses |
| `Theme/ElegantFin-theme-v*.css` and the dated add-ons | the releases of ElegantFin up to v26.09.05, which Fintage is based on |

<hr>

### 🛠️ Troubleshooting

<details>
  <summary><i>Which version am I using?</i></summary>

> - The version is shown at the bottom of the settings, like `Fintage v1.5.5`.

</details>

<details>
  <summary><i>A new version is out, but I still see the old one.</i></summary>

> - jsDelivr can take up to 12 hours to serve a new version; the purge link in Installation skips that wait.
> - Browsers and apps also keep the theme in their cache. In a browser, reload with <kbd>Ctrl</kbd>+<kbd>F5</kbd>. In the apps, sign out and in again, or clear the cache of the app.

</details>

<details>
  <summary><i>Why does Jellyfin Media Player show visual glitches?</i></summary>

> - Jellyfin Media Player uses an old web engine (Qt 5) that lacks many modern CSS features, so it is not supported. [Jellium Desktop](https://github.com/andrewrabert/jellium-desktop) is an unofficial alternative that is worth a try.

</details>

<details>
  <summary><i>Does it work in the Jellyfin app for Android TV?</i></summary>

> - The Android TV app does not support custom CSS (as of version 0.18.11). The Android app for phones does, and the webOS app on LG TVs supports it as well.

</details>

<details>
  <summary><i>The icons on my LG TV are broken.</i></summary>

> - Some webOS TVs cannot show the modern Material icons ([details](https://github.com/lscambo13/ElegantFin/issues/39)). The older icons work there:
>   ```css
>   :root {
>       --iconPack: 'Material Icons';
>   }
>   ```

</details>

<details>
  <summary><i>How do I report a bug?</i></summary>

> - First check whether it comes from a plugin: turn your plugins off, or remove the CSS of their add-ons, and see whether the bug is still there.
> - Then look through the [issues](https://github.com/ricolehn/fintage/issues), and if it is not there yet, [report it](https://github.com/ricolehn/fintage/issues/new/choose) with the version of Fintage, of Jellyfin and your device.

</details>

<hr>

### 🆗 Tested on

- Jellyfin 12.2 and 12.4 in the `Auto`, `Desktop (Legacy)`, `Mobile (Legacy)` and `TV` display modes, with the Dark and the Light theme, in Chromium-based browsers

<hr>

### 📌 Contributing

Pull requests and ideas are welcome. Please read the [Contributor Guidelines](CONTRIBUTING.md) first; they also explain how a new version is built.

<hr>

### 📜 License

Fintage is free software under the [GNU General Public License v2.0](LICENSE), the same license as ElegantFin.

<hr>

### 🍴 Where Fintage comes from

Fintage started as a fork of [ElegantFin](https://github.com/lscambo13/ElegantFin) by [lscambo13](https://github.com/lscambo13), a Jellyfin theme inspired by Jellyseerr. Fintage v1.0.0 builds on ElegantFin v26.09.05, and the structure and most of the code come from there. Many thanks to lscambo13 for this great foundation.
