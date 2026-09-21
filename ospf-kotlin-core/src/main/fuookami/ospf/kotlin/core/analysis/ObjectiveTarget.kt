/** Objective-target protocol for feasibility analysis. / 可行性分析的目标条件协议。 */
package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId

/** Direction of an objective feasibility target. / 目标可行性条件的方向。 */
enum class ObjectiveTargetRelation {
    AtLeast,
    AtMost
}

/**
 * 目标可行性分析使用的不可变目标边界。 / An immutable objective bound used by target-feasibility analysis.
 *
 * 目标只包含稳定模型身份和值；临时 CP 约束应由后续分析阶段从目标派生。值必须有限，避免畸形目标
 * 静默变成恒真或恒假条件。 / The target deliberately contains only stable model identity and a value;
 * deriving a temporary CP constraint from it belongs to a later analysis stage. Values are finite
 * so that a malformed target cannot silently become an always-true or always-false condition.
 *
 * @property objectiveId 稳定目标标识 / Stable objective identity
 * @property value 目标边界值 / Objective bound
 * @property relation 边界方向 / Bound direction
 * @property stableId 稳定诊断键 / Stable diagnostic key
 */
sealed interface ObjectiveTarget {
    /** Stable objective identity. / 稳定目标标识。 */
    val objectiveId: ObjectiveId

    /** Objective bound. / 目标边界值。 */
    val value: Flt64

    /** Bound direction. / 边界方向。 */
    val relation: ObjectiveTargetRelation

    /** Stable diagnostic key. / 稳定诊断键。 */
    val stableId: String

    /** 在 Result 边界校验目标身份与有限边界。 / Validate target identity and finite bound at a Result boundary.
     *
     * @return 结构化校验结果 / Structured validation result
     */
    fun validate(): Try {
        return if (objectiveId.value.isNotBlank() && value.isFinite()) {
            ok
        } else {
            Failed(
                ErrorCode.IllegalArgument,
                "ObjectiveTarget 身份不能为空且边界必须有限 / ObjectiveTarget identity must not be blank and bound must be finite"
            )
        }
    }

    /** 检查具体目标值是否满足条件。 / Check a concrete objective value against this target.
     *
     * @param objectiveValue 待检查的目标值 / Objective value to check
     * @return 目标是否满足 / Whether the target is satisfied
     */
    fun isSatisfied(objectiveValue: Flt64): Boolean {
        if (!objectiveValue.isFinite()) {
            return false
        }
        return when (relation) {
            ObjectiveTargetRelation.AtLeast -> objectiveValue >= value
            ObjectiveTargetRelation.AtMost -> objectiveValue <= value
        }
    }

    /**
     * 返回到目标边界的有符号距离，正值表示满足，负值表示违反。 / Return the signed distance to the target boundary;
     * positive values satisfy the target and negative values violate it.
     *
     * @param objectiveValue 待测目标值 / Objective value to measure
     * @return 到目标边界的有符号距离 / Signed distance to the target boundary
     */
    fun signedDistance(objectiveValue: Flt64): Flt64 {
        return when (relation) {
            ObjectiveTargetRelation.AtLeast -> objectiveValue - value
            ObjectiveTargetRelation.AtMost -> value - objectiveValue
        }
    }

    companion object {
        /** 构造绝对下界 target。 / Build an absolute lower target.
         *
         * @param objectiveId 目标 ID / Objective ID
         * @param value 下界值 / Lower-bound value
         * @return 下界目标 / Lower-bound target
         */
        fun atLeast(objectiveId: ObjectiveId, value: Flt64): AtLeast {
            return AtLeast(objectiveId, value)
        }

        /** 构造绝对上界 target。 / Build an absolute upper target.
         *
         * @param objectiveId 目标 ID / Objective ID
         * @param value 上界值 / Upper-bound value
         * @return 上界目标 / Upper-bound target
         */
        fun atMost(objectiveId: ObjectiveId, value: Flt64): AtMost {
            return AtMost(objectiveId, value)
        }

        /** 从基线和相对变化比例构造绝对下界 target。 / Build a lower target from a baseline and a fractional relative change.
         *
         * @param objectiveId 目标 ID / Objective ID
         * @param baseline 基线目标值 / Baseline objective value
         * @param fraction 相对变化比例 / Relative change fraction
         * @return 相对下界目标 / Relative lower target
         */
        fun relativeAtLeast(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtLeast {
            require(baseline.isFinite() && fraction.isFinite()) { "Relative target values must be finite" }
            return AtLeast(objectiveId, baseline * (Flt64.one + fraction))
        }

        /** 从基线和相对变化比例构造上界 target。 / Build an upper target from a baseline and a fractional relative change.
         *
         * @param objectiveId 目标 ID / Objective ID
         * @param baseline 基线目标值 / Baseline objective value
         * @param fraction 相对变化比例 / Relative change fraction
         * @return 相对上界目标 / Relative upper target
         */
        fun relativeAtMost(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtMost {
            require(baseline.isFinite() && fraction.isFinite()) { "Relative target values must be finite" }
            return AtMost(objectiveId, baseline * (Flt64.one + fraction))
        }

        /** 与 Rust 对齐的相对下界工厂名。 / Rust-compatible spelling for a relative lower target.
         *
         * @param objectiveId 目标 ID / Objective ID
         * @param baseline 基线目标值 / Baseline objective value
         * @param fraction 相对变化比例 / Relative change fraction
         * @return 相对下界目标 / Relative lower target
         */
        fun atLeastRelative(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtLeast {
            return relativeAtLeast(objectiveId, baseline, fraction)
        }

        /** 与 Rust 对齐的相对上界工厂名。 / Rust-compatible spelling for a relative upper target.
         *
         * @param objectiveId 目标 ID / Objective ID
         * @param baseline 基线目标值 / Baseline objective value
         * @param fraction 相对变化比例 / Relative change fraction
         * @return 相对上界目标 / Relative upper target
         */
        fun atMostRelative(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtMost {
            return relativeAtMost(objectiveId, baseline, fraction)
        }

        /** 使用调用方提供的业务单位增量构造下界 target。 / Build a lower target using a caller-provided business-unit delta.
         *
         * @param objectiveId 目标 ID / Objective ID
         * @param baseline 基线目标值 / Baseline objective value
         * @param delta 业务单位增量 / Business-unit delta
         * @return 业务单位下界目标 / Business-unit lower target
         */
        fun businessUnitAtLeast(objectiveId: ObjectiveId, baseline: Flt64, delta: Flt64): AtLeast {
            require(baseline.isFinite() && delta.isFinite()) { "Business-unit target values must be finite" }
            return AtLeast(objectiveId, baseline + delta)
        }

        /** 使用调用方提供的业务单位增量构造上界 target。 / Build an upper target using a caller-provided business-unit delta.
         *
         * @param objectiveId 目标 ID / Objective ID
         * @param baseline 基线目标值 / Baseline objective value
         * @param delta 业务单位增量 / Business-unit delta
         * @return 业务单位上界目标 / Business-unit upper target
         */
        fun businessUnitAtMost(objectiveId: ObjectiveId, baseline: Flt64, delta: Flt64): AtMost {
            require(baseline.isFinite() && delta.isFinite()) { "Business-unit target values must be finite" }
            return AtMost(objectiveId, baseline + delta)
        }

        /** 与 Rust 对齐的业务单位下界工厂名。 / Rust-compatible spelling for a business-unit lower target.
         *
         * @param objectiveId 目标 ID / Objective ID
         * @param baseline 基线目标值 / Baseline objective value
         * @param delta 业务单位增量 / Business-unit delta
         * @return 业务单位下界目标 / Business-unit lower target
         */
        fun atLeastBusinessUnit(objectiveId: ObjectiveId, baseline: Flt64, delta: Flt64): AtLeast {
            return businessUnitAtLeast(objectiveId, baseline, delta)
        }

        /** 与 Rust 对齐的业务单位上界工厂名。 / Rust-compatible spelling for a business-unit upper target.
         *
         * @param objectiveId 目标 ID / Objective ID
         * @param baseline 基线目标值 / Baseline objective value
         * @param delta 业务单位增量 / Business-unit delta
         * @return 业务单位上界目标 / Business-unit upper target
         */
        fun atMostBusinessUnit(objectiveId: ObjectiveId, baseline: Flt64, delta: Flt64): AtMost {
            return businessUnitAtMost(objectiveId, baseline, delta)
        }
    }

    /** 目标要求的最小目标值。 / Minimum objective value required by the target.
     *
     * @property objectiveId 目标 ID / Objective ID
     * @property value 下界值 / Lower-bound value
     */
    data class AtLeast(
        override val objectiveId: ObjectiveId,
        override val value: Flt64
    ) : ObjectiveTarget {
        companion object {
            /** 构造相对下界 target。 / Build a relative lower target.
             *
             * @param objectiveId 目标 ID / Objective ID
             * @param baseline 基线目标值 / Baseline objective value
             * @param fraction 相对变化比例 / Relative change fraction
             * @return 相对下界目标 / Relative lower target
             */
            fun relative(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtLeast {
                return ObjectiveTarget.relativeAtLeast(objectiveId, baseline, fraction)
            }

            /** 构造业务单位下界 target。 / Build a business-unit lower target.
             *
             * @param objectiveId 目标 ID / Objective ID
             * @param baseline 基线目标值 / Baseline objective value
             * @param delta 业务单位增量 / Business-unit delta
             * @return 业务单位下界目标 / Business-unit lower target
             */
            fun businessUnit(objectiveId: ObjectiveId, baseline: Flt64, delta: Flt64): AtLeast {
                return ObjectiveTarget.businessUnitAtLeast(objectiveId, baseline, delta)
            }
        }

        init {
            require(objectiveId.value.isNotBlank()) { "Objective ID must not be blank" }
            require(value.isFinite()) { "Objective target must be finite" }
        }

        override val relation: ObjectiveTargetRelation
            get() = ObjectiveTargetRelation.AtLeast

        override val stableId: String
            get() = "objective-target:${objectiveId.value}:at-least:$value"
    }

    /** 目标允许的最大目标值。 / Maximum objective value allowed by the target.
     *
     * @property objectiveId 目标 ID / Objective ID
     * @property value 上界值 / Upper-bound value
     */
    data class AtMost(
        override val objectiveId: ObjectiveId,
        override val value: Flt64
    ) : ObjectiveTarget {
        companion object {
            /** 构造相对上界 target。 / Build a relative upper target.
             *
             * @param objectiveId 目标 ID / Objective ID
             * @param baseline 基线目标值 / Baseline objective value
             * @param fraction 相对变化比例 / Relative change fraction
             * @return 相对上界目标 / Relative upper target
             */
            fun relative(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtMost {
                return ObjectiveTarget.relativeAtMost(objectiveId, baseline, fraction)
            }

            /** 构造业务单位上界 target。 / Build a business-unit upper target.
             *
             * @param objectiveId 目标 ID / Objective ID
             * @param baseline 基线目标值 / Baseline objective value
             * @param delta 业务单位增量 / Business-unit delta
             * @return 业务单位上界目标 / Business-unit upper target
             */
            fun businessUnit(objectiveId: ObjectiveId, baseline: Flt64, delta: Flt64): AtMost {
                return ObjectiveTarget.businessUnitAtMost(objectiveId, baseline, delta)
            }
        }

        init {
            require(objectiveId.value.isNotBlank()) { "Objective ID must not be blank" }
            require(value.isFinite()) { "Objective target must be finite" }
        }

        override val relation: ObjectiveTargetRelation
            get() = ObjectiveTargetRelation.AtMost

        override val stableId: String
            get() = "objective-target:${objectiveId.value}:at-most:$value"
    }
}
