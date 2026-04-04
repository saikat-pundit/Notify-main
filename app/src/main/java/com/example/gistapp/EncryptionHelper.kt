package com.example.gistapp

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    // THE MASTER PASSWORD: 
    // This MUST match the password in your Logger app exactly!
    private const val SHARED_PASSWORD = "MySuperSecretEncryptionPassword123!"

    private val secretKey: SecretKeySpec
        get() {
            val digest = MessageDigest.getInstance("SHA-256")
            val keyBytes = digest.digest(SHARED_PASSWORD.toByteArray(Charsets.UTF_8))
            return SecretKeySpec(keyBytes, "AES")
        }

    private val iv = IvParameterSpec(ByteArray(16) { 0 })

    fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
            
            // Clean up the string just in case GitHub adds newlines or spaces to the Base64
            val cleanBase64 = encryptedBase64.replace("\n", "").replace("\r", "").trim()
            
            val decodedBytes = Base64.decode(cleanBase64, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "" // Returns empty if decryption fails
        }
    }
}
