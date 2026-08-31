import re, os

# Collect all viewModel.XXX calls
methods = set()
for root, dirs, files in os.walk("app/src/main/java/com/example/ui"):
    for file in files:
        if file.endswith(".kt"):
            with open(os.path.join(root, file), 'r') as f:
                content = f.read()
                matches = re.findall(r'viewModel\.([a-zA-Z0-9_]+)', content)
                methods.update(matches)

# Print them out so we can classify them
for m in sorted(methods):
    print(m)
