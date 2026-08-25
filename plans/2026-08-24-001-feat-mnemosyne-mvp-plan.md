---
title: Mnemosyne MVP - Plan
type: feat
date: 2026-08-24
topic: mnemosyne-mvp
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Mnemosyne MVP - Plan

## Goal Capsule

- **Objective:** Build the Mnemosyne v1 Android app — a fully on-device personal memory prosthetic that captures, transcribes, embeds, and stores voice memories with location context, and retrieves them via semantic search.
- **Product authority:** This plan owns the full v1 app. Audio playback, always-listening capture, multi-device sync, and LLM-powered structured recall are not active scope.
- **Execution profile:** New project, greenfield Android app. No existing codebase to navigate.
- **Stop conditions:** Stop and surface to user if any on-device AI library cannot be integrated at API 21 minimum, or if Drive `appDataFolder` scope is unavailable on the target test device.
- **Tail ownership:** Implementer.

---

## Product Contract

### Summary

Mnemosyne is a native Android app (Kotlin / Jetpack Compose) that lets a user press and hold a button to record a voice memory, transcribes it on-device, embeds the transcript, and stores it with a timestamp and GPS coordinates — all without leaving the device. A reverse-chronological memory feed is searchable by voice or text via semantic similarity. Memories are encrypted at rest and back up automatically to Google Drive on WiFi; full restore to a new device comes from that backup.

### Problem Frame

The user has Severely Deficient Autobiographical Memory (SDAM) — a neurological condition in which autobiographical memories do not form with the vivid, first-person episodic quality that most people experience. Without external capture, events fade quickly and without reliable recall cues. Existing note-taking tools require typing (high friction at the moment of experience), rely on cloud AI (privacy and connectivity dependency), or lack semantic retrieval (keyword search does not surface memories by meaning). Mnemosyne is designed as a friction-free memory prosthetic: capture in seconds, retrieve by concept, and work entirely offline with no data leaving the device.

### Key Decisions

- KD1. **All AI inference runs on-device.** No STT, embedding, or search query is sent to a remote server. Governs R1, R2, R3.
- KD2. **Press-and-hold capture.** (session-settled: user-directed — chosen over tap-toggle and always-listening: minimizes accidental capture without requiring a separate stop action.) Governs R4.
- KD3. **Reverse-chron feed with semantic search overlay.** (session-settled: user-directed — chosen over pure semantic-only or timeline-only: supports both discovery browsing and intentional recall.) Governs R7, R8.
- KD4. **Transcript + timestamp + GPS; no raw audio stored.** (session-settled: user-directed — chosen over retaining raw audio: reduces storage burden; audio playback is deferred.) Governs R5.
- KD5. **Encrypted at rest, encrypted in backup.** The same encryption key protects on-device storage and the Drive backup archive. Governs R11, R12.
- KD6. **Drive `appDataFolder` scope.** Backup writes to a private app sandbox in Google Drive, invisible to the Drive UI. Governs R12.

### Requirements

**Voice Capture**

- R1. The app transcribes audio on-device using an on-device STT engine; no audio or transcript is transmitted to a remote server.
- R2. Transcription runs after the user releases the record button (not streaming during hold).
- R3. An on-device text embedding model converts the completed transcript to a vector; no text is transmitted to a remote server.
- R4. The capture gesture is press-and-hold on a dedicated record button; releasing the button ends the recording and triggers transcription and embedding.
- R5. Each stored memory contains: transcript text, UTC timestamp of capture start, and GPS coordinates captured at capture start. No raw audio file is stored.
- R6. The app requests microphone and fine location permissions at first launch, before the record button is available.

**Memory Storage & Retrieval**

- R7. Memories are displayed in a reverse-chronological feed as the default home view.
- R8. A voice or text search field filters and re-ranks the feed by semantic similarity to the query; voice search uses the same on-device STT pipeline as capture (per R1).
- R9. Each memory card in the feed shows: transcript excerpt, relative or absolute timestamp, and approximate location name or coordinates.
- R10. Tapping a memory card opens a detail view showing the full transcript, exact timestamp, and GPS coordinates.

**Security**

- R11. All memory data (transcripts, embeddings, metadata) is encrypted at rest on the device.
- R12. The Google Drive backup file is an encrypted archive using the same key as on-device storage (per KD5), written to the Drive `appDataFolder` scope.

**Backup & Restore**

- R13. When the device is on WiFi and the user is authenticated with Google, the app backs up automatically in the background without user action.
- R14. Restore from a Drive backup re-downloads the encrypted archive and imports all memories, replacing any existing local data. Restore is triggered manually from Settings.

---

### Key Flows

- F1. **Capture a memory**
  - **Trigger:** User presses and holds the record button.
  - **Steps:** App records audio. User releases button. App runs STT on the audio buffer (R1, R2). App captures GPS fix at release time (R5). App generates embedding of transcript (R3). App encrypts and writes the memory record to local storage (R11). Memory appears at the top of the feed (R7).
  - **Error path:** If STT produces an empty transcript, the memory is discarded and the user is notified with a brief in-app message.

- F2. **Search memories**
  - **Trigger:** User taps the search field and types, or taps the mic icon.
  - **Steps:** For voice input, app runs STT on the query (R1). App generates an embedding of the query text (R3). App queries the vector store for nearest neighbors. Feed updates to show ranked results in place of the default reverse-chron order.
  - **Clear:** Dismissing or clearing the search field returns the feed to reverse-chron order (R7).

- F3. **Automatic Drive backup**
  - **Trigger:** Device connects to WiFi and user is authenticated.
  - **Steps:** Background worker detects new or modified memories since last sync. App serializes and encrypts the full memory corpus into an archive (R12). App uploads the archive to `appDataFolder` via Drive REST API v3.
  - **Conflict:** The Drive backup is a single authoritative snapshot; the most recent upload wins. No merge logic.

- F4. **Restore from Drive**
  - **Trigger:** User taps "Restore from Drive" in Settings.
  - **Steps:** App downloads the encrypted archive from `appDataFolder`. App decrypts and imports all memory records. Existing local data is replaced. User is shown a count of imported memories on completion.

---

### Acceptance Examples

- AE1. **Capture succeeds.**
  **Covers R1, R2, R4, R5.**
  Given the user holds the record button and speaks a sentence, when they release, then a new memory appears at the top of the feed with the transcript, current timestamp, and a location.

- AE2. **Empty recording discarded.**
  **Covers R4, F1 error path.**
  Given the user holds the record button for less than 1 second and releases, when STT returns an empty result, then no memory is written and the user sees a brief discard notification.

- AE3. **Voice search re-ranks feed.**
  **Covers R8.**
  Given the feed shows 10 memories and the user speaks a search query, when results appear, then the feed order changes to semantic similarity rank and the search field shows the query text.

- AE4. **Search cleared restores chron order.**
  **Covers R7, R8.**
  Given search results are displayed, when the user clears the search field, then the feed returns to reverse-chronological order.

- AE5. **Backup fires on WiFi.**
  **Covers R13.**
  Given the user has at least one memory and the device is on WiFi with a valid Google session, when the background worker runs, then a Drive `appDataFolder` file is present or updated with an encrypted archive.

- AE6. **Restore replaces local data.**
  **Covers R14.**
  Given the app is installed fresh on a new device and the user authenticates with Google and triggers restore, when restore completes, then the feed shows the same memories as the backed-up device.

---

### Scope Boundaries

**Deferred for later**

- Raw audio retention and playback alongside transcripts.
- Always-listening / wake-word capture.
- LLM-powered structured recall (e.g., "what did I do last Tuesday" → generated summary).
- Per-memory editing or deletion of transcripts.
- Multi-device real-time sync (Drive is backup/restore only, not a sync bus).
- App-level biometric or PIN lock.

**Outside this product's identity**

- Any remote AI inference (cloud STT, cloud embedding, cloud LLM).
- Sharing memories with other users.
- Public or shared Google Drive folder (backup uses the private `appDataFolder` scope only).

---

### Dependencies / Assumptions

- Target minimum Android API level is 21 (Android 5.0).
- A bundled STT engine is used (not platform `SpeechRecognizer`); OEM reliability outside Pixel is poor for on-device mode.
- Google Play Services are present on the target device; de-Googled ROMs cannot use Drive backup.
- The encryption key is device-local. Key recovery (e.g., passphrase-wrapped) is out of scope for v1.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **STT library: `sherpa-onnx` with Zipformer model.**
  Chosen over `whisper.cpp` (higher RAM, JNI-only, no Kotlin SDK) and platform `SpeechRecognizer` (OEM-unreliable). `sherpa-onnx` provides first-class Kotlin bindings, ~50–100 MB RAM at inference, and 40–80 MB model size. Streaming capability is a free future option. Governs U1, U4.

- KTD2. **Embedding: MediaPipe `TextEmbedder` with Universal Sentence Encoder.**
  Chosen over ONNX Runtime + `all-MiniLM-L6-v2` (requires custom tokenizer implementation). MediaPipe handles tokenization natively, has API 21+ support, and produces 512-dim embeddings at ~15–25 MB model size. Inference is ~10–35 ms on device CPU. Governs U1, U5.

- KTD3. **Vector store: ObjectBox with HNSW vector indexing.**
  Chosen over `sqlite-vec` (requires NDK custom SQLite build, O(N) brute-force). ObjectBox is mobile-first, has native Kotlin support, sub-millisecond ANN queries at 100k+ vectors, and ~2–3 MB native binary overhead. Governs U2, U5.

- KTD4. **Encryption: Android Keystore + AES-256-GCM.**
  The encryption key is generated and stored in the Android Keystore (hardware-backed where available). All memory records are encrypted with AES-256-GCM before writing to ObjectBox. The backup archive is encrypted with the same key before upload. Governs R11, R12, U2, U6.

- KTD5. **Background backup: WorkManager with `NetworkType.UNMETERED` constraint.**
  WorkManager is the Android-recommended API for deferrable background work. The `UNMETERED` network constraint maps to WiFi (per R13). The worker runs a `PeriodicWorkRequest` at minimum 15-minute intervals (WorkManager floor). Governs R13, U6.

- KTD6. **Drive auth: Google Credential Manager + `drive.appdata` OAuth scope.**
  Google Credential Manager replaces the deprecated Google Sign-In SDK. The `drive.appdata` scope grants access only to the app's private `appDataFolder`. Governs R12, R13, R14, U6.

- KTD7. **Model delivery: on-demand download at first launch.**
  The STT and embedding models are not bundled in the APK. They download to `filesDir` on first launch with a progress screen. Chosen over Play Asset Delivery (requires Play Console setup, adds tooling complexity for a single-developer app). Total download ~55–105 MB. Governs U3.

- KTD8. **Architecture: MVVM with Repository pattern, single-module app.**
  ViewModels expose `StateFlow` to Compose UI. Repositories abstract ObjectBox, STT, embedding, and Drive. No multi-module split in v1 — premature for a single developer. Feature packages: `capture`, `feed`, `search`, `settings`, `backup`.

- KTD9. **Backup archive format: JSON Lines + AES-256-GCM envelope.**
  The backup serializes all memory records as JSON Lines (one record per line: transcript, timestamp, lat, lon, embedding vector as float array). The entire file is AES-256-GCM encrypted as a single envelope before Drive upload. Restore decrypts then re-imports each line into ObjectBox. Governs R12, R14, U6.

### High-Level Technical Design

```
┌─────────────────────────────────────────────────────────────┐
│                        Compose UI Layer                      │
│  HomeScreen (feed)  │  CaptureButton  │  SearchBar          │
│  MemoryDetailScreen │  SettingsScreen  │  PermissionScreen   │
└──────────┬──────────────────┬────────────────┬──────────────┘
           │                  │                │
    ┌──────▼──────┐   ┌───────▼──────┐  ┌─────▼──────────┐
    │  FeedVM     │   │  CaptureVM   │  │  SettingsVM    │
    │  SearchVM   │   │              │  │                │
    └──────┬──────┘   └───────┬──────┘  └─────┬──────────┘
           │                  │                │
    ┌──────▼──────────────────▼────────────────▼──────────┐
    │                   Repository Layer                    │
    │  MemoryRepository │ SttRepository │ BackupRepository  │
    └──────┬────────────────┬────────────────┬─────────────┘
           │                │                │
    ┌──────▼──────┐  ┌──────▼──────┐  ┌─────▼──────────┐
    │  ObjectBox  │  │ sherpa-onnx │  │  Drive REST v3  │
    │  (AES-GCM)  │  │  MediaPipe  │  │  WorkManager   │
    └─────────────┘  └─────────────┘  └────────────────┘
```

**Post-release pipeline (F1 detail):**

```
release button → stop MediaRecorder → flush to ByteArray
  → SttRepository.transcribe(audio)     [sherpa-onnx, ~1x realtime]
  → EmbeddingRepository.embed(text)     [MediaPipe, ~20ms]
  → LocationRepository.getLastKnown()   [FusedLocationProvider]
  → MemoryRepository.save(Memory)       [encrypt → ObjectBox]
  → emit to FeedViewModel StateFlow
```

### Sequencing

Units are ordered by dependency. U1–U3 establish foundations; U4–U7 build features; U8 integrates and tests end-to-end.

1. **U1** — Project scaffold, permissions, STT pipeline
2. **U2** — ObjectBox data layer with encryption
3. **U3** — Model download and initialization
4. **U4** — Capture UI and record-to-memory flow
5. **U5** — Feed UI, embedding pipeline, and vector search
6. **U6** — Drive backup and restore
7. **U7** — Settings screen and first-launch onboarding
8. **U8** — Integration, end-to-end testing, and polish

---

## Implementation Units

### U1. Project Scaffold, Permissions, and STT Pipeline

**Goal:** Create the Android project, configure dependencies, implement the permission request flow, and wire up `sherpa-onnx` STT so that audio can be recorded and transcribed on-device.

**Requirements:** R1, R2, R6.

**Files:**
- `app/build.gradle.kts` — dependencies: Jetpack Compose BOM, `sherpa-onnx-android`, ObjectBox plugin, MediaPipe Tasks Text, WorkManager, Google Credential Manager, Drive REST client
- `app/src/main/AndroidManifest.xml` — permissions: `RECORD_AUDIO`, `ACCESS_FINE_LOCATION`, `INTERNET`, `ACCESS_NETWORK_STATE`
- `app/src/main/java/com/mnemosyne/stt/SttRepository.kt` — wraps `OnlineRecognizer` / `OfflineRecognizer` from sherpa-onnx; exposes `suspend fun transcribe(audio: ShortArray): String`
- `app/src/main/java/com/mnemosyne/stt/AudioRecorder.kt` — `MediaRecorder`-based PCM capture; exposes `start()` / `stop(): ShortArray`
- `app/src/main/java/com/mnemosyne/permissions/PermissionViewModel.kt` — tracks `RECORD_AUDIO` and `ACCESS_FINE_LOCATION` grant state; exposes `StateFlow<PermissionState>`
- `app/src/main/java/com/mnemosyne/permissions/PermissionScreen.kt` — Compose screen shown when permissions are not granted
- `app/src/test/java/com/mnemosyne/stt/SttRepositoryTest.kt`
- `app/src/test/java/com/mnemosyne/stt/AudioRecorderTest.kt`

**Approach:**
- Minimum SDK 21. `compileSdk` and `targetSdk` = 35 (current stable).
- Use sherpa-onnx `OfflineRecognizer` with a Zipformer-bilingual model (supports EN). Model path is passed at construction; model files live in `filesDir` after U3 downloads them.
- `AudioRecorder` uses `AudioRecord` (not `MediaRecorder`) for direct PCM output — sherpa-onnx expects raw PCM at 16 kHz, 16-bit mono.
- `PermissionScreen` uses `rememberLauncherForActivityResult` with `RequestMultiplePermissions`. Show a rationale if any permission was previously denied.

**Test Scenarios:**
- `SttRepositoryTest`: given a pre-recorded 5-second PCM WAV of spoken English, `transcribe()` returns a non-empty string.
- `SttRepositoryTest`: given a silent PCM buffer (all zeros), `transcribe()` returns an empty or whitespace-only string.
- `AudioRecorderTest`: `start()` then `stop()` within 2 seconds returns a `ShortArray` of length > 0.
- `PermissionViewModelTest`: initial state is `PermissionState.Denied`; after grant callback, state transitions to `PermissionState.Granted`.

**Verification:** Unit tests pass with `./gradlew :app:test`. Manual: record button visible only after permissions granted.

---

### U2. ObjectBox Data Layer with AES-256-GCM Encryption

**Goal:** Define the `Memory` entity, configure ObjectBox, and implement at-rest encryption using Android Keystore + AES-256-GCM so that all persisted data is encrypted before write and decrypted after read.

**Requirements:** R5, R11. Covered by KTD3, KTD4.

**Files:**
- `app/src/main/java/com/mnemosyne/data/Memory.kt` — ObjectBox `@Entity`: fields `id: Long`, `transcript: String` (encrypted), `timestampUtcMs: Long`, `latitudeDeg: Double`, `longitudeDeg: Double`, `embeddingVector: FloatArray` (ObjectBox HNSW vector property)
- `app/src/main/java/com/mnemosyne/data/MemoryBox.kt` — ObjectBox `BoxStore` singleton; initializes DB at `filesDir/objectbox/`
- `app/src/main/java/com/mnemosyne/data/MemoryRepository.kt` — `save(memory: Memory)`, `getAll(): List<Memory>`, `searchByVector(query: FloatArray, limit: Int): List<Memory>`
- `app/src/main/java/com/mnemosyne/crypto/CryptoManager.kt` — generates/retrieves AES-256-GCM key in Android Keystore alias `mnemosyne_key`; exposes `encrypt(plaintext: ByteArray): ByteArray` and `decrypt(ciphertext: ByteArray): ByteArray`
- `app/src/test/java/com/mnemosyne/data/MemoryRepositoryTest.kt`
- `app/src/test/java/com/mnemosyne/crypto/CryptoManagerTest.kt`

**Approach:**
- ObjectBox entity stores the transcript as a Base64-encoded AES-256-GCM ciphertext string. The embedding vector is stored unencrypted (float values reveal nothing sensitive). `CryptoManager` is called in `MemoryRepository.save()` and at read time in a mapping function.
- AES-256-GCM IV (12 bytes) is prepended to the ciphertext before Base64 encoding. `decrypt()` strips the IV prefix before decryption.
- `BoxStore` is initialized once in `Application.onCreate()` via a Hilt singleton.
- HNSW index on `embeddingVector`: `dimensions = 512` (MediaPipe USE output), `maxDegrees = 16`, `efConstruction = 100`. These values are ObjectBox defaults suitable for <100k vectors.

**Test Scenarios:**
- `CryptoManagerTest`: encrypt then decrypt round-trips a UTF-8 string and returns the original bytes.
- `CryptoManagerTest`: two calls to `encrypt()` with the same plaintext produce different ciphertexts (IV randomness).
- `MemoryRepositoryTest`: save a `Memory`, call `getAll()`, and assert the returned transcript equals the original plaintext.
- `MemoryRepositoryTest`: save 3 memories, call `searchByVector()` with the first memory's embedding, assert the first result's ID matches.

**Verification:** `./gradlew :app:test`. No ObjectBox raw data readable without the Keystore key (manual check: examine DB file bytes for plaintext transcripts — none should appear).

---

### U3. Model Download and Initialization Screen

**Goal:** Download the sherpa-onnx STT model and MediaPipe USE embedding model to `filesDir` on first launch. Show a progress screen while downloading. Block the rest of the app until models are ready.

**Requirements:** Governs KTD7.

**Files:**
- `app/src/main/java/com/mnemosyne/models/ModelManager.kt` — checks whether model files exist in `filesDir/models/`; downloads from hardcoded HTTPS URLs if missing; exposes `suspend fun ensureModels(): ModelStatus`
- `app/src/main/java/com/mnemosyne/models/ModelDownloadViewModel.kt` — drives the download UI; exposes `StateFlow<DownloadState>` (progress percentage, error, done)
- `app/src/main/java/com/mnemosyne/models/ModelDownloadScreen.kt` — Compose screen with a progress indicator; shown by `MainActivity` when `ModelStatus.NotReady`
- `app/src/test/java/com/mnemosyne/models/ModelManagerTest.kt`

**Approach:**
- sherpa-onnx model: `sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20` (EN subset). ~80 MB. Hosted at the official sherpa-onnx GitHub release URL.
- MediaPipe USE model: `universal_sentence_encoder.tflite` from the MediaPipe model hub. ~25 MB.
- `ModelManager` uses `OkHttp` for download with progress tracking via `ResponseBody.source()`. Files are written to a temp path and renamed atomically on completion.
- `ModelDownloadScreen` shows a linear `LinearProgressIndicator` with MB downloaded / MB total and a retry button on error.

**Test Scenarios:**
- `ModelManagerTest`: when model files already exist in `filesDir/models/`, `ensureModels()` returns `ModelStatus.Ready` without making any network call.
- `ModelManagerTest`: when a download URL returns 404, `ensureModels()` returns `ModelStatus.Error`.
- `ModelDownloadViewModelTest`: given a `ModelManager` mock that emits 0%, 50%, 100% progress, the StateFlow sequences through the corresponding `DownloadState` values.

**Verification:** `./gradlew :app:test`. Manual: fresh install shows download screen, completes, then proceeds to home.

---

### U4. Capture UI and Record-to-Memory Flow

**Goal:** Implement the home screen's capture button with press-and-hold gesture, wire the full F1 pipeline (record → STT → GPS → embed → save → appear in feed), and surface the empty-transcript error.

**Requirements:** R1, R2, R3, R4, R5, R6. Covers F1, AE1, AE2.

**Files:**
- `app/src/main/java/com/mnemosyne/capture/CaptureViewModel.kt` — orchestrates `AudioRecorder`, `SttRepository`, `EmbeddingRepository`, `LocationRepository`, and `MemoryRepository`; exposes `StateFlow<CaptureState>` (idle, recording, processing, done, error)
- `app/src/main/java/com/mnemosyne/capture/CaptureButton.kt` — Compose component with `Modifier.pointerInput` for press-and-hold; calls `onPressStart` / `onPressEnd` on the ViewModel; shows recording indicator (pulsing red dot) while held
- `app/src/main/java/com/mnemosyne/location/LocationRepository.kt` — wraps `FusedLocationProviderClient.lastLocation`; exposes `suspend fun getLastKnownLocation(): LatLng?`
- `app/src/main/java/com/mnemosyne/embedding/EmbeddingRepository.kt` — wraps MediaPipe `TextEmbedder`; exposes `suspend fun embed(text: String): FloatArray`
- `app/src/test/java/com/mnemosyne/capture/CaptureViewModelTest.kt`
- `app/src/test/java/com/mnemosyne/embedding/EmbeddingRepositoryTest.kt`

**Approach:**
- `CaptureButton` uses `detectTapGestures(onPress = { ... })` inside `pointerInput`. On `onPress`, call ViewModel to start recording. In the `finally` block (covers both release and cancel), call ViewModel to stop.
- `CaptureViewModel.onRecordingStop()` is a `suspend fun` launched in `viewModelScope`. It chains: `transcribe()` → if blank, emit `CaptureState.Error("empty")` and return; else `embed()` + `getLastKnownLocation()` (both launched in parallel with `async`) → `MemoryRepository.save()` → emit `CaptureState.Done`.
- `EmbeddingRepository` initializes `TextEmbedder` lazily on first call. Use `TextEmbedder.embedText()` (synchronous; run on `Dispatchers.Default`).
- Snackbar on `CaptureState.Error("empty")` with message "Nothing recorded — try again."

**Test Scenarios:**
- `CaptureViewModelTest`: given STT returns "Hello world" and embed returns a float array, `onRecordingStop()` transitions state to `CaptureState.Done` and calls `MemoryRepository.save()` once.
- `CaptureViewModelTest`: given STT returns "", `onRecordingStop()` transitions to `CaptureState.Error` and does not call `MemoryRepository.save()`.
- `EmbeddingRepositoryTest`: `embed("hello")` returns a `FloatArray` of length 512.
- `LocationRepositoryTest`: when `FusedLocationProviderClient.lastLocation` returns null, `getLastKnownLocation()` returns null without throwing.

**Verification:** `./gradlew :app:test`. Manual: hold button, speak, release — memory appears in feed within ~3 seconds on a mid-range device.

---

### U5. Feed UI, Vector Search, and Memory Detail

**Goal:** Implement the reverse-chronological memory feed, the voice/text search bar with semantic re-ranking, and the memory detail screen.

**Requirements:** R7, R8, R9, R10. Covers F2, AE3, AE4.

**Files:**
- `app/src/main/java/com/mnemosyne/feed/FeedViewModel.kt` — observes `MemoryRepository.getAll()` as a `StateFlow<List<Memory>>`; exposes `searchQuery: StateFlow<String>` and `displayedMemories: StateFlow<List<Memory>>` (chron or ranked)
- `app/src/main/java/com/mnemosyne/feed/FeedScreen.kt` — Compose `LazyColumn` of `MemoryCard` composables; `SearchBar` at top; navigates to `MemoryDetailScreen` on card tap
- `app/src/main/java/com/mnemosyne/feed/MemoryCard.kt` — shows transcript excerpt (max 2 lines), relative timestamp, approximate location
- `app/src/main/java/com/mnemosyne/feed/MemoryDetailScreen.kt` — shows full transcript, exact UTC timestamp, GPS coordinates
- `app/src/main/java/com/mnemosyne/feed/SearchBar.kt` — Compose text field with mic icon; mic tap triggers voice capture for query (reuses `SttRepository`)
- `app/src/test/java/com/mnemosyne/feed/FeedViewModelTest.kt`

**Approach:**
- When `searchQuery` is empty, `displayedMemories` = `getAll()` sorted descending by `timestampUtcMs`.
- When `searchQuery` is non-empty: embed the query text with `EmbeddingRepository.embed()`, call `MemoryRepository.searchByVector(queryVector, limit = 20)`, and use the returned order.
- Debounce text search by 300ms using `StateFlow.debounce()` to avoid embedding on every keystroke.
- Voice search: mic tap → hold to record (same `AudioRecorder` path) → on release, STT → fills `searchQuery` text field → debounce triggers embed → vector search.
- Location display: use Android `Geocoder` reverse-geocode to get a locality string; fall back to "%.4f, %.4f".format(lat, lon) if Geocoder fails or returns empty.

**Test Scenarios:**
- `FeedViewModelTest`: with 3 memories at timestamps T1 < T2 < T3 and empty query, `displayedMemories` is [T3, T2, T1].
- `FeedViewModelTest`: setting a non-empty query calls `EmbeddingRepository.embed()` and `MemoryRepository.searchByVector()`.
- `FeedViewModelTest`: clearing the query returns the list to chronological order.
- `FeedViewModelTest`: text query with debounce — two rapid query updates result in only one embed call after 300ms.

**Verification:** `./gradlew :app:test`. Manual: feed shows most recent memory at top; search re-ranks correctly for semantically related queries.

---

### U6. Drive Backup and Restore

**Goal:** Implement automatic WiFi backup via WorkManager and manual restore from Settings, with encrypted archive serialization/deserialization, per KTD5, KTD6, KTD9.

**Requirements:** R12, R13, R14. Covers F3, F4, AE5, AE6.

**Files:**
- `app/src/main/java/com/mnemosyne/backup/BackupWorker.kt` — `CoroutineWorker`; serializes memories to JSON Lines, encrypts with `CryptoManager`, uploads via Drive REST API
- `app/src/main/java/com/mnemosyne/backup/BackupRepository.kt` — `scheduleBackup()` enqueues `PeriodicWorkRequest`; `uploadNow()` one-time request; `download(): InputStream`
- `app/src/main/java/com/mnemosyne/backup/DriveClient.kt` — wraps Drive REST API v3 via `HttpTransport`; `upload(inputStream: InputStream)`, `download(): InputStream`; uses Google Credential Manager token
- `app/src/main/java/com/mnemosyne/backup/RestoreViewModel.kt` — drives the restore flow from Settings; exposes `StateFlow<RestoreState>`
- `app/src/test/java/com/mnemosyne/backup/BackupWorkerTest.kt`
- `app/src/test/java/com/mnemosyne/backup/DriveClientTest.kt`

**Approach:**
- Archive format (per KTD9): each memory is serialized as a single-line JSON object. All lines are concatenated as bytes, then AES-256-GCM encrypted by `CryptoManager`. The encrypted bytes are uploaded as `application/octet-stream` to `appDataFolder/mnemosyne_backup.bin`.
- `PeriodicWorkRequest` interval: 1 hour (above WorkManager's 15-min floor; keeps upload frequency reasonable). Constraints: `NetworkType.UNMETERED`.
- `DriveClient` uses `GoogleCredentials` obtained from Google Credential Manager. On `401` response, refresh token and retry once.
- Restore: `BackupRepository.download()` returns the raw encrypted stream → `CryptoManager.decrypt()` → parse JSON Lines → `MemoryRepository` bulk save. Existing ObjectBox data is deleted before import.

**Test Scenarios:**
- `BackupWorkerTest`: given a mocked `MemoryRepository` with 2 memories and a mocked `DriveClient` that succeeds, `doWork()` returns `Result.success()`.
- `BackupWorkerTest`: given `DriveClient` throws an `IOException`, `doWork()` returns `Result.retry()`.
- `DriveClientTest`: `upload()` sends a PUT/POST to the correct Drive v3 endpoint with `Content-Type: application/octet-stream`.
- Restore round-trip integration test: serialize 3 memories, encrypt, decrypt, deserialize, assert all 3 match originals.

**Verification:** `./gradlew :app:test`. Manual on device: trigger backup from Settings "Backup Now" button; confirm `appDataFolder` file exists in Drive API Explorer; uninstall app, reinstall, restore, confirm memories appear.

---

### U7. Settings Screen and First-Launch Onboarding

**Goal:** Implement the Settings screen (Google sign-in status, manual backup, restore trigger) and the first-launch permission + model-download onboarding flow.

**Requirements:** R6, R13, R14.

**Files:**
- `app/src/main/java/com/mnemosyne/settings/SettingsScreen.kt` — Compose screen: Google account display, "Sign in / Sign out", "Backup Now", "Restore from Drive", model download status
- `app/src/main/java/com/mnemosyne/settings/SettingsViewModel.kt` — orchestrates `BackupRepository`, `ModelManager`, Google Credential Manager sign-in
- `app/src/main/java/com/mnemosyne/MainActivity.kt` — top-level Compose nav graph: routes through `PermissionScreen` → `ModelDownloadScreen` → `HomeScreen` based on app state

**Approach:**
- Nav graph has three guarded routes: permissions (if not granted), model download (if not ready), home. Each guard checks state from a shared `AppState` singleton.
- Google sign-in uses `CredentialManager.getCredential()` with `GetGoogleIdOption`. On success, persist the ID token; use it to obtain a Drive access token via `GoogleIdTokenCredential`.
- "Backup Now" triggers a one-time `WorkRequest` via `BackupRepository.uploadNow()` and shows a `Snackbar` on completion.

**Test Scenarios:**
- `SettingsViewModelTest`: `signIn()` calls `CredentialManager.getCredential()` and on success emits `SignInState.SignedIn`.
- `SettingsViewModelTest`: "Backup Now" enqueues a one-time `WorkRequest` with `NetworkType.UNMETERED`.
- `MainActivityTest` (Compose UI test): given permissions not granted, first composable shown is `PermissionScreen`.
- `MainActivityTest`: given permissions granted and models ready, first composable is `HomeScreen`.

**Verification:** `./gradlew :app:connectedAndroidTest` for Compose UI tests. Manual: fresh install flow navigates permission → model download → home.

---

### U8. Integration, End-to-End Testing, and Polish

**Goal:** Wire all units together, run the end-to-end capture-search-backup flow manually and via instrumented tests, fix integration seams, and apply UI polish (theming, loading states, accessibility).

**Requirements:** All R1–R14. Covers all AE1–AE6.

**Files:**
- `app/src/androidTest/java/com/mnemosyne/e2e/CaptureSearchE2ETest.kt` — Espresso/Compose UI test: record a memory, search for a term in it, assert it surfaces.
- `app/src/androidTest/java/com/mnemosyne/e2e/BackupRestoreE2ETest.kt` — requires a test Google account; skippable via `@Ignore` for CI without credentials.
- `app/src/main/res/` — Material 3 theme, app icon, color scheme.
- `app/src/main/java/com/mnemosyne/ui/theme/MnemosyneTheme.kt`

**Approach:**
- Use a `FakeMemoryRepository` in Compose UI tests to avoid ObjectBox on the test host.
- `CaptureSearchE2ETest` mocks `SttRepository` to return a canned transcript, then asserts the card appears in the feed and the same card surfaces when the search query matches the transcript text.
- Material 3 dynamic color where available (API 31+); static seed color palette on API < 31.
- Accessibility: all interactive composables have `contentDescription`; minimum touch target 48dp.

**Test Scenarios:**
- E2E capture flow: mock STT returns "dentist appointment tomorrow" → card appears in feed.
- E2E search flow: search "doctor" → card with "dentist appointment" surfaces in top 3 results (semantic similarity).
- E2E backup flow: mock Drive upload succeeds → `BackupState.Success` emitted.
- Accessibility: TalkBack navigates capture button and reads "Record memory" description.

**Verification:** `./gradlew :app:connectedAndroidTest`. Manual walkthrough of all six AEs on a physical device.

---

## Verification Contract

| Step | Command | Done signal |
|---|---|---|
| Unit tests | `./gradlew :app:test` | All tests pass; no failures |
| Instrumented tests | `./gradlew :app:connectedAndroidTest` | All Compose UI and integration tests pass on device/emulator |
| Lint | `./gradlew :app:lint` | No errors (warnings acceptable) |
| Build | `./gradlew :app:assembleDebug` | APK produced without errors |
| Manual AE walkthrough | Physical device, Android 8+ | All 6 AEs in the Product Contract pass |

No CI pipeline exists yet — the Verification Contract applies to local verification during development.

---

## Definition of Done

**Global:**
- All unit tests pass (`./gradlew :app:test`).
- All instrumented tests pass (`./gradlew :app:connectedAndroidTest`).
- Lint passes with no errors.
- All 6 Acceptance Examples verified manually on a physical device.
- No dead-end or experimental code left in the diff — only shipped code.

**Per unit:**
- U1: `SttRepository.transcribe()` returns non-empty string for a spoken audio buffer.
- U2: ObjectBox persists a `Memory` and retrieves it with the original plaintext transcript (demonstrating decrypt-on-read works).
- U3: Fresh install shows download screen; subsequent launches skip download.
- U4: Press-hold-release saves a memory; empty recording shows discard message.
- U5: Feed shows memories newest-first; search re-ranks by semantic similarity.
- U6: Background backup uploads encrypted archive on WiFi; restore imports memories to a clean install.
- U7: First-launch guard routes through permission → model download → home correctly.
- U8: E2E capture-search test passes; no regressions in any prior unit.
