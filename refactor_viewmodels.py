import os
import re

ui_dir = "app/src/main/java/com/example/ui"

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    if "MainViewModel" not in content:
        return

    # To be continued...
