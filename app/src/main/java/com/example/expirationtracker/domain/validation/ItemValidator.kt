package com.example.expirationtracker.domain.validation

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.domain.model.ErrorResponse
import com.example.expirationtracker.domain.model.ItemInput
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Encapsulates validation results including overall validity flag and collected descriptive error messages.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * Domain validator ensuring item inputs adhere strictly to business rules and data constraints:
 * - REQ-F-002 & AC-F-002: Title must be non-empty and not solely whitespace.
 * - REQ-F-003 & AC-F-003: Expiration date must be a valid calendar date in ISO-8601 format (YYYY-MM-DD). Past dates are valid.
 */
class ItemValidator {

    companion object {
        const val DATE_FORMAT_PATTERN = "uuuu-MM-dd"
        const val ERROR_TITLE_EMPTY = "Validation error: Item title cannot be empty or whitespace-only."
        const val ERROR_DATE_EMPTY = "Validation error: Expiration date cannot be empty."
        const val ERROR_DATE_INVALID = "Validation error: Expiration date must be a valid calendar date in YYYY-MM-DD format."
        const val DEFAULT_VALIDATION_ERROR = "Validation error occurred."
        const val HTTP_BAD_REQUEST_STATUS = 400
        const val HTTP_BAD_REQUEST_ERROR = "Bad Request"
        private const val TAG = "ItemValidator"
    }

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern(DATE_FORMAT_PATTERN)
        .withResolverStyle(ResolverStyle.STRICT)

    /**
     * Validates an [ItemInput] payload against title non-blankness and strict ISO-8601 calendar date rules.
     *
     * @param input The [ItemInput] data to validate.
     * @return A [ValidationResult] indicating success or containing detailed failure reasons.
     */
    fun validate(input: ItemInput): ValidationResult {
        val errors = mutableListOf<String>()

        // Title validation (REQ-F-002)
        if (input.title.trim().isEmpty()) {
            errors.add(ERROR_TITLE_EMPTY)
        }

        // Expiration date validation (REQ-F-003)
        if (input.expirationDate.isBlank()) {
            errors.add(ERROR_DATE_EMPTY)
        } else {
            try {
                // Strict calendar date parsing prevents impossible dates like 2026-02-30
                LocalDate.parse(input.expirationDate.trim(), dateFormatter)
            } catch (e: DateTimeParseException) {
                errors.add(ERROR_DATE_INVALID)
            }
        }

        val result = ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )

        if (result.isValid) {
            SystemLogger.info(TAG, "Validation succeeded for item '${input.title}'.")
        } else {
            SystemLogger.warn(TAG, "Validation failed for item '${input.title}': ${errors.joinToString("; ")}")
        }

        return result
    }

    /**
     * Maps validation failure results to OpenAPI-compliant [ErrorResponse] data structure with HTTP 400 Bad Request semantics.
     *
     * @param validationResult The [ValidationResult] containing failure error messages.
     * @param path The API request path associated with the validation error.
     * @param timestamp The ISO-8601 timestamp string (defaults to current instant).
     * @return An [ErrorResponse] populated with HTTP 400 status and consolidated error messages.
     */
    fun toErrorResponse(
        validationResult: ValidationResult,
        path: String,
        timestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    ): ErrorResponse {
        val errorMessage = if (validationResult.errors.isNotEmpty()) {
            validationResult.errors.joinToString("; ")
        } else {
            DEFAULT_VALIDATION_ERROR
        }

        val response = ErrorResponse(
            timestamp = timestamp,
            status = HTTP_BAD_REQUEST_STATUS,
            error = HTTP_BAD_REQUEST_ERROR,
            message = errorMessage,
            path = path
        )

        SystemLogger.info(
            TAG,
            "Generated ErrorResponse for path '$path': status=$HTTP_BAD_REQUEST_STATUS, message='${response.message}'"
        )
        return response
    }
}
