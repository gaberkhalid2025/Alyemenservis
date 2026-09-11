import sys

def replace_between(filename, start_marker, end_marker, replacement):
    with open(filename, 'r') as f:
        content = f.read()
    
    start_idx = content.find(start_marker)
    if start_idx == -1:
        print("Start marker not found")
        return
        
    end_idx = content.find(end_marker, start_idx)
    if end_idx == -1:
        print("End marker not found")
        return
        
    end_idx += len(end_marker)
    
    new_content = content[:start_idx] + replacement + content[end_idx:]
    
    with open(filename, 'w') as f:
        f.write(new_content)
    print("Success")

replacement = """                    // التحقق السحابي الآمن
                    androidx.lifecycle.viewModelScope.launch {
                        val result = com.example.utils.AdminSecurityManager.verifyCredentials(inputUsername, inputPassword, settingsState)
                        when (result) {
                            "OWNER" -> {
                                isAuthorized = true
                                activeSubTab = "BACKDOOR"
                                viewModel.authenticateAdmin(context, "OWNER", rememberMe)
                                viewModel.triggerNotification("👑 مرحباً بك مالك التطبيق في لوحة التحكم والإعدادات!")
                                isLoading = false
                            }
                            "ADMIN" -> {
                                isAuthorized = true
                                activeSubTab = "REG_REQ"
                                viewModel.authenticateAdmin(context, "ADMIN", rememberMe)
                                viewModel.triggerNotification("👑 مرحباً بك مدير المنصة في لوحة التحكم والإعدادات!")
                                isLoading = false
                            }
                            "SUPERVISOR" -> {
                                val matchingSup = viewModel.supervisors.value.find { it.id == inputUsername || it.name.trim().equals(inputUsername.trim(), ignoreCase = true) }
                                if (matchingSup != null) {
                                    isAuthorized = true
                                    viewModel.setSupervisorSession(matchingSup)
                                    if (rememberMe) {
                                        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
                                        sp.edit().putString("saved_admin_role", "SUPERVISOR").apply()
                                    }
                                    viewModel.triggerNotification("🔓 مرحباً بك المشرف: ${matchingSup.name}")
                                } else {
                                    viewModel.triggerNotification("❌ بيانات الدخول غير صحيحة!")
                                }
                                isLoading = false
                            }
                            else -> {
                                viewModel.triggerNotification("❌ بيانات الدخول غير صحيحة!")
                                isLoading = false
                            }
                        }
                    }"""

replace_between('app/src/main/java/com/example/ui/screens/admin/AdminPanelLayout.kt',
                '// استخدام AdminSecurityManager للتحقق',
                'isLoading = false\n                            }\n                        }\n                    }',
                replacement)
