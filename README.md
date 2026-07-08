<div align="center">
  
  # pissgrind

  A private music streaming app powered by Navidrome.

[![GitHub release](https://img.shields.io/github/v/release/skit1312/pissgrind)](https://github.com/skit1312/pissgrind/releases)

</div>

---

## About

pissgrind is a fork of [Chora](https://github.com/CraftWorksMC/Chora) by CraftWorksMC, heavily modified and rebranded for private use. It connects to a self-hosted Navidrome server and provides a streamlined music streaming experience with a custom theme.

---

## What's different from Chora?

### Added
- **Library system:** Save favorite songs, albums, and artists locally (replaces Navidrome star/unstar)
- **Music request screen:** Request songs and albums directly in the app
- **In-app registration:** Register with email, admin approval workflow
- **Update checker:** Automatic update notifications via GitHub Releases API
- **What's New dialog:** Shows changelog after updating
- **Recently Added shortcut:** Button on home screen to browse all recently added albums
- **Collection button on mini player:** Quickly save the currently playing song in your library

### Changed
- **Custom theme:** PissGreen (`#99ff33`) + PissBlack (`#000000`) throughout the entire UI
- **Simplified navigation:** Bottom nav reduced to Home, Search, Library, and Request
- **Sharp UI:** Consistent 2.dp corner radius across all UI elements (cards, buttons, chips, sheets)
- **Redesigned Now Playing:** Thinner progress bar, smaller thumb, PissGreen color scheme
- **Album art:** Full cover display without blur or fading overlay, sharp corners
- **Unified Library screen:** Albums, artists, and songs in a single view with filter chips and search
- **Swipe to remove:** Library items removed via swipe gesture with animated visibility

### Removed
- Navidrome star/unstar functionality (replaced by local Library)
- Navbar item customization
- Home items configuration
- "Show Navidrome logo" toggle
- "Show provider dividers" toggle
- "More song info" toggle
- Multiple obsolete code sections

---

## Installation

1. Download the latest APK from [Releases](https://github.com/skit1312/pissgrind/-/releases)
2. Install on your Android device
3. Register an account and wait for admin approval
4. Login and start streaming

Alternatively, add `https://github.com/skit1312/pissgrind` to [Obtainium](https://github.com/ImranR98/Obtainium) for automatic updates.

---

## Screenshots

<div align="center">
  <table>
    <tr>
      <td><img width="250" alt="Home" src="screenshots/home.png" /></td>
      <td><img width="250" alt="Now Playing" src="screenshots/nowplaying.png" /></td>
      <td><img width="250" alt="Library" src="screenshots/library.png" /></td>
    </tr>
  </table>
</div>

---

## Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Backend | Navidrome (Subsonic API) |
| Registration | Custom FastAPI backend |
| Hosting | Infomaniak VPS Cloud (CH), Arch Linux |
| Networking | Ktor |
| DI | Hilt |
| Local DB | Room |

---

## Self-Hosting

To run your own instance you need:
- [Navidrome](https://www.navidrome.org/) music server
- Registration backend (FastAPI + SMTP)
- Reverse proxy (Cloudflared / Nginx)

---

## Credits

Fork of [Chora](https://github.com/CraftWorksMC/Chora) by CraftWorksMC.

---

## License

Apache-2.0
