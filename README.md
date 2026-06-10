# Wallwidgy

Wallwidgy is a modern, high-performance Android wallpaper application built entirely in Kotlin and Jetpack Compose. It features a sleek Material Design 3 interface, responsive staggered grids, offline favorites, and a built-in wallpaper customization suite.

You can download the latest pre-compiled APKs from the [Releases](https://github.com/not-ayan/Wallwidgy_Android/releases) page.

## Features

- **Staggered Grid View**: A beautiful, fluid grid layout featuring dual-column dynamic spacing with native loading skeletons.
- **Search & Advanced Filtering**: Search wallpapers by name or filter them by device orientation (Mobile vs. Desktop) and category tags.
- **In-App Wallpaper Editor**: Custom editor component to edit wallpapers before applying them:
  - **Color Matrix Adjustments**: Intuitive sliders for brightness, contrast, and saturation.
  - **Custom Crop Overlay**: Interactive gesture-driven crop tool with L-shaped corner handles.
- **System Integration**: Directly apply customized wallpapers to the Home Screen, Lock Screen, or both, or export them to the system picker.
- **Local Favorites**: Simple, offline-first favorite system to save wallpapers to local storage.
- **Device Downloads**: Clean integration with `DownloadManager` for backgrounds and galleries.

## Tech Stack

- **UI Framework**: Jetpack Compose (Material Design 3)
- **Programming Language**: Kotlin
- **Asynchronous Execution**: Kotlin Coroutines & Flow
- **Image Loading**: Coil (with hardware acceleration settings)
- **Asset Processing**: Android Canvas & ColorMatrixColorFilter
- **Storage**: Android Room / SharedPreferences & MediaStore API

## Architecture & Structure

The codebase is organized following standard Android MVVM principles:

- `/app/src/main/java/.../ui/screens`: Contains core application screens (`HomeScreen`, `WallpaperDetailScreen`, `FavoritesScreen`, `AboutScreen`).
- `/app/src/main/java/.../ui/components`: Reusable UI components including the custom `WallpaperEditor` and `WheelSlider`.
- `/app/src/main/java/.../ui/theme`: System typography, palettes, colors, and dynamic theme settings.
- `/app/src/main/java/.../data`: Domain models and local repository structures for handling wallpaper data.

## Getting Started

### Prerequisites

- Android Studio Koala+ or equivalent IDE
- JDK 17+
- Android SDK 26 (Android 8.0) or higher

### Build & Run

1. Clone this repository to your local machine.
2. Open the project in Android Studio.
3. Synchronize Gradle files.
4. Run the project on a physical device or emulator using the `app` configurations:
   ```bash
   ./gradlew installDebug
   ```

## Development and Verification

Verify the codebase compiling correctness using the Kotlin compilation task:
```bash
./gradlew :app:compileDebugKotlin
```
