# Jellyseerr Remote

A native Android app to search and request movies and TV shows from your [Jellyseerr](https://github.com/Fallenbagel/jellyseerr) instance—from your phone, on the same network or remotely.

## What is Jellyseerr?

[Jellyseerr](https://github.com/Fallenbagel/jellyseerr) is a request management tool for media servers (e.g. Plex, Jellyfin). Users search for content and submit requests; Jellyseerr can integrate with *arr stack apps for automation. This app is a lightweight mobile client for that workflow.

## Features

- **Search** — Find movies and TV shows; results show availability and request status.
- **Request** — One-tap request with optional season selection for TV.
- **Local & remote** — Connect via local URL (e.g. `http://192.168.1.50:5055`) or a remote URL (e.g. Cloudflare Tunnel or your own domain).
- **Cloudflare Tunnel** — Optional built-in support for a trycloudflare.com tunnel ID.
- **Secure storage** — Server URL and auth are stored using Android’s EncryptedSharedPreferences.

## Screenshots

**Search** — Results with status (Requested, Available, etc.)

![Search results](screenshots/1-search-results.png)

**Request** — Tap a result to open the sheet and request.

![Request sheet](screenshots/2-request-sheet.png)

**Settings** — Local/remote URL, Cloudflare Tunnel, connection status.

![Settings](screenshots/3-settings.png)

**Login** — Sign in with Jellyseerr email/password.

![Login](screenshots/4-login.png)

## Requirements

- Android 8.0 (API 26) or higher
- A running [Jellyseerr](https://github.com/Fallenbagel/jellyseerr) instance
- For remote access: a tunnel (e.g. Cloudflare) or a publicly reachable Jellyseerr URL

## Build and run

1. Clone the repo:
   ```bash
   git clone https://github.com/YOUR_USERNAME/JellyseerrRemote.git
   cd JellyseerrRemote
   ```
2. Open the project in [Android Studio](https://developer.android.com/studio) (or use the command line below).
3. Build and run:
   - **IDE:** Use **Run** (e.g. Run > Run 'app') with a device or emulator connected.
   - **CLI:** `./gradlew installDebug` (or `gradlew.bat installDebug` on Windows), then launch the app on your device/emulator.

Release build:

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk` (sign with your keystore for distribution).

## Configuration

1. Open **Settings** and set your **Local URL** (e.g. `http://192.168.1.50:5055`).
2. Optionally enable **Remote** and set a Cloudflare Tunnel ID or a custom remote URL.
3. Use **Login** to sign in with your Jellyseerr email/password (enable “Password sign-in” in Jellyseerr → Settings → Users if needed).
4. The status dot (gray / yellow / green / red) shows connection and auth state.

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

**AI disclosure:** This project was written primarily using Cursor.
