import re

def process_file(filepath):
    print(f"Processing {filepath}")
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Add MainViewModel import
    if "import com.example.ui.MainViewModel" not in content:
        # Insert after package declaration
        package_match = re.search(r'package\s+[a-zA-Z0-9\.]+', content)
        if package_match:
            end_package = package_match.end()
            content = content[:end_package] + "\n\nimport com.example.ui.MainViewModel" + content[end_package:]
            
    # 2. Change private val _ to internal val _
    content = re.sub(r'private val _', 'internal val _', content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

process_file('app/src/main/java/com/example/ui/viewmodels/AdminViewModel.kt')
process_file('app/src/main/java/com/example/ui/viewmodels/SettingsViewModel.kt')
process_file('app/src/main/java/com/example/ui/viewmodels/InstantRequestViewModel.kt')

print("Modified visibility and imports successfully!")
