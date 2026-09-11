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

replacement = """        DisposableEffect(phoneInput) {
            val cleanPhone = phoneInput.trim().replace(" ", "")
            val listener = viewModel.db.collection("password_resets").document(cleanPhone)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("status") ?: "PENDING"
                        resetStatus = status
                        if (status == "APPROVED") {
                            tempPassword = snapshot.getString("tempPassword") ?: snapshot.getString("newPassword") ?: ""
                            sharedPrefs.edit().clear().apply()
                        } else if (status == "REJECTED") {
                            sharedPrefs.edit().clear().apply()
                        }
                    }
                }
            
            // Timeout logic
            val timeoutJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                kotlinx.coroutines.delay(5 * 60 * 1000) // 5 minutes timeout
                if (resetStatus == "PENDING") {
                    resetStatus = "TIMEOUT"
                    sharedPrefs.edit().clear().apply()
                }
            }

            onDispose {
                listener.remove()
                timeoutJob.cancel()
            }
        }"""

replace_between('app/src/main/java/com/example/ui/dialogs/ForgotPasswordRecoveryDialog.kt',
                '    if (isSubmitted) {\n        LaunchedEffect(phoneInput) {',
                '                kotlinx.coroutines.delay(3000)\n            }\n        }',
                replacement)
