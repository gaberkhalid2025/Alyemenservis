with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = lines[:492]
new_lines.append("}\n")

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.writelines(new_lines)
