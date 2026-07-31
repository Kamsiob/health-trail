package com.kamsiob.healthtrail.data

import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicLong

/**
 * Row ids, generated locally, collision safe, and ordered by creation time.
 *
 * Why this shape and not an integer. Two devices both creating row 47 has no
 * correct merge, and fixing that after real data exists means reassigning every
 * id and every foreign key on someone's records. So ids are generated on the
 * device that creates the row, are never reused, and are the only thing a
 * foreign key ever points at.
 *
 * Why time ordered rather than fully random. A random id scatters inserts
 * across the whole index, which costs on write and costs again on any range
 * scan. A time ordered id appends, and it gives a stable natural ordering for
 * free, which matters in an app whose central object is a chronological trail.
 *
 * The format is UUID version 7: 48 bits of Unix milliseconds, then 74 bits of
 * randomness, with the version and variant bits set as the specification
 * requires. Rendered as the ordinary 36 character hyphenated form, so anyone
 * opening the database with a SQLite browser in ten years sees something they
 * recognize.
 *
 * Ordering within a millisecond is handled explicitly rather than left to luck:
 * ids generated in the same millisecond increment a counter in the high random
 * bits, so a tight loop of inserts still sorts in the order it ran. Without
 * that, two rows written in the same millisecond sort arbitrarily, and the
 * trail would show them in an order that changes between queries.
 */
object Ids {

    private val random = SecureRandom()

    private val lastMillis = AtomicLong(-1L)
    private val counter = AtomicLong(0L)

    private const val COUNTER_BITS = 12
    private const val COUNTER_MASK = (1L shl COUNTER_BITS) - 1

    /** A new id. Safe to call from any thread. */
    fun new(nowMillis: Long = System.currentTimeMillis()): String {
        val (millis, sequence) = nextSequence(nowMillis)

        val bytes = ByteArray(16)
        random.nextBytes(bytes)

        // 48 bits of milliseconds, big endian, in bytes 0 through 5.
        bytes[0] = (millis ushr 40).toByte()
        bytes[1] = (millis ushr 32).toByte()
        bytes[2] = (millis ushr 24).toByte()
        bytes[3] = (millis ushr 16).toByte()
        bytes[4] = (millis ushr 8).toByte()
        bytes[5] = millis.toByte()

        // Version 7 in the high nibble of byte 6, then 12 bits of sequence
        // across the rest of byte 6 and byte 7. Putting the sequence in the
        // most significant random bits is what makes same millisecond ids sort
        // in generation order.
        bytes[6] = (0x70 or ((sequence ushr 8).toInt() and 0x0F)).toByte()
        bytes[7] = (sequence and 0xFF).toByte()

        // Variant 10 in the two most significant bits of byte 8.
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        return format(bytes)
    }

    /**
     * The millisecond to stamp and the sequence within it.
     *
     * If the clock goes backward, which happens when a timezone change or a
     * manual clock set lands mid session, the last millisecond seen is reused
     * rather than the earlier one. Ids must never go backward, because they are
     * the natural ordering of the trail, and a person whose phone corrected its
     * clock should not find yesterday's call filed after today's.
     */
    private fun nextSequence(nowMillis: Long): Pair<Long, Long> {
        while (true) {
            val previous = lastMillis.get()
            val millis = if (nowMillis > previous) nowMillis else previous

            if (millis != previous) {
                if (lastMillis.compareAndSet(previous, millis)) {
                    counter.set(0L)
                    return millis to 0L
                }
                continue
            }

            val sequence = counter.incrementAndGet()
            if (sequence <= COUNTER_MASK) {
                return millis to sequence
            }

            // More than 4,096 ids inside one millisecond. Move to the next
            // millisecond rather than wrapping, which would break ordering.
            if (lastMillis.compareAndSet(previous, previous + 1)) {
                counter.set(0L)
                return previous + 1 to 0L
            }
        }
    }

    private fun format(bytes: ByteArray): String {
        val hex = StringBuilder(36)
        for (index in bytes.indices) {
            if (index == 4 || index == 6 || index == 8 || index == 10) hex.append('-')
            hex.append(HEX[(bytes[index].toInt() shr 4) and 0x0F])
            hex.append(HEX[bytes[index].toInt() and 0x0F])
        }
        return hex.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()

    /**
     * The id of this installation, used for `origin_device` on every row and as
     * the conflict tiebreaker when two versions carry identical timestamps.
     *
     * Random, local, and generated once. It is never transmitted anywhere,
     * except inside an export the person created, and it identifies a device
     * rather than a person: it survives no reinstall and links to nothing.
     */
    fun newDeviceId(): String = new()
}
