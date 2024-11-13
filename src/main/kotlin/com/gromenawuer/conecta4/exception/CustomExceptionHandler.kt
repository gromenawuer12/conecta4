package com.gromenawuer.conecta4.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@RestControllerAdvice
class CustomExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(value = [CapacityExceededException::class])
    fun handleConflict(
        ex: CapacityExceededException, request: WebRequest
    ): ResponseEntity<Any> {
        val bodyOfResponse = mapOf(
            "error" to "Capacity exceeded",
            "message" to ex.message
        )
        return ResponseEntity(bodyOfResponse, HttpStatus.CONFLICT)
    }
}
