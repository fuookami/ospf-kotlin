/**
 * 可移植 checkpoint codec 单元测试。 / Unit tests for the portable checkpoint codec.
 *
 * 本 codec 与 core 的 `ConstraintProgrammingCheckpointCodec` 共享同一 wire 形状，并且**委托**给它：
 * 字段列表、摘要与父链校验都只有 core 一处实现。本用例固定转发后的父链语义与 core 完全一致，并
 * 守护"框架侧不再有第二份实现"。
 *
 * This codec shares its wire shape with core's `ConstraintProgrammingCheckpointCodec` and
 * **delegates** to it: the field list, the digest, and parent-link validation each have exactly one
 * implementation, in core. These tests pin the forwarded parent-link semantics to core's and guard
 * the absence of a second implementation on the framework side.
 */
package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlin.test.*
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointCodec

class PortableCheckpointCodecTest {

    /**
     * 构造一个可正常编码的最小 envelope。 / Build a minimal envelope that encodes cleanly.
     *
     * 收敛后 `PortableCheckpointCodec` 委托 core，而 core 会规范化并校验 snapshot，因此这里必须使用
     * **规范编码**的最小 snapshot，而不是曾经的占位串（占位串甚至无法反序列化）。
     * After convergence `PortableCheckpointCodec` delegates to core, which canonicalizes and validates
     * the snapshot, so a **canonically encoded** minimal snapshot is required here instead of the
     * placeholder string used before (the placeholder does not even deserialize).
     */
    private fun envelope(
        checkpointId: String = "checkpoint:attempt-2",
        parentCheckpointId: String? = null
    ): PortableCheckpointEnvelope {
        return PortableCheckpointEnvelope(
            checkpointId = checkpointId,
            identitySchemaVersion = "1.0",
            identityNamespace = "model-local",
            modelName = "codec-test",
            modelFingerprint = PortableCheckpointCodec.sha256(CODEC_TEST_SNAPSHOT),
            runId = "run:1",
            attemptId = "attempt:2",
            parentCheckpointId = parentCheckpointId,
            createdAtEpochMs = 1_000L,
            snapshotJson = CODEC_TEST_SNAPSHOT
        )
    }

    /** 重新签名以保证只有父链被改动。/ Re-sign so that only the parent link differs. */
    private fun sign(env: PortableCheckpointEnvelope): String = PortableCheckpointCodec.encode(env)

    @Test
    fun roundTripShouldPreserveIdentityAndParentLink() {
        // 对照：合法父链必须能正常往返，证明拒绝来自校验而非恒失败。
        // Control: a legitimate parent link round-trips, proving rejections come from validation
        // rather than failing unconditionally.
        val encoded = sign(envelope(parentCheckpointId = "checkpoint:attempt-1"))

        val decoded = assertNotNull(
            PortableCheckpointCodec.decodeOrNull(encoded),
            "合法 checkpoint 必须可解码"
        )

        assertEquals("checkpoint:attempt-2", decoded.checkpointId)
        assertEquals("checkpoint:attempt-1", decoded.parentCheckpointId)
        assertEquals("run:1", decoded.runId)
        assertEquals("attempt:2", decoded.attemptId)
    }

    @Test
    fun decodeShouldRejectSelfReferencingParent() {
        // 自引用父链必须被拒绝 / A self-referencing parent link must be rejected.
        val encoded = sign(envelope(checkpointId = "checkpoint:self", parentCheckpointId = "checkpoint:self"))

        assertNull(
            PortableCheckpointCodec.decodeOrNull(encoded),
            "自引用父链必须被拒绝"
        )
    }

    @Test
    fun decodeShouldRejectBlankParent() {
        // 空白父标识必须被拒绝，不能被当作"无父链"。
        // A blank parent must be rejected rather than treated as "no parent".
        val encoded = sign(envelope(parentCheckpointId = "   "))

        assertNull(
            PortableCheckpointCodec.decodeOrNull(encoded),
            "空白父链必须被拒绝"
        )
    }

    @Test
    fun decodeCompatibleShouldAlsoRejectIllegalParentLinks() {
        // 兼容入口不得绕过同一校验 / The compatible entry must not bypass the same check.
        val selfReferencing = sign(
            envelope(checkpointId = "checkpoint:self", parentCheckpointId = "checkpoint:self")
        )
        assertNull(PortableCheckpointCodec.decodeCompatibleOrNull(selfReferencing))

        val blank = sign(envelope(parentCheckpointId = "   "))
        assertNull(PortableCheckpointCodec.decodeCompatibleOrNull(blank))
    }

    @Test
    fun absentParentLinkShouldRemainAcceptable() {
        // 无父链是合法状态（首个 checkpoint），不得被误拒。
        // An absent parent is legitimate (the first checkpoint) and must not be rejected.
        val encoded = sign(envelope(parentCheckpointId = null))

        val decoded = assertNotNull(PortableCheckpointCodec.decodeOrNull(encoded))
        assertNull(decoded.parentCheckpointId)
    }

    @Test
    fun legitimateParentLinkShouldSurviveEncodeDecodeEncode() {
        // 二次编码必须稳定：合法父链不会在往返中被改写或丢失。
        // Re-encoding is stable: a legitimate parent link is neither rewritten nor lost.
        val first = sign(envelope(parentCheckpointId = "checkpoint:attempt-1"))
        val decoded = assertNotNull(PortableCheckpointCodec.decodeOrNull(first))
        val second = PortableCheckpointCodec.encode(decoded)

        assertEquals(
            "checkpoint:attempt-1",
            assertNotNull(PortableCheckpointCodec.decodeOrNull(second)).parentCheckpointId
        )
    }

    @Test
    fun tamperedParentLinkWithStaleDigestShouldBeRejected() {
        // 改写父链但不重新签名必须被摘要校验拒绝——这正是"仅完整性"能力，与父链校验各自独立。
        // Rewriting the parent without re-signing must fail the digest check: this is the
        // integrity-only capability, independent of parent-link validation.
        val encoded = sign(envelope(parentCheckpointId = "checkpoint:attempt-1"))
        val tampered = encoded.replace("checkpoint:attempt-1", "checkpoint:forged")

        assertNull(
            PortableCheckpointCodec.decodeOrNull(tampered),
            "摘要不匹配必须被拒绝"
        )
    }

    @Test
    fun encodeShouldEmitTheCanonicalSchemaVersionFromCore() {
        // 框架侧不再自己维护 schema 常量：编码结果必须携带 core 当前 schema，一旦有人重新引入
        // 副本就会在这里暴露。
        // The framework side no longer owns a schema constant: the encoding must carry core's current
        // schema, so reintroducing a copy surfaces here.
        val encoded = sign(envelope())

        assertTrue(
            encoded.contains("\"schemaVersion\":\"3.0\""),
            "编码结果必须携带 core 当前 schema 3.0 / the encoding must carry core's current schema 3.0"
        )
        assertEquals(
            "3.0",
            assertNotNull(PortableCheckpointCodec.decodeOrNull(encoded)).schemaVersion
        )
    }

    @Test
    fun frameworkCodecShouldForwardCoreParentLinkSemantics() {
        // 直接对照 core 的判定，防止框架侧再次分叉出第二份父链校验。
        // Compare against core's verdict directly so a second parent-link implementation cannot fork
        // off on the framework side again.
        val selfReferencing = sign(envelope(checkpointId = "checkpoint:self", parentCheckpointId = "checkpoint:self"))
        val blank = sign(envelope(parentCheckpointId = "   "))
        val legitimate = sign(envelope(parentCheckpointId = "checkpoint:attempt-1"))

        assertNull(ConstraintProgrammingCheckpointCodec.decode(selfReferencing).value)
        assertNull(ConstraintProgrammingCheckpointCodec.decode(blank).value)
        assertNotNull(ConstraintProgrammingCheckpointCodec.decode(legitimate).value)
        assertNull(PortableCheckpointCodec.decodeOrNull(selfReferencing))
        assertNull(PortableCheckpointCodec.decodeOrNull(blank))
        assertNotNull(PortableCheckpointCodec.decodeOrNull(legitimate))
    }

    private companion object {
        /**
         * 规范编码的最小 CP snapshot。 / The canonically encoded minimal CP snapshot.
         *
         * 字段顺序与默认值必须与 `ConstraintProgrammingSnapshotCodec` 的 `SnapshotPayload` 声明一致，
         * 这样 `canonicalize` 的输出与本文本逐字相同，声明的 `modelFingerprint` 才能被 core 复算出。
         * Field order and defaults must match the declaration of `ConstraintProgrammingSnapshotCodec`'s
         * `SnapshotPayload`, so `canonicalize` reproduces this text verbatim and core recomputes the
         * declared `modelFingerprint`.
         */
        const val CODEC_TEST_SNAPSHOT: String =
            "{\"schema\":1,\"name\":\"codec-test\",\"objectCategory\":\"Minimum\",\"variables\":[]," +
                "\"intervals\":[],\"expressions\":[],\"constraints\":[],\"objectives\":[]," +
                "\"constraintGroups\":[],\"identitySchemaVersion\":\"1.0\"," +
                "\"identityNamespace\":\"model-local\"}"
    }
}
