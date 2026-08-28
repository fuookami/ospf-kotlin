package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.RealVar

class UnivariateLinearPiecewiseFailureBoundaryTest {
    @Test
    fun converterFailureShouldNotEscapeConstructionOrRangeRegistration() {
        val converter = ThrowingConverter(shouldThrow = { true })
        val function = try {
            piecewiseFunction(converter = converter)
        } catch (error: RuntimeException) {
            fail("piecewise construction should not invoke a failing converter: ${error.message}")
        }

        val bounds = function.resolveOutputBounds()
        assertTrue(bounds is Failed, "range conversion failure should be returned as Failed")

        val outputRange = function.resolveOutputRange()
        assertTrue(outputRange is Failed, "solver range conversion failure should be returned as Failed")
        function.resultVar.range.set(testRange())
        val beforeLower = function.resultVar.lowerBound!!.value.unwrap()
        val beforeUpper = function.resultVar.upperBound!!.value.unwrap()

        val tokens = AutoTokenTable<Flt64>(Linear, false)
        try {
            val before = tokens.tokens.size
            val registration = function.registerAuxiliaryTokens(tokens)
            assertTrue(registration is Failed, "auxiliary token registration should return Failed")
            assertEquals(before, tokens.tokens.size, "failed range registration must not add tokens")
            assertEquals(beforeLower, function.resultVar.lowerBound!!.value.unwrap())
            assertEquals(beforeUpper, function.resultVar.upperBound!!.value.unwrap())
        } finally {
            tokens.close()
        }
    }

    @Test
    fun existingHelperTokenShouldBePreservedDuringRegistration() {
        val function = piecewiseFunction(converter = IntoValue.Identity)
        val tokens = ManualTokenTable<Flt64>(Linear, true)

        try {
            val existingSelector = function.selectorVars.first()
            assertTrue(tokens.add(existingSelector) is Ok)
            val existingToken = tokens.tokens.single()

            val registration = function.registerAuxiliaryTokens(tokens)

            assertTrue(registration is Ok, "already registered helper tokens should be reused")
            assertEquals(function.helperVariables.size, tokens.tokens.size)
            assertSame(existingToken, tokens.find(existingSelector), "the pre-existing token must be preserved")
            for (helperVariable in function.helperVariables) {
                assertNotNull(tokens.find(helperVariable))
            }
        } finally {
            tokens.close()
        }
    }

    @Test
    fun endpointOverflowShouldReturnFailedWithoutAddingTokens() {
        val function = piecewiseFunction(
            breakpoints = listOf(Flt64.zero, Flt64(3)),
            slopes = listOf(Flt64(Double.MAX_VALUE / 2.0)),
            converter = IntoValue.Identity
        )
        function.resultVar.range.set(testRange())
        val beforeLower = function.resultVar.lowerBound!!.value.unwrap()
        val beforeUpper = function.resultVar.upperBound!!.value.unwrap()
        val bounds = function.resolveOutputBounds()
        assertTrue(bounds is Failed, "overflowed endpoint arithmetic should return Failed")

        val tokens = AutoTokenTable<Flt64>(Linear, false)
        try {
            val registration = function.registerAuxiliaryTokens(tokens)
            assertTrue(registration is Failed, "overflowed output range should reject token registration")
            assertEquals(0, tokens.tokens.size, "failed range registration must not add tokens")
            assertEquals(beforeLower, function.resultVar.lowerBound!!.value.unwrap())
            assertEquals(beforeUpper, function.resultVar.upperBound!!.value.unwrap())
        } finally {
            tokens.close()
        }
    }

    @Test
    fun finiteBoundsFailureDuringDefaultBigMShouldReturnFailedBeforeWritingConstraints() {
        val x = RealVar("piecewise_failure_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(5), x)),
            constant = Flt64.zero
        )
        val function = piecewiseFunction(
            x = xPoly,
            converter = ThrowingConverter(shouldThrow = { it == Flt64(5) })
        )
        val model = NoWriteLinearMechanismModel()
        try {
            assertTrue(function.registerAuxiliaryTokens(model.addableTokens) is Ok)
            function.resultVar.range.set(testRange())
            val beforeLower = function.resultVar.lowerBound!!.value.unwrap()
            val beforeUpper = function.resultVar.upperBound!!.value.unwrap()

            val result = try {
                function.registerConstraints(model)
            } catch (error: RuntimeException) {
                fail("finiteBounds failure should be returned as Failed: ${error.message}")
            }
            assertTrue(result is Failed, "finiteBounds failure should be returned as Failed")
            assertEquals(0, model.addCalls, "failed Big-M inference must not write constraints")
            assertEquals(beforeLower, function.resultVar.lowerBound!!.value.unwrap())
            assertEquals(beforeUpper, function.resultVar.upperBound!!.value.unwrap())
        } finally {
            model.close()
        }
    }

    @Test
    fun defaultBigMConverterFailureShouldReturnFailedBeforeWritingConstraints() {
        val x = RealVar("piecewise_default_m_converter_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val function = piecewiseFunction(
            x = xPoly,
            converter = ThrowingConverter(
                shouldThrow = { false },
                throwOnIntoValue = { it == Flt64(BIG_M_DEFAULT) }
            )
        )
        val model = NoWriteLinearMechanismModel()

        try {
            assertTrue(function.registerAuxiliaryTokens(model.addableTokens) is Ok)
            assertTrue(model.addableTokens.add(x) is Ok)

            val result = try {
                function.registerConstraints(model)
            } catch (error: RuntimeException) {
                fail("defaultBigM failure should be returned as Failed: ${error.message}")
            }
            assertTrue(result is Failed, "defaultBigM failure should be returned as Failed")
            assertEquals(0, model.addCalls, "defaultBigM failure must happen before constraint writes")
        } finally {
            model.close()
        }
    }

    @Test
    fun automaticBigMMustNotHideASecondFiniteBoundsConversionFailure() {
        val x = RealVar("piecewise_no_default_big_m_fallback_x")
        x.range.set(testRange())
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val converter = ThrowingConverter(
            shouldThrow = { false },
            throwOnIntoValue = { value -> value == Flt64(-10.0) }
        )
        val function = piecewiseFunction(x = xPoly, converter = converter)
        val model = NoWriteLinearMechanismModel()

        try {
            assertTrue(function.registerAuxiliaryTokens(model.addableTokens) is Ok)
            assertTrue(model.addableTokens.add(x) is Ok)

            val result = try {
                function.registerConstraints(model)
            } catch (error: RuntimeException) {
                fail("finite-bounds conversion failure should be returned as Failed: ${error.message}")
            }
            assertTrue(result is Failed, "automatic Big-M inference must not fall back after a conversion failure")
            assertEquals(0, model.addCalls)
        } finally {
            model.close()
        }
    }

    @Test
    fun repeatedAuxiliaryRegistrationDoesNotReplaceExistingSolverTokens() {
        val function = piecewiseFunction(converter = IntoValue.Identity)
        val tokens = AutoTokenTable<Flt64>(Linear, false)

        try {
            assertTrue(function.registerAuxiliaryTokens(tokens) is Ok)
            val firstTokens = tokens.tokens.associateBy { it.key }

            assertTrue(function.registerAuxiliaryTokens(tokens) is Ok)
            for (helperVariable in function.helperVariables) {
                assertSame(
                    firstTokens[helperVariable.key],
                    tokens.find(helperVariable),
                    "re-registering a piecewise function must preserve the existing solver token"
                )
            }
        } finally {
            tokens.close()
        }
    }

    @Test
    fun constraintWriteFailureShouldRollbackConstraintsAndPreserveResultRange() {
        val x = RealVar("piecewise_write_failure_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val function = piecewiseFunction(x = xPoly, converter = IntoValue.Identity)
        val model = PartiallyWritingLinearMechanismModel(failOnWrite = 2)

        try {
            assertTrue(function.registerAuxiliaryTokens(model.addableTokens) is Ok)
            model.addableTokens.add(x)
            function.resultVar.range.set(testRange())
            val beforeLower = function.resultVar.lowerBound!!.value.unwrap()
            val beforeUpper = function.resultVar.upperBound!!.value.unwrap()

            val result = try {
                function.registerConstraints(model)
            } catch (error: RuntimeException) {
                fail("constraint write failure should be returned as Failed: ${error.message}")
            }

            assertTrue(result is Failed, "constraint write failure should be returned as Failed")
            assertEquals(0, model.constraints.size, "failed constraint batches must be rolled back")
            assertEquals(beforeLower, function.resultVar.lowerBound!!.value.unwrap())
            assertEquals(beforeUpper, function.resultVar.upperBound!!.value.unwrap())
        } finally {
            model.close()
        }
    }

    @Test
    fun constraintFailureShouldRollbackImplicitTokensConstraintsAndResultRange() {
        val function = piecewiseFunction(converter = IntoValue.Identity)
        val model = PartiallyWritingLinearMechanismModel(
            failOnWrite = 2,
            materializeTokens = true
        )
        val preExisting = RealVar("piecewise_pre_existing_x")

        try {
            assertTrue(model.addableTokens.add(preExisting) is Ok)
            val existingToken = model.addableTokens.tokens.single()
            function.resultVar.range.set(testRange())
            val beforeLower = function.resultVar.lowerBound!!.value.unwrap()
            val beforeUpper = function.resultVar.upperBound!!.value.unwrap()

            val result = try {
                function.registerConstraints(model)
            } catch (error: RuntimeException) {
                fail("implicit token failure should be returned as Failed: ${error.message}")
            }

            assertTrue(result is Failed, "constraint write failure should return Failed")
            assertEquals(1, model.addableTokens.tokens.size, "implicit tokens must be rolled back")
            assertSame(existingToken, model.addableTokens.tokens.single(), "pre-existing token must be preserved")
            assertEquals(0, model.constraints.size, "failed constraints must be rolled back")
            assertEquals(beforeLower, function.resultVar.lowerBound!!.value.unwrap())
            assertEquals(beforeUpper, function.resultVar.upperBound!!.value.unwrap())
        } finally {
            model.close()
        }
    }

    @Test
    fun converterFailureDuringConstraintConstructionShouldPreserveResultRange() {
        val function = piecewiseFunction(
            converter = ThrowingConverter(
                shouldThrow = { false },
                throwOnZero = true
            )
        )
        val model = NoWriteLinearMechanismModel()

        try {
            assertTrue(function.registerAuxiliaryTokens(model.addableTokens) is Ok)
            function.resultVar.range.set(testRange())
            val beforeLower = function.resultVar.lowerBound!!.value.unwrap()
            val beforeUpper = function.resultVar.upperBound!!.value.unwrap()

            val result = try {
                function.registerConstraints(model)
            } catch (error: RuntimeException) {
                fail("converter failure should be returned as Failed: ${error.message}")
            }

            assertTrue(result is Failed, "converter failure should be returned as Failed")
            assertEquals(0, model.addCalls, "converter failure must happen before constraint writes")
            assertEquals(beforeLower, function.resultVar.lowerBound!!.value.unwrap())
            assertEquals(beforeUpper, function.resultVar.upperBound!!.value.unwrap())
        } finally {
            model.close()
        }
    }

    @Test
    fun invalidExplicitBigMShouldFailBeforeWritingConstraints() {
        val invalidValues = listOf(
            Flt64(Double.NaN),
            Flt64.infinity,
            Flt64.negativeInfinity,
            Flt64.minimum,
            Flt64.maximum,
            Flt64.zero
        )

        for (invalidM in invalidValues) {
            val function = piecewiseFunction(
                x = LinearPolynomial(emptyList(), Flt64.zero),
                m = invalidM,
                converter = IntoValue.Identity
            )
            val model = NoWriteLinearMechanismModel()
            try {
                assertTrue(function.registerAuxiliaryTokens(model.addableTokens) is Ok)
                assertTrue(function.registerConstraints(model) is Failed, "invalid m should fail: $invalidM")
                assertEquals(0, model.addCalls, "invalid m must not write constraints: $invalidM")
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun explicitBigMShouldNotUseSentinelInputBounds() {
        val x = RealVar("piecewise_explicit_m_x")
        val function = piecewiseFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            m = Flt64.one,
            converter = IntoValue.Identity
        )
        val model = PartiallyWritingLinearMechanismModel(failOnWrite = Int.MAX_VALUE)

        try {
            assertTrue(function.registerAuxiliaryTokens(model.addableTokens) is Ok)
            assertTrue(model.addableTokens.add(x) is Ok)
            assertTrue(function.registerConstraints(model) is Ok)
            assertTrue(model.constraints.isNotEmpty())
        } finally {
            model.close()
        }
    }

    @Test
    fun automaticBigMShouldRejectSentinelInputBounds() {
        val x = RealVar("piecewise_automatic_m_x")
        val function = piecewiseFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            converter = IntoValue.Identity
        )
        val model = NoWriteLinearMechanismModel()

        try {
            assertTrue(function.registerAuxiliaryTokens(model.addableTokens) is Ok)
            assertTrue(model.addableTokens.add(x) is Ok)
            assertTrue(function.registerConstraints(model) is Failed)
            assertEquals(0, model.addCalls, "sentinel input bounds must fail before writing")
        } finally {
            model.close()
        }
    }

    @Test
    fun nonFiniteGeneratedConstraintShouldFailBeforeWriting() {
        val large = Flt64(Double.MAX_VALUE * 0.75)
        val x = RealVar("piecewise_constraint_overflow_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(large, x)),
            constant = Flt64.zero
        )
        val function = piecewiseFunction(
            x = xPoly,
            breakpoints = listOf(Flt64.zero, Flt64.one),
            slopes = listOf(Flt64.two),
            m = Flt64.one,
            converter = IntoValue.Identity
        )
        val model = NoWriteLinearMechanismModel()

        try {
            assertTrue(function.registerAuxiliaryTokens(model.addableTokens) is Ok)
            assertTrue(model.addableTokens.add(x) is Ok)
            assertTrue(function.registerConstraints(model) is Failed)
            assertEquals(0, model.addCalls, "non-finite generated constraints must not be written")
        } finally {
            model.close()
        }
    }

    @Test
    fun fromPointsInvalidShapeShouldReturnResultFailureWithoutEscaping() {
        val x = LinearPolynomial(emptyList(), Flt64.zero)
        val shortResult = try {
            UnivariateLinearPiecewiseFunction.fromPointsResult(
                x = x,
                points = listOf(point2(Flt64.zero, Flt64.zero)),
                converter = IntoValue.Identity,
                name = "piecewise_short_points"
            )
        } catch (error: RuntimeException) {
            fail("short point input should return Failed: ${error.message}")
        }
        assertTrue(shortResult is Failed)

        val duplicatePoints = listOf(
            point2(Flt64.zero, Flt64.zero),
            point2(Flt64.zero, Flt64.one)
        )
        val duplicateResult = try {
            UnivariateLinearPiecewiseFunction.fromPointsResult(
                x = x,
                points = duplicatePoints,
                converter = IntoValue.Identity,
                name = "piecewise_duplicate_points"
            )
        } catch (error: RuntimeException) {
            fail("duplicate x input should return Failed: ${error.message}")
        }
        assertTrue(duplicateResult is Failed)

        val legacyShort = try {
            UnivariateLinearPiecewiseFunction.fromPoints(
                x = x,
                points = listOf(point2(Flt64.zero, Flt64.zero)),
                converter = IntoValue.Identity,
                name = "piecewise_legacy_short_points"
            )
        } catch (error: RuntimeException) {
            fail("legacy short point input should not escape: ${error.message}")
        }
        assertTrue(legacyShort.resolveOutputBounds() is Failed)

        val legacyDuplicate = try {
            UnivariateLinearPiecewiseFunction.fromPoints(
                x = x,
                points = duplicatePoints,
                converter = IntoValue.Identity,
                name = "piecewise_legacy_duplicate_points"
            )
        } catch (error: RuntimeException) {
            fail("legacy duplicate x input should not escape: ${error.message}")
        }
        assertTrue(legacyDuplicate.resolveOutputBounds() is Failed)
    }

    @Test
    fun fromPointsPointAccessFailureShouldReturnFailedWithoutEscaping() {
        val points = object : AbstractList<Point<Dim2, Flt64>>() {
            override val size: Int = 2

            override fun get(index: Int): Point<Dim2, Flt64> {
                throw IllegalStateException("test point access failure")
            }
        }
        val result = try {
            UnivariateLinearPiecewiseFunction.fromPointsResult(
                x = LinearPolynomial(emptyList(), Flt64.zero),
                points = points,
                converter = IntoValue.Identity,
                name = "piecewise_point_access_failure"
            )
        } catch (error: RuntimeException) {
            fail("point construction failure should be returned as Failed: ${error.message}")
        }

        assertTrue(result is Failed)
    }

    @Test
    fun directConstructionBreakpointSizeFailureShouldReachResultBoundary() {
        val breakpoints = object : AbstractList<Flt64>() {
            override val size: Int
                get() = throw IllegalStateException("test breakpoint size failure")

            override fun get(index: Int): Flt64 {
                throw IllegalStateException("test breakpoint access failure")
            }
        }

        val function = try {
            UnivariateLinearPiecewiseFunction(
                x = LinearPolynomial(emptyList(), Flt64.zero),
                breakpoints = breakpoints,
                slopes = emptyList(),
                intercepts = emptyList(),
                converter = IntoValue.Identity,
                name = "piecewise_breakpoint_size_failure"
            )
        } catch (error: RuntimeException) {
            fail("direct piecewise construction should not escape: ${error.message}")
        }

        assertTrue(function.resolveOutputBounds() is Failed)
        assertEquals("piecewise_breakpoint_size_failure_y", function.resultVar.name)
    }

    private fun piecewiseFunction(
        x: LinearPolynomial<Flt64> = LinearPolynomial(emptyList(), Flt64.zero),
        breakpoints: List<Flt64> = listOf(Flt64.zero, Flt64.one),
        slopes: List<Flt64> = listOf(Flt64.one),
        intercepts: List<Flt64> = listOf(Flt64.zero),
        m: Flt64? = null,
        converter: IntoValue<Flt64>
    ): UnivariateLinearPiecewiseFunction<Flt64> {
        return UnivariateLinearPiecewiseFunction(
            x = x,
            breakpoints = breakpoints,
            slopes = slopes,
            intercepts = intercepts,
            m = m,
            converter = converter,
            name = "piecewise_failure"
        )
    }

    private fun testRange(): ValueRange<Flt64> {
        return ValueRange(
            lb = Flt64(-10.0),
            ub = Flt64(10.0),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = Flt64
        ).value!!
    }

    private class ThrowingConverter(
        private val shouldThrow: (Flt64) -> Boolean,
        private val throwOnZero: Boolean = false,
        private val throwOnIntoValue: (Flt64) -> Boolean = { false },
        private val throwOnIntoValueCall: (Flt64, Int) -> Boolean = { _, _ -> false }
    ) : IntoValue<Flt64> {
        private val intoValueCounts = mutableMapOf<Flt64, Int>()

        override fun intoValue(value: Flt64): Flt64 {
            val count = (intoValueCounts[value] ?: 0) + 1
            intoValueCounts[value] = count
            if (throwOnIntoValue(value) || throwOnIntoValueCall(value, count)) {
                throw IllegalStateException("test converter rejected intoValue")
            }
            return value
        }

        override val zero: Flt64
            get() {
                if (throwOnZero) {
                    throw IllegalStateException("test converter rejected zero")
                }
                return Flt64.zero
            }

        override val one: Flt64 get() = Flt64.one

        override fun fromValue(value: Flt64): Flt64 {
            if (shouldThrow(value)) {
                throw IllegalStateException("test converter rejected value")
            }
            return value
        }
    }

    private class PartiallyWritingLinearMechanismModel(
        private val failOnWrite: Int,
        private val materializeTokens: Boolean = false
    ) : AbstractLinearMechanismModel<Flt64> {
        override var name: String = "piecewise_partial_write_model"
        val addableTokens = AutoTokenTable<Flt64>(Linear, false)
        override val tokens: AbstractTokenTable<Flt64> = addableTokens
        override val identityRegistry: fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry? = null
        override val objectFunction: Object = SingleObject(
            category = ObjectCategory.Minimum,
            subObjects = emptyList<LinearSubObject<Flt64>>()
        )
        private val storedConstraints = mutableListOf<Constraint<Flt64, *>>()
        override val constraints: List<Constraint<Flt64, *>> get() = storedConstraints
        private var writeCount = 0

        override fun addConstraint(
            relation: LinearInequality<Flt64>,
            name: String?,
            from: Pair<IntermediateSymbol<out Flt64>, Boolean>?
        ): Try {
            val write = writeCount++
            if (materializeTokens) {
                for (monomial in relation.lhs.monomials + relation.rhs.monomials) {
                    val variable = monomial.symbol as? fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>
                    if (variable != null) {
                        addableTokens.find(variable)
                    }
                }
            }
            storedConstraints.add(
                LinearConstraintImpl(
                    lhs = emptyList(),
                    sign = ConstraintRelation.Equal,
                    rhs = Flt64.zero,
                    name = name.orEmpty()
                )
            )
            return if (write == failOnWrite) {
                Failed(ErrorCode.ApplicationFailed, "constraint write should fail")
            } else {
                ok
            }
        }

        override fun rollbackConstraintsTo(size: Int): Try {
            return if (size in 0..storedConstraints.size) {
                storedConstraints.subList(size, storedConstraints.size).clear()
                ok
            } else {
                Failed(ErrorCode.IllegalArgument, "invalid constraint rollback position")
            }
        }
    }

    private class NoWriteLinearMechanismModel : AbstractLinearMechanismModel<Flt64> {
        override var name: String = "piecewise_failure_model"
        val addableTokens = AutoTokenTable<Flt64>(Linear, false)
        override val tokens: AbstractTokenTable<Flt64> = addableTokens
        override val identityRegistry: fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry? = null
        override val objectFunction: Object = SingleObject(
            category = ObjectCategory.Minimum,
            subObjects = emptyList<LinearSubObject<Flt64>>()
        )
        override val constraints: List<Constraint<Flt64, *>> = emptyList()
        var addCalls: Int = 0

        override fun addConstraint(
            relation: LinearInequality<Flt64>,
            name: String?,
            from: Pair<IntermediateSymbol<out Flt64>, Boolean>?
        ): Try {
            ++addCalls
            return Failed(ErrorCode.ApplicationFailed, "constraint write should not be reached")
        }

        override fun rollbackConstraintsTo(size: Int): Try {
            return if (size == 0) {
                ok
            } else {
                Failed(ErrorCode.IllegalArgument, "unexpected rollback position")
            }
        }
    }
}
