package fuookami.ospf.kotlin.framework.solver.remote.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointCodec
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointEnvelope

/**
 * 远端 side 的 checkpoint 线格式契约测试。 / Remote-side checkpoint wire-contract test.
 *
 * 收敛之后，framework 的 `PortableCheckpointEnvelope` 只是 core envelope 的 `typealias`，
 * `PortableCheckpointCodec` 也只是转发。本用例在**框架侧**断言同一份契约，从而覆盖"两套 codec"：
 * 如果框架侧再分叉出第二份字段列表、摘要或父链校验，这里的字段序列或字节一致性断言就会失败。
 *
 * After convergence, the framework's `PortableCheckpointEnvelope` is only a `typealias` of core's
 * envelope and `PortableCheckpointCodec` merely forwards. This test asserts the same contract from the
 * **framework side**, which covers both former codecs: if a second field list, digest, or parent-link
 * validation ever forks off here, the field-sequence or byte-identity assertion fails.
 */
class RemoteCheckpointWireContractTest {
    /**
     * 定位两端共同维护的 `analysis-fixtures`。
     *
     * 候选顺序在 core 的 `CheckpointWireContractTest` 基础上增加一项：core 模块的仓内镜像。
     * 这样 framework 模块不需要再存第三份契约副本，也不会因为单仓库检出而静默通过。
     *
     * Locate the shared `analysis-fixtures`. The candidate list extends core's
     * `CheckpointWireContractTest` with one entry — core's in-repo mirror — so the framework module
     * needs no third copy of the contract and does not pass silently in a single-repository checkout.
     */
    private val fixtureRoot: File? = sequenceOf(
        File("../../analysis-fixtures"),
        File("../analysis-fixtures"),
        File("analysis-fixtures"),
        File("../ospf-kotlin-core/src/test/resources/analysis-fixtures"),
        File("src/test/resources/analysis-fixtures")
    ).firstOrNull { it.isDirectory }

    @Test
    fun frameworkEnvelopeEncodingMatchesTheContractFieldSequence() {
        requireFixtures()
        val expected = rows("envelope-field").map { it[1] }
        assertEquals(
            25,
            expected.size,
            "契约必须声明 25 个 envelope 字段 / the contract must declare 25 envelope fields"
        )
        assertEquals("cancellationChain", expected[21], "第 21 位必须是 cancellationChain")
        assertEquals("provenance", expected[22], "第 22 位必须是 provenance")
        assertEquals("metadata", expected[23], "第 23 位必须是 metadata")

        val actual = Json.parseToJsonElement(PortableCheckpointCodec.encode(envelope())).jsonObject.keys.toList()

        assertEquals(
            expected,
            actual,
            "框架侧编码的顶层 key 序列必须与契约 [envelope-field] 逐位相同 / " +
                "the framework-side encoding's top-level key sequence must match the contract exactly"
        )
    }

    @Test
    fun frameworkCodecProducesTheSameDocumentAsTheCoreCodec() {
        // 最强的一致性断言：框架入口与 core 入口必须产出**逐字节相同**的文档，否则说明框架侧
        // 又在摘要或字段顺序上做了一次自己的决定。
        // The strongest consistency assertion: the framework entry point and the core entry point must
        // produce **byte-identical** documents, otherwise the framework side is again making its own
        // decision about the digest or the field order.
        val envelope = envelope()

        assertEquals(
            ConstraintProgrammingCheckpointCodec.encode(envelope).value,
            PortableCheckpointCodec.encode(envelope)
        )
    }

    @Test
    fun frameworkEnvelopeIsTheCoreEnvelopeType() {
        // typealias 的直接证据：框架侧的 envelope 可以直接交给 core 的 codec，反之亦然。
        // Direct evidence of the typealias: a framework-side envelope is accepted by core's codec and
        // vice versa, with no conversion step.
        val envelope: ConstraintProgrammingCheckpointEnvelope = envelope()

        assertTrue(ConstraintProgrammingCheckpointCodec.encode(envelope).ok)
        assertNotNull(PortableCheckpointCodec.decodeOrNull(PortableCheckpointCodec.encode(envelope)))
    }

    @Test
    fun forwardedDecodeAppliesTheContractSchemaGate() {
        requireFixtures()
        val encoded = PortableCheckpointCodec.encode(envelope())
        assertTrue(
            encoded.contains("\"schemaVersion\":\"3.0\""),
            "编码必须携带契约 schema 3.0 / the encoding must carry the contract's schema 3.0"
        )

        val decoded = assertNotNull(PortableCheckpointCodec.decodeOrNull(encoded))
        assertEquals("3.0", decoded.schemaVersion)
        assertEquals(rows("schema").associate { it[0] to it[1] }["schema_version"], decoded.schemaVersion)

        // 旧 schema 文档必须被当前入口拒绝（legacy 迁移由 decodeCompatibleOrNull 单独负责）。
        // A document of the previous schema must be rejected by the current entry point; legacy
        // migration is handled separately by decodeCompatibleOrNull.
        val previousSchema = encoded.replace("\"schemaVersion\":\"3.0\"", "\"schemaVersion\":\"2.0\"")
        assertTrue(previousSchema != encoded, "版本替换必须命中 schemaVersion / the version replacement must hit schemaVersion")
        assertEquals(null, PortableCheckpointCodec.decodeOrNull(previousSchema))
    }

    @Test
    fun catchAllOriginCodesSurviveTheFrameworkRoundTrip() {
        // 框架入口同样必须无损往返兜底代码：Rust 的 `backend` 与未知代码经 Kotlin 解析后仍要原样写回。
        // The framework entry point must round-trip catch-all codes losslessly too: Rust's `backend`
        // and an unknown code must be written back verbatim after Kotlin has parsed them.
        val encoded = PortableCheckpointCodec.encode(envelope())
        val decoded = assertNotNull(PortableCheckpointCodec.decodeOrNull(encoded))
        val reEncoded = PortableCheckpointCodec.encode(decoded)

        val origins = (Json.parseToJsonElement(reEncoded).jsonObject["cancellationChain"] as? JsonArray)
            ?.map { (it as? JsonObject)?.get("origin")?.let { origin -> origin.toString().trim('"') } }
        assertEquals(
            listOf("user", "backend", "vendorSpecificStop"),
            origins,
            "取消链来源代码必须逐位无损往返 / the chain's origin codes must round-trip losslessly"
        )
        assertEquals(
            encoded,
            reEncoded,
            "二次编码必须逐字稳定 / re-encoding must be byte-stable"
        )
    }

    /**
     * 构造一个可正常编码的最小 envelope。 / Build a minimal envelope that encodes cleanly.
     *
     * @return 带取消链与 provenance 的最小 envelope / A minimal envelope carrying a cancellation chain and provenance
     */
    private fun envelope(): PortableCheckpointEnvelope {
        val snapshot = canonicalSnapshot()
        return PortableCheckpointEnvelope(
            checkpointId = "checkpoint:remote-wire-contract",
            identitySchemaVersion = "1.0",
            identityNamespace = "model-local",
            modelName = "remote-cp",
            modelFingerprint = PortableCheckpointCodec.sha256(snapshot),
            configurationFingerprint = "configuration:run-1",
            solverFingerprint = "solver:fake",
            runId = "run:1",
            attemptId = "attempt:2",
            createdAtEpochMs = 1_000L,
            snapshotJson = snapshot,
            cancellationChain = listOf(
                PortableCancellationRecord(origin = "user", requestedAtEpochMs = 1_000L, reason = "caller"),
                PortableCancellationRecord(origin = "backend", requestedAtEpochMs = 1_001L),
                PortableCancellationRecord(origin = "vendorSpecificStop", requestedAtEpochMs = 1_002L)
            ),
            provenance = PortableSolverProvenance(
                solverId = "solver:fake",
                backendName = "fake",
                requestedConfiguration = linkedMapOf("threads" to "2"),
                effectiveConfiguration = linkedMapOf("threads" to "2"),
                environmentSummary = linkedMapOf("os" to "windows")
            )
        )
    }

    /** 取回规范编码的最小 CP snapshot。 / Fetch the canonically encoded minimal CP snapshot. */
    private fun canonicalSnapshot(): String {
        return "{\"schema\":1,\"name\":\"remote-wire-contract\",\"objectCategory\":\"Minimum\"," +
            "\"variables\":[],\"intervals\":[],\"expressions\":[],\"constraints\":[],\"objectives\":[]," +
            "\"constraintGroups\":[],\"identitySchemaVersion\":\"1.0\"," +
            "\"identityNamespace\":\"model-local\"}"
    }

    /** 取回 fixture 根目录；缺失时显式失败，与 core 的契约测试行为一致。 */
    private fun requireFixtures() {
        if (fixtureRoot != null) {
            return
        }
        assumeTrue(
            !SKIP_REQUESTED,
            "跨语言契约被显式跳过（$SKIP_ENV 已设置） / cross-language contract explicitly skipped because $SKIP_ENV is set"
        )
        fail(
            "checkpoint-wire-contract.tsv not found. Expected it at one of: <repo>/../../analysis-fixtures, " +
                "<repo>/../analysis-fixtures, <repo>/analysis-fixtures, " +
                "<core-module>/src/test/resources/analysis-fixtures. Set $SKIP_ENV=1 to skip explicitly."
        )
    }

    /**
     * 读取契约文件的某个段。 / Read one section of the contract file.
     *
     * @param section 段名 / Section name
     * @return 该段的数据行 / The section's data rows
     */
    private fun rows(section: String): List<List<String>> {
        val root = fixtureRoot ?: return emptyList()
        val file = File(root, CONTRACT_FILE)
        if (!file.isFile) {
            return emptyList()
        }
        val result = ArrayList<List<String>>()
        var current: String? = null
        file.readLines().forEach { raw ->
            val line = raw.trimEnd()
            if (line.isBlank() || line.trimStart().startsWith("#")) {
                return@forEach
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                current = line.substring(1, line.length - 1)
                return@forEach
            }
            if (current == section) {
                // 数据行既支持 TAB 分隔，也支持单行 `key=value`（`[schema]` 段用的就是后者）。
                // A data row is either TAB-separated or a single `key=value` pair, the latter being
                // what the `[schema]` section uses.
                val fields = line.split("\t").map { it.trim() }
                result += if (fields.size == 1 && fields[0].contains('=')) {
                    val index = fields[0].indexOf('=')
                    listOf(fields[0].substring(0, index), fields[0].substring(index + 1))
                } else {
                    fields
                }
            }
        }
        return result
    }

    private companion object {
        /** 显式跳过开关名。 / Name of the explicit skip switch. */
        const val SKIP_ENV: String = "OSPF_SKIP_CROSS_LANGUAGE_FIXTURE"

        /** 契约文件名。 / The contract file name. */
        const val CONTRACT_FILE: String = "checkpoint-wire-contract.tsv"

        /** 是否由调用方显式请求跳过跨语言契约。 / Whether the caller explicitly requested skipping the contract. */
        val SKIP_REQUESTED: Boolean = System.getenv(SKIP_ENV)?.let { it.isNotEmpty() && it != "0" } == true
    }
}
