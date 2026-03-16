# TwinMind Recorder

A professional Android application for recording, transcribing, and summarizing meetings using OpenAI's Whisper and GPT-4o APIs.

## Overview

TwinMind Recorder captures audio in 30-second chunks, transcribes speech using OpenAI Whisper, and generates structured meeting summaries with titles, action items, and key points using GPT-4o streaming. The app operates as a foreground service to ensure reliable recording and processes transcription/summarization tasks in the background via WorkManager.

## Architecture

**MVVM + Clean Architecture**

```
├── data/
│   ├── local/        # Room database, DAOs, entities
│   ├── remote/       # Retrofit APIs, DTOs
│   └── repository/   # Data layer abstraction
├── di/               # Hilt modules
├── service/          # Foreground recording service
├── ui/               # Compose screens, ViewModels
├── util/             # Helpers, silence detection, storage utils
└── worker/           # WorkManager background tasks
```

**Key Components:**
- **RecordingService**: Foreground service with microphone type for continuous audio capture
- **TranscriptionWorker**: Uploads chunks to Whisper API with retry logic
- **SummaryWorker**: Streams GPT-4o responses with real-time database updates
- **SilenceDetector**: RMS-based audio level monitoring for silence detection
- **Repository Pattern**: Single source of truth via Room + Flow

## Tech Stack

### Core
- **Kotlin** - Primary language
- **Jetpack Compose** - Declarative UI with Material 3
- **MVVM** - Architecture pattern
- **Coroutines & Flow** - Asynchronous operations

### Dependency Injection
- **Hilt** - DI framework with @HiltAndroidApp, @AndroidEntryPoint

### Data
- **Room** - Local SQLite database with Flow queries
- **DataStore** - Preferences storage
- **Retrofit + OkHttp** - Network layer with logging interceptors

### Background Processing
- **WorkManager** - Reliable background task execution with constraints
- **Foreground Service** - Audio recording with notification

### AI/ML Integration
- **OpenAI Whisper API** - Speech-to-text transcription
- **GPT-4o Streaming** - Real-time summary generation via SSE
- **Custom JSON Parsing** - Structured response extraction

## Features

### Recording
- Real-time waveform visualization
- 30-second chunked recording with 2-second overlap for continuity
- Background recording via foreground service
- Live transcript preview

### Transcription
- Automatic upload of audio chunks to Whisper API
- Deduplication of overlapping content during stitching
- Network-aware retry with exponential backoff

### Summarization
- GPT-4o generates structured JSON with:
  - Meeting title (max 10 words)
  - Summary paragraph
  - Action items array
  - Key points array
- Real-time streaming updates to UI
- Progressive parsing during token accumulation

### Dashboard
- Meeting history with timestamps
- Session status tracking (recording, transcribing, summarizing, completed)
- Quick access to summaries

## Edge Cases Handled

### Network & API
1. **Connection Failures** - Automatic retry with WorkManager (max 3 attempts for transcription, 5 for summary)
2. **Socket Timeouts** - Separate handling with retry logic
3. **HTTP Errors** - Status tracking in database, retry based on error type
4. **Malformed SSE Chunks** - Silent skip of corrupted streaming data

### Storage & Resources
5. **Low Storage** - Pre-recording check (minimum 50MB required), checked before each chunk
6. **Corrupt Audio Files** - Validation before upload (minimum 500 bytes, proper WAV header)
7. **Empty Transcripts** - Graceful handling, retry if transcript is blank

### Audio & Recording
8. **Silence Detection** - RMS-based monitoring with configurable threshold (10-second sustained silence triggers callback)
9. **Audio Focus Loss** - Pause/resume recording via AudioFocusManager
10. **Headset Events** - Broadcast receiver for plug/unplug handling
11. **Phone Calls** - READ_PHONE_STATE permission for call state awareness

### System & Lifecycle
12. **Process Death** - StateFlow + Room persistence for state restoration
13. **Boot Completed** - WorkManager re-enqueues pending tasks via BootReceiver
14. **Permission Denials** - Runtime request handling for RECORD_AUDIO, POST_NOTIFICATIONS, READ_PHONE_STATE
15. **Android 14+ Requirements** - Proper foreground service type (microphone) declaration

### Data Integrity
16. **Chunk Deduplication** - Overlapping audio regions removed during transcript stitching
17. **Duplicate Uploads** - UploadStatus tracking prevents re-processing completed chunks
18. **Concurrent Access** - Room database transactions ensure consistency

## Setup

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 34+
- OpenAI API key

### Configuration
1. Add your OpenAI API key in `NetworkModule.kt` or via BuildConfig
2. Build and run on device (emulator has microphone limitations)

### Permissions (Auto-requested)
- RECORD_AUDIO
- POST_NOTIFICATIONS (Android 13+)
- READ_PHONE_STATE (Android 12+)
- FOREGROUND_SERVICE_MICROPHONE

## Building

```bash
./gradlew :app:assembleDebug
```

## Project Structure

```
app/src/main/java/com/twinmind/recorder/
├── TwinMindApp.kt              # Application class with WorkManager config
├── MainActivity.kt             # Single activity with Compose navigation
├── data/
│   ├── local/                  # Room DB, DAOs, entities
│   ├── remote/                 # WhisperApi, GptStreamClient, DTOs
│   └── repository/             # RecordingRepository, TranscriptRepository
├── di/                         # Hilt modules (NetworkModule, DatabaseModule)
├── service/                    # RecordingService, AudioFocusManager
├── ui/
│   ├── dashboard/              # Dashboard screen
│   ├── recording/              # Recording screen with waveform
│   ├── summary/                # Summary display screen
│   └── theme/                  # Material 3 theme
├── util/                       # SilenceDetector, StorageUtils, WavUtils
└── worker/                     # TranscriptionWorker, SummaryWorker
```

## Dependencies

All versions managed via `gradle/libs.versions.toml`:
- Compose BOM 2024.12.01
- Hilt 2.52
- Room 2.6.1
- WorkManager 2.9.1
- Retrofit 2.11.0
- Lifecycle 2.8.7
- Coroutines 1.9.0

## License

Private project - All rights reserved
