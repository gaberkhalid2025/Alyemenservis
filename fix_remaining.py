import re, os

mapping = {
    'cancelOrResetJoinRequest': 'registrationViewModel',
    'setUserSessionDetails': 'authViewModel',
    'submitJoinForm': 'registrationViewModel',
    'resetRegistrationState': 'registrationViewModel',
    'openChatChannel': 'chatViewModel',
    'isProviderUser': 'authViewModel',
}

def fix_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    changed = False

    # Fix authViewModel.isProviderUser -> authViewModel.isProviderUser (wait, what is the error?)
    # "Unresolved reference 'isProviderUser'" means the variable itself is used, but it's not declared as a state.
    # Ah! `val isProviderUser by authViewModel.isProviderUser.collectAsState()` is missing!
    if "isProviderUser" in content and "val isProviderUser" not in content and "AuthViewModel" in content:
        content = re.sub(
            r'(@Composable\s*fun\s*[a-zA-Z0-9_]+\s*\([^)]*\)\s*\{)',
            r'\1\n    val isProviderUser by authViewModel.isProviderUser.collectAsState()',
            content
        )
        changed = True

    if "currentUserId" in content and "val currentUserId" not in content and "AuthViewModel" in content:
        content = re.sub(
            r'(@Composable\s*fun\s*[a-zA-Z0-9_]+\s*\([^)]*\)\s*\{)',
            r'\1\n    val currentUserId by authViewModel.currentUserId.collectAsState()',
            content
        )
        changed = True

    # Check mapping
    for method, vm in mapping.items():
        if f"authViewModel.{method}" in content:
            content = content.replace(f"authViewModel.{method}", f"{vm}.{method}")
            changed = True
            
    if changed:
        with open(filepath, 'w') as f:
            f.write(content)

for root, dirs, files in os.walk("app/src/main/java/com/example/ui"):
    for file in files:
        if file.endswith(".kt"):
            fix_file(os.path.join(root, file))

