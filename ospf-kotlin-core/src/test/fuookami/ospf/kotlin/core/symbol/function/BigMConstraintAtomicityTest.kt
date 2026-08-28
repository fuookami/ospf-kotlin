package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*

class BigMConstraintAtomicityTest {
    @Test
    fun addConstraintsRollsBackWritesAfterMiddleFailure() {
        val model = FailingLinearMechanismModel()
        try {
            val existing = relation("existing")
            assertTrue(model.addConstraint(existing, existing.name) is Ok)
            val originalConstraint = model.constraints.single()
            val originalRelations = model.relations.toList()
            model.failOnWrite = 2

            val result = addConstraints(
                model = model,
                constraints = listOf(
                    relation("first"),
                    relation("failed"),
                    relation("unwritten")
                )
            )

            assertTrue(result is Failed)
            assertEquals(1, model.constraints.size)
            assertSame(originalConstraint, model.constraints.single())
            assertEquals(originalRelations, model.relations)
            assertEquals(listOf("existing"), model.relations.map { it.name })

            model.failOnWrite = 4
            model.throwOnWrite = true
            val exceptionResult = addConstraints(
                model = model,
                constraints = listOf(
                    relation("first-after-exception"),
                    relation("failed-after-exception"),
                    relation("unwritten-after-exception")
                )
            )

            assertTrue(exceptionResult is Failed)
            assertEquals(1, model.constraints.size)
            assertSame(originalConstraint, model.constraints.single())
            assertEquals(originalRelations, model.relations)
            assertEquals(listOf("existing"), model.relations.map { it.name })
        } finally {
            model.close()
        }
    }

    @Test
    fun addConstraintsRollsBackAutoMaterializedTokensAfterFailure() {
        val model = FailingLinearMechanismModel()
        val variable = RealVar("big_m_atomic_materialized")
        model.materializeOnWrite = variable
        model.failOnWrite = 1

        try {
            val result = addConstraints(
                model = model,
                constraints = listOf(
                    relation("materialize_first"),
                    relation("materialize_failed")
                )
            )

            assertTrue(result is Failed)
            assertEquals(0, model.constraints.size)
            assertTrue(model.tokens.tokens.none { it.variable === variable })
        } finally {
            model.close()
        }
    }

    @Test
    fun legacyIndicatorHelpersRejectNonFiniteInputsAndOverflow() {
        val indicator = BinVar("big_m_invalid_indicator")
        val side = BinVar("big_m_invalid_side")
        val invalidPolynomial = LinearPolynomial<Flt64>(emptyList(), Flt64.nan)

        assertTrue(
            safePositiveIndicatorConstraints(
                poly = invalidPolynomial,
                indicator = indicator,
                bigM = Flt64.one,
                tolerance = Flt64(0.1),
                namePrefix = "invalid"
            ) is Failed
        )

        val finiteButOverflowing = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, RealVar("big_m_overflow_x"))),
            constant = Flt64.zero
        )
        assertTrue(
            safePositiveIndicatorConstraints(
                poly = finiteButOverflowing,
                indicator = indicator,
                bigM = Flt64(1.0e308),
                tolerance = Flt64(1.0e308),
                namePrefix = "overflow"
            ) is Failed
        )
        assertTrue(
            safeZeroIndicatorConstraints(
                poly = finiteButOverflowing,
                indicator = indicator,
                sideVar = side,
                bigM = Flt64(1.0e308),
                tolerance = Flt64(0.1),
                strictBoundary = Flt64(1.0e308),
                namePrefix = "overflow_zero"
            ) is Failed
        )
    }

    private fun relation(name: String): LinearInequality<Flt64> {
        return LinearInequality(
            lhs = LinearPolynomial(emptyList(), Flt64.zero),
            rhs = LinearPolynomial(emptyList(), Flt64.zero),
            comparison = Comparison.LE,
            name = name
        )
    }

    private class FailingLinearMechanismModel : AbstractLinearMechanismModel<Flt64> {
        override var name: String = "failing-linear-mechanism"
        override val tokens: AbstractTokenTable<Flt64> = AutoTokenTable(Linear, false)
        override val identityRegistry: ModelElementIdentityRegistry? = null
        override val objectFunction: Object = SingleObject(
            category = ObjectCategory.Minimum,
            subObjects = emptyList<LinearSubObject<Flt64>>()
        )

        val relations = mutableListOf<LinearInequality<Flt64>>()
        private val storedConstraints = mutableListOf<Constraint<Flt64, *>>()
        override val constraints: List<Constraint<Flt64, *>> get() = storedConstraints
        var failOnWrite: Int? = null
        var throwOnWrite: Boolean = false
        var materializeOnWrite: RealVar? = null
        private var writeCount = 0

        override fun addConstraint(
            relation: LinearInequality<Flt64>,
            name: String?,
            from: Pair<IntermediateSymbol<out Flt64>, Boolean>?
        ): Try {
            val write = writeCount++
            materializeOnWrite?.let { tokens.find(it) }
            relations.add(relation)
            storedConstraints.add(
                LinearConstraintImpl(
                    lhs = emptyList(),
                    sign = ConstraintRelation.Equal,
                    rhs = Flt64.zero,
                    name = name.orEmpty()
                )
            )
            if (write == failOnWrite && throwOnWrite) {
                throw IllegalStateException("Failed to write test constraint / 测试约束写入失败")
            }
            return if (write == failOnWrite) {
                Failed(
                    Err(
                        ErrorCode.ApplicationFailed,
                        "Failed to write test constraint / 测试约束写入失败"
                    )
                )
            } else {
                ok
            }
        }

        override fun rollbackConstraintsTo(size: Int): Try {
            if (size < 0 || size > storedConstraints.size) {
                return Failed(
                    Err(
                        ErrorCode.IllegalArgument,
                        "Invalid constraint rollback position: $size / 无效的约束回滚位置：$size"
                    )
                )
            }
            storedConstraints.subList(size, storedConstraints.size).clear()
            relations.subList(size, relations.size).clear()
            return ok
        }
    }
}
