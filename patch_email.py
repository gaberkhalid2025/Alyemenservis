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

replace_in_file('app/src/main/java/com/example/ui/screens/about/AboutContentRenderer.kt', '"mah73646@gmail.com"', '""')
replace_in_file('app/src/main/java/com/example/ui/viewmodels/SettingsViewModel.kt', 'adminUsername: String = "mah73646@gmail.com"', 'adminUsername: String = ""')
replace_in_file('app/src/main/java/com/example/ui/dialogs/BackdoorLoginDialog.kt', 'Text("mah73646@gmail.com",', 'Text("admin@example.com",')
replace_in_file('app/src/main/java/com/example/ui/MainViewModelDelegates.kt', 'adminUsername: String = "mah73646@gmail.com"', 'adminUsername: String = ""')
replace_in_file('app/src/main/java/com/example/data/models/AdminSettingsEntity.kt', 'val supportEmail: String = "mah73646@gmail.com"', 'val supportEmail: String = ""')
replace_in_file('app/src/main/java/com/example/data/models/AdminSettingsEntity.kt', 'val adminUsername: String = "mah73646@gmail.com"', 'val adminUsername: String = ""')
replace_in_file('app/src/main/java/com/example/data/models/AdminSettingsEntity.kt', 'val ownerEmail: String = "mah73646@gmail.com"', 'val ownerEmail: String = ""')

