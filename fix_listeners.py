import re

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r") as f:
    content = f.read()

# First, undo the incorrect 'reg(db.collection' replacements where there is no matching closing parenthesis for reg.
# Actually, the simplest is to find all 'reg(db.collection' and replace with 'db.collection'
content = content.replace("reg(db.collection", "db.collection")

# Now properly wrap all db.collection(...).addSnapshotListener { ... }
# It's tricky because the closing brace is many lines down.
# Let's just do a regex that finds 'db.collection(....addSnapshotListener {' and we'll manually or via regex add the closing ')' at the end of the block.
# Actually, let's just restore the file from an earlier state if possible, or I can just fix it.

# Let's write the content back first with 'reg(' removed so it's back to normal.
with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w") as f:
    f.write(content)
