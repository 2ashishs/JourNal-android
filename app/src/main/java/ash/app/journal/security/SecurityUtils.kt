package ash.app.journal.security

import java.security.MessageDigest

object SecurityUtils {
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(enteredPin: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrBlank()) return false
        return hashPin(enteredPin) == storedHash
    }
}