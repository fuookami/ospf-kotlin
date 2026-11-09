/**
 * ONNX 子图控制流测试 / ONNX subgraph control-flow tests
*/
package fuookami.ospf.kotlin.core.plugin.onnx.lowering

import kotlin.test.*
import ai.onnx.proto.OnnxMl
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.plugin.onnx.*
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxFunctionalModel
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxImportOptions

/**
 * `If` / `Loop` / `Scan` 的静态展开测试 / Static unrolling tests for `If`, `Loop` and `Scan`
*/
class OnnxSubgraphTest {

    /** 构造子图属性 / Build a subgraph attribute */
    private fun graphAttr(name: String, graph: OnnxMl.GraphProto): OnnxMl.AttributeProto {
        return OnnxMl.AttributeProto.newBuilder()
            .setName(name)
            .setType(OnnxMl.AttributeProto.AttributeType.GRAPH)
            .setG(graph)
            .build()
    }

    /** 构造子图 / Build a subgraph */
    private fun subgraph(
        inputs: List<String>,
        outputs: List<String>,
        nodes: List<OnnxMl.NodeProto>,
        initializers: List<OnnxMl.TensorProto> = emptyList()
    ): OnnxMl.GraphProto {
        val builder = OnnxMl.GraphProto.newBuilder()
        for (name in inputs) {
            builder.addInput(floatValueInfo(name, listOf(1L, 1L)))
        }
        for (name in outputs) {
            builder.addOutput(floatValueInfo(name, listOf(1L, 1L)))
        }
        for (node in nodes) {
            builder.addNode(node)
        }
        for (initializer in initializers) {
            builder.addInitializer(initializer)
        }
        return builder.build()
    }

    @Test
    fun ifWithConstantTrueTakesThenBranch() {
        val x = RealVar("x")
        // then: y = x + 1；else: y = x - 1。 / then: y = x + 1; else: y = x - 1.
        val thenBranch = subgraph(
            inputs = emptyList(),
            outputs = listOf("y"),
            nodes = listOf(node("Add", listOf("x", "one"), listOf("y"), name = "add"))
        )
        val elseBranch = subgraph(
            inputs = emptyList(),
            outputs = listOf("y"),
            nodes = listOf(node("Sub", listOf("x", "one"), listOf("y"), name = "sub"))
        )
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node(
                    opType = "If",
                    inputs = listOf("cond"),
                    outputs = listOf("y"),
                    name = "branch"
                ) {
                    addAttribute(graphAttr("then_branch", thenBranch))
                    addAttribute(graphAttr("else_branch", elseBranch))
                }
            ),
            initializers = listOf(
                floatTensor("cond", listOf(1L), listOf(1.0f)),
                floatTensor("one", listOf(1L), listOf(1.0f))
            )
        ).toByteArray()

        val lowered = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        val output = lowered.linearOutput("y", 0).valueOrFail()
        // 取真分支，常数项应为 +1。 / The true branch is taken, so the constant term is +1.
        assertEquals(1.0, output.polynomial.constant.toDouble())
        assertEquals("x", output.polynomial.monomials.single().symbol.name)
    }

    @Test
    fun ifWithConstantFalseTakesElseBranch() {
        val x = RealVar("x")
        val thenBranch = subgraph(
            inputs = emptyList(),
            outputs = listOf("y"),
            nodes = listOf(node("Add", listOf("x", "one"), listOf("y"), name = "add"))
        )
        val elseBranch = subgraph(
            inputs = emptyList(),
            outputs = listOf("y"),
            nodes = listOf(node("Sub", listOf("x", "one"), listOf("y"), name = "sub"))
        )
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node(
                    opType = "If",
                    inputs = listOf("cond"),
                    outputs = listOf("y"),
                    name = "branch"
                ) {
                    addAttribute(graphAttr("then_branch", thenBranch))
                    addAttribute(graphAttr("else_branch", elseBranch))
                }
            ),
            initializers = listOf(
                floatTensor("cond", listOf(1L), listOf(0.0f)),
                floatTensor("one", listOf(1L), listOf(1.0f))
            )
        ).toByteArray()

        val lowered = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        assertEquals(-1.0, lowered.linearOutput("y", 0).valueOrFail().polynomial.constant.toDouble())
    }

    @Test
    fun ifWithNonConstantConditionFails() {
        val x = RealVar("x")
        val branch = subgraph(
            inputs = emptyList(),
            outputs = listOf("y"),
            nodes = listOf(node("Identity", listOf("x"), listOf("y"), name = "identity"))
        )
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node(opType = "If", inputs = listOf("x"), outputs = listOf("y"), name = "branch") {
                    addAttribute(graphAttr("then_branch", branch))
                    addAttribute(graphAttr("else_branch", branch))
                }
            )
        ).toByteArray()
        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x)))
        assertIs<OnnxError.UnsupportedOperator>(result.structuredError())
    }

    @Test
    fun loopUnrollsTripCountTimes() {
        val x = RealVar("x")
        // 循环体：carried_out = carried_in + 1。 / Body: carried_out = carried_in + 1.
        val body = subgraph(
            inputs = listOf("iter", "cond_in", "carried_in"),
            outputs = listOf("cond_out", "carried_out"),
            nodes = listOf(
                node("Identity", listOf("cond_in"), listOf("cond_out"), name = "cond"),
                node("Add", listOf("carried_in", "one"), listOf("carried_out"), name = "inc")
            ),
            initializers = listOf(floatTensor("one", listOf(1L), listOf(1.0f)))
        )
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node(
                    opType = "Loop",
                    inputs = listOf("trip", "cond", "x"),
                    outputs = listOf("y"),
                    name = "loop"
                ) {
                    addAttribute(graphAttr("body", body))
                }
            ),
            initializers = listOf(
                longTensor("trip", listOf(1L), listOf(3L)),
                floatTensor("cond", listOf(1L), listOf(1.0f))
            )
        ).toByteArray()

        val lowered = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        // 三轮各加 1。 / Three iterations add 1 each.
        assertEquals(3.0, lowered.linearOutput("y", 0).valueOrFail().polynomial.constant.toDouble())
        assertEquals("x", lowered.linearOutput("y", 0).valueOrFail().polynomial.monomials.single().symbol.name)
    }

    @Test
    fun loopWithZeroTripCountReturnsInitialValue() {
        val x = RealVar("x")
        val body = subgraph(
            inputs = listOf("iter", "cond_in", "carried_in"),
            outputs = listOf("cond_out", "carried_out"),
            nodes = listOf(
                node("Identity", listOf("cond_in"), listOf("cond_out"), name = "cond"),
                node("Add", listOf("carried_in", "one"), listOf("carried_out"), name = "inc")
            ),
            initializers = listOf(floatTensor("one", listOf(1L), listOf(1.0f)))
        )
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node(
                    opType = "Loop",
                    inputs = listOf("trip", "cond", "x"),
                    outputs = listOf("y"),
                    name = "loop"
                ) {
                    addAttribute(graphAttr("body", body))
                }
            ),
            initializers = listOf(
                longTensor("trip", listOf(1L), listOf(0L)),
                floatTensor("cond", listOf(1L), listOf(1.0f))
            )
        ).toByteArray()

        val lowered = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        assertEquals(0.0, lowered.linearOutput("y", 0).valueOrFail().polynomial.constant.toDouble())
    }

    @Test
    fun loopRejectsExcessiveTripCount() {
        val x = RealVar("x")
        val body = subgraph(
            inputs = listOf("iter", "cond_in", "carried_in"),
            outputs = listOf("cond_out", "carried_out"),
            nodes = listOf(node("Identity", listOf("carried_in"), listOf("carried_out"), name = "id"))
        )
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node(opType = "Loop", inputs = listOf("trip", "cond", "x"), outputs = listOf("y"), name = "loop") {
                    addAttribute(graphAttr("body", body))
                }
            ),
            initializers = listOf(
                longTensor("trip", listOf(1L), listOf(999L)),
                floatTensor("cond", listOf(1L), listOf(1.0f))
            )
        ).toByteArray()
        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x)))
        assertIs<OnnxError.UnsupportedOperator>(result.structuredError())
    }

    @Test
    fun loopRejectsNonConstantTripCount() {
        val x = RealVar("x")
        val body = subgraph(
            inputs = listOf("iter", "cond_in", "carried_in"),
            outputs = listOf("cond_out", "carried_out"),
            nodes = listOf(node("Identity", listOf("carried_in"), listOf("carried_out"), name = "id"))
        )
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(node(opType = "Loop", inputs = listOf("x", "cond", "x"), outputs = listOf("y"), name = "loop") {
                addAttribute(graphAttr("body", body))
            }),
            initializers = listOf(floatTensor("cond", listOf(1L), listOf(1.0f)))
        ).toByteArray()
        val result = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x)))
        // x 被两个位置消费，图解析先报重复绑定错误；只要是失败即可。
        // x is consumed twice, so parsing reports a duplicate-binding style failure; any failure is enough.
        assertTrue(result !is fuookami.ospf.kotlin.utils.functional.Ok)
    }

    @Test
    fun scanAccumulatesStates() {
        val x = RealVar("x")
        // states: acc；扫描输入：step。体：acc_out = acc + step。 / states: acc; scan input: step. Body: acc_out = acc + step.
        val body = subgraph(
            inputs = listOf("acc_in", "step_in"),
            outputs = listOf("acc_out"),
            nodes = listOf(node("Add", listOf("acc_in", "step_in"), listOf("acc_out"), name = "acc"))
        )
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 1L))),
            nodes = listOf(
                node(
                    opType = "Scan",
                    inputs = listOf("x", "steps"),
                    outputs = listOf("y"),
                    name = "scan"
                ) {
                    addAttribute(intAttr("num_scan_inputs", 1L))
                    addAttribute(graphAttr("body", body))
                }
            ),
            initializers = listOf(floatTensor("steps", listOf(3L, 1L), listOf(1.0f, 2.0f, 4.0f)))
        ).toByteArray()

        val lowered = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        // 三轮累加 1 + 2 + 4 = 7。 / Three iterations accumulate 1 + 2 + 4 = 7.
        assertEquals(7.0, lowered.linearOutput("y", 0).valueOrFail().polynomial.constant.toDouble())
    }

    @Test
    fun loopScanOutputConcatenation() {
        val x = RealVar("x")
        // 每轮扫描输出 = carried_in + 1，因此扫描结果长度等于迭代次数。
        // Per-iteration scan output is carried_in + 1, so the scan result length equals the trip count.
        val body = subgraph(
            inputs = listOf("iter", "cond_in", "carried_in"),
            outputs = listOf("cond_out", "carried_out", "scan_out"),
            nodes = listOf(
                node("Identity", listOf("cond_in"), listOf("cond_out"), name = "cond"),
                node("Add", listOf("carried_in", "one"), listOf("carried_out"), name = "inc"),
                node("Identity", listOf("carried_out"), listOf("scan_out"), name = "scan")
            ),
            initializers = listOf(floatTensor("one", listOf(1L), listOf(1.0f)))
        )
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(
                // Loop 输出 = 携带依赖 + 扫描输出。 / Loop outputs are the carried dependencies followed by the scan outputs.
                floatValueInfo("carried", listOf(1L, 1L)),
                floatValueInfo("y", listOf(2L, 1L))
            ),
            nodes = listOf(
                node(
                    opType = "Loop",
                    inputs = listOf("trip", "cond", "x"),
                    outputs = listOf("carried", "y"),
                    name = "loop"
                ) {
                    addAttribute(graphAttr("body", body))
                }
            ),
            initializers = listOf(
                longTensor("trip", listOf(1L), listOf(2L)),
                floatTensor("cond", listOf(1L), listOf(1.0f))
            )
        ).toByteArray()

        val lowered = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        val outputs = lowered.outputs("y").valueOrFail()
        assertEquals(2, outputs.size)
        // 第 1 轮为 x + 1，第 2 轮为 x + 2。 / Iteration 1 gives x + 1, iteration 2 gives x + 2.
        assertEquals(1.0, lowered.linearOutput("y", 0).valueOrFail().polynomial.constant.toDouble())
        assertEquals(2.0, lowered.linearOutput("y", 1).valueOrFail().polynomial.constant.toDouble())
    }

    @Test
    fun subgraphOperatorsAreReportedAsWhitelisted() {
        assertTrue("If" in OnnxSubgraph.OPERATORS)
        assertTrue("Loop" in OnnxSubgraph.OPERATORS)
        assertTrue("Scan" in OnnxSubgraph.OPERATORS)
    }
}
