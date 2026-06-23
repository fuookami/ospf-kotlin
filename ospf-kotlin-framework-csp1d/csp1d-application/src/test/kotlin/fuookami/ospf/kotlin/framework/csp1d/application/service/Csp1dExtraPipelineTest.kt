package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.*
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.framework.csp1d.application.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 扩展管线集成测试 / Extra pipeline integration test
 *
 * 验证 Csp1dProduceContext 的 extraPipelines 扩展接口能承载
 * 下游 same unit length / same width 类业务约束。
 *
 * Verify that Csp1dProduceContext's extraPipelines extension interface
 * can carry downstream same unit length / same width business constraints.
 */
class Csp1dExtraPipelineTest {

    /**
     * Fake 同单位长度约束管线 / Fake same unit length constraint pipeline
     *
     * 模拟下游 withSameUnitLengthOnSide 类约束：
     * 对使用同一物料的切割方案，要求其分配的卷长相同。
     * 此处简化为：对指定物料的所有方案，添加一个辅助变量 same_length，
     * 并约束 same_length >= 各方案的 assigned_length。
     *
     * Simulate a downstream withSameUnitLengthOnSide-like constraint:
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
     * 模拟下游 withSameWidthOnSide 类约束：
     * 对使用同一物料的切割方案，要求其宽度分配一致。
     * 此处简化为添加一个辅助变量和约束。
     *
     * Simulate a downstream withSameWidthOnSide-like constraint.
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

    /**
     * Fake 增量扩展管线 / Fake incremental extension pipeline
     */
    class FakeIncrementalPipeline<V : RealNumber<V>> : Csp1dIncrementalPipeline<V> {
        override val name = "fake_incremental"

        var registerCount = 0
            private set

        var addColumnsCount = 0
            private set

        val addedPlanIds = ArrayList<String>()

        override fun invoke(model: LinearMetaModel<Flt64>): fuookami.ospf.kotlin.utils.functional.Try {
            registerCount += 1
            return ok
        }

        override suspend fun addColumns(
            context: Csp1dModelingContext<V>,
            iteration: UInt64,
            newPlans: List<CuttingPlan<V>>,
            model: AbstractLinearMetaModel<Flt64>
        ): Ret<List<CuttingPlan<V>>> {
            addColumnsCount += 1
            addedPlanIds.addAll(newPlans.map { it.id })
            return Ok(newPlans)
        }
    }

    @Test
    /** 测试方法 / Test method */
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
    /** 测试方法 / Test method */
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
    /** 测试方法 / Test method */
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
    /** 测试方法 / Test method */
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

    // ===== Public solve 入口扩展传播测试 =====

    /**
     * 验证普通 MILP public 入口通过 Csp1dSolveConfig 注入扩展管线 /
     * Verify plain MILP public entry injects extension pipeline through Csp1dSolveConfig
     */
    @Test
    fun milpPublicEntryShouldInjectExtensionFromSolveConfig(): Unit = runBlocking {
        val material = testMaterial("mat-milp-ext")
        val product = testProduct("prod-milp-ext")
        val demand = ProductDemand(
            product = product,
            quantity = Quantity(Flt64(100.0), Meter)
        )
        val sameWidthPipeline = FakeSameWidthPipeline<Flt64>(materialId = "mat-milp-ext")
        val extension = Csp1dModelingExtension<Flt64>(
            pipeline = sameWidthPipeline,
            mode = Csp1dExtensionMode.MILP
        )
        val solver = ConstraintNameCapturingSolver()
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(demand),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val solveConfig = Csp1dSolveConfig<Flt64>(
            columnGeneration = problem.configuration,
            extensions = listOf(extension)
        )

        val solution = Csp1dMilp<Flt64>(solver).solve(
            problem = problem,
            solveConfig = solveConfig
        )

        assertEquals(Csp1dSolutionStatus.Feasible, solution.status)
        assertTrue(
            solver.constraintNames.contains("same_width_bound_mat-milp-ext"),
            "Same width constraint should be registered through MILP public entry"
        )
    }

    /**
     * 验证列生成 public 入口通过 Csp1dSolveConfig 注入扩展管线到 LP 和 final MILP /
     * Verify column generation public entry injects extension pipeline through Csp1dSolveConfig to LP and final MILP
     */
    @Test
    fun columnGenerationPublicEntryShouldInjectExtensionFromSolveConfig(): Unit = runBlocking {
        val material = testMaterial("mat-cg-ext")
        val product = testProduct("prod-cg-ext")
        val demand = ProductDemand(
            product = product,
            quantity = Quantity(Flt64(100.0), Meter)
        )
        val sameWidthPipeline = FakeSameWidthPipeline<Flt64>(materialId = "mat-cg-ext")
        val extension = Csp1dModelingExtension<Flt64>(
            pipeline = sameWidthPipeline,
            mode = Csp1dExtensionMode.ALL
        )
        val solver = ConstraintNameCapturingSolver()
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(demand),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val solveConfig = Csp1dSolveConfig<Flt64>(
            columnGeneration = problem.configuration,
            extensions = listOf(extension)
        )

        val result = Csp1dColumnGeneration<Flt64>(solver).solveWithTrace(
            problem = problem,
            solveConfig = solveConfig
        )

        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
        assertTrue(
            solver.constraintNames.contains("same_width_bound_mat-cg-ext"),
            "Same width constraint should be registered through CG public entry"
        )
    }

    /**
     * 验证 LP 上下文在同一个主问题上原地刷新增量扩展管线 /
     * Verify LP context refreshes incremental extension pipelines in place on the same master
     */
    @Test
    fun produceContextShouldRefreshIncrementalPipelineOnAddColumns(): Unit = runBlocking {
        val material = testMaterial("mat-cg-incremental")
        val product = testProduct("prod-cg-incremental")
        val demand = ProductDemand(
            product = product,
            quantity = Quantity(Flt64(100.0), Meter)
        )
        val initialPlan = testCuttingPlan(
            id = "plan-cg-incremental-initial",
            material = material,
            product = product,
            contributionValue = Flt64(50.0)
        )
        val newPlan = testCuttingPlan(
            id = "plan-cg-incremental-new",
            material = material,
            product = product,
            contributionValue = Flt64(75.0)
        )
        val incrementalPipeline = FakeIncrementalPipeline<Flt64>()
        val extension = Csp1dModelingExtension<Flt64>(
            pipeline = incrementalPipeline,
            mode = Csp1dExtensionMode.LP
        )
        val input = ProduceInput(
            cuttingPlans = listOf(initialPlan),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )
        val context = Csp1dProduceContextBuilder(input)
            .mode(Csp1dModelingMode.LP)
            .extension(extension)
            .build()
        val model = LinearMetaModel(
            name = "test_incremental_add_columns",
            converter = fuookami.ospf.kotlin.core.solver.value.IntoValue.Identity
        )
        when (val result = context.register(model)) {
            is Ok -> {}
            is fuookami.ospf.kotlin.utils.functional.Failed -> throw IllegalStateException("register failed: ${result.error}")
            is fuookami.ospf.kotlin.utils.functional.Fatal -> throw IllegalStateException("register fatal: ${result.errors}")
        }

        val addedPlans = when (val result = context.addColumns(
            iteration = UInt64.one,
            newPlans = listOf(newPlan),
            model = model
        )) {
            is Ok -> result.value
            is fuookami.ospf.kotlin.utils.functional.Failed -> throw IllegalStateException("addColumns failed: ${result.error}")
            is fuookami.ospf.kotlin.utils.functional.Fatal -> throw IllegalStateException("addColumns fatal: ${result.errors}")
        }
        assertEquals(listOf(newPlan), addedPlans)
        assertEquals(1, incrementalPipeline.registerCount)
        assertEquals(1, incrementalPipeline.addColumnsCount)
        assertTrue(
            incrementalPipeline.addedPlanIds.contains("plan-cg-incremental-new"),
            "Incremental pipeline should receive the priced plan through addColumns"
        )
    }

    /**
     * 验证多个扩展管线同时通过 public 入口注册 /
     * Verify multiple extension pipelines can be registered simultaneously through public entry
     */
    @Test
    fun multipleExtensionsShouldAllRegisterThroughPublicEntry(): Unit = runBlocking {
        val material1 = testMaterial("mat-multi-ext-1")
        val material2 = testMaterial("mat-multi-ext-2")
        val product = testProduct("prod-multi-ext")
        val demand = ProductDemand(
            product = product,
            quantity = Quantity(Flt64(100.0), Meter)
        )
        val sameWidthPipeline1 = FakeSameWidthPipeline<Flt64>(materialId = "mat-multi-ext-1")
        val sameWidthPipeline2 = FakeSameWidthPipeline<Flt64>(materialId = "mat-multi-ext-2")
        val extensions = listOf(
            Csp1dModelingExtension<Flt64>(
                pipeline = sameWidthPipeline1,
                mode = Csp1dExtensionMode.ALL
            ),
            Csp1dModelingExtension<Flt64>(
                pipeline = sameWidthPipeline2,
                mode = Csp1dExtensionMode.ALL
            )
        )
        val solver = ConstraintNameCapturingSolver()
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material1, material2),
            machines = emptyList(),
            demands = listOf(demand),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val solveConfig = Csp1dSolveConfig<Flt64>(
            columnGeneration = problem.configuration,
            extensions = extensions
        )

        val solution = Csp1dMilp<Flt64>(solver).solve(
            problem = problem,
            solveConfig = solveConfig
        )

        assertEquals(Csp1dSolutionStatus.Feasible, solution.status)
        assertTrue(
            solver.constraintNames.contains("same_width_bound_mat-multi-ext-1"),
            "First extension constraint should be registered"
        )
        assertTrue(
            solver.constraintNames.contains("same_width_bound_mat-multi-ext-2"),
            "Second extension constraint should be registered"
        )
    }

    // ===== Recovery / partial 路径扩展传播测试 =====

    /**
     * 验证 Csp1dRecovery 路径不丢失扩展配置 /
     * Verify Csp1dRecovery path does not lose extension configuration
     *
     * Csp1dRecovery.solveWithTrace 将 solveConfig 透传到 Csp1dMilp.solve，
     * 而 Csp1dMilp.solve 将 extensions 传入 Csp1dMilpSolver.solve，
     * 此处验证整个链路中扩展管线约束已注册到模型。
     */
    @Test
    fun recoveryShouldPreserveExtensionConfig(): Unit = runBlocking {
        val material = testMaterial("mat-recovery-ext")
        val product = testProduct("prod-recovery-ext")
        val demand = ProductDemand(
            product = product,
            quantity = Quantity(Flt64(100.0), Meter)
        )
        val plan = testCuttingPlan("plan-recovery-ext", material, product, Flt64(50.0))
        val sameWidthPipeline = FakeSameWidthPipeline<Flt64>(materialId = "mat-recovery-ext")
        val extension = Csp1dModelingExtension<Flt64>(
            pipeline = sameWidthPipeline,
            mode = Csp1dExtensionMode.ALL
        )
        val solver = ConstraintNameCapturingSolver()
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(demand),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val solveConfig = Csp1dSolveConfig<Flt64>(
            columnGeneration = problem.configuration,
            extensions = listOf(extension)
        )

        val result = Csp1dRecovery<Flt64>(solver).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                solveConfig = solveConfig
            )
        ).valueOrFail()

        assertEquals(Csp1dRecoveryStatus.Solved, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.NotProvided, result.trace.warmStartStatus)
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
        // 验证扩展管线约束已注册到模型 / Verify extension pipeline constraint was registered in model
        assertTrue(
            solver.constraintNames.contains("same_width_bound_mat-recovery-ext"),
            "Same width constraint should be registered through recovery MILP path"
        )
    }

    /**
     * 验证 Csp1dColumnGenerationRecovery 路径不丢失扩展配置 /
     * Verify Csp1dColumnGenerationRecovery path does not lose extension configuration
     *
     * Csp1dColumnGenerationRecovery.solveWithTrace 将 solveConfig 透传到
     * Csp1dColumnGeneration.solve，列生成主循环和最终 MILP 均注入 extensions，
     * 此处验证扩展管线约束在最终 MILP 模型中已注册。
     */
    @Test
    fun columnGenerationRecoveryShouldPreserveExtensionConfig(): Unit = runBlocking {
        val material = testMaterial("mat-cg-recovery-ext")
        val product = testProduct("prod-cg-recovery-ext")
        val demand = ProductDemand(
            product = product,
            quantity = Quantity(Flt64(100.0), Meter)
        )
        val plan = testCuttingPlan("plan-cg-recovery-ext", material, product, Flt64(50.0))
        val sameWidthPipeline = FakeSameWidthPipeline<Flt64>(materialId = "mat-cg-recovery-ext")
        val extension = Csp1dModelingExtension<Flt64>(
            pipeline = sameWidthPipeline,
            mode = Csp1dExtensionMode.ALL
        )
        val solver = ConstraintNameCapturingSolver()
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(demand),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val solveConfig = Csp1dSolveConfig<Flt64>(
            columnGeneration = problem.configuration,
            extensions = listOf(extension)
        )

        val result = Csp1dColumnGenerationRecovery<Flt64>(
            solver = solver,
            pricingGenerator = fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingGenerator<Flt64> { emptyList() }
        ).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                solveConfig = solveConfig
            )
        ).valueOrFail()

        assertEquals(Csp1dRecoveryStatus.Solved, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.NotProvided, result.trace.warmStartStatus)
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
        // 验证扩展管线约束已注册到最终 MILP 模型 / Verify extension constraint registered in final MILP
        assertTrue(
            solver.constraintNames.contains("same_width_bound_mat-cg-recovery-ext"),
            "Same width constraint should be registered through CG recovery final MILP path"
        )
    }

    /**
     * 验证列生成恢复在最终 MILP 失败（partial 路径）时仍保留扩展配置 /
     * Verify CG recovery retains extension configuration even when final MILP fails (partial path)
     *
     * 最终 MILP 失败时 allowPartialSolution=true 会返回 Partial 解，
     * 此处验证即使最终 MILP 失败，扩展管线约束仍被注入模型（失败模型也包含约束）。
     */
    @Test
    fun columnGenerationRecoveryShouldPreserveExtensionConfigWhenFinalMilpFails(): Unit = runBlocking {
        val material = testMaterial("mat-cg-partial-ext")
        val product = testProduct("prod-cg-partial-ext")
        val demand = ProductDemand(
            product = product,
            quantity = Quantity(Flt64(100.0), Meter)
        )
        val plan = testCuttingPlan("plan-cg-partial-ext", material, product, Flt64(50.0))
        val sameWidthPipeline = FakeSameWidthPipeline<Flt64>(materialId = "mat-cg-partial-ext")
        val extension = Csp1dModelingExtension<Flt64>(
            pipeline = sameWidthPipeline,
            mode = Csp1dExtensionMode.ALL
        )
        val solver = FailingMilpButConstraintCapturingSolver()
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(demand),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val solveConfig = Csp1dSolveConfig<Flt64>(
            columnGeneration = problem.configuration,
            extensions = listOf(extension),
            allowPartialSolution = true,
            topKPlanLimit = Int64.one
        )

        val result = Csp1dColumnGenerationRecovery<Flt64>(
            solver = solver,
            pricingGenerator = fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingGenerator<Flt64> { emptyList() }
        ).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                solveConfig = solveConfig
            )
        ).valueOrFail()

        assertEquals(Csp1dSolutionStatus.Partial, result.solution.status)
        // 验证扩展管线约束在尝试求解最终 MILP 时已注册 /
        // Verify extension constraint was registered when attempting final MILP solve
        assertTrue(
            solver.constraintNames.contains("same_width_bound_mat-cg-partial-ext"),
            "Same width constraint should be registered in failed final MILP model (partial path)"
        )
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

    /**
     * 约束名捕获 fake solver / Constraint-name-capturing fake solver
     *
     * 求解成功，同时记录所有约束名，用于验证扩展管线约束已注入模型。
     */
    private class ConstraintNameCapturingSolver : ColumnGenerationSolver {
        override val name: String = "constraint-name-capturing"

        val constraintNames = mutableSetOf<String>()

        override suspend fun solveMILP(
            name: String,
            metaModel: Flt64LinearMetaModel,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Flt64FeasibleSolverOutput> {
            captureConstraintNames(metaModel)
            return Ok(captureFakeFeasibleOutput(metaModel))
        }

        override suspend fun solveLP(
            name: String,
            metaModel: Flt64LinearMetaModel,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<ColumnGenerationSolver.LPResult> {
            captureConstraintNames(metaModel)
            return Ok(
                ColumnGenerationSolver.LPResult(
                    result = captureFakeFeasibleOutput(metaModel),
                    dualSolution = emptyMap()
                )
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun captureConstraintNames(metaModel: Flt64LinearMetaModel) {
            for (constraint in metaModel.constraints) {
                (constraint as? fuookami.ospf.kotlin.core.model.mechanism.LinearInequalityConstraint<Flt64>)?.name?.let {
                    constraintNames.add(it)
                }
            }
        }

        private fun captureFakeFeasibleOutput(metaModel: Flt64LinearMetaModel): Flt64FeasibleSolverOutput {
            val size = metaModel.tokens.tokensInSolver.size
            val solution: Solution<Flt64> = (0 until size).map { Flt64(1.0) }
            return FeasibleSolverOutput(
                obj = Flt64.zero,
                solution = solution,
                time = Duration.ZERO,
                possibleBestObj = Flt64.zero,
                gap = Flt64.zero
            )
        }
    }

    /**
     * MILP 失败但约束名仍捕获的 fake solver /
     * Fake solver that fails MILP but still captures constraint names
     */
    private class FailingMilpButConstraintCapturingSolver : ColumnGenerationSolver {
        override val name: String = "failing-milp-constraint-capturing"

        val constraintNames = mutableSetOf<String>()

        override suspend fun solveMILP(
            name: String,
            metaModel: Flt64LinearMetaModel,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<Flt64FeasibleSolverOutput> {
            captureConstraintNames(metaModel)
            return Failed(ErrorCode.ApplicationError, "forced final MILP failure for partial path test")
        }

        override suspend fun solveLP(
            name: String,
            metaModel: Flt64LinearMetaModel,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<ColumnGenerationSolver.LPResult> {
            captureConstraintNames(metaModel)
            return Ok(
                ColumnGenerationSolver.LPResult(
                    result = captureFakeFeasibleOutput(metaModel),
                    dualSolution = emptyMap()
                )
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun captureConstraintNames(metaModel: Flt64LinearMetaModel) {
            for (constraint in metaModel.constraints) {
                (constraint as? fuookami.ospf.kotlin.core.model.mechanism.LinearInequalityConstraint<Flt64>)?.name?.let {
                    constraintNames.add(it)
                }
            }
        }

        private fun captureFakeFeasibleOutput(metaModel: Flt64LinearMetaModel): Flt64FeasibleSolverOutput {
            val size = metaModel.tokens.tokensInSolver.size
            val solution: Solution<Flt64> = (0 until size).map { Flt64(1.0) }
            return FeasibleSolverOutput(
                obj = Flt64.zero,
                solution = solution,
                time = Duration.ZERO,
                possibleBestObj = Flt64.zero,
                gap = Flt64.zero
            )
        }
    }
}
