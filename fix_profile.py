import re

filepath = "app/src/main/java/com/example/ui/screens/entities/DynamicPolymorphicProfileScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# Extract from val isOwner up to the closing brace
pattern = r"val isOwner = remember\(currentUserId, currentUserPhone, provider, store, property, adminRole\) \{.*?\n    \}"
replacement = """val adminRole by viewModel.adminRole.collectAsState()
    val isOwner = remember(currentUserId, currentUserPhone, provider, store, property, adminRole) {
        val phoneClean = currentUserPhone.trim()
        val uidClean = currentUserId.trim()
        val provPhone = provider?.phone?.trim() ?: ""
        val storePhone = store?.phone?.trim() ?: ""
        val storeOwner = store?.ownerId?.trim() ?: ""
        val propPhone = property?.phone?.trim() ?: ""
        val propOwner = property?.ownerId?.trim() ?: ""
        val isAdmin = adminRole != "GUEST"
        
        isAdmin || (uidClean.isNotEmpty() && (uidClean == storeOwner || uidClean == propOwner)) ||
        (phoneClean.isNotEmpty() && (phoneClean == provPhone || phoneClean == storePhone || phoneClean == propPhone))
    }"""

content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open(filepath, "w") as f:
    f.write(content)

