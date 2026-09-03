with open("app/src/main/java/com/example/ui/navigation/AppNavigator.kt", "r") as f:
    content = f.read()

old_code = """            FloatingIconsOverlay(
                settings = settingsState,
                themeColors = themeColors,
                onAssistantClick = { showAssistantDialog = true },
                onRequestServiceClick = { showRequestServiceModal = true }
            )"""

new_code = """            if (currentScreen == AppScreens.USER_BROWSE || currentScreen == AppScreens.HOME || currentScreen == AppScreens.FAVORITES_VIEW) {
                FloatingIconsOverlay(
                    settings = settingsState,
                    themeColors = themeColors,
                    onAssistantClick = { showAssistantDialog = true },
                    onRequestServiceClick = { showRequestServiceModal = true }
                )
            }"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/ui/navigation/AppNavigator.kt", "w") as f:
    f.write(content)
