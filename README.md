# Abbay Audiobook Player

Abbay is an Android audiobook player app built with Jetpack Compose and ExoPlayer. It supports single and multi-chapter books, playback speed control, screen lock and more.

## Features

- 📚 Browse and play audiobooks (single or multiple chapters)
- ⏩ Skip forward/backward by 10 seconds
- 🎚️ Change playback speed
- 🔒 Lock the player screen to prevent accidental touches
- 📖 View and select chapters in multi-chapter books
- 🎵 Modern UI with Jetpack Compose

## Screenshots

<!-- Add screenshots here if available -->

## Getting Started

### Prerequisites

- Android Studio (latest recommended)
- Android device or emulator (API 23+)

### Build & Run

1. Clone this repository:
    ```sh
    git clone https://github.com/yourusername/abbay-android.git
    cd abbay-android
    ```

2. Open the project in Android Studio.

3. Click **Run** ▶️ to build and launch the app.

## Project Structure

- `app/src/main/java/com/mobyle/abbay/presentation/booklist/widgets/`
    - `MiniPlayer.kt` – Main player UI and logic
    - `models/` – UI models and enums
- `app/src/main/res/raw/`
    - `motion_scene.json5` – MotionLayout scene for player transitions

## Key Libraries

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [ExoPlayer](https://exoplayer.dev/)
- [Coil](https://coil-kt.github.io/coil/) (for image loading)
- [Media3](https://developer.android.com/guide/topics/media/media3)

## Customization

- **Playback Speed:** Tap the speed icon in the top bar to change.
- **Screen Lock:** Tap the lock icon to lock/unlock the player.
- **Chapters:** Tap the chapter name to view/select chapters.

## Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

## License

[MIT](LICENSE)

---

*Made with ❤️ for audiobooks!*