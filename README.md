<div align="center">
  <img width="150" height="150" alt="Image" src="https://github.com/user-attachments/assets/46ded284-ea52-474d-8006-6bbb183f5d7c" />
  
  # pissgrind
  
  A private music streaming app powered by Navidrome.
  
  [![GitHub release](https://img.shields.io/github/v/release/skit1312/pissgrind)](https://github.com/skit1312/pissgrind/releases)
</div>

---

## Features

- Stream the pissgrind.ch music library for free -> 600 Albums, with 6700 Songs by 625 Artists
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
      <td><img width="300" alt="Screenshot 1" src="https://github.com/user-attachments/assets/36ecc9b5-c6d4-45eb-8364-f9b7b178fcbc" /></td>
      <td><img width="300" alt="Screenshot 2" src="https://github.com/user-attachments/assets/73011b66-0d71-45e5-bba1-bafc44f44f04" /></td>
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
