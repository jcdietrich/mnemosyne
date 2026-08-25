package com.mnemosyne.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages encryption and decryption of sensitive memory content using AES-256-GCM.
 * Keys are stored in the hardware-backed Android KeyStore where available.
 * Governs R11, R12, KD5, KTD4.
 */
@Singleton
open class CryptoManager @Inject constructor() {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "mnemosyne_key"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH_BYTES = 12
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private fun getOrCreateKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM with a random IV.
     * Output format: [12-byte IV] + [ciphertext + 16-byte GCM tag].
     */
    open fun encrypt(plaintext: ByteArray, customKey: SecretKey? = null): ByteArray {
        val key = customKey ?: getOrCreateKey()
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    /**
     * Decrypts combined IV + ciphertext bytes using AES-256-GCM.
     */
    open fun decrypt(encryptedBytes: ByteArray, customKey: SecretKey? = null): ByteArray {
        require(encryptedBytes.size > IV_LENGTH_BYTES) { "Ciphertext too short" }
        val key = customKey ?: getOrCreateKey()
        val iv = encryptedBytes.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = encryptedBytes.copyOfRange(IV_LENGTH_BYTES, encryptedBytes.size)
        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(ciphertext)
    }

    /**
     * Helper to encrypt a string and return a Base64-encoded string representation.
     */
    fun encryptString(plaintext: String): String {
        val encrypted = encrypt(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)
    }

    /**
     * Helper to decrypt a Base64-encoded ciphertext string.
     */
    fun decryptString(base64Ciphertext: String): String {
        val decoded = Base64.getDecoder().decode(base64Ciphertext)
        return String(decrypt(decoded), Charsets.UTF_8)
    }
}
