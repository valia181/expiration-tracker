# Coding Conventions & Style Guide

This document establishes the official coding standards and style guidelines for the **Expiration Tracker** Android project. All developers contributing to this codebase must adhere strictly to these guidelines to ensure code readability, maintainability, and consistency across all layers.

---

## 1. Base Standards

This project strictly adheres to established industry standards:
- **Kotlin Language:** Official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) by JetBrains.
- **Android Development:** Official [Android Kotlin Style Guide](https://developer.android.com/kotlin/style) by Google.

---

## 2. Naming Conventions

Consistent naming makes code self-documenting and easy to navigate.

| Construct / Component | Case Style | Example |
| :--- | :--- | :--- |
| **Classes, Interfaces, Objects, Enums** | `PascalCase` | `ItemRepository`, `ExpirationDatabase`, `ItemEntity` |
| **Jetpack Compose UI Functions** | `PascalCase` | `ItemListScreen`, `ItemCard`, `AddEditItemDialog` |
| **Functions, Properties, Local Variables** | `camelCase` | `itemName`, `calculateExpirationDate()`, `isLoading` |
| **Constants (`const val`)** | `UPPER_SNAKE_CASE` | `MAX_NOTIFICATION_RETRIES`, `DEFAULT_TIMEOUT_MS` |
| **Package Names** | `lowercase` (no underscores) | `com.example.expirationtracker.data.repository` |

### Examples:
```kotlin
const val MAX_RETRIES = 3

class ItemRepositoryImpl(
    private val itemDao: ItemDao
) {
    val activeItemsFlow: Flow<List<ItemEntity>> = itemDao.getActives()
}

@Composable
fun ItemCard(
    itemName: String,
    daysUntilExpiration: Int
) {
    // Component implementation
}
```

---

## 3. Formatting & Style Rules

- **Indentation:** Use **4 spaces** for indentation. **Tabs are strictly prohibited.**
- **Maximum Line Length:** Keep lines under **120 characters**. Wrap long parameter lists or expressions gracefully.
- **Braces:** 
  - Opening brace `{` on the **same line** as the declaration.
  - Closing brace `}` on a **new line** aligned with the start of the block/statement.
- **Imports:** 
  - Group imports logically (e.g., standard library, Android/Jetpack libraries, third-party libraries, project packages).
  - **Wildcard imports (`import foo.*`) are strictly forbidden.** Every imported symbol must be explicitly declared.

### Example:
```kotlin
fun processItemExpiration(
    itemId: Long,
    notificationManager: NotificationManager
) {
    if (itemId > 0L) {
        notificationManager.sendExpirationAlert(itemId)
    } else {
        logInvalidId(itemId)
    }
}
```

---

## 4. Architectural & File Naming Conventions

To maintain a clean separation of concerns following modern Android architecture (MVVM/Clean Architecture), files and classes must use explicit architectural suffixes corresponding to their layer:

### Data Layer (`com.example.expirationtracker.data`)
- **DAO Interfaces:** `*Dao` (e.g., [ItemDao](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/data/ItemDao.kt))
- **Database Entities:** `*Entity` (e.g., [ItemEntity](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/data/ItemEntity.kt))
- **Repositories:** `*Repository` (e.g., [ItemRepository](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/data/ItemRepository.kt))

### Domain Layer (`com.example.expirationtracker.domain`)
- **Use Cases / Interactors:** `*UseCase` (e.g., `GetExpiringItemsUseCase`)

### UI Layer (`com.example.expirationtracker.ui` / `com.example.expirationtracker.notification`)
- **ViewModels:** `*ViewModel` (e.g., `ItemViewModel`, `SettingsViewModel`)
- **Screens (Compose):** `*Screen` (e.g., `HomeScreen`, `DetailScreen`)
- **Dialogs / Modals:** `*Dialog` (e.g., `DeleteItemDialog`, `EditItemDialog`)
- **Workers / Background Tasks:** `*Worker` (e.g., [ExpirationNotificationWorker](file:///C:/Users/HelpDesk/Desktop/expiration-tracker/expiration-tracker/app/src/main/java/com/example/expirationtracker/notification/ExpirationNotificationWorker.kt))

### Testing (`src/test` / `src/androidTest`)
- **Test Files:** `*Test` (e.g., `ItemRepositoryTest`, `ItemViewModelTest`)
