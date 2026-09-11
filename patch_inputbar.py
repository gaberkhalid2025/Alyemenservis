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

replacement = """    var showAttachmentMenu by remember { mutableStateOf(false) }

    // إصلاح تسريب MediaRecorder
    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                try {
                    mediaRecorder?.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                mediaRecorder?.release()
                mediaRecorder = null
                audioFile?.delete()
            }
        }
    }

    LaunchedEffect(editingMessage) {"""

replace_between('app/src/main/java/com/example/ui/screens/chat/components/ChatInputBar.kt',
                '    var showAttachmentMenu by remember { mutableStateOf(false) }\n\n    LaunchedEffect(editingMessage) {',
                '    LaunchedEffect(editingMessage) {',
                replacement)
