package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.variable.Continuous

/**
 * 派生模型的所有权方向回归测试。
 *
 * `dual()` / `elastic()` 会把来源模型记为 `dualOrigin`，而 [LinearTriadModel.close] /
 * [QuadraticTetradModel.close] 曾经在关闭时级联调用 `dualOrigin?.close()`。那会让"关闭派生模型"
 * 静默清空**来源模型**的全部行——这是最难察觉的一类缺陷：来源对象仍然存活、访问不报错，却已经
 * 没有任何约束。SCIP 固定整数 LP 对偶迁移正是踩中者："解完对偶模型 → 关闭它 → 继续读原始 triad
 * 的行对偶"使 `triad.constraints.size` 变成 0，强对偶校验随之失败，对偶被整体误判为不可用。
 *
 * 所有权方向必须是"创建者负责释放"：关闭派生模型只释放它自己。本测试锁定该方向。
 *
 * Regression test for derived-model ownership. `dual()` / `elastic()` record the source model as
 * `dualOrigin`, and [LinearTriadModel.close] / [QuadraticTetradModel.close] used to cascade into
 * `dualOrigin?.close()`. That made closing a derived model silently empty the **source** model's
 * rows — the hardest kind of defect to notice: the source stays alive and keeps answering, but has
 * no constraints left. The SCIP fixed-integer LP dual migration hit exactly this: "solve the dual
 * model, close it, then keep reading the original triad's row duals" left
 * `triad.constraints.size == 0`, failed the strong-duality check, and degraded the whole dual
 * result. Ownership must run creator-to-created: closing a derived model releases only itself.
 */
class DerivedModelCloseOwnershipTest {
    /** 来源模型在派生模型关闭前后必须完全一致的结构事实。 / Structural facts that must survive. */
    private data class Structure(
        val constraintCount: Int,
        val rhsValues: List<String>,
        val names: List<String>,
        /** 关系枚举按名称比较：`model.basic` 与 `model.intermediate` 各有一个同名枚举。 / Compared by
         * name because both `model.basic` and `model.intermediate` declare a `ConstraintRelation`. */
        val signs: List<String>,
        val ids: List<String>,
        val origins: List<Any?>,
        val variableCount: Int
    )

    private fun structure(model: LinearTriadModel): Structure {
        return Structure(
            constraintCount = model.constraints.size,
            rhsValues = model.constraints.rhs.map { it.toString() },
            names = model.constraints.names.toList(),
            signs = model.constraints.signs.map { it.name },
            ids = model.constraints.ids.map { it.value },
            origins = model.constraints.origins.toList(),
            variableCount = model.variables.size
        )
    }

    private fun structure(model: QuadraticTetradModel): Structure {
        return Structure(
            constraintCount = model.constraints.size,
            rhsValues = model.constraints.rhs.map { it.toString() },
            names = model.constraints.names.toList(),
            signs = model.constraints.signs.map { it.name },
            ids = model.constraints.ids.map { it.value },
            origins = model.constraints.origins.toList(),
            variableCount = model.variables.size
        )
    }

    @Test
    fun closingALinearDerivedModelMustNotEmptyItsSource() = runBlocking {
        val source = linearModel()
        try {
            val before = structure(source)
            // 前置条件：来源模型确实有内容，否则本测试会因"空对空"而假通过。
            // Precondition: the source must actually have content, otherwise an empty-vs-empty
            // comparison would pass vacuously.
            assertTrue(before.constraintCount > 0, "source must have constraints")

            val dualModel = source.dual()
            dualModel.close()
            assertEquals(before, structure(source), "closing a dual model must not touch its source")

            val elasticModel = source.elastic(minmaxSlack = true)
            elasticModel.close()
            assertEquals(before, structure(source), "closing an elastic model must not touch its source")

            // 派生模型自身仍应可被独立关闭两次而不抛错。
            // A derived model must remain independently closable, twice, without throwing.
            dualModel.close()
            assertEquals(before, structure(source), "double close must stay side-effect free")
        } finally {
            source.close()
        }
    }

    @Test
    fun closingAQuadraticDerivedModelMustNotEmptyItsSource() = runBlocking {
        val source = quadraticModel()
        try {
            val before = structure(source)
            assertTrue(before.constraintCount > 0, "source must have constraints")

            val dualModel = source.dual()
            dualModel.close()
            assertEquals(before, structure(source), "closing a dual model must not touch its source")

            val elasticModel = source.elastic()
            elasticModel.close()
            assertEquals(before, structure(source), "closing an elastic model must not touch its source")

            dualModel.close()
            assertEquals(before, structure(source), "double close must stay side-effect free")
        } finally {
            source.close()
        }
    }

    private fun linearModel(): LinearTriadModel {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64(10.0),
            type = Continuous,
            origin = null,
            name = "x",
            id = VariableId("variable:x")
        )
        val constraints = LinearConstraintBatch(
            sparseLhs = SparseMatrix<Flt64>().also { matrix ->
                matrix.addRow(SparseVector<Flt64>().also { it.add(0, Flt64.one) })
                matrix.addRow(SparseVector<Flt64>().also { it.add(0, Flt64.one) })
            },
            signs = listOf(ConstraintRelation.Equal, ConstraintRelation.LessEqual),
            rhs = listOf(Flt64.one, Flt64(8.0)),
            names = listOf("balance", "capacity"),
            sources = listOf(ConstraintSource.Origin, ConstraintSource.Origin),
            ids = listOf(ConstraintId("constraint:balance"), ConstraintId("constraint:capacity"))
        )
        return LinearTriadModel(
            impl = BasicLinearTriadModel(listOf(variable), constraints, "close-ownership"),
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = listOf(LinearObjectiveCell(0, Flt64.one)),
                id = ObjectiveId("objective:cost")
            )
        )
    }

    private fun quadraticModel(): QuadraticTetradModel {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64(10.0),
            type = Continuous,
            origin = null,
            name = "x",
            id = VariableId("variable:x")
        )
        val constraints = QuadraticConstraintBatch(
            sparseLhs = SparseQuadraticMatrix().also { matrix ->
                matrix.addRow(SparseQuadraticVector().also { it.add(0, null, Flt64.one) })
                matrix.addRow(SparseQuadraticVector().also { it.add(0, 0, Flt64.one) })
            },
            signs = listOf(ConstraintRelation.Equal, ConstraintRelation.LessEqual),
            rhs = listOf(Flt64.one, Flt64(8.0)),
            names = listOf("balance", "capacity"),
            sources = listOf(ConstraintSource.Origin, ConstraintSource.Origin),
            ids = listOf(ConstraintId("constraint:balance"), ConstraintId("constraint:capacity"))
        )
        return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(listOf(variable), constraints, "close-ownership"),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(
                category = ObjectCategory.Minimum,
                objective = listOf(QuadraticObjectiveCell(0, 0, Flt64.one)),
                id = ObjectiveId("objective:cost")
            )
        )
    }
}
