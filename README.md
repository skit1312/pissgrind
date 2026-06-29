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

<img width="1151" height="2560" alt="Image" src="https://github.com/user-attachments/assets/ccce79d6-c2cd-474e-b13a-ff643ef59ff4" />
<img width="1151" height="2560" alt="Image" src="https://github.com/user-attachments/assets/8c71ce85-b9bb-47dc-b68b-c617f52518b9" />
<img width="1151" height="2560" alt="Image" src="https://github.com/user-attachments/assets/6121197b-dbcf-420c-be3c-5151b117f60f" />
<img width="1151" height="2560" alt="Image" src="https://github.com/user-attachments/assets/bc9abb5f-7029-47e9-8445-51d57b878188" />

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
