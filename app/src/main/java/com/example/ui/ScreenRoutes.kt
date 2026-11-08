package com.example.ui

object ScreenRoutes {
    fun isFullScreen(currentScreen: String): Boolean {
        return currentScreen == AppScreens.MAP_VIEW || currentScreen.endsWith("_DETAILS")
    }

    fun showBottomBar(currentScreen: String, adminRole: String = "GUEST"): Boolean {
        return currentScreen != AppScreens.CHAT_DIRECT && currentScreen != AppScreens.MAP_VIEW && currentScreen != AppScreens.SMART_ASSISTANT
    }

    fun showTopBar(currentScreen: String, adminRole: String = "GUEST"): Boolean {
        return currentScreen != AppScreens.CHAT_DIRECT && currentScreen != AppScreens.MAP_VIEW && currentScreen != AppScreens.SMART_ASSISTANT
    }

    fun isRegistrationForm(screen: String): Boolean =
        screen in setOf("REGISTER_FORM", "JOIN_REQUEST_STATUS", "LOGIN", "REGISTER")

    fun isFormOpen(dialogAction: String?): Boolean = dialogAction != null

    fun isRegistrationOrFormOpen(
        currentScreen: String,
        showGuestRegisterDialogForAction: String? = null,
        showAssistantDialog: Boolean = false,
        showRequestServiceModal: Boolean = false
    ): Boolean {
        return currentScreen in setOf(
            "REGISTER_FORM", "JOIN_REQUEST_STATUS", "LOGIN", 
            "PROVIDER_REGISTRATION", "STORE_CREATION", "PROPERTY_CREATION", 
            "JOB_CREATION", "CREATE_BOOKING", "REGISTER",
            "MAP", "MAP_VIEW", "ADMIN_PANEL", "ADMIN_LOGIN", "OWNER_PANEL",
            "CHAT_LIST", "CHAT_DIRECT", "CHAT",
            AppScreens.SMART_ASSISTANT, AppScreens.QUICK_SERVICE_REQUEST
        ) || showGuestRegisterDialogForAction != null || 
          showAssistantDialog || showRequestServiceModal
    }

    val ALLOWED_EXTERNAL_TARGETS = setOf(
        AppScreens.HOME, AppScreens.INSTANT_REQUESTS_VIEW, AppScreens.ORDERS_VIEW,
        AppScreens.NOTIFICATIONS_VIEW, AppScreens.MAP_VIEW, AppScreens.CHAT_LIST,
        AppScreens.STATUS_VIEW, AppScreens.JOIN_REQUEST_STATUS,
        AppScreens.SMART_ASSISTANT, AppScreens.QUICK_SERVICE_REQUEST
    )

    fun safeTarget(raw: String?, isAdminOrOwner: Boolean): String? {
        if (raw.isNullOrBlank() || raw == "MAIN" || raw == "HOME") return null
        if (raw in ALLOWED_EXTERNAL_TARGETS) return raw
        if ((raw == AppScreens.ADMIN_PANEL || raw == AppScreens.OWNER_PANEL || raw == "PASSWORD_RECOVERY_PANEL") && isAdminOrOwner) {
            return raw
        }
        return null
    }
}
