with open("app/src/main/java/com/example/ui/screens/home/ServicesBrowserContent.kt", "r") as f:
    content = f.read()

old_render = """        } else if (displayProviders.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🔍 لا توجد نتائج مطابقة لبحثك", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("جرب تغيير معايير البحث أو اختيار قسم آخر لعرض المتاحين باليمن.", fontSize = 10.5.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                }
            }
        } else {
            val limitedProviders = displayProviders.take(providersLimit)
            limitedProviders.forEach { provider ->
                ProviderCard(
                    provider = provider,
                    themeColors = themeColors,
                    viewModel = viewModel,
                    onChatOpen = onChatOpen
                )
            }
            if (displayProviders.size > providersLimit) {
                Button(
                    onClick = onLoadMore,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("عرض المزيد من الفنيين (${displayProviders.size - providersLimit} متبقي) ⬇️", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }"""

new_render = """        } else if (displayProviders.isEmpty() && displayStores.isEmpty() && displayProperties.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🔍 لا توجد نتائج مطابقة لبحثك", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("جرب تغيير معايير البحث أو اختيار قسم آخر لعرض المتاحين باليمن.", fontSize = 10.5.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                }
            }
        } else {
            displayStores.forEach { store ->
                com.example.ui.screens.entities.StoreItemCard(
                    store = store,
                    themeColors = themeColors,
                    isLoggedIn = true,
                    onClick = { onStoreClick(store) },
                    onChatClick = { onChatOpen(store.ownerPhone) },
                    onRequestServiceClick = { }
                )
            }
            
            displayProperties.forEach { prop ->
                com.example.ui.screens.entities.PropertyCard(
                    property = prop,
                    themeColors = themeColors,
                    isLoggedIn = true,
                    onClick = { onPropertyClick(prop) },
                    onChatClick = { onChatOpen(prop.ownerPhone) },
                    onRequestInspectionClick = { }
                )
            }
            
            val limitedProviders = displayProviders.take(providersLimit)
            limitedProviders.forEach { provider ->
                ProviderCard(
                    provider = provider,
                    themeColors = themeColors,
                    viewModel = viewModel,
                    onChatOpen = onChatOpen
                )
            }
            if (displayProviders.size > providersLimit) {
                Button(
                    onClick = onLoadMore,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("عرض المزيد من الفنيين (${displayProviders.size - providersLimit} متبقي) ⬇️", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }"""

content = content.replace(old_render, new_render)

with open("app/src/main/java/com/example/ui/screens/home/ServicesBrowserContent.kt", "w") as f:
    f.write(content)
