package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionFallbackConstraints
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionFallbackTarget
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionStructure
import fuookami.ospf.kotlin.core.model.intermediate.materializeDeferredFunctionFallbacks

class DeferredFunctionFallbackTransactionTest {
    @Test
    fun secondMaterializerFailureRollsBackToNonZeroCheckpoint() {
        val first = TestStructure("first")
        val second = TestStructure("second")
        val checkpoint = 3
        val target = RecordingTarget(checkpoint)
        val secondError = Err(
            ErrorCode.ApplicationFailed,
            "second materializer failed / 第二节点 materializer 失败"
        )

        val result = materializeDeferredFunctionFallbacks(
            structures = listOf(first, second),
            target = target,
            materializer = { structure ->
                if (structure === first) {
                    Ok(DeferredFunctionFallbackConstraints(listOf(row("first-0"), row("first-1"))))
                } else {
                    Failed(secondError)
                }
            }
        )

        assertTrue(result is Failed)
        assertSame(secondError, (result as Failed).error)
        assertEquals(checkpoint, target.constraintCount)
        assertEquals(listOf(checkpoint), target.rollbackRequests)
        assertEquals(listOf(2), target.appendedSizes)
    }

    @Test
    fun secondFatalMaterializerFailureRollsBackToNonZeroCheckpoint() {
        val first = TestStructure("first")
        val second = TestStructure("second")
        val checkpoint = 4
        val target = RecordingTarget(checkpoint)
        val fatalErrors = listOf(
            Err(ErrorCode.ApplicationError, "fatal materializer error / materializer 致命错误"),
            Err(ErrorCode.IllegalArgument, "invalid second structure / 第二节点结构无效")
        )

        val result = materializeDeferredFunctionFallbacks(
            structures = listOf(first, second),
            target = target,
            materializer = { structure ->
                if (structure === first) {
                    Ok(DeferredFunctionFallbackConstraints(listOf(row("first"))))
                } else {
                    Fatal(fatalErrors)
                }
            }
        )

        assertTrue(result is Fatal)
        assertEquals(fatalErrors, (result as Fatal).errors)
        assertEquals(checkpoint, target.constraintCount)
        assertEquals(listOf(checkpoint), target.rollbackRequests)
        assertEquals(listOf(1), target.appendedSizes)
    }

    @Test
    fun partialAppendFailureRollsBackAllWritesToCheckpoint() {
        val checkpoint = 2
        val target = RecordingTarget(checkpoint).apply {
            appendWriteCount = 1
            appendResult = Failed(
                ErrorCode.ApplicationFailed,
                "append failed after partial write / append 部分写入后失败"
            )
        }

        val result = materializeDeferredFunctionFallbacks(
            structures = listOf(TestStructure("partial")),
            target = target,
            materializer = {
                Ok(
                    DeferredFunctionFallbackConstraints(
                        listOf(row("partial-0"), row("partial-1"), row("partial-2"))
                    )
                )
            }
        )

        assertTrue(result is Failed)
        assertEquals("append failed after partial write / append 部分写入后失败", (result as Failed).error.message)
        assertEquals(checkpoint, target.constraintCount)
        assertEquals(listOf(checkpoint), target.rollbackRequests)
        assertEquals(listOf(3), target.appendedSizes)
    }

    @Test
    fun rollbackFailureAggregatesAllOriginalErrors() {
        val first = TestStructure("first")
        val second = TestStructure("second")
        val checkpoint = 5
        val target = RecordingTarget(checkpoint)
        val originalErrors = listOf(
            Err(ErrorCode.ApplicationError, "first materialization error / 首个 materializer 错误"),
            Err(ErrorCode.ApplicationFailed, "second materialization error / 第二个 materializer 错误")
        )
        val rollbackError = Err(
            ErrorCode.IllegalArgument,
            "rollback failed / rollback 失败"
        )
        target.rollbackResult = Failed(rollbackError)

        val result = materializeDeferredFunctionFallbacks(
            structures = listOf(first, second),
            target = target,
            materializer = { structure ->
                if (structure === first) {
                    Ok(DeferredFunctionFallbackConstraints(listOf(row("first"))))
                } else {
                    Fatal(originalErrors)
                }
            }
        )

        assertTrue(result is Fatal)
        assertEquals(originalErrors + rollbackError, (result as Fatal).errors)
        assertEquals(checkpoint + 1, target.constraintCount)
        assertEquals(listOf(checkpoint), target.rollbackRequests)
    }

    @Test
    fun successfulMaterializationBindsEachNodeToItsExactConstraintRegion() {
        val first = TestStructure("first")
        val second = TestStructure("second")
        val checkpoint = 6
        val target = RecordingTarget(checkpoint)

        val result = materializeDeferredFunctionFallbacks(
            structures = listOf(first, second),
            target = target,
            materializer = { structure ->
                if (structure === first) {
                    Ok(DeferredFunctionFallbackConstraints(listOf(row("first-0"), row("first-1"))))
                } else {
                    Ok(DeferredFunctionFallbackConstraints(listOf(row("second-0"), row("second-1"), row("second-2"))))
                }
            }
        )

        assertTrue(result is Ok)
        val regions = (result as Ok).value
        assertEquals(2, regions.size)
        assertSame(first, regions[0].structure)
        assertEquals(checkpoint, regions[0].firstConstraintIndex)
        assertEquals(2, regions[0].constraintCount)
        assertEquals(checkpoint + 2, regions[0].lastConstraintIndex)
        assertSame(second, regions[1].structure)
        assertEquals(checkpoint + 2, regions[1].firstConstraintIndex)
        assertEquals(3, regions[1].constraintCount)
        assertEquals(checkpoint + 5, regions[1].lastConstraintIndex)
        assertEquals(regions[0].lastConstraintIndex, regions[1].firstConstraintIndex)
        assertEquals(checkpoint + 5, target.constraintCount)
        assertEquals(listOf(2, 3), target.appendedSizes)
        assertTrue(target.rollbackRequests.isEmpty())
    }

    private class TestStructure(val name: String) : DeferredFunctionStructure

    private class RecordingTarget(initialConstraintCount: Int) : DeferredFunctionFallbackTarget {
        private val rows = MutableList(initialConstraintCount) { index -> row("existing-$index") }

        var appendWriteCount: Int? = null
        var appendResult: Try = ok
        var rollbackResult: Try = ok
        val appendedSizes = mutableListOf<Int>()
        val rollbackRequests = mutableListOf<Int>()

        override val constraintCount: Int
            get() = rows.size

        override fun append(constraints: List<LinearInequality<Flt64>>): Try {
            appendedSizes += constraints.size
            rows += constraints.take(appendWriteCount ?: constraints.size)
            return appendResult
        }

        override fun rollback(constraintCount: Int): Try {
            rollbackRequests += constraintCount
            if (rollbackResult is Ok) {
                rows.subList(constraintCount, rows.size).clear()
            }
            return rollbackResult
        }
    }

    private companion object {
        fun row(name: String): LinearInequality<Flt64> {
            return LinearInequality(
                lhs = LinearPolynomial(emptyList(), Flt64.zero),
                rhs = LinearPolynomial(emptyList(), Flt64.zero),
                comparison = Comparison.LE,
                name = name
            )
        }
    }
}
