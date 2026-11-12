package com.example.expirationtracker.data.api

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.repository.ItemRepository
import com.example.expirationtracker.domain.calculator.StatusColorCalculator
import com.example.expirationtracker.domain.model.BackupData
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.ErrorResponse
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.ItemInput
import com.example.expirationtracker.domain.model.Settings
import com.example.expirationtracker.domain.usecase.GetSortedItemsWithStatusUseCase
import com.example.expirationtracker.domain.validation.ItemValidator
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Controller simulating REST API endpoints specified in `openapi.yaml`:
 * - `POST /api/v1/items`
 * - `GET /api/v1/items`
 * - `PUT /api/v1/items/{id}`
 * - `DELETE /api/v1/items/{id}`
 * - `GET /api/v1/settings`
 * - `PUT /api/v1/settings`
 * - `POST /api/v1/backups/export`
 * - `POST /api/v1/backups/import`
 *
 * Clean Architecture Guarantee:
 * - Completely decoupled from SQLite database details (communicates strictly via [ItemRepository]).
 * - Enforces validation via [ItemValidator] and returns standardized HTTP status codes wrapped in [ApiResponse].
 */
class ItemApiController(
    private val repository: ItemRepository? = null,
    private val validator: ItemValidator = ItemValidator(),
    private val getSortedItemsUseCase: GetSortedItemsWithStatusUseCase = GetSortedItemsWithStatusUseCase(StatusColorCalculator()),
    private val statusColorCalculator: StatusColorCalculator = StatusColorCalculator()
) {

    companion object {
        private const val TAG = "ItemApiController"
        const val PATH_ITEMS = "/api/v1/items"
        const val ALLOWED_CATEGORIES_STRING = "Food, Medicine, Cosmetics, Chemicals"
        const val DEFAULT_NOTIFICATION_LEAD_TIME_DAYS = 7
        const val ERROR_MSG_INVALID_LEAD_TIME = "Validation error: 'notificationLeadTime' must be one of [1, 3, 7, 14]."
    }

    // In-memory backing store for testing / standalone usage if repository is not injected
    private val inMemoryItems = mutableListOf<Item>()
    private var inMemoryLeadTime = DEFAULT_NOTIFICATION_LEAD_TIME_DAYS

    /**
     * Simulates `POST /api/v1/items`: Creates a new tracked item.
     *
     * @param input The [ItemInput] payload containing title, expiration date, category, and optional storage location.
     * @return An [ApiResponse] containing HTTP 201 Created with the persisted [Item] on success, or HTTP 400 Bad Request with [ErrorResponse] if validation fails.
     */
    suspend fun createItem(input: ItemInput): ApiResponse<Item> {
        val method = "POST"
        val path = PATH_ITEMS
        SystemLogger.info(TAG, "Incoming API request: $method $path, body=$input")

        // 1. Validation (delegated to ItemValidator)
        val validationResult = validator.validate(input)
        if (!validationResult.isValid) {
            val errorResponse = validator.toErrorResponse(validationResult, path)
            SystemLogger.warn(
                TAG,
                "Outgoing API response: $method $path -> HTTP ${errorResponse.status} ${HttpStatus.REASON_BAD_REQUEST}: ${errorResponse.message}"
            )
            return ApiResponse(status = HttpStatus.BAD_REQUEST, error = errorResponse)
        }

        // 2. Dynamic status color calculation
        val today = LocalDate.now()
        val parsedDate = try {
            LocalDate.parse(input.expirationDate.trim())
        } catch (_: Exception) {
            today
        }
        val statusColor = statusColorCalculator.calculateStatus(
            expirationDate = parsedDate,
            currentDate = today,
            leadTimeDays = DEFAULT_NOTIFICATION_LEAD_TIME_DAYS
        )

        val domainItem = Item(
            id = UUID.randomUUID().toString(),
            title = input.title.trim(),
            expirationDate = input.expirationDate.trim(),
            category = input.category,
            storageLocation = input.storageLocation?.trim()?.ifBlank { null },
            statusColor = statusColor
        )

        // 3. Persistence (delegated exclusively to ItemRepository)
        return if (repository != null) {
            val result = repository.createItem(input)
            if (result.isFailure) {
                val errorResponse = ErrorResponse(
                    timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                    status = HttpStatus.BAD_REQUEST,
                    error = HttpStatus.REASON_BAD_REQUEST,
                    message = result.exceptionOrNull()?.message ?: "Item creation failed",
                    path = path
                )
                SystemLogger.error(TAG, "Outgoing API response: $method $path -> HTTP ${HttpStatus.BAD_REQUEST} error")
                ApiResponse(status = HttpStatus.BAD_REQUEST, error = errorResponse)
            } else {
                val stored = result.getOrNull() ?: domainItem
                val responseItem = stored.copy(statusColor = statusColor)
                SystemLogger.info(TAG, "Outgoing API response: $method $path -> HTTP ${HttpStatus.CREATED} ${HttpStatus.REASON_CREATED} (ID=${responseItem.id})")
                ApiResponse(status = HttpStatus.CREATED, data = responseItem)
            }
        } else {
            inMemoryItems.add(domainItem)
            SystemLogger.info(TAG, "Outgoing API response: $method $path -> HTTP ${HttpStatus.CREATED} ${HttpStatus.REASON_CREATED} (ID=${domainItem.id})")
            ApiResponse(status = HttpStatus.CREATED, data = domainItem)
        }
    }

    /**
     * Simulates `PUT /api/v1/items/{id}`: Updates an existing item record.
     *
     * @param id The unique string identifier of the item to update.
     * @param input The updated [ItemInput] payload.
     * @return An [ApiResponse] containing HTTP 200 OK with the updated [Item], HTTP 400 Bad Request for validation errors, or HTTP 404 Not Found if the item ID does not exist.
     */
    suspend fun updateItem(id: String, input: ItemInput): ApiResponse<Item> {
        val method = "PUT"
        val path = "$PATH_ITEMS/$id"
        SystemLogger.info(TAG, "Incoming API request: $method $path, body=$input")

        // 1. Validation (delegated to ItemValidator)
        val validationResult = validator.validate(input)
        if (!validationResult.isValid) {
            val errorResponse = validator.toErrorResponse(validationResult, path)
            SystemLogger.warn(
                TAG,
                "Outgoing API response: $method $path -> HTTP ${errorResponse.status} ${HttpStatus.REASON_BAD_REQUEST}: ${errorResponse.message}"
            )
            return ApiResponse(status = HttpStatus.BAD_REQUEST, error = errorResponse)
        }

        // 2. Dynamic status color calculation
        val leadTime = if (repository != null) repository.getLeadTime() else inMemoryLeadTime
        val today = LocalDate.now()
        val parsedDate = try {
            LocalDate.parse(input.expirationDate.trim())
        } catch (_: Exception) {
            today
        }
        val statusColor = statusColorCalculator.calculateStatus(
            expirationDate = parsedDate,
            currentDate = today,
            leadTimeDays = leadTime
        )

        val domainItem = Item(
            id = id,
            title = input.title.trim(),
            expirationDate = input.expirationDate.trim(),
            category = input.category,
            storageLocation = input.storageLocation?.trim()?.ifBlank { null },
            statusColor = statusColor
        )

        // 3. Persistence (delegated exclusively to ItemRepository)
        return if (repository != null) {
            val result = repository.updateItem(id, input)
            if (result.isFailure) {
                val ex = result.exceptionOrNull()
                val isNotFound = ex is NoSuchElementException || (ex?.message?.contains("not found", ignoreCase = true) == true)
                val status = if (isNotFound) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
                val errorReason = if (isNotFound) HttpStatus.REASON_NOT_FOUND else HttpStatus.REASON_BAD_REQUEST
                val errorMessage = ex?.message ?: if (isNotFound) "Item with ID '$id' was not found." else "Item update failed"

                val errorResponse = ErrorResponse(
                    timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                    status = status,
                    error = errorReason,
                    message = errorMessage,
                    path = path
                )
                SystemLogger.warn(TAG, "Outgoing API response: $method $path -> HTTP $status $errorReason: $errorMessage")
                ApiResponse(status = status, error = errorResponse)
            } else {
                val stored = result.getOrNull() ?: domainItem
                val responseItem = stored.copy(statusColor = statusColor)
                SystemLogger.info(TAG, "Outgoing API response: $method $path -> HTTP ${HttpStatus.OK} ${HttpStatus.REASON_OK} (ID=${responseItem.id})")
                ApiResponse(status = HttpStatus.OK, data = responseItem)
            }
        } else {
            val index = inMemoryItems.indexOfFirst { it.id == id }
            if (index >= 0) {
                inMemoryItems[index] = domainItem
                SystemLogger.info(TAG, "Outgoing API response: $method $path -> HTTP ${HttpStatus.OK} ${HttpStatus.REASON_OK} (ID=${domainItem.id})")
                ApiResponse(status = HttpStatus.OK, data = domainItem)
            } else {
                val errorMessage = "Item with ID '$id' was not found."
                val errorResponse = ErrorResponse(
                    timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                    status = HttpStatus.NOT_FOUND,
                    error = HttpStatus.REASON_NOT_FOUND,
                    message = errorMessage,
                    path = path
                )
                SystemLogger.warn(TAG, "Outgoing API response: $method $path -> HTTP ${HttpStatus.NOT_FOUND} ${HttpStatus.REASON_NOT_FOUND}: $errorMessage")
                ApiResponse(status = HttpStatus.NOT_FOUND, error = errorResponse)
            }
        }
    }

    /**
     * Simulates `GET /api/v1/items`: Retrieves all items sorted chronologically by expiration date.
     *
     * @param search Optional keyword query to filter items by title matching.
     * @param category Optional category filter ("Food", "Medicine", "Cosmetics", "Chemicals", "All", or null).
     * @return An [ApiResponse] containing HTTP 200 OK with a sorted list of [Item] instances, or HTTP 400 Bad Request if the category query parameter is invalid.
     */
    suspend fun getItems(
        search: String? = null,
        category: String? = null
    ): ApiResponse<List<Item>> {
        val method = "GET"
        val path = PATH_ITEMS
        SystemLogger.info(
            TAG,
            "Incoming API request: $method $path, params={search='$search', category='$category'}"
        )

        // 1. Validate Category query parameter per openapi.yaml
        var categoryFilter: Category? = null
        if (!category.isNullOrBlank() && !category.equals("All", ignoreCase = true)) {
            categoryFilter = Category.fromDisplayName(category)
            if (categoryFilter == null) {
                val errorMessage =
                    "Validation error: Invalid category '$category'. Allowed categories are $ALLOWED_CATEGORIES_STRING."
                val errorResponse = ErrorResponse(
                    timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                    status = HttpStatus.BAD_REQUEST,
                    error = HttpStatus.REASON_BAD_REQUEST,
                    message = errorMessage,
                    path = path
                )
                SystemLogger.warn(
                    TAG,
                    "Outgoing API response: $method $path -> HTTP ${HttpStatus.BAD_REQUEST} ${HttpStatus.REASON_BAD_REQUEST}: $errorMessage"
                )
                return ApiResponse(status = HttpStatus.BAD_REQUEST, error = errorResponse)
            }
        }

        // 2. Fetch items exclusively via repository or memory abstraction
        val rawItems: List<Item> = if (repository != null) {
            repository.getAllItems(
                search = search,
                category = categoryFilter?.name ?: if (category.equals("All", ignoreCase = true)) null else category
            )
        } else {
            inMemoryItems.toList()
        }

        // 3. Apply search and category filtering if inMemory
        var filteredItems = rawItems
        if (repository == null) {
            if (!search.isNullOrBlank()) {
                filteredItems = filteredItems.filter { it.title.contains(search, ignoreCase = true) }
            }
            if (categoryFilter != null) {
                filteredItems = filteredItems.filter { it.category == categoryFilter }
            }
        }

        // 4. Sort chronologically and assign dynamic statusColor (delegated to UseCase)
        val processedItems = getSortedItemsUseCase.execute(
            items = filteredItems,
            currentDate = LocalDate.now(),
            leadTimeDays = DEFAULT_NOTIFICATION_LEAD_TIME_DAYS
        )

        SystemLogger.info(
            TAG,
            "Outgoing API response: $method $path -> HTTP ${HttpStatus.OK} ${HttpStatus.REASON_OK} (${processedItems.size} items returned)"
        )
        return ApiResponse(status = HttpStatus.OK, data = processedItems)
    }

    /**
     * Simulates `DELETE /api/v1/items/{id}`: Permanently deletes an item record by ID.
     *
     * @param id The unique string identifier of the item to delete.
     * @return An [ApiResponse] with HTTP 204 No Content on success, or HTTP 404 Not Found with [ErrorResponse] if the item ID is missing.
     */
    suspend fun deleteItem(id: String): ApiResponse<Unit> {
        val method = "DELETE"
        val path = "$PATH_ITEMS/$id"
        SystemLogger.info(TAG, "Incoming API request: $method $path, id='$id'")

        val deleted: Boolean = if (repository != null) {
            repository.deleteItem(id)
        } else {
            val existed = inMemoryItems.any { it.id == id }
            if (existed) {
                inMemoryItems.removeAll { it.id == id }
            }
            existed
        }

        return if (deleted) {
            SystemLogger.info(
                TAG,
                "Outgoing API response: $method $path -> HTTP ${HttpStatus.NO_CONTENT} ${HttpStatus.NO_CONTENT}"
            )
            ApiResponse(status = HttpStatus.NO_CONTENT)
        } else {
            val errorMessage = "Item with ID '$id' was not found."
            val errorResponse = ErrorResponse(
                timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                status = HttpStatus.NOT_FOUND,
                error = HttpStatus.REASON_NOT_FOUND,
                message = errorMessage,
                path = path
            )
            SystemLogger.warn(
                TAG,
                "Outgoing API response: $method $path -> HTTP ${HttpStatus.NOT_FOUND} ${HttpStatus.REASON_NOT_FOUND}: $errorMessage"
            )
            ApiResponse(status = HttpStatus.NOT_FOUND, error = errorResponse)
        }
    }

    /**
     * Simulates `GET /api/v1/settings`: Retrieves global configuration settings.
     *
     * @return An [ApiResponse] containing HTTP 200 OK with current [Settings].
     */
    suspend fun getSettings(): ApiResponse<Settings> {
        val method = "GET"
        val path = "/api/v1/settings"
        SystemLogger.info(TAG, "Incoming API request: $method $path")

        val leadTime = if (repository != null) {
            repository.getLeadTime()
        } else {
            inMemoryLeadTime
        }

        val settings = Settings(notificationLeadTime = leadTime)
        SystemLogger.info(
            TAG,
            "Outgoing API response: $method $path -> HTTP ${HttpStatus.OK} ${HttpStatus.REASON_OK} (leadTime=$leadTime)"
        )
        return ApiResponse(status = HttpStatus.OK, data = settings)
    }

    /**
     * Simulates `PUT /api/v1/settings`: Updates global configuration settings.
     *
     * @param settings The [Settings] object containing the new notification lead time. Must be 1, 3, 7, or 14 days.
     * @return An [ApiResponse] containing HTTP 200 OK with updated [Settings], or HTTP 400 Bad Request if the lead time value is unsupported.
     */
    suspend fun updateSettings(settings: Settings): ApiResponse<Settings> {
        val method = "PUT"
        val path = "/api/v1/settings"
        SystemLogger.info(TAG, "Incoming API request: $method $path, body=$settings")

        if (!Settings.isValidLeadTime(settings.notificationLeadTime)) {
            val errorMessage = ERROR_MSG_INVALID_LEAD_TIME
            val errorResponse = ErrorResponse(
                timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                status = HttpStatus.BAD_REQUEST,
                error = HttpStatus.REASON_BAD_REQUEST,
                message = errorMessage,
                path = path
            )
            SystemLogger.warn(
                TAG,
                "Outgoing API response: $method $path -> HTTP ${HttpStatus.BAD_REQUEST} ${HttpStatus.REASON_BAD_REQUEST}: $errorMessage"
            )
            return ApiResponse(status = HttpStatus.BAD_REQUEST, error = errorResponse)
        }

        val success = if (repository != null) {
            repository.saveLeadTime(settings.notificationLeadTime).isSuccess
        } else {
            inMemoryLeadTime = settings.notificationLeadTime
            true
        }

        return if (success) {
            SystemLogger.info(
                TAG,
                "Outgoing API response: $method $path -> HTTP ${HttpStatus.OK} ${HttpStatus.REASON_OK} (leadTime=${settings.notificationLeadTime})"
            )
            ApiResponse(status = HttpStatus.OK, data = settings)
        } else {
            val errorResponse = ErrorResponse(
                timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                status = HttpStatus.BAD_REQUEST,
                error = HttpStatus.REASON_BAD_REQUEST,
                message = "Failed to save settings",
                path = path
            )
            SystemLogger.error(TAG, "Outgoing API response: $method $path -> HTTP ${HttpStatus.BAD_REQUEST} error")
            ApiResponse(status = HttpStatus.BAD_REQUEST, error = errorResponse)
        }
    }

    /**
     * Simulates `POST /api/v1/backups/export`: Serializes database state into a backup export payload.
     *
     * @return An [ApiResponse] containing HTTP 200 OK with [BackupData], or HTTP 500 Internal Server Error if export fails.
     */
    suspend fun exportBackup(): ApiResponse<BackupData> {
        val method = "POST"
        val path = "/api/v1/backups/export"
        SystemLogger.info(TAG, "Incoming API request: $method $path")

        return try {
            val backupData = if (repository != null) {
                repository.exportBackupData()
            } else {
                BackupData(
                    settings = Settings(notificationLeadTime = inMemoryLeadTime),
                    items = inMemoryItems.toList()
                )
            }
            SystemLogger.info(
                TAG,
                "Outgoing API response: $method $path -> HTTP ${HttpStatus.OK} ${HttpStatus.REASON_OK} (${backupData.items.size} items exported)"
            )
            ApiResponse(status = HttpStatus.OK, data = backupData)
        } catch (e: Exception) {
            val errorResponse = ErrorResponse(
                timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                error = "Internal Server Error",
                message = "Failed to generate database backup due to a file system error.",
                path = path
            )
            SystemLogger.error(TAG, "Outgoing API response: $method $path -> HTTP 500 Internal Server Error", e)
            ApiResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, error = errorResponse)
        }
    }

    /**
     * Simulates `POST /api/v1/backups/import`: Restores database state from a backup import payload.
     *
     * @param backupData The [BackupData] payload containing settings and item records to restore.
     * @return An [ApiResponse] containing HTTP 200 OK with a success message, or HTTP 400 Bad Request if backup schema validation or transaction restoration fails.
     */
    suspend fun importBackup(backupData: BackupData?): ApiResponse<String> {
        val method = "POST"
        val path = "/api/v1/backups/import"
        SystemLogger.info(TAG, "Incoming API request: $method $path")

        // 1. Validate required backup schema fields
        if (backupData == null || backupData.settings == null || backupData.items == null) {
            val errorMessage = "Invalid backup file: missing required 'settings' or 'items' object."
            val errorResponse = ErrorResponse(
                timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                status = HttpStatus.BAD_REQUEST,
                error = HttpStatus.REASON_BAD_REQUEST,
                message = errorMessage,
                path = path
            )
            SystemLogger.warn(TAG, "Outgoing API response: $method $path -> HTTP 400 Bad Request: $errorMessage")
            return ApiResponse(status = HttpStatus.BAD_REQUEST, error = errorResponse)
        }

        if (!Settings.isValidLeadTime(backupData.settings.notificationLeadTime)) {
            val errorMessage = "Invalid backup file: notificationLeadTime '${backupData.settings.notificationLeadTime}' must be one of [1, 3, 7, 14]."
            val errorResponse = ErrorResponse(
                timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                status = HttpStatus.BAD_REQUEST,
                error = HttpStatus.REASON_BAD_REQUEST,
                message = errorMessage,
                path = path
            )
            SystemLogger.warn(TAG, "Outgoing API response: $method $path -> HTTP 400 Bad Request: $errorMessage")
            return ApiResponse(status = HttpStatus.BAD_REQUEST, error = errorResponse)
        }

        // 2. Perform Import via Repository
        val success = if (repository != null) {
            repository.importBackupData(backupData)
        } else {
            inMemoryLeadTime = backupData.settings.notificationLeadTime
            inMemoryItems.clear()
            inMemoryItems.addAll(backupData.items)
            true
        }

        return if (success) {
            val successMessage = "Database successfully restored."
            SystemLogger.info(TAG, "Outgoing API response: $method $path -> HTTP 200 OK: $successMessage")
            ApiResponse(status = HttpStatus.OK, data = successMessage)
        } else {
            val errorMessage = "Invalid backup file: failed to restore database transactionally."
            val errorResponse = ErrorResponse(
                timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                status = HttpStatus.BAD_REQUEST,
                error = HttpStatus.REASON_BAD_REQUEST,
                message = errorMessage,
                path = path
            )
            SystemLogger.warn(TAG, "Outgoing API response: $method $path -> HTTP 400 Bad Request: $errorMessage")
            ApiResponse(status = HttpStatus.BAD_REQUEST, error = errorResponse)
        }
    }
}
