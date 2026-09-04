<div align="center">
  <img width="250" height="250" alt="icon2" src="https://github.com/user-attachments/assets/2d3a455a-4821-4be2-872b-9a685e03b55a" />
  
  ## pissgrind

  A private music streaming app powered by [Navidrome](https://navidrome.org).

[![GitHub release](https://img.shields.io/github/v/release/skit1312/pissgrind)](https://github.com/skit1312/pissgrind/releases)

</div>

---

## About

pissgrind is a fork of [Chora](https://github.com/CraftWorksMC/Chora) by CraftWorksMC, heavily modified and rebranded for private use. It connects to a self hosted Navidrome server and provides a streamlined Navidrome music streaming experience with a custom theme and many new features. Currently there are over 10'000 songs from 1000 albums by 550+ artists available to stream for free.

**Disclaimer / Content Warning**
The pissgrind music library contains Music / Album Art, that features / displays violent imagery, topics and / or gore, that might be disturbing to certain individiuals. 

---

### Changes / New Features 
- **Library System:** Save favorite songs, albums and artists locally (replaces Navidrome star / unstar)
- **Music Request Screen:** Request songs and albums directly in the app
- **In-app Registration:** Register with email, admin approval workflow
- **Update Checker:** Automatic update notifications via GitHub Releases API
- **What's New dialog:** Shows changelog after updating
- **Add to Favourites Button:** Added a "Add to Favourites" Option in all option Dialgoues(creates a "Favourites" Playlist)
- **Swipe to change Song:** Swipe horizontally to change to the next/previous song in NowPlaying
- **Recommendation Engine(Algorirhm):** Train your own Recommendation Algorithm, by giving it Feedback. Logs for the Recommendation Engine, can be downloaded to see how the Algorithm makes it's recommendations. Training data is synced to user account, can be wiped
- **Swipe Gestures:** Swipe to Queue and Swipe to Delete Gestures added in song lazy lists, and for items in the Library
- **Custom Playlist Covers:** Added ability to choose an image from device storage as a cover image for any playlist
- **Custom theme:** Dark Theme(PissGreen: #70dd00 and PissBlack: #000000) / Light Theme(PissWhite: #ffffff and PissBlack: #000000) throughout the entire UI
- **Simplified Navigation:** Bottom nav reduced to Home, Search, Library, and Request
- **Sharp UI:** Consistent 2.dp corner radius across all UI elements (cards, buttons, chips, sheets)
- **Redesigned Now Playing:** Thinner progress bar, smaller thumb, white color scheme
- **Album Art:** Full cover display without blur or fading overlay, sharp corners
- **Unified Library Screen:** Albums, artists, songs and playlists in a single view with filter chips and search
- **Genre Browse Screen:** Browse all available genres on the pissgrind library in a new "Browse Genres" screen
- **Hero Pager:** A swipeable Hero Pager shows the recently added Albums on the Home Screen
- **Train:** A train button starts a randomly shuffled playlist where you can train the Recommendation Engine with Thumbs Up / Thumbs Down
- **Train & Discover:** Added a "Train & Discover" button that opens an overlay where you can swipe left / rigth(or skip) to train the Recommendation Engine and/or simply discover new songs. Save any song by swiping up to see all available options like save to playlist, add to library, etc.
- **Swipeable Button**: Added a swipeable button on Home Screen that lets you Shuffle All songs randomly, play "My Mix", a playlist generated from the feedback you give to the Recommendation Enginel, or play a "Genre Mix" that generates a random playlist from any selected genre. Genres can be selected by a long-press on the button.
- **Recommended Layzy Row:** Added a "Recommended" lazy row on Home Screen, that lets you see all recommended albums based on your feedback
- **RecommendationsScreen:** Added a seperate RecommendationsScreen, that can be accessed by clicking the "See all ->" button above the Recommended Lazy Row on the Home Screen
- **Shuffle Settings:** Added a new "Shuffle" settings tab in the settings menu
- **Algorithm Settings:** Added a new "Algorithm" settings tab in the settings menu
- **Account Settings:** Added a new "Account" settings tab in the settings menu
- **Share Button:** Added a new share button in the settings menu, that displays a QR-Code[www.pissgrind.ch] for sharing the app
- **Clickable Artist / Song Names:** All song titles and artist names are clickable in the NowPlayingScreen, and lead to either the album by clicking on the song title or the artists page by clicking on the artists name
- **NowPlaying Background:** New Now Playing Background, adapts to Light / Dark Theme

### Removed
- Navidrome star / unstar functionality (replaced by local Library)
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

## User Privacy & Rights(CH and EU)
All user data is stored on a VPS Server provided by [Infomaniak, CH](https://infomaniak.com). All user data is stored in Switzerland and ist not transfered outside of Switzerland in any way by the Server and it's associated Services(exceptions apply for users who access pissgrind services from outside Switzerland). Data protection and privacy laws apply accordingly. For users in Switzerland see the [Federal Act of Data Protection, FADP](https://www.fedlex.admin.ch/eli/cc/2022/491/en) for more information or lodge a complaint with the [FDPIC](https://www.edoeb.admin.ch/en). Users in the European Union can find more information about their rights regarding personal data [here](https://eur-lex.europa.eu/EN/legal-content/summary/general-data-protection-regulation-gdpr.html) and can lodge a complaint with their local data protection authority. You can search for your local data protection authority [here](https://www.edpb.europa.eu/about-edpb/our-members_en) if you are unsure about which one is responsible for you.

---

## Credits

Fork of [Chora](https://github.com/CraftWorksMC/Chora) by CraftWorksMC.

## License

Apache-2.0
