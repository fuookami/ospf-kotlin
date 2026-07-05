package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.*

/** 位置最大载重限制 / Position maximum load limit */
data class PositionMaxLoadLimit(
    /** 目标积载位置 / The target stowage position */
    val position: StowagePosition,
    /** 最大载重（吨）/ Maximum load weight (tons) */
    val maxLoad: Double
) : Limit {
    override fun invoke(plan: StowagePlan): Result<FlattenConcreteConstraint, Error> {
        return try {
            val flattened = plan.flatten(position)
            val binVar = plan.get(position)
            val totalLoad = plan.sum(position) { _, cargo -> cargo.weight }
            val constraint = (totalLoad leq maxLoad) or (!binVar)
            Ok(flattened.flatten(constraint))
        } catch (e: Exception) {
            Failed(ConstraintError(e.message ?: "Unknown error"))
        }
    }
}
