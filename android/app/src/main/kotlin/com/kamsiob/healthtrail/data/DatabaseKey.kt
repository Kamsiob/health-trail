package com.kamsiob.healthtrail.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The passphrase that unlocks the database at rest.
 *
 * The arrangement, and why it has this shape:
 *
 * A 32 byte passphrase is generated once, from [SecureRandom], and it is what
 * SQLCipher actually opens the database with. That passphrase is never stored
 * in the clear. It is wrapped with an AES key that lives inside the Android
 * Keystore, and only the wrapped bytes are written to preferences.
 *
 * The Keystore key cannot be exported. On a device with secure hardware it does
 * not exist in ordinary memory at all, so the passphrase cannot be recovered by
 * reading the app's files, by pulling a backup, or by rooting the device and
 * copying preferences, without also being able to run code as this app on this
 * device.
 *
 * **This is not the same thing as the export passphrase, and the two must never
 * be conflated.** This key never leaves the device and the person never sees
 * it. An export is a portable file that has to open on a different device, so
 * it cannot depend on one device's keystore and uses a passphrase the person
 * chooses instead. See `contract/export-format.md`.
 *
 * Platform backup is switched off, so these wrapped bytes never leave the
 * device either. If they did, they would be useless without the Keystore key,
 * which does not travel.
 */
class DatabaseKey(private val context: Context) {

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "health_trail_db_key"
        private const val PREFS = "health_trail_key"
        private const val PREF_WRAPPED = "wrapped_passphrase"
        private const val PREF_IV = "wrap_iv"

        /** 256 bits. Longer buys nothing here and costs on every open. */
        private const val PASSPHRASE_BYTES = 32
        private const val GCM_TAG_BITS = 128
    }

    /**
     * The passphrase, generating and wrapping one on first call.
     *
     * Returned as a [ByteArray] rather than a String on purpose: a String
     * cannot be wiped, and would sit in the heap until the garbage collector
     * happened to move it. Callers should zero the array once SQLCipher has
     * taken it.
     */
    fun passphrase(): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wrapped = prefs.getString(PREF_WRAPPED, null)
        val iv = prefs.getString(PREF_IV, null)

        if (wrapped != null && iv != null) {
            return unwrap(
                Base64.decode(wrapped, Base64.NO_WRAP),
                Base64.decode(iv, Base64.NO_WRAP),
            )
        }

        val fresh = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, keystoreKey())
        }
        val sealed = cipher.doFinal(fresh)

        prefs.edit()
            .putString(PREF_WRAPPED, Base64.encodeToString(sealed, Base64.NO_WRAP))
            .putString(PREF_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .commit() // commit, not apply: losing this write loses the database

        return fresh
    }

    /** True once a passphrase exists, meaning the database has been created. */
    fun exists(): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .contains(PREF_WRAPPED)

    private fun unwrap(sealed: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(sealed)
    }

    private fun keystoreKey(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // Deliberately not requiring user authentication. The person
                // using this app is often in a hallway with one hand free, and
                // an app that demands a fingerprint before it will show the
                // emergency card has failed at the moment it mattered most.
                // The device lock is the boundary this app relies on, and
                // SECURITY.md says so.
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }
}
