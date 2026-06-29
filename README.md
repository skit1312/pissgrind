<div align="center">
  <img src="app/src/main/res/drawable/pissgrind_logo.png" width="150" />
  
  # pissgrind
  
  A private music streaming app powered by Navidrome.
  
  [![GitHub release](https://img.shields.io/github/v/release/skit1312/pissgrind)](https://github.com/skit1312/pissgrind/releases)
</div>

---

## Features

- Stream the pissgrind.ch music library for free
- In-app registration with email approval system
- Request songs and albums directly from the app
- Custom dark theme (PissGreen #99ff33 + PissBlack #000000)
- Secure authentication with admin approval workflow

## Installation

1. Download the latest APK from [Releases](https://github.com/skit1312/pissgrind/releases)
2. Install on your Android device
3. Register an account and wait for admin approval
4. Login and enjoy your music!

## Screenshots



## Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Backend:** Navidrome (Subsonic API)
- **Registration:** Custom FastAPI backend
- **Hosting:** Infomaniak VPS Cloud(CH) (Arch Linux)

## Self-Hosting

To run your own instance you need:
- [Navidrome](https://www.navidrome.org/) music server
- Registration backend (FastAPI + SMTP)
- Reverse proxy (Cloudflared / Nginx)

## Credits

This app is a fork of [Chora](https://github.com/CraftWorksMC/Chora) by CraftWorksMC.
Modified and rebranded for private use.

## License

Apache-2.0 license
