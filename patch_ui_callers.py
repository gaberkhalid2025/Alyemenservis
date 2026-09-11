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

old_main = """    fun MainViewModel.verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
        return authViewModel.verifyAdminOrOwnerPassword(password, adminPass, ownerPass)
    }"""

new_main = """    suspend fun MainViewModel.verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
        return authViewModel.verifyAdminOrOwnerPassword(password, adminPass, ownerPass)
    }"""

replace_in_file('app/src/main/java/com/example/ui/MainViewModelDelegates.kt', old_main, new_main)

old_settings_prop = """    var verifyAdminOrOwnerPassword: ((String) -> Boolean)? = null"""
new_settings_prop = """    var verifyAdminOrOwnerPassword: (suspend (String) -> Boolean)? = null"""

replace_in_file('app/src/main/java/com/example/ui/viewmodels/SettingsViewModel.kt', old_settings_prop, new_settings_prop)

old_settings_fun = """        fun verifyAdminOrOwnerPassword(password: String): Boolean {
            return this@SettingsViewModel.verifyAdminOrOwnerPassword?.invoke(password) ?: false
        }"""
new_settings_fun = """        suspend fun verifyAdminOrOwnerPassword(password: String): Boolean {
            return this@SettingsViewModel.verifyAdminOrOwnerPassword?.invoke(password) ?: false
        }"""

replace_in_file('app/src/main/java/com/example/ui/viewmodels/SettingsViewModel.kt', old_settings_fun, new_settings_fun)

old_main_prop = """        settingsViewModel.verifyAdminOrOwnerPassword = { password ->
            verifyAdminOrOwnerPassword(password)
        }"""
new_main_prop = """        settingsViewModel.verifyAdminOrOwnerPassword = { password ->
            verifyAdminOrOwnerPassword(password)
        }"""
# No change needed since lambda can be suspend if the type is suspend.

