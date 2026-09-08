import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    lines = f.readlines()

# We want to identify delegation functions and move them out.
# Let's find functions that look like `fun saveStore(...) = adminViewModel...`
# or `fun ... { adminViewModel... }`
