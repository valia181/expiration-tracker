package com.example.expirationtracker.domain.validation

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.domain.model.ItemInput
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Validation result containing status and localized/descriptive error messages.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * Validates Item input per requirements:
 * - REQ-F-002 & AC-F-002: Title must be non-empty and not solely whitespace.
 * - REQ-F-003 & AC-F-003: Expiration date must be a valid calendar date in ISO-8601 format (YYYY-MM-DD). Past dates are valid.
 */
class ItemValidator {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("uuuu-MM-dd")
        .withResolverStyle(ResolverStyle.STRICT)

    fun validate(input: ItemInput): ValidationResult {
        val errors = mutableListOf<String>()

        // Title validation (REQ-F-002)
        if (input.title.trim().isEmpty()) {
            errors.add("Validation error: Item title cannot be empty or whitespace-only.")
        }

        // Expiration date validation (REQ-F-003)
        if (input.expirationDate.isBlank()) {
            errors.add("Validation error: Expiration date cannot be empty.")
        } else {
            try {
                // Strict calendar date parsing prevents impossible dates like 2026-02-30
                LocalDate.parse(input.expirationDate.trim(), dateFormatter)
            } catch (e: DateTimeParseException) {
                errors.add("Validation error: Expiration date must be a valid calendar date in YYYY-MM-DD format.")
            }
        }

        val result = ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )

        if (result.isValid) {
            SystemLogger.info("ItemValidator", "Validation succeeded for item '${input.title}'.")
        } else {
            SystemLogger.warn("ItemValidator", "Validation failed for item '${input.title}': ${errors.joinToString("; ")}")
        }

        return result
    }
}
