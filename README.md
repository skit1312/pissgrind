<div align="center">
  
  # pissgrind

  A private music streaming app powered by Navidrome.

[![GitHub release](https://img.shields.io/github/v/release/skit1312/pissgrind)](https://github.com/skit1312/pissgrind/releases)

</div>

---

## About

pissgrind is a fork of [Chora](https://github.com/CraftWorksMC/Chora) by CraftWorksMC, heavily modified and rebranded for private use. It connects to a self hosted Navidrome server and provides a streamlined Navidrome music streaming experience with a custom theme and many new features.

---

### Changes / New Features 
- **Library system:** Save favorite songs, albums and artists locally (replaces Navidrome star/unstar)
- **Music request screen:** Request songs and albums directly in the app
- **In-app registration:** Register with email, admin approval workflow
- **Update checker:** Automatic update notifications via GitHub Releases API
- **What's New dialog:** Shows changelog after updating
- **Add to Favourites Button:** Added a "Add to Favourites" Option in all option Dialgoues(creates a "Favourites" Playlist
- **Swipe to change Song:** Swipe horizontally to change to the next/previous song in NowPlaying
- **Recommendation Engine(Algorirhm):** Train your own Recommendation Algorithm, by giving it Feedback. Logs for the Recommendation Engine, can be downloaded to see how the Algorithm makes it's recommendations. Training data is synced to user account, can be wiped
- **Swipe Gestures:** Swipe to Queue and Swipe to Delete Gestures added in song lazy lists, and for items in the Library
- **Custom Playlist Covers:** Added ability to choose an image from device storage as a cover image for any playlists
- **Custom theme:** Dark Theme(PissGreen: #70dd00 and PissBlack: #000000) / Light Theme(PissWhite: #ffffff and PissBlack: #000000) throughout the entire UI
- **Simplified navigation:** Bottom nav reduced to Home, Search, Library, and Request
- **Sharp UI:** Consistent 2.dp corner radius across all UI elements (cards, buttons, chips, sheets)
- **Redesigned Now Playing:** Thinner progress bar, smaller thumb, white color scheme
- **Album Art:** Full cover display without blur or fading overlay, sharp corners
- **Unified Library Screen:** Albums, artists, songs and playlists in a single view with filter chips and search

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

1. Download the latest APK from [Releases](https://github.com/skit1312/pissgrind/releases)
2. Install on your Android device
3. Register an account and wait for admin approval
4. Login and start streaming

Alternatively, add `https://github.com/skit1312/pissgrind` to [Obtainium](https://github.com/ImranR98/Obtainium) for automatic updates.

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

## Credits

Fork of [Chora](https://github.com/CraftWorksMC/Chora) by CraftWorksMC.

---
## User Privacy & Rights(CH and EU)
All user data is stored on a VPS Server provided by [Infomaniak, CH](https://infomaniak.com). All user data is stored in Switzerland and ist not transfered outside of Switzerland in any way by the Server and it's associated Services(exceptions apply for users who access pissgrind services from outside Switzerland). Data protection and privacy laws apply accordingly. For users in Switzerland see [https://www.fedlex.admin.ch/eli/cc/2022/491/en] for more information or lodge a complaint with the [FDPIC](https://www.edoeb.admin.ch/en). Users in the European Union can find more information about their rights regarding personal data [here](https://eur-lex.europa.eu/EN/legal-content/summary/general-data-protection-regulation-gdpr.html) and can lodge a complaint with their local data protection authority. You can search for your local data protection authority [here](https://www.edpb.europa.eu/about-edpb/our-members%5C_en) if you are unsure about which one is responsible for you.

## License

Apache-2.0
