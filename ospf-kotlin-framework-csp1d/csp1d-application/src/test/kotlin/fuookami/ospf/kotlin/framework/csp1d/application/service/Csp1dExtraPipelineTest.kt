package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dModelingMode

/**
 * 扩展管线集成测试 / Extra pipeline integration test
 *
 * 验证 Csp1dProduceContext 的 extraPipelines 扩展接口能承载
 * POIT 的 same unit length / same width 类业务约束。
 *
 * Verify that Csp1dProduceContext's extraPipelines extension interface
 * can carry POIT's same unit length / same width business constraints.
 */
class Csp1dExtraPipelineTest {

    /**
     * Fake 同单位长度约束管线 / Fake same unit length constraint pipeline
     *
     * 模拟 POIT 中 withSameUnitLengthOnSide 约束：
     * 对使用同一物料的切割方案，要求其分配的卷长相同。
     * 此处简化为：对指定物料的所有方案，添加一个辅助变量 same_length，
     * 并约束 same_length >= 各方案的 assigned_length。
     *
     * Simulate POIT's withSameUnitLengthOnSide constraint:
     * For cutting plans using the same material, require the assigned length to be the same.
     * Simplified here: for all plans of a specified material, add a helper variable same_length,
     * and constrain same_length >= each plan's assigned_length.
     */
    class FakeSameUnitLengthPipeline<V : RealNumber<V>>(
        private val materialId: String,
        private val cuttingPlans: List<CuttingPlan<V>>,
        private val assignmentIndices: List<Int>
    ) : Pipeline<LinearMetaModel<Flt64>> {

        override val name: String = "fake_same_unit_length_$materialId"

        /** 辅助变量：同单位长度 / Helper variable: same unit length */
        var sameLengthVar: URealVar? = null
            private set

        /** 约束注册计数 / Constraint registration count */
        var constraintCount: Int = 0
            private set

        override fun invoke(model: LinearMetaModel<Flt64>): fuookami.ospf.kotlin.utils.functional.Try {
            val var_ = URealVar("same_length_$materialId")
            when (val result = model.add(var_)) {
                is Ok -> {}
                is fuookami.ospf.kotlin.utils.functional.Failed -> return fuookami.ospf.kotlin.utils.functional.Failed(result.error)
                is fuookami.ospf.kotlin.utils.functional.Fatal -> return fuookami.ospf.kotlin.utils.functional.Fatal(result.errors)
            }
            sameLengthVar = var_

            // 对每个使用该物料的方案，添加约束 same_length >= assigned_length
            // 此处简化：same_length >= 0（仅验证管线能注册约束到模型）
            for (index in assignmentIndices) {
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = LinearPolynomial(
                            monomials = listOf(LinearMonomial(Flt64.one, var_)),
                            constant = Flt64.zero
                        ),
                        rhs = LinearPolynomial(emptyList(), Flt64.zero),
                        comparison = Comparison.GE
                    ),
                    name = "same_unit_length_${materialId}_$index"
                )
                constraintCount++
            }

            return ok
        }
    }

    /**
     * Fake 同宽度约束管线 / Fake same width constraint pipeline
     *
     * 模拟 POIT 中 withSameWidthOnSide 约束：
     * 对使用同一物料的切割方案，要求其宽度分配一致。
     * 此处简化为添加一个辅助变量和约束。
     *
     * Simulate POIT's withSameWidthOnSide constraint.
     */
    class FakeSameWidthPipeline<V : RealNumber<V>>(
        private val materialId: String
    ) : Pipeline<LinearMetaModel<Flt64>> {

        override val name: String = "fake_same_width_$materialId"

        var sameWidthVar: URealVar? = null
            private set

        override fun invoke(model: LinearMetaModel<Flt64>): fuookami.ospf.kotlin.utils.functional.Try {
            val var_ = URealVar("same_width_$materialId")
            when (val result = model.add(var_)) {
                is Ok -> {}
                is fuookami.ospf.kotlin.utils.functional.Failed -> return fuookami.ospf.kotlin.utils.functional.Failed(result.error)
                is fuookami.ospf.kotlin.utils.functional.Fatal -> return fuookami.ospf.kotlin.utils.functional.Fatal(result.errors)
            }
            sameWidthVar = var_

            model.addConstraint(
                relation = LinearInequality(
                    lhs = LinearPolynomial(
                        monomials = listOf(LinearMonomial(Flt64.one, var_)),
                        constant = Flt64.zero
                    ),
                    rhs = LinearPolynomial(emptyList(), Flt64(100.0)),
                    comparison = Comparison.LE
                ),
                name = "same_width_bound_$materialId"
            )

            return ok
        }
    }

    @Test
    fun extraPipelineShouldRegisterSameUnitLengthConstraint(): Unit = runBlocking {
        val material = testMaterial("mat1")
        val product = testProduct("prod1")
        val demand = ProductDemand(
            product = product,
            quantity = Quantity(Flt64(100.0), Meter)
        )
        val plan = testCuttingPlan("plan1", material, product, Flt64(50.0))
        val input = ProduceInput(
            cuttingPlans = listOf(plan),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val sameLengthPipeline = FakeSameUnitLengthPipeline<Flt64>(
            materialId = "mat1",
            cuttingPlans = listOf(plan),
            assignmentIndices = listOf(0)
        )

        val context = Csp1dProduceContextBuilder(input)
            .mode(Csp1dModelingMode.MILP)
            .extraPipeline(sameLengthPipeline)
            .build()

        val model = LinearMetaModel(
            name = "test_extra_pipeline",
            converter = fuookami.ospf.kotlin.core.solver.value.IntoValue.Identity
        )

        when (val result = context.register(model)) {
            is Ok -> {}
            is fuookami.ospf.kotlin.utils.functional.Failed -> throw IllegalStateException("register failed: ${result.error}")
            is fuookami.ospf.kotlin.utils.functional.Fatal -> throw IllegalStateException("register fatal: ${result.errors}")
        }

        // 验证辅助变量已注册 / Verify helper variable was registered
        assertNotNull(sameLengthPipeline.sameLengthVar, "sameLengthVar should be registered")
        // 验证约束已注册 / Verify constraints were registered
        assertEquals(1, sameLengthPipeline.constraintCount, "One same-unit-length constraint should be registered")
        // 验证变量在模型中 / Verify variable is in model
        assertNotNull(model.tokens.find(sameLengthPipeline.sameLengthVar!!), "sameLengthVar should be in model token table")
    }

    @Test
    fun extraPipelineShouldRegisterSameWidthConstraint(): Unit = runBlocking {
        val material = testMaterial("mat1")
        val product = testProduct("prod1")
        val demand = ProductDemand(
            product = product,
            quantity = Quantity(Flt64(100.0), Meter)
        )
        val plan = testCuttingPlan("plan1", material, product, Flt64(50.0))
        val input = ProduceInput(
            cuttingPlans = listOf(plan),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val sameWidthPipeline = FakeSameWidthPipeline<Flt64>(materialId = "mat1")

        val context = Csp1dProduceContextBuilder(input)
            .mode(Csp1dModelingMode.MILP)
            .extraPipeline(sameWidthPipeline)
            .build()

        val model = LinearMetaModel(
            name = "test_extra_pipeline",
            converter = fuookami.ospf.kotlin.core.solver.value.IntoValue.Identity
        )

        when (val result = context.register(model)) {
            is Ok -> {}
            is fuookami.ospf.kotlin.utils.functional.Failed -> throw IllegalStateException("register failed: ${result.error}")
            is fuookami.ospf.kotlin.utils.functional.Fatal -> throw IllegalStateException("register fatal: ${result.errors}")
        }

        assertNotNull(sameWidthPipeline.sameWidthVar, "sameWidthVar should be registered")
        assertNotNull(model.tokens.find(sameWidthPipeline.sameWidthVar!!), "sameWidthVar should be in model token table")
    }

    @Test
    fun multipleExtraPipelinesShouldAllRegister(): Unit = runBlocking {
        val material1 = testMaterial("mat1")
        val material2 = testMaterial("mat2")
        val product = testProduct("prod1")
        val demand = ProductDemand(
            product = product,
            quantity = Quantity(Flt64(100.0), Meter)
        )
        val plan1 = testCuttingPlan("plan1", material1, product, Flt64(50.0))
        val plan2 = testCuttingPlan("plan2", material2, product, Flt64(30.0))
        val input = ProduceInput(
            cuttingPlans = listOf(plan1, plan2),
            demands = listOf(demand),
            materials = listOf(material1, material2),
            machines = emptyList()
        )

        val sameLengthPipeline = FakeSameUnitLengthPipeline<Flt64>(
            materialId = "mat1",
            cuttingPlans = listOf(plan1),
            assignmentIndices = listOf(0)
        )
        val sameWidthPipeline = FakeSameWidthPipeline<Flt64>(materialId = "mat2")

        val context = Csp1dProduceContextBuilder(input)
            .mode(Csp1dModelingMode.MILP)
            .extraPipeline(sameLengthPipeline)
            .extraPipeline(sameWidthPipeline)
            .build()

        val model = LinearMetaModel(
            name = "test_multiple_extra_pipelines",
            converter = fuookami.ospf.kotlin.core.solver.value.IntoValue.Identity
        )

        when (val result = context.register(model)) {
            is Ok -> {}
            is fuookami.ospf.kotlin.utils.functional.Failed -> throw IllegalStateException("register failed: ${result.error}")
            is fuookami.ospf.kotlin.utils.functional.Fatal -> throw IllegalStateException("register fatal: ${result.errors}")
        }

        assertNotNull(sameLengthPipeline.sameLengthVar, "sameLengthVar should be registered")
        assertNotNull(sameWidthPipeline.sameWidthVar, "sameWidthVar should be registered")
        assertEquals(1, sameLengthPipeline.constraintCount, "One same-unit-length constraint should be registered")
    }

    @Test
    fun extraPipelineShouldWorkWithLpMode(): Unit = runBlocking {
        val material = testMaterial("mat1")
        val product = testProduct("prod1")
        val demand = ProductDemand(
            product = product,
            quantity = Quantity(Flt64(100.0), Meter)
        )
        val plan = testCuttingPlan("plan1", material, product, Flt64(50.0))
        val input = ProduceInput(
            cuttingPlans = listOf(plan),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val sameLengthPipeline = FakeSameUnitLengthPipeline<Flt64>(
            materialId = "mat1",
            cuttingPlans = listOf(plan),
            assignmentIndices = listOf(0)
        )

        // LP 模式也支持扩展管线 / LP mode also supports extra pipelines
        val context = Csp1dProduceContextBuilder(input)
            .mode(Csp1dModelingMode.LP)
            .extraPipeline(sameLengthPipeline)
            .build()

        val model = LinearMetaModel(
            name = "test_extra_pipeline_lp",
            converter = fuookami.ospf.kotlin.core.solver.value.IntoValue.Identity
        )

        when (val result = context.register(model)) {
            is Ok -> {}
            is fuookami.ospf.kotlin.utils.functional.Failed -> throw IllegalStateException("register failed: ${result.error}")
            is fuookami.ospf.kotlin.utils.functional.Fatal -> throw IllegalStateException("register fatal: ${result.errors}")
        }

        assertNotNull(sameLengthPipeline.sameLengthVar, "sameLengthVar should be registered in LP mode")
        assertEquals(1, sameLengthPipeline.constraintCount, "One same-unit-length constraint should be registered in LP mode")
    }

    // ===== 测试辅助方法 =====

    private fun testMaterial(id: String): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64(800.0), Meter),
                    upperBound = Quantity(Flt64(1200.0), Meter)
                ),
                step = Quantity(Flt64(1.0), Meter)
            )
        )
    }

    private fun testProduct(id: String): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = listOf(Quantity(Flt64(150.0), Meter))
        )
    }

    private fun testCuttingPlan(
        id: String,
        material: Material<Flt64>,
        product: Product<Flt64>,
        contributionValue: Flt64
    ): CuttingPlan<Flt64> {
        return CuttingPlan(
            id = id,
            material = material,
            slices = listOf(
                CuttingPlanSlice(
                    production = product,
                    width = Quantity(Flt64(150.0), Meter),
                    amount = UInt64(1u)
                )
            ),
            demandContributions = listOf(
                CuttingPlanDemandContribution(
                    product = product,
                    quantity = Quantity(contributionValue, Meter)
                )
            ),
            capacityConsumption = null
        )
    }
}
