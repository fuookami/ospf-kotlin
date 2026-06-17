package fuookami.ospf.kotlin.example.core_demo

import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

/** 将数值类型名称与其 IntoValue 转换器配对的测试用例。A test case pairing a numeric type name with its IntoValue converter. */
private data class GenericNumberCase<V>(
    val name: String,
    val converter: IntoValue<V>
) where V : RealNumber<V>, V : NumberField<V>

/** Flt64、Rtn64、FltX 和 RtnX 的预定义泛型数值测试用例。Predefined generic number test cases for Flt64, Rtn64, FltX, and RtnX. */
private object GenericNumberCases {
    val flt64 = GenericNumberCase(
        name = "Flt64",
        converter = IntoValue.Identity
    )

    val rtn64 = GenericNumberCase(
        name = "Rtn64",
        converter = object : IntoValue<Rtn64> {
            override fun intoValue(value: Flt64): Rtn64 = value.toRtn64()
            override val zero: Rtn64 get() = Rtn64(Flt64.zero.toInt64(), Flt64.one.toInt64())
            override val one: Rtn64 get() = Rtn64(Flt64.one.toInt64(), Flt64.one.toInt64())
            override fun fromValue(value: Rtn64): Flt64 = value.toFlt64()
        }
    )

    val fltX = GenericNumberCase(
        name = "FltX",
        converter = object : IntoValue<FltX> {
            override fun intoValue(value: Flt64): FltX = value.toFltX()
            override val zero: FltX get() = FltX.zero
            override val one: FltX get() = FltX.one
            override fun fromValue(value: FltX): Flt64 = value.toFlt64()
        }
    )

    val rtnX = GenericNumberCase(
        name = "RtnX",
        converter = object : IntoValue<RtnX> {
            override fun intoValue(value: Flt64): RtnX = value.toRtnX()
            override val zero: RtnX get() = RtnX(0, 1)
            override val one: RtnX get() = RtnX(1, 1)
            override fun fromValue(value: RtnX): Flt64 = value.toFlt64()
        }
    )
}

/** 演示线性和二次模型构建器在多种数值类型下正常工作。Demonstrates that the linear and quadratic model builders work correctly across multiple numeric types. */
data object GenericNumberDemo {
    /**
     * 线性模型构建摘要（包括约束数量和目标系数）。Summary of a linear model build, including constraint count and objective coefficients.
     *
     * @property success 参数。
     * @property constraintCount 参数。
     * @property objectiveCategory 参数。
     * @property objectiveCoefficients 参数。
     */
    data class LinearBuildSummary(
        val success: Boolean,
        val constraintCount: Int,
        val objectiveCategory: ObjectCategory,
        val objectiveCoefficients: Map<String, Flt64>
    )

    /**
     * 二次模型构建摘要（包括约束数量和目标系数）。Summary of a quadratic model build, including constraint count and objective coefficients.
     *
     * @property success 参数。
     * @property constraintCount 参数。
     * @property objectiveCategory 参数。
     * @property objectiveCoefficients 参数。
     */
    data class QuadraticBuildSummary(
        val success: Boolean,
        val constraintCount: Int,
        val objectiveCategory: ObjectCategory,
        val objectiveCoefficients: Map<Pair<String, String?>, Flt64>
    )

    /**
     * 给定数值类型的线性和二次构建组合摘要。Combined linear and quadratic build summary for a given numeric type.
     *
     * @property numberType 参数。
     * @property linear 参数。
     * @property quadratic 参数。
     */
    data class GenericNumberBuildSummary(
        val numberType: String,
        val linear: LinearBuildSummary,
        val quadratic: QuadraticBuildSummary
    )

    /**
     * Runs linear and quadratic build-and-dump for all predefined numeric types.
 *
     * @return 返回结果。
     */
    fun runBuildAndDump(): List<GenericNumberBuildSummary> {
        return runBlocking {
            return@runBlocking listOf(
                buildCase(GenericNumberCases.flt64),
                buildCase(GenericNumberCases.rtn64),
                buildCase(GenericNumberCases.fltX),
                buildCase(GenericNumberCases.rtnX)
            )
        }
    }

    /** 为单个数值类型用例构建线性和二次摘要。Builds both linear and quadratic summaries for a single numeric type case. */
    private suspend fun <V> buildCase(numberCase: GenericNumberCase<V>): GenericNumberBuildSummary
            where V : RealNumber<V>, V : NumberField<V> {
        val linear = buildLinear(numberCase)
        val quadratic = buildQuadratic(numberCase)
        return GenericNumberBuildSummary(
            numberType = numberCase.name,
            linear = linear,
            quadratic = quadratic
        )
    }

    /** 构建线性 MetaModel 并提取机制级摘要。Constructs a linear MetaModel, extracts mechanism-level summary. */
    private suspend fun <V> buildLinear(numberCase: GenericNumberCase<V>): LinearBuildSummary
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_demo_linear_x")
        val y = RealVar("${numberCase.name.lowercase()}_demo_linear_y")
        val model = LinearMetaModel<V>(
            name = "generic-number-demo-linear-${numberCase.name.lowercase()}",
            objectCategory = ObjectCategory.Minimum,
            converter = numberCase.converter
        )

        try {
            check(model.add(listOf(x, y)) is Ok)

            val lhs = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(numberCase.converter.one, x),
                    LinearMonomial(numberCase.converter.intoValue(Flt64.two), y)
                ),
                constant = numberCase.converter.zero
            )
            val rhs = LinearPolynomial<V>(emptyList(), numberCase.converter.zero)
            val relation = LinearInequality(lhs = lhs, rhs = rhs, comparison = Comparison.LE)
            check(model.addConstraint(relation = relation, name = "c_${numberCase.name.lowercase()}") is Ok)

            val objective = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(numberCase.converter.intoValue(Flt64.two), x),
                    LinearMonomial(numberCase.converter.one, y)
                ),
                constant = numberCase.converter.zero
            )
            check(model.minimize(objective) is Ok)
            val mechanismResult = LinearMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            check(mechanismResult is Ok)
            val mechanismModel = mechanismResult.value
            val subObject = mechanismModel.objectFunction.subObjects.firstOrNull()
            val objectiveCategory = subObject?.category ?: mechanismModel.objectFunction.category
            val objectiveCoefficients = if (subObject != null) {
                subObject.cells
                    .groupBy { it.token.variable.name }
                    .mapValues { (_, cells) ->
                        cells.fold(numberCase.converter.zero) { acc, cell -> acc + cell.coefficient }
                    }
                    .mapValues { (_, value) -> numberCase.converter.fromValue(value) }
            } else {
                objective.monomials.associate { monomial ->
                    monomial.symbol.name to numberCase.converter.fromValue(monomial.coefficient)
                }
            }
            return LinearBuildSummary(
                success = true,
                constraintCount = mechanismModel.constraints.size,
                objectiveCategory = objectiveCategory,
                objectiveCoefficients = objectiveCoefficients
            )
        } finally {
            model.close()
        }
    }

    /** 构建二次 MetaModel 并提取机制级摘要。Constructs a quadratic MetaModel, extracts mechanism-level summary. */
    private suspend fun <V> buildQuadratic(numberCase: GenericNumberCase<V>): QuadraticBuildSummary
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_demo_quad_x")
        val y = RealVar("${numberCase.name.lowercase()}_demo_quad_y")
        val model = QuadraticMetaModel<V>(
            name = "generic-number-demo-quadratic-${numberCase.name.lowercase()}",
            objectCategory = ObjectCategory.Minimum,
            converter = numberCase.converter
        )

        try {
            check(model.add(listOf(x, y)) is Ok)

            val lhs = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(numberCase.converter.one, x, y)),
                constant = numberCase.converter.zero
            )
            val rhs = QuadraticPolynomial<V>(emptyList(), numberCase.converter.zero)
            val relation = QuadraticInequalityOf(lhs = lhs, rhs = rhs, comparison = Comparison.LE)
            check(model.addConstraint(relation = relation, name = "qc_${numberCase.name.lowercase()}") is Ok)

            val objective = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(numberCase.converter.one, x, y),
                    QuadraticMonomial.linear(numberCase.converter.one, x)
                ),
                constant = numberCase.converter.zero
            )
            check(model.minimize(objective) is Ok)
            val mechanismResult = QuadraticMechanismModel.invoke<V>(metaModel = model, concurrent = false)
            check(mechanismResult is Ok)
            val mechanismModel = mechanismResult.value
            val subObject = mechanismModel.objectFunction.subObjects.firstOrNull()
            val objectiveCategory = subObject?.category ?: mechanismModel.objectFunction.category
            val objectiveCoefficients = if (subObject != null) {
                subObject.cells
                    .groupBy { it.token1.variable.name to it.token2?.variable?.name }
                    .mapValues { (_, cells) ->
                        cells.fold(numberCase.converter.zero) { acc, cell -> acc + cell.coefficient }
                    }
                    .mapValues { (_, value) -> numberCase.converter.fromValue(value) }
            } else {
                objective.monomials.associate { monomial ->
                    monomial.symbol1.name to monomial.symbol2?.name to numberCase.converter.fromValue(monomial.coefficient)
                }
            }
            return QuadraticBuildSummary(
                success = true,
                constraintCount = mechanismModel.constraints.size,
                objectiveCategory = objectiveCategory,
                objectiveCoefficients = objectiveCoefficients
            )
        } finally {
            model.close()
        }
    }
}
