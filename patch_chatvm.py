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

replacement = """    private var markAsReadJob: kotlinx.coroutines.Job? = null

    fun markAsRead(channelId: String, currentUserId: String) {
        // تجميع عمليات markAsRead لمدة 5 ثواني لمنع حرق حصة Firebase
        markAsReadJob?.cancel()
        markAsReadJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            val currentMsgs = _messages.value
            // تحقق إذا كان هناك أي رسائل غير مقروءة قبل الإرسال
            val hasUnread = currentMsgs.any { it.senderId != currentUserId && !it.readBy.contains(currentUserId) }
            if (hasUnread) {
                repository.markChannelAsRead(channelId, currentUserId)
            }
        }
    }"""

replace_between('app/src/main/java/com/example/ui/screens/chat/ChatViewModel.kt',
                '    fun markAsRead(channelId: String, currentUserId: String) {',
                '        }\n    }',
                replacement)
