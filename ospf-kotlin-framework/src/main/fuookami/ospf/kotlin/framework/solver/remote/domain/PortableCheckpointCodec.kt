/**
 * Remote portable checkpoint codec. / 远端可移植 checkpoint codec。
 *
 * 本对象曾经是 core `ConstraintProgrammingCheckpointCodec` 的一份**独立实现**：自带字段列表、
 * 摘要算法与父链校验。两份实现靠人工保持一致，改一处漏一处（父链校验缺口就是这样出现的）。
 * 现在它只做转发，三种入口都委托给 core，语义只有一处实现。
 *
 * This object used to be an **independent implementation** of core's
 * `ConstraintProgrammingCheckpointCodec`, carrying its own field list, digest, and parent-link
 * validation. The two copies were kept aligned by hand and drifted (which is how the parent-link gap
 * arose). It now forwards: all three entry points delegate to core, so the semantics have exactly one
 * implementation.
 */
package fuookami.ospf.kotlin.framework.solver.remote.domain

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingCheckpointCodec
import fuookami.ospf.kotlin.core.solver.report.SolveFingerprinting

/** Remote checkpoint codec delegating to the canonical core implementation. / 转发到 core 规范实现的远端 checkpoint codec。 */
object PortableCheckpointCodec {
    /**
     * 编码 envelope 并计算完整性摘要。 / Encode an envelope and compute its integrity digest.
     *
     * 完全委托 core：字段顺序、schema 版本规范化、string-map 排序与摘要都由 core 决定。
     * Fully delegated to core: field order, schema normalization, string-map ordering, and the digest
     * are all decided by core.
     *
     * 注意返回类型无法表达失败（既有公开 API 形态必须保留），因此对无法编码的 envelope
     * **显式抛出**而不是静默产出一份永远无法解码的文档。
     * The return type cannot express failure (the existing public API shape must be preserved), so an
     * envelope that cannot be encoded **throws explicitly** rather than silently producing a document
     * that can never be decoded.
     *
     * @param envelope checkpoint envelope / checkpoint envelope
     * @return JSON 文本 / JSON text
     * @throws IllegalArgumentException envelope 无法编码时抛出 / Thrown when the envelope cannot be encoded
     */
    fun encode(envelope: PortableCheckpointEnvelope): String {
        return when (val encoded = ConstraintProgrammingCheckpointCodec.encode(envelope)) {
            is Ok -> encoded.value
            is Failed -> throw IllegalArgumentException(
                "可移植 checkpoint 编码失败 / Portable checkpoint encoding failed: ${encoded.error.message}"
            )
            is Fatal -> throw IllegalArgumentException(
                "可移植 checkpoint 编码失败 / Portable checkpoint encoding failed: " +
                    encoded.errors.joinToString(separator = "; ") { it.message ?: "" }
            )
            else -> throw IllegalArgumentException(
                "可移植 checkpoint 编码失败 / Portable checkpoint encoding failed"
            )
        }
    }

    /**
     * 仅解码当前 schema 的已验证 envelope；历史格式迁移由 [decodeCompatibleOrNull] 单独负责。 /
     * Decode only a verified envelope of the current schema; historical-format migration is
     * deliberately separate and handled by [decodeCompatibleOrNull].
     *
     * 本入口不再像收敛前那样内联回退到 schema 2.0 形状：那条回退路径本是 core 的迁移逻辑在框架侧
     * 的第二份实现，现在只保留在 [decodeCompatibleOrNull] 中。
     * This entry no longer falls back inline to the schema 2.0 shape the way it did before
     * convergence: that fallback was a second framework-side copy of core's migration logic and now
     * lives only in [decodeCompatibleOrNull].
     *
     * @param encoded JSON 文本 / JSON text
     * @return envelope 或 null / Envelope, or null
     */
    fun decodeOrNull(encoded: String): PortableCheckpointEnvelope? {
        return ConstraintProgrammingCheckpointCodec.decode(encoded).value
    }

    /**
     * 解码当前 schema、已发布的 schema 2.0 形状或 legacy v1 形状的 checkpoint。 /
     * Decode a checkpoint of the current schema, the published schema 2.0 shape, or the legacy v1 shape.
     *
     * 迁移入口刻意与 [decodeOrNull] 分开，因此需要读取历史 checkpoint 的调用方必须显式选择本入口。
     * The migration entry point is deliberately separate from [decodeOrNull], so a caller that needs to
     * read a historical checkpoint must opt in explicitly.
     *
     * @param encoded JSON 文本 / JSON text
     * @return envelope 或 null / Envelope, or null
     */
    fun decodeCompatibleOrNull(encoded: String): PortableCheckpointEnvelope? {
        return ConstraintProgrammingCheckpointCodec.decodeCompatible(encoded).value
    }

    /**
     * 计算 UTF-8 内容的 SHA-256 摘要。 / Compute the SHA-256 digest of UTF-8 content.
     *
     * @param value 待摘要文本 / Text to digest
     * @return 小写十六进制摘要 / Lowercase hexadecimal digest
     */
    fun sha256(value: String): String {
        return SolveFingerprinting.sha256(value).value
    }
}
