package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.Test
import kotlin.test.assertEquals
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.variable.Continuous

/**
 * **跨语言互通：基线侧弹性模型的确切形状**
 * Cross-language parity: the exact shape of the baseline's elastic model.
 *
 * 本文件是 C# 侧 `CrossLanguageElasticParityTests` 的**对照方**。
 * 两侧构造**同一个模型**，断言**同一组数字**：
 *
 * | 量 | 值 |
 * | --- | --- |
 * | 变量数 | 4 |
 * | 约束数 | 3 |
 * | 目标项数 | 3 |
 * | 变量名（有序） | `x`, `c0_lb_slack`, `x_lb_slack`, `x_ub_slack` |
 *
 * **基线原本的判据是弱的**：`QuadraticElasticModelTest` 只断言
 * `elastic.variables.size > model.variables.size`。那条判据**无法发现"多加了不该加的松弛列"**——
 * 多一条、少一条都满足"大于"。所以这里钉住**精确数字**。
 *
 * This file is the counterpart of the C# `CrossLanguageElasticParityTests`. Both sides build **the same model** and assert **the same numbers** (table above).
 * **The baseline's original criterion was weak**: `QuadraticElasticModelTest` only asserted `elastic.variables.size > model.variables.size`, which **cannot detect an
 * extra slack column that should not exist** — one more or one fewer both satisfy "greater than". So the exact figures are pinned here.
 *
 * 跑法 / How to run:
 * ```
 * mvn -o -pl ospf-kotlin-core test -Dtest=ElasticShapeParityTest
 * ```
 */
class ElasticShapeParityTest {
    /** **弹性模型的确切形状与 C# 侧一致** / The elastic shape matches the C# side exactly. */
    @Test
    fun elasticShapeShouldMatchTheCSharpSide() {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64.one,
            type = Continuous,
            origin = null,
            name = "x"
        )
        val constraints = QuadraticConstraintBatch(
            sparseLhs = SparseQuadraticMatrix().also { matrix ->
                val row = SparseQuadraticVector()
                row.add(0, null, Flt64.one)
                matrix.addRow(row)
            },
            signs = listOf(ConstraintRelation.LessEqual),
            rhs = listOf(Flt64.zero),
            names = listOf("c0"),
            sources = listOf(ConstraintSource.Origin)
        )
        val model = QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = listOf(variable),
                constraints = constraints,
                name = "single-constraint"
            ),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(ObjectCategory.Minimum, emptyList())
        )

        val elastic = model.elastic()

        assertEquals(4, elastic.variables.size, "variables: x plus row slack, lower-bound slack, upper-bound slack")
        assertEquals(3, elastic.constraints.size, "constraint rows")
        assertEquals(3, elastic.objective.objective.size, "objective cells")
        assertEquals(
            listOf("x", "c0_lb_slack", "x_lb_slack", "x_ub_slack"),
            elastic.variables.map { it.name },
            "the variable names and their order must match the C# side"
        )
    }
}
