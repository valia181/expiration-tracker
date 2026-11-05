package com.example.expirationtracker.domain.model

/**
 * ErrorResponse model strictly corresponding to openapi.yaml components/schemas/ErrorResponse.
 *
 * Required fields per specification:
 * - timestamp: ISO-8601 date-time string
 * - status: HTTP status code integer (e.g. 400)
 * - error: HTTP status error text (e.g. "Bad Request")
 * - message: Human-readable error description
 * - path: Request URI / operation endpoint path
 */
data class ErrorResponse(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)
