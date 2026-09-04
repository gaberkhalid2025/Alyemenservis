#!/bin/bash
sed -i 's/authViewModel.currentUser.value?.uid ?: return/authViewModel.currentUserId.value.ifBlank { return }/g' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i 's/authViewModel.currentUser.value?.displayName/authViewModel.currentUserName.value/g' app/src/main/java/com/example/ui/MainViewModel.kt
sed -i 's/authViewModel.currentUser.value?.photoUrl ?: ""/""/g' app/src/main/java/com/example/ui/MainViewModel.kt
