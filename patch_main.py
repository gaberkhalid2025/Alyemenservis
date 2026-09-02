with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r") as f:
    content = f.read()

# First, remove reg( if it precedes db.collection
content = content.replace("reg(db.collection", "db.collection")

# Now replace addSnapshotListener { with addSnapshotListenerReg {
content = content.replace(".addSnapshotListener {", ".addSnapshotListenerReg {")

# Also, there might be a dangling ')' at the end of the block for those that were manually wrapped before.
# Let's hope it wasn't too many, or we can just try to build and fix the ones that fail.
# Actually, wait, let me just run compilation to see the errors.

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w") as f:
    f.write(content)
