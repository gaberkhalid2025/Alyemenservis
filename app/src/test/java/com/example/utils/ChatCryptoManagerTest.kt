package com.example.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * 🧪 اختبارات تشفير المحادثات والرسائل (ChatCryptoManager)
 * تغطي التشفير المتماثل AES-256، اشتقاق المفاتيح PBKDF2، وفك التشفير.
 */
class ChatCryptoManagerTest {

    @Test
    fun `test encrypt produces enc prefix`() {
        val plain = "السلام عليكم ورحمة الله"
        val cipher = ChatCryptoManager.encrypt(plain, "channel_room_101")

        assertNotNull(cipher)
        assertTrue("Cipher must start with enc::", cipher.startsWith("enc::"))
        assertNotEquals(plain, cipher)
    }

    @Test
    fun `test encrypt and decrypt round trip succeeds`() {
        val original = "بيانات حساسة وسرية للطلب رقم 9988"
        val roomKey = "room_abc_123"

        val encrypted = ChatCryptoManager.encrypt(original, roomKey)
        val decrypted = ChatCryptoManager.decrypt(encrypted, roomKey)

        assertEquals(original, decrypted)
    }

    @Test
    fun `test blank text is returned as is without encryption`() {
        assertEquals("", ChatCryptoManager.encrypt("", "key"))
        assertEquals("   ", ChatCryptoManager.encrypt("   ", "key"))
    }

    @Test
    fun `test decrypting unencrypted text returns original string`() {
        val plain = "رسالة عادية غير مشفرة"
        val result = ChatCryptoManager.decrypt(plain, "key")
        assertEquals(plain, result)
    }

    @Test
    fun `test different room keys produce different ciphertexts`() {
        val text = "رسالة اختبار المفاتيح"
        val cipherA = ChatCryptoManager.encrypt(text, "key_channel_A")
        val cipherB = ChatCryptoManager.encrypt(text, "key_channel_B")

        assertNotEquals(cipherA, cipherB)
    }

    @Test
    fun `test decryption with wrong key fails securely`() {
        val text = "رسالة سرية جداً"
        val encrypted = ChatCryptoManager.encrypt(text, "correct_key_123")

        try {
            val decrypted = ChatCryptoManager.decrypt(encrypted, "wrong_key_999")
            // If it returns, it must not match the original plain text
            assertNotEquals(text, decrypted)
        } catch (e: SecurityException) {
            // Expected security exception on bad padding or decryption error
            assertTrue(true)
        }
    }
}
