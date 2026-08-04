package com.kamsiob.healthtrail.data

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/**
 * Key derivation and payload encryption for the export file.
 *
 * **Argon2id and AES-256-GCM, exactly as `contract/EXPORT-FORMAT.md` names
 * them.** Neither is a preference and neither is negotiable, which is the point
 * of D51.
 *
 * **Why not PBKDF2, which is already in the platform and would need no
 * dependency.** Per D24 the export file is **the only recovery path from key
 * loss**, which makes it the most security sensitive artifact this project
 * produces: the database on the phone is protected by a Keystore key that never
 * leaves the device, and if that key is gone this file is what is left.
 *
 * PBKDF2 needs almost no memory, so an attacker with parallel hardware gets
 * enormous advantage per dollar. Argon2id is memory-hard specifically to remove
 * that. The substitution would also be **invisible**: a file encrypted with
 * PBKDF2 looks exactly as safe as one encrypted properly, and nothing in the
 * app or the file would say otherwise.
 *
 * **Argon2 comes from Bouncy Castle, AES from the platform.** `Argon2Bytes
 * Generator` is pure Java, so it adds no native library and no NDK step.
 * AES-256-GCM is in the JCE on every supported version, so there is no reason
 * to take a second implementation of it.
 */
internal object ExportCrypto {

    /**
     * The cost this build writes.
     *
     * **Above the OWASP baseline rather than at it**, which the format already
     * specified before this was implemented. 64 MiB with three passes is well
     * inside what a phone can do for a one-off export, and the export is not a
     * login: it happens rarely and the person expects it to take a moment.
     *
     * **These are written into the manifest of every file**, and an importer
     * reads them from the file rather than using these constants. That is what
     * lets the cost rise later without stranding anything. See [derive].
     */
    const val ITERATIONS = 3
    const val MEMORY_KIB = 65536
    const val PARALLELISM = 1

    const val KEY_BITS = 256
    const val SALT_BYTES = 16

    /** 96 bits, which is the size GCM is specified around. */
    const val NONCE_BYTES = 12

    /** 128 bits, the full tag. Truncating it is a weaker authentication claim. */
    const val TAG_BITS = 128

    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    /**
     * Derives the key, using the parameters given rather than the constants.
     *
     * **The caller passes the cost in, and on import it comes from the file.**
     * Hardware gets faster, the recommended cost goes up with it, and a file
     * written in 2026 has to still open in 2031 against whatever it was written
     * with. An importer that assumed today's constants would fail to derive the
     * right key from a **correct** passphrase and would report it as a wrong
     * passphrase, which is the worst available failure for somebody's only
     * copy: it tells them their memory is wrong when their file is fine.
     *
     * **The passphrase is taken as a CharArray and cleared by the caller.** A
     * String cannot be wiped, and a passphrase that stays in the heap until
     * garbage collection is a passphrase in a heap dump.
     */
    fun derive(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int = ITERATIONS,
        memoryKib: Int = MEMORY_KIB,
        parallelism: Int = PARALLELISM,
    ): ByteArray {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(iterations)
            .withMemoryAsKB(memoryKib)
            .withParallelism(parallelism)
            .withSalt(salt)
            .build()

        val generator = Argon2BytesGenerator().apply { init(parameters) }
        val key = ByteArray(KEY_BITS / 8)
        generator.generateBytes(passphrase, key)
        return key
    }

    fun randomSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun randomNonce(): ByteArray = ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) }

    /**
     * Encrypts, returning ciphertext with the authentication tag appended.
     *
     * GCM authenticates as well as encrypts, so a file altered by one byte
     * fails to decrypt rather than decrypting into something subtly wrong. For
     * a medical record that distinction is the whole reason to prefer it.
     */
    fun encrypt(key: ByteArray, nonce: ByteArray, plaintext: ByteArray): ByteArray =
        cipher(Cipher.ENCRYPT_MODE, key, nonce).doFinal(plaintext)

    /**
     * Decrypts, or throws.
     *
     * **A wrong passphrase and a tampered file are the same exception here**,
     * because GCM cannot tell them apart: both mean the tag did not verify. The
     * caller decides what to say, and it must not claim to know which. Telling
     * somebody their file is corrupt when they mistyped is as bad as the
     * reverse.
     */
    fun decrypt(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray): ByteArray =
        cipher(Cipher.DECRYPT_MODE, key, nonce).doFinal(ciphertext)

    private fun cipher(mode: Int, key: ByteArray, nonce: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        }

    /** Overwrites a key or a derived secret in place, so it does not linger. */
    fun wipe(bytes: ByteArray) = bytes.fill(0)
}
