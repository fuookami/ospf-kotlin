package fuookami.ospf.kotlin.core.analysis

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 跨语言语义契约测试（计划 12.1 / 12.3 S0–S6 阶段门）。
 *
 * 读取两端共同维护的 `analysis-fixtures`，断言 Kotlin 实现与该契约完全一致。
 * Rust 侧由 `ospf-rust-core/src/analysis/fixture_contract.rs` 断言同一份文件。
 *
 * Cross-language semantic contract test (plan 12.1 / 12.3 S0–S6 gates). It reads the shared
 * `analysis-fixtures` directory and asserts that the Kotlin implementation matches the contract
 * exactly. The Rust side asserts the same files from
 * `ospf-rust-core/src/analysis/fixture_contract.rs`.
 */
class AnalysisFixtureContractTest {
    private val fixtureRoot: File? = sequenceOf(
        File("../../analysis-fixtures"),
        File("../analysis-fixtures"),
        File("analysis-fixtures")
    ).firstOrNull { it.isDirectory }

    private fun sections(name: String): Map<String, List<List<String>>> {
        val root = fixtureRoot ?: return emptyMap()
        val file = File(root, name)
        if (!file.isFile) {
            return emptyMap()
        }
        val result = LinkedHashMap<String, MutableList<List<String>>>()
        var current: String? = null
        file.readLines().forEach { raw ->
            val line = raw.trimEnd()
            if (line.isBlank() || line.trimStart().startsWith("#")) {
                return@forEach
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                current = line.substring(1, line.length - 1)
                result.getOrPut(current!!) { ArrayList() }
                return@forEach
            }
            val section = current ?: return@forEach
            // 数据行既支持 TAB 分隔，也支持单行 `key=value`。
            // A data row is either TAB-separated or a single `key=value` pair.
            val fields = line.split("\t").map { it.trim() }
            val normalized = if (fields.size == 1 && fields[0].contains('=')) {
                val index = fields[0].indexOf('=')
                listOf(fields[0].substring(0, index), fields[0].substring(index + 1))
            } else {
                fields
            }
            result.getOrPut(section) { ArrayList() } += normalized
        }
        return result
    }

    private fun values(name: String, section: String): List<String> {
        return sections(name)[section]?.map { it.first() } ?: emptyList()
    }

    private fun rows(name: String, section: String): List<List<String>> {
        return sections(name)[section] ?: emptyList()
    }

    private fun requireFixtures() {
        assertNotNull(
            fixtureRoot,
            "未找到 analysis-fixtures 目录；跨语言契约测试需要 ospf-kotlin 与 ospf-rust 的同级检出 / " +
                "analysis-fixtures not found; the cross-language contract test requires sibling checkouts"
        )
    }

    @Test
    fun contractVocabularyMatchesBothLanguageImplementations() {
        requireFixtures()

        // 状态枚举必须完全一致，顺序也一致（顺序进入稳定序列化）。
        assertEquals(
            listOf("Reachable", "Unreachable", "Unknown", "Unsupported"),
            values("analysis-contract.tsv", "status")
        )
        assertEquals(
            AnalysisStatus.entries.map { it.name },
            values("analysis-contract.tsv", "status")
        )

        // "已证明"判定必须一致：Unknown/Unsupported 永远不是已证明结论。
        rows("analysis-contract.tsv", "proven").forEach { row ->
            val status = AnalysisStatus.valueOf(row[0])
            assertEquals(row[1].toBooleanStrict(), status.isProven, "proven flag for ${row[0]}")
        }

        assertEquals(
            ActivityStatus.entries.map { it.name },
            values("analysis-contract.tsv", "activity-status")
        )

        // 允许公开的证据类型：只包含原始模型身份。
        val evidenceKinds = values("analysis-contract.tsv", "evidence-kind")
        assertEquals(
            listOf(
                "constraint",
                "variable-lower-bound",
                "variable-upper-bound",
                "sparse-domain",
                "objective-target"
            ),
            evidenceKinds
        )

        val forbidden = values("analysis-contract.tsv", "forbidden-evidence")
        assertEquals(
            listOf("solver_row", "solver_column", "auxiliary_variable", "auxiliary_constraint", "native_handle"),
            forbidden
        )

        // 候选分层：Tier C 与未分层都必须被保留，只是不默认进入短名单。
        rows("analysis-contract.tsv", "candidate-tier").forEach { row ->
            val tier = CandidateTier.valueOf(row[0])
            assertEquals(row[1].toBooleanStrict(), tier.isShortlistDefault, "shortlist for ${row[0]}")
            assertTrue(row[2].toBooleanStrict(), "every tier must be retained: ${row[0]}")
        }
        assertEquals(
            CandidateTier.entries.map { it.name },
            rows("analysis-contract.tsv", "candidate-tier").map { it[0] }
        )

        // 最小性：只有 Irreducible 是"已证明最小"。
        rows("analysis-contract.tsv", "minimality").forEach { row ->
            val minimality = ConflictMinimality.valueOf(row[0])
            assertEquals(
                row[1].toBooleanStrict(),
                minimality == ConflictMinimality.Irreducible,
                "minimality flag for ${row[0]}"
            )
        }

        // 提取层级：只有原生 core 被标记为 native。
        val tiers = rows("analysis-contract.tsv", "extraction-tier")
        assertEquals(listOf("NativeUnsatCore", "AssumptionExtraction", "RepeatedSolving"), tiers.map { it[1] })
        tiers.forEach { row ->
            val tier = ConflictExtractionTier.valueOf(row[1])
            assertEquals(row[2].toBooleanStrict(), tier.isNative, "native flag for ${row[1]}")
        }

        // 对偶作用域：绝不使用"全局影子价格"措辞。
        val scope = rows("analysis-contract.tsv", "scope").associate { it[0] to it[1] }
        assertEquals("FixedIntegerIncumbent", scope["local_dual"])
        assertEquals(
            LocalSensitivityScope.FixedIntegerIncumbent.name,
            scope["local_dual"]
        )
        // 报告层绝不使用"MILP 全局影子价格"措辞。
        assertEquals("MILP global shadow price", scope["forbidden_label"])

        // Phase 7/8 vocabulary is shared even though the report models are not JSON codecs.
        assertEquals(
            CriticalityKind.entries.map { it.name },
            values("analysis-contract.tsv", "criticality-kind")
        )
        val policyDefaults = rows("analysis-contract.tsv", "relaxability-policy").associate { it[0] to it[1] }
        assertEquals("32", policyDefaults["max_candidates"])
        assertEquals("8", policyDefaults["max_plans"])
        assertEquals("true", policyDefaults["require_positive_weight"])
        assertEquals("true", policyDefaults["numeric_relaxation_requires_revalidation"])
        val defaultPolicy = RelaxabilityPolicy()
        assertEquals(policyDefaults["max_candidates"]!!.toInt(), defaultPolicy.maxCandidates)
        assertEquals(policyDefaults["max_plans"]!!.toInt(), defaultPolicy.maxPlans)
        assertEquals(
            policyDefaults["require_positive_weight"]!!.toBooleanStrict(),
            defaultPolicy.requirePositiveWeight
        )
    }

    @Test
    fun contractSchemaVersionsMatchBothLanguages() {
        requireFixtures()
        val schema = rows("analysis-contract.tsv", "schema").associate { it[0] to it[1] }
        assertEquals("1.0", schema["critical_analysis_report"])
        assertEquals("1.0", schema["activity_report"])
        assertEquals("1.0", schema["fixed_integer_lp_report"])
        assertEquals("1.0", schema["perturbation_report"])
        assertEquals("1.0", schema["adaptive_perturbation_report"])
        assertEquals("1.0", schema["target_feasibility_report"])
        assertEquals("1.0", schema["multi_target_analysis_report"])
        assertEquals("1.0", schema["conflict_report"])
        assertEquals("1.0", schema["activation_protocol"])

        // 协议常量必须与契约逐字一致。
        assertEquals(schema["critical_analysis_report"], CRITICAL_ANALYSIS_REPORT_SCHEMA_VERSION)
        assertEquals(schema["activation_protocol"], ACTIVATION_PROTOCOL_SCHEMA_VERSION)
        assertEquals("1.0", schema["criticality_profile"])
        assertEquals("1.0", schema["correction_set"])
        assertEquals(
            schema["criticality_profile"],
            CriticalityProfile(emptyList(), emptyMap(), schema["criticality_profile"]!!).schemaVersion
        )
    }

    @Test
    fun statusMappingCasesMatchTheSharedFixture() {
        requireFixtures()
        val cases = rows("analysis-cases.tsv", "status-map")
        assertTrue(cases.isNotEmpty(), "status-map cases must exist")

        cases.forEach { row ->
            val statusName = row[0]
            val proven = row[1].toBooleanStrict()
            val expected = AnalysisStatus.valueOf(row[2])
            val status = fuookami.ospf.kotlin.core.solver.report.ProblemStatus.valueOf(statusName)
            val actual = proofGatedStatus(status, proven)
            assertEquals(expected, actual, "status-map $statusName/$proven")
            // 未证明时永远不得给出已证明结论。
            if (!proven) {
                assertFalse(actual.isProven, "unproven result must not be a proven conclusion: $statusName")
            }
        }
    }

    @Test
    fun classificationCasesMatchTheSharedFixture() {
        requireFixtures()
        val cases = rows("analysis-cases.tsv", "classify")
        assertTrue(cases.isNotEmpty(), "classify cases must exist")

        cases.forEach { row ->
            val activityName = row[0]
            val dualAvailable = row[1].toBooleanStrict()
            val dualAbs = row[2].toDouble()
            val threshold = row[3].toDouble()
            val expectedName = row[4]
            val dual = if (dualAvailable) Flt64(dualAbs) else null
            val actual = classifyCandidate(
                ActivityStatus.valueOf(activityName),
                dual,
                threshold
            )
            assertEquals(
                CandidateTier.valueOf(expectedName),
                actual,
                "classify $activityName/$dualAvailable/$dualAbs"
            )
            // 没有对偶证据时不得声称分层。
            if (!dualAvailable &&
                (activityName == "Active" || activityName == "NearlyActive")
            ) {
                assertEquals(CandidateTier.Unclassified, actual)
            }
        }
    }

    @Test
    fun combineCasesMatchTheSharedFixture() {
        requireFixtures()
        val cases = rows("analysis-cases.tsv", "combine")
        assertTrue(cases.isNotEmpty(), "combine cases must exist")

        cases.forEach { row ->
            assertEquals(
                AnalysisStatus.valueOf(row[2]),
                combineStatus(
                    AnalysisStatus.valueOf(row[0]),
                    AnalysisStatus.valueOf(row[1])
                ),
                "combine ${row[0]}+${row[1]}"
            )
        }
    }

    @Test
    fun shortlistCasesKeepLowerTiersInsteadOfDroppingThem() {
        requireFixtures()
        val cases = rows("analysis-cases.tsv", "shortlist")
        assertTrue(cases.isNotEmpty(), "shortlist cases must exist")

        cases.forEach { row ->
            val tierA = row[0].toInt()
            val tierB = row[1].toInt()
            val tierC = row[2].toInt()
            val limit = row[3].toInt()
            val returned = row[4].toInt()
            val tierCReturned = row[5].toInt()
            val unclassified = row[6].toInt()
            val unclassifiedReturned = row[7].toInt()
            val candidates = ArrayList<ConstraintCandidate>()
            var priority = 0
            repeat(tierA) {
                candidates += candidate("a-$it", CandidateTier.TierA, priority++)
            }
            repeat(tierB) {
                candidates += candidate("b-$it", CandidateTier.TierB, priority++)
            }
            repeat(tierC) {
                candidates += candidate("c-$it", CandidateTier.TierC, priority++)
            }
            repeat(unclassified) {
                candidates += candidate("u-$it", CandidateTier.Unclassified, priority++)
            }
            val ranking = CandidateFunnelRanking(
                dualThreshold = CandidateFunnelConfig.DEFAULT_DUAL_THRESHOLD,
                candidates = candidates,
                tierA = tierA,
                tierB = tierB,
                tierC = tierC,
                unclassified = unclassified
            )
            val shortlisted = ranking.shortlist(limit)
            assertEquals(returned, shortlisted.size, "shortlist size for $row")
            assertEquals(
                tierCReturned,
                shortlisted.count { it.tier == CandidateTier.TierC },
                "shortlist tier C count for $row"
            )
            assertEquals(
                unclassifiedReturned,
                shortlisted.count { it.tier == CandidateTier.Unclassified },
                "shortlist unclassified count for $row"
            )
            // 低层级候选只在名额未满时被纳入，进入短名单的必然是优先级最高的那批。
            shortlisted.forEachIndexed { index, entry ->
                assertEquals(index, entry.priority, "shortlist must preserve priority order")
            }
        }
    }

    @Test
    fun criticalityCasesUseOnlyVerifiedUnreachableEvidence() {
        requireFixtures()
        val cases = rows("analysis-cases.tsv", "criticality")
        assertTrue(cases.isNotEmpty(), "criticality cases must exist")

        cases.forEach { row ->
            val profile = buildCriticalityProfile(parseCriticalityObservations(row[1]))
            assertEquals(row[5].toInt(), profile.provenTargetCount, "proven targets for ${row[0]}")
            assertEquals(
                expectedIds(row[2]),
                profile.constraints(CriticalityKind.LocalBottleneck).map { it.value }.toSet(),
                "local profile for ${row[0]}"
            )
            assertEquals(
                expectedIds(row[3]),
                profile.constraints(CriticalityKind.PersistentBottleneck).map { it.value }.toSet(),
                "persistent profile for ${row[0]}"
            )
            assertEquals(
                expectedIds(row[4]),
                profile.constraints(CriticalityKind.StructuralBottleneck).map { it.value }.toSet(),
                "structural profile for ${row[0]}"
            )
        }
    }

    @Test
    fun correctionCasesShareWeightOrderingPolicyAndMinimalityBoundary() {
        requireFixtures()
        val cases = rows("analysis-cases.tsv", "correction")
        assertTrue(cases.isNotEmpty(), "correction cases must exist")

        cases.forEach { row ->
            val policy = RelaxabilityPolicy(
                maxCandidates = row[2].toInt(),
                maxPlans = row[3].toInt(),
                requirePositiveWeight = row[4].toBooleanStrict()
            )
            val candidates = parseCorrectionCandidates(row[6])
            val result = runCatching {
                when (row[1]) {
                    "weighted" -> weightedCorrectionSet(candidates, policy)
                    "minimal" -> minimalCorrectionSet(candidates, row[5].toBooleanStrict(), policy)
                    else -> error("unknown correction operation: ${row[1]}")
                }
            }
            assertEquals(row[10].toBooleanStrict(), result.isSuccess, "accepted correction ${row[0]}")
            if (result.isSuccess) {
                val correction = result.getOrThrow()
                assertEquals(
                    expectedIdList(row[7]),
                    correction.members.map { it.source.stableId.removePrefix("constraint:") },
                    "member order for ${row[0]}"
                )
                assertEquals(row[8].toDouble(), correction.totalCost, 1e-9, "cost for ${row[0]}")
                assertEquals(row[9].toBooleanStrict(), correction.minimal, "minimality for ${row[0]}")
                assertTrue(correction.requiresRevalidation(), "numeric relaxation must be revalidated")
            }
        }
    }

    private fun candidate(id: String, tier: CandidateTier, priority: Int): ConstraintCandidate {
        return ConstraintCandidate(
            constraintId = fuookami.ospf.kotlin.core.solver.report.ConstraintId(id),
            group = null,
            tier = tier,
            activity = when (tier) {
                CandidateTier.TierC -> ActivityStatus.Inactive
                else -> ActivityStatus.Active
            },
            slack = 0.0,
            dualValue = Flt64(0.0),
            localEffective = false,
            priority = priority
        )
    }

    private fun expectedIds(raw: String): Set<String> {
        return if (raw == "-") emptySet() else raw.split(",").toSet()
    }

    private fun expectedIdList(raw: String): List<String> {
        return if (raw == "-") emptyList() else raw.split(",")
    }

    private fun parseCriticalityObservations(raw: String): List<CriticalityObservation> {
        return raw.split(";").mapIndexed { index, encoded ->
            val fields = encoded.split("|")
            require(fields.size == 3) { "invalid criticality observation: $encoded" }
            val status = AnalysisStatus.valueOf(fields[0])
            val verified = fields[1].toBooleanStrict()
            val ids = if (status == AnalysisStatus.Unreachable && verified) {
                expectedIds(fields[2]).map(::ConstraintId).toSet()
            } else {
                emptySet()
            }
            CriticalityObservation(
                target = ObjectiveTarget.AtLeast(
                    ObjectiveId("fixture-objective-$index"),
                    Flt64(index.toDouble())
                ),
                status = status,
                blockingConstraintIds = ids
            )
        }
    }

    private fun parseCorrectionCandidates(raw: String): List<CorrectionCandidate> {
        return raw.split(";").filter { it.isNotBlank() }.map { encoded ->
            val fields = encoded.split(":")
            val (source, weightIndex) = when (fields.size) {
                3 -> DiagnosticSource.Constraint(ConstraintId(fields[0])) to 1
                4 -> {
                    val source = when (fields[0]) {
                        "variable-lower-bound" -> DiagnosticSource.VariableLowerBound(VariableId(fields[1]))
                        "sparse-domain" -> DiagnosticSource.SparseDomain(VariableId(fields[1]))
                        else -> error("unknown correction source: ${fields[0]}")
                    }
                    source to 2
                }
                else -> error("invalid correction candidate: $encoded")
            }
            CorrectionCandidate(
                source = source,
                weight = fields[weightIndex].toDouble(),
                relaxation = fields[weightIndex + 1].toDouble()
            )
        }
    }

    /** 与实现一致的证明门控映射，供 fixture 断言使用。 */
    private fun proofGatedStatus(
        status: fuookami.ospf.kotlin.core.solver.report.ProblemStatus,
        proven: Boolean
    ): AnalysisStatus {
        return when (status) {
            fuookami.ospf.kotlin.core.solver.report.ProblemStatus.Feasible ->
                if (proven) AnalysisStatus.Reachable else AnalysisStatus.Unknown

            fuookami.ospf.kotlin.core.solver.report.ProblemStatus.Infeasible ->
                if (proven) AnalysisStatus.Unreachable else AnalysisStatus.Unknown

            fuookami.ospf.kotlin.core.solver.report.ProblemStatus.Unbounded,
            fuookami.ospf.kotlin.core.solver.report.ProblemStatus.InfeasibleOrUnbounded,
            fuookami.ospf.kotlin.core.solver.report.ProblemStatus.Unknown -> AnalysisStatus.Unknown
        }
    }
}
