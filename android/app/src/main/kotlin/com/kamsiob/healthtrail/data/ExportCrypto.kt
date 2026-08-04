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
     * How much plaintext goes into one frame of the payload.
     *
     * **The payload is encrypted in frames rather than in one call**, because a
     * single call needs the whole archive in memory at once and the contract
     * requires this to work past four gigabytes. A megabyte is large enough that
     * the per frame overhead is nothing and small enough that the phone never
     * holds more than a megabyte of somebody's record in the clear.
     */
    const val CHUNK_BYTES = 1 shl 20

    /** The random half of a frame nonce. The other eight bytes are the counter. */
    const val NONCE_PREFIX_BYTES = 4

    fun randomNoncePrefix(): ByteArray =
        ByteArray(NONCE_PREFIX_BYTES).also { SecureRandom().nextBytes(it) }

    /**
     * The nonce for one frame: a per file random prefix and a counter.
     *
     * **Never random per frame.** Random 96 bit nonces collide at a rate that is
     * fine for a handful of messages and not fine for the millions of frames a
     * large archive would have, and a collision under one key breaks GCM
     * outright. A counter cannot collide within a file, and the prefix is what
     * keeps two files from sharing a nonce space.
     */
    fun chunkNonce(prefix: ByteArray, index: Long): ByteArray {
        require(prefix.size == NONCE_PREFIX_BYTES) { "the nonce prefix is the wrong size" }
        val nonce = ByteArray(NONCE_BYTES)
        prefix.copyInto(nonce)
        for (byte in 0 until 8) {
            nonce[NONCE_PREFIX_BYTES + byte] = (index ushr (56 - 8 * byte)).toByte()
        }
        return nonce
    }

    /**
     * What each frame authenticates besides its own bytes: where it sits and
     * whether the file ends with it.
     */
    fun frameAad(index: Long, last: Boolean): ByteArray {
        val aad = ByteArray(9)
        for (byte in 0 until 8) aad[byte] = (index ushr (56 - 8 * byte)).toByte()
        aad[8] = if (last) 1 else 0
        return aad
    }

    /**
     * Encrypts, returning ciphertext with the authentication tag appended.
     *
     * GCM authenticates as well as encrypts, so a file altered by one byte
     * fails to decrypt rather than decrypting into something subtly wrong. For
     * a medical record that distinction is the whole reason to prefer it.
     */
    fun encrypt(
        key: ByteArray,
        nonce: ByteArray,
        plaintext: ByteArray,
        /**
         * Authenticated but not encrypted, and the payload's frames use it to
         * carry their own position and whether they are the last.
         *
         * **Belt and braces, and this says so rather than overclaiming.** Two
         * other things already resist the attacks this is aimed at: a frame
         * moved to another position decrypts under the wrong nonce, because the
         * nonce carries the counter, and a payload with its tail cut off leaves
         * a zip whose central directory is gone. Both were probed by removing
         * this binding and watching the checks stay green.
         *
         * It is kept because it costs nothing and because the structural
         * protection is an accident of what is inside the payload today. The day
         * something other than a zip goes in there, this is what is left.
         */
        aad: ByteArray? = null,
    ): ByteArray =
        cipher(Cipher.ENCRYPT_MODE, key, nonce).also { aad?.let(it::updateAAD) }.doFinal(plaintext)

    /**
     * Decrypts, or throws.
     *
     * **A wrong passphrase and a tampered file are the same exception here**,
     * because GCM cannot tell them apart: both mean the tag did not verify. The
     * caller decides what to say, and it must not claim to know which. Telling
     * somebody their file is corrupt when they mistyped is as bad as the
     * reverse.
     */
    fun decrypt(
        key: ByteArray,
        nonce: ByteArray,
        ciphertext: ByteArray,
        aad: ByteArray? = null,
    ): ByteArray =
        cipher(Cipher.DECRYPT_MODE, key, nonce).also { aad?.let(it::updateAAD) }.doFinal(ciphertext)

    private fun cipher(mode: Int, key: ByteArray, nonce: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        }

    /** Overwrites a key or a derived secret in place, so it does not linger. */
    fun wipe(bytes: ByteArray) = bytes.fill(0)
}
