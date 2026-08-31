import os
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    if "hiltViewModel" not in content:
        return

    # Replace hiltViewModel() with viewModel()
    content = content.replace("hiltViewModel()", "viewModel()")
    
    # Replace the hilt import with the standard viewmodel compose import
    content = content.replace("import androidx.hilt.navigation.compose.hiltViewModel", "import androidx.lifecycle.viewmodel.compose.viewModel")
    
    with open(filepath, 'w') as f:
        f.write(content)

for root, dirs, files in os.walk("app/src/main/java/com/example/ui"):
    for file in files:
        if file.endswith(".kt"):
            process_file(os.path.join(root, file))

