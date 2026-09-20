package com.pensiunsehat.finansial.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertFailsWith

class BackupCipherTest {
    @Test
    fun encryptDecryptRoundTrip() {
        val plain = """{"backupVersion":1}""".toByteArray()
        val encrypted = BackupCipher.encrypt(plain, "passphrase-aman")
        val decrypted = BackupCipher.decrypt(encrypted, "passphrase-aman")
        assertEquals(String(plain), String(decrypted))
    }

    @Test
    fun decryptFailsWhenPayloadCorrupt() {
        val plain = """{"backupVersion":1}""".toByteArray()
        val encrypted = BackupCipher.encrypt(plain, "passphrase-aman")
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 0x01).toByte()
        assertFailsWith<Exception> {
            BackupCipher.decrypt(encrypted, "passphrase-aman")
        }
    }
}
