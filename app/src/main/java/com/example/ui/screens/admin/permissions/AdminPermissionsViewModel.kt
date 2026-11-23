package com.example.ui.screens.admin.permissions

import androidx.lifecycle.ViewModel
import com.example.data.models.AdminPermissionItem
import com.example.data.models.AdminPermissionsRegistry
import com.example.data.models.PermissionCategory
import com.example.data.models.PermissionLevel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminPermissionsViewModel : ViewModel() {

    companion object {
        private const val MATRIX_CACHE_TTL_MS = 5 * 60 * 1000L
        @Volatile
        private var cachedRoleOverrides: Map<String, Set<String>> = emptyMap()
        @Volatile
        private var lastMatrixFetchMs: Long = 0L
    }

    private val _selectedRole = MutableStateFlow("ADMIN")
    val selectedRole: StateFlow<String> = _selectedRole.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLevelFilter = MutableStateFlow<String?>(null)
    val selectedLevelFilter: StateFlow<String?> = _selectedLevelFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _expandedCategories = MutableStateFlow<Set<PermissionCategory>>(emptySet())
    val expandedCategories: StateFlow<Set<PermissionCategory>> = _expandedCategories.asStateFlow()

    private val _activePermissions = MutableStateFlow<Set<String>>(
        AdminPermissionsRegistry.allPermissions.map { it.key }.toSet()
    )
    val activePermissions: StateFlow<Set<String>> = _activePermissions.asStateFlow()

    val allPermissions: List<AdminPermissionItem> = AdminPermissionsRegistry.allPermissions

    private var savedRoleOverrides: Map<String, Set<String>> = emptyMap()

    init {
        loadSavedRolePermissions()
    }

    fun invalidatePermissionsCache() {
        lastMatrixFetchMs = 0L
    }

    fun loadSavedRolePermissions(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedRoleOverrides.isNotEmpty() && (now - lastMatrixFetchMs) < MATRIX_CACHE_TTL_MS) {
            savedRoleOverrides = cachedRoleOverrides
            cachedRoleOverrides[_selectedRole.value.uppercase()]?.let {
                _activePermissions.value = it
            }
            return
        }
        try {
            FirebaseFirestore.getInstance()
                .collection("settings")
                .document("admin_permissions_matrix")
                .get()
                .addOnSuccessListener { doc ->
                    if (doc == null || !doc.exists()) return@addOnSuccessListener
                    val rolesRaw = doc.get("roles") as? Map<*, *> ?: return@addOnSuccessListener
                    val parsed = mutableMapOf<String, Set<String>>()
                    rolesRaw.forEach { (roleKey, value) ->
                        val rName = roleKey?.toString()?.uppercase() ?: return@forEach
                        val roleMap = value as? Map<*, *> ?: return@forEach
                        val perms = (roleMap["activePermissions"] as? List<*>)
                            ?.mapNotNull { it?.toString() }
                            ?.toSet()
                        if (perms != null) {
                            parsed[rName] = perms
                        }
                    }
                    savedRoleOverrides = parsed
                    cachedRoleOverrides = parsed
                    lastMatrixFetchMs = System.currentTimeMillis()
                    val currentOverride = parsed[_selectedRole.value.uppercase()]
                    if (currentOverride != null) {
                        _activePermissions.value = currentOverride
                    }
                }
        } catch (_: Exception) {
        }
    }

    fun selectRole(roleKey: String) {
        _selectedRole.value = roleKey
        val saved = savedRoleOverrides[roleKey.uppercase()]
        if (saved != null) {
            _activePermissions.value = saved
            return
        }
        _activePermissions.value = when (roleKey) {
            "OWNER" -> allPermissions.map { it.key }.toSet()
            "ADMIN" -> allPermissions.map { it.key }.toSet()
            "SUPERVISOR" -> allPermissions
                .filter { it.level != PermissionLevel.SENSITIVE || it.category == PermissionCategory.SUPERVISORS }
                .map { it.key }.toSet()
            "AUDITOR" -> allPermissions
                .filter { it.level == PermissionLevel.BASIC || it.level == PermissionLevel.MEDIUM || it.category == PermissionCategory.COMPLAINTS || it.category == PermissionCategory.BLOCKED }
                .map { it.key }.toSet()
            "SUPPORT" -> allPermissions
                .filter { it.category == PermissionCategory.CHATS || it.category == PermissionCategory.NOTIFICATIONS || it.category == PermissionCategory.BOOKINGS || it.category == PermissionCategory.REVIEWS }
                .map { it.key }.toSet()
            "OPERATIONS" -> allPermissions
                .filter { it.category == PermissionCategory.STORES || it.category == PermissionCategory.RESTAURANTS || it.category == PermissionCategory.MEDICAL || it.category == PermissionCategory.PROPERTIES || it.category == PermissionCategory.JOBS || it.category == PermissionCategory.CATEGORIES || it.category == PermissionCategory.CITIES }
                .map { it.key }.toSet()
            else -> allPermissions.map { it.key }.toSet()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLevelFilter(level: String?) {
        _selectedLevelFilter.value = level
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun toggleCategoryExpansion(category: PermissionCategory) {
        val current = _expandedCategories.value
        _expandedCategories.value = if (current.contains(category)) {
            current - category
        } else {
            current + category
        }
    }

    fun expandAllCategories() {
        _expandedCategories.value = PermissionCategory.values().toSet()
    }

    fun collapseAllCategories() {
        _expandedCategories.value = emptySet()
    }

    fun enableAllPermissions() {
        _activePermissions.value = allPermissions.map { it.key }.toSet()
    }

    fun disableAllPermissions() {
        _activePermissions.value = emptySet()
    }

    fun enableBasicPermissionsOnly() {
        _activePermissions.value = allPermissions
            .filter { it.level == PermissionLevel.BASIC }
            .map { it.key }.toSet()
    }

    fun enableBasicAndMediumPermissions() {
        _activePermissions.value = allPermissions
            .filter { it.level == PermissionLevel.BASIC || it.level == PermissionLevel.MEDIUM }
            .map { it.key }.toSet()
    }

    fun togglePermission(key: String, enabled: Boolean) {
        val current = _activePermissions.value.toMutableSet()
        if (enabled) current.add(key) else current.remove(key)
        _activePermissions.value = current
    }

    fun toggleCategoryPermissions(category: PermissionCategory, enable: Boolean) {
        val keys = AdminPermissionsRegistry.getByCategory(category).map { it.key }.toSet()
        val current = _activePermissions.value.toMutableSet()
        if (enable) current.addAll(keys) else current.removeAll(keys)
        _activePermissions.value = current
    }

    fun setPermissions(permissions: Set<String>) {
        _activePermissions.value = permissions
    }
}
