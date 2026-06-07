@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Flt64MaterialDemand
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Flt64MaterialReserves
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Material
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialDemand
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialReserves

/**
 * Additional FltX tests covering V-generic paths and Flt64 compat typealiases.
 */
class ProduceGenericFltXTest {

    // Simple test material implementation
    private class TestMaterial(override val index: Int, val label: String) : Material {
        override val material: Material get() = this
        override fun toString() = label
        override fun equals(other: Any?) = other is TestMaterial && index == other.index
        override fun hashCode() = index.hashCode()
    }

    @Suppress("unused")
    private val productA = TestMaterial(0, "productA")
    @Suppress("unused")
    private val productB = TestMaterial(1, "productB")

    // ---- MaterialDemand Flt64 compat typealias ----

    @Test
    fun flt64MaterialDemandTypealiasCompat() {
        val range = ValueRange(Flt64(0.0), Flt64(100.0), Interval.Closed, Interval.Closed, Flt64).value!!
        val demand = Flt64MaterialDemand(
            quantity = range,
            lessQuantity = Flt64(5.0),
            overQuantity = Flt64(3.0)
        )
        assertTrue(demand.quantity.lowerBound.value eq Flt64(0.0))
        assertTrue(demand.quantity.upperBound.value eq Flt64(100.0))
        assertTrue(demand.lessQuantity!! eq Flt64(5.0))
    }

    @Test
    fun flt64MaterialReservesTypealiasCompat() {
        val range = ValueRange(Flt64(10.0), Flt64(200.0), Interval.Closed, Interval.Closed, Flt64).value!!
        val reserves = Flt64MaterialReserves(quantity = range, overQuantity = Flt64(8.0))
        assertTrue(reserves.quantity.lowerBound.value eq Flt64(10.0))
        assertTrue(reserves.overQuantity!! eq Flt64(8.0))
    }

    // ---- MaterialDemand toFlt64 boundary ----

    @Test
    fun materialDemandFltXToFlt64Conversion() {
        val range = ValueRange(FltX("0"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val demand = MaterialDemand<FltX>(
            quantity = range,
            lessQuantity = FltX("5.0"),
            overQuantity = FltX("3.0")
        )
        val ubFlt64 = demand.quantity.upperBound.value.unwrap().toFlt64()
        assertTrue(ubFlt64 eq Flt64(100.0))
        val lessFlt64 = demand.lessQuantity!!.toFlt64()
        assertTrue(lessFlt64 eq Flt64(5.0))
    }

    // ---- MaterialReserves toFlt64 boundary ----

    @Test
    fun materialReservesFltXToFlt64Conversion() {
        val range = ValueRange(FltX("10"), FltX("500"), Interval.Closed, Interval.Closed, FltX).value!!
        val reserves = MaterialReserves<FltX>(
            quantity = range,
            overQuantity = FltX("15.0")
        )
        val lbFlt64 = reserves.quantity.lowerBound.value.unwrap().toFlt64()
        assertTrue(lbFlt64 eq Flt64(10.0))
        val overFlt64 = reserves.overQuantity!!.toFlt64()
        assertTrue(overFlt64 eq Flt64(15.0))
    }

    // ---- MaterialDemand with various FltX precision ----

    @Test
    fun materialDemandWithHighPrecisionFltX() {
        val range = ValueRange(
            FltX("0.000001"), FltX("999999.999999"),
            Interval.Closed, Interval.Closed, FltX
        ).value!!
        val demand = MaterialDemand<FltX>(
            quantity = range,
            lessQuantity = FltX("0.001"),
            overQuantity = FltX("0.001")
        )
        assertTrue(demand.quantity.lowerBound.value eq FltX("0.000001"))
        assertTrue(demand.quantity.upperBound.value eq FltX("999999.999999"))
    }
}
