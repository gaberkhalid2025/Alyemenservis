import sys

def replace_in_file(filename, old_str, new_str):
    with open(filename, 'r') as f:
        content = f.read()
    
    if old_str in content:
        content = content.replace(old_str, new_str)
        with open(filename, 'w') as f:
            f.write(content)
        print(f"Patched {filename}")
    else:
        print(f"Not found in {filename}")

old_fun = """    suspend fun verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
        val trimmed = password.trim()
        if (trimmed.isEmpty()) return false
        
        val role = com.example.utils.AdminSecurityManager.verifyCredentials("admin@example.com", trimmed)
        if (role == "OWNER" || role == "ADMIN" || role == "SUPERVISOR") {
            return true
        }
        
        if (trimmed == adminPass ||"""

new_fun = """    fun verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
        val trimmed = password.trim()
        if (trimmed.isEmpty()) return false
        
        if (trimmed == adminPass ||"""

replace_in_file('app/src/main/java/com/example/ui/viewmodels/AuthViewModel.kt', old_fun, new_fun)

old_main = """    suspend fun MainViewModel.verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {"""
new_main = """    fun MainViewModel.verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {"""
replace_in_file('app/src/main/java/com/example/ui/MainViewModelDelegates.kt', old_main, new_main)

old_settings_prop = """    var verifyAdminOrOwnerPassword: (suspend (String) -> Boolean)? = null"""
new_settings_prop = """    var verifyAdminOrOwnerPassword: ((String) -> Boolean)? = null"""
replace_in_file('app/src/main/java/com/example/ui/viewmodels/SettingsViewModel.kt', old_settings_prop, new_settings_prop)

old_settings_fun = """        suspend fun verifyAdminOrOwnerPassword(password: String): Boolean {"""
new_settings_fun = """        fun verifyAdminOrOwnerPassword(password: String): Boolean {"""
replace_in_file('app/src/main/java/com/example/ui/viewmodels/SettingsViewModel.kt', old_settings_fun, new_settings_fun)

