<div align="center">
  <img width="150" height="150" alt="Image" src="https://github.com/user-attachments/assets/46ded284-ea52-474d-8006-6bbb183f5d7c" />
  
  # pissgrind
  
  A private music streaming app powered by Navidrome.
  
  [![GitHub release](https://img.shields.io/github/v/release/skit1312/pissgrind)](https://github.com/skit1312/pissgrind/releases)
</div>

---

## Features

- Stream the pissgrind.ch music library for free
- In-app registration with email approval system
- Request songs and albums directly in the app
- Custom dark theme (PissGreen #99ff33 + PissBlack #000000)
- Secure authentication with admin approval workflow

## Installation

1. Download the latest APK from [Releases](https://github.com/skit1312/pissgrind/releases)
2. Install on your Android device
3. Register an account and wait for admin approval
4. Login and start streaming music

## Screenshots

<div align="center">
  <table>
    <tr>
<img width="350" height="900" alt="Image" src="https://github.com/user-attachments/assets/16a0ce88-421d-4169-9049-861068dc5dea" />

<img width="350" height="900" alt="Image" src="https://github.com/user-attachments/assets/2d9a8483-1aa6-4774-842c-7c5fa578eac2" />

<img width="350" height="900" alt="Image" src="https://github.com/user-attachments/assets/d837ce11-7261-4302-a1b9-d740c8359182" />

<img width="350" height="900" alt="Image" src="https://github.com/user-attachments/assets/e891f03a-6f40-4de6-b62b-fbee6c0e7d8e" />
    </tr>
  </table>
</div>


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
