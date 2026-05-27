package fuookami.ospf.kotlin.core.symbol

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.math.algebra.concept.RealNumberConstants
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.Bound
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.algebra.value_range.ValueWrapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * ExpressionRange<V> 求解器边界安全回归测试。
 * Regression tests for ExpressionRange<V> solver-boundary safety.
 *
 * SolverBoundaryCasts.expressionRangeFromFlt64() 会把 ValueRange<Flt64> 转为
 * ValueRange<V>，并把 Flt64 常量转为 V。运行时 V=Flt64 时该转换安全；若 V!=Flt64
 * 且用户读取 range/lowerBound/upperBound，则存在运行时类型污染风险。本测试固定当前行为，
 * 并记录求解器边界技术债。
 *
 * SolverBoundaryCasts.expressionRangeFromFlt64() casts ValueRange<Flt64> to
 * ValueRange<V> and Flt64 constants to V. At runtime V=Flt64 so the cast is
 * safe, but if V!=Flt64 users read range/lowerBound/upperBound, there is a
 * runtime type pollution risk. These tests lock down the current behavior and
 * document the solver-boundary tech debt.
 */
class ExpressionRangeSolverBoundaryTest {

    private fun Bound<Flt64>.flt64Value(): Flt64 = (this.value as ValueWrapper.Value).value

    @Test
    fun `ExpressionRange Flt64 round-trip via invoke with constants`() {
        val range: ExpressionRange<Flt64> = ExpressionRange.invoke()
        assertNotNull(range.range)
        assertEquals(Flt64.minimum, range.lowerBound!!.flt64Value())
        assertEquals(Flt64.maximum, range.upperBound!!.flt64Value())
    }

    @Test
    fun `ExpressionRange Flt64 with explicit ValueRange`() {
        val vr = ValueRange(
            Flt64(1.0),
            Flt64(10.0),
            Interval.Closed,
            Interval.Closed,
            Flt64 as RealNumberConstants<Flt64>
        ).value!!
        val range: ExpressionRange<Flt64> = ExpressionRange.invoke(vr)
        assertNotNull(range.range)
        assertEquals(Flt64(1.0), range.lowerBound!!.flt64Value())
        assertEquals(Flt64(10.0), range.upperBound!!.flt64Value())
    }

    @Test
    fun `ExpressionRange intersectWith narrows bounds`() {
        val range: ExpressionRange<Flt64> = ExpressionRange.invoke()
        val narrow = ValueRange(
            Flt64(5.0),
            Flt64(15.0),
            Interval.Closed,
            Interval.Closed,
            Flt64 as RealNumberConstants<Flt64>
        ).value!!
        assertTrue(range.intersectWith(narrow))
        assertEquals(Flt64(5.0), range.lowerBound!!.flt64Value())
        assertEquals(Flt64(15.0), range.upperBound!!.flt64Value())
    }

    @Test
    fun `SolverBoundaryCasts expressionRangeFromFlt64 preserves Flt64 range`() {
        val vr = ValueRange(
            Flt64(2.0),
            Flt64(20.0),
            Interval.Closed,
            Interval.Closed,
            Flt64 as RealNumberConstants<Flt64>
        ).value!!
        val range: ExpressionRange<Flt64> = SolverBoundaryCasts.expressionRangeFromFlt64(vr)
        assertNotNull(range.range)
        assertEquals(Flt64(2.0), range.lowerBound!!.flt64Value())
        assertEquals(Flt64(20.0), range.upperBound!!.flt64Value())
    }

    @Test
    fun `SolverBoundaryCasts fullExpressionRange gives full Flt64 range`() {
        val range: ExpressionRange<Flt64> = SolverBoundaryCasts.fullExpressionRange()
        assertNotNull(range.range)
        assertEquals(Flt64.minimum, range.lowerBound!!.flt64Value())
        assertEquals(Flt64.maximum, range.upperBound!!.flt64Value())
    }

    @Test
    fun `SolverBoundaryCasts expressionRangeFromFlt64 null returns empty range`() {
        val range: ExpressionRange<Flt64> = SolverBoundaryCasts.expressionRangeFromFlt64(null)
        assertNull(range.range)
        assertTrue(range.empty)
    }
}
