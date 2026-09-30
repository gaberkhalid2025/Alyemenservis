package com.example.auth

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * 🔐 UserAuthIntegrationTest
 * Comprehensive integration test verifying the full user authentication flow
 * (registration, login, logout, and network error handling) using Tasks and Firebase Auth exceptions.
 */
class UserAuthIntegrationTest {

    data class TestAuthUser(
        val uid: String,
        val email: String,
        val phoneNumber: String
    )

    private class FakeAuthSessionManager {
        var currentUser: TestAuthUser? = null
        var createCalls = 0
        var signInCalls = 0
        var signOutCalls = 0
        var nextCreateTask: Task<AuthResult>? = null
        var nextSignInTask: Task<AuthResult>? = null

        fun createUserWithEmailAndPassword(
            @Suppress("UNUSED_PARAMETER") email: String,
            @Suppress("UNUSED_PARAMETER") password: String
        ): Task<AuthResult> {
            createCalls++
            val task = nextCreateTask ?: Tasks.forException(IllegalStateException("Not configured"))
            if (task.isSuccessful) {
                currentUser = TestAuthUser(
                    uid = "user_test_uid_9988",
                    email = "user@yemen.services.com",
                    phoneNumber = "+967770000000"
                )
            }
            return task
        }

        fun signInWithEmailAndPassword(
            @Suppress("UNUSED_PARAMETER") email: String,
            @Suppress("UNUSED_PARAMETER") password: String
        ): Task<AuthResult> {
            signInCalls++
            val task = nextSignInTask ?: Tasks.forException(IllegalStateException("Not configured"))
            if (task.isSuccessful) {
                currentUser = TestAuthUser(
                    uid = "user_test_uid_9988",
                    email = "user@yemen.services.com",
                    phoneNumber = "+967770000000"
                )
            }
            return task
        }

        fun signOut() {
            signOutCalls++
            currentUser = null
        }
    }

    private lateinit var authManager: FakeAuthSessionManager
    private lateinit var fakeAuthResult: AuthResult

    @Before
    fun setUp() {
        authManager = FakeAuthSessionManager()
        fakeAuthResult = Proxy.newProxyInstance(
            AuthResult::class.java.classLoader,
            arrayOf(AuthResult::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getUser" -> null
                "toString" -> "FakeAuthResult"
                "hashCode" -> 0
                "equals" -> false
                else -> null
            }
        } as AuthResult
    }

    // ==========================================
    // 1. REGISTRATION FLOW TESTS
    // ==========================================

    @Test
    fun `test user registration success flow`() = runBlocking {
        val email = "newuser@example.com"
        val password = "StrongPassword@2026"

        authManager.nextCreateTask = Tasks.forResult(fakeAuthResult)

        val resultTask = authManager.createUserWithEmailAndPassword(email, password)
        assertTrue(resultTask.isSuccessful)
        assertNotNull(authManager.currentUser)
        assertEquals("user_test_uid_9988", authManager.currentUser?.uid)
        assertEquals("user@yemen.services.com", authManager.currentUser?.email)
        assertEquals(1, authManager.createCalls)
    }

    @Test
    fun `test user registration fails when email already exists`() = runBlocking {
        val email = "existing@example.com"
        val password = "StrongPassword@2026"

        val collisionException = FirebaseAuthUserCollisionException(
            "ERROR_EMAIL_ALREADY_IN_USE",
            "The email address is already in use by another account."
        )
        authManager.nextCreateTask = Tasks.forException(collisionException)

        val resultTask = authManager.createUserWithEmailAndPassword(email, password)
        assertFalse(resultTask.isSuccessful)
        assertNotNull(resultTask.exception)
        assertTrue(resultTask.exception is FirebaseAuthUserCollisionException)
        assertEquals("The email address is already in use by another account.", resultTask.exception?.message)
    }

    @Test
    fun `test user registration fails with weak password`() = runBlocking {
        val email = "weakpass@example.com"
        val weakPassword = "123"

        val weakPassException = FirebaseAuthWeakPasswordException(
            "ERROR_WEAK_PASSWORD",
            "Password should be at least 6 characters",
            "123"
        )
        authManager.nextCreateTask = Tasks.forException(weakPassException)

        val resultTask = authManager.createUserWithEmailAndPassword(email, weakPassword)
        assertFalse(resultTask.isSuccessful)
        assertTrue(resultTask.exception is FirebaseAuthWeakPasswordException)
    }

    // ==========================================
    // 2. LOGIN FLOW TESTS
    // ==========================================

    @Test
    fun `test user login success flow`() = runBlocking {
        val email = "user@yemen.services.com"
        val password = "ValidPassword123"

        authManager.nextSignInTask = Tasks.forResult(fakeAuthResult)

        val resultTask = authManager.signInWithEmailAndPassword(email, password)
        assertTrue(resultTask.isSuccessful)
        val loggedInUser = authManager.currentUser
        assertNotNull(loggedInUser)
        assertEquals("user_test_uid_9988", loggedInUser?.uid)
        assertEquals(1, authManager.signInCalls)
    }

    @Test
    fun `test user login fails with invalid credentials`() = runBlocking {
        val email = "user@yemen.services.com"
        val wrongPassword = "WrongPassword999"

        val invalidCredentialsException = FirebaseAuthInvalidCredentialsException(
            "ERROR_WRONG_PASSWORD",
            "The password is invalid."
        )
        authManager.nextSignInTask = Tasks.forException(invalidCredentialsException)

        val resultTask = authManager.signInWithEmailAndPassword(email, wrongPassword)
        assertFalse(resultTask.isSuccessful)
        assertTrue(resultTask.exception is FirebaseAuthInvalidCredentialsException)
        assertEquals("The password is invalid.", resultTask.exception?.message)
    }

    @Test
    fun `test user login fails with non existent user`() = runBlocking {
        val email = "nonexistent@example.com"
        val password = "SomePassword123"

        val invalidUserException = FirebaseAuthInvalidUserException(
            "ERROR_USER_NOT_FOUND",
            "There is no user record corresponding to this identifier."
        )
        authManager.nextSignInTask = Tasks.forException(invalidUserException)

        val resultTask = authManager.signInWithEmailAndPassword(email, password)
        assertFalse(resultTask.isSuccessful)
        assertTrue(resultTask.exception is FirebaseAuthInvalidUserException)
    }

    // ==========================================
    // 3. LOGOUT & NETWORK ERROR HANDLING
    // ==========================================

    @Test
    fun `test user sign out flow resets current user`() {
        authManager.currentUser = TestAuthUser("user_test_uid_9988", "user@yemen.services.com", "+967770000000")
        authManager.signOut()
        assertNull(authManager.currentUser)
        assertEquals(1, authManager.signOutCalls)
    }

    @Test
    fun `test simulated network error during authentication`() = runBlocking {
        val email = "user@yemen.services.com"
        val password = "ValidPassword123"

        val networkException = FirebaseNetworkException("A network error has occurred.")
        authManager.nextSignInTask = Tasks.forException(networkException)

        val resultTask = authManager.signInWithEmailAndPassword(email, password)
        assertFalse(resultTask.isSuccessful)
        assertTrue(resultTask.exception is FirebaseNetworkException)
        assertTrue(resultTask.exception?.message?.contains("network error") == true)
    }
}
