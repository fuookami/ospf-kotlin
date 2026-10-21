/**
 * 通道投影测试 / Channel projection tests
*/
package fuookami.ospf.kotlin.math.sequence

import kotlin.math.*
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.*

class ChannelProjectionTest {

    private fun <T> Ret<T>.valueOrFail(): T {
        return when (this) {
            is Ok -> value
            else -> fail("expected Ok but got $this")
        }
    }

    @Test
    fun scalarShouldAcceptSingleComponent() {
        assertEquals(Flt64(3.0), ChannelProjection.Scalar.project(Flt64(3.0)).valueOrFail())
        assertEquals(Flt64(3.0), ChannelProjection.Scalar.project(listOf(Flt64(3.0))).valueOrFail())
    }

    @Test
    fun scalarShouldRejectMultiComponent() {
        assertTrue(ChannelProjection.Scalar.project(listOf(Flt64.one, Flt64.two)) is Failed)
    }

    @Test
    fun componentShouldSelectInRangeIndex() {
        val components = listOf(Flt64(1.0), Flt64(2.0), Flt64(3.0))
        assertEquals(Flt64(2.0), ChannelProjection.Component(1).project(components).valueOrFail())
        assertTrue(ChannelProjection.Component(5).project(components) is Failed)
    }

    @Test
    fun magnitudeShouldReturnL2Norm() {
        assertEquals(
            Flt64(5.0),
            ChannelProjection.Magnitude.project(listOf(Flt64(3.0), Flt64(4.0))).valueOrFail()
        )
        assertEquals(
            Flt64(5.0),
            ChannelProjection.Magnitude.project(point2(Flt64(3.0), Flt64(4.0))).valueOrFail()
        )
    }

    @Test
    fun phaseShouldReturnPolarAngle() {
        val phase = ChannelProjection.Phase.project(listOf(Flt64.zero, Flt64.one)).valueOrFail().value
        assertEquals(PI / 2.0, phase, 1.0e-9)
    }

    @Test
    fun phaseShouldRequireAtLeastTwoComponents() {
        assertTrue(ChannelProjection.Phase.project(listOf(Flt64.one)) is Failed)
    }

    @Test
    fun emptyComponentsShouldBeRejected() {
        assertTrue(ChannelProjection.Magnitude.project(emptyList()) is Failed)
        assertTrue(ChannelProjection.Phase.project(emptyList()) is Failed)
    }

    @Test
    fun scalarShouldRejectMultiComponentProjectionFromScalarValue() {
        assertTrue(ChannelProjection.Component(0).project(Flt64.one) is Failed)
        assertTrue(ChannelProjection.Phase.project(Flt64.one) is Failed)
    }
}
