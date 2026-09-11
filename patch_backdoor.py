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

replacement = """                            isAuthenticating = true
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                try {
                                    val result = com.example.utils.AdminSecurityManager.verifyCredentials(trimmedUser, trimmedPass, settingsState)
                                    when (result) {
                                        "OWNER" -> {
                                            onDismiss()
                                            viewModel.authenticateAdmin(context, "OWNER", rememberMe)
                                            viewModel.triggerNotification("🔓 مرحباً بك في البوابة الخلفية بصلاحية المالك!")
                                        }
                                        "ADMIN" -> {
                                            onDismiss()
                                            viewModel.authenticateAdmin(context, "ADMIN", rememberMe)
                                            viewModel.triggerNotification("🔓 مرحباً بك بصلاحية مدير النظام!")
                                        }
                                        "SUPERVISOR" -> {
                                            val matchingSup = supervisors.find { it.id == trimmedUser || it.name.trim().equals(trimmedUser, ignoreCase = true) }
                                            if (matchingSup != null) {
                                                viewModel.setSupervisorSession(matchingSup)
                                                if (rememberMe) {
                                                    val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
                                                    sp.edit().putString("saved_admin_role", "SUPERVISOR").apply()
                                                }
                                                onDismiss()
                                                viewModel.authenticateAdmin(context, "SUPERVISOR", rememberMe)
                                                viewModel.triggerNotification("🔓 مرحباً بك المشرف: ${matchingSup.name}")
                                            } else {
                                                viewModel.triggerNotification("❌ بيانات الدخول غير صحيحة!")
                                            }
                                        }
                                        else -> {
                                            viewModel.triggerNotification("❌ بيانات الدخول غير صحيحة!")
                                        }
                                    }
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                    viewModel.triggerNotification("❌ حدث خطأ غير متوقع. حاول مرة أخرى.")
                                } finally {
                                    isAuthenticating = false
                                }
                            }"""

replace_between('app/src/main/java/com/example/ui/dialogs/BackdoorLoginDialog.kt',
                '                            isAuthenticating = true\n                            try {',
                '                                isAuthenticating = false\n                            }',
                replacement)
