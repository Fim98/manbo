# Local Video Player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local video player app with video import, grid/list browsing, and fullscreen immersive playback using Media3 ExoPlayer.

**Architecture:** Single-Activity Compose app with Navigation Compose for routing. Room for local persistence of video metadata and play history. Media3 ExoPlayer for video decoding and playback. ViewModels + StateFlow for state management.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose (BOM 2026.02.01), Media3 ExoPlayer 1.7.1, Room 2.7.1, Navigation Compose 2.9.0, Material 3

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `gradle/libs.versions.toml` | Add dependency version catalog entries |
| Modify | `app/build.gradle.kts` | Add plugin (KSP) and dependency declarations |
| Modify | `app/src/main/AndroidManifest.xml` | Add activities intent (no extra permissions needed) |
| Modify | `app/src/main/java/.../MainActivity.kt` | Wire up VideoplayerApp composable |
| Create | `app/src/main/java/.../data/db/VideoEntity.kt` | Room entity for imported videos |
| Create | `app/src/main/java/.../data/db/PlayHistoryEntity.kt` | Room entity for play history |
| Create | `app/src/main/java/.../data/db/VideoDao.kt` | Room DAO for video + history queries |
| Create | `app/src/main/java/.../data/db/AppDatabase.kt` | Room database singleton |
| Create | `app/src/main/java/.../data/VideoRepository.kt` | Repository wrapping DAOs |
| Create | `app/src/main/java/.../util/VideoMetadataExtractor.kt` | Extract duration/thumbnail from video URI |
| Create | `app/src/main/java/.../ui/viewmodel/VideoViewModel.kt` | Video list state + import/delete logic |
| Create | `app/src/main/java/.../ui/viewmodel/PlayerViewModel.kt` | ExoPlayer lifecycle + progress saving |
| Create | `app/src/main/java/.../ui/components/VideoCard.kt` | Grid card composable (thumbnail, name, duration) |
| Create | `app/src/main/java/.../ui/components/RecentPlayItem.kt` | List row composable (thumbnail, name, progress bar, time) |
| Create | `app/src/main/java/.../ui/screens/VideoListScreen.kt` | Video list tab with grid + import button |
| Create | `app/src/main/java/.../ui/screens/RecentPlayScreen.kt` | Recent plays tab with list |
| Create | `app/src/main/java/.../ui/screens/PlayerScreen.kt` | Fullscreen immersive player with gestures |
| Create | `app/src/main/java/.../ui/navigation/NavGraph.kt` | Navigation routes and NavHost setup |
| Create | `app/src/main/java/.../VideoplayerApp.kt` | Root composable: NavHost + bottom NavigationBar |

Base package: `com.manbo.videoplayer` (path: `app/src/main/java/com/example/videoplayer/`)

---

## Task 1: Add Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add version entries to `gradle/libs.versions.toml`**

Add these entries to the `[versions]` section:

```toml
lifecycleViewmodelCompose = "2.10.0"
navigationCompose = "2.9.0"
media3 = "1.7.1"
room = "2.7.1"
ksp = "2.2.10-1.0.29"
coil = "2.7.0"
```

Add these entries to the `[libraries]` section:

```toml
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
androidx-media3-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "media3" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
```

Add to the `[plugins]` section:

```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Update `app/build.gradle.kts` to declare dependencies**

Replace the entire file with:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.manbo.videoplayer"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.manbo.videoplayer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.coil.compose)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

- [ ] **Step 3: Verify Gradle sync compiles**

Run: `cd /Users/fim98/Documents/videoplayer && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "chore: add Media3, Room, Navigation, Coil dependencies"
```

---

## Task 2: Room Database Layer

**Files:**
- Create: `app/src/main/java/com/example/videoplayer/data/db/VideoEntity.kt`
- Create: `app/src/main/java/com/example/videoplayer/data/db/PlayHistoryEntity.kt`
- Create: `app/src/main/java/com/example/videoplayer/data/db/VideoDao.kt`
- Create: `app/src/main/java/com/example/videoplayer/data/db/AppDatabase.kt`

- [ ] **Step 1: Create `VideoEntity.kt`**

```kotlin
package com.manbo.videoplayer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val fileName: String,
    val duration: Long,
    val thumbnailPath: String,
    val addedAt: Long
)
```

- [ ] **Step 2: Create `PlayHistoryEntity.kt`**

```kotlin
package com.manbo.videoplayer.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "play_history",
    foreignKeys = [ForeignKey(
        entity = VideoEntity::class,
        parentColumns = ["id"],
        childColumns = ["videoId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PlayHistoryEntity(
    @PrimaryKey val videoId: Long,
    val position: Long,
    val lastPlayedAt: Long
)
```

- [ ] **Step 3: Create `VideoDao.kt`**

```kotlin
package com.manbo.videoplayer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY addedAt DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: Long): VideoEntity?

    @Insert
    suspend fun insertVideo(video: VideoEntity): Long

    @Delete
    suspend fun deleteVideo(video: VideoEntity)

    @Query("SELECT * FROM play_history ORDER BY lastPlayedAt DESC")
    fun getAllHistory(): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history WHERE videoId = :videoId")
    suspend fun getHistory(videoId: Long): PlayHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: PlayHistoryEntity)

    @Query("""
        SELECT h.* FROM play_history h
        INNER JOIN videos v ON h.videoId = v.id
        ORDER BY h.lastPlayedAt DESC
    """)
    fun getHistoryWithExistingVideos(): Flow<List<PlayHistoryEntity>>
}
```

- [ ] **Step 4: Create `AppDatabase.kt`**

```kotlin
package com.manbo.videoplayer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VideoEntity::class, PlayHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "videoplayer_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/videoplayer/data/
git commit -m "feat: add Room database layer with VideoEntity, PlayHistoryEntity, DAO"
```

---

## Task 3: Video Repository

**Files:**
- Create: `app/src/main/java/com/example/videoplayer/data/VideoRepository.kt`

- [ ] **Step 1: Create `VideoRepository.kt`**

```kotlin
package com.manbo.videoplayer.data

import com.manbo.videoplayer.data.db.AppDatabase
import com.manbo.videoplayer.data.db.PlayHistoryEntity
import com.manbo.videoplayer.data.db.VideoDao
import com.manbo.videoplayer.data.db.VideoEntity
import kotlinx.coroutines.flow.Flow

class VideoRepository(private val dao: VideoDao) {

    fun getAllVideos(): Flow<List<VideoEntity>> = dao.getAllVideos()

    suspend fun getVideoById(id: Long): VideoEntity? = dao.getVideoById(id)

    suspend fun insertVideo(video: VideoEntity): Long = dao.insertVideo(video)

    suspend fun deleteVideo(video: VideoEntity) = dao.deleteVideo(video)

    fun getRecentPlays(): Flow<List<PlayHistoryEntity>> = dao.getHistoryWithExistingVideos()

    suspend fun getPlayHistory(videoId: Long): PlayHistoryEntity? = dao.getHistory(videoId)

    suspend fun savePlayHistory(videoId: Long, position: Long) {
        dao.upsertHistory(
            PlayHistoryEntity(
                videoId = videoId,
                position = position,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: VideoRepository? = null

        fun getRepository(database: AppDatabase): VideoRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = VideoRepository(database.videoDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/videoplayer/data/VideoRepository.kt
git commit -m "feat: add VideoRepository wrapping Room DAO"
```

---

## Task 4: Video Metadata Extractor Utility

**Files:**
- Create: `app/src/main/java/com/example/videoplayer/util/VideoMetadataExtractor.kt`

- [ ] **Step 1: Create `VideoMetadataExtractor.kt`**

```kotlin
package com.manbo.videoplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object VideoMetadataExtractor {

    data class Metadata(
        val duration: Long,
        val thumbnailPath: String
    )

    fun extract(context: Context, uri: Uri, videoId: Long): Metadata {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L

            val thumbnailDir = File(context.filesDir, "thumbnails")
            thumbnailDir.mkdirs()
            val thumbnailFile = File(thumbnailDir, "$videoId.jpg")

            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (frame != null && !thumbnailFile.exists()) {
                FileOutputStream(thumbnailFile).use { fos ->
                    frame.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
            }

            return Metadata(
                duration = duration,
                thumbnailPath = thumbnailFile.absolutePath
            )
        } finally {
            retriever.release()
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/videoplayer/util/
git commit -m "feat: add VideoMetadataExtractor for duration and thumbnail extraction"
```

---

## Task 5: VideoViewModel

**Files:**
- Create: `app/src/main/java/com/example/videoplayer/ui/viewmodel/VideoViewModel.kt`

- [ ] **Step 1: Create `VideoViewModel.kt`**

```kotlin
package com.manbo.videoplayer.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.manbo.videoplayer.data.VideoRepository
import com.manbo.videoplayer.data.db.AppDatabase
import com.manbo.videoplayer.data.db.VideoEntity
import com.manbo.videoplayer.util.VideoMetadataExtractor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository.getRepository(
        AppDatabase.getDatabase(application)
    )

    val videos: StateFlow<List<VideoEntity>> = repository.getAllVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPlays = repository.getRecentPlays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importVideos(uris: List<Uri>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            for (uri in uris) {
                try {
                    val tempId = System.currentTimeMillis()
                    val metadata = VideoMetadataExtractor.extract(context, uri, tempId)
                    val fileName = getFileName(context, uri)

                    val entity = VideoEntity(
                        uri = uri.toString(),
                        fileName = fileName,
                        duration = metadata.duration,
                        thumbnailPath = metadata.thumbnailPath,
                        addedAt = System.currentTimeMillis()
                    )
                    val insertedId = repository.insertVideo(entity)

                    // Re-extract thumbnail with real ID if different from tempId
                    if (insertedId != tempId) {
                        val finalMetadata = VideoMetadataExtractor.extract(context, uri, insertedId)
                        repository.deleteVideo(entity)
                        repository.insertVideo(
                            entity.copy(
                                id = insertedId,
                                thumbnailPath = finalMetadata.thumbnailPath
                            )
                        )
                    }
                } catch (_: Exception) {
                    // Skip files that fail to extract
                }
            }
        }
    }

    fun deleteVideo(video: VideoEntity) {
        viewModelScope.launch {
            repository.deleteVideo(video)
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) return it.getString(nameIndex)
            }
        }
        return uri.lastPathSegment ?: "Unknown"
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/videoplayer/ui/viewmodel/VideoViewModel.kt
git commit -m "feat: add VideoViewModel for video list state and import/delete operations"
```

---

## Task 6: PlayerViewModel

**Files:**
- Create: `app/src/main/java/com/example/videoplayer/ui/viewmodel/PlayerViewModel.kt`

- [ ] **Step 1: Create `PlayerViewModel.kt`**

```kotlin
package com.manbo.videoplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.manbo.videoplayer.data.VideoRepository
import com.manbo.videoplayer.data.db.AppDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository.getRepository(
        AppDatabase.getDatabase(application)
    )

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private var currentVideoId: Long = -1

    private val progressSaver = object : Player.Listener {
        override fun onPositionDiscontinuity(reason: Int) {}
    }

    fun play(uri: String, videoId: Long, startPosition: Long = 0) {
        currentVideoId = videoId
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        if (startPosition > 0) {
            player.seekTo(startPosition)
        }
        player.playWhenReady = true
        startProgressSaver()
    }

    private fun startProgressSaver() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                if (currentVideoId > 0 && player.isPlaying) {
                    saveProgress()
                }
            }
        }
    }

    fun saveProgress() {
        if (currentVideoId > 0 && player.contentDuration > 0) {
            viewModelScope.launch {
                repository.savePlayHistory(
                    currentVideoId,
                    player.currentPosition
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveProgress()
        player.release()
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/videoplayer/ui/viewmodel/PlayerViewModel.kt
git commit -m "feat: add PlayerViewModel for ExoPlayer lifecycle and progress saving"
```

---

## Task 7: UI Components (VideoCard + RecentPlayItem)

**Files:**
- Create: `app/src/main/java/com/example/videoplayer/ui/components/VideoCard.kt`
- Create: `app/src/main/java/com/example/videoplayer/ui/components/RecentPlayItem.kt`

- [ ] **Step 1: Create `VideoCard.kt`**

```kotlin
package com.manbo.videoplayer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    fileName: String,
    duration: Long,
    thumbnailPath: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentAlignment = Alignment.BottomEnd
            ) {
                val thumbFile = File(thumbnailPath)
                if (thumbFile.exists()) {
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = formatDuration(duration),
                    modifier = Modifier.padding(4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fileName,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
```

- [ ] **Step 2: Create `RecentPlayItem.kt`**

```kotlin
package com.manbo.videoplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentPlayItem(
    fileName: String,
    thumbnailPath: String,
    position: Long,
    duration: Long,
    lastPlayedAt: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbFile = File(thumbnailPath)
        if (thumbFile.exists()) {
            AsyncImage(
                model = thumbFile,
                contentDescription = fileName,
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Spacer(
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (duration > 0) {
                LinearProgressIndicator(
                    progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDate(lastPlayedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(epoch: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(epoch))
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/videoplayer/ui/components/
git commit -m "feat: add VideoCard and RecentPlayItem UI components"
```

---

## Task 8: VideoListScreen

**Files:**
- Create: `app/src/main/java/com/example/videoplayer/ui/screens/VideoListScreen.kt`

- [ ] **Step 1: Create `VideoListScreen.kt`**

```kotlin
package com.manbo.videoplayer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.manbo.videoplayer.data.db.VideoEntity
import com.manbo.videoplayer.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    videos: List<VideoEntity>,
    onVideoClick: (Long) -> Unit,
    onVideoLongClick: (VideoEntity) -> Unit,
    onImport: (List<android.net.Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) onImport(uris)
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Videos") },
                actions = {
                    IconButton(onClick = {
                        launcher.launch(arrayOf("video/*"))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Import video")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No videos yet. Tap + to import.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoCard(
                        fileName = video.fileName,
                        duration = video.duration,
                        thumbnailPath = video.thumbnailPath,
                        onClick = { onVideoClick(video.id) },
                        onLongClick = { onVideoLongClick(video) }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/videoplayer/ui/screens/VideoListScreen.kt
git commit -m "feat: add VideoListScreen with grid layout and import button"
```

---

## Task 9: RecentPlayScreen

**Files:**
- Create: `app/src/main/java/com/example/videoplayer/ui/screens/RecentPlayScreen.kt`

- [ ] **Step 1: Create `RecentPlayScreen.kt`**

```kotlin
package com.manbo.videoplayer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.manbo.videoplayer.data.db.PlayHistoryEntity
import com.manbo.videoplayer.data.db.VideoEntity
import com.manbo.videoplayer.ui.components.RecentPlayItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentPlayScreen(
    videos: List<VideoEntity>,
    history: List<PlayHistoryEntity>,
    onVideoClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val videoMap = remember(videos) { videos.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Recent") })
        },
        modifier = modifier
    ) { innerPadding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No recent plays yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(history, key = { it.videoId }) { entry ->
                    val video = videoMap[entry.videoId]
                    if (video != null) {
                        RecentPlayItem(
                            fileName = video.fileName,
                            thumbnailPath = video.thumbnailPath,
                            position = entry.position,
                            duration = video.duration,
                            lastPlayedAt = entry.lastPlayedAt,
                            onClick = { onVideoClick(video.id) }
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/videoplayer/ui/screens/RecentPlayScreen.kt
git commit -m "feat: add RecentPlayScreen with list layout and progress display"
```

---

## Task 10: PlayerScreen

**Files:**
- Create: `app/src/main/java/com/example/videoplayer/ui/screens/PlayerScreen.kt`

- [ ] **Step 1: Create `PlayerScreen.kt`**

```kotlin
package com.manbo.videoplayer.ui.screens

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.PlayerView
import com.manbo.videoplayer.ui.viewmodel.PlayerViewModel
import java.util.Locale
import kotlin.math.abs

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }

    // Hide system bars for immersive mode
    DisposableEffect(Unit) {
        activity?.let { act ->
            val window = act.window
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.let { act ->
                val window = act.window
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Pause/resume with lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.player.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (viewModel.player.playbackState == androidx.media3.common.Player.STATE_READY) {
                        viewModel.player.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Save progress and release on exit
    BackHandler {
        viewModel.saveProgress()
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures {
                    if (!isLocked) {
                        controlsVisible = !controlsVisible
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val width = size.width
                    val height = size.height
                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                        // Horizontal: seek
                        val seekMs = (dragAmount.x / width) * 120_000L
                        viewModel.player.seekTo(
                            (viewModel.player.currentPosition + seekMs).coerceIn(
                                0,
                                viewModel.player.duration.coerceAtLeast(0)
                            )
                        )
                    } else {
                        if (change.position.x < width / 2) {
                            // Left vertical: brightness
                            activity?.let { act ->
                                val window = act.window
                                val layout = window.attributes
                                layout.screenBrightness = (layout.screenBrightness - dragAmount.y / height * 0.5f)
                                    .coerceIn(0f, 1f)
                                window.attributes = layout
                            }
                        } else {
                            // Right vertical: volume
                            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE)
                                as android.media.AudioManager
                            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                            val newVolume = (currentVolume - (dragAmount.y / height * maxVolume).toInt())
                                .coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0)
                        }
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (controlsVisible && !isLocked) {
            PlayerControls(
                viewModel = viewModel,
                onBack = {
                    viewModel.saveProgress()
                    onBack()
                },
                onLock = { isLocked = true },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (isLocked) {
            IconButton(
                onClick = { isLocked = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Unlock", tint = Color.White)
            }
        }
    }
}

@Composable
private fun PlayerControls(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .statusBarsPadding()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatPosition(viewModel.player.currentPosition) + " / " +
                        formatPosition(viewModel.player.duration.coerceAtLeast(0)),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onLock) {
                Icon(Icons.Default.LockOpen, contentDescription = "Lock", tint = Color.White)
            }
        }

        LinearProgressIndicator(
            progress = {
                val duration = viewModel.player.duration.coerceAtLeast(1)
                (viewModel.player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.player.seekBack() }) {
                Text("⏪", color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = {
                if (viewModel.player.isPlaying) viewModel.player.pause()
                else viewModel.player.play()
            }) {
                Text(
                    if (viewModel.player.isPlaying) "⏸" else "▶",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { viewModel.player.seekForward() }) {
                Text("⏩", color = Color.White)
            }
        }
    }
}

private fun formatPosition(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/videoplayer/ui/screens/PlayerScreen.kt
git commit -m "feat: add fullscreen PlayerScreen with gestures and immersive mode"
```

---

## Task 11: Navigation Setup

**Files:**
- Create: `app/src/main/java/com/example/videoplayer/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create `NavGraph.kt`**

```kotlin
package com.manbo.videoplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.manbo.videoplayer.data.db.VideoEntity
import com.manbo.videoplayer.ui.screens.PlayerScreen
import com.manbo.videoplayer.ui.screens.RecentPlayScreen
import com.manbo.videoplayer.ui.screens.VideoListScreen
import com.manbo.videoplayer.ui.viewmodel.PlayerViewModel
import com.manbo.videoplayer.ui.viewmodel.VideoViewModel

object Routes {
    const val VIDEO_LIST = "videoList"
    const val RECENT_PLAY = "recentPlay"
    const val PLAYER = "player/{videoId}"

    fun playerRoute(videoId: Long) = "player/$videoId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    videoViewModel: VideoViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.VIDEO_LIST,
        modifier = modifier
    ) {
        composable(Routes.VIDEO_LIST) {
            VideoListScreen(
                videos = videoViewModel.videos.value,
                onVideoClick = { videoId ->
                    navController.navigate(Routes.playerRoute(videoId))
                },
                onVideoLongClick = { video: VideoEntity ->
                    videoViewModel.deleteVideo(video)
                },
                onImport = { uris -> videoViewModel.importVideos(uris) }
            )
        }

        composable(Routes.RECENT_PLAY) {
            RecentPlayScreen(
                videos = videoViewModel.videos.value,
                history = videoViewModel.recentPlays.value,
                onVideoClick = { videoId ->
                    navController.navigate(Routes.playerRoute(videoId))
                }
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("videoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getLong("videoId") ?: return@composable
            val video = videoViewModel.videos.value.find { it.id == videoId }

            if (video != null) {
                val playerViewModel: PlayerViewModel = viewModel()
                val history = videoViewModel.recentPlays.value.find { it.videoId == videoId }
                val startPosition = history?.position ?: 0L

                PlayerScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )

                // Trigger playback once
                androidx.compose.runtime.LaunchedEffect(video.uri) {
                    playerViewModel.play(video.uri, video.id, startPosition)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/videoplayer/ui/navigation/NavGraph.kt
git commit -m "feat: add NavGraph with video list, recent plays, and player routes"
```

---

## Task 12: Wire Up App Root (VideoplayerApp + MainActivity)

**Files:**
- Create: `app/src/main/java/com/example/videoplayer/VideoplayerApp.kt`
- Modify: `app/src/main/java/com/example/videoplayer/MainActivity.kt`

- [ ] **Step 1: Create `VideoplayerApp.kt`**

```kotlin
package com.manbo.videoplayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.manbo.videoplayer.ui.navigation.NavGraph
import com.manbo.videoplayer.ui.navigation.Routes
import com.manbo.videoplayer.ui.viewmodel.VideoViewModel

data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun VideoplayerApp() {
    val navController = rememberNavController()
    val videoViewModel: VideoViewModel = viewModel()

    val tabs = listOf(
        TabItem(Routes.VIDEO_LIST, "Videos", Icons.Default.VideoFile),
        TabItem(Routes.RECENT_PLAY, "Recent", Icons.Default.PlayArrow)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(Routes.VIDEO_LIST, Routes.RECENT_PLAY)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            videoViewModel = videoViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
```

- [ ] **Step 2: Update `MainActivity.kt`**

Replace the entire file with:

```kotlin
package com.manbo.videoplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.manbo.videoplayer.ui.theme.VideoplayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoplayerTheme {
                VideoplayerApp()
            }
        }
    }
}
```

- [ ] **Step 3: Verify full build**

Run: `./gradlew :app:assembleDebug 2>&1 | tail -10`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/videoplayer/VideoplayerApp.kt app/src/main/java/com/example/videoplayer/MainActivity.kt
git commit -m "feat: wire up VideoplayerApp with navigation and bottom bar"
```

---

## Self-Review Checklist

- **Spec coverage:** Import flow (Task 5 + 8), video list (Task 8), recent plays (Task 9), fullscreen player with gestures (Task 10), progress saving (Task 6), immersive mode (Task 10), navigation (Task 11), bottom bar tabs (Task 12) — all covered.
- **Placeholder scan:** No TBD/TODO/placeholders. All code blocks contain complete implementations.
- **Type consistency:** `VideoEntity`, `PlayHistoryEntity`, `VideoRepository`, `VideoViewModel`, `PlayerViewModel` — all type signatures match across files. Route strings consistent between `NavGraph.kt` and `VideoplayerApp.kt`.
