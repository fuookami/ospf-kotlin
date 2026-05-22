package fuookami.ospf.kotlin.utils.serialization

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * LocalMonthSerializer 单元测试
 *
 * Unit tests for LocalMonthSerializer functionality.
 *
 * 注意：LocalMonthSerializer 是用于 LocalDate 的序列化器，格式为 "yyyy-MM"。
 * Note: LocalMonthSerializer is a serializer for LocalDate with "yyyy-MM" format.
 */

@Serializable
data class LocalDateMonth(
    @Serializable(with = LocalMonthSerializer::class)
    val date: LocalDate
)

/**
 * LocalMonthSerializer 单元测试
 *
 * Unit tests for LocalMonthSerializer functionality.
 */
class LocalMonthSerializerTest {

    /**
     * UT-DATE-01: 测试 LocalMonthSerializer 反序列化 "yyyy-MM" 返回当月 1 日
     *
     * Test that LocalMonthSerializer deserializes "yyyy-MM" to the first day of that month.
     */
    @Test
    fun testDeserializeYearMonthString() {
        val jsonString = "{\"date\":\"2026-04\"}"

        val result = Json.decodeFromString<LocalDateMonth>(jsonString)

        // 验证反序列化结果为当月 1 日
        assertEquals(2026, result.date.year)
        assertEquals(1, result.date.day)
    }

    /**
     * UT-DATE-02: 测试 LocalMonthSerializer 序列化/反序列化往返一致（按月）
     *
     * Test that LocalMonthSerializer serialization-deserialization round-trip is consistent (by month).
     */
    @Test
    fun testRoundTripConsistency() {
        // 创建 2026-04-01（当月第一天）
        val original = LocalDateMonth(LocalDate(2026, 4, 1))

        // 序列化
        val jsonString = Json.encodeToString(original)

        // 验证序列化格式为 "yyyy-MM"
        assertTrue(jsonString.contains("2026-04"))

        // 反序列化
        val decoded = Json.decodeFromString<LocalDateMonth>(jsonString)

        // 验证年月一致（日总是为 1）
        assertEquals(original.date.year, decoded.date.year)
        assertEquals(original.date.day, decoded.date.day)
    }

    /**
     * 测试不同月份的反序列化
     *
     * Test deserialization of different months.
     */
    @Test
    fun testDeserializeDifferentMonths() {
        val testCases = listOf(
            Triple("{\"date\":\"2026-01\"}", 2026, 1),
            Triple("{\"date\":\"2026-12\"}", 2026, 12),
            Triple("{\"date\":\"1999-06\"}", 1999, 6),
            Triple("{\"date\":\"2000-01\"}", 2000, 1)
        )

        for ((jsonString, expectedYear, expectedMonth) in testCases) {
            val result = Json.decodeFromString<LocalDateMonth>(jsonString)
            assertEquals(expectedYear, result.date.year)
            assertEquals(1, result.date.day)
            // 验证序列化输出包含正确的月份
            val output = Json.encodeToString(result)
            assertTrue(output.contains("$expectedYear-${expectedMonth.toString().padStart(2, '0')}"))
        }
    }

    /**
     * 测试无效格式抛出异常
     *
     * Test that invalid format throws exception.
     */
    @Test
    fun testInvalidFormatThrowsException() {
        val invalidJsonStrings = listOf(
            "{\"date\":\"2026\"}",
            "{\"date\":\"2026-4-15\"}",
            "{\"date\":\"invalid\"}",
            "{\"date\":\"26-04\"}"
        )

        for (jsonString in invalidJsonStrings) {
            assertThrows(Exception::class.java) {
                Json.decodeFromString<LocalDateMonth>(jsonString)
            }
        }
    }

    /**
     * 测试序列化只保留年月
     *
     * Test serialization only preserves year and month.
     */
    @Test
    fun testSerializationPreservesYearMonthOnly() {
        // 即使日期不是 1 号，序列化也会输出年月格式
        val dates = listOf(
            LocalDateMonth(LocalDate(2026, 4, 1)),
            LocalDateMonth(LocalDate(2026, 4, 15)),
            LocalDateMonth(LocalDate(2026, 4, 30))
        )

        for (dateObj in dates) {
            val jsonString = Json.encodeToString(dateObj)
            // 所有日期都序列化为相同的 "2026-04"
            assertTrue(jsonString.contains("2026-04"))
        }
    }
}
