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

old_fun = """    fun verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
        val trimmed = password.trim()
        if (trimmed.isEmpty()) return false
        
        if (trimmed == adminPass ||
            trimmed == ownerPass ||
            com.example.utils.SecurityCryptoUtils.hashPassword(trimmed) == adminPass ||
            com.example.utils.SecurityCryptoUtils.hashPassword(trimmed) == ownerPass ||
            com.example.utils.PasswordHasher.verifyPassword(trimmed, adminPass) ||
            com.example.utils.PasswordHasher.verifyPassword(trimmed, ownerPass) ||
            com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmed, adminPass) ||
            com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmed, ownerPass)) {
            return true
        }
        val matchSup = _supervisors.value.find {
            (it.passcode.isNotBlank() && it.passcode.trim() == trimmed) ||
            (it.passcode.isNotBlank() && com.example.utils.PasswordHasher.verifyPassword(trimmed, it.passcode)) ||
            (it.passcode.isNotBlank() && com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmed, it.passcode))
        }
        return matchSup != null
    }"""

new_fun = """    suspend fun verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
        val trimmed = password.trim()
        if (trimmed.isEmpty()) return false
        
        val role = com.example.utils.AdminSecurityManager.verifyCredentials("admin@example.com", trimmed)
        if (role == "OWNER" || role == "ADMIN" || role == "SUPERVISOR") {
            return true
        }
        
        if (trimmed == adminPass ||
            trimmed == ownerPass ||
            com.example.utils.SecurityCryptoUtils.hashPassword(trimmed) == adminPass ||
            com.example.utils.SecurityCryptoUtils.hashPassword(trimmed) == ownerPass ||
            com.example.utils.PasswordHasher.verifyPassword(trimmed, adminPass) ||
            com.example.utils.PasswordHasher.verifyPassword(trimmed, ownerPass) ||
            com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmed, adminPass) ||
            com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmed, ownerPass)) {
            return true
        }
        val matchSup = _supervisors.value.find {
            (it.passcode.isNotBlank() && it.passcode.trim() == trimmed) ||
            (it.passcode.isNotBlank() && com.example.utils.PasswordHasher.verifyPassword(trimmed, it.passcode)) ||
            (it.passcode.isNotBlank() && com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmed, it.passcode))
        }
        return matchSup != null
    }"""

replace_in_file('app/src/main/java/com/example/ui/viewmodels/AuthViewModel.kt', old_fun, new_fun)

