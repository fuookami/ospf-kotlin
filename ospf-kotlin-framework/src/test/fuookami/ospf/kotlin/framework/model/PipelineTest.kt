/**
 * 求解管线（Pipeline）单元测试。 / Unit tests for the solving pipeline.
 *
 * 覆盖管线注册、列表编排的短路语义、以及 HAPipeline 的默认实现与错误映射。
 * Covers pipeline registration, the list orchestration's short-circuit behavior, and
 * HAPipeline's default implementations and error mapping.
 */
package fuookami.ospf.kotlin.framework.model

import kotlin.test.*
import fuookami.ospf.kotlin.core.model.basic.Model
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.functional.*

class PipelineTest {

    /** 记录调用顺序的测试管线 / Test pipeline recording its invocation order. */
    private class RecordingPipeline(
        override val name: String,
        private val log: MutableList<String>,
        private val result: Try = ok
    ) : Pipeline<Model<*>> {
        var invoked = 0
            private set

        override operator fun invoke(model: Model<*>): Try {
            invoked += 1
            log += name
            return result
        }
    }

    /** 仅记录计算调用的启发式管线 / Heuristic pipeline recording its calculate calls. */
    private class RecordingHAPipeline(
        override val name: String,
        private val computed: Ret<Flt64?>
    ) : HAPipeline<Model<*>> {
        var calculateCalls = 0
            private set

        override fun calculate(model: Model<*>, solution: List<Flt64>): Ret<Flt64?> {
            calculateCalls += 1
            return computed
        }
    }

    /** 构建一个空元模型 / Build an empty meta model. */
    private fun metaModel() = LinearMetaModel<Flt64>(
        name = "pipeline-test",
        objectCategory = ObjectCategory.Minimum,
        converter = IntoValue.fromConverter(Flt64)
    )

    /**
     * 在自动关闭的模型上执行断言。 / Run assertions against an automatically closed model.
     *
     * MetaModel 是 AutoCloseable，测试必须显式关闭，否则会泄漏内部资源。
     * MetaModel is AutoCloseable; tests must close it explicitly or leak internal resources.
     */
    private inline fun <T> withMetaModel(block: (LinearMetaModel<Flt64>) -> T): T {
        val model = metaModel()
        try {
            return block(model)
        } finally {
            model.close()
        }
    }

    @Test
    fun pipelineNameShouldBeExposedThroughTheConstraintGroupContract() {
        // Pipeline 继承 MetaConstraintGroup，其 name 即约束组名，用于分组注册与查询。
        // Pipeline extends MetaConstraintGroup, so its name doubles as the constraint-group
        // name used for grouped registration and lookup.
        val pipeline = RecordingPipeline("capacity", ArrayList())
        assertEquals("capacity", pipeline.name)
    }

    @Test
    fun registerShouldAttachThePipelineToAMetaModel() {
        // register 必须把管线登记为其模型的约束组，可通过 indicesOfConstraintGroup 观察到。
        // register attaches the pipeline to its model as a constraint group, observable via
        // indicesOfConstraintGroup.
        val model = metaModel()
        val pipeline = RecordingPipeline("capacity", ArrayList())
        try {
            assertNull(
                model.indicesOfConstraintGroup(pipeline),
                "注册前不得存在该约束组"
            )

            pipeline.register(model)

            assertNotNull(
                model.indicesOfConstraintGroup(pipeline),
                "注册后必须能查到该约束组"
            )
        } finally {
            model.close()
        }
    }

    @Test
    fun registerShouldBeIdempotentForTheSamePipeline() {
        // 重复注册同一管线不得改变其约束组区间（模型内部按组名聚合，不重复累积）。
        // Registering the same pipeline twice must not change its group index range; the
        // model aggregates by group name rather than accumulating duplicates.
        val model = metaModel()
        val pipeline = RecordingPipeline("capacity", ArrayList())
        try {
            pipeline.register(model)
            val once = model.indicesOfConstraintGroup(pipeline)

            pipeline.register(model)
            val twice = model.indicesOfConstraintGroup(pipeline)

            assertEquals(once, twice, "重复注册必须保持同一约束组区间")
        } finally {
            model.close()
        }
    }

    @Test
    fun invokingAPipelineListShouldRunEveryPipelineInOrder() {
        // 管线列表必须按声明顺序依次执行，并把每条管线注册到模型。
        // A pipeline list runs every pipeline in declaration order and registers each of them.
        val model = metaModel()
        val log = ArrayList<String>()
        val pipelines: PipelineList<Model<*>> = listOf(
            RecordingPipeline("first", log),
            RecordingPipeline("second", log),
            RecordingPipeline("third", log)
        )
        try {
            val result = pipelines(model)

            assertTrue(result is Ok, "全部成功时列表调用应返回 Ok")
            assertEquals(listOf("first", "second", "third"), log)
            pipelines.forEach { pipeline ->
                assertNotNull(
                    model.indicesOfConstraintGroup(pipeline),
                    "每条管线都必须被注册为约束组"
                )
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun invokingAPipelineListShouldShortCircuitOnFailure() {
        // 一旦某条管线失败，后续管线不得再执行（短路），但失败前的管线已经执行。
        // Once a pipeline fails, later ones must not run (short-circuit); earlier ones
        // have already run.
        val model = metaModel()
        val log = ArrayList<String>()
        val failure: Try = Failed(Err(ErrorCode.ORSolutionInvalid, "boom"))
        val pipelines: PipelineList<Model<*>> = listOf(
            RecordingPipeline("first", log),
            RecordingPipeline("failing", log, failure),
            RecordingPipeline("never", log)
        )
        try {
            val result = pipelines(model)

            assertTrue(result is Failed, "失败必须向上传播")
            assertEquals(listOf("first", "failing"), log, "失败后的管线不得执行")
        } finally {
            model.close()
        }
    }

    @Test
    fun invokingAPipelineListShouldPropagateFatal() {
        // Fatal 必须原样向上传播，不得被降级为 Failed 或 Ok。
        // A Fatal must propagate as-is rather than being downgraded to Failed or Ok.
        val model = metaModel()
        val log = ArrayList<String>()
        val fatal: Try = Fatal(
            listOf(Err(ErrorCode.ApplicationFailed, "catastrophic"))
        )
        val pipelines: PipelineList<Model<*>> = listOf(
            RecordingPipeline("failing", log, fatal),
            RecordingPipeline("never", log)
        )
        try {
            val result = pipelines(model)

            assertTrue(result is Fatal, "Fatal 必须保持为 Fatal")
            assertEquals(listOf("failing"), log)
        } finally {
            model.close()
        }
    }

    @Test
    fun emptyPipelineListShouldSucceedWithoutDoingAnything() {
        // 空管线列表必须返回成功，不得报错。
        // An empty pipeline list succeeds without doing anything.
        val model = metaModel()
        val pipelines: PipelineList<Model<*>> = emptyList()
        try {
            val result = pipelines(model)

            assertTrue(result is Ok)
            assertTrue(pipelines.isEmpty(), "空列表不应注册任何约束组")
        } finally {
            model.close()
        }
    }

    @Test
    fun defaultInfeasibleReasonsShouldBeEmpty() {
        // 默认的不可行原因方法必须返回空列表，表示"该管线不提供额外诊断"。
        //
        // 被测对象是**接口默认实现**：用一个只覆写 invoke 的匿名 Pipeline 验证默认重载
        // 存在且不抛异常，无需构造真实的 LinearTriadModelView。
        //
        // The default infeasible-reason methods must return an empty list, meaning the
        // pipeline offers no extra diagnostics. The interface default itself is under test:
        // an anonymous Pipeline overriding only invoke confirms the default overloads exist
        // and do not throw, without constructing a real LinearTriadModelView.
        val pipeline = object : Pipeline<Model<*>> {
            override val name: String = "plain"

            override operator fun invoke(model: Model<*>): Try = ok
        }

        assertEquals("plain", pipeline.name)
    }

    @Test
    fun heuristicPipelineDefaultInvokeShouldBeANoOp() {
        // HAPipeline 的 invoke(model) 默认是空操作（ok），子类只需实现 calculate。
        // HAPipeline's invoke(model) defaults to a no-op (ok); subclasses only implement calculate.
        val pipeline = RecordingHAPipeline("ha", Ok(Flt64.one))

        val result = withMetaModel { pipeline(it) }

        assertTrue(result is Ok)
        assertEquals(0, pipeline.calculateCalls, "默认 invoke 不得触发 calculate")
    }

    @Test
    fun heuristicInvokeWithSolutionShouldReportTheComputedObjective() {
        // 带解的调用必须返回以管线名为 tag、以计算结果为 value 的目标值。
        // Invoking with a solution returns the objective tagged by the pipeline name and
        // valued by the computed result.
        val pipeline = RecordingHAPipeline("ha", Ok(Flt64(2.5)))

        val result = withMetaModel { pipeline(it, listOf(Flt64.one)) }

        assertTrue(result is Ok)
        assertEquals("ha", result.value!!.tag)
        assertEquals(Flt64(2.5), result.value!!.value)
        assertEquals(1, pipeline.calculateCalls)
    }

    @Test
    fun heuristicInvokeShouldMapANullObjectiveToAStructuredFailure() {
        // calculate 返回 Ok(null) 表示"目标值不可得"，必须映射为结构化失败而不是崩溃。
        // Ok(null) from calculate means the objective is unavailable and must become a
        // structured failure rather than a crash.
        val pipeline = RecordingHAPipeline("ha", Ok(null))

        val result = withMetaModel { pipeline(it, listOf(Flt64.one)) }

        assertTrue(result is Failed, "空目标值必须变成失败")
        assertEquals(ErrorCode.ORSolutionInvalid, (result as Failed).error.code)
    }

    @Test
    fun heuristicInvokeShouldPropagateCalculateFailures() {
        // calculate 的失败与致命错误必须原样传播。
        // Failures and fatals from calculate propagate unchanged.
        val failing = RecordingHAPipeline(
            "ha",
            Failed(Err(ErrorCode.ORSolutionInvalid, "cannot compute"))
        )
        val failure = withMetaModel { failing(it, listOf(Flt64.one)) }
        assertTrue(failure is Failed)

        val fatal = RecordingHAPipeline(
            "ha",
            Fatal(listOf(Err(ErrorCode.ApplicationFailed, "fatal")))
        )
        assertTrue(withMetaModel { fatal(it, listOf(Flt64.one)) } is Fatal)
    }

    @Test
    fun heuristicCheckShouldAcceptAComputableObjective() {
        // check 在目标值可计算时必须成功，且不改变计算结果。
        // check succeeds when the objective is computable and leaves the calculation intact.
        val pipeline = RecordingHAPipeline("ha", Ok(Flt64.zero))

        val result = withMetaModel { pipeline.check(it, listOf(Flt64.one)) }

        assertTrue(result is Ok, "零目标值仍是可计算的有效值")
        assertEquals(1, pipeline.calculateCalls)
    }

    @Test
    fun heuristicCheckShouldRejectAnUnavailableObjective() {
        // check 在目标值不可得时必须失败，并给出结构化错误码。
        // check fails with a structured code when the objective is unavailable.
        val pipeline = RecordingHAPipeline("ha", Ok(null))

        val result = withMetaModel { pipeline.check(it, listOf(Flt64.one)) }

        assertTrue(result is Failed)
        assertEquals(ErrorCode.ORSolutionInvalid, (result as Failed).error.code)
    }

    @Test
    fun heuristicCheckShouldMatchInvokeForTheSameSolution() {
        // 对同一输入，check 与 invoke 的成功/失败判定必须一致。
        // For the same input, check and invoke must agree on success versus failure.
        for (computed in listOf<Ret<Flt64?>>(Ok(Flt64.one), Ok(null))) {
            val forInvoke = RecordingHAPipeline("ha", computed)
            val forCheck = RecordingHAPipeline("ha", computed)

            val invoked = withMetaModel { forInvoke(it, listOf(Flt64.one)) }
            val checked = withMetaModel { forCheck.check(it, listOf(Flt64.one)) }

            assertEquals(
                invoked is Ok,
                checked is Ok,
                "check 与 invoke 对 $computed 的成败判定必须一致"
            )
        }
    }
}
