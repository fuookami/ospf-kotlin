/**
 * 变量范围（Range）单元测试。 / Unit tests for the variable range.
 *
 * Range 由变量类型派生初始值域，并通过 intersectWith 家族**原地收窄**。
 * 原地变更与"空值域不可恢复"是这里最容易误用的语义，因此逐条固定。
 *
 * A Range derives its initial value range from the variable type and **narrows in place**
 * through the intersectWith family. In-place mutation and the non-recoverable empty range
 * are the easiest semantics to misuse, so they are pinned here case by case.
 */
package fuookami.ospf.kotlin.core.variable

import kotlin.test.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

class VariableRangeTest {

    /** 构造二值变量范围 / Build a binary variable range. */
    private fun binaryRange() = Range(Binary, UInt8)

    /** 构造三值变量范围 / Build a ternary variable range. */
    private fun ternaryRange() = Range(Ternary, UInt8)

    /** 构造 [lb, ∞) 形式的值范围，用作求交输入。/ Build a `[lb, ∞)` value range used as an intersection input. */
    private fun geqRange(lb: UInt8) =
        ValueRange(lb, Infinity, Interval.Closed, UInt8).value!!

    /** 构造 (-∞, ub] 形式的值范围 / Build a `(-∞, ub]` value range. */
    private fun leqRange(ub: UInt8) =
        ValueRange(NegativeInfinity, ub, Interval.Closed, UInt8).value!!

    @Test
    fun rangeShouldDeriveBoundsFromTheVariableType() {
        // Range 必须直接采用变量类型的最小/最大值，不得自行收窄。
        // A Range adopts the variable type's minimum/maximum verbatim.
        val range = binaryRange()
        assertEquals(Binary.minimum, range.lowerBound!!.value.unwrap())
        assertEquals(Binary.maximum, range.upperBound!!.value.unwrap())
        assertFalse(range.empty)
        assertEquals(Flt64.zero, range.lowerBound!!.toFlt64().value.unwrap())
        assertEquals(Flt64.one, range.upperBound!!.toFlt64().value.unwrap())
    }

    @Test
    fun boundsShouldBeClosedForFiniteTypes() {
        // 有限类型的两端必须是闭区间，否则端点值会被错误排除。
        // Bounds of a finite type are closed; otherwise the endpoint values are wrongly excluded.
        val range = binaryRange()
        assertEquals(Interval.Closed, range.lowerBound!!.interval)
        assertEquals(Interval.Closed, range.upperBound!!.interval)
    }

    @Test
    fun signedAndUnsignedRangesShouldDifferInTheirLowerBound() {
        // 有符号连续域下界为负、无符号为 0，二者不得混同。
        // The signed continuous lower bound is negative while the unsigned one is zero.
        val signed = Range(Continuous, Flt64)
        val unsigned = Range(UContinuous, Flt64)

        assertTrue(signed.lowerBound!!.value.unwrap() < Flt64.zero)
        assertEquals(Flt64.zero, unsigned.lowerBound!!.value.unwrap())
        assertEquals(
            signed.upperBound!!.value.unwrap(),
            unsigned.upperBound!!.value.unwrap()
        )
    }

    @Test
    fun equalityShouldCompareTypeAndConstantsOnly() {
        // 数据类相等性只比较 type 与 constants，**不比较当前值域状态**。
        // 因此收窄后的 Range 仍等于同类型的初始 Range —— 这是一个容易被误认为 bug
        // 的既有语义，固定下来以免被当作相等性判据使用。
        //
        // Data-class equality compares only type and constants, **not** the current range
        // state. A narrowed Range therefore still equals a fresh one of the same type.
        // Pinned so nobody relies on equality as a range comparison.
        val left = binaryRange()
        val right = binaryRange()
        assertEquals(left, right)

        assertTrue(left.geq(UInt8.one), "收窄应成功")
        assertEquals(UInt8.one, left.lowerBound!!.value.unwrap(), "左值域已收窄")
        assertEquals(left, right, "相等性不反映值域差异")
    }

    @Test
    fun rangesOfDifferentTypesShouldNotBeEqual() {
        // 不同类型的 Range 必须不相等 / Ranges of different types must never be equal.
        assertNotEquals(binaryRange() as Any, ternaryRange() as Any)
    }

    @Test
    fun copyShouldResetToTheFullTypeRange() {
        // copy() 依据 type/constants 重新构造，因此丢弃已收窄的状态并发回完整值域。
        // copy() reconstructs from type/constants, so it discards narrowing and restores
        // the full type range.
        val range = binaryRange()
        assertTrue(range.geq(UInt8.one))
        assertEquals(UInt8.one, range.lowerBound!!.value.unwrap())

        val copied = range.copy()
        assertEquals(UInt8.zero, copied.lowerBound!!.value.unwrap(), "副本应回到完整值域")
        assertEquals(UInt8.one, copied.upperBound!!.value.unwrap())
    }

    @Test
    fun setTrueAndSetFalseShouldNarrowToASingleValue() {
        // setTrue/setFalse 必须把二值域收窄到单点，并标记 fixed。
        // setTrue/setFalse narrow the binary range to a single point and mark it fixed.
        val trued = binaryRange()
        assertTrue(trued.setTrue())
        assertTrue(trued.fixed)
        assertEquals(UInt8.one, trued.lowerBound!!.value.unwrap())
        assertEquals(UInt8.one, trued.upperBound!!.value.unwrap())

        val falsed = binaryRange()
        assertTrue(falsed.setFalse())
        assertTrue(falsed.fixed)
        assertEquals(UInt8.zero, falsed.lowerBound!!.value.unwrap())
        assertEquals(UInt8.zero, falsed.upperBound!!.value.unwrap())
    }

    @Test
    fun conflictingBinaryConstraintsShouldEmptyTheRange() {
        // 先固定为真、再要求为假必须使值域变空（约束不可行），而不是静默保留其一。
        // Fixing true and then requiring false must empty the range rather than silently
        // keeping one of them.
        val range = binaryRange()
        assertTrue(range.setTrue())

        assertFalse(range.setFalse(), "真与假不可同时满足")
        assertTrue(range.empty, "冲突后值域必须为空")
    }

    @Test
    fun outOfDomainConstraintShouldEmptyTheRange() {
        // 对二值域施加 >= 2 的约束必须失败并清空值域。
        // Applying >= 2 to a binary range must fail and empty it.
        val range = binaryRange()
        assertFalse(range.geq(UInt8.two))
        assertTrue(range.empty)
    }

    @Test
    fun booleanOverloadsShouldMatchTheirNumericCounterparts() {
        // Boolean 重载只是 UInt8 重载的语法糖，结果必须一致。
        // The Boolean overloads are sugar over the UInt8 ones and must agree.
        val viaBoolean = binaryRange()
        assertTrue(viaBoolean.setTrue())
        val viaNumeric = binaryRange()
        assertTrue(viaNumeric.geq(UInt8.one))
        assertEquals(
            viaNumeric.lowerBound!!.value.unwrap(),
            viaBoolean.lowerBound!!.value.unwrap()
        )
        assertEquals(
            viaNumeric.upperBound!!.value.unwrap(),
            viaBoolean.upperBound!!.value.unwrap()
        )

        assertEquals(binaryRange().setFalse(), binaryRange().leq(false))
        assertEquals(binaryRange().geq(true), binaryRange().geq(UInt8.one))
        assertEquals(binaryRange().ls(true), binaryRange().ls(UInt8.one))
    }

    @Test
    fun ternaryShouldNarrowToItsThreeStates() {
        // 三值的三种状态必须各自映射到 0/1/2 的单点域。
        // Each ternary state maps to a single point among 0/1/2.
        val falseRange = ternaryRange()
        assertTrue(falseRange.setFalse())
        assertEquals(UInt8.zero, falseRange.lowerBound!!.value.unwrap())
        assertEquals(UInt8.zero, falseRange.upperBound!!.value.unwrap())

        val unknownRange = ternaryRange()
        assertTrue(unknownRange.setUnknown())
        assertEquals(UInt8.one, unknownRange.lowerBound!!.value.unwrap())
        assertEquals(UInt8.one, unknownRange.upperBound!!.value.unwrap())

        val trueRange = ternaryRange()
        assertTrue(trueRange.setTrue())
        assertEquals(UInt8.two, trueRange.lowerBound!!.value.unwrap())
        assertEquals(UInt8.two, trueRange.upperBound!!.value.unwrap())
    }

    @Test
    fun ternaryShouldRejectOutOfDomainStates() {
        // 三值域不得接受 3 之类的越界取值。
        // A ternary range must reject out-of-domain values such as 3.
        val range = ternaryRange()
        assertFalse(range.geq(UInt8(3U)))
        assertTrue(range.empty)
    }

    @Test
    fun balancedTernaryShouldNarrowToMinusOneZeroAndOne() {
        // 平衡三值域为 [-1, 1]，三种状态各自收窄到单点。
        // The balanced ternary range is [-1, 1]; each state narrows to a single point.
        val trueRange = Range(BalancedTernary, Int8)
        assertTrue(trueRange.setTrue())
        assertEquals(Int8.one, trueRange.lowerBound!!.value.unwrap())
        assertEquals(Int8.one, trueRange.upperBound!!.value.unwrap())

        val unknownRange = Range(BalancedTernary, Int8)
        assertTrue(unknownRange.setUnknown())
        assertEquals(Int8.zero, unknownRange.lowerBound!!.value.unwrap())
        assertEquals(Int8.zero, unknownRange.upperBound!!.value.unwrap())

        val falseRange = Range(BalancedTernary, Int8)
        assertTrue(falseRange.setFalse())
        assertEquals(-Int8.one, falseRange.lowerBound!!.value.unwrap())
        assertEquals(-Int8.one, falseRange.upperBound!!.value.unwrap())
    }

    @Test
    fun repeatedNarrowingShouldBeMonotonic() {
        // 连续收窄必须是单调的：一旦限定 >= 1，再要求 >= 0 不应放宽回 0。
        // Repeated narrowing is monotonic: once >= 1 is applied, a later >= 0 must not
        // widen the lower bound back to 0.
        val range = ternaryRange()
        assertTrue(range.geq(UInt8.one))
        assertEquals(UInt8.one, range.lowerBound!!.value.unwrap())

        assertTrue(range.geq(UInt8.zero), "更弱的约束仍然可行")
        assertEquals(UInt8.one, range.lowerBound!!.value.unwrap(), "下界不得被放宽")
    }

    @Test
    fun narrowingShouldPreserveAnAlreadyEmptyRange() {
        // 空值域是不可恢复的：后续任何约束都不应把它"救活"。
        // An empty range is not recoverable: no later constraint may revive it.
        val range = binaryRange()
        assertFalse(range.geq(UInt8.two))
        assertTrue(range.empty)

        assertFalse(range.leq(UInt8.one), "空值域与任何约束求交都应为空")
        assertTrue(range.empty)
        assertFalse(range.setTrue())
        assertTrue(range.empty)
    }

    @Test
    fun intersectWithShouldReportWhetherTheResultIsNonEmpty() {
        // 单参数 intersectWith 返回交集是否非空，并在空交集时把值域置空。
        // The one-argument intersectWith reports whether the intersection is non-empty and
        // empties the range on an empty intersection.
        val overlapping = binaryRange()
        assertTrue(overlapping.intersectWith(geqRange(UInt8.one)))
        assertFalse(overlapping.empty)

        val disjoint = binaryRange()
        assertFalse(disjoint.intersectWith(geqRange(UInt8(5U))))
        assertTrue(disjoint.empty)
    }

    @Test
    fun leqIntersectionShouldCapTheUpperBound() {
        // (-∞, 0] 与二值域 [0,1] 求交必须得到单点 0。
        // Intersecting `(-∞, 0]` with the binary range `[0,1]` must yield the single point 0.
        val range = binaryRange()
        assertTrue(range.intersectWith(leqRange(UInt8.zero)))
        assertEquals(UInt8.zero, range.lowerBound!!.value.unwrap())
        assertEquals(UInt8.zero, range.upperBound!!.value.unwrap())
        assertTrue(range.fixed)
    }

    @Test
    fun invertedBoundsShouldThrowInsteadOfReportingInfeasibility() {
        // 现状记录：两参数 intersectWith(lb, ub) 在 lb > ub 时经由 `value!!` 抛
        // NullPointerException，而不是像单参数版本那样返回 false。二者行为不一致，
        // 调用方不能依赖"返回 false"来判断不可行。
        //
        // Characterizes current behavior: the two-argument intersectWith(lb, ub) throws a
        // NullPointerException via `value!!` when lb > ub, instead of returning false the
        // way the one-argument overload does. Callers must not rely on a `false` return to
        // detect infeasibility here.
        val range = binaryRange()
        assertFailsWith<NullPointerException> {
            range.intersectWith(UInt8.two, UInt8.one)
        }
    }

    @Test
    fun twoArgumentIntersectionShouldNarrowWithinBounds() {
        // 合法的两参数求交必须正常收窄 / A valid two-argument intersection narrows normally.
        val range = ternaryRange()
        assertTrue(range.intersectWith(UInt8.one, UInt8.two))
        assertEquals(UInt8.one, range.lowerBound!!.value.unwrap())
        assertEquals(UInt8.two, range.upperBound!!.value.unwrap())
    }

    @Test
    fun toStringShouldNotThrowForAnyRangeState() {
        // toString 在完整域、收窄域与空域下都必须可渲染（用于日志与断言消息）。
        // toString must render for a full, narrowed, and empty range alike.
        assertTrue(binaryRange().toString().isNotEmpty())

        val narrowed = ternaryRange()
        assertTrue(narrowed.geq(UInt8.one))
        assertTrue(narrowed.toString().isNotEmpty())

        val emptied = binaryRange()
        assertFalse(emptied.geq(UInt8.two))
        assertTrue(emptied.toString().isNotEmpty())
    }
}
