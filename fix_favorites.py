import re

filepath = "app/src/main/java/com/example/ui/screens/home/FavoritesScreenLayout.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.foundation.lazy.LazyColumn\n", "")
content = content.replace("import androidx.compose.foundation.lazy.items\n", "")

# Replace LazyColumn with Column
content = re.sub(
    r"LazyColumn\(\s*modifier = Modifier\.fillMaxSize\(\),\s*verticalArrangement = Arrangement\.spacedBy\(8\.dp\)\s*\)\s*\{",
    r"Column(\n                    modifier = Modifier.fillMaxSize(),\n                    verticalArrangement = Arrangement.spacedBy(8.dp)\n                ) {",
    content
)

# Replace item { ... } with just ...
content = re.sub(r"item\s*\{\s*(.*?)\s*\}", r"\1", content, flags=re.DOTALL)

# Replace items(favoriteProviders) { provider -> ... } with favoriteProviders.forEach { provider -> ... }
content = re.sub(
    r"items\((\w+)\)\s*\{\s*(\w+)\s*->",
    r"\1.forEach { \2 ->",
    content
)

with open(filepath, "w") as f:
    f.write(content)

