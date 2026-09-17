/**
 * 变量类型（VariableType）单元测试。 / Unit tests for the variable type system.
 *
 * 变量类型决定变量取值域与分类标志，是建模正确性的基础，因此逐类型固定其契约。
 * Variable types define value ranges and classification flags; they underpin modeling
 * correctness, so each type's contract is pinned here.
 */
package fuookami.ospf.kotlin.core.variable

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*

class VariableTypeTest {

    @Test
    fun everyTypeShouldExposeStableNameAndShortName() {
        // 名称与缩写进入序列化与日志，必须稳定。
        // Names and short names feed serialization and logs, so they must stay stable.
        assertEquals("Binary", Binary.name)
        assertEquals("bin", Binary.shortName)
        assertEquals("Ternary", Ternary.name)
        assertEquals("ter", Ternary.shortName)
        assertEquals("BalancedTernary", BalancedTernary.name)
        assertEquals("bter", BalancedTernary.shortName)
        assertEquals("Percentage", Percentage.name)
        assertEquals("pct", Percentage.shortName)
        assertEquals("Integer", Integer.name)
        assertEquals("int", Integer.shortName)
        assertEquals("UInteger", UInteger.name)
        assertEquals("uint", UInteger.shortName)
        assertEquals("Continuous", Continuous.name)
        assertEquals("real", Continuous.shortName)
        assertEquals("UContinuous", UContinuous.name)
        assertEquals("ureal", UContinuous.shortName)
    }

    @Test
    fun binaryShouldBeTheOnlyBinaryType() {
        // 只有 Binary 被标记为二值；二值又必然是无符号整数。
        // Only Binary is flagged as binary, and a binary type is necessarily an unsigned integer.
        assertTrue(Binary.isBinaryType)
        assertFalse(Ternary.isBinaryType)
        assertFalse(BalancedTernary.isBinaryType)
        assertFalse(Integer.isBinaryType)
        assertFalse(Continuous.isBinaryType)

        assertTrue(Binary.isIntegerType)
        assertTrue(Binary.isUnsignedType)
        assertTrue(Binary.isUnsignedIntegerType)
        // 二值已由 isBinaryType 表达，故不算"非二值整数"。
        // Being binary is already expressed by isBinaryType, so it is not a non-binary integer.
        assertFalse(Binary.isNotBinaryIntegerType)
    }

    @Test
    fun unsignedIntegerTypesShouldBeClassifiedAsSuch() {
        // Ternary 与 UInteger 是无符号整数，但不是二值。
        // Ternary and UInteger are unsigned integers but not binary.
        assertTrue(Ternary.isIntegerType)
        assertTrue(Ternary.isUnsignedType)
        assertTrue(Ternary.isUnsignedIntegerType)
        assertTrue(Ternary.isNotBinaryIntegerType)

        assertTrue(UInteger.isIntegerType)
        assertTrue(UInteger.isUnsignedType)
        assertTrue(UInteger.isUnsignedIntegerType)
        assertTrue(UInteger.isNotBinaryIntegerType)
    }

    @Test
    fun signedIntegerTypesShouldNotBeClassifiedAsUnsigned() {
        // 有符号整数必须与无符号区分开，否则会误判取值域下界。
        // Signed integers must be distinguished from unsigned ones, otherwise the lower
        // bound of the value range is misjudged.
        for (type in listOf<VariableTypeInterface<*>>(BalancedTernary, Integer)) {
            assertTrue(type.isIntegerType, "${type.name} 应为整数类型")
            assertFalse(type.isUnsignedType, "${type.name} 不应为无符号类型")
            assertFalse(type.isUnsignedIntegerType, "${type.name} 不应为无符号整数类型")
            assertFalse(type.isContinuousType, "${type.name} 不应为连续类型")
        }
    }

    @Test
    fun continuousFamilyShouldBeClassifiedAsContinuous() {
        // 连续类型必须与整数类型互斥，且无符号连续只标记 unsigned。
        // Continuous types are mutually exclusive with integer types, and only the
        // unsigned continuous type carries the unsigned flag.
        for (type in listOf<VariableTypeInterface<*>>(Continuous, UContinuous, Percentage)) {
            assertFalse(type.isIntegerType, "${type.name} 不应为整数类型")
            assertTrue(type.isContinuousType, "${type.name} 应为连续类型")
            assertFalse(type.isUnsignedIntegerType)
        }

        assertFalse(Continuous.isUnsignedType)
        assertFalse(Continuous.isUnsignedContinuousType)
        assertTrue(UContinuous.isUnsignedType)
        assertTrue(UContinuous.isUnsignedContinuousType)
        assertTrue(Percentage.isUnsignedType)
        assertTrue(Percentage.isUnsignedContinuousType)
    }

    @Test
    fun integerTypesRangeShouldSpanTheWholeIntegerDomain() {
        // 有符号/无符号整数类型必须覆盖各自数值类型的完整域边界。
        // Signed and unsigned integer types must span the full bounds of their number type.
        assertEquals(Int64.minimum, Integer.minimum)
        assertEquals(Int64.maximum, Integer.maximum)

        assertEquals(UInt64.zero, UInteger.minimum)
        assertEquals(UInt64.maximum, UInteger.maximum)
    }

    @Test
    fun discreteTypesShouldHaveExactSmallRanges() {
        // 二值 [0,1]、三值 [0,2]、平衡三值 [-1,1]、百分比 [0,1] 必须精确。
        // Binary [0,1], ternary [0,2], balanced ternary [-1,1], percentage [0,1] are exact.
        assertEquals(UInt8.zero, Binary.minimum)
        assertEquals(UInt8.one, Binary.maximum)

        assertEquals(UInt8.zero, Ternary.minimum)
        assertEquals(UInt8.two, Ternary.maximum)

        assertEquals(-Int8.one, BalancedTernary.minimum)
        assertEquals(Int8.one, BalancedTernary.maximum)

        assertEquals(Flt64.zero, Percentage.minimum)
        assertEquals(Flt64.one, Percentage.maximum)
    }

    @Test
    fun continuousRangeShouldBeSymmetricAroundZero() {
        // 有符号连续域必须关于 0 对称，边界为十进制精度的倒数。
        // The signed continuous domain is symmetric about 0 with bounds at the reciprocal
        // of the decimal precision.
        val bound = Flt64.decimalPrecision.reciprocal()

        assertEquals(-bound, Continuous.minimum)
        assertEquals(bound, Continuous.maximum)
        assertTrue(Continuous.minimum < Flt64.zero, "下界必须为负")
        assertTrue(Continuous.maximum > Flt64.zero, "上界必须为正")
    }

    @Test
    fun unsignedContinuousRangeShouldStartAtZero() {
        // 无符号连续域下界必须是 0，不能沿用有符号域的负下界。
        // The unsigned continuous lower bound must be 0, not the signed domain's negative bound.
        assertEquals(Flt64.zero, UContinuous.minimum)
        assertEquals(Flt64.decimalPrecision.reciprocal(), UContinuous.maximum)
    }

    @Test
    fun integerTypesShouldExposeTheirConstantsCompanion() {
        // 每种类型必须暴露与其数值类型匹配的常量对象，供值域构造复用。
        // Each type exposes the constants object of its number type, reused by range construction.
        assertEquals(UInt8, Binary.constants)
        assertEquals(UInt8, Ternary.constants)
        assertEquals(Int8, BalancedTernary.constants)
        assertEquals(Int64, Integer.constants)
        assertEquals(UInt64, UInteger.constants)
        assertEquals(Flt64, Continuous.constants)
        assertEquals(Flt64, UContinuous.constants)
        assertEquals(Flt64, Percentage.constants)
    }

    @Test
    fun typeSingletonsShouldBeIdenticalAndMutuallyDistinct() {
        // 数据对象必须保持单例身份，且不同类型互不相等。
        // Data objects keep singleton identity, and distinct types are never equal.
        assertSame(Binary, Binary)
        assertSame(Continuous, Continuous)
        assertEquals(Binary, Binary)
        assertEquals(Continuous, Continuous)

        val types = listOf<Any>(
            Binary, Ternary, BalancedTernary, Percentage,
            Integer, UInteger, Continuous, UContinuous
        )
        assertEquals(types.size, types.toSet().size, "所有类型单例必须互不相同")
    }

    @Test
    fun toStringShouldMatchTheDeclaredNameForDiscreteTypes() {
        // 离散类型的 toString 与 name 一致。
        // For discrete types toString agrees with name.
        assertEquals("Binary", Binary.toString())
        assertEquals("Ternary", Ternary.toString())
        assertEquals("BalancedTernary", BalancedTernary.toString())
        assertEquals("Percentage", Percentage.toString())
        assertEquals("Integer", Integer.toString())
        assertEquals("UInteger", UInteger.toString())
    }

    @Test
    fun continuousToStringShouldMatchTheDeclaredName() {
        // 连续类型的 toString 必须与 name 一致。此前实现误拼为 "Continues"/"UContinues"，
        // 已修正；本用例防止拼写回退，并保证所有类型的 toString 与 name 统一。
        //
        // The continuous types' toString must agree with their `name`. The implementation
        // previously misspelled them as "Continues"/"UContinues", now corrected; this test
        // guards against regression and keeps toString consistent with name for every type.
        assertEquals("Continuous", Continuous.toString())
        assertEquals("UContinuous", UContinuous.toString())
        assertEquals(Continuous.name, Continuous.toString())
        assertEquals(UContinuous.name, UContinuous.toString())

        // 全类型统一：toString 必须等于 name，不得再出现任何拼写特例。
        // Uniform across all types: toString equals name with no spelling special case left.
        val allTypes = listOf(
            Binary, Ternary, BalancedTernary, Percentage,
            Integer, UInteger, Continuous, UContinuous
        )
        allTypes.forEach { type ->
            assertEquals(type.name, type.toString(), "${type.name} 的 toString 必须与 name 一致")
        }
    }

    @Test
    fun continuousTypeInterfacesShouldBeNamedConsistently() {
        // 类型层次接口名不得再出现 "Continues" 拼写。
        // The type-hierarchy interfaces must no longer carry the "Continues" misspelling.
        //
        // 反向断言（如 `Continuous !is UContinuousVariableType`）由编译器静态判定为恒假，
        // 无法写成运行期断言；有符号/无符号的区分改由下面的分类属性覆盖。
        //
        // Negative checks such as `Continuous !is UContinuousVariableType` are statically
        // constant-false and cannot be expressed at runtime; the signed/unsigned split is
        // therefore covered through the classification properties below instead.
        assertTrue(Continuous is ContinuousVariableType<*>)
        assertTrue(UContinuous is UContinuousVariableType<*>)
        assertTrue(Percentage is UContinuousVariableType<*>)

        assertFalse(Continuous.isUnsignedType)
        assertFalse(Continuous.isUnsignedContinuousType)
        assertTrue(UContinuous.isUnsignedType)
        assertTrue(UContinuous.isUnsignedContinuousType)
    }
}
