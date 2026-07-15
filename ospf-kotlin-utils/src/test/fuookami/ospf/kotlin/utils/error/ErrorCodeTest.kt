package fuookami.ospf.kotlin.utils.error

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * ErrorCode 单元测试
 *
 * Unit tests for ErrorCode functionality.
 */
class ErrorCodeTest {

    /**
     * UT-ERR-01: 测试 ErrorCode.from(unknown) 返回 Unknown 而非抛异常
     *
     * Test that ErrorCode.from(unknown) returns Unknown instead of throwing exception.
     */
    @Test
    fun testFromUnknownCodeReturnsUnknown() {
        // 测试未知的 UByte code
        val unknownUByte: UByte = 0xEEU  // 未定义的错误码
        val result = ErrorCode.from(unknownUByte)
        assertEquals(ErrorCode.Unknown, result)

        // 测试未知的 ULong code
        val unknownULong: ULong = 0xEEUL
        val resultULong = ErrorCode.from(unknownULong)
        assertEquals(ErrorCode.Unknown, resultULong)
    }

    /**
     * 测试已知错误码的 from 方法
     *
     * Test from method with known error codes.
     */
    @Test
    fun testFromKnownCode() {
        // 使用显式类型消除歧义
        assertEquals(ErrorCode.None, ErrorCode.from(0x00U.toUByte()))
        assertEquals(ErrorCode.AuthenticationError, ErrorCode.from(0x01U.toUByte()))
        assertEquals(ErrorCode.NotAFile, ErrorCode.from(0x10U.toUByte()))
        assertEquals(ErrorCode.FileNotFound, ErrorCode.from(0x12U.toUByte()))
        assertEquals(ErrorCode.ORModelInfeasible, ErrorCode.from(0x29U.toUByte()))
        assertEquals(ErrorCode.ORModelUnbounded, ErrorCode.from(0x2aU.toUByte()))
        assertEquals(ErrorCode.ORModelInfeasibleOrUnbounded, ErrorCode.from(0x2bU.toUByte()))
    }

    /**
     * UT-ERR-02: 测试错误码值唯一性校验（防重复）
     *
     * Test error code value uniqueness (prevent duplicates).
     */
    @Test
    fun testErrorCodeUniqueness() {
        val allCodes = ErrorCode.entries.map { it.toUByte() }
        val uniqueCodes = allCodes.distinct()

        // 验证所有错误码值唯一
        assertEquals(allCodes.size, uniqueCodes.size, "Error codes must be unique")

        // 验证修复后的 ORModelInfeasibleOrUnbounded 错误码不再重复
        val infeasibleOrUnboundedCode = ErrorCode.ORModelInfeasibleOrUnbounded.toUByte()
        val unboundedCode = ErrorCode.ORModelUnbounded.toUByte()
        assertNotEquals(infeasibleOrUnboundedCode, unboundedCode,
            "ORModelInfeasibleOrUnbounded (0x2bU) must not duplicate ORModelUnbounded (0x2aU)")
    }

    /**
     * 测试 toUByte 转换
     *
     * Test toUByte conversion.
     */
    @Test
    fun testToUByte() {
        assertEquals(0x01U.toUByte(), ErrorCode.AuthenticationError.toUByte())
    }

    /**
     * 测试 toULong 转换
     *
     * Test toULong conversion.
     */
    @Test
    fun testToULong() {
        assertEquals(0x01UL, ErrorCode.AuthenticationError.toULong())
    }

    /**
     * 测试 toString 返回枚举名称
     *
     * Test toString returns enum name.
     */
    @Test
    fun testToString() {
        assertEquals("None", ErrorCode.None.toString())
        assertEquals("AuthenticationError", ErrorCode.AuthenticationError.toString())
        assertEquals("Unknown", ErrorCode.Unknown.toString())
    }

    /**
     * 测试边界值
     *
     * Test boundary values.
     */
    @Test
    fun testBoundaryValues() {
        // 最小值
        assertEquals(ErrorCode.None, ErrorCode.from(0x00U.toUByte()))

        // 最大值（Unknown）
        assertEquals(ErrorCode.Unknown, ErrorCode.from(0xffU.toUByte()))

        // Other
        assertEquals(ErrorCode.Other, ErrorCode.from(0xfeU.toUByte()))
    }

    /**
     * 测试 from ULong 重载
     *
     * Test from ULong overload.
     */
    @Test
    fun testFromULong() {
        assertEquals(ErrorCode.None, ErrorCode.from(0x00UL))
        assertEquals(ErrorCode.Unknown, ErrorCode.from(0xffUL))
        assertEquals(ErrorCode.Other, ErrorCode.from(0xfeUL))
    }
}
