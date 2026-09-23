package com.example.ui.screens.register.forms

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SimplifiedRegistrationViewModelTest {
    
    private lateinit var viewModel: SimplifiedRegistrationViewModel
    private lateinit var application: Application
    
    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = SimplifiedRegistrationViewModel(application)
        viewModel.loadDraft("CLIENT")
    }
    
    // ============================================
    // 1. اختبارات Events للحقول
    // ============================================
    
    @Test
    fun `entity name changed updates state`() {
        viewModel.onEvent(RegistrationUiEvent.EntityNameChanged("علي محمد"))
        assertEquals("علي محمد", viewModel.state.value.entityName)
    }
    
    @Test
    fun `phone changed updates state`() {
        viewModel.onEvent(RegistrationUiEvent.PhoneChanged("771234567"))
        assertEquals("771234567", viewModel.state.value.phone)
    }
    
    @Test
    fun `password and confirm password match`() {
        viewModel.onEvent(RegistrationUiEvent.PasswordChanged("Password123"))
        viewModel.onEvent(RegistrationUiEvent.ConfirmPasswordChanged("Password123"))
        assertEquals("Password123", viewModel.state.value.password)
        assertEquals("Password123", viewModel.state.value.confirmPassword)
    }
    
    // ============================================
    // 2. اختبارات form validity & Multi-error Edge Cases
    // ============================================
    
    @Test
    fun `form invalid initially`() {
        assertFalse(viewModel.state.value.isFormValid)
    }
    
    @Test
    fun `form becomes valid after all fields filled correctly`() {
        viewModel.onEvent(RegistrationUiEvent.EntityNameChanged("علي محمد أحمد"))
        viewModel.onEvent(RegistrationUiEvent.PhoneChanged("771234567"))
        viewModel.onEvent(RegistrationUiEvent.PasswordChanged("Password123"))
        viewModel.onEvent(RegistrationUiEvent.ConfirmPasswordChanged("Password123"))
        viewModel.onEvent(RegistrationUiEvent.AgreedToTermsChanged(true))
        
        assertTrue(viewModel.state.value.isFormValid)
    }
    
    @Test
    fun `form invalid without terms agreement`() {
        viewModel.onEvent(RegistrationUiEvent.EntityNameChanged("علي محمد"))
        viewModel.onEvent(RegistrationUiEvent.PhoneChanged("771234567"))
        viewModel.onEvent(RegistrationUiEvent.PasswordChanged("Password123"))
        viewModel.onEvent(RegistrationUiEvent.ConfirmPasswordChanged("Password123"))
        viewModel.onEvent(RegistrationUiEvent.AgreedToTermsChanged(false))
        
        assertFalse(viewModel.state.value.isFormValid)
    }

    @Test
    fun `form invalid with multiple combined errors invalid phone mismatched password no terms`() {
        viewModel.onEvent(RegistrationUiEvent.EntityNameChanged("علي")) // valid name or short
        viewModel.onEvent(RegistrationUiEvent.PhoneChanged("123")) // invalid phone
        viewModel.onEvent(RegistrationUiEvent.PasswordChanged("Pass123"))
        viewModel.onEvent(RegistrationUiEvent.ConfirmPasswordChanged("DifferentPass")) // mismatch
        viewModel.onEvent(RegistrationUiEvent.AgreedToTermsChanged(false)) // no terms

        assertFalse(viewModel.state.value.isFormValid)
    }
}
