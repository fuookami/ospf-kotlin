package fuookami.ospf.kotlin.core.solver

import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointCodec
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointEnvelope
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointRestore
import fuookami.ospf.kotlin.core.solver.constraint_programming.FakeConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.PortableCancellationRecord
import fuookami.ospf.kotlin.core.solver.constraint_programming.PortableSolverProvenance
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.CancellationRecord
import fuookami.ospf.kotlin.core.solver.report.CancellationSource
import fuookami.ospf.kotlin.core.variable.IntVar

/**
 * checkpoint 审计链测试。 / Checkpoint audit-chain tests.
 *
 * Kotlin 侧唯一的可移植 checkpoint codec 是 CP checkpoint v2 envelope，因此本测试覆盖它真实具备的
 * 审计能力：身份与父 checkpoint 链接跨序列化边界保持、配置/求解器/模型指纹不匹配拒绝、
 * 完整性摘要篡改检测，以及可选启用的父链一致性校验。Rust 侧存在而 Kotlin 侧仍然缺失的取消链校验，
 * 在文件末尾以记录现状的形式单独标注。
 * The only portable checkpoint codec on the Kotlin side is the CP checkpoint v2 envelope, so these
 * tests cover the audit capabilities it really has: identity and parent-link survival across the
 * serialization boundary, fingerprint mismatch rejection, integrity-digest tamper detection, and
 * opt-in parent-chain consistency validation. The cancellation-chain validation that exists only in
 * Rust is characterized separately at the end of this file.
 */
class SolveCheckpointAuditTest {
    /**
     * 验证 checkpoint 身份、父 checkpoint 链接与 incumbent 跨序列化边界保持不变。
     * Verifies that checkpoint identity, the parent checkpoint link, and the incumbent survive the
     * serialization boundary and a follow-up restore.
     */
    @Test
    fun checkpointIdentityParentLinkAndIncumbentSurviveTheSerializationBoundary() = runBlocking {
        val model = auditModel("checkpoint-audit-identity")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val solved = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(solver.solve(model)).value
            )
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:attempt-2",
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                incumbent = solved.solution,
                attemptId = "attempt:2",
                parentCheckpointId = "checkpoint:attempt-1"
            )
            val encoded = assertIs<Ok<String, *, *>>(
                ConstraintProgrammingCheckpointCodec.encode(captured)
            ).value
            val decoded = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(encoded)
            ).value

            assertEquals("run:1", decoded.runId)
            assertEquals("attempt:2", decoded.attemptId)
            assertEquals("checkpoint:attempt-1", decoded.parentCheckpointId)
            assertEquals("checkpoint:attempt-2", decoded.checkpointId)
            assertEquals(captured.modelFingerprint, decoded.modelFingerprint)
            assertEquals(captured.configurationFingerprint, decoded.configurationFingerprint)
            assertEquals(captured.solverFingerprint, decoded.solverFingerprint)
            assertEquals(captured.incumbent, assertNotNull(decoded.incumbent))

            val restored = assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(
                    envelope = decoded,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = assertNotNull(decoded.solverFingerprint)
                )
            ).value

            assertEquals(decoded.attemptId, restored.envelope.attemptId)
            assertEquals(decoded.parentCheckpointId, restored.envelope.parentCheckpointId)
            assertEquals(decoded.modelFingerprint, restored.envelope.modelFingerprint)
            assertEquals(solved.solution, restored.incumbent)
        } finally {
            model.close()
        }
    }

    /**
     * 验证恢复时缺失、空白或与 checkpoint 不一致的配置/求解器指纹都会被拒绝。
     * Verifies that missing, blank, or mismatched configuration and solver fingerprints are rejected
     * on restore.
     */
    @Test
    fun restoreRejectsMismatchedAndBlankConfigurationAndSolverFingerprints() {
        val model = auditModel("checkpoint-audit-fingerprints")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:fingerprints",
                configurationFingerprint = CONFIGURATION_FINGERPRINT
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)

            assertIs<Ok<*, *, *>>(
                restoreEnvelope(captured, snapshot, CONFIGURATION_FINGERPRINT, solverFingerprint)
            )
            assertFailedWith(
                restoreEnvelope(captured, snapshot, "configuration:run-2", solverFingerprint),
                "配置指纹缺失或不匹配"
            )
            assertFailedWith(
                restoreEnvelope(captured, snapshot, CONFIGURATION_FINGERPRINT, "solver:other"),
                "求解器指纹缺失或不匹配"
            )
            assertFailedWith(
                restoreEnvelope(captured, snapshot, "", solverFingerprint),
                "必须提供当前配置和求解器指纹"
            )
            assertFailedWith(
                restoreEnvelope(captured, snapshot, CONFIGURATION_FINGERPRINT, ""),
                "必须提供当前配置和求解器指纹"
            )
            assertFailedWith(
                restoreEnvelope(
                    envelope = captured.copy(solverFingerprint = " "),
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = "solver:expected"
                ),
                "求解器指纹缺失或不匹配"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证未重新签名的 checkpoint 篡改会被完整性摘要拒绝，而同一份未篡改 envelope 可正常恢复。
     * Verifies that a checkpoint tampered without re-signing is rejected by the integrity digest,
     * while the untampered envelope still restores.
     */
    @Test
    fun tamperedCheckpointIdentityIsRejectedByTheIntegrityDigest() {
        val model = auditModel("checkpoint-audit-tamper")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:tamper",
                configurationFingerprint = CONFIGURATION_FINGERPRINT
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)

            assertIs<Ok<*, *, *>>(
                restoreEnvelope(captured, snapshot, CONFIGURATION_FINGERPRINT, solverFingerprint)
            )
            assertFailedWith(
                restoreEnvelope(
                    envelope = captured.copy(runId = "run:tampered"),
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint
                ),
                "完整性摘要"
            )
            assertFailedWith(
                restoreEnvelope(
                    envelope = captured.copy(checkpointId = "checkpoint:tampered"),
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint
                ),
                "完整性摘要"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证父链校验默认关闭时父链仍被原样携带，既有调用方行为不变。
     *
     * 语义变化说明：`restore` 过去从不读取 `parentCheckpointId`，被替换或自引用的父链接在重新签名后会被
     * 静默接受；现在它提供可选参数 `validateParentLink`（默认 `false`）与
     * `expectedParentCheckpointId`（默认 `null`）。二者都未启用时保持历史行为，因此本用例断言父链被
     * 原样携带；一旦启用，则会拒绝自引用、空白父标识以及与调用方期望不符的父链（含 envelope 完全没有
     * 父标识的情况）。Rust 侧 `validate_resume_from` / `validate_resumed_child` 是同一语义的参照。
     *
     * Verifies that with parent-chain validation disabled by default the parent link is still carried
     * verbatim, so existing callers keep their historical behavior.
     *
     * Semantics change: `restore` used to ignore `parentCheckpointId`, so a replaced or
     * self-referencing parent link was silently accepted once re-signed. It now exposes the optional
     * `validateParentLink` (default `false`) and `expectedParentCheckpointId` (default `null`)
     * parameters. With neither enabled the historical behavior is preserved, so this case asserts the
     * parent link is carried verbatim. When enabled, self-reference (`parentCheckpointId ==
     * checkpointId`), a blank parent identifier (`"   "`) and a parent that disagrees with the
     * caller's expectation, including a missing parent link, are rejected. Rust's
     * `validate_resume_from` / `validate_resumed_child` are the semantic reference.
     */
    @Test
    fun parentCheckpointLinkIsCarriedWhenValidationIsDisabled() {
        val model = auditModel("checkpoint-audit-parent-chain-carried")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:attempt-2",
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                parentCheckpointId = "checkpoint:attempt-1"
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)

            val carried = assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(captured, snapshot, CONFIGURATION_FINGERPRINT, solverFingerprint)
            ).value
            assertEquals("checkpoint:attempt-1", carried.envelope.parentCheckpointId)
        } finally {
            model.close()
        }
    }

    /**
     * 验证启用父链校验后自引用的父 checkpoint 链接会被拒绝。
     *
     * `capture` 现在已在捕获侧拒绝自引用父链，因此本用例通过“先捕获合法 envelope、再改写父
     * 标识并重新签名”来复现一个已落盘的损坏 checkpoint——这正是恢复侧必须独立拦截的场景，
     * 与 Rust `validate` 拒绝“父 attempt 等于当前 attempt”的语义对齐。
     *
     * Verifies that a self-referencing parent checkpoint link is rejected once parent-chain
     * validation is enabled. `capture` now rejects a self-referencing parent at capture time, so
     * this test reproduces an already-persisted corrupt checkpoint by capturing a valid envelope,
     * then rewriting the parent identifier and re-signing it — exactly the case the restore side
     * must still catch on its own. It aligns with Rust's `validate`, which rejects a parent attempt
     * equal to the current attempt.
     */
    @Test
    fun parentChainValidationRejectsSelfReferencingParentCheckpoint() {
        val model = auditModel("checkpoint-audit-parent-self")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:self",
                configurationFingerprint = CONFIGURATION_FINGERPRINT
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)
            val forged = ConstraintProgrammingCheckpointCodec.withIntegrity(
                captured.copy(parentCheckpointId = captured.checkpointId)
            )

            assertFailedWith(
                restoreEnvelope(
                    envelope = forged,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    validateParentLink = true
                ),
                "父标识不能等于自身标识"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证捕获侧即拒绝自引用父链。
     *
     * 只在恢复侧拦截是不够的：那样损坏的 envelope 会先被持久化，直到恢复时才暴露。
     * `capture` 必须在写入前拒绝。
     *
     * Verifies that a self-referencing parent link is rejected at capture time. Restore-side
     * validation alone is insufficient: the corrupt envelope would be persisted first and only
     * surface later, on recovery. `capture` must reject it before writing.
     */
    @Test
    fun captureRejectsSelfReferencingParentCheckpoint() {
        val model = auditModel("checkpoint-audit-capture-self")
        try {
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value

            val result = ConstraintProgrammingCheckpointCodec.capture(
                snapshot = snapshot,
                descriptor = FakeConstraintProgrammingSolver().descriptor,
                checkpointId = "checkpoint:self",
                createdAtEpochMs = 1L,
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                parentCheckpointId = "checkpoint:self"
            )

            assertFailedWith(result, "父标识不能等于自身标识")
        } finally {
            model.close()
        }
    }

    /**
     * 验证捕获侧即拒绝空白父链。 / Verifies that a blank parent link is rejected at capture time.
     */
    @Test
    fun captureRejectsBlankParentCheckpoint() {
        val model = auditModel("checkpoint-audit-capture-blank")
        try {
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value

            val result = ConstraintProgrammingCheckpointCodec.capture(
                snapshot = snapshot,
                descriptor = FakeConstraintProgrammingSolver().descriptor,
                checkpointId = "checkpoint:attempt-2",
                createdAtEpochMs = 1L,
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                parentCheckpointId = "   "
            )

            assertFailedWith(result, "父标识不能为空白")
        } finally {
            model.close()
        }
    }

    /**
     * 验证启用父链校验后空白父 checkpoint 标识会被拒绝。
     *
     * Rust 侧 `validate` 拒绝空白父 attempt 标识，本用例对齐该语义：仅含空白的父标识不能被当作
     * “无父链”而放行。
     *
     * Verifies that a blank parent checkpoint identifier is rejected once parent-chain validation is
     * enabled. Rust's `validate` rejects a blank parent attempt identifier; a whitespace-only parent
     * must not be treated as a missing parent link.
     */
    @Test
    fun parentChainValidationRejectsBlankParentCheckpoint() {
        val model = auditModel("checkpoint-audit-parent-blank")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:attempt-2",
                configurationFingerprint = CONFIGURATION_FINGERPRINT
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)
            val forged = ConstraintProgrammingCheckpointCodec.withIntegrity(
                captured.copy(parentCheckpointId = "   ")
            )

            assertFailedWith(
                restoreEnvelope(
                    envelope = forged,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    validateParentLink = true
                ),
                "父标识不能为空白"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证调用方声明期望父标识后，被替换的父链会被拒绝。
     *
     * Rust 侧 `validate_resume_from` 要求 checkpoint 的父身份与恢复请求一次性匹配，本用例对齐该语义：
     * 期望 `checkpoint:attempt-1` 时，重新签名的 `checkpoint:forged` 父链必须被拒绝。
     *
     * Verifies that a replaced parent link is rejected once the caller declares an expected parent
     * identifier. Rust's `validate_resume_from` requires the checkpoint parent identity to match the
     * resume request, so an expectation of `checkpoint:attempt-1` must reject the re-signed
     * `checkpoint:forged` parent link.
     */
    @Test
    fun parentChainValidationRejectsParentThatDiffersFromTheExpectedParent() {
        val model = auditModel("checkpoint-audit-parent-forged")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:attempt-2",
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                parentCheckpointId = "checkpoint:attempt-1"
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)

            val forged = ConstraintProgrammingCheckpointCodec.withIntegrity(
                captured.copy(parentCheckpointId = "checkpoint:forged")
            )
            assertFailedWith(
                restoreEnvelope(
                    envelope = forged,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedParentCheckpointId = "checkpoint:attempt-1"
                ),
                "父标识与恢复请求不一致"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证声明了期望父标识时，完全没有父标识的 envelope 同样被拒绝。
     *
     * 这是“父标识与期望不符”路径中缺失父链的显式覆盖：`expectedParentCheckpointId` 非 null 会同时启用
     * 父链校验，因此 envelope 没有父标识不能被当成“无需校验”而放行。
     *
     * Verifies that when an expected parent identifier is declared, an envelope carrying no parent at
     * all is rejected as well. A non-null `expectedParentCheckpointId` also enables parent-chain
     * validation, so a missing parent link must not be treated as "nothing to validate".
     */
    @Test
    fun parentChainValidationRejectsMissingParentWhenExpectedParentIsDeclared() {
        val model = auditModel("checkpoint-audit-parent-missing")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:attempt-2",
                configurationFingerprint = CONFIGURATION_FINGERPRINT
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)

            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedParentCheckpointId = "checkpoint:attempt-1"
                ),
                "父标识与恢复请求不一致"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证解码入口同样拒绝自引用父链。
     *
     * 父链校验此前只存在于 `restore`；若下游把 `decode`/`decodeCompatible` 当作审计入口，
     * 伪造的父链只要携带正确摘要即可通过。解码是第二条必须独立设防的进路。
     *
     * Verifies that the decode entry point also rejects a self-referencing parent link. Parent-chain
     * validation previously lived only in `restore`; if a downstream treats `decode` or
     * `decodeCompatible` as its audit entry, a forged parent link passed as long as its digest was
     * correct. Decoding is a second path that must defend itself independently.
     */
    @Test
    fun decodeRejectsSelfReferencingParentCheckpoint() {
        val model = auditModel("checkpoint-audit-decode-self")
        try {
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = FakeConstraintProgrammingSolver(),
                snapshot = snapshot,
                checkpointId = "checkpoint:self",
                configurationFingerprint = CONFIGURATION_FINGERPRINT
            )
            val encoded = assertIs<Ok<String, *, *>>(
                ConstraintProgrammingCheckpointCodec.encode(
                    ConstraintProgrammingCheckpointCodec.withIntegrity(
                        captured.copy(parentCheckpointId = captured.checkpointId)
                    )
                )
            ).value

            assertFailedWith(ConstraintProgrammingCheckpointCodec.decode(encoded), "父标识不能等于自身标识")
            assertFailedWith(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(encoded),
                "父标识不能等于自身标识"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证解码入口同样拒绝空白父链。 / Verifies that the decode entry point also rejects a blank parent link.
     */
    @Test
    fun decodeRejectsBlankParentCheckpoint() {
        val model = auditModel("checkpoint-audit-decode-blank")
        try {
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = FakeConstraintProgrammingSolver(),
                snapshot = snapshot,
                checkpointId = "checkpoint:attempt-2",
                configurationFingerprint = CONFIGURATION_FINGERPRINT
            )
            val encoded = assertIs<Ok<String, *, *>>(
                ConstraintProgrammingCheckpointCodec.encode(
                    ConstraintProgrammingCheckpointCodec.withIntegrity(
                        captured.copy(parentCheckpointId = "   ")
                    )
                )
            ).value

            assertFailedWith(ConstraintProgrammingCheckpointCodec.decode(encoded), "父标识不能为空白")
            assertFailedWith(
                ConstraintProgrammingCheckpointCodec.decodeCompatible(encoded),
                "父标识不能为空白"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证运行标识不一致时恢复被拒绝。
     *
     * Rust `validate_resume_from` 要求 runId 与 attemptId 一次性匹配；Kotlin 的 `restore` 此前
     * 从不比对这两个字段，因此"用另一个运行的 checkpoint 恢复当前运行"无法被发现。
     *
     * Verifies that restoring with a mismatched run identity is rejected. Rust's
     * `validate_resume_from` requires runId and attemptId to match in one pass; Kotlin's `restore`
     * previously never compared them, so resuming a run from another run's checkpoint went
     * undetected.
     */
    @Test
    fun restoreRejectsMismatchedRunIdentity() {
        val model = auditModel("checkpoint-audit-run-identity")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:attempt-2",
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                runId = "run:1",
                attemptId = "attempt:1"
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)

            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedRunId = "run:other"
                ),
                "运行标识与恢复请求不一致"
            )
            // 对照：匹配的 runId 必须放行，证明拒绝来自比对而非恒失败。
            // Control: a matching runId must be accepted, proving the rejection comes from the
            // comparison rather than failing unconditionally.
            assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedRunId = "run:1"
                )
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证尝试标识不一致时恢复被拒绝。 / Verifies that restoring with a mismatched attempt identity is rejected.
     */
    @Test
    fun restoreRejectsMismatchedAttemptIdentity() {
        val model = auditModel("checkpoint-audit-attempt-identity")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:attempt-2",
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                runId = "run:1",
                attemptId = "attempt:1"
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)

            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedAttemptId = "attempt:other"
                ),
                "尝试标识与恢复请求不一致"
            )
            assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedAttemptId = "attempt:1"
                )
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证声明空白身份期望值时被拒绝，而不是被当作"匹配成功"。
     *
     * 若调用方传入空串，`envelope.runId != ""` 对正常 envelope 恰好成立而"看似"报不匹配；
     * 但对本身 runId 为 null 的 envelope 就会误放行。因此空白期望值必须显式拒绝。
     *
     * Verifies that a blank identity expectation is rejected rather than treated as a successful
     * match. With an empty string, `envelope.runId != ""` may coincidentally look like a mismatch
     * for ordinary envelopes but would wrongly pass one whose runId is null. A blank expectation
     * must therefore be rejected explicitly.
     */
    @Test
    fun restoreRejectsBlankIdentityExpectations() {
        val model = auditModel("checkpoint-audit-blank-identity")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:attempt-2",
                configurationFingerprint = CONFIGURATION_FINGERPRINT
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)

            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedRunId = "   "
                ),
                "运行标识不能为空白"
            )
            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedAttemptId = "   "
                ),
                "尝试标识不能为空白"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证不传身份期望值时，既有调用行为完全不变（向后兼容）。
     *
     * runId/attemptId 校验为显式启用；省略这两个参数时不应引入任何新拒绝路径。
     *
     * Verifies that omitting the identity expectations leaves existing call behavior unchanged
     * (backward compatibility). runId/attemptId validation is explicitly opt-in; omitting both
     * parameters must not introduce any new rejection path.
     */
    @Test
    fun identityValidationIsSkippedWhenExpectationsAreOmitted() {
        val model = auditModel("checkpoint-audit-identity-default")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            // runId/attemptId 故意留空，若默认启用比对则会被拒绝。
            // runId/attemptId are deliberately absent; default-on comparison would reject them.
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:attempt-2",
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                runId = null,
                attemptId = null
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)

            assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint
                )
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证乱序的取消链被拒绝，而有序链正常放行。
     *
     * 取消链表示因果顺序，Rust `SolveCheckpoint::validate` 以"时间戳单调不减"无条件校验它；一条乱序
     * 的链意味着审计轨迹被篡改或拼接错误。链的顺序校验与父链一样在恢复侧无条件生效，因此本用例通过
     * "先捕获合法 envelope、再改写链并重新签名"复现已落盘的损坏 checkpoint。
     *
     * Verifies that a misordered cancellation chain is rejected while an ordered chain is accepted.
     * A cancellation chain expresses causal order and Rust's `SolveCheckpoint::validate` checks
     * non-decreasing timestamps unconditionally; an out-of-order chain means the audit trail was
     * tampered with or mis-spliced. Like the parent link, chain ordering is validated unconditionally on
     * restore, so this test reproduces an already-persisted corrupt checkpoint by capturing a valid
     * envelope, rewriting the chain, and re-signing it.
     */
    @Test
    fun cancellationChainMustBeOrderedAndAnOrderedChainIsAccepted() {
        val model = auditModel("checkpoint-audit-cancellation-order")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:cancellation-order",
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                cancellationChain = listOf(
                    cancellationRecord(CancellationSource.Caller, 1_000L, "caller"),
                    cancellationRecord(CancellationSource.Remote, 1_002L, "peer stop")
                )
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)

            // 对照：有序链必须放行，证明拒绝来自顺序校验而非恒失败。
            // Control: an ordered chain must be accepted, proving the rejection comes from the ordering
            // check rather than failing unconditionally.
            assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(captured, snapshot, CONFIGURATION_FINGERPRINT, solverFingerprint)
            )

            // 相等的时间戳是合法的"单调不减"，不得被误判为乱序。
            // Equal timestamps are legitimately "non-decreasing" and must not be mistaken for disorder.
            val equalTimestamps = ConstraintProgrammingCheckpointCodec.withIntegrity(
                captured.copy(
                    cancellationChain = listOf(
                        wireCancellationRecord(CancellationSource.Caller, 1_000L, "caller"),
                        wireCancellationRecord(CancellationSource.Remote, 1_000L, "peer stop")
                    )
                )
            )
            assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(equalTimestamps, snapshot, CONFIGURATION_FINGERPRINT, solverFingerprint)
            )

            val misordered = ConstraintProgrammingCheckpointCodec.withIntegrity(
                captured.copy(
                    cancellationChain = listOf(
                        wireCancellationRecord(CancellationSource.Remote, 1_002L, "peer stop"),
                        wireCancellationRecord(CancellationSource.Caller, 1_000L, "caller")
                    )
                )
            )
            assertFailedWith(
                restoreEnvelope(misordered, snapshot, CONFIGURATION_FINGERPRINT, solverFingerprint),
                "取消链顺序无效"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证捕获侧即拒绝乱序的取消链。
     *
     * 只在恢复侧拦截是不够的：乱序链会先被持久化。`capture` 必须在写入前拒绝，与父链一致。
     *
     * Verifies that a misordered cancellation chain is rejected at capture time. Restore-side validation
     * alone is insufficient: the misordered chain would be persisted first. `capture` must reject it
     * before writing, exactly as it does for the parent link.
     */
    @Test
    fun captureRejectsMisorderedCancellationChain() {
        val model = auditModel("checkpoint-audit-capture-cancellation-order")
        try {
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value

            val result = ConstraintProgrammingCheckpointCodec.capture(
                snapshot = snapshot,
                descriptor = FakeConstraintProgrammingSolver().descriptor,
                checkpointId = "checkpoint:cancellation-order",
                createdAtEpochMs = 1L,
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                cancellationChain = listOf(
                    cancellationRecord(CancellationSource.Remote, 1_002L, "peer stop"),
                    cancellationRecord(CancellationSource.Caller, 1_000L, "caller")
                )
            )

            assertFailedWith(result, "取消链顺序无效")
        } finally {
            model.close()
        }
    }

    /**
     * 验证恢复请求声明的期望取消链必须是 envelope 链的前缀。
     *
     * Rust `validate_resume_from` 只要求 envelope 链**以期望链为前缀**：恢复请求通常只声明自己已知的
     * 那段因果历史，envelope 上更晚发生的取消是合法的。长度不足或前缀不符都必须拒绝。
     *
     * Verifies that a declared expected cancellation chain must be a prefix of the envelope's chain. Rust's
     * `validate_resume_from` only requires the envelope's chain to **carry the expected chain as a
     * prefix**: a resume request usually declares just the causal history it knows about, and
     * cancellations that happened later in the envelope are legitimate. A shorter envelope chain or a
     * mismatched prefix must both be rejected.
     */
    @Test
    fun expectedCancellationChainMustBeAPrefixOfTheEnvelopeChain() {
        val model = auditModel("checkpoint-audit-expected-cancellation")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:expected-cancellation",
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                cancellationChain = listOf(
                    cancellationRecord(CancellationSource.Caller, 1_000L, "caller"),
                    cancellationRecord(CancellationSource.Remote, 1_002L, "peer stop")
                )
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)
            val expectedPrefix = listOf(
                wireCancellationRecord(CancellationSource.Caller, 1_000L, "caller")
            )

            // 对照：恰好是前缀（含全等与真前缀两种情形）必须放行。
            // Control: an actual prefix (both the full-chain and strict-prefix cases) must be accepted.
            assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedCancellationChain = expectedPrefix
                )
            )
            assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedCancellationChain = captured.cancellationChain
                )
            )

            // 期望链比 envelope 链长：必须拒绝。
            // The expected chain is longer than the envelope's chain: must be rejected.
            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedCancellationChain = captured.cancellationChain +
                        wireCancellationRecord(CancellationSource.Timeout, 1_003L, "deadline")
                ),
                "取消链未保留期望前缀"
            )

            // 前缀内容不符：必须拒绝。
            // The prefix content differs: must be rejected.
            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedCancellationChain = listOf(
                        wireCancellationRecord(CancellationSource.Remote, 1_000L, "caller")
                    )
                ),
                "取消链未保留期望前缀"
            )

            // 不传期望链时不得引入任何新拒绝路径（向后兼容）。
            // Omitting the expected chain must not introduce any new rejection path (backward compatible).
            assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(captured, snapshot, CONFIGURATION_FINGERPRINT, solverFingerprint)
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证恢复请求声明的期望 provenance 必须与 envelope 的 provenance 整体相同。
     *
     * Rust `validate_resume_internal` 在指纹比对之后整体比对 provenance；本用例同时覆盖"身份为空的
     * 期望 provenance 必须被显式拒绝"，避免它只以误导性的"不一致"理由失败。
     *
     * Verifies that a declared expected provenance must match the envelope's provenance in full. Rust's
     * `validate_resume_internal` compares provenance in full after the fingerprint comparisons; this test
     * also covers "a blank-identity expected provenance must be rejected explicitly" so it does not fail
     * only with the misleading reason "does not match".
     */
    @Test
    fun expectedProvenanceMustMatchTheEnvelopeProvenance() {
        val model = auditModel("checkpoint-audit-expected-provenance")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val provenance = PortableSolverProvenance(
                solverId = "solver:audit",
                backendName = "audit-backend",
                backendVersion = "1.0",
                requestedConfiguration = linkedMapOf("threads" to "4", "seed" to "7"),
                effectiveConfiguration = linkedMapOf("threads" to "4"),
                threadCount = 4,
                randomSeed = 7L,
                deterministic = true,
                environmentSummary = linkedMapOf("os" to "windows")
            )
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:expected-provenance",
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                provenance = provenance
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)

            // 对照：全等的期望 provenance 必须放行（含乱序插入的 map，因为编码已按键升序规范化）。
            // Control: an identical expected provenance must be accepted, including out-of-order maps,
            // because the encoding canonicalizes keys into ascending order.
            assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedProvenance = provenance
                )
            )

            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedProvenance = provenance.copy(threadCount = 8)
                ),
                "provenance 与恢复请求不一致"
            )
            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedProvenance = provenance.copy(backendName = "other-backend")
                ),
                "provenance 与恢复请求不一致"
            )

            // 身份为空的期望 provenance 必须被显式拒绝，而不是以"不一致"失败。
            // A blank-identity expected provenance must be rejected explicitly rather than failing as
            // "does not match".
            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedProvenance = PortableSolverProvenance(solverId = "  ", backendName = "audit-backend")
                ),
                "恢复请求的 provenance 身份不能为空白"
            )
            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedProvenance = PortableSolverProvenance(solverId = "solver:audit", backendName = "")
                ),
                "恢复请求的 provenance 身份不能为空白"
            )

            // 不传期望 provenance 时不得引入任何新拒绝路径（向后兼容）。
            // Omitting the expected provenance must not introduce any new rejection path (backward
            // compatible).
            assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(captured, snapshot, CONFIGURATION_FINGERPRINT, solverFingerprint)
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证 envelope 完全没有 provenance 时，期望 provenance 仍然按"整体相同"拒绝。
     *
     * 缺失与空对象是两种不同状态：Rust 侧 `Option<SolverProvenance>` 的 `None` 与 `Some(默认值)`
     * 不相等，因此期望一份非空 provenance 而 envelope 为 null 时必须拒绝，不能被当作"无需校验"。
     *
     * Verifies that when the envelope carries no provenance at all, an expected provenance is still
     * rejected under "must match in full". Missing and empty are distinct states: on the Rust side
     * `Option<SolverProvenance>`'s `None` and `Some(default)` are not equal, so expecting a non-null
     * provenance while the envelope carries null must be rejected rather than treated as "nothing to
     * compare".
     */
    @Test
    fun expectedProvenanceIsRejectedWhenTheEnvelopeCarriesNone() {
        val model = auditModel("checkpoint-audit-missing-provenance")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:missing-provenance",
                configurationFingerprint = CONFIGURATION_FINGERPRINT
            )
            val solverFingerprint = assertNotNull(captured.solverFingerprint)
            assertEquals(null, captured.provenance)

            assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(captured, snapshot, CONFIGURATION_FINGERPRINT, solverFingerprint)
            )
            assertFailedWith(
                restoreEnvelope(
                    envelope = captured,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedProvenance = PortableSolverProvenance(
                        solverId = "solver:audit",
                        backendName = "audit-backend"
                    )
                ),
                "provenance 与恢复请求不一致"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 构造一条带时间戳的取消事实。 / Build one cancellation fact with an explicit timestamp.
     *
     * @param source 取消来源 / Cancellation source
     * @param requestedAtEpochMs 取消请求时间戳（epoch 毫秒） / Cancellation request timestamp in epoch milliseconds
     * @param reason 取消原因 / Cancellation reason
     * @return 取消事实 / The cancellation fact
     */
    private fun cancellationRecord(
        source: CancellationSource,
        requestedAtEpochMs: Long,
        reason: String? = null
    ): CancellationRecord {
        return CancellationRecord(
            source = source,
            requestedAt = Instant.ofEpochMilli(requestedAtEpochMs),
            reason = reason
        )
    }

    /** 构造与 envelope 中某一项相同的线格式取消记录。 / Build the wire cancellation record matching an item of the envelope. */
    private fun wireCancellationRecord(
        source: CancellationSource,
        requestedAtEpochMs: Long,
        reason: String? = null
    ): PortableCancellationRecord {
        return PortableCancellationRecord(
            origin = source.toWireCode(),
            requestedAtEpochMs = requestedAtEpochMs,
            reason = reason
        )
    }

    /**
     * 验证历史 bound / gap 的数值语义与 Rust 一致。
     *
     * Kotlin 过去把 `bestBound`/`gap` 当作**不透明字符串**，因此一条自相矛盾的审计记录
     * （负 gap、gap 与 incumbent/界不符、甚至只带 bound 的 gap）都会被静默接受。Rust
     * `SolveCheckpoint::validate` 对这三点都有校验，本用例把两侧对齐。
     *
     * Verifies that the historical bound and gap carry the same numeric semantics as Rust. Kotlin
     * previously treated `bestBound`/`gap` as **opaque strings**, so a self-contradictory audit
     * record — a negative gap, a gap inconsistent with the incumbent/bound, or even a gap with only
     * a bound — was silently accepted. Rust's `SolveCheckpoint::validate` checks all three, and this
     * test aligns the two sides.
     */
    @Test
    fun historicalBoundsMustBeNumericallyConsistent() = runBlocking {
        val model = auditModel("checkpoint-audit-bounds")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val solved = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(solver.solve(model)).value
            )
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val incumbent = solved.solution

            // 目标值由 envelope 依据 snapshot 的目标表达式推导，因此先做一次不带界与 gap 的捕获，
            // 取回 Kotlin 自己算出的目标值文本，再据此构造三元组。
            //
            // The objective is derived by the envelope from the snapshot's objective expression, so
            // first capture without a bound or gap and read back the objective text Kotlin computed;
            // the triples below are then built from it.
            val baseline = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:bounds-baseline",
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                incumbent = incumbent
            )
            val objective = assertNotNull(baseline.incumbent?.objective)

            // 对照：自洽的三元组必须被接受。objective == bound 时 gap 必须为 0。
            // Control: a self-consistent triple must be accepted; with objective == bound the gap must be 0.
            assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                captureRaw(
                    solver = solver,
                    snapshot = snapshot,
                    checkpointId = "checkpoint:bounds-ok",
                    incumbent = incumbent,
                    bestBound = objective,
                    gap = "0"
                )
            )

            // 负 gap 必须被拒绝。 / A negative gap must be rejected.
            assertFailedWith(
                captureRaw(
                    solver = solver,
                    snapshot = snapshot,
                    checkpointId = "checkpoint:bounds-negative",
                    incumbent = incumbent,
                    bestBound = objective,
                    gap = "-0.1"
                ),
                "相对 gap 不能为负"
            )

            // gap 与 incumbent/界不符必须被拒绝。 / A gap inconsistent with the incumbent/bound must be rejected.
            assertFailedWith(
                captureRaw(
                    solver = solver,
                    snapshot = snapshot,
                    checkpointId = "checkpoint:bounds-mismatch",
                    incumbent = incumbent,
                    bestBound = objective,
                    gap = "0.5"
                ),
                "相对 gap 与 incumbent 及最佳界不一致"
            )

            // 只带 bound、没有 incumbent 的 gap 必须被拒绝（这是相对旧行为的收紧）。
            // A gap with only a bound and no incumbent must be rejected (a tightening over the old behavior).
            assertFailedWith(
                captureRaw(
                    solver = solver,
                    snapshot = snapshot,
                    checkpointId = "checkpoint:bounds-gap-only",
                    bestBound = objective,
                    gap = "0"
                ),
                "相对 gap 需要同时提供 incumbent 目标值与最佳界"
            )

            // 非数值文本必须被拒绝，而不是被当成"无信息"。
            // Non-numeric text must be rejected rather than treated as "no information".
            assertFailedWith(
                captureRaw(
                    solver = solver,
                    snapshot = snapshot,
                    checkpointId = "checkpoint:bounds-nan",
                    incumbent = incumbent,
                    bestBound = objective,
                    gap = "NaN"
                ),
                "不是有效数值"
            )
            assertFailedWith(
                captureRaw(
                    solver = solver,
                    snapshot = snapshot,
                    checkpointId = "checkpoint:bounds-infinity",
                    incumbent = incumbent,
                    bestBound = "Infinity",
                    gap = "0"
                ),
                "不是有效数值"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 验证恢复链的父子关系校验与 fork 语义与 Rust 一致。
     *
     * Rust 的 `validate_resumed_child` / `fork_for_resume` 保证"恢复出的子 attempt 必须指向源
     * attempt、保持身份、且不丢失取消历史"。Kotlin 此前没有对应入口，因此一次串号或丢历史的恢复
     * 无法被发现。本用例把两者对齐。
     *
     * Verifies that resumed-chain parent/child validation and fork semantics match Rust. Rust's
     * `validate_resumed_child` / `fork_for_resume` guarantee that a resumed child attempt points at its
     * source, preserves identity, and does not lose cancellation history. Kotlin had no equivalent, so
     * a cross-run or history-losing resume went undetected. This test aligns the two.
     */
    @Test
    fun resumedChildMustPointAtItsSourceAndPreserveIdentityAndHistory() {
        val model = auditModel("checkpoint-audit-fork")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val parent = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:attempt-1",
                configurationFingerprint = CONFIGURATION_FINGERPRINT,
                runId = "run:1",
                attemptId = "attempt:1",
                cancellationChain = listOf(
                    CancellationRecord(
                        source = CancellationSource.Remote,
                        requestedAt = Instant.ofEpochMilli(1_700_000_000_001L),
                        reason = "operator stopped the attempt"
                    )
                ),
                provenance = contractProvenance()
            )

            // 对照：fork 必须成功，并把父链接指向源、保留取消历史。
            // Control: forking must succeed, point the parent link at the source, and keep the history.
            val child = assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
                ConstraintProgrammingCheckpointCodec.forkForResume(
                    parent = parent,
                    checkpointId = "checkpoint:attempt-2",
                    createdAtEpochMs = 2_000L,
                    attemptId = "attempt:2"
                )
            ).value
            assertEquals("checkpoint:attempt-1", child.parentCheckpointId)
            assertEquals("attempt:2", child.attemptId)
            assertEquals(parent.cancellationChain, child.cancellationChain)
            assertNull(
                ConstraintProgrammingCheckpointCodec.validateResumedChild(parent, child),
                "合法的子 checkpoint 必须通过校验 / a legitimate child must validate"
            )

            // 串号：子必须属于源运行。 / Cross-run: the child must belong to the source run.
            val otherRun = ConstraintProgrammingCheckpointCodec.withIntegrity(
                child.copy(runId = "run:2")
            )
            assertTrue(
                assertNotNull(
                    ConstraintProgrammingCheckpointCodec.validateResumedChild(parent, otherRun)
                ).contains("不属于源运行")
            )

            // 父链接错误：子必须恰好指向源。 / Wrong parent link: the child must point exactly at the source.
            val wrongParent = ConstraintProgrammingCheckpointCodec.withIntegrity(
                child.copy(parentCheckpointId = "checkpoint:elsewhere")
            )
            assertTrue(
                assertNotNull(
                    ConstraintProgrammingCheckpointCodec.validateResumedChild(parent, wrongParent)
                ).contains("父链接与源 checkpoint 不一致")
            )

            // 身份被换：指纹或 provenance 变化必须被发现。
            // Swapped identity: a changed fingerprint or provenance must be caught.
            val swappedIdentity = ConstraintProgrammingCheckpointCodec.withIntegrity(
                child.copy(solverFingerprint = "solver:other")
            )
            assertTrue(
                assertNotNull(
                    ConstraintProgrammingCheckpointCodec.validateResumedChild(parent, swappedIdentity)
                ).contains("未保持源身份")
            )

            // 丢失取消历史：子的链不得短于源，也不得前缀不符。
            // Lost cancellation history: the child chain must not be shorter than or diverge from the source.
            val lostHistory = ConstraintProgrammingCheckpointCodec.withIntegrity(
                child.copy(cancellationChain = emptyList())
            )
            assertTrue(
                assertNotNull(
                    ConstraintProgrammingCheckpointCodec.validateResumedChild(parent, lostHistory)
                ).contains("丢失了源的取消历史")
            )

            // fork 自身不得允许空白标识或与源相同的标识。
            // Forking itself must reject a blank identifier or one equal to the source.
            assertFailedWith(
                ConstraintProgrammingCheckpointCodec.forkForResume(
                    parent = parent,
                    checkpointId = "   ",
                    createdAtEpochMs = 2_000L
                ),
                "fork 出的 checkpoint 标识不能为空白"
            )
            assertFailedWith(
                ConstraintProgrammingCheckpointCodec.forkForResume(
                    parent = parent,
                    checkpointId = parent.checkpointId,
                    createdAtEpochMs = 2_000L
                ),
                "fork 出的 checkpoint 标识不能与源相同"
            )

            // 派生的子必须能独立通过恢复（父链校验开启时，期望父标识即源）。
            // The forked child must restore on its own, with the source as the expected parent.
            val solverFingerprint = assertNotNull(child.solverFingerprint)
            val restored = assertIs<Ok<ConstraintProgrammingCheckpointRestore, *, *>>(
                restoreEnvelope(
                    envelope = child,
                    snapshot = snapshot,
                    expectedConfigurationFingerprint = CONFIGURATION_FINGERPRINT,
                    expectedSolverFingerprint = solverFingerprint,
                    expectedParentCheckpointId = parent.checkpointId
                )
            ).value
            assertEquals("checkpoint:attempt-1", restored.envelope.parentCheckpointId)
        } finally {
            model.close()
        }
    }

    /**
     * 构造一份合法的线格式 provenance。 / Build a legitimate wire-shape provenance.
     */
    private fun contractProvenance(): PortableSolverProvenance = PortableSolverProvenance(
        solverId = "fake/1.0",
        backendName = "fake",
        backendVersion = "1.0"
    )

    /**
     * 验证 state 载荷与 envelope 的绑定：篡改 `snapshotJson` 即使重新签名也必被拒绝。
     *
     * Rust 用 `SolveCheckpointArtifact.state_digest`（state 字节的摘要）把**载荷**绑定到 checkpoint；
     * Kotlin 的 envelope 没有独立的 state 字段，而是用 `modelFingerprint = SHA-256(snapshotJson)`
     * 绑定同一份载荷。本用例证明该等价保证确实成立，因此 `state_digest` 在 Kotlin 侧不是缺失能力，
     * 而是同一保证的不同结构表达。
     *
     * 关键点：仅靠 envelope 摘要不足以说明这一点，因为 envelope 摘要可以合法重算（见 `withIntegrity`
     * 关于"仅完整性、非真实性"的说明）。因此本用例**同时重算 envelope 摘要**，把载荷绑定单独隔离
     * 出来验证。
     *
     * Verifies the state payload's binding to the envelope: tampering with `snapshotJson` must be
     * rejected even after re-signing. Rust binds the **payload** to a checkpoint through
     * `SolveCheckpointArtifact.state_digest` (a digest of the state bytes); the Kotlin envelope has no
     * separate state field and instead binds the same payload through
     * `modelFingerprint = SHA-256(snapshotJson)`. This test proves that equivalent guarantee holds, so
     * `state_digest` is not a missing capability on the Kotlin side but the same guarantee expressed
     * by a different structure.
     *
     * The point matters: the envelope digest alone cannot show this, because that digest may
     * legitimately be recomputed (see `withIntegrity` on "integrity, not authenticity"). This test
     * therefore **also recomputes the envelope digest**, isolating the payload binding for
     * verification.
     */
    @Test
    fun tamperedStatePayloadIsRejectedEvenWhenTheEnvelopeIsReSigned() {
        val model = auditModel("checkpoint-audit-state-binding")
        try {
            val solver = FakeConstraintProgrammingSolver()
            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            val captured = captureEnvelope(
                solver = solver,
                snapshot = snapshot,
                checkpointId = "checkpoint:state-binding",
                configurationFingerprint = CONFIGURATION_FINGERPRINT
            )
            val encoded = assertIs<Ok<String, *, *>>(
                ConstraintProgrammingCheckpointCodec.encode(captured)
            ).value
            val root = Json.parseToJsonElement(encoded).jsonObject.toMutableMap()

            // 改写 snapshotJson 但**保留** modelFingerprint：载荷与指纹不再一致。
            //
            // 篡改必须**保持结构合法**（只改 `name` 的值，不动键名），否则 snapshot 结构校验会先
            // 拦住它，本用例就无法把"载荷绑定"单独隔离出来验证。
            //
            // Rewrite snapshotJson while **keeping** modelFingerprint: payload and fingerprint now
            // disagree.
            //
            // The tampering must stay **structurally valid** (change only the value of `name`, not a key
            // name); otherwise the snapshot's structural validation catches it first and this test can no
            // longer isolate the payload binding.
            val originalSnapshotJson = root.getValue("snapshotJson").jsonPrimitive.content
            val tamperedSnapshotJson = originalSnapshotJson.replace("\"name\":\"", "\"name\":\"tampered-")
            assertTrue(
                tamperedSnapshotJson != originalSnapshotJson,
                "篡改必须命中 snapshotJson 内的一个真实值 / the tampering must hit a real value inside snapshotJson"
            )
            root["snapshotJson"] = JsonPrimitive(tamperedSnapshotJson)
            assertEquals(
                captured.modelFingerprint,
                root.getValue("modelFingerprint").jsonPrimitive.content,
                "本用例必须保留原 modelFingerprint，才能隔离出载荷绑定"
            )

            // 重算 envelope 摘要，使摘要校验**通过**，从而只有载荷绑定能拦住它。
            // Recompute the envelope digest so the digest check **passes**, leaving only the payload
            // binding able to stop it.
            root["integritySha256"] = JsonPrimitive("")
            val unsigned = JsonObject(root)
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(unsigned.toString().toByteArray(Charsets.UTF_8))
                .joinToString(separator = "") { byte -> "%02x".format(byte) }
            root["integritySha256"] = JsonPrimitive(digest)
            val resigned = Json.encodeToString(JsonObject(root))

            assertFailedWith(
                ConstraintProgrammingCheckpointCodec.decode(resigned),
                "snapshot 与模型指纹不一致"
            )
        } finally {
            model.close()
        }
    }

    /**
     * 构造用于审计链测试的最小 CP 模型。 / Build the minimal CP model used by the audit-chain tests.
     *
     * @param name 模型名称 / Model name
     * @return 可行且有小规模整数域区的 CP 模型 / A feasible CP model with a small integer domain
     */
    private fun auditModel(name: String): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel(name, ObjectCategory.Minimum)
        val value = IntVar("$name-value")
        model.registerVariable(value, IntegerDomain.interval(0, 3).value!!)
        val expression = ConstraintProgrammingExpression.Variable(value)
        model.addConstraint(
            constraint = ConstraintProgrammingConstraint.greaterOrEqual(expression, Int64.one).value!!,
            id = "audit-lower-bound"
        )
        model.minimize(expression)
        return model
    }

    /**
     * 捕获一个带有审计身份字段的 v2 envelope。 / Capture a v2 envelope carrying audit identity fields.
     *
     * @param solver 用于生成求解器指纹的 CP 求解器 / CP solver providing the solver fingerprint
     * @param snapshot 当前模型 snapshot / Current model snapshot
     * @param checkpointId checkpoint 标识 / Checkpoint identifier
     * @param configurationFingerprint 生效配置指纹 / Effective configuration fingerprint
     * @param incumbent 可选 incumbent / Optional incumbent
     * @param runId 运行标识 / Run identifier
     * @param attemptId 尝试标识 / Attempt identifier
     * @param parentCheckpointId 父 checkpoint 标识 / Parent checkpoint identifier
     * @param cancellationChain 取消事实链 / Cancellation-fact chain
     * @param provenance 求解器执行来源 / Solver execution provenance
     * @param bestBound 历史最佳界 / Historical best bound
     * @param gap 历史最优间隙 / Historical optimality gap
     * @return 已签名的 v2 envelope / Signed v2 envelope
     */
    private fun captureEnvelope(
        solver: FakeConstraintProgrammingSolver,
        snapshot: ConstraintProgrammingModelSnapshot,
        checkpointId: String,
        configurationFingerprint: String,
        incumbent: ConstraintProgrammingSolution? = null,
        runId: String? = "run:1",
        attemptId: String? = "attempt:1",
        parentCheckpointId: String? = null,
        cancellationChain: List<CancellationRecord> = emptyList(),
        provenance: PortableSolverProvenance? = null,
        bestBound: String? = null,
        gap: String? = null
    ): ConstraintProgrammingCheckpointEnvelope {
        return assertIs<Ok<ConstraintProgrammingCheckpointEnvelope, *, *>>(
            ConstraintProgrammingCheckpointCodec.capture(
                snapshot = snapshot,
                descriptor = solver.descriptor,
                checkpointId = checkpointId,
                createdAtEpochMs = 1_000L,
                incumbent = incumbent,
                configurationFingerprint = configurationFingerprint,
                runId = runId,
                attemptId = attemptId,
                parentCheckpointId = parentCheckpointId,
                cancellationChain = cancellationChain,
                provenance = provenance,
                bestBound = bestBound,
                gap = gap
            )
        ).value
    }

    /**
     * 直接调用 `capture` 并把结果原样返回，用于断言**捕获侧**的拒绝。
     * Call `capture` and return its raw result, for asserting a **capture-side** rejection.
     */
    private fun captureRaw(
        solver: FakeConstraintProgrammingSolver,
        snapshot: ConstraintProgrammingModelSnapshot,
        checkpointId: String,
        incumbent: ConstraintProgrammingSolution? = null,
        bestBound: String? = null,
        gap: String? = null
    ): Ret<ConstraintProgrammingCheckpointEnvelope> {
        return ConstraintProgrammingCheckpointCodec.capture(
            snapshot = snapshot,
            descriptor = solver.descriptor,
            checkpointId = checkpointId,
            createdAtEpochMs = 1_000L,
            incumbent = incumbent,
            configurationFingerprint = CONFIGURATION_FINGERPRINT,
            runId = "run:1",
            attemptId = "attempt:1",
            bestBound = bestBound,
            gap = gap
        )
    }

    /**
     * 使用当前配置与求解器指纹恢复 checkpoint。 / Restore a checkpoint with the current fingerprints.
     *
     * @param envelope 待恢复 envelope / Envelope to restore
     * @param snapshot 当前模型 snapshot / Current model snapshot
     * @param expectedConfigurationFingerprint 当前生效配置指纹 / Fingerprint of the effective configuration
     * @param expectedSolverFingerprint 当前求解器指纹 / Fingerprint of the active solver runtime
     * @param validateParentLink 是否启用父链一致性校验 / Whether to enable parent-chain consistency validation
     * @param expectedParentCheckpointId 调用方声明的期望父 checkpoint 标识 / Expected parent checkpoint identifier declared by the caller
     * @param expectedRunId 调用方声明的期望运行标识 / Expected run identifier declared by the caller
     * @param expectedAttemptId 调用方声明的期望尝试标识 / Expected attempt identifier declared by the caller
     * @param expectedCancellationChain 调用方声明的期望取消链 / Expected cancellation chain declared by the caller
     * @param expectedProvenance 调用方声明的期望 provenance / Expected provenance declared by the caller
     * @return 恢复结果 / Restore result
     */
    private fun restoreEnvelope(
        envelope: ConstraintProgrammingCheckpointEnvelope,
        snapshot: ConstraintProgrammingModelSnapshot,
        expectedConfigurationFingerprint: String,
        expectedSolverFingerprint: String,
        validateParentLink: Boolean = false,
        expectedParentCheckpointId: String? = null,
        expectedRunId: String? = null,
        expectedAttemptId: String? = null,
        expectedCancellationChain: List<PortableCancellationRecord>? = null,
        expectedProvenance: PortableSolverProvenance? = null
    ): Ret<ConstraintProgrammingCheckpointRestore> {
        return ConstraintProgrammingCheckpointCodec.restore(
            envelope = envelope,
            snapshot = snapshot,
            expectedConfigurationFingerprint = expectedConfigurationFingerprint,
            expectedSolverFingerprint = expectedSolverFingerprint,
            validateParentLink = validateParentLink,
            expectedParentCheckpointId = expectedParentCheckpointId,
            expectedRunId = expectedRunId,
            expectedAttemptId = expectedAttemptId,
            expectedCancellationChain = expectedCancellationChain,
            expectedProvenance = expectedProvenance
        )
    }

    /**
     * 断言结果失败且错误消息包含指定片段。 / Assert failure with a specific message fragment.
     *
     * @param result 待校验结果 / Result under test
     * @param messageFragment 期望出现的错误消息片段 / Expected error-message fragment
     */
    private fun assertFailedWith(result: Ret<*>, messageFragment: String) {
        val failed = assertIs<Failed<*, *, *>>(result)
        val message = failed.error.message
        assertTrue(message.contains(messageFragment), message)
    }

    private companion object {
        /** 测试使用的生效配置指纹 / Effective configuration fingerprint used by these tests */
        const val CONFIGURATION_FINGERPRINT = "configuration:run-1"
    }
}
