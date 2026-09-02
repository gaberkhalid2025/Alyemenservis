with open("app/src/main/java/com/example/ui/screens/home/ServicesBrowserContent.kt", "r") as f:
    content = f.read()

args_old = """    displayProviders: List<ProviderEntity>,
    isProvidersLoading: Boolean,"""

args_new = """    displayProviders: List<ProviderEntity>,
    displayStores: List<StoreEntity> = emptyList(),
    displayProperties: List<PropertyEntity> = emptyList(),
    isProvidersLoading: Boolean,"""

content = content.replace(args_old, args_new)

with open("app/src/main/java/com/example/ui/screens/home/ServicesBrowserContent.kt", "w") as f:
    f.write(content)
