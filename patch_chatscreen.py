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

replacement = """    val activeChannel = currentChannel

    // Mark as read only when opening or closing the chat, not on every single message
    DisposableEffect(activeChannel) {
        val chId = activeChannel?.id ?: channelId
        if (!chId.isNullOrBlank()) {
            chatViewModel.markAsRead(chId, currentUserId)
        }
        onDispose {
            if (!chId.isNullOrBlank()) {
                chatViewModel.markAsRead(chId, currentUserId)
            }
        }
    }"""

replace_between('app/src/main/java/com/example/ui/screens/chat/ChatScreen.kt',
                '    val activeChannel = currentChannel\n\n    // Automatically mark channel as read whenever messages arrive',
                '        }\n    }',
                replacement)
