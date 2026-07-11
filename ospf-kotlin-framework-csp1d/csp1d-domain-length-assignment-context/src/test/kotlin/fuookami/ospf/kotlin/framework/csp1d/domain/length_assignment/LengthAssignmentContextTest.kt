package fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.Meter

/**
 * Length assignment context unit tests.
 * 长度分配上下文单元测试
 */
class LengthAssignmentContextTest {
    private val arithmetic: QuantityArithmetic<Flt64> = assertNotNull(DefaultQuantityArithmetic.resolveFor(Flt64.one).value)

    /** 执行长度分配，失败时抛出断言错误 / Execute length assignment, throw on failure */
    private fun LengthAssignmentContext<Flt64>.assignOrFail(
        input: LengthAssignmentInput<Flt64>
    ): LengthAssignmentResult<Flt64> {
        return assign(input).value ?: fail("length assignment should succeed")
    }

    /**
     * Create a test dynamic-length product with optional max over-production length.
     * 创建带可选最大超产长度的测试动态长度产品
     *
     * @param id Product identifier / 产品标识
     * @param maxOverProduceLength Maximum over-production length constraint / 最大超产长度约束
     * @return Test dynamic-length product instance / 测试动态长度产品实例
     */
    private fun dynamicProduct(
        id: String = "dp",
        maxOverProduceLength: Quantity<Flt64>? = null
    ): Product<Flt64> {
        return Product.dynamicLengthOf(
            id = ProductIdImpl(id),
            name = "dynamic-$id",
            width = listOf(Quantity(Flt64(1.0), Meter)),
            unitWeight = null
        ).let {
            Product(
                id = it.id,
                name = it.name,
                width = it.width,
                dynamicLength = true,
                maxOverProduceLength = maxOverProduceLength
            )
        }
    }

    @Test
    fun dynamicLengthIsAssignedViaDerivation() {
        val p = dynamicProduct("dp1")
        val demand = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))

        val derivation = LengthDerivation<Flt64> { demandQuantity, _ ->
            Quantity(demandQuantity.value * Flt64(20.0), Meter)
        }

        val ctx = LengthAssignmentContext(arithmetic, derivation)
        val result = ctx.assignOrFail(
            LengthAssignmentInput(
                dynamicProducts = listOf(p),
                demands = listOf(demand)
            )
        )

        assertEquals(1, result.assignments.size)
        assertTrue(result.assignments[0].assignedLength eq Quantity(Flt64(100.0), Meter))
        assertEquals(0, result.overLengthRecords.size)
    }

    @Test
    fun overLengthIsDetectedWhenExceedingMax() {
        val maxLen = Quantity(Flt64(80.0), Meter)
        val p = dynamicProduct("dp2", maxOverProduceLength = maxLen)
        val demand = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))

        val derivation = LengthDerivation<Flt64> { _, _ ->
            Quantity(Flt64(100.0), Meter)
        }

        val ctx = LengthAssignmentContext(arithmetic, derivation)
        val result = ctx.assignOrFail(
            LengthAssignmentInput(
                dynamicProducts = listOf(p),
                demands = listOf(demand)
            )
        )

        assertEquals(1, result.assignments.size)
        assertEquals(1, result.overLengthRecords.size)
        assertTrue(result.overLengthRecords[0].overLength eq Quantity(Flt64(20.0), Meter))
    }

    @Test
    fun skippedWhenDerivationReturnsNull() {
        val p = dynamicProduct("dp3")
        val demand = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))

        val derivation = LengthDerivation<Flt64> { _, _ -> null }

        val ctx = LengthAssignmentContext(arithmetic, derivation)
        val result = ctx.assignOrFail(
            LengthAssignmentInput(
                dynamicProducts = listOf(p),
                demands = listOf(demand)
            )
        )

        assertEquals(0, result.assignments.size)
        assertEquals(0, result.overLengthRecords.size)
    }

    @Test
    fun skippedWhenNoMatchingDemand() {
        val p = dynamicProduct("dp4")
        val otherProduct = Product(
            id = productIdOf("other"),
            name = "other",
            width = listOf(Quantity(Flt64(1.0), Meter))
        )
        val demand = ProductDemand.roll(otherProduct, Quantity(Flt64(5.0), RollCountUnit))

        val derivation = LengthDerivation<Flt64> { _, _ ->
            Quantity(Flt64(100.0), Meter)
        }

        val ctx = LengthAssignmentContext(arithmetic, derivation)
        val result = ctx.assignOrFail(
            LengthAssignmentInput(
                dynamicProducts = listOf(p),
                demands = listOf(demand)
            )
        )

        assertEquals(0, result.assignments.size)
        assertEquals(0, result.overLengthRecords.size)
    }
}
