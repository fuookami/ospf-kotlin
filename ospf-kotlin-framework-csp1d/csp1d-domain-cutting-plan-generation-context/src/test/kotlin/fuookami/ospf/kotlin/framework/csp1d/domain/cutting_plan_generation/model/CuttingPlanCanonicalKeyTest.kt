package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Flt64QuantityArithmetic
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.RollCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange

class CuttingPlanCanonicalKeyTest {

    private fun product(id: String, width: Double): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = listOf(Quantity(Flt64(width), Meter))
        )
    }

    private fun material(id: String = "m"): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64(0.5), Meter),
                    upperBound = Quantity(Flt64(2.0), Meter)
                ),
                step = Quantity(Flt64(0.1), Meter)
            ),
            length = Quantity(Flt64(100.0), Meter)
        )
    }

    @Test
    fun canonicalKeyIgnoresSliceOrderAndMergesDuplicateSlices() {
        val p1 = product(id = "p1", width = 0.5)
        val p2 = product(id = "p2", width = 0.8)
        val m = material()
        val demand1 = ProductDemand.roll(p1, Quantity(Flt64(2.0), RollCountUnit))
        val demand2 = ProductDemand.roll(p2, Quantity(Flt64(1.0), RollCountUnit))

        val first = CuttingPlan(
            id = "first",
            material = m,
            slices = listOf(
                CuttingPlanSlice(
                    production = p1,
                    width = p1.width.first(),
                    amount = UInt64.one
                ),
                CuttingPlanSlice(
                    production = p2,
                    width = p2.width.first(),
                    amount = UInt64.one
                ),
                CuttingPlanSlice(
                    production = p1,
                    width = p1.width.first(),
                    amount = UInt64.one
                )
            ),
            demandContributions = listOf(
                CuttingPlanDemandContribution(
                    product = p1,
                    quantity = demand1.quantity
                ),
                CuttingPlanDemandContribution(
                    product = p2,
                    quantity = demand2.quantity
                )
            ),
            arithmetic = Flt64QuantityArithmetic
        )
        val second = CuttingPlan(
            id = "second",
            material = m,
            slices = listOf(
                CuttingPlanSlice(
                    production = p2,
                    width = p2.width.first(),
                    amount = UInt64.one
                ),
                CuttingPlanSlice(
                    production = p1,
                    width = p1.width.first(),
                    amount = UInt64(2UL)
                )
            ),
            demandContributions = listOf(
                CuttingPlanDemandContribution(
                    product = p2,
                    quantity = demand2.quantity
                ),
                CuttingPlanDemandContribution(
                    product = p1,
                    quantity = demand1.quantity
                )
            ),
            arithmetic = Flt64QuantityArithmetic
        )

        assertEquals(
            expected = first.canonicalKey(),
            actual = second.canonicalKey()
        )
    }

    @Test
    fun canonicalKeyKeepsMaterialIdentity() {
        val p = product(id = "p", width = 0.5)
        val demand = ProductDemand.roll(p, Quantity(Flt64(1.0), RollCountUnit))
        val first = singlePlan(
            id = "first",
            material = material(id = "m1"),
            product = p,
            demand = demand
        )
        val second = singlePlan(
            id = "second",
            material = material(id = "m2"),
            product = p,
            demand = demand
        )

        assertNotEquals(
            illegal = first.canonicalKey(),
            actual = second.canonicalKey()
        )
    }

    @Test
    fun canonicalKeyKeepsCapacityConsumption() {
        val p = product(id = "p", width = 0.5)
        val demand = ProductDemand.roll(p, Quantity(Flt64(1.0), RollCountUnit))
        val first = singlePlan(
            id = "first",
            material = material(),
            product = p,
            demand = demand
        ).copy(capacityConsumption = Quantity(Flt64(10.0), Kilogram))
        val second = singlePlan(
            id = "second",
            material = material(),
            product = p,
            demand = demand
        ).copy(capacityConsumption = Quantity(Flt64(12.0), Kilogram))

        assertNotEquals(
            illegal = first.canonicalKey(),
            actual = second.canonicalKey()
        )
    }

    private fun singlePlan(
        id: String,
        material: Material<Flt64>,
        product: Product<Flt64>,
        demand: ProductDemand<Flt64>
    ): CuttingPlan<Flt64> {
        return CuttingPlan(
            id = id,
            material = material,
            slices = listOf(
                CuttingPlanSlice(
                    production = product,
                    width = product.width.first(),
                    amount = UInt64.one
                )
            ),
            demandContributions = listOf(
                CuttingPlanDemandContribution(
                    product = product,
                    quantity = demand.quantity
                )
            ),
            arithmetic = Flt64QuantityArithmetic
        )
    }
}
