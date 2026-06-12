package fuookami.ospf.kotlin.utils.serialization

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.utils.meta_programming.NamingSystem

/**
 * JSON 命名策略单元测试
 *
 * Unit tests for JsonNamingPolicy functionality.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
class JsonNamingPolicyTest {

    @Serializable
    data class TestData(
        val playStation: String,
        val userName: String,
        val isActive: Boolean
    )

    /**
     * UT-JSON-01: 测试 JsonNamingPolicy(CamelCase -> SnakeCase) 序列化字段名转换正确
     *
     * Test that JsonNamingPolicy correctly converts field names from CamelCase to SnakeCase
     * during serialization.
     */
    @Test
    fun testCamelCaseToSnakeCaseSerialization() {
        val policy = JsonNamingPolicy(NamingSystem.CamelCase, NamingSystem.SnakeCase)
        val json = Json { namingStrategy = policy }

        val data = TestData(
            playStation = "PS5",
            userName = "testUser",
            isActive = true
        )

        val jsonString = json.encodeToString(data)

        // 验证字段名转换为 snake_case
        assertTrue(jsonString.contains("play_station"))
        assertTrue(jsonString.contains("user_name"))
        assertTrue(jsonString.contains("is_active"))

        // 验证原来的 camelCase 字段名不存在
        assertFalse(jsonString.contains("playStation"))
        assertFalse(jsonString.contains("userName"))
        assertFalse(jsonString.contains("isActive"))
    }

    /**
     * UT-JSON-02: 测试同策略反序列化可正确映射回 Kotlin 属性
     *
     * Test that the same policy can correctly deserialize JSON back to Kotlin properties.
     */
    @Test
    fun testSnakeCaseToCamelCaseDeserialization() {
        val policy = JsonNamingPolicy(NamingSystem.CamelCase, NamingSystem.SnakeCase)
        val json = Json { namingStrategy = policy }

        // 使用 snake_case 格式的 JSON
        val jsonString = """
            {
                "play_station": "PS5",
                "user_name": "testUser",
                "is_active": true
            }
        """.trimIndent()

        val data = json.decodeFromString<TestData>(jsonString)

        assertEquals("PS5", data.playStation)
        assertEquals("testUser", data.userName)
        assertEquals(true, data.isActive)
    }

    /**
     * 测试序列化-反序列化往返一致性
     *
     * Test serialization-deserialization round-trip consistency.
     */
    @Test
    fun testRoundTripConsistency() {
        val policy = JsonNamingPolicy(NamingSystem.CamelCase, NamingSystem.SnakeCase)
        val json = Json { namingStrategy = policy }

        val original = TestData(
            playStation = "PS5",
            userName = "testUser",
            isActive = true
        )

        // 序列化
        val jsonString = json.encodeToString(original)

        // 反序列化
        val decoded = json.decodeFromString<TestData>(jsonString)

        assertEquals(original, decoded)
    }

    /**
     * 测试 PascalCase 到 SnakeCase 的转换
     *
     * Test PascalCase to SnakeCase conversion.
     */
    @Test
    fun testPascalCaseToSnakeCase() {
        @Serializable
        data class PascalData(
            val PlayStation: String,
            val UserName: String
        )

        val policy = JsonNamingPolicy(NamingSystem.PascalCase, NamingSystem.SnakeCase)
        val json = Json { namingStrategy = policy }

        val data = PascalData(PlayStation = "PS5", UserName = "test")
        val jsonString = json.encodeToString(data)

        assertTrue(jsonString.contains("play_station"))
        assertTrue(jsonString.contains("user_name"))
    }

    /**
     * 测试 CamelCase 到 KebabCase 的转换
     *
     * Test CamelCase to KebabCase conversion.
     */
    @Test
    fun testCamelCaseToKebabCase() {
        val policy = JsonNamingPolicy(NamingSystem.CamelCase, NamingSystem.KebabCase)
        val json = Json { namingStrategy = policy }

        val data = TestData(
            playStation = "PS5",
            userName = "testUser",
            isActive = true
        )

        val jsonString = json.encodeToString(data)

        assertTrue(jsonString.contains("play-station"))
        assertTrue(jsonString.contains("user-name"))
        assertTrue(jsonString.contains("is-active"))
    }
}
