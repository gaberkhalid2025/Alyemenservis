with open("app/src/main/java/com/example/ui/screens/home/ServicesBrowserLayout.kt", "r") as f:
    content = f.read()

args = """                        displayProviders = filteredProviders,
                        displayStores = displayStores,
                        displayProperties = displayProperties,"""

content = content.replace("                        displayProviders = filteredProviders,", args)

with open("app/src/main/java/com/example/ui/screens/home/ServicesBrowserLayout.kt", "w") as f:
    f.write(content)
