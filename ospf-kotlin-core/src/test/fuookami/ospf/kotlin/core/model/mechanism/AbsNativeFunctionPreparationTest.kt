package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.intermediate.AbsStructure
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.AbsFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.URealVar

class AbsNativeFunctionPreparationTest {
    @Test
    fun nativeAbsUsesResultKeyAndOwnsAllHelpers() = runBlocking {
        val scenario = buildScenario(
            lower = -2.0,
            upper = 3.0,
            bigM = 5.0,
            name = "native_abs_owned"
        )
        try {
            val structure = scenario.structure()
            val selected = nativeFunctionSelectorKeys(
                model = scenario.mechanism,
                nativeFunctionKeys = setOf(structure.resultVariable.key),
                fixedVariables = null
            )

            val helperKeys = requireOk(selected)
            assertEquals(
                setOf(
                    structure.positiveVariable.key,
                    structure.negativeVariable.key,
                    structure.signVariable.key
                ),
                helperKeys
            )
            assertFalse(structure.resultVariable.key in helperKeys)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun helperRestrictionAndOrdinaryReferenceForceFallback() = runBlocking {
        val restricted = buildScenario(
            lower = -2.0,
            upper = 3.0,
            bigM = 5.0,
            name = "native_abs_restricted"
        )
        try {
            val structure = restricted.structure()
            (structure.positiveVariable as URealVar).range.leq(Flt64.one)
            assertTrue(
                nativeFunctionSelectorKeys(
                    model = restricted.mechanism,
                    nativeFunctionKeys = setOf(structure.resultVariable.key),
                    fixedVariables = null
                ) is Failed
            )
        } finally {
            restricted.close()
        }

        val referenced = buildScenario(
            lower = -2.0,
            upper = 3.0,
            bigM = 5.0,
            name = "native_abs_referenced"
        )
        try {
            val structure = referenced.structure()
            val relation = LinearInequality(
                lhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(Flt64.one, structure.positiveVariable)),
                    constant = Flt64.zero
                ),
                rhs = LinearPolynomial(emptyList(), Flt64.one),
                comparison = Comparison.LE,
                name = "native_abs_helper_reference"
            )
            assertTrue(referenced.mechanism.addConstraint(relation = relation, name = relation.name) is Ok)
            assertTrue(
                nativeFunctionSelectorKeys(
                    model = referenced.mechanism,
                    nativeFunctionKeys = setOf(structure.resultVariable.key),
                    fixedVariables = null
                ) is Failed
            )
        } finally {
            referenced.close()
        }
    }

    @Test
    fun explicitSmallBigMCannotBecomeNativeWhenInputExceedsIt() = runBlocking {
        val scenario = buildScenario(
            lower = -5.0,
            upper = 5.0,
            bigM = 3.0,
            name = "native_abs_small_m"
        )
        try {
            val structure = scenario.structure()
            assertTrue(
                nativeFunctionSelectorKeys(
                    model = scenario.mechanism,
                    nativeFunctionKeys = setOf(structure.resultVariable.key),
                    fixedVariables = null
                ) is Failed
            )
        } finally {
            scenario.close()
        }
    }

    @Test
    fun nativeAbsRejectsCrossTypeHelperReference() = runBlocking {
        val scenario = buildCrossTypeHelperReferenceScenario()
        try {
            val structure = scenario.absStructure()
            assertTrue(
                nativeFunctionSelectorKeys(
                    model = scenario.mechanism,
                    nativeFunctionKeys = setOf(structure.resultVariable.key),
                    fixedVariables = null
                ) is Failed
            )
        } finally {
            scenario.close()
        }
    }

    private suspend fun buildScenario(
        lower: Double,
        upper: Double,
        bigM: Double,
        name: String
    ): Scenario {
        val input = RealVar("${name}_input")
        input.range.geq(Flt64(lower))
        input.range.leq(Flt64(upper))
        val function = AbsFunction(
            polynomial = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, input)),
                constant = Flt64.zero
            ),
            converter = IntoValue.Identity,
            bigM = Flt64(bigM),
            name = name
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = name,
            configuration = MetaModelConfiguration(
                concurrent = false,
                dumpBlocking = true,
                functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
            ),
            converter = IntoValue.Identity
        )
        assertTrue(metaModel.add(input) is Ok)
        assertTrue(metaModel.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
        assertTrue(metaModel.minimize(function.resultPolynomial) is Ok)
        val mechanism = when (val result = LinearMechanismModel.invoke<Flt64>(
            metaModel = metaModel,
            concurrent = false,
            blocking = true
        )) {
            is Ok -> result.value
            is Failed -> error(result.error.message)
            else -> error("unexpected fatal result")
        }
        return Scenario(metaModel, mechanism)
    }

    private suspend fun buildCrossTypeHelperReferenceScenario(): MixedScenario {
        val input = RealVar("native_abs_cross_input")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(3.0))
        val absolute = AbsFunction(
            polynomial = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, input)),
                constant = Flt64.zero
            ),
            converter = IntoValue.Identity,
            bigM = Flt64(5.0),
            name = "native_abs_cross"
        )
        val piecewise = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, absolute.posVar)),
                constant = Flt64.zero
            ),
            breakpoints = listOf(Flt64.zero, Flt64.one),
            slopes = listOf(Flt64.one),
            intercepts = listOf(Flt64.zero),
            m = Flt64(5.0),
            converter = IntoValue.Identity,
            name = "native_abs_cross_pwl"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "native-abs-cross",
            configuration = MetaModelConfiguration(
                concurrent = false,
                dumpBlocking = true,
                functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
            ),
            converter = IntoValue.Identity
        )
        assertTrue(metaModel.add(input) is Ok)
        assertTrue(metaModel.add(LinearFunctionSymbolAdapter(absolute, IntoValue.Identity)) is Ok)
        assertTrue(metaModel.add(LinearFunctionSymbolAdapter(piecewise, IntoValue.Identity)) is Ok)
        assertTrue(metaModel.minimize(absolute.resultPolynomial) is Ok)
        val mechanism = when (val result = LinearMechanismModel.invoke<Flt64>(
            metaModel = metaModel,
            concurrent = false,
            blocking = true
        )) {
            is Ok -> result.value
            is Failed -> error(result.error.message)
            else -> error("unexpected fatal result")
        }
        return MixedScenario(metaModel, mechanism)
    }

    private data class Scenario(
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>
    ) : AutoCloseable {
        fun structure(): AbsStructure<*> {
            return mechanism.deferredFunctionStructures.single() as AbsStructure<*>
        }

        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }

    private data class MixedScenario(
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>
    ) : AutoCloseable {
        fun absStructure(): AbsStructure<*> {
            return mechanism.deferredFunctionStructures.filterIsInstance<AbsStructure<*>>().single()
        }

        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }

    private fun <T> requireOk(result: Ret<T>): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> error(result.error.message)
            else -> error("unexpected fatal result")
        }
    }
}
