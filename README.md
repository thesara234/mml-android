# MML Android app (Kotlin, Jetpack Compose)

Android app for Melie Mei Langi (MML) customers. Customers can sign in, check rates and fees, send money or shopping, choose internet banking or cash, see their collection code, view their activity and chat with MML staff.

The app connects to the MML web application's API (`/api/v1`), so the web application must be running first.

## Features

- Sign in and profile
- Home screen with new-message alert
- Send money (rate and fee preview, internet banking or cash)
- Receive (collection code)
- Activity (transfer history and status)
- Chat with MML staff

## Requirements

- Android Studio (Koala or newer)
- Android device or emulator, Android 7.0 (API 24) or newer
- The MML web application running on your computer (see the `mml-web` repository)

## Setup

1. In Android Studio, choose **File › Open** and select the `mml-android` folder. Wait for Gradle sync to finish. Don't accept prompts to upgrade the Gradle plugin or libraries.
2. Set the server address in `app/build.gradle.kts` (`API_BASE_URL`):
   - Emulator: `http://10.0.2.2:8000/api/v1/`
   - Real phone: your computer's IP address on the same Wi-Fi, for example `http://192.168.1.50:8000/api/v1/`
3. Start the web application with `php artisan serve --host=0.0.0.0 --port=8000`.
4. Create an emulator in **Tools › Device Manager** (for example Pixel 7, API 34), or connect a phone with USB debugging turned on.
5. Click **Run**.

If the screen stays blank after the splash screen, set the emulator's **Graphics** option to **Software** in Device Manager, then cold-boot it.

## Build notes

- Release builds only allow HTTPS. Set `API_BASE_URL` to the live address before building a release.
