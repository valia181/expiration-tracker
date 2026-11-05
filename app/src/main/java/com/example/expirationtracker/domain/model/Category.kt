package com.example.expirationtracker.domain.model

/**
 * Predefined item categories per SRS Section 3 and REQ-F-001.
 * Strict enum: Food, Medicine, Cosmetics, Chemicals.
 */
enum class Category(val displayName: String) {
    FOOD("Food"),
    MEDICINE("Medicine"),
    COSMETICS("Cosmetics"),
    CHEMICALS("Chemicals");

    companion object {
        fun fromDisplayName(name: String): Category? =
            entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
    }
}
