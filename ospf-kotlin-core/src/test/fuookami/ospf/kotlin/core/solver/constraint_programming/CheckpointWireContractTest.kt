package fuookami.ospf.kotlin.core.solver.constraint_programming

import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.solver.report.CancellationRecord
import fuookami.ospf.kotlin.core.solver.report.CancellationSource
import fuookami.ospf.kotlin.core.solver.report.SolveFingerprinting
import fuookami.ospf.kotlin.core.variable.IntVar

/**
 * checkpoint envelope 线格式契约测试（schema 3.0）。 / Checkpoint envelope wire-contract test (schema 3.0).
 *
 * 读取两端共同维护的 `analysis-fixtures/checkpoint-wire-contract.tsv`，断言：
 *
 * 1. `[envelope-field]` 的字段序列（含数量）与 envelope 编码后的 JSON 顶层 key 序列**逐位相同**；
 * 2. `[cancellation-origin]` 的词表与 Kotlin 侧枚举映射一致，且专用变体以外的代码（Rust 的
 *    `backend`、完全未知的代码）能无损往返；
 * 3. `[provenance-field]` 与 `[cancellation-record]` 的字段序列与编码结果一致；
 * 4. `[canonicalization]` 的四条前提（字段顺序、显式输出可空字段、string-map 按键升序、摘要可复算）
 *    在实现中成立。
 *
 * Reads the shared `analysis-fixtures/checkpoint-wire-contract.tsv` and asserts that:
 *
 * 1. the `[envelope-field]` sequence (count included) matches the top-level key sequence of the
 *    encoded envelope **position by position**;
 * 2. the `[cancellation-origin]` vocabulary matches Kotlin's enum mapping, and codes outside the
 *    dedicated variants (Rust's `backend`, wholly unknown codes) round-trip losslessly;
 * 3. the `[provenance-field]` and `[cancellation-record]` sequences match the encoding;
 * 4. the four `[canonicalization]` preconditions (field order, explicit nullable fields, ascending
 *    string-map keys, recomputable digest) hold in the implementation.
 */
class CheckpointWireContractTest {
    /**
     * 定位两端共同维护的 `analysis-fixtures`。
     *
     * 候选顺序与 `AnalysisFixtureContractTest` 保持一致：仓库同级检出、父仓库嵌套检出、仓内镜像。
     * 仓内镜像保证单仓库检出也能真实断言契约，而不是静默通过。
     *
     * Locate the shared `analysis-fixtures`, using the same candidate order as
     * `AnalysisFixtureContractTest`: sibling checkout, parent checkout, and the in-repo mirror. The
     * mirror keeps a single-repository checkout genuinely asserting the contract instead of passing
     * silently.
     */
    private val fixtureRoot: File? = sequenceOf(
        File("../../analysis-fixtures"),
        File("../analysis-fixtures"),
        File("analysis-fixtures"),
        File("src/test/resources/analysis-fixtures")
    ).firstOrNull { it.isDirectory }

    @Test
    fun envelopeEncodingFieldSequenceMatchesTheContract() {
        requireFixtures()
        val expected = fieldNames("envelope-field")
        assertEquals(
            25,
            expected.size,
            "契约必须声明 25 个 envelope 字段 / the contract must declare 25 envelope fields"
        )
        assertEquals("cancellationChain", expected[21], "第 21 位必须是 cancellationChain")
        assertEquals("provenance", expected[22], "第 22 位必须是 provenance")
        assertEquals("metadata", expected[23], "第 23 位必须是 metadata")
        assertEquals("integritySha256", expected[24], "第 24 位必须是 integritySha256")

        val actual = topLevelKeys(encodeAuditedEnvelope())

        assertEquals(
            expected,
            actual,
            "envelope 编码后的顶层 key 序列必须与契约 [envelope-field] 逐位相同 / " +
                "the encoded envelope's top-level key sequence must match the contract's [envelope-field] exactly"
        )
        assertEquals(
            expected.size,
            actual.size,
            "envelope 不得输出契约之外的多余字段 / the envelope must not emit fields beyond the contract"
        )
    }

    @Test
    fun envelopeSchemaVersionMatchesTheContractSchema() {
        requireFixtures()
        val schema = rows("schema").associate { it[0] to it[1] }
        assertEquals("3.0", schema["schema_version"])

        val root = jsonRoot(encodeAuditedEnvelope())
        assertEquals(
            "3.0",
            root.stringValue("schemaVersion"),
            "编码必须输出契约声明 schema 3.0 / the encoding must carry the contract's schema 3.0"
        )
    }

    @Test
    fun provenanceFieldSequenceMatchesTheContract() {
        requireFixtures()
        val expected = fieldNames("provenance-field")
        assertEquals(
            10,
            expected.size,
            "契约必须声明 10 个 provenance 字段 / the contract must declare 10 provenance fields"
        )

        val provenance = assertNotNull(
            jsonRoot(encodeAuditedEnvelope())["provenance"]?.jsonObject,
            "已提供 provenance 的 envelope 必须显式输出该对象 / an envelope carrying provenance must emit the object"
        )
        assertEquals(expected, provenance.keys.toList())
    }

    @Test
    fun cancellationRecordFieldSequenceMatchesTheContract() {
        requireFixtures()
        val expected = fieldNames("cancellation-record")
        assertEquals(listOf("origin", "requestedAtEpochMs", "reason"), expected)

        val chain = assertNotNull(
            jsonRoot(encodeAuditedEnvelope())["cancellationChain"],
            "取消链必须显式输出 / the cancellation chain must be emitted explicitly"
        )
        val first = assertNotNull(
            (chain as? JsonArray)?.firstOrNull()?.jsonObject,
            "取消链必须输出至少一条记录 / the chain must emit at least one record"
        )
        assertEquals(expected, first.keys.toList())
    }

    @Test
    fun cancellationOriginVocabularyMatchesKotlinMapping() {
        requireFixtures()
        val vocabulary = rows("cancellation-origin")
        assertTrue(vocabulary.isNotEmpty(), "cancellation-origin 词表不能为空 / the vocabulary must not be empty")

        val seenCodes = HashSet<String>()
        vocabulary.forEach { row ->
            val wireCode = row[0]
            assertTrue(seenCodes.add(wireCode), "规范代码不得重复：$wireCode / a canonical code must not repeat")
            assertEquals(
                row[2],
                CancellationSource.fromWireCode(wireCode).name,
                "wire 代码 $wireCode 必须映射到 Kotlin 的 ${row[2]} / code $wireCode must map to Kotlin's ${row[2]}"
            )
        }

        // 专用变体必须与规范代码一一对应且可复现；兜底代码 `external`/`backend` 都落到 `Other`，
        // 因此它们不满足一一对应，必须靠 `wireOrigin` 才能无损。
        // Dedicated variants must map one-to-one onto canonical codes and reproduce them; the
        // catch-all codes `external`/`backend` both land in `Other`, so they are not one-to-one and
        // rely on `wireOrigin` for losslessness.
        val dedicated = vocabulary.filter { it[2] != CancellationSource.Other.name }
        assertEquals(
            CancellationSource.entries.size - 1,
            dedicated.size,
            "除兜底变体外，每个 Kotlin 变体必须恰好有一个规范代码 / " +
                "every Kotlin variant except the catch-all must have exactly one canonical code"
        )
        dedicated.forEach { row ->
            assertEquals(
                row[0],
                CancellationSource.valueOf(row[2]).toWireCode(),
                "Kotlin 的 ${row[2]} 必须写回规范代码 ${row[0]} / Kotlin's ${row[2]} must write back canonical code ${row[0]}"
            )
        }
        assertEquals(
            listOf("external", "backend"),
            vocabulary.filter { it[2] == CancellationSource.Other.name }.map { it[0] },
            "兜底代码集合必须与契约一致 / the catch-all code set must match the contract"
        )
    }

    @Test
    fun cancellationChainRoundTripPreservesCatchAllAndUnknownCodes() {
        // 从线格式收到的代码全部按契约词表解析；`backend`、`external` 与完全未知的代码落到兜底
        // 变体，必须连同原始文本一起保留，再序列化回去仍是同一个代码。
        //
        // Every code received from the wire is parsed against the contract vocabulary; `backend`,
        // `external`, and a wholly unknown code land in the catch-all variant and must be preserved
        // together with their original text, so re-serializing still yields the same code.
        val wireCodes = listOf(
            "user",
            "callback",
            "taskAbort",
            "future",
            "frameworkLoser",
            "remoteStop",
            "timeout",
            "backend",
            "external",
            "vendorSpecificStop"
        )
        val records = wireCodes.mapIndexed { index, code ->
            PortableCancellationRecord(
                origin = code,
                requestedAtEpochMs = 1_000L + index,
                reason = "reason-$index"
            ).toCancellationRecord()
        }

        // 解析结果本身必须符合契约映射。 / The parse result itself must follow the contract mapping.
        val expectedSources = listOf(
            CancellationSource.Caller,
            CancellationSource.Callback,
            CancellationSource.Coroutine,
            CancellationSource.Future,
            CancellationSource.Combinatorial,
            CancellationSource.Remote,
            CancellationSource.Timeout,
            CancellationSource.Other,
            CancellationSource.Other,
            CancellationSource.Other
        )
        assertEquals(expectedSources, records.map { it.source })
        assertEquals(
            listOf(null, null, null, null, null, null, null, "backend", "external", "vendorSpecificStop"),
            records.map { it.wireOrigin },
            "兜底变体必须原样保留收到的代码文本，否则无法无损往返 / " +
                "a catch-all variant must keep the received code text verbatim or the round trip is lossy"
        )

        val encoded = encodeAuditedEnvelope(cancellationChain = records)
        val decoded = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
            ConstraintProgrammingCheckpointCodec.decode(encoded)
        ).value
        assertEquals(records.size, decoded.cancellationChain.size)

        val reEncoded = assertIs<Ok<String, *, *>>(
            ConstraintProgrammingCheckpointCodec.encode(decoded)
        ).value
        val encodedChain = jsonRoot(reEncoded)["cancellationChain"] as? JsonArray
        assertNotNull(encodedChain, "取消链必须显式输出 / the cancellation chain must be emitted explicitly")
        assertEquals(
            wireCodes,
            encodedChain.mapNotNull { (it as? JsonObject)?.stringValue("origin") },
            "取消链来源代码必须逐位无损往返 / the chain's origin codes must round-trip losslessly, position by position"
        )
        assertEquals(
            (0 until wireCodes.size).map { 1_000L + it },
            encodedChain.mapNotNull { (it as? JsonObject)?.stringValue("requestedAtEpochMs")?.toLongOrNull() },
            "取消链时间戳必须逐位无损往返 / the chain's timestamps must round-trip losslessly"
        )
        assertEquals(
            encoded,
            reEncoded,
            "二次编码必须逐字稳定，否则跨语言摘要无从复算 / re-encoding must be byte-stable or the cross-language digest is not recomputable"
        )
    }

    @Test
    fun provenanceMapsAreEmittedInAscendingKeyOrder() {
        // 契约 [canonicalization] 第 3 条：string-map 必须按键升序输出。Kotlin 的 Map 保留插入
        // 顺序，因此这里刻意乱序插入，断言编码结果仍然升序（Rust 的 BTreeMap 天然有序）。
        //
        // Contract [canonicalization] rule 3: string-maps must be emitted in ascending key order.
        // Kotlin's Map preserves insertion order, so keys are deliberately inserted out of order and
        // the encoding must still come out ascending (Rust's BTreeMap is inherently ordered).
        val provenance = PortableSolverProvenance(
            solverId = "solver:gurobi",
            backendName = "gurobi",
            backendVersion = "11.0",
            pluginVersion = "1.2.3",
            requestedConfiguration = linkedMapOf("threads" to "4", "seed" to "7", "mipGap" to "0.01"),
            effectiveConfiguration = linkedMapOf("zeta" to "1", "alpha" to "2", "mu" to "3"),
            threadCount = 4,
            randomSeed = 7L,
            deterministic = true,
            environmentSummary = linkedMapOf("os" to "linux", "cpu" to "arm64", "jvm" to "21")
        )

        val encodedProvenance = assertNotNull(
            jsonRoot(encodeAuditedEnvelope(provenance = provenance))["provenance"]?.jsonObject
        )

        assertEquals(listOf("mipGap", "seed", "threads"), encodedProvenance.mapKeys("requestedConfiguration"))
        assertEquals(listOf("alpha", "mu", "zeta"), encodedProvenance.mapKeys("effectiveConfiguration"))
        assertEquals(listOf("cpu", "jvm", "os"), encodedProvenance.mapKeys("environmentSummary"))
        assertEquals("4", encodedProvenance.stringValue("threadCount"))
        assertEquals("7", encodedProvenance.stringValue("randomSeed"))
        assertEquals("true", encodedProvenance.stringValue("deterministic"))

        // 解码回来的 provenance 也必须已经是升序的，调用方无需再排序。
        // The decoded provenance must already be ascending, so callers need not sort again.
        val decodedProvenance = assertNotNull(
            assertNotNull(
                ConstraintProgrammingCheckpointCodec.decode(
                    encodeAuditedEnvelope(provenance = provenance)
                ).value
            ).provenance
        )
        assertEquals(listOf("mipGap", "seed", "threads"), decodedProvenance.requestedConfiguration.keys.toList())
        assertEquals(listOf("alpha", "mu", "zeta"), decodedProvenance.effectiveConfiguration.keys.toList())
        assertEquals(listOf("cpu", "jvm", "os"), decodedProvenance.environmentSummary.keys.toList())
    }

    @Test
    fun nullableFieldsAreEmittedExplicitly() {
        requireFixtures()
        val canonicalization = rows("canonicalization").associate { it[0] to it[1] }
        assertEquals("true", canonicalization["emit_nulls"])

        // 未提供 provenance / benders 时，字段必须以 null 显式输出而不是被省略，否则跨语言摘要失配。
        // With no provenance/benders supplied, the fields must be emitted as explicit null rather than
        // omitted, otherwise the cross-language digest breaks.
        val encoded = encodeAuditedEnvelope(cancellationChain = emptyList(), provenance = null)
        val root = jsonRoot(encoded)
        assertTrue(root.containsKey("provenance"), "可空字段必须显式输出 / nullable fields must be emitted explicitly")
        assertTrue(root.containsKey("benders"), "可空字段必须显式输出 / nullable fields must be emitted explicitly")
        assertTrue(root.containsKey("configurationFingerprint"), "可空字段必须显式输出 / nullable fields must be emitted explicitly")
        assertEquals("null", root["provenance"].toString(), "缺失 provenance 必须输出 null / an absent provenance must be emitted as null")
        assertEquals("null", root["benders"].toString(), "缺失 benders 必须输出 null / absent benders must be emitted as null")
        assertEquals("[]", root["cancellationChain"].toString(), "空取消链必须输出 [] / an empty chain must be emitted as []")
    }

    @Test
    fun sharedCrossLanguageEnvelopeIsAcceptedAndItsDigestRecomputes() {
        // **跨语言互操作的判据**：`analysis-fixtures/checkpoint-envelope-v3.json` 是 **Rust 侧
        // 序列化产出**的完整 envelope（含 Rust 计算出的 integritySha256）。若 Kotlin 能
        // 解码它、并通过摘要校验，就证明两侧对**同一份文档**产出**逐字节相同**的 JSON ——
        // 因为摘要是对整份 JSON 计算的，任何字段顺序、null 输出或键序差异都会让摘要失配。
        //
        // 这比"两侧各自跑绿"强得多：各自的契约测试只能证明各自符合契约文件，而本用例要求
        // 两侧在同一份具体文档上真正等价。
        //
        // **The cross-language interoperability criterion**: `analysis-fixtures/checkpoint-envelope-v3.json`
        // is a complete envelope **produced by Rust serialization** (including the Rust-computed
        // integritySha256). If Kotlin can decode it and pass the digest check, both sides provably
        // emit **byte-identical** JSON for the same document, because the digest covers the whole
        // JSON and any divergence in field order, null emission, or key ordering would break it.
        //
        // This is much stronger than "both sides pass their own tests": each side's contract test only
        // shows it conforms to the contract file, whereas this case requires the two sides to be
        // genuinely equivalent on one concrete document.
        requireFixtures()
        val root = assertNotNull(fixtureRoot)
        val file = File(root, CROSS_LANGUAGE_ENVELOPE)
        assertTrue(file.isFile, "跨语言 envelope 夹具缺失：${file.path}")

        val document = file.readText().trimEnd()
        val decoded = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
            ConstraintProgrammingCheckpointCodec.decode(document),
            "Rust 产出的 envelope 必须能被 Kotlin 解码并通过摘要校验 / a Rust-produced envelope must decode under Kotlin and pass its digest check"
        )

        // 摘要必须等于 Kotlin 依据契约定义复算的结果。
        // The digest must equal what Kotlin recomputes from the contract definition.
        val declared = decoded.value.integritySha256
        assertEquals(64, declared.length)
        assertEquals(
            declared,
            SolveFingerprinting.sha256(
                document.replace("\"integritySha256\":\"$declared\"", "\"integritySha256\":\"\"")
            ).value,
            "Kotlin 复算的摘要必须等于 Rust 写入的摘要 / Kotlin's recomputed digest must equal the digest Rust wrote"
        )

        // 取消链与 provenance 必须完整可读（Rust 写入的内容不得在 Kotlin 侧丢失或错位）。
        // The cancellation chain and provenance must read back completely (nothing Rust wrote may be
        // lost or shifted on the Kotlin side).
        assertEquals(2, decoded.value.cancellationChain.size)
        assertEquals("remoteStop", decoded.value.cancellationChain[0].origin)
        assertEquals(1_700_000_000_001L, decoded.value.cancellationChain[0].requestedAtEpochMs)
        assertEquals(
            "remote dispatcher stopped the attempt",
            decoded.value.cancellationChain[0].reason
        )
        assertEquals("frameworkLoser", decoded.value.cancellationChain[1].origin)
        assertEquals(null, decoded.value.cancellationChain[1].reason)

        val provenance = assertNotNull(decoded.value.provenance)
        assertEquals("kotlin-cp/1.0", provenance.solverId)
        assertEquals("kotlin-backend", provenance.backendName)
        assertEquals(4, provenance.threadCount)
        assertEquals(1_700_000_000_123L, provenance.randomSeed)
        assertEquals(true, provenance.deterministic)
        assertEquals("4", provenance.effectiveConfiguration["mipThreads"])

        // 结构化 metadata 是一等字段，必须能完整读回（并且已是升序规范形态）。
        // The structured metadata is a first-class field and must read back completely (already in
        // canonical ascending-key form).
        assertEquals(
            mapOf("alpha" to "first", "mid" to "middle", "zeta" to "last"),
            decoded.value.metadata
        )

        // 再编码必须与输入**逐字节相同**：这条断言把"两侧序列化等价"钉死，而不只是"能互相解析"。
        // Re-encoding must be **byte-identical** to the input, pinning "the two sides serialize
        // equivalently" rather than merely "they can parse each other".
        val reencoded = assertIs<Ok<String, *, *>>(
            ConstraintProgrammingCheckpointCodec.encode(decoded.value)
        ).value
        assertEquals(
            document,
            reencoded,
            "Kotlin 重编码结果必须与 Rust 产出的文档逐字节相同 / Kotlin's re-encoding must be byte-identical to the Rust-produced document"
        )
    }

    @Test
    fun sourceFormatVocabularyMatchesTheContract() {
        // `sourceFormat` 是一等字段，却是**来源族**标记而非版本号：当前 envelope 写 `v2` 而
        // `schemaVersion` 是 `3.0`。该词表此前只存在于代码里、契约未规定，属于跨语言契约缺口；
        // 本用例把它钉住，并断言两侧的宽严分工与契约一致：严格路径只收 `v2`，宽松路径额外收
        // `legacy-v1` 并迁移。
        //
        // `sourceFormat` is a first-class field but is a **source-family** tag rather than a version
        // number: the current envelope writes `v2` while `schemaVersion` is `3.0`. That vocabulary
        // previously existed only in code and was unspecified by the contract, a gap in the cross-language
        // contract; this test pins it and asserts the two leniency levels match the contract: the strict
        // path accepts only `v2`, while the lenient path additionally accepts `legacy-v1` and migrates it.
        requireFixtures()
        val rows = rows("source-format")
        assertEquals(5, rows.size, "source-format 必须声明五行 / source-format must declare five rows")
        val values = rows.associate { it[0] to it[1] }
        assertEquals("v2", values["producer_value"])
        assertEquals("v2", values["strict_readers_accept"])
        assertEquals("v2,legacy-v1", values["lenient_readers_accept"])
        assertEquals("legacy-v1", values["legacy_marker"])

        // 产出方必须写 `v2`。 / A producer must write `v2`.
        assertEquals(
            "v2",
            encodeAuditedEnvelope().let { jsonRoot(it).stringValue("sourceFormat") },
            "产出方必须写 sourceFormat = v2 / a producer must write sourceFormat = v2"
        )

        // 严格路径只接受 `v2`；宽松路径额外接受 `legacy-v1`。
        // The strict path accepts only `v2`; the lenient path additionally accepts `legacy-v1`.
        val encoded = encodeAuditedEnvelope()
        assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
            ConstraintProgrammingCheckpointCodec.decode(encoded)
        )
        val legacy = encoded.replace("\"sourceFormat\":\"v2\"", "\"sourceFormat\":\"legacy-v1\"")
        assertTrue(legacy != encoded, "篡改必须命中 sourceFormat / the tampering must hit sourceFormat")
        assertIs<Failed<*, *, *>>(
            ConstraintProgrammingCheckpointCodec.decode(legacy),
            "严格路径必须拒绝 legacy-v1 / the strict path must reject legacy-v1"
        )
    }

    @Test
    fun integrityDigestIsRecomputableFromTheContractDefinition() {
        // 契约 [canonicalization]：integritySha256 = SHA-256(UTF-8(JSON(envelope with integritySha256 = "")))。
        // Contract [canonicalization]: the digest is the SHA-256 of the envelope's JSON with the digest field cleared.
        val encoded = encodeAuditedEnvelope()
        val declared = assertNotNull(jsonRoot(encoded).stringValue("integritySha256"))
        assertEquals(64, declared.length, "摘要必须是 64 位十六进制 / the digest must be 64 hex characters")

        val unsigned = encoded.replace("\"integritySha256\":\"$declared\"", "\"integritySha256\":\"\"")
        assertTrue(unsigned != encoded, "必须存在可清除的摘要字段 / a clearable digest field must exist")
        assertEquals(
            declared,
            SolveFingerprinting.sha256(unsigned).value,
            "摘要必须可由契约定义复算 / the digest must be recomputable from the contract definition"
        )

        // 篡改任一字段都会改变摘要——这正是跨语言摘要能充当契约守卫的原因。
        // Tampering with any field changes the digest, which is why the cross-language digest can
        // serve as a contract guard.
        val tampered = encoded.replace("\"runId\":\"run:1\"", "\"runId\":\"run:2\"")
        assertTrue(tampered != encoded, "篡改必须命中一个真实字段 / the tampering must hit a real field")
        assertIs<Failed<*, *, *>>(ConstraintProgrammingCheckpointCodec.decode(tampered))
    }

    @Test
    fun invalidCancellationChainAndProvenanceAreRejectedOnRestore() {
        // 线格式承载的非法取值必须在恢复侧被拒绝：Rust 的 `requested_at_epoch_ms` 是无符号数，
        // 来源代码也不能为空白（否则回读时无法映射回任何变体）。
        //
        // Illegal values that the wire carries must be rejected on restore: Rust's
        // `requested_at_epoch_ms` is unsigned, and the origin code must not be blank (otherwise it
        // cannot map back to any variant).
        val model = checkpointModel("wire-contract-invalid")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = snapshot,
                    descriptor = solver.descriptor,
                    checkpointId = "checkpoint:wire-contract",
                    createdAtEpochMs = 1_000L,
                    configurationFingerprint = "configuration:run-1"
                )
            ).value

            val blankOrigin = ConstraintProgrammingCheckpointCodec.withIntegrity(
                captured.copy(
                    cancellationChain = listOf(
                        PortableCancellationRecord(origin = "  ", requestedAtEpochMs = 1L)
                    )
                )
            )
            assertEquals(
                "checkpoint 取消来源代码不能为空白",
                assertIs<Failed<*, *, *>>(
                    ConstraintProgrammingCheckpointCodec.restore(
                        envelope = blankOrigin,
                        snapshot = snapshot,
                        expectedConfigurationFingerprint = "configuration:run-1",
                        expectedSolverFingerprint = blankOrigin.solverFingerprint
                    )
                ).error.message?.substringBefore(" / ")
            )

            val negativeTimestamp = ConstraintProgrammingCheckpointCodec.withIntegrity(
                captured.copy(
                    cancellationChain = listOf(
                        PortableCancellationRecord(origin = "user", requestedAtEpochMs = -1L)
                    )
                )
            )
            assertEquals(
                "checkpoint 取消时间戳不能为负",
                assertIs<Failed<*, *, *>>(
                    ConstraintProgrammingCheckpointCodec.restore(
                        envelope = negativeTimestamp,
                        snapshot = snapshot,
                        expectedConfigurationFingerprint = "configuration:run-1",
                        expectedSolverFingerprint = negativeTimestamp.solverFingerprint
                    )
                ).error.message?.substringBefore(" / ")
            )

            val blankProvenance = ConstraintProgrammingCheckpointCodec.withIntegrity(
                captured.copy(
                    provenance = PortableSolverProvenance(solverId = " ", backendName = "fake")
                )
            )
            assertIs<Failed<*, *, *>>(
                ConstraintProgrammingCheckpointCodec.restore(
                    envelope = blankProvenance,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = "configuration:run-1",
                    expectedSolverFingerprint = blankProvenance.solverFingerprint
                )
            )
        } finally {
            model.close()
        }
    }

    @Test
    fun decodeRejectsDocumentsWhoseMapsAreNotInCanonicalOrder() {
        // 契约 [canonicalization] 第 3 条把"string-map 升序"作为摘要可复算的前提，因此摘要必须在
        // 规范化文本上复算：一份 map 乱序、并按乱序文本自己签名的文档必须被拒绝（Rust 侧反序列化到
        // BTreeMap 后天然如此）。对照组证明拒绝来自排序，而不是恒失败。
        //
        // Contract [canonicalization] rule 3 makes ascending string-map keys a precondition for digest
        // recomputation, so the digest must be recomputed over canonical text: a document whose maps are
        // unsorted and whose digest was signed over that unsorted text must be rejected (Rust behaves
        // this way for free by deserializing into BTreeMaps). The control case proves the rejection comes
        // from the ordering rather than failing unconditionally.
        val encoded = encodeAuditedEnvelope(provenance = contractProvenance())
        val root = jsonRoot(encoded).toMutableMap()
        val provenance = root.getValue("provenance").jsonObject.toMutableMap()

        fun signedWith(configuration: Map<String, String>): String {
            provenance["requestedConfiguration"] = JsonObject(
                configuration.entries.associate { (key, value) -> key to JsonPrimitive(value) }
            )
            root["provenance"] = JsonObject(provenance)
            root["integritySha256"] = JsonPrimitive("")
            val unsigned = JsonObject(root).toString()
            root["integritySha256"] = JsonPrimitive(SolveFingerprinting.sha256(unsigned).value)
            return Json.encodeToString(JsonObject(root))
        }

        // 对照：升序 map 与升序文本上的签名必须通过。
        // Control: ascending maps signed over ascending text must pass.
        val canonical = signedWith(linkedMapOf("seed" to "1", "threads" to "2"))
        assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
            ConstraintProgrammingCheckpointCodec.decode(canonical)
        )

        val unsorted = signedWith(linkedMapOf("threads" to "2", "seed" to "1"))
        assertTrue(unsorted != canonical, "乱序文本必须与规范文本不同 / the unsorted text must differ from the canonical one")
        assertEquals(
            "checkpoint 完整性摘要不匹配",
            assertIs<Failed<*, *, *>>(ConstraintProgrammingCheckpointCodec.decode(unsorted))
                .error.message?.substringBefore(" / ")
        )
    }

    /**
     * 捕获一个带审计身份、取消链与 provenance 的 envelope 并编码。 /
     * Capture an envelope carrying audit identity, a cancellation chain, and provenance, then encode it.
     *
     * @param cancellationChain 取消事实链 / Cancellation-fact chain
     * @param provenance 求解器执行来源 / Solver execution provenance
     * @return 编码后的 JSON 文本 / Encoded JSON text
     */
    private fun encodeAuditedEnvelope(
        cancellationChain: List<CancellationRecord> = listOf(
            CancellationRecord(
                source = CancellationSource.Remote,
                requestedAt = Instant.ofEpochMilli(1_000L),
                reason = "peer stop"
            )
        ),
        provenance: PortableSolverProvenance? = contractProvenance()
    ): String {
        val model = checkpointModel("wire-contract")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.capture(
                    snapshot = snapshot,
                    descriptor = solver.descriptor,
                    checkpointId = "checkpoint:wire-contract",
                    createdAtEpochMs = 1_000L,
                    configurationFingerprint = "configuration:run-1",
                    runId = "run:1",
                    attemptId = "attempt:2",
                    cancellationChain = cancellationChain,
                    provenance = provenance
                )
            ).value
            return assertIs<Ok<String, *, *>>(ConstraintProgrammingCheckpointCodec.encode(captured)).value
        } finally {
            model.close()
        }
    }

    /**
     * 构造契约测试使用的 provenance。 / Build the provenance used by the contract tests.
     *
     * `requestedConfiguration` 与 `environmentSummary` 都刻意乱序插入，用来验证编码输出按键升序。
     * `requestedConfiguration` and `environmentSummary` are deliberately inserted out of order to
     * verify that the encoding emits ascending keys.
     *
     * @return 三个 map 用于验证排序语义的 provenance / A provenance whose three maps exercise key ordering
     */
    private fun contractProvenance(): PortableSolverProvenance {
        return PortableSolverProvenance(
            solverId = "solver:fake",
            backendName = "fake",
            backendVersion = "1.0",
            pluginVersion = "0.1",
            requestedConfiguration = linkedMapOf("threads" to "2", "seed" to "1"),
            effectiveConfiguration = linkedMapOf("threads" to "2"),
            threadCount = 2,
            randomSeed = 1L,
            deterministic = true,
            environmentSummary = linkedMapOf("jvm" to "21", "os" to "windows")
        )
    }

    /**
     * 构造契约测试使用的最小 CP 模型。 / Build the minimal CP model used by the contract tests.
     *
     * @param name 模型名称 / Model name
     * @return 可行且有小规模整数域的 CP 模型 / A feasible CP model with a small integer domain
     */
    private fun checkpointModel(name: String): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel(name, ObjectCategory.Minimum)
        val value = IntVar("$name-value")
        model.registerVariable(value, IntegerDomain.interval(0, 3).value!!)
        val expression = ConstraintProgrammingExpression.Variable(value)
        model.addConstraint(
            constraint = ConstraintProgrammingConstraint.greaterOrEqual(expression, Int64.one).value!!,
            id = "$name-lower-bound"
        )
        model.minimize(expression)
        return model
    }

    /** 取回 fixture 根目录；缺失时显式失败，与 `AnalysisFixtureContractTest` 行为一致。 */
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
                "<repo>/../analysis-fixtures, <repo>/analysis-fixtures, <repo>/src/test/resources/analysis-fixtures. " +
                "Set $SKIP_ENV=1 to skip explicitly."
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
                // 数据行既支持 TAB 分隔，也支持单行 `key=value`。
                // A data row is either TAB-separated or a single `key=value` pair.
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

    /**
     * 读取某个字段列表段的字段名。 / Read the field names of one field-list section.
     *
     * @param section 段名 / Section name
     * @return 按契约顺序排列的字段名 / Field names in contract order
     */
    private fun fieldNames(section: String): List<String> {
        return rows(section).map { it[1] }
    }

    companion object {
        /** 显式跳过开关名。 / Name of the explicit skip switch. */
        const val SKIP_ENV: String = "OSPF_SKIP_CROSS_LANGUAGE_FIXTURE"

        /** 契约文件名。 / The contract file name. */
        const val CONTRACT_FILE: String = "checkpoint-wire-contract.tsv"

        /**
         * 由 Rust 侧序列化产出的完整 envelope，用作跨语言互操作夹具。
         * A complete envelope produced by Rust serialization, used as the cross-language interop fixture.
         */
        const val CROSS_LANGUAGE_ENVELOPE: String = "checkpoint-envelope-v3.json"

        /** 是否由调用方显式请求跳过跨语言契约。 / Whether the caller explicitly requested skipping the contract. */
        val SKIP_REQUESTED: Boolean = System.getenv(SKIP_ENV)?.let { it.isNotEmpty() && it != "0" } == true

        /**
         * 解析编码结果并取回顶层对象。 / Parse the encoding and fetch the top-level object.
         *
         * @param encoded 编码后的 JSON 文本 / Encoded JSON text
         * @return 顶层 JSON 对象 / The top-level JSON object
         */
        fun jsonRoot(encoded: String): JsonObject {
            return Json.parseToJsonElement(encoded).jsonObject
        }

        /**
         * 取回编码结果的全部顶层 key，保持文档顺序。 / Fetch every top-level key of the encoding, preserving document order.
         *
         * @param encoded 编码后的 JSON 文本 / Encoded JSON text
         * @return 顶层 key 序列 / The top-level key sequence
         */
        fun topLevelKeys(encoded: String): List<String> {
            return jsonRoot(encoded).keys.toList()
        }
    }
}

/**
 * 读取 JSON 字符串字面量的内容。 / Read the content of a JSON string literal.
 *
 * @param name 字段名 / Field name
 * @return 字符串内容，缺失或非字符串时为 null / The string content, or null when absent or not a string
 */
private fun JsonObject.stringValue(name: String): String? {
    return (this[name] as? JsonPrimitive)?.content
}

/**
 * 读取内嵌 string-map 的键序列。 / Read the key sequence of a nested string-map.
 *
 * @param name 字段名 / Field name
 * @return 键序列，字段缺失时为 emptyList / The key sequence, or emptyList when the field is absent
 */
private fun JsonObject.mapKeys(name: String): List<String> {
    return this[name]?.jsonObject?.keys?.toList() ?: emptyList()
}
