package com.example.expirationtracker.data.api

/**
 * Standard HTTP Status Constants per RFC 9110 and OpenAPI 3.0 specification.
 */
object HttpStatus {
    const val OK = 200
    const val CREATED = 201
    const val NO_CONTENT = 204
    const val BAD_REQUEST = 400
    const val NOT_FOUND = 404
    const val INTERNAL_SERVER_ERROR = 500

    const val REASON_OK = "OK"
    const val REASON_CREATED = "Created"
    const val REASON_BAD_REQUEST = "Bad Request"
    const val REASON_NOT_FOUND = "Not Found"
}
