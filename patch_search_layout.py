import re

with open("app/src/main/java/com/example/ui/screens/home/ServicesBrowserLayout.kt", "r") as f:
    content = f.read()

# We need to add stores, properties and products to the state collection
state_collections = """    val filteredProviders by viewModel.filteredProviders.collectAsState()
    val isProvidersLoading by viewModel.isProvidersLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val products by viewModel.products.collectAsState()"""

content = re.sub(r"    val filteredProviders by viewModel\.filteredProviders\.collectAsState\(\)\n    val isProvidersLoading by viewModel\.isProvidersLoading\.collectAsState\(\)\n    val selectedCategory by viewModel\.selectedCategoryId\.collectAsState\(\)\n    val searchQuery by viewModel\.searchQuery\.collectAsState\(\)", state_collections, content)

# Then we filter stores and properties based on searchQuery
# We can do this right before ServicesBrowserMainContent is called.

filtering_logic = """                else -> {
                    val searchLower = searchQuery.trim().lowercase()
                    val matchedStoreIds = if (searchLower.isNotEmpty()) {
                        products.filter { it.name.lowercase().contains(searchLower) || it.description.lowercase().contains(searchLower) }
                            .map { it.storeId }.toSet()
                    } else emptySet()
                    
                    val displayStores = if (searchLower.isNotEmpty()) {
                        stores.filter { store ->
                            store.name.lowercase().contains(searchLower) ||
                            store.phone.contains(searchLower) ||
                            store.categoryId.lowercase().contains(searchLower) ||
                            matchedStoreIds.contains(store.id)
                        }
                    } else emptyList()
                    
                    val displayProperties = if (searchLower.isNotEmpty()) {
                        properties.filter { prop ->
                            prop.title.lowercase().contains(searchLower) ||
                            prop.ownerPhone.contains(searchLower) ||
                            prop.cityId.lowercase().contains(searchLower) ||
                            prop.neighborhood.lowercase().contains(searchLower) ||
                            prop.propertyType.lowercase().contains(searchLower)
                        }
                    } else emptyList()

                    ServicesBrowserMainContent("""

content = content.replace("                else -> {\n                    ServicesBrowserMainContent(", filtering_logic)

with open("app/src/main/java/com/example/ui/screens/home/ServicesBrowserLayout.kt", "w") as f:
    f.write(content)
