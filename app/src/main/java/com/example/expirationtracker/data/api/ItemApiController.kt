package com.example.expirationtracker.data.api

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.repository.ItemRepository
import com.example.expirationtracker.domain.calculator.StatusColorCalculator
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.ErrorResponse
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.ItemInput
import com.example.expirationtracker.domain.usecase.GetSortedItemsWithStatusUseCase
import com.example.expirationtracker.domain.validation.ItemValidator
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Controller simulating the REST API endpoints specified in openapi.yaml:
 * - POST /api/v1/items
 * - GET /api/v1/items
 * - DELETE /api/v1/items/{id}
 *
 * Clean Architecture Guarantee:
 * - Completely decoupled from SQLite database details (communicates strictly via ItemRepository).
 * - Standardized HTTP status codes via HttpStatus object.
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
    }

    // In-memory backing store for testing / standalone usage if repository is not injected
    private val inMemoryItems = mutableListOf<Item>()

    /**
     * POST /api/v1/items: Create a new item.
     * Consumes ItemInput and returns ApiResponse(201, Item) or ApiResponse(400, ErrorResponse).
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
     * GET /api/v1/items: Retrieve all items sorted chronologically by expiration date.
     * Consumes optional search and category query parameters.
     * Returns ApiResponse(200, List<Item>) or ApiResponse(400, ErrorResponse).
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
     * DELETE /api/v1/items/{id}: Delete an item by ID.
     * Consumes item id path parameter.
     * Returns ApiResponse(204, Unit) on success or ApiResponse(404, ErrorResponse) if missing.
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
}
