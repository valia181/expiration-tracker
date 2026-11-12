# Expiration Tracker

## 1. Project Title & Description
**Expiration Tracker** is a native Android application designed to help users track household supplies (food, medicine, cosmetics, and chemicals), monitor item conditions, prevent spoilage, and ensure timely disposal of expired goods. It features a chronological main view with dynamic 3-level color status indicators (Red, Orange, Green), search and category filtering, global notification lead time configuration, reliable background push notifications via WorkManager, and local database backup/restore via JSON export/import using the Android Storage Access Framework (SAF).

---

## 2. Tech Stack
- **Language:** Kotlin 2.0+ with Coroutines & Flows for asynchronous and reactive data streams.
- **UI Framework:** Jetpack Compose with Material 3 design guidelines.
- **Local Persistence:** Native Android SQLite (`SQLiteOpenHelper`) ensuring transactional integrity and 100% offline functionality.
- **Background Processing:** Android WorkManager (`CoroutineWorker`) for reliable persistent notification checks.
- **Serialization:** Android native `org.json` for database backup and restore payloads.
- **Build System:** Gradle with Android Gradle Plugin (AGP) and Kotlin DSL (`build.gradle.kts`).

---

## 3. Prerequisites (Dependencies)
To build and run this project, ensure you have the following installed:
- **Android Studio** (Koala or newer recommended).
- **Java Development Kit (JDK):** JDK 11 or JDK 17.
- **Android SDK:** 
  - **Compile SDK:** 37
  - **Target SDK:** 37
  - **Minimum SDK:** 24 (Android 7.0 Nougat)
- **Android Virtual Device (AVD)** running API level 24 or higher, or a physical Android device with USB debugging enabled.

---

## 4. Installation & Running

### Step 1: Clone the Repository
```bash
git clone https://github.com/valia181/expiration-tracker.git
cd expiration-tracker
```

### Step 2: Open and Build in Android Studio
1. Open **Android Studio**.
2. Select **Open** and choose the `expiration-tracker` root directory.
3. Wait for Gradle sync to complete. If needed, click **Sync Project with Gradle Files**.
4. Build the project via **Build > Make Project** or by running:
   ```bash
   ./gradlew assembleDebug
   ```

### Step 3: Run the App
1. Connect a physical Android device or start an Android Emulator (API 24+).
2. Click the **Run 'app'** button (green play icon) in Android Studio, or execute:
   ```bash
   ./gradlew installDebug
   ```

---

## 5. Testing

The project includes comprehensive unit tests, integration tests, contract tests, and instrumented UI tests.

### Running Unit Tests (Host Machine)
To execute all local unit tests (including API contracts, validation, domain use cases, and viewmodels):
```bash
./gradlew testDebugUnitTest
```

### Running Instrumented UI & Integration Tests (Device / Emulator)
To execute instrumented tests on a connected Android device or emulator:
```bash
./gradlew connectedAndroidTest
```

---

## 6. Project Architecture
For a detailed breakdown of the Clean Architecture layers (`data`, `domain`, `ui`, `notification`), component responsibilities, and internal API surface, please refer to the [Technical Architecture Guide](docs/architecture.md).
