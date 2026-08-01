package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The export's key derivation and payload encryption.
 *
 * **On the device rather than the JVM**, because the point is partly that this
 * works on the phone at a cost the phone can bear, and a desktop timing tells
 * nobody anything about that.
 */
@RunWith(AndroidJUnit4::class)
class ExportCryptoTest {

    private val passphrase get() = "correct horse battery staple".toCharArray()

    @Test
    fun whatGoesInComesOut() {
        val salt = ExportCrypto.randomSalt()
        val nonce = ExportCrypto.randomNonce()
        val key = ExportCrypto.derive(passphrase, salt)
        val plaintext = "a notebook, or a good approximation of one".toByteArray()

        val sealed = ExportCrypto.encrypt(key, nonce, plaintext)
        assertArrayEquals(plaintext, ExportCrypto.decrypt(key, nonce, sealed))
    }

    @Test
    fun theSamePassphraseAndSaltAlwaysGiveTheSameKey() {
        // Otherwise nothing written today opens tomorrow.
        val salt = ExportCrypto.randomSalt()
        assertArrayEquals(
            ExportCrypto.derive(passphrase, salt),
            ExportCrypto.derive(passphrase, salt),
        )
    }

    @Test
    fun aDifferentSaltGivesADifferentKeyForTheSamePassphrase() {
        // What the salt is for: two people with the same passphrase do not
        // share a key, and one cracked file does not open another.
        val a = ExportCrypto.derive(passphrase, ExportCrypto.randomSalt())
        val b = ExportCrypto.derive(passphrase, ExportCrypto.randomSalt())
        assertNotEquals(a.toList(), b.toList())
    }

    @Test
    fun theRecordedParametersAreWhatOpensTheFile() {
        // **The reason the cost lives in the manifest.** A file written at one
        // cost must open years later when the shipped cost has risen. An
        // importer that assumed today's constants would fail to derive the key
        // from a correct passphrase and report a wrong passphrase, which tells
        // the person their memory is wrong when their file is fine.
        val salt = ExportCrypto.randomSalt()
        val nonce = ExportCrypto.randomNonce()

        // A cheaper cost, as an older file would carry.
        val older = ExportCrypto.derive(passphrase, salt, iterations = 1, memoryKib = 8192)
        val sealed = ExportCrypto.encrypt(older, nonce, "old file".toByteArray())

        // Today's constants cannot open it, which is the failure being guarded.
        val today = ExportCrypto.derive(passphrase, salt)
        assertThrows(AEADBadTagException::class.java) {
            ExportCrypto.decrypt(today, nonce, sealed)
        }

        // The recorded ones can.
        val recorded = ExportCrypto.derive(passphrase, salt, iterations = 1, memoryKib = 8192)
        assertArrayEquals("old file".toByteArray(), ExportCrypto.decrypt(recorded, nonce, sealed))
    }

    @Test
    fun aWrongPassphraseFailsToAuthenticateRatherThanReturningRubbish() {
        val salt = ExportCrypto.randomSalt()
        val nonce = ExportCrypto.randomNonce()
        val sealed = ExportCrypto.encrypt(
            ExportCrypto.derive(passphrase, salt), nonce, "records".toByteArray(),
        )
        val wrong = ExportCrypto.derive("not the passphrase".toCharArray(), salt)

        assertThrows(AEADBadTagException::class.java) {
            ExportCrypto.decrypt(wrong, nonce, sealed)
        }
    }

    @Test
    fun oneAlteredByteFailsRatherThanDecryptingIntoSomethingSubtlyWrong() {
        // The reason GCM rather than a mode without authentication. For a
        // medical record, decrypting into plausible but altered content is far
        // worse than refusing.
        val salt = ExportCrypto.randomSalt()
        val nonce = ExportCrypto.randomNonce()
        val key = ExportCrypto.derive(passphrase, salt)
        val sealed = ExportCrypto.encrypt(key, nonce, "the dose was 5mg".toByteArray())

        sealed[4] = (sealed[4].toInt() xor 0x01).toByte()

        assertThrows(AEADBadTagException::class.java) {
            ExportCrypto.decrypt(key, nonce, sealed)
        }
    }

    @Test
    fun theSamePlaintextTwiceDoesNotProduceTheSameCiphertext() {
        // A fresh nonce per file. Reusing one under the same key is the failure
        // that breaks GCM outright rather than merely weakening it.
        val salt = ExportCrypto.randomSalt()
        val key = ExportCrypto.derive(passphrase, salt)
        val plaintext = "identical content".toByteArray()

        val first = ExportCrypto.encrypt(key, ExportCrypto.randomNonce(), plaintext)
        val second = ExportCrypto.encrypt(key, ExportCrypto.randomNonce(), plaintext)

        assertNotEquals(first.toList(), second.toList())
    }

    @Test
    fun theKeyIsTwoHundredAndFiftySixBitsAndTheSaltAndNonceAreNotConstant() {
        assertEquals(32, ExportCrypto.derive(passphrase, ExportCrypto.randomSalt()).size)
        assertEquals(16, ExportCrypto.randomSalt().size)
        assertEquals(12, ExportCrypto.randomNonce().size)
        assertNotEquals(
            ExportCrypto.randomSalt().toList(),
            ExportCrypto.randomSalt().toList(),
        )
    }

    @Test
    fun wipingActuallyClearsTheKey() {
        val key = ExportCrypto.derive(passphrase, ExportCrypto.randomSalt())
        ExportCrypto.wipe(key)
        assertTrue("the key was not cleared", key.all { it == 0.toByte() })
    }

    @Test
    fun derivingAtTheShippedCostIsBearableOnThisPhone() {
        // Not a benchmark. A floor check: if this takes minutes on the owner's
        // device the cost has to be tuned, and the format says to tune only on
        // a measurement rather than on a feeling.
        val salt = ExportCrypto.randomSalt()
        val started = System.currentTimeMillis()
        ExportCrypto.derive(passphrase, salt)
        val took = System.currentTimeMillis() - started

        assertTrue(
            "deriving took ${took}ms at ${ExportCrypto.MEMORY_KIB} KiB and " +
                "${ExportCrypto.ITERATIONS} passes, which is too slow to ask of somebody " +
                "exporting their notebook. Tune the cost and record it in the manifest.",
            took < 10_000,
        )
        println("Argon2id at the shipped cost took ${took}ms on this device")
    }
}
