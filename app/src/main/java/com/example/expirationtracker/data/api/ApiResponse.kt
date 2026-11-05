package com.example.expirationtracker.data.api

import com.example.expirationtracker.domain.model.ErrorResponse

/**
 * Generic HTTP response wrapper simulating REST endpoint execution.
 * Aligns strictly with OpenAPI status codes and schemas.
 */
data class ApiResponse<T>(
    val status: Int,
    val data: T? = null,
    val error: ErrorResponse? = null
) {
    val isSuccessful: Boolean get() = status in 200..299
}
