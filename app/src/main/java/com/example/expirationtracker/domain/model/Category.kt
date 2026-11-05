package com.example.expirationtracker.domain.model

/**
 * Predefined item categories per SRS Section 3 and REQ-F-001.
 * Strict enum: Food, Medicine, Cosmetics, Chemicals.
 * Centralizes category parsing, validation, and database value mapping.
 */
enum class Category(val displayName: String) {
    FOOD("Food"),
    MEDICINE("Medicine"),
    COSMETICS("Cosmetics"),
    CHEMICALS("Chemicals");

    companion object {
        /**
         * Resolves a Category from either display name (e.g. "Food") or enum name (e.g. "FOOD") case-insensitively.
         * Returns null if name is null, blank, or "All".
         */
        fun fromDisplayName(name: String?): Category? {
            if (name.isNullOrBlank() || name.equals("All", ignoreCase = true)) return null
            return entries.firstOrNull {
                it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true)
            }
        }

        /**
         * Validates whether the given category string is valid (either a recognized category or "All"/null/blank).
         */
        fun isValid(name: String?): Boolean {
            if (name.isNullOrBlank() || name.equals("All", ignoreCase = true)) return true
            return fromDisplayName(name) != null
        }

        /**
         * Maps category query parameter to standard database storage representation (enum name).
         */
        fun toDbValue(name: String?): String? {
            val category = fromDisplayName(name)
            return category?.name ?: if (name.isNullOrBlank() || name.equals("All", ignoreCase = true)) null else name.trim()
        }
    }
}
