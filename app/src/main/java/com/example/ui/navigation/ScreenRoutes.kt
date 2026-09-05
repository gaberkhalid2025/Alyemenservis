package com.example.ui.navigation

object ScreenRoutes {
    fun isFullScreen(currentScreen: String): Boolean {
        return currentScreen == AppScreens.MAP_VIEW || currentScreen.endsWith("_DETAILS")
    }

    fun showBottomBar(currentScreen: String, adminRole: String = "GUEST"): Boolean {
        return currentScreen != AppScreens.CHAT_DIRECT
    }

    fun showTopBar(currentScreen: String, adminRole: String = "GUEST"): Boolean {
        return currentScreen != AppScreens.CHAT_DIRECT
    }

    fun isRegistrationOrFormOpen(
        currentScreen: String,
        showGuestRegisterDialogForAction: String?,
        showAssistantDialog: Boolean,
        showRequestServiceModal: Boolean
    ): Boolean {
        return currentScreen in setOf(
            "REGISTER_FORM", "JOIN_REQUEST_STATUS", "LOGIN", 
            "PROVIDER_REGISTRATION", "STORE_CREATION", "PROPERTY_CREATION", 
            "JOB_CREATION", "CREATE_BOOKING", "REGISTER",
            "MAP", "MAP_VIEW", "ADMIN_PANEL", "ADMIN_LOGIN", "OWNER_PANEL",
            "CHAT_LIST", "CHAT_DIRECT", "CHAT"
        ) || showGuestRegisterDialogForAction != null || 
          showAssistantDialog || showRequestServiceModal
    }
}
