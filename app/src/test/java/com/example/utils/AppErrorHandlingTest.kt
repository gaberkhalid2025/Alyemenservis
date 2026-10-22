package com.example.utils

import org.junit.Assert.*
import org.junit.Test
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

class AppErrorHandlingTest {
    
    @Test
    fun `AppResult Success contains data`() {
        val result = AppResult.Success("data")
        assertTrue(result.isSuccess)
        assertEquals("data", result.data)
    }
    
    @Test
    fun `AppResult Error contains error`() {
        val error = AppError.NetworkError()
        val result = AppResult.Error(error)
        assertTrue(result.isError)
        assertEquals(error, result.error)
    }
    
    @Test
    fun `runCatchingApp with success returns Success`() {
        val result = AppResult.runCatchingApp { "success" }
        assertTrue(result is AppResult.Success)
        assertEquals("success", (result as AppResult.Success).data)
    }
    
    @Test
    fun `runCatchingApp with UnknownHostException returns NetworkError`() {
        val result = AppResult.runCatchingApp { throw UnknownHostException() }
        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is AppError.NetworkError)
    }
    
    @Test
    fun `runCatchingApp with TimeoutException returns TimeoutError`() {
        val result = AppResult.runCatchingApp { throw TimeoutException() }
        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is AppError.TimeoutError)
    }
    
    @Test
    fun `AppError NetworkError has correct message`() {
        val error = AppError.NetworkError()
        assertTrue(error.messageArabic.contains("الإنترنت"))
    }
    
    @Test
    fun `AppError UnauthorizedError has correct message`() {
        val error = AppError.UnauthorizedError()
        assertTrue(error.messageArabic.contains("صلاحية"))
    }
    
    @Test
    fun `getOrNull returns data for Success`() {
        val result = AppResult.Success("test")
        assertEquals("test", result.getOrNull())
    }
    
    @Test
    fun `getOrNull returns null for Error`() {
        val result = AppResult.Error(AppError.NetworkError())
        assertNull(result.getOrNull())
    }
}
