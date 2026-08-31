import re, os

mapping = {
    'cancelOrResetJoinRequest': 'registrationViewModel',
    'setUserSessionDetails': 'authViewModel',
    'submitJoinForm': 'registrationViewModel',
    'resetRegistrationState': 'registrationViewModel',
    'openChatChannel': 'chatViewModel',
    'isProviderUser': 'authViewModel',
}

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    changed = False
    
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
            process_file(os.path.join(root, file))

