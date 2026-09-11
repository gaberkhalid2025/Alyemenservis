import sys

def replace_in_file(filename, old_str, new_str):
    with open(filename, 'r') as f:
        content = f.read()
    
    if old_str in content:
        content = content.replace(old_str, new_str)
        with open(filename, 'w') as f:
            f.write(content)
        print(f"Patched {filename}")
    else:
        print(f"Not found in {filename}")

old_queue = """        val json = messagesListAdapter.toJson(pending)
        prefs.edit().putString(KEY_OFFLINE_PENDING_MSGS, json).apply()"""

new_queue = """        val json = messagesListAdapter.toJson(pending)
        val encrypted = ChatCryptoManager.encrypt(json, "ChatLocalKey_PENDING")
        prefs.edit().putString(KEY_OFFLINE_PENDING_MSGS, encrypted).apply()"""

old_remove = """        val json = messagesListAdapter.toJson(pending)
        prefs.edit().putString(KEY_OFFLINE_PENDING_MSGS, json).apply()"""

new_remove = """        val json = messagesListAdapter.toJson(pending)
        val encrypted = ChatCryptoManager.encrypt(json, "ChatLocalKey_PENDING")
        prefs.edit().putString(KEY_OFFLINE_PENDING_MSGS, encrypted).apply()"""

old_get_pending = """    private fun getPendingMessagesInternal(): List<ChatMessage> {
        val raw = prefs.getString(KEY_OFFLINE_PENDING_MSGS, null) ?: return emptyList()
        return try {
            messagesListAdapter.fromJson(raw) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }"""

new_get_pending = """    private fun getPendingMessagesInternal(): List<ChatMessage> {
        val rawEncrypted = prefs.getString(KEY_OFFLINE_PENDING_MSGS, null) ?: return emptyList()
        return try {
            val decrypted = if (rawEncrypted.startsWith("enc::")) {
                ChatCryptoManager.decrypt(rawEncrypted, "ChatLocalKey_PENDING")
            } else {
                SecurityCryptoUtils.decrypt(rawEncrypted)
            }
            if (decrypted.isNotBlank() && decrypted != "[]") {
                messagesListAdapter.fromJson(decrypted) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }"""

old_fallback = """        } catch (e: Exception) {
            // Fallback plain parse if not encrypted
            try {
                messagesListAdapter.fromJson(rawEncrypted) ?: emptyList()
            } catch (ex: Exception) {
                emptyList()
            }
        }"""

new_fallback = """        } catch (e: Exception) {
            emptyList()
        }"""

replace_in_file('app/src/main/java/com/example/data/local/ChatLocalDataSource.kt', old_queue, new_queue)
replace_in_file('app/src/main/java/com/example/data/local/ChatLocalDataSource.kt', old_get_pending, new_get_pending)
replace_in_file('app/src/main/java/com/example/data/local/ChatLocalDataSource.kt', old_fallback, new_fallback)

