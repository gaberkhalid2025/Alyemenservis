with open("app/src/main/java/com/example/ui/screens/chat/ChatScreen.kt", "r") as f:
    content = f.read()

content = content.replace("val isTyping by viewModel.isPeerTyping.collectAsState()", "val isTyping by chatViewModel.isPeerTyping.collectAsState()")
content = content.replace("userName = activeChannel?.participantsNames?.firstOrNull { it != currentUserName } ?: \"\"", "userName = activeChannel?.participantNames?.values?.firstOrNull { it != currentUserName } ?: \"\"")

with open("app/src/main/java/com/example/ui/screens/chat/ChatScreen.kt", "w") as f:
    f.write(content)
