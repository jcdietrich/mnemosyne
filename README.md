<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="128" height="128" alt="Mnemosyne Icon" />
</p>

# Mnemosyne

An on-device personal memory prosthetic for Android.

## Overview

Mnemosyne helps people with SDAM (Severely Deficient Autobiographical Memory) capture and retrieve episodic memories. All AI inference (STT, embedding, vector search) runs entirely on-device with no data sent to remote servers.

## Key Features

- **Voice capture:** Press-and-hold to record, release to save. 20s–5min recordings.
- **On-device transcription:** sherpa-onnx Zipformer STT, no cloud STT.
- **Semantic search:** MediaPipe Universal Sentence Encoder + ObjectBox HNSW vector search.
- **Location tagging:** GPS coordinates attached to each memory.
- **Encrypted at rest:** Android Keystore + AES-256-GCM.
- **Drive backup:** Automatic WiFi backup to Google Drive `appDataFolder` (encrypted).

## Requirements

- Android 5.0+ (API 21+)
- Google Play Services (for Drive backup/restore)
- ~55–105 MB storage for AI models (downloaded on first launch)

## Development

### Setup

1. Android Studio Hedgehog or later
2. Android SDK API 34
3. Clone the repo and open in Android Studio

```bash
git clone <repo>
cd mnemosyne
./gradlew :app:assembleDebug
```

### Build

```bash
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:assembleRelease    # release APK
```

### Test

```bash
./gradlew :app:test                       # unit tests
./gradlew :app:connectedAndroidTest       # instrumented tests (device/emulator required)
./gradlew :app:lint                       # lint
```

## Architecture

MVVM with Repository pattern, single-module app.

```
UI Layer (Compose) → ViewModels → Repositories → {ObjectBox, sherpa-onnx, MediaPipe, WorkManager, Drive API}
```

## Plan

See [`plans/2026-08-24-001-feat-mnemosyne-mvp-plan.md`](plans/2026-08-24-001-feat-mnemosyne-mvp-plan.md) for the full implementation plan.
