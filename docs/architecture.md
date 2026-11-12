# Technical Architecture Guide

This document provides a comprehensive technical overview of the **Expiration Tracker** Android application architecture, component responsibilities, technology stack, and internal API surface.

---

## 1. High-Level Architecture

The application is structured following **Clean Architecture** principles and the **MVVM (Model-View-ViewModel)** design pattern. This ensures strict separation of concerns, testability, and maintainability across all layers:

- **UI Layer (Presentation):** Jetpack Compose declarative UI screens driven reactively by [ItemListViewModel](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/ui/viewmodel/ItemListViewModel.kt) state flows (`StateFlow<ItemListUiState>`).
- **Domain Layer (Business Logic):** Pure Kotlin business models, use cases ([GetSortedItemsWithStatusUseCase](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/usecase/GetSortedItemsWithStatusUseCase.kt)), validation rules ([ItemValidator](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/validation/ItemValidator.kt)), and status calculation algorithms ([StatusColorCalculator](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/calculator/StatusColorCalculator.kt)). Completely free of Android framework dependencies.
- **Data Layer:** Abstracted data access via [ItemRepository](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/data/repository/ItemRepository.kt), concrete SQLite DAOs ([ItemDao](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/data/local/dao/ItemDao.kt), [SettingsDao](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/data/local/dao/SettingsDao.kt)), and the simulated REST API controller ([ItemApiController](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/data/api/ItemApiController.kt)).

---

## 2. Technology Stack

- **Language:** Kotlin 2.0+ with Coroutines and Flows for asynchronous reactive programming.
- **UI Framework:** Jetpack Compose with Material 3 design guidelines.
- **Local Persistence:** Native Android SQLite (`SQLiteOpenHelper`) with ACID transactional integrity.
- **Background Processing:** Android WorkManager (`CoroutineWorker`) for reliable persistent notification checks.
- **Serialization:** Android native `org.json` for database backup export/import via Storage Access Framework (SAF).
- **Testing:** JUnit 4, AndroidX Test, Espresso, and WorkManager testing framework.

---

## 3. Component Breakdown

### 3.1 Data Layer (`com.example.expirationtracker.data`)
- **`ItemDao` & `SettingsDao`**: Encapsulate raw SQL queries, inserts, updates, deletes, and transactional block execution against SQLite (`ExpirationTrackerDbHelper`).
- **`ItemRepository` / `ItemRepositoryImpl`**: Provides a clean asynchronous repository boundary (`Dispatchers.IO`) integrating data validation, mapping ([ItemMapper](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/data/mapper/ItemMapper.kt)), and database operations.
- **`ItemApiController`**: Simulates the REST API contract defined in [openapi.yaml](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/spec/openapi.yaml), handling HTTP request routing, input validation, and returning standardized `ApiResponse` objects.

### 3.2 Domain Layer (`com.example.expirationtracker.domain`)
- **Domain Models**: Immutable data classes representing business entities ([Item](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/model/Item.kt), [ItemInput](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/model/ItemInput.kt), [Category](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/model/Category.kt), [Settings](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/model/Settings.kt), [BackupData](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/model/BackupData.kt), [StatusColor](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/model/StatusColor.kt)).
- **Use Cases**: Encapsulate complex business operations, such as sorting items chronologically and computing dynamic status colors ([GetSortedItemsWithStatusUseCase](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/usecase/GetSortedItemsWithStatusUseCase.kt)).
- **Validators & Calculators**: [ItemValidator](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/validation/ItemValidator.kt) enforces title non-blankness and strict ISO-8601 calendar date rules. [StatusColorCalculator](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/domain/calculator/StatusColorCalculator.kt) computes Red, Orange, and Green statuses based on global notification lead times.

### 3.3 UI Layer (`com.example.expirationtracker.ui`)
- **`ItemListViewModel`**: Manages UI state ([ItemListUiState]), handles user actions, coordinates repository calls, and triggers sorting and filtering.
- **Jetpack Compose Screens & Dialogs**: [ItemListScreen](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/ui/screen/ItemListScreen.kt), [ItemCard](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/ui/screen/ItemCard.kt), [AddItemDialog](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/ui/screen/AddItemDialog.kt), and [SettingsDialog](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/ui/screen/SettingsDialog.kt).

### 3.4 Notification Layer (`com.example.expirationtracker.notification`)
- **`ExpirationNotificationWorker`**: WorkManager background worker that evaluates item expirations against global lead time settings and triggers high-priority unique push notifications.
- **`NotificationScheduler`**: Enqueues persistent daily periodic work requests.

---

## 4. Internal API Surface (`ItemApiController`)

The application exposes a simulated internal REST API surface via [ItemApiController](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/data/api/ItemApiController.kt) mapped to [openapi.yaml](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/spec/openapi.yaml):

| Endpoint | HTTP Method | Description | Response Status Codes |
| :--- | :--- | :--- | :--- |
| `/api/v1/items` | `GET` | Retrieve items sorted chronologically (supports search & category filters) | `200 OK`, `400 Bad Request` |
| `/api/v1/items` | `POST` | Create a new tracked item | `201 Created`, `400 Bad Request` |
| `/api/v1/items/{id}` | `PUT` | Update an existing item | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `/api/v1/items/{id}` | `DELETE` | Permanently delete an item | `204 No Content`, `404 Not Found` |
| `/api/v1/settings` | `GET` | Retrieve global notification lead time settings | `200 OK` |
| `/api/v1/settings` | `PUT` | Update global notification lead time settings (`1, 3, 7, 14` days) | `200 OK`, `400 Bad Request` |
| `/api/v1/backups/export` | `POST` | Export database state into a JSON backup payload | `200 OK`, `500 Internal Server Error` |
| `/api/v1/backups/import` | `POST` | Import database state from a JSON backup payload | `200 OK`, `400 Bad Request` |
