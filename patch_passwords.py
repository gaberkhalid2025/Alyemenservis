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

replace_in_file('app/src/main/java/com/example/ui/viewmodels/SettingsViewModel.kt', 'adminPassword: String = "Maher@@--@@736462##"', 'adminPassword: String = ""')
replace_in_file('app/src/main/java/com/example/ui/MainViewModelDelegates.kt', 'adminPassword: String = "Maher@@--@@736462##"', 'adminPassword: String = ""')
replace_in_file('app/src/main/java/com/example/ui/viewmodels/AuthViewModel.kt', 'if (trimmed == "Maher@@--@@736462##") return true', '')

