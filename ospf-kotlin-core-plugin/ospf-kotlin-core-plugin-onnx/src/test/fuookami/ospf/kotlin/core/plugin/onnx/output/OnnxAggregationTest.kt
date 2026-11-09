/**
 * ONNX 输出聚合测试 / ONNX output aggregation tests
*/
package fuookami.ospf.kotlin.core.plugin.onnx.output

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.ManualTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.plugin.onnx.*
import fuookami.ospf.kotlin.core.plugin.onnx.callback.OnnxCallbackAdapter
import fuookami.ospf.kotlin.core.plugin.onnx.callback.OnnxInputBinding
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxInference
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxFunctionalModel

/**
 * 两条路径上的 sum/mean/first/last/max/min 聚合测试 / sum/mean/first/last/max/min aggregation tests on both paths
*/
class OnnxAggregationTest {

    /** 四元素输出模型 y = x / Four-element output model y = x */
    private fun identityModel(size: Long = 4L): ByteArray {
        return model(
            inputs = listOf(floatValueInfo("x", listOf(1L, size))),
            outputs = listOf(floatValueInfo("y", listOf(1L, size))),
            nodes = listOf(node("Identity", listOf("x"), listOf("y"), name = "identity"))
        ).toByteArray()
    }

    /** 建令牌表并注册变量 / Build a token table with registered variables */
    private fun tokenTable(names: List<String>): Pair<ManualTokenTable<Flt64>, List<RealVar>> {
        val table = ManualTokenTable<Flt64>(category = Linear, checkTokenExists = false)
        val variables = names.map { RealVar(it) }
        for (variable in variables) {
            table.add(variable)
        }
        return table to variables
    }

    @Test
    fun selectionValidatesAggregationShape() {
        val element = OnnxOutputSelection.element("y", 2)
        assertEquals(OnnxAggregation.Element, element.aggregation)
        assertEquals(2, element.elementIndex)
        assertFalse(element.aggregation.isFold)

        val sum = OnnxOutputSelection.fold("y", OnnxAggregation.Sum)
        assertTrue(sum.aggregation.isFold)
        assertFailsWith<IllegalArgumentException> {
            OnnxOutputSelection.fold("y", OnnxAggregation.Element)
        }
        assertFailsWith<IllegalArgumentException> {
            OnnxOutputSelection.element("y", -1)
        }
    }

    @Test
    fun callbackSumMeanFirstLastMaxMin() {
        val (tokens, variables) = tokenTable(listOf("x0", "x1", "x2", "x3"))
        val inference = OnnxInference.load(identityModel()).valueOrFail()
        try {
            val solution = listOf(Flt64(1.0), Flt64(3.0), Flt64(-2.0), Flt64(8.0))
            val expectations = mapOf(
                OnnxAggregation.Sum to 10.0,
                OnnxAggregation.Mean to 2.5,
                OnnxAggregation.First to 1.0,
                OnnxAggregation.Last to 8.0,
                OnnxAggregation.Max to 8.0,
                OnnxAggregation.Min to -2.0
            )
            for ((aggregation, expected) in expectations) {
                val adapter = OnnxCallbackAdapter.create(
                    inference = inference,
                    tokens = tokens,
                    bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
                    selection = OnnxOutputSelection.fold("y", aggregation),
                    converter = IntoValue.Identity
                ).valueOrFail()
                val actual = adapter.evaluate(solution).valueOrFail().toDouble()
                assertEquals(expected, actual, 1e-9, "aggregation=$aggregation")
                assertEquals(expected, adapter.objectiveExtractor()(solution)!!.toDouble(), 1e-9)
            }
        } finally {
            inference.close()
        }
    }

    @Test
    fun callbackElementSelectionStillWorks() {
        val (tokens, variables) = tokenTable(listOf("x0", "x1", "x2", "x3"))
        val inference = OnnxInference.load(identityModel()).valueOrFail()
        try {
            val adapter = OnnxCallbackAdapter.create(
                inference = inference,
                tokens = tokens,
                bindings = listOf(OnnxInputBinding(inputName = "x", variables = variables)),
                outputName = "y",
                elementIndex = 2,
                converter = IntoValue.Identity
            ).valueOrFail()
            assertEquals(-2.0, adapter.evaluate(listOf(Flt64(1.0), Flt64(3.0), Flt64(-2.0), Flt64(8.0))).valueOrFail().toDouble())
        } finally {
            inference.close()
        }
    }

    @Test
    fun loweringSumAndMean() {
        val variables = (0 until 4).map { RealVar("x$it") }
        val model = OnnxFunctionalModel.load(identityModel(), mapOf("x" to variables)).valueOrFail()

        val sum = model.aggregateOutput(OnnxOutputSelection.fold("y", OnnxAggregation.Sum)).valueOrFail()
        assertEquals(4, sum.polynomial.monomials.size)
        assertTrue(sum.polynomial.monomials.all { it.coefficient.toDouble() == 1.0 })

        val mean = model.aggregateOutput(OnnxOutputSelection.fold("y", OnnxAggregation.Mean)).valueOrFail()
        assertEquals(4, mean.polynomial.monomials.size)
        assertTrue(mean.polynomial.monomials.all { kotlin.math.abs(it.coefficient.toDouble() - 0.25) < 1e-12 })
    }

    @Test
    fun loweringFirstAndLast() {
        val variables = (0 until 4).map { RealVar("x$it") }
        val model = OnnxFunctionalModel.load(identityModel(), mapOf("x" to variables)).valueOrFail()

        val first = model.aggregateOutput(OnnxOutputSelection.fold("y", OnnxAggregation.First)).valueOrFail()
        assertEquals("x0", first.polynomial.monomials.single().symbol.name)

        val last = model.aggregateOutput(OnnxOutputSelection.fold("y", OnnxAggregation.Last)).valueOrFail()
        assertEquals("x3", last.polynomial.monomials.single().symbol.name)
    }

    @Test
    fun loweringMaxAndMinRegisterExtremumSymbols() {
        val variables = (0 until 4).map { variable ->
            RealVar("x$variable").also {
                it.range.geq(Flt64(-10.0))
                it.range.leq(Flt64(10.0))
            }
        }
        val model = OnnxFunctionalModel.load(identityModel(), mapOf("x" to variables)).valueOrFail()
        val before = model.symbols.size

        val max = model.aggregateOutput(OnnxOutputSelection.fold("y", OnnxAggregation.Max)).valueOrFail()
        val min = model.aggregateOutput(OnnxOutputSelection.fold("y", OnnxAggregation.Min)).valueOrFail()

        // 每个极值符号 + 输出符号各一个。 / One extremum symbol plus one output symbol each.
        assertTrue(model.symbols.size >= before + 4)
        assertEquals(1, max.polynomial.monomials.size)
        assertEquals(1, min.polynomial.monomials.size)
        assertNotEquals(
            max.polynomial.monomials.single().symbol.name,
            min.polynomial.monomials.single().symbol.name
        )
    }

    @Test
    fun loweringAggregationOnConstantOutputFoldsNumerically() {
        // 常量输出：Max/Min 直接数值折叠，不建极值模型。
        // Constant output: Max/Min fold numerically without building an extremum model.
        val bytes = model(
            inputs = listOf(floatValueInfo("x", listOf(1L, 1L))),
            outputs = listOf(floatValueInfo("y", listOf(1L, 4L))),
            nodes = listOf(node("Identity", listOf("c"), listOf("y"), name = "identity")),
            initializers = listOf(floatTensor("c", listOf(1L, 4L), listOf(1.0f, 9.0f, -3.0f, 5.0f)))
        ).toByteArray()
        val x = RealVar("x")
        val model = OnnxFunctionalModel.load(bytes, mapOf("x" to listOf<Symbol>(x))).valueOrFail()
        val before = model.symbols.size

        val max = model.aggregateOutput(OnnxOutputSelection.fold("y", OnnxAggregation.Max)).valueOrFail()
        // 常量折叠不引入极值符号。 / Constant folding introduces no extremum symbol.
        assertEquals(before + 1, model.symbols.size)
        assertEquals(9.0, max.polynomial.constant.toDouble())
        assertTrue(max.polynomial.monomials.isEmpty())
    }

    @Test
    fun loweringAggregationRejectsOutOfRangeElement() {
        val variables = (0 until 4).map { RealVar("x$it") }
        val model = OnnxFunctionalModel.load(identityModel(), mapOf("x" to variables)).valueOrFail()
        val result = model.aggregateOutput(OnnxOutputSelection.element("y", 9))
        assertIs<OnnxError.OutputSelection>(result.structuredError())
    }

    @Test
    fun loweringAggregationRejectsUnknownOutput() {
        val variables = (0 until 4).map { RealVar("x$it") }
        val model = OnnxFunctionalModel.load(identityModel(), mapOf("x" to variables)).valueOrFail()
        val result = model.aggregateOutput(OnnxOutputSelection.fold("not_y", OnnxAggregation.Sum))
        assertIs<OnnxError.OutputSelection>(result.structuredError())
    }

    @Test
    fun aggregationSymbolsAreRegisteredInModel() {
        val variables = (0 until 4).map { RealVar("x$it") }
        val model = OnnxFunctionalModel.load(identityModel(), mapOf("x" to variables)).valueOrFail()
        val sum = model.aggregateOutput(OnnxOutputSelection.fold("y", OnnxAggregation.Sum)).valueOrFail()
        assertTrue(model.symbols.any { it === sum })
    }
}
