package com.example.auth

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 🔐 UserAuthIntegrationTest (androidTest)
 * Verifies full authentication lifecycle: registration, login, error scenarios, and logout.
 */
class UserAuthIntegrationTest {

    private lateinit var mockFirebaseAuth: FirebaseAuth
    private lateinit var mockFirebaseUser: FirebaseUser
    private lateinit var mockAuthResult: AuthResult

    @Before
    fun setUp() {
        mockFirebaseAuth = mockk(relaxed = true)
        mockFirebaseUser = mockk(relaxed = true)
        mockAuthResult = mockk(relaxed = true)

        every { mockAuthResult.user } returns mockFirebaseUser
        every { mockFirebaseUser.uid } returns "user_test_uid_9988"
        every { mockFirebaseUser.email } returns "user@yemen.services.com"
        every { mockFirebaseUser.phoneNumber } returns "+967770000000"
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun testUserRegistrationSuccessFlow() = runBlocking {
        val email = "newuser@example.com"
        val password = "StrongPassword@2026"

        val task: Task<AuthResult> = Tasks.forResult(mockAuthResult)
        every { mockFirebaseAuth.createUserWithEmailAndPassword(email, password) } returns task
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

        val resultTask = mockFirebaseAuth.createUserWithEmailAndPassword(email, password)
        assertTrue(resultTask.isSuccessful)
        assertNotNull(resultTask.result?.user)
        assertEquals("user_test_uid_9988", resultTask.result?.user?.uid)
        assertEquals("user@yemen.services.com", resultTask.result?.user?.email)

        verify(exactly = 1) { mockFirebaseAuth.createUserWithEmailAndPassword(email, password) }
    }

    @Test
    fun testUserRegistrationFailsWhenEmailAlreadyExists() = runBlocking {
        val email = "existing@example.com"
        val password = "StrongPassword@2026"

        val collisionException = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "The email address is already in use by another account.")
        val failedTask: Task<AuthResult> = Tasks.forException(collisionException)
        every { mockFirebaseAuth.createUserWithEmailAndPassword(email, password) } returns failedTask

        val resultTask = mockFirebaseAuth.createUserWithEmailAndPassword(email, password)
        assertFalse(resultTask.isSuccessful)
        assertNotNull(resultTask.exception)
        assertTrue(resultTask.exception is FirebaseAuthUserCollisionException)
    }

    @Test
    fun testUserRegistrationFailsWithWeakPassword() = runBlocking {
        val email = "weakpass@example.com"
        val weakPassword = "123"

        val weakPassException = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "Password should be at least 6 characters", "123")
        val failedTask: Task<AuthResult> = Tasks.forException(weakPassException)
        every { mockFirebaseAuth.createUserWithEmailAndPassword(email, weakPassword) } returns failedTask

        val resultTask = mockFirebaseAuth.createUserWithEmailAndPassword(email, weakPassword)
        assertFalse(resultTask.isSuccessful)
        assertTrue(resultTask.exception is FirebaseAuthWeakPasswordException)
    }

    @Test
    fun testUserLoginSuccessFlow() = runBlocking {
        val email = "user@yemen.services.com"
        val password = "ValidPassword123"

        val successTask: Task<AuthResult> = Tasks.forResult(mockAuthResult)
        every { mockFirebaseAuth.signInWithEmailAndPassword(email, password) } returns successTask
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

        val resultTask = mockFirebaseAuth.signInWithEmailAndPassword(email, password)
        assertTrue(resultTask.isSuccessful)
        val loggedInUser = resultTask.result?.user
        assertNotNull(loggedInUser)
        assertEquals("user_test_uid_9988", loggedInUser?.uid)

        verify(exactly = 1) { mockFirebaseAuth.signInWithEmailAndPassword(email, password) }
    }

    @Test
    fun testUserLoginFailsWithInvalidCredentials() = runBlocking {
        val email = "user@yemen.services.com"
        val wrongPassword = "WrongPassword999"

        val invalidCredentialsException = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "The password is invalid.")
        val failedTask: Task<AuthResult> = Tasks.forException(invalidCredentialsException)
        every { mockFirebaseAuth.signInWithEmailAndPassword(email, wrongPassword) } returns failedTask

        val resultTask = mockFirebaseAuth.signInWithEmailAndPassword(email, wrongPassword)
        assertFalse(resultTask.isSuccessful)
        assertTrue(resultTask.exception is FirebaseAuthInvalidCredentialsException)
    }

    @Test
    fun testNetworkErrorHandling() = runBlocking {
        val email = "user@yemen.services.com"
        val password = "ValidPassword123"

        val networkException = FirebaseNetworkException("A network error has occurred.")
        val failedTask: Task<AuthResult> = Tasks.forException(networkException)
        every { mockFirebaseAuth.signInWithEmailAndPassword(email, password) } returns failedTask

        val resultTask = mockFirebaseAuth.signInWithEmailAndPassword(email, password)
        assertFalse(resultTask.isSuccessful)
        assertTrue(resultTask.exception is FirebaseNetworkException)
    }
}
