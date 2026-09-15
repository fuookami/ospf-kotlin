/** Objective-target protocol for feasibility analysis. / 可行性分析的目标条件协议。 */
package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/** Direction of an objective feasibility target. / 目标可行性条件的方向。 */
enum class ObjectiveTargetRelation {
    AtLeast,
    AtMost
}

/**
 * An immutable objective bound used by target-feasibility analysis.
 *
 * The target deliberately contains only stable model identity and a value;
 * deriving a temporary CP constraint from it belongs to a later analysis
 * stage. Values are finite so that a malformed target cannot silently become
 * an always-true or always-false condition.
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

    /** Validate target identity and finite bound at a Result boundary. / 在 Result 边界校验目标身份与有限边界。 */
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

    /** Check a concrete objective value against this target. / 检查具体目标值是否满足条件。 */
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
     * Return the signed distance to the target boundary.
     * Positive values satisfy the target; negative values violate it.
     */
    fun signedDistance(objectiveValue: Flt64): Flt64 {
        return when (relation) {
            ObjectiveTargetRelation.AtLeast -> objectiveValue - value
            ObjectiveTargetRelation.AtMost -> value - objectiveValue
        }
    }

    companion object {
        /** Build an absolute lower target. / 构造绝对下界 target。 */
        fun atLeast(objectiveId: ObjectiveId, value: Flt64): AtLeast {
            return AtLeast(objectiveId, value)
        }

        /** Build an absolute upper target. / 构造绝对上界 target。 */
        fun atMost(objectiveId: ObjectiveId, value: Flt64): AtMost {
            return AtMost(objectiveId, value)
        }

        /** Build an absolute target from a baseline and a fractional relative change. */
        fun relativeAtLeast(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtLeast {
            require(baseline.isFinite() && fraction.isFinite()) { "Relative target values must be finite" }
            return AtLeast(objectiveId, baseline * (Flt64.one + fraction))
        }

        /** Build an upper target from a baseline and a fractional relative change. */
        fun relativeAtMost(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtMost {
            require(baseline.isFinite() && fraction.isFinite()) { "Relative target values must be finite" }
            return AtMost(objectiveId, baseline * (Flt64.one + fraction))
        }

        /** Rust-compatible spelling for a relative lower target. / 与 Rust 对齐的相对下界工厂名。 */
        fun atLeastRelative(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtLeast {
            return relativeAtLeast(objectiveId, baseline, fraction)
        }

        /** Rust-compatible spelling for a relative upper target. / 与 Rust 对齐的相对上界工厂名。 */
        fun atMostRelative(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtMost {
            return relativeAtMost(objectiveId, baseline, fraction)
        }

        /** Build a target using a caller-provided business-unit delta. */
        fun businessUnitAtLeast(objectiveId: ObjectiveId, baseline: Flt64, delta: Flt64): AtLeast {
            require(baseline.isFinite() && delta.isFinite()) { "Business-unit target values must be finite" }
            return AtLeast(objectiveId, baseline + delta)
        }

        /** Build an upper target using a caller-provided business-unit delta. */
        fun businessUnitAtMost(objectiveId: ObjectiveId, baseline: Flt64, delta: Flt64): AtMost {
            require(baseline.isFinite() && delta.isFinite()) { "Business-unit target values must be finite" }
            return AtMost(objectiveId, baseline + delta)
        }

        /** Rust-compatible spelling for a business-unit lower target. / 与 Rust 对齐的业务单位下界工厂名。 */
        fun atLeastBusinessUnit(objectiveId: ObjectiveId, baseline: Flt64, delta: Flt64): AtLeast {
            return businessUnitAtLeast(objectiveId, baseline, delta)
        }

        /** Rust-compatible spelling for a business-unit upper target. / 与 Rust 对齐的业务单位上界工厂名。 */
        fun atMostBusinessUnit(objectiveId: ObjectiveId, baseline: Flt64, delta: Flt64): AtMost {
            return businessUnitAtMost(objectiveId, baseline, delta)
        }
    }

    /** Minimum objective value required by the target. / 目标要求的最小目标值。 */
    data class AtLeast(
        override val objectiveId: ObjectiveId,
        override val value: Flt64
    ) : ObjectiveTarget {
        companion object {
            /** Build a relative lower target. / 构造相对下界 target。 */
            fun relative(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtLeast {
                return ObjectiveTarget.relativeAtLeast(objectiveId, baseline, fraction)
            }

            /** Build a business-unit lower target. / 构造业务单位下界 target。 */
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

    /** Maximum objective value allowed by the target. / 目标允许的最大目标值。 */
    data class AtMost(
        override val objectiveId: ObjectiveId,
        override val value: Flt64
    ) : ObjectiveTarget {
        companion object {
            /** Build a relative upper target. / 构造相对上界 target。 */
            fun relative(objectiveId: ObjectiveId, baseline: Flt64, fraction: Flt64): AtMost {
                return ObjectiveTarget.relativeAtMost(objectiveId, baseline, fraction)
            }

            /** Build a business-unit upper target. / 构造业务单位上界 target。 */
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
