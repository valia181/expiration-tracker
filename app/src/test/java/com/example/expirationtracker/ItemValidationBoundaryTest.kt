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
 * Boundary, Edge Case, and Robustness Test Suite for Item Validation (TSK-002).
 *
 * Traceability:
 * - REQ-F-001: Item manual creation with title, expiration date, category, optional storage location
 * - REQ-F-002: Title validation (not empty, not whitespace-only)
 * - REQ-F-003: Expiration date validation (valid calendar date; past dates permitted)
 * - REQ-F-007: Strict category constraints (Food, Medicine, Cosmetics, Chemicals)
 * - openapi.yaml: ItemInput schema definitions
 */
class ItemValidationBoundaryTest {

    private lateinit var validator: ItemValidator

    @Before
    fun setUp() {
        validator = ItemValidator()
    }

    // ==========================================
    // 1. Title Boundary Scenarios
    // ==========================================

    @Test
    fun validation_succeeds_for_single_character_title() {
        // Minimum valid title length (1 non-whitespace character)
        val input = ItemInput(
            title = "A",
            expirationDate = "2027-01-01",
            category = Category.FOOD
        )
        val result = validator.validate(input)
        assertTrue("Single non-whitespace character title should be valid", result.isValid)
    }

    @Test
    fun validation_succeeds_for_extremely_long_title() {
        // Boundary: 500 characters title with diverse unicode characters
        val longTitle = "A".repeat(500)
        val input = ItemInput(
            title = longTitle,
            expirationDate = "2027-01-01",
            category = Category.MEDICINE
        )
        val result = validator.validate(input)
        assertTrue("Long title should be supported without failure", result.isValid)
    }

    @Test
    fun validation_succeeds_for_title_with_special_characters_and_emojis() {
        // Special characters, punctuation, quotes, symbols, and emojis
        val specialTitle = "Paracetamol 500mg (Extra) & Vitamin C+ #1 / 100% Organic! 💊"
        val input = ItemInput(
            title = specialTitle,
            expirationDate = "2027-01-01",
            category = Category.MEDICINE
        )
        val result = validator.validate(input)
        assertTrue("Special characters and emojis in title should be valid", result.isValid)
    }

    @Test
    fun validation_succeeds_for_title_with_leading_and_trailing_whitespace() {
        // Title has surrounding spaces but contains valid content
        val input = ItemInput(
            title = "   Baby Lotion   ",
            expirationDate = "2027-01-01",
            category = Category.COSMETICS
        )
        val result = validator.validate(input)
        assertTrue("Title with surrounded whitespace should be valid", result.isValid)
    }

    @Test
    fun validation_fails_for_various_unicode_whitespace_only_titles() {
        // Non-breaking space, tabs, newlines, carriage returns, form feeds
        val whitespaceVariations = listOf(
            "\u00A0", // non-breaking space
            "\t\n\r\u000C",
            "   \t  \n  \r  "
        )

        for (ws in whitespaceVariations) {
            val input = ItemInput(
                title = ws,
                expirationDate = "2027-01-01",
                category = Category.FOOD
            )
            val result = validator.validate(input)
            assertFalse("Title with only whitespace variation '$ws' must fail", result.isValid)
            assertTrue(
                "Error message must mention title",
                result.errors.any { it.contains("title", ignoreCase = true) }
            )
        }
    }

    // ==========================================
    // 2. Expiration Date Boundary & Calendar Scenarios
    // ==========================================

    @Test
    fun validation_succeeds_for_valid_leap_year_february_29() {
        // Valid leap year date: 2028-02-29
        val input = ItemInput(
            title = "Emergency Water Rations",
            expirationDate = "2028-02-29",
            category = Category.FOOD
        )
        val result = validator.validate(input)
        assertTrue("February 29th on leap year 2028 must be valid", result.isValid)
    }

    @Test
    fun validation_fails_for_non_leap_year_february_29() {
        // 2027 is not a leap year -> February 29 does not exist
        val input = ItemInput(
            title = "Cooking Oil",
            expirationDate = "2027-02-29",
            category = Category.FOOD
        )
        val result = validator.validate(input)
        assertFalse("February 29th on non-leap year 2027 must fail validation", result.isValid)
        assertTrue(result.errors.any { it.contains("date", ignoreCase = true) })
    }

    @Test
    fun validation_fails_for_day_31_in_30_day_months() {
        // April (04), June (06), September (09), November (11) have only 30 days
        val monthsWith30Days = listOf("2027-04-31", "2027-06-31", "2027-09-31", "2027-11-31")

        for (date in monthsWith30Days) {
            val input = ItemInput(
                title = "Item $date",
                expirationDate = date,
                category = Category.FOOD
            )
            val result = validator.validate(input)
            assertFalse("Date $date must be rejected as invalid calendar date", result.isValid)
            assertTrue(result.errors.any { it.contains("date", ignoreCase = true) })
        }
    }

    @Test
    fun validation_fails_for_out_of_range_month_or_day() {
        val outOfRangeDates = listOf(
            "2027-00-15", // Month 0
            "2027-13-15", // Month 13
            "2027-05-00", // Day 0
            "2027-05-32"  // Day 32
        )

        for (date in outOfRangeDates) {
            val input = ItemInput(
                title = "Chemical Solvent",
                expirationDate = date,
                category = Category.CHEMICALS
            )
            val result = validator.validate(input)
            assertFalse("Out of range date $date must fail validation", result.isValid)
            assertTrue(result.errors.any { it.contains("date", ignoreCase = true) })
        }
    }

    @Test
    fun validation_fails_for_date_with_slash_or_dot_separators() {
        // Must strictly adhere to YYYY-MM-DD ISO format
        val nonIsoDates = listOf(
            "2027/05/15",
            "15-05-2027",
            "15.05.2027",
            "2027.05.15",
            "05-15-2027"
        )

        for (date in nonIsoDates) {
            val input = ItemInput(
                title = "Antiseptic",
                expirationDate = date,
                category = Category.MEDICINE
            )
            val result = validator.validate(input)
            assertFalse("Non-ISO date format $date must fail validation", result.isValid)
            assertTrue(result.errors.any { it.contains("date", ignoreCase = true) })
        }
    }

    @Test
    fun validation_fails_for_date_with_timestamp_or_timezone_components() {
        // Contract requires date format 'YYYY-MM-DD', not full ISO-8601 datetime with time or Z
        val dateTimes = listOf(
            "2027-05-15T00:00:00Z",
            "2027-05-15 12:00:00",
            "2027-05-15T15:30:00+02:00"
        )

        for (dt in dateTimes) {
            val input = ItemInput(
                title = "Cosmetic Cream",
                expirationDate = dt,
                category = Category.COSMETICS
            )
            val result = validator.validate(input)
            assertFalse("DateTime with time/timezone '$dt' must fail date-only validation", result.isValid)
            assertTrue(result.errors.any { it.contains("date", ignoreCase = true) })
        }
    }

    // ==========================================
    // 3. Storage Location Edge Cases
    // ==========================================

    @Test
    fun validation_succeeds_for_empty_or_whitespace_storage_location() {
        // Storage location is optional; empty or whitespace should be accepted as omitted
        val input = ItemInput(
            title = "Floor Cleaner",
            expirationDate = "2027-08-01",
            category = Category.CHEMICALS,
            storageLocation = "   "
        )
        val result = validator.validate(input)
        assertTrue("Whitespace storage location should not invalidate the item", result.isValid)
    }

    // ==========================================
    // 4. Category Strictness Scenarios
    // ==========================================

    @Test
    fun category_fromDisplayName_matches_case_insensitively_and_rejects_unknown() {
        // Valid categories
        assertEquals(Category.FOOD, Category.fromDisplayName("food"))
        assertEquals(Category.FOOD, Category.fromDisplayName("Food"))
        assertEquals(Category.FOOD, Category.fromDisplayName("FOOD"))
        assertEquals(Category.MEDICINE, Category.fromDisplayName("medicine"))
        assertEquals(Category.COSMETICS, Category.fromDisplayName("cosmetics"))
        assertEquals(Category.CHEMICALS, Category.fromDisplayName("chemicals"))

        // Invalid categories (e.g. Toys, Electronics, Perishables)
        assertNull(Category.fromDisplayName("Toys"))
        assertNull(Category.fromDisplayName("Electronics"))
        assertNull(Category.fromDisplayName(""))
        assertNull(Category.fromDisplayName("Custom Category"))
    }
}
