/**
 * UUIDv7 generator providing time-ordered unique identifiers.
 * 提供时间排序的唯一标识符的 UUIDv7 生成器。
*/
package fuookami.ospf.kotlin.utils

import java.time.Instant
import java.security.SecureRandom

/**
 * A singleton object for generating UUIDv7 identifiers.
 * 用于生成 UUIDv7 标识符的单例对象。
 *
 * UUIDv7 is a time-ordered UUID format that combines a Unix timestamp with random bits,
 * providing sortable unique identifiers suitable for distributed systems.
 * UUIDv7 是一种时间排序的 UUID 格式，将 Unix 时间戳与随机位组合，提供适合分布式系统的可排序唯一标识符。
*/
data object UUIDv7 {
    private val random = SecureRandom()

    /**
     * Generates a new UUIDv7 as a 16-byte array.
     * 生成一个新的 UUIDv7 作为 16 字节数组。
     *
     * The UUID structure:
     * - Bytes 0-5: 48-bit Unix timestamp in milliseconds
     * - Bytes 6-7: Version (4-bit) + Random data (12-bit)
     * - Bytes 8-15: Variant (2-bit) + Random data (62-bit)
     * UUID 结构：字节 0-5：48 位 Unix 时间戳（毫秒）；字节 6-7：版本（4 位）+ 随机数据（12 位）；字节 8-15：变体（2 位）+ 随机数据（62 位）
     *
     * @return a 16-byte array representing the UUIDv7 / 16 字节的 UUIDv7 字节数组
    */
    fun generate(): ByteArray {
        // random bytes / 随机字节
        val value = ByteArray(16)
        random.nextBytes(value)

        // current timestamp in ms / 当前时间戳（毫秒）
        val timestamp = Instant.now().toEpochMilli()

        // timestamp / 写入时间戳
        value[0] = ((timestamp shr 40) and 0xFF).toByte()
        value[1] = ((timestamp shr 32) and 0xFF).toByte()
        value[2] = ((timestamp shr 24) and 0xFF).toByte()
        value[3] = ((timestamp shr 16) and 0xFF).toByte()
        value[4] = ((timestamp shr 8) and 0xFF).toByte()
        value[5] = (timestamp and 0xFF).toByte()

        // version and variant / 设置版本号和变体位
        value[6] = (value[6].toInt() and 0x0F or 0x70).toByte()
        value[8] = (value[8].toInt() and 0x3F or 0x80).toByte()

        return value
    }

    /**
     * Synchronously generates a new UUIDv7 as a 16-byte array.
     * 同步生成一个新的 UUIDv7 作为 16 字节数组。此方法是线程安全的，可在并发环境中使用。
     *
     * @return a 16-byte array representing the UUIDv7 / 16 字节的 UUIDv7 字节数组
    */
    @Synchronized
    fun generateSync(): ByteArray {
        return generate()
    }
}
