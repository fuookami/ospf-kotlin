package fuookami.ospf.kotlin.core.solver

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.basic.Objective
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.ConstraintSource
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation as ModelConstraintRelation
import fuookami.ospf.kotlin.core.model.mechanism.MathConstraint
import fuookami.ospf.kotlin.core.model.mechanism.MetaConstraintGroup
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseVector
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearObjectiveCell
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.redactedValue
import fuookami.ospf.kotlin.core.solver.report.BackendParameter
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.SolveFingerprinting
import fuookami.ospf.kotlin.core.solver.report.BackendParameterValue
import fuookami.ospf.kotlin.core.solver.report.NormalizedMathematicalModel
import fuookami.ospf.kotlin.core.solver.report.BackendConfigurationSnapshot
import fuookami.ospf.kotlin.core.solver.report.toNormalizedMathematicalModel
import fuookami.ospf.kotlin.core.variable.Continuous

/**
 * 模型与配置指纹规范化边界测试。 / Canonicalization-boundary tests for model and configuration fingerprints.
 *
 * 覆盖 Kotlin 侧真实存在的规范化能力：配置键排序与分隔符转义、敏感值脱敏、约束行排序、
 * 未注册元素的确定性 model-local 标识，以及身份 scope / origin / provenance 参与指纹。
 * Rust 侧存在而 Kotlin 侧缺失的规范化（稀疏项合并、零项剔除、有符号零归一化、非有限值拒绝）
 * 在文件末尾以“记录现状”的形式单独标注，用于暴露缺口而不是伪装成期望行为。
 * Covers the canonicalization that really exists on the Kotlin side: configuration key ordering and
 * delimiter escaping, sensitive-value redaction, constraint-row ordering, deterministic model-local
 * identifiers for unregistered elements, and identity scope / origin / provenance participation.
 * The canonicalization that exists only in Rust (sparse-term merging, zero-term dropping, signed-zero
 * normalization, non-finite rejection) is characterized separately at the end of this file.
 */
class FingerprintCanonicalizationTest {
    /**
     * 验证配置指纹与键顺序无关，且键值中的分隔符不能被伪造。
     * Verifies that the configuration fingerprint is order independent and that delimiters inside
     * keys or values cannot be forged.
     */
    @Test
    fun configurationFingerprintIsOrderIndependentAndEscapesDelimiters() {
        val first = SolveFingerprinting.configuration(mapOf("alpha" to "1", "beta" to "2"))
        val reordered = SolveFingerprinting.configuration(mapOf("beta" to "2", "alpha" to "1"))

        assertEquals(first, reordered)
        assertNotEquals(first, SolveFingerprinting.configuration(mapOf("alpha" to "1", "beta" to "3")))
        assertEquals("SHA-256", first.algorithm)
        assertEquals("1.0", first.schemaVersion)
        assertEquals(64, first.value.length)

        val embeddedNewline = SolveFingerprinting.configuration(mapOf("alpha" to "1\nbeta=2"))
        val splitEntry = SolveFingerprinting.configuration(mapOf("alpha" to "1", "beta=2" to ""))
        assertNotEquals(embeddedNewline, splitEntry)

        val escapedKey = SolveFingerprinting.configuration(mapOf("alpha=1" to "2"))
        val escapedValue = SolveFingerprinting.configuration(mapOf("alpha" to "1=2"))
        assertNotEquals(escapedKey, escapedValue)
    }

    /**
     * 验证 backend 配置快照指纹与参数顺序无关，且敏感值不进入指纹。
     * Verifies that the backend configuration snapshot fingerprint is parameter-order independent and
     * that sensitive values never enter the fingerprint.
     */
    @Test
    fun backendConfigurationSnapshotFingerprintIgnoresOrderAndRedactsSensitiveValues() {
        val threaded = BackendParameter("threads", BackendParameterValue.Integer(4))
        val secret = BackendParameter("licenseKey", BackendParameterValue.Text("license:one"), sensitive = true)
        val first = BackendConfigurationSnapshot("scip", listOf(threaded, secret))
        val reordered = BackendConfigurationSnapshot("scip", listOf(secret, threaded))

        assertEquals(first.fingerprint(), reordered.fingerprint())
        assertEquals("backend-config-1", first.fingerprint().schemaVersion)
        assertEquals("<redacted>", secret.redactedValue())

        val rotatedSecret = first.copy(
            parameters = listOf(threaded, secret.copy(value = BackendParameterValue.Text("license:two")))
        )
        assertEquals(first.fingerprint(), rotatedSecret.fingerprint())

        val revealedSecret = first.copy(
            parameters = listOf(threaded, secret.copy(sensitive = false))
        )
        assertNotEquals(first.fingerprint(), revealedSecret.fingerprint())
        assertNotEquals(
            first.fingerprint(),
            BackendConfigurationSnapshot("gurobi", listOf(threaded, secret)).fingerprint()
        )
    }

    /**
     * 验证模型指纹与约束行顺序无关：规范文本按稳定约束 ID 排序。
     * Verifies that the model fingerprint is independent of constraint row order because the
     * canonical text is sorted by stable constraint ID.
     */
    @Test
    fun modelFingerprintIsIndependentOfConstraintRowOrder() {
        val firstRow = FingerprintRow(
            name = "row-a",
            entries = listOf(0 to Flt64.one),
            rhs = Flt64.one,
            id = ConstraintId("constraint:a")
        )
        val secondRow = FingerprintRow(
            name = "row-b",
            entries = listOf(0 to Flt64(2.0)),
            rhs = Flt64(2.0),
            id = ConstraintId("constraint:b")
        )
        val ascending = linearModel(
            name = "row-order",
            variables = listOf(variable(index = 0, name = "x", id = VariableId("variable:x"))),
            rows = listOf(firstRow, secondRow)
        )
        val descending = linearModel(
            name = "row-order",
            variables = listOf(variable(index = 0, name = "x", id = VariableId("variable:x"))),
            rows = listOf(secondRow, firstRow)
        )

        val ascendingNormalized = ascending.toNormalizedMathematicalModel()
        val descendingNormalized = descending.toNormalizedMathematicalModel()

        assertEquals(listOf("constraint:a", "constraint:b"), ascendingNormalized.constraints.map { it.id.value })
        assertEquals(listOf("constraint:b", "constraint:a"), descendingNormalized.constraints.map { it.id.value })
        assertEquals(ascendingNormalized.fingerprint(), descendingNormalized.fingerprint())
    }

    /**
     * 验证未注册元素使用按索引生成的确定性 model-local 标识，因此独立重建得到相同指纹。
     * Verifies that unregistered elements receive deterministic index-derived model-local identifiers,
     * so independently rebuilt models share the same fingerprint.
     */
    @Test
    fun modelFingerprintUsesRebuildStableIndexDerivedIdentifiers() {
        val first = linearModel(
            name = "rebuild-stable",
            variables = listOf(variable(index = 0, name = "x")),
            rows = listOf(FingerprintRow(name = "row", entries = listOf(0 to Flt64.one), rhs = Flt64.one))
        )
        val second = linearModel(
            name = "rebuild-stable",
            variables = listOf(variable(index = 0, name = "x")),
            rows = listOf(FingerprintRow(name = "row", entries = listOf(0 to Flt64.one), rhs = Flt64.one))
        )

        val firstNormalized = first.toNormalizedMathematicalModel()
        val secondNormalized = second.toNormalizedMathematicalModel()

        assertEquals("model-local-variable:0", firstNormalized.variables.single().id.value)
        assertEquals("model-local-constraint:0", firstNormalized.constraints.single().id.value)
        assertEquals(firstNormalized.fingerprint(), secondNormalized.fingerprint())

        val registered = linearModel(
            name = "rebuild-stable",
            variables = listOf(variable(index = 0, name = "x", id = VariableId("variable:x"))),
            rows = listOf(
                FingerprintRow(
                    name = "row",
                    entries = listOf(0 to Flt64.one),
                    rhs = Flt64.one,
                    id = ConstraintId("constraint:row")
                )
            )
        )
        assertNotEquals(firstNormalized.fingerprint(), registered.toNormalizedMathematicalModel().fingerprint())
    }

    /**
     * 验证模型名称参与指纹。
     *
     * Rust 的 `linear_model_fingerprint` 纳入模型名；Kotlin 此前完全不含名称，导致同名/改名
     * 模型指纹相同。此用例固定名称现在是身份的一部分。
     *
     * Verifies that the model name participates in the fingerprint. Rust's
     * `linear_model_fingerprint` includes the model name, whereas Kotlin previously omitted it
     * entirely, so a renamed model kept the same fingerprint. This pins the name as part of the
     * identity.
     */
    @Test
    fun modelFingerprintIncludesTheModelName() {
        fun fingerprint(name: String) = linearModel(
            name = name,
            variables = listOf(variable(index = 0, name = "x", id = VariableId("variable:x"))),
            rows = listOf(
                FingerprintRow(
                    name = "row",
                    entries = listOf(0 to Flt64.one),
                    rhs = Flt64.one,
                    id = ConstraintId("constraint:row")
                )
            )
        ).toNormalizedMathematicalModel().fingerprint()

        assertNotEquals(fingerprint("model-a"), fingerprint("model-b"), "模型名必须参与指纹")
        assertEquals(fingerprint("model-a"), fingerprint("model-a"), "同名必须稳定同指纹")
    }

    /**
     * 验证变量名参与指纹。 / Verifies that variable names participate in the fingerprint.
     */
    @Test
    fun modelFingerprintIncludesVariableNames() {
        fun fingerprint(variableName: String) = linearModel(
            name = "variable-name",
            variables = listOf(variable(index = 0, name = variableName, id = VariableId("variable:x"))),
            rows = listOf(
                FingerprintRow(
                    name = "row",
                    entries = listOf(0 to Flt64.one),
                    rhs = Flt64.one,
                    id = ConstraintId("constraint:row")
                )
            )
        ).toNormalizedMathematicalModel().fingerprint()

        assertNotEquals(fingerprint("supply"), fingerprint("demand"), "变量名必须参与指纹")
    }

    /**
     * 验证约束名与来源参与指纹。 / Verifies that constraint names and sources participate in the fingerprint.
     */
    @Test
    fun modelFingerprintIncludesConstraintNameAndSource() {
        fun fingerprint(rowName: String, source: ConstraintSource) = linearModel(
            name = "constraint-name",
            variables = listOf(variable(index = 0, name = "x", id = VariableId("variable:x"))),
            rows = listOf(
                FingerprintRow(
                    name = rowName,
                    entries = listOf(0 to Flt64.one),
                    rhs = Flt64.one,
                    id = ConstraintId("constraint:row"),
                    source = source
                )
            )
        ).toNormalizedMathematicalModel().fingerprint()

        assertNotEquals(
            fingerprint("capacity", ConstraintSource.Origin),
            fingerprint("demand", ConstraintSource.Origin),
            "约束名必须参与指纹"
        )
        assertNotEquals(
            fingerprint("capacity", ConstraintSource.Origin),
            fingerprint("capacity", ConstraintSource.UpperBound),
            "约束来源必须参与指纹"
        )
    }

    /**
     * 验证约束优先级与 lazy 标志参与模型指纹。
     *
     * Rust 的 `linear_model_fingerprint` 纳入 `constraint_priorities` 与 `constraint_lazy_flags`；
     * Kotlin 此前两者都不参与，因此"调整优先级"或"把约束改为 lazy"不会改变指纹，缓存可能复用
     * 错误结果。优先级与 lazy 都会改变求解语义，必须进入身份。
     *
     * Verifies that constraint priority and the lazy flag participate in the model fingerprint.
     * Rust's `linear_model_fingerprint` includes `constraint_priorities` and
     * `constraint_lazy_flags`, whereas Kotlin previously omitted both, so changing a priority or
     * marking a constraint lazy did not change the fingerprint and a cache could reuse the wrong
     * result. Both alter solving semantics and therefore belong to the identity.
     */
    @Test
    fun modelFingerprintIncludesConstraintPriorityAndLazyFlag() {
        val variables = listOf(variable(index = 0, name = "x", id = VariableId("variable:x")))
        val rows = listOf(
            FingerprintRow(
                name = "row",
                entries = listOf(0 to Flt64.one),
                rhs = Flt64.one,
                id = ConstraintId("constraint:row")
            )
        )

        fun fingerprint(priority: Int?, lazy: Boolean): String = linearModel(
            name = "constraint-priority",
            variables = variables,
            rows = rows,
            priorities = listOf(priority),
            lazies = listOf(lazy)
        ).toNormalizedMathematicalModel().fingerprint().value

        val baseline = fingerprint(1, false)

        assertNotEquals(baseline, fingerprint(2, false), "优先级必须参与指纹")
        assertNotEquals(baseline, fingerprint(null, false), "缺失优先级必须与有值不同")
        assertNotEquals(baseline, fingerprint(1, true), "lazy 标志必须参与指纹")
        assertEquals(baseline, fingerprint(1, false), "相同元数据必须稳定同指纹")

        // 规范化后的元数据必须真的被读取到，而不是恒为默认值。
        // The normalized metadata must actually be populated rather than defaulting to null.
        val normalized = linearModel(
            name = "constraint-priority",
            variables = variables,
            rows = rows,
            priorities = listOf(7),
            lazies = listOf(true)
        ).toNormalizedMathematicalModel()
        assertEquals(7, normalized.constraints.single().priority)
        assertEquals(true, normalized.constraints.single().lazy)
    }

    /**
     * 验证约束的 scope、origin 与 provenance 元数据参与模型指纹。
     * Verifies that constraint scope, origin, and provenance metadata participate in the model
     * fingerprint.
     */
    @Test
    fun modelFingerprintIncludesConstraintScopeOriginAndProvenanceMetadata() {
        val capacity = ModelElementOrigin("pipeline", "capacity")

        fun model(
            scope: ModelElementScope,
            origin: ModelElementOrigin,
            provenance: List<ModelElementOrigin>
        ): NormalizedMathematicalModel {
            return linearModel(
                name = "constraint-metadata",
                variables = listOf(variable(index = 0, name = "x", id = VariableId("variable:x"))),
                rows = listOf(
                    FingerprintRow(
                        name = "row",
                        entries = listOf(0 to Flt64.one),
                        rhs = Flt64.one,
                        id = ConstraintId("constraint:row")
                    )
                ),
                identityScopes = listOf(scope),
                identityOrigins = listOf(origin),
                identityProvenance = listOf(provenance)
            ).toNormalizedMathematicalModel()
        }

        val baseline = model(ModelElementScope.Stable, capacity, listOf(capacity)).fingerprint()
        val modelLocal = model(ModelElementScope.ModelLocal, capacity, listOf(capacity)).fingerprint()
        val changedOrigin = model(ModelElementScope.Stable, ModelElementOrigin("pipeline", "volume"), listOf(capacity)).fingerprint()
        val changedProvenance = model(
            ModelElementScope.Stable,
            capacity,
            listOf(capacity, ModelElementOrigin("source", "capacity"))
        ).fingerprint()

        assertNotEquals(baseline, modelLocal)
        assertNotEquals(baseline, changedOrigin)
        assertNotEquals(baseline, changedProvenance)
        assertNotEquals(changedOrigin, changedProvenance)
    }

    /**
     * 验证重复稀疏项被合并、零项被剔除，因此与等价单表达式同指纹。
     *
     * 这修正了此前的缺口：Kotlin 规范化原本只排序，`(0,1)+(0,2)+0y` 与 `(0,3)` 会得到不同
     * 指纹。现在与 Rust `canonical_sparse_entries` 语义一致——按变量合并后剔除零系数。
     *
     * Verifies that duplicate sparse terms are merged and zero terms dropped, so the model
     * fingerprints identically to its equivalent single-term form. This closes the earlier gap
     * where Kotlin only sorted, giving `(0,1)+(0,2)+0y` and `(0,3)` different fingerprints. It
     * now matches Rust's `canonical_sparse_entries`: merge per variable, then drop zero
     * coefficients.
     */
    @Test
    fun duplicateAndZeroSparseTermsAreConsolidatedInTheFingerprint() {
        val variables = listOf(
            variable(index = 0, name = "x", id = VariableId("variable:x")),
            variable(index = 1, name = "y", id = VariableId("variable:y"))
        )
        val consolidated = linearModel(
            name = "sparse-canonicalization",
            variables = variables,
            rows = listOf(
                FingerprintRow(
                    name = "row",
                    entries = listOf(0 to Flt64(3.0)),
                    rhs = Flt64(3.0),
                    id = ConstraintId("constraint:row")
                )
            )
        )
        val repeated = linearModel(
            name = "sparse-canonicalization",
            variables = variables,
            rows = listOf(
                FingerprintRow(
                    name = "row",
                    entries = listOf(0 to Flt64.one, 0 to Flt64(2.0), 1 to Flt64.zero),
                    rhs = Flt64(3.0),
                    id = ConstraintId("constraint:row")
                )
            )
        )

        assertEquals(
            consolidated.toNormalizedMathematicalModel().fingerprint(),
            repeated.toNormalizedMathematicalModel().fingerprint(),
            "合并重复项并剔除零项后，等价模型必须同指纹"
        )
    }

    /**
     * 验证稀疏项重排不改变指纹。 / Verifies that reordering sparse terms does not change the fingerprint.
     */
    @Test
    fun reorderedSparseTermsShareTheSameFingerprint() {
        val variables = listOf(
            variable(index = 0, name = "x", id = VariableId("variable:x")),
            variable(index = 1, name = "y", id = VariableId("variable:y"))
        )
        fun model(entries: List<Pair<Int, Flt64>>) = linearModel(
            name = "sparse-ordering",
            variables = variables,
            rows = listOf(
                FingerprintRow(
                    name = "row",
                    entries = entries,
                    rhs = Flt64(3.0),
                    id = ConstraintId("constraint:row")
                )
            )
        )

        assertEquals(
            model(listOf(0 to Flt64.one, 1 to Flt64(2.0))).toNormalizedMathematicalModel().fingerprint(),
            model(listOf(1 to Flt64(2.0), 0 to Flt64.one)).toNormalizedMathematicalModel().fingerprint()
        )
    }

    /**
     * 验证有符号零被归一化，因此与 `0.0` 同指纹。
     *
     * 这修正了此前的缺口：下界原本经 `toString()` 编码为 `-0.0`，与 `0.0` 产生不同指纹。
     * 现在与 Rust `append_f64` 一致，把 `-0.0` 归一为 `0.0`——数值相等的边界必须等价。
     *
     * Verifies that signed zero is normalized, so it fingerprints identically to `0.0`. This
     * closes the earlier gap where bounds encoded through `toString()` as `-0.0` differed from
     * `0.0`. It now matches Rust's `append_f64`, which normalizes `-0.0` to `0.0`: numerically
     * equal bounds must be equivalent.
     */
    @Test
    fun signedZeroBoundsShareTheSameFingerprint() {
        val rows = listOf(
            FingerprintRow(
                name = "row",
                entries = listOf(0 to Flt64.one),
                rhs = Flt64.one,
                id = ConstraintId("constraint:row")
            )
        )
        val positiveZero = linearModel(
            name = "signed-zero",
            variables = listOf(
                variable(index = 0, name = "x", id = VariableId("variable:x"), lowerBound = Flt64.zero)
            ),
            rows = rows
        )
        val negativeZero = linearModel(
            name = "signed-zero",
            variables = listOf(
                variable(index = 0, name = "x", id = VariableId("variable:x"), lowerBound = Flt64(-0.0))
            ),
            rows = rows
        )

        assertFalse(
            negativeZero.toNormalizedMathematicalModel().canonicalText().contains("-0.0"),
            "有符号零必须被归一化，规范文本中不得出现 -0.0"
        )
        assertEquals(
            positiveZero.toNormalizedMathematicalModel().fingerprint(),
            negativeZero.toNormalizedMathematicalModel().fingerprint(),
            "-0.0 与 0.0 必须同指纹"
        )
    }

    /**
     * 验证非有限模型值被拒绝，而不是被编码进指纹。
     *
     * 这修正了此前的缺口：`NaN` 原本会被编码为 `"NaN"` 文本并参与指纹，使脏数据看起来像一次
     * 正常求解。现在与 Rust `linear_model_fingerprint` 一致，返回结构化失败。
     *
     * Verifies that non-finite model values are rejected rather than fingerprinted. This closes
     * the earlier gap where `NaN` was encoded as the text `"NaN"` and contributed to the
     * fingerprint, making corrupt data look like an ordinary run. It now matches Rust's
     * `linear_model_fingerprint` by returning a structured failure.
     */
    @Test
    fun nonFiniteObjectiveValuesAreRejected() {
        val variables = listOf(variable(index = 0, name = "x", id = VariableId("variable:x")))
        val rows = listOf(
            FingerprintRow(
                name = "row",
                entries = listOf(0 to Flt64.one),
                rhs = Flt64.one,
                id = ConstraintId("constraint:row")
            )
        )
        val finite = linearModel(
            name = "non-finite",
            variables = variables,
            rows = rows,
            objectiveTerms = listOf(0 to Flt64.one)
        )
        val nonFinite = linearModel(
            name = "non-finite",
            variables = variables,
            rows = rows,
            objectiveTerms = listOf(0 to Flt64.nan)
        )

        assertTrue(
            finite.toNormalizedMathematicalModel().validateFiniteValues() is Ok,
            "有限模型必须通过校验"
        )

        val result = nonFinite.toNormalizedMathematicalModel().validateFiniteValues()
        assertTrue(result is Failed, "非有限目标系数必须被拒绝，而不是进入指纹")
        assertEquals(ErrorCode.IllegalArgument, (result as Failed).error.code)
        assertTrue(
            result.error.message.contains("非有限") || result.error.message.contains("non-finite")
        )
    }

    /**
     * 验证无穷边界按方向被接受，而 NaN 与方向错误的无穷被拒绝。
     *
     * 无穷表示"该侧无约束"，是合法建模手段（Rust `finite_bound` 同样接受 `-∞` 下界与 `+∞`
     * 上界）；因此不能把它当作脏数据一并拒绝。真正非法的是 NaN，以及 `+∞` 下界 / `-∞` 上界
     * 这类方向错误的边界——它们会使模型在数学上无意义。
     *
     * Verifies that infinite bounds are accepted when correctly directed, while NaN and
     * wrongly directed infinities are rejected. Infinity means "unbounded on that side" and is a
     * legitimate modeling device (Rust's `finite_bound` likewise accepts a `-∞` lower bound and
     * a `+∞` upper bound), so it must not be lumped in with corrupt data. What is genuinely
     * invalid is NaN and misdirected bounds such as a `+∞` lower bound or `-∞` upper bound,
     * which make the model mathematically meaningless.
     */
    @Test
    fun infiniteBoundsAreAcceptedButNanAndMisdirectedInfinityAreRejected() {
        fun modelWithBounds(lower: Flt64, upper: Flt64) = linearModel(
            name = "bounds",
            variables = listOf(
                variable(
                    index = 0,
                    name = "x",
                    id = VariableId("variable:x"),
                    lowerBound = lower,
                    upperBound = upper
                )
            ),
            rows = listOf(
                FingerprintRow(
                    name = "row",
                    entries = listOf(0 to Flt64.one),
                    rhs = Flt64.one,
                    id = ConstraintId("constraint:row")
                )
            )
        )

        // 合法：无下界 / 无上界 / 双侧有界。
        // Legitimate: no lower bound, no upper bound, both sides bounded.
        assertTrue(
            modelWithBounds(-Flt64.infinity, Flt64.infinity)
                .toNormalizedMathematicalModel().validateFiniteValues() is Ok,
            "无穷边界是合法的无约束表示"
        )
        assertTrue(
            modelWithBounds(Flt64.zero, Flt64.infinity)
                .toNormalizedMathematicalModel().validateFiniteValues() is Ok
        )
        assertTrue(
            modelWithBounds(Flt64.zero, Flt64.one)
                .toNormalizedMathematicalModel().validateFiniteValues() is Ok
        )

        // 非法：NaN 边界 / 方向错误的无穷边界。
        // Invalid: NaN bound, and misdirected infinity.
        val nanBound = modelWithBounds(Flt64.nan, Flt64.one)
            .toNormalizedMathematicalModel().validateFiniteValues()
        assertTrue(nanBound is Failed, "NaN 边界必须被拒绝")
        assertEquals(ErrorCode.IllegalArgument, (nanBound as Failed).error.code)

        val wrongLower = modelWithBounds(Flt64.infinity, Flt64.infinity)
            .toNormalizedMathematicalModel().validateFiniteValues()
        assertTrue(wrongLower is Failed, "+∞ 下界方向错误，必须被拒绝")
        assertEquals(ErrorCode.IllegalArgument, (wrongLower as Failed).error.code)

        val wrongUpper = modelWithBounds(-Flt64.infinity, -Flt64.infinity)
            .toNormalizedMathematicalModel().validateFiniteValues()
        assertTrue(wrongUpper is Failed, "-∞ 上界方向错误，必须被拒绝")
        assertEquals(ErrorCode.IllegalArgument, (wrongUpper as Failed).error.code)
    }

    /**
     * 验证非有限约束右端项与系数被拒绝。 / Verifies that non-finite constraint sides and coefficients are rejected.
     */
    @Test
    fun nonFiniteConstraintValuesAreRejected() {
        val variables = listOf(variable(index = 0, name = "x", id = VariableId("variable:x")))

        val nonFiniteRhs = linearModel(
            name = "non-finite-rhs",
            variables = variables,
            rows = listOf(
                FingerprintRow(
                    name = "row",
                    entries = listOf(0 to Flt64.one),
                    rhs = Flt64.nan,
                    id = ConstraintId("constraint:row")
                )
            )
        )
        val rhsResult = nonFiniteRhs.toNormalizedMathematicalModel().validateFiniteValues()
        assertTrue(rhsResult is Failed, "非有限右端项必须被拒绝")
        assertEquals(ErrorCode.IllegalArgument, (rhsResult as Failed).error.code)

        val nonFiniteCoefficient = linearModel(
            name = "non-finite-coefficient",
            variables = variables,
            rows = listOf(
                FingerprintRow(
                    name = "row",
                    entries = listOf(0 to Flt64.nan),
                    rhs = Flt64.one,
                    id = ConstraintId("constraint:row")
                )
            )
        )
        val coefficientResult = nonFiniteCoefficient.toNormalizedMathematicalModel().validateFiniteValues()
        assertTrue(coefficientResult is Failed, "非有限系数必须被拒绝")
        assertEquals(ErrorCode.IllegalArgument, (coefficientResult as Failed).error.code)
    }

    /**
     * 验证约束分组与参数参与指纹，且参数的文本是**确定性**的。
     *
     * Rust 的 `linear_model_fingerprint` 纳入 `constraint_group_ids` 与 `constraint_args`；Kotlin
     * 此前两者都不参与，因此"把约束移到另一个分组"或"换掉约束参数"不会改变指纹。
     *
     * 参数部分还固定了一条更重要的性质：`args` 是 `Any?`，若直接 `toString()`，未覆写该方法的类型会
     * 落到 `Object.toString()`，其身份哈希每次运行都不同——那样的指纹是不稳定的，比不纳入更糟。
     * 因此本用例断言：结构化 `data class` 参数稳定可辨，而**无稳定表示**的参数只以类名参与，
     * 两次构造同一模型必须得到同一指纹。
     *
     * Verifies that constraint group and args participate in the fingerprint, and that the args text is
     * **deterministic**. Rust's `linear_model_fingerprint` includes `constraint_group_ids` and
     * `constraint_args`, whereas Kotlin previously omitted both, so moving a constraint to another group
     * or swapping its arguments did not change the fingerprint.
     *
     * The args half also pins a more important property: `args` is `Any?`, and a bare `toString()` falls
     * back to `Object.toString()` for types that do not override it, whose identity hash differs on every
     * run — such a fingerprint would be unstable, which is worse than omitting it. This test therefore
     * asserts that a structured `data class` argument stays stable and distinguishable, while an argument
     * with **no stable representation** contributes only its class name, and that building the same model
     * twice yields the same fingerprint.
     */
    @Test
    fun modelFingerprintIncludesConstraintGroupAndStableArgs() {
        val variables = listOf(variable(index = 0, name = "x", id = VariableId("variable:x")))
        val rows = listOf(
            FingerprintRow(
                name = "row",
                entries = listOf(0 to Flt64.one),
                rhs = Flt64.one,
                id = ConstraintId("constraint:row")
            )
        )

        fun fingerprint(group: String?, args: Any?): String = linearModel(
            name = "constraint-group-args",
            variables = variables,
            rows = rows,
            constraintGroups = listOf(group),
            constraintArgs = listOf(args)
        ).toNormalizedMathematicalModel().fingerprint().value

        // 分组参与指纹。 / The group participates in the fingerprint.
        assertNotEquals(
            fingerprint("capacity", null),
            fingerprint("demand", null),
            "约束分组必须参与指纹"
        )
        assertNotEquals(
            fingerprint("capacity", null),
            fingerprint(null, null),
            "缺失分组必须与有值不同"
        )

        // 结构化参数稳定且可辨。 / A structured argument is stable and distinguishable.
        assertEquals(
            fingerprint("capacity", StableArgs("a")),
            fingerprint("capacity", StableArgs("a")),
            "相同结构化参数必须稳定同指纹"
        )
        assertNotEquals(
            fingerprint("capacity", StableArgs("a")),
            fingerprint("capacity", StableArgs("b")),
            "不同结构化参数必须区分"
        )

        // 无稳定表示的参数只以类名参与：同一模型两次构造必须同指纹（不得引入身份哈希）。
        // An argument with no stable representation contributes only its class name: building the same
        // model twice must yield the same fingerprint (no identity hash may leak in).
        assertEquals(
            fingerprint("capacity", UnstableArgs()),
            fingerprint("capacity", UnstableArgs()),
            "无稳定表示的参数不得让指纹在两次构造间漂移"
        )
        assertNotEquals(
            fingerprint("capacity", UnstableArgs()),
            fingerprint("capacity", null),
            "带参数必须与不带参数不同"
        )

        // 规范化后的元数据必须真的被读取到。 / The normalized metadata must actually be populated.
        val normalized = linearModel(
            name = "constraint-group-args",
            variables = variables,
            rows = rows,
            constraintGroups = listOf("capacity"),
            constraintArgs = listOf(StableArgs("a"))
        ).toNormalizedMathematicalModel()
        assertEquals("capacity", normalized.constraints.single().group)
        assertEquals("StableArgs(label=a)", normalized.constraints.single().args)
    }

    /** 具备稳定结构化表示的参数。 / Argument with a stable structural representation. */
    private data class StableArgs(val label: String)

    /** **没有**稳定表示的参数：依赖 `Object.toString()` 的身份哈希。 / Argument with **no** stable representation. */
    private class UnstableArgs

    /** 最小约束分组替身。 / Minimal constraint-group stand-in. */
    private class TestConstraintGroup(override val name: String) : MetaConstraintGroup

    /**
     * 最小数学约束替身，仅承载分组与参数。 / Minimal math-constraint stand-in carrying only group and args.
     *
     * 指纹只读取 `group` / `args` / `lazy` / `priority`，不调用 `isTrue`，因此求值返回 null 即可。
     * The fingerprint reads only `group` / `args` / `lazy` / `priority` and never calls `isTrue`, so
     * evaluation may return null.
     */
    private class TestMathConstraint(
        override val group: MetaConstraintGroup? = null,
        override val args: Any? = null,
        override val lazy: Boolean = false,
        override val priority: Int? = null
    ) : MathConstraint {
        override fun <V> isTrue(
            solution: List<V>,
            tokenTable: AbstractTokenTable<V>,
            zeroIfNone: Boolean
        ): Boolean? where V : RealNumber<V>, V : NumberField<V> = null
    }

    /**
     * 构造用于指纹边界的最小线性模型。 / Build a minimal linear model for fingerprint boundaries.
     *
     * @param name 模型名称（参与指纹）/ Model name; it enters the fingerprint
     * @param variables 求解器索引的变量列表 / Solver-indexed variable list
     * @param rows 约束行 / Constraint rows
     * @param objectiveTerms 目标线性项 / Objective linear terms
     * @param identityScopes 每行身份作用域 / Per-row identity scopes
     * @param identityOrigins 每行身份来源 / Per-row identity origins
     * @param identityProvenance 每行完整身份来源 / Per-row complete identity provenance
     * @return 线性三元模型 / Linear triad model
     */
    private fun linearModel(
        name: String,
        variables: List<Variable>,
        rows: List<FingerprintRow>,
        objectiveTerms: List<Pair<Int, Flt64>> = emptyList(),
        identityScopes: List<ModelElementScope> = emptyList(),
        identityOrigins: List<ModelElementOrigin?> = emptyList(),
        identityProvenance: List<List<ModelElementOrigin>> = emptyList(),
        priorities: List<Int?> = emptyList(),
        lazies: List<Boolean> = emptyList(),
        constraintGroups: List<String?> = emptyList(),
        constraintArgs: List<Any?> = emptyList()
    ): LinearTriadModel {
        val matrix = SparseMatrix<Flt64>()
        rows.forEach { row ->
            matrix.addRow(SparseVector<Flt64>().also { vector ->
                row.entries.forEach { (column, coefficient) -> vector.add(column, coefficient) }
            })
        }
        val rowIds = rows.mapNotNull { it.id }
        // 只要任一行的 lazy / 分组 / 参数被指定，就为**所有**行构造带 origin 的替身，保持下标对齐。
        // As soon as any row specifies a lazy flag, group, or args, build origin-carrying stand-ins for
        // **every** row so the indices stay aligned.
        val needsOrigins = lazies.isNotEmpty() || constraintGroups.isNotEmpty() || constraintArgs.isNotEmpty()
        val origins = if (!needsOrigins) {
            emptyList()
        } else {
            rows.indices.map { index ->
                LinearConstraintImpl<Flt64>(
                    lhs = emptyList(),
                    sign = ModelConstraintRelation.Equal,
                    rhs = rows[index].rhs,
                    lazy = lazies.getOrNull(index) ?: false,
                    name = rows[index].name,
                    origin = TestMathConstraint(
                        group = constraintGroups.getOrNull(index)?.let { TestConstraintGroup(it) },
                        args = constraintArgs.getOrNull(index)
                    )
                )
            }
        }
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = variables,
                constraints = LinearConstraintBatch(
                    sparseLhs = matrix,
                    signs = rows.map { ModelConstraintRelation.Equal },
                    rhs = rows.map { it.rhs },
                    names = rows.map { it.name },
                    sources = rows.map { it.source },
                    origins = if (origins.isEmpty()) {
                        (0 until rows.size).map { null }
                    } else {
                        origins
                    },
                    priorities = if (priorities.isEmpty()) {
                        (0 until rows.size).map { null }
                    } else {
                        priorities
                    },
                    // 只有每一行都带稳定 ID 时才注册稳定身份，否则保持 model-local 回退。
                    // Stable identities are registered only when every row carries one.
                    ids = rowIds.takeIf { it.size == rows.size }.orEmpty(),
                    identityScopes = identityScopes,
                    identityOrigins = identityOrigins,
                    identityProvenance = identityProvenance
                ),
                name = name
            ),
            tokensInSolver = emptyList(),
            objective = Objective(
                category = ObjectCategory.Minimum,
                objective = objectiveTerms.map { (column, coefficient) -> LinearObjectiveCell(column, coefficient) },
                constant = Flt64.zero
            )
        )
    }

    /**
     * 构造一个求解器边界变量。 / Build a solver-boundary variable.
     *
     * @param index 求解器列索引 / Solver column index
     * @param name 变量名 / Variable name
     * @param id 可选稳定标识；省略时保持 model-local / Optional stable identifier; omitted means model-local
     * @param lowerBound 下界 / Lower bound
     * @param upperBound 上界 / Upper bound
     * @return 求解器边界变量 / Solver-boundary variable
     */
    private fun variable(
        index: Int,
        name: String,
        id: VariableId? = null,
        lowerBound: Flt64 = Flt64.zero,
        upperBound: Flt64 = Flt64.one
    ): Variable {
        return Variable(
            index = index,
            lowerBound = lowerBound,
            upperBound = upperBound,
            type = Continuous,
            origin = null,
            name = name,
            id = id
        )
    }

    /**
     * 指纹测试使用的约束行。 / Constraint row used by the fingerprint tests.
     *
     * @property name 约束名（不参与 Kotlin 指纹）/ Constraint name; it does not enter the Kotlin fingerprint
     * @property entries 列索引与系数对 / Column-index and coefficient pairs
     * @property rhs 右端项 / Right-hand side
     * @property id 可选稳定约束标识 / Optional stable constraint identifier
     */
    private class FingerprintRow(
        val name: String,
        val entries: List<Pair<Int, Flt64>>,
        val rhs: Flt64,
        val id: ConstraintId? = null,
        val source: ConstraintSource = ConstraintSource.Origin
    )
}
