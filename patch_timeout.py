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

replacement = """                        when (resetStatus) {
                            "TIMEOUT" -> {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Text(
                                    text = "⏳ جارٍ المراجعة...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "يمكنك إغلاق هذه الشاشة والمتابعة لاحقاً، سيتم تنبيهك عند قبول الطلب.",
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.surfaceVariant)
                                ) {
                                    Text("حسناً، إغلاق", color = Color.White)
                                }
                            }
                            "PENDING" -> {"""

replace_between('app/src/main/java/com/example/ui/dialogs/ForgotPasswordRecoveryDialog.kt',
                '                        when (resetStatus) {\n                            "PENDING" -> {',
                '                        when (resetStatus) {\n                            "PENDING" -> {',
                replacement)
