package com.example.expirationtracker

import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.ErrorResponse
import com.example.expirationtracker.domain.model.ItemInput
import com.example.expirationtracker.domain.validation.ItemValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.format.DateTimeFormatter

/**
 * OpenAPI Contract Verification Unit Test (TEST-GATE).
 *
 * Verifies strict alignment with spec/openapi.yaml:
 * 1. Category enum values strictly match [Food, Medicine, Cosmetics, Chemicals].
 * 2. ItemInput schema structure matches required fields (title, expirationDate, category) and optional storageLocation.
 * 3. Validation errors accurately populate the ErrorResponse model with HTTP 400 status semantics matching openapi.yaml.
 */
class OpenApiContractVerificationTest {

    @Test
    fun category_enum_strictly_matches_openapi_specification() {
        // openapi.yaml: enum: [Food, Medicine, Cosmetics, Chemicals]
        val expectedOpenApiCategories = listOf("Food", "Medicine", "Cosmetics", "Chemicals")

        // 1. Enum display representations must cover exactly the OpenAPI set
        val actualDisplayNames = Category.entries.map { it.displayName }
        assertEquals(
            "Category displayName list must match openapi.yaml enum exactly in values and count",
            expectedOpenApiCategories,
            actualDisplayNames
        )

        // 2. Strict enum identifier mapping
        val expectedEnumNames = listOf("FOOD", "MEDICINE", "COSMETICS", "CHEMICALS")
        val actualEnumNames = Category.entries.map { it.name }
        assertEquals(
            "Category enum constants must match expected domain mapping",
            expectedEnumNames,
            actualEnumNames
        )

        // 3. Exact count check
        assertEquals("Category must have exactly 4 items per OpenAPI spec", 4, Category.entries.size)
    }

    @Test
    fun itemInput_schema_structure_matches_openapi_required_and_optional_properties() {
        // openapi.yaml components/schemas/ItemInput:
        // required: [title, expirationDate, category]
        // properties: title (string), expirationDate (string/date), category (string enum), storageLocation (string optional)

        val constructor = ItemInput::class.java.constructors.firstOrNull()
        assertNotNull("ItemInput must have a constructor", constructor)

        // Verify fields exist with proper types matching openapi schema
        val fields = ItemInput::class.java.declaredFields.associate { it.name to it.type }

        assertTrue("ItemInput must contain 'title' field", fields.containsKey("title"))
        assertEquals(String::class.java, fields["title"])

        assertTrue("ItemInput must contain 'expirationDate' field", fields.containsKey("expirationDate"))
        assertEquals(String::class.java, fields["expirationDate"])

        assertTrue("ItemInput must contain 'category' field", fields.containsKey("category"))
        assertEquals(Category::class.java, fields["category"])

        assertTrue("ItemInput must contain 'storageLocation' field", fields.containsKey("storageLocation"))
        assertEquals(String::class.java, fields["storageLocation"])

        // Check instance creation verifying required vs optional semantics
        val inputWithoutStorage = ItemInput(
            title = "Aspirin",
            expirationDate = "2027-01-01",
            category = Category.MEDICINE
        )
        assertNull("storageLocation must default to null when omitted (optional in OpenAPI)", inputWithoutStorage.storageLocation)

        val inputWithStorage = ItemInput(
            title = "Aspirin",
            expirationDate = "2027-01-01",
            category = Category.MEDICINE,
            storageLocation = "Cabinet"
        )
        assertEquals("Cabinet", inputWithStorage.storageLocation)
    }

    @Test
    fun validation_errors_accurately_populate_errorResponse_model_with_http_400_semantics() {
        // openapi.yaml POST /items 400 response:
        // schema: ErrorResponse
        // example:
        //   timestamp: "2026-10-09T17:00:00Z"
        //   status: 400
        //   error: "Bad Request"
        //   message: "Validation error occurred."
        //   path: "/api/v1/items"

        val invalidInput = ItemInput(
            title = "   ",
            expirationDate = "invalid-date",
            category = Category.MEDICINE
        )

        val validator = ItemValidator()
        val validationResult = validator.validate(invalidInput)

        assertFalse("Validation must fail for invalid input", validationResult.isValid)
        assertTrue("Validation errors must not be empty", validationResult.errors.isNotEmpty())

        // Map validation failure to OpenAPI ErrorResponse model
        // Following strict TDD: toErrorResponse() does NOT exist yet in production code
        val errorResponse: ErrorResponse = validator.toErrorResponse(
            validationResult = validationResult,
            path = "/api/v1/items"
        )

        assertEquals("Status must be 400 Bad Request per openapi.yaml", 400, errorResponse.status)
        assertEquals("Error string must be 'Bad Request' per openapi.yaml", "Bad Request", errorResponse.error)
        assertEquals("Path must match requested endpoint", "/api/v1/items", errorResponse.path)
        assertTrue("Message must contain validation errors", errorResponse.message.contains("Validation error"))

        // Verify timestamp follows ISO-8601 date-time format
        val parsedTime = DateTimeFormatter.ISO_DATE_TIME.parse(errorResponse.timestamp)
        assertNotNull("Timestamp in ErrorResponse must be valid ISO-8601 date-time format", parsedTime)
    }
}
