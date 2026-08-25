package com.mnemosyne.crypto

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class CryptoManagerTest {

    private val testKey: SecretKey by lazy {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        keyGen.generateKey()
    }

    private val cryptoManager = CryptoManager()

    @Test
    fun `encrypt and decrypt round trip preserves original plaintext`() {
        val originalText = "I remembered meeting Alice at the coffee shop on 5th Street."
        val plaintextBytes = originalText.toByteArray(Charsets.UTF_8)

        val encrypted = cryptoManager.encrypt(plaintextBytes, customKey = testKey)
        val decryptedBytes = cryptoManager.decrypt(encrypted, customKey = testKey)
        val decryptedText = String(decryptedBytes, Charsets.UTF_8)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun `encrypting same plaintext twice produces different ciphertexts due to random IV`() {
        val text = "Consistent note text"
        val bytes = text.toByteArray(Charsets.UTF_8)

        val enc1 = cryptoManager.encrypt(bytes, customKey = testKey)
        val enc2 = cryptoManager.encrypt(bytes, customKey = testKey)

        assertFalse("Ciphertexts must differ due to random IVs", enc1.contentEquals(enc2))

        val dec1 = String(cryptoManager.decrypt(enc1, customKey = testKey), Charsets.UTF_8)
        val dec2 = String(cryptoManager.decrypt(enc2, customKey = testKey), Charsets.UTF_8)

        assertEquals(text, dec1)
        assertEquals(text, dec2)
    }

    @Test
    fun `decrypt throws exception on corrupt or truncated ciphertext`() {
        val invalidBytes = ByteArray(5) { 0 }
        assertThrows(IllegalArgumentException::class.java) {
            cryptoManager.decrypt(invalidBytes, customKey = testKey)
        }
    }
}
