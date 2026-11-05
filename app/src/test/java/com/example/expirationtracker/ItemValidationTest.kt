package com.example.expirationtracker

import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.ItemInput
import com.example.expirationtracker.domain.validation.ItemValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for Item Validation and Creation (TSK-002).
 *
 * Traceability:
 * - REQ-F-001: Item manual creation with title, expiration date, category, optional storage location
 * - REQ-F-002: Title validation (not empty, not whitespace-only)
 * - REQ-F-003: Expiration date validation (valid calendar date; past dates permitted)
 * - AC-F-001, AC-F-002, AC-F-003
 */
class ItemValidationTest {

    private lateinit var validator: ItemValidator

    @Before
    fun setUp() {
        validator = ItemValidator()
    }

    @Test
    fun creation_succeeds_with_all_valid_fields() {
        // Given valid inputs matching REQ-F-001 & AC-F-001
        val input = ItemInput(
            title = "Aspirin",
            expirationDate = "2027-01-01",
            category = Category.MEDICINE,
            storageLocation = "First Aid Kit"
        )

        // Then item creation preserves attributes
        assertEquals("Aspirin", input.title)
        assertEquals("2027-01-01", input.expirationDate)
        assertEquals(Category.MEDICINE, input.category)
        assertEquals("First Aid Kit", input.storageLocation)

        // And validation succeeds
        val result = validator.validate(input)
        assertTrue("Expected validation to succeed with valid fields", result.isValid)
        assertTrue("Expected no validation errors", result.errors.isEmpty())
    }

    @Test
    fun creation_succeeds_with_optional_storage_location_omitted() {
        // Given valid inputs without optional storage location (REQ-F-001)
        val input = ItemInput(
            title = "Milk",
            expirationDate = "2026-11-20",
            category = Category.FOOD,
            storageLocation = null
        )

        assertEquals("Milk", input.title)
        assertNull(input.storageLocation)

        val result = validator.validate(input)
        assertTrue("Expected validation to succeed when storage location is null", result.isValid)
        assertTrue("Expected no validation errors", result.errors.isEmpty())
    }

    @Test
    fun validation_succeeds_for_past_expiration_date() {
        // REQ-F-003: Dates in the past must be permitted to allow entry of already expired items
        val input = ItemInput(
            title = "Expired Cereal",
            expirationDate = "2020-05-15",
            category = Category.FOOD,
            storageLocation = "Pantry"
        )

        val result = validator.validate(input)
        assertTrue("Expected past expiration date to be valid per REQ-F-003", result.isValid)
        assertTrue("Expected no validation errors for past expiration date", result.errors.isEmpty())
    }

    @Test
    fun validation_fails_when_title_is_empty() {
        // REQ-F-002 & AC-F-002: Title cannot be empty
        val input = ItemInput(
            title = "",
            expirationDate = "2027-01-01",
            category = Category.MEDICINE,
            storageLocation = "Bathroom Cabinet"
        )

        val result = validator.validate(input)
        assertFalse("Expected validation to fail when title is empty", result.isValid)
        assertTrue(
            "Expected title error in validation results",
            result.errors.any { it.contains("title", ignoreCase = true) }
        )
    }

    @Test
    fun validation_fails_when_title_is_whitespace_only() {
        // REQ-F-002 & AC-F-002: Title cannot consist solely of whitespace characters
        val input = ItemInput(
            title = "   \t \n ",
            expirationDate = "2027-01-01",
            category = Category.CHEMICALS,
            storageLocation = "Garage"
        )

        val result = validator.validate(input)
        assertFalse("Expected validation to fail when title is whitespace-only", result.isValid)
        assertTrue(
            "Expected title error in validation results",
            result.errors.any { it.contains("title", ignoreCase = true) }
        )
    }

    @Test
    fun validation_fails_when_expiration_date_is_empty() {
        // REQ-F-003 & AC-F-003: Valid calendar date must be provided
        val input = ItemInput(
            title = "Sunscreen",
            expirationDate = "",
            category = Category.COSMETICS,
            storageLocation = "Bathroom"
        )

        val result = validator.validate(input)
        assertFalse("Expected validation to fail when expiration date is empty", result.isValid)
        assertTrue(
            "Expected date error in validation results",
            result.errors.any { it.contains("date", ignoreCase = true) }
        )
    }

    @Test
    fun validation_fails_when_expiration_date_is_invalid_format() {
        // REQ-F-003 & AC-F-003: Valid calendar date required (e.g. YYYY-MM-DD)
        val input = ItemInput(
            title = "Sunscreen",
            expirationDate = "not-a-valid-date",
            category = Category.COSMETICS,
            storageLocation = "Bathroom"
        )

        val result = validator.validate(input)
        assertFalse("Expected validation to fail when expiration date format is invalid", result.isValid)
        assertTrue(
            "Expected date error in validation results",
            result.errors.any { it.contains("date", ignoreCase = true) }
        )
    }

    @Test
    fun validation_fails_when_expiration_date_is_nonexistent_calendar_date() {
        // REQ-F-003: Must be a valid calendar date (e.g., February 30th is invalid)
        val input = ItemInput(
            title = "Bleach",
            expirationDate = "2026-02-30",
            category = Category.CHEMICALS,
            storageLocation = "Laundry Room"
        )

        val result = validator.validate(input)
        assertFalse("Expected validation to fail for nonexistent calendar date", result.isValid)
        assertTrue(
            "Expected date error in validation results",
            result.errors.any { it.contains("date", ignoreCase = true) }
        )
    }
}
