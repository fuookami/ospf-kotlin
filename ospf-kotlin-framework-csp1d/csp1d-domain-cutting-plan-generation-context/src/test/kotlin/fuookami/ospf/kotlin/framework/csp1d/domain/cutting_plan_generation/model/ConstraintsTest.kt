package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter

class ConstraintsTest {
    private fun product(id: String = "p"): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = listOf(Quantity(Flt64(0.5), Meter))
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
            )
        )
    }

    private fun slice(product: Product<Flt64>, amount: UInt64 = UInt64.one): CuttingPlanSlice<Flt64> {
        return CuttingPlanSlice(
            production = product,
            width = product.width.first(),
            amount = amount
        )
    }

    private fun context(
        slices: List<CuttingPlanSlice<Flt64>>,
        totalWidth: Double = 1.0,
        upperBound: Double = 2.0,
        material: Material<Flt64> = material()
    ): CuttingPlanConstraintContext<Flt64> {
        return CuttingPlanConstraintContext(
            slices = slices,
            totalWidth = Quantity(Flt64(totalWidth), Meter),
            upperBound = Quantity(Flt64(upperBound), Meter),
            material = material
        )
    }

    @Test
    fun maxKnifeCountConstraintSatisfied() {
        val p = product()
        val constraint = MaxKnifeCountConstraint<Flt64>(UInt64(3UL))
        val ctx = context(listOf(slice(p), slice(p), slice(p)))
        assertTrue(constraint.isSatisfied(ctx))
        assertTrue(constraint.isPruning)
    }

    @Test
    fun maxKnifeCountConstraintViolated() {
        val p = product()
        val constraint = MaxKnifeCountConstraint<Flt64>(UInt64(2UL))
        val ctx = context(listOf(slice(p), slice(p), slice(p)))
        assertFalse(constraint.isSatisfied(ctx))
    }

    @Test
    fun minKnifeCountConstraintSatisfied() {
        val p = product()
        val constraint = MinKnifeCountConstraint<Flt64>(UInt64(2UL))
        val ctx = context(listOf(slice(p), slice(p), slice(p)))
        assertTrue(constraint.isSatisfied(ctx))
        assertFalse(constraint.isPruning)
    }

    @Test
    fun minKnifeCountConstraintViolated() {
        val p = product()
        val constraint = MinKnifeCountConstraint<Flt64>(UInt64(3UL))
        val ctx = context(listOf(slice(p), slice(p)))
        assertFalse(constraint.isSatisfied(ctx))
    }

    @Test
    fun widthUpperBoundConstraintSatisfied() {
        val p = product()
        val constraint = WidthUpperBoundConstraint<Flt64>()
        val ctx = context(listOf(slice(p)), totalWidth = 1.5, upperBound = 2.0)
        assertTrue(constraint.isSatisfied(ctx))
    }

    @Test
    fun widthUpperBoundConstraintViolated() {
        val p = product()
        val constraint = WidthUpperBoundConstraint<Flt64>()
        val ctx = context(listOf(slice(p)), totalWidth = 2.5, upperBound = 2.0)
        assertFalse(constraint.isSatisfied(ctx))
    }

    @Test
    fun maxOverProduceLengthConstraintSatisfied() {
        val p = Product(
            id = "p",
            name = "product-p",
            width = listOf(Quantity(Flt64(0.5), Meter)),
            length = Quantity(Flt64(1.5), Meter)
        )
        val constraint = MaxOverProduceLengthConstraint<Flt64>(Quantity(Flt64(2.0), Meter))
        val ctx = context(listOf(slice(p)))
        assertTrue(constraint.isSatisfied(ctx))
    }

    @Test
    fun maxOverProduceLengthConstraintViolated() {
        val p = Product(
            id = "p",
            name = "product-p",
            width = listOf(Quantity(Flt64(0.5), Meter)),
            length = Quantity(Flt64(3.0), Meter)
        )
        val constraint = MaxOverProduceLengthConstraint<Flt64>(Quantity(Flt64(2.0), Meter))
        val ctx = context(listOf(slice(p)))
        assertFalse(constraint.isSatisfied(ctx))
    }
}