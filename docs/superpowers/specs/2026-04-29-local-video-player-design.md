# Local Video Player Design

## Overview

A local video player Android app built with Jetpack Compose. Users import video files from device storage, browse them in a list, and play them in a fullscreen immersive player. Two tab pages (video list + recent plays) with a non-tab fullscreen player page.

## Tech Stack

| Component | Library |
|-----------|---------|
| UI Framework | Jetpack Compose + Material 3 |
| Navigation | Jetpack Navigation Compose |
| Video Engine | Media3 ExoPlayer |
| Player UI | Media3 UI (PlayerView via AndroidView) |
| Local Storage | Room (SQLite) |
| DI / State | ViewModel + StateFlow |
| Build | Kotlin 2.2.10, AGP 9.2.0, Compose BOM 2026.02.01 |

## Architecture

```
MainActivity (single Activity)
└── VideoplayerApp (Compose root)
    ├── NavHost
    │   ├── videoList (Tab)      — all imported videos
    │   ├── recentPlay (Tab)     — play history with progress
    │   └── player/{videoId}     — fullscreen player (non-Tab)
    └── NavigationBar (bottom)
```

### Data Layer

**Room Entities:**

- `VideoEntity` — id (Long, auto), uri (String), fileName, duration (Long ms), thumbnailPath, addedAt (Long epoch)
- `PlayHistoryEntity` — videoId (FK), position (Long ms), lastPlayedAt (Long epoch)

**Repository:**

- `VideoRepository` — wraps Room DAOs for CRUD on videos and play history

**ViewModels:**

- `VideoViewModel` — manages video list state, import/delete operations
- `PlayerViewModel` — manages ExoPlayer instance, playback state, progress saving

## Pages

### Video List Tab (VideoListScreen)

- Top bar with title and import button
- Grid layout (2-3 columns) of video cards
- Each card: thumbnail, file name, duration
- Tap card → navigate to player
- Long press card → context menu (delete, rename)

### Recent Plays Tab (RecentPlayScreen)

- List layout, one item per row
- Each item: thumbnail, file name, progress bar, last played time
- Sorted by lastPlayedAt descending
- Tap → navigate to player, resume from saved position

### Player Page (PlayerScreen)

- Fullscreen immersive (hide status bar + navigation bar)
- Media3 `PlayerView` embedded via Compose `AndroidView`
- Gestures:
  - Horizontal swipe: seek forward/backward
  - Left vertical swipe: brightness
  - Right vertical swipe: volume
- Control bar: auto-hide after 3s idle, tap screen to show
- Controls: progress bar, play/pause, fast-forward/rewind, lock button
- Exit: save progress to Room, release ExoPlayer

## Video Import Flow

1. User taps import button
2. Launch `ActivityResultContracts.OpenMultipleDocuments()` with `video/*` mime filter
3. For each selected URI:
   - Call `takePersistableUriPermission()` for persistent access
   - Use `MediaMetadataRetriever` to extract duration
   - Generate thumbnail, save to `filesDir/thumbnails/`
   - Create `VideoEntity` and insert into Room
4. Refresh video list via StateFlow

## Player Lifecycle

| Event | Action |
|-------|--------|
| Enter player page | Create ExoPlayer, set MediaItem from URI, seekTo saved position if exists |
| During playback | Periodically (every 5s) update Room with current position |
| Exit player page | Save final position, call `player.release()` in `DisposableEffect.onDispose` |
| Screen rotation | Save position via `rememberSaveable`, restore after config change |

## Permissions

- No runtime permissions needed — using SAF (Storage Access Framework) document picker
- `android.permission.FOREGROUND_SERVICE` if background playback added later

## File Structure

```
app/src/main/java/com/example/videoplayer/
├── MainActivity.kt
├── VideoplayerApp.kt              — NavHost + NavigationBar setup
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── VideoEntity.kt
│   │   └── PlayHistoryEntity.kt
│   └── VideoRepository.kt
├── ui/
│   ├── navigation/
│   │   └── NavGraph.kt
│   ├── screens/
│   │   ├── VideoListScreen.kt
│   │   ├── RecentPlayScreen.kt
│   │   └── PlayerScreen.kt
│   ├── components/
│   │   ├── VideoCard.kt
│   │   └── RecentPlayItem.kt
│   ├── viewmodel/
│   │   ├── VideoViewModel.kt
│   │   └── PlayerViewModel.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── util/
    └── VideoMetadataExtractor.kt
```

## Out of Scope (Future)

- Picture-in-Picture mode
- Subtitle support
- Streaming / network playback
- Background audio playback
- Playlist management
