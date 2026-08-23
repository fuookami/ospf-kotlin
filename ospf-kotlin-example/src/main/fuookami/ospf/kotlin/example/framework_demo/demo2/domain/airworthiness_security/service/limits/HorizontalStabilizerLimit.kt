package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * 约束水平安定面配平在最小/最大边界内（在推荐模式下使用警告限制）。Constrains horizontal stabilizer trim within min/max bounds, using warning limits in recommendation mode.
 *
 * @property horizontalStabilizers 各键对应的水平安定面配平数据 / The horizontal stabilizer trim data per key
 * @property stowageMode 决定应用哪些限制的装载模式 / The stowage mode determining which limits to apply
*/
class HorizontalStabilizerLimit(
    private val horizontalStabilizers: Map<HorizontalStabilizer.Key, HorizontalStabilizer>,
    private val stowageMode: StowageMode,
    override val name: String = "horizontal_stabilizer_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((key, horizontalStabilizer) in horizontalStabilizers) {
            val maxTrim = if (stowageMode == StowageMode.WeightRecommendation) {
                horizontalStabilizer.limit.warnMaxTrim ?: horizontalStabilizer.limit.maxTrim
            } else {
                horizontalStabilizer.limit.maxTrim
            }
            if (maxTrim != null) {
                when (val result = model.addConstraint(
                    horizontalStabilizer.trim leq maxTrim,
                    name = if (stowageMode == StowageMode.WeightRecommendation &&
                        horizontalStabilizer.limit.warnMaxTrim != null
                    ) {
                        "${name}_${key}_warn_ub"
                    } else {
                        "${name}_${key}_ub"
                    }
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }

            if (stowageMode == StowageMode.WeightRecommendation) {
                val warnMinTrim = horizontalStabilizer.limit.warnMinTrim
                if (warnMinTrim != null) {
                    when (val result = model.addConstraint(
                        horizontalStabilizer.trim geq warnMinTrim,
                        name = "${name}_${key}_warn_lb"
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                }
            }

            val minTrim = horizontalStabilizer.limit.minTrim
            if (minTrim != null) {
                when (val result = model.addConstraint(
                    horizontalStabilizer.trim geq minTrim,
                    name = "${name}_${key}_lb"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return ok
    }
}
