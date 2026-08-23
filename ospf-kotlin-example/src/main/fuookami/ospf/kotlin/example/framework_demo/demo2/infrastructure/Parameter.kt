package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * Aggregated business objective parameters for the stowage optimization model.
 * 配载优化模型的聚合业务目标参数。
 *
 * @property macRangeC 重心范围优化权重系数 / Weight coefficient for MAC range optimization.
 * @property longitudinalBalance 纵向平衡权重系数 / Weight coefficient for longitudinal balance.
 * @property B737LongitudinalBalance Weight coefficient for B737-specific longitudinal balance. / B737 专用纵向平衡权重系数
 * @property lateralBalance 横向平衡权重系数 / Weight coefficient for lateral balance.
 * @property horizontalStabilizerWarn 水平安定面警告权重系数 / Weight coefficient for horizontal stabilizer warning.
 * @property ballastWeight 压舱重量惩罚权重系数 / Weight coefficient for ballast weight penalty.
 * @property emptyHated 空舱位惩罚权重系数 / Weight coefficient for empty position penalty.
 * @property besideDoorMainPosition 门旁主舱位偏好权重系数 / Weight coefficient for beside-door main position preference.
 * @property dividedEmpty 分隔空位惩罚权重系数 / Weight coefficient for divided empty position penalty.
 * @property adviceLoadAmount 建议装载量权重系数 / Weight coefficient for advised load amount.
 * @property adviceLoadWeight 建议装载重量权重系数 / Weight coefficient for advised load weight.
 * @property sameFlowTransferIn 同流向转入偏好权重系数 / Weight coefficient for same-flow transfer-in preference.
 * @property sameFlowTransferOut 同流向转出偏好权重系数 / Weight coefficient for same-flow transfer-out preference.
 * @property itemOrder 货物排序偏好权重系数 / Weight coefficient for item ordering preference.
 * @property trailerChange 拖车更换惩罚权重系数 / Weight coefficient for trailer change penalty.
 * @property trailerCircling 拖车绕行惩罚权重系数 / Weight coefficient for trailer circling penalty.
 * @property priority 货物优先级权重系数 / Weight coefficient for cargo priority.
 * @property priorityCategory 优先级类别权重系数 / Weight coefficient for priority category.
 * @property experimentalLongitudinalBalance 余度中实验性纵向平衡权重系数 / Weight coefficient for experimental longitudinal balance in redundancy.
 * @property redundancyRange 余度范围权重系数 / Weight coefficient for redundancy range.
 * @property weightRecommendationBalance 重量推荐的均衡偏差惩罚系数 / Penalty coefficient for weight-recommendation balance deviation.
 * @property weightRecommendationPayload 重量推荐的载荷奖励系数 / Reward coefficient for weight-recommendation payload.
*/
data class Parameter(
    // 业务目标参数：重心优化上下文
    val macRangeC: Flt64,
    val longitudinalBalance: Flt64,
    val B737LongitudinalBalance: Flt64,
    val lateralBalance: Flt64,
    val horizontalStabilizerWarn: Flt64,
    // 业务目标参数：软性安全上下文
    val ballastWeight: Flt64,
    val emptyHated: Flt64,
    val besideDoorMainPosition: Flt64,
    val dividedEmpty: Flt64,
    // 业务目标参数：装卸效率上下文,
    val adviceLoadAmount: Flt64,
    val adviceLoadWeight: Flt64,
    val sameFlowTransferIn: Flt64,
    val sameFlowTransferOut: Flt64,
    val itemOrder: Flt64,
    val trailerChange: Flt64,
    val trailerCircling: Flt64,
    // 业务目标参数：货物时效上下文
    val priority: Flt64,
    val priorityCategory: Flt64,
    // 业务目标参数：余度上下文
    val experimentalLongitudinalBalance: Flt64,
    val redundancyRange: Flt64,
    // 业务目标参数：重量推荐上下文
    val weightRecommendationBalance: Flt64,
    val weightRecommendationPayload: Flt64
) {

    /**
     * Companion object providing a factory method for constructing Parameter with nullable defaults.
     * 伴生对象，提供使用可空默认值构造 Parameter 的工厂方法。
    */
    companion object {
        operator fun invoke(
            // 业务目标参数：重心优化上下文
            macRangeC: Flt64? = null,
            longitudinalBalance: Flt64? = null,
            B737LongitudinalBalance: Flt64? = null,
            lateralBalance: Flt64? = null,
            horizontalStabilizerWarn: Flt64? = null,
            // 业务目标参数：软性安全上下文
            ballastWeight: Flt64? = null,
            emptyHated: Flt64? = null,
            besideDoorMainPosition: Flt64? = null,
            dividedEmpty: Flt64? = null,
            // 业务目标参数：装卸效率上下文
            adviceLoadAmount: Flt64? = null,
            adviceLoadWeight: Flt64? = null,
            sameFlowTransferIn: Flt64? = null,
            sameFlowTransferOut: Flt64? = null,
            itemOrder: Flt64? = null,
            trailerChange: Flt64? = null,
            trailerCircling: Flt64? = null,
            // 业务目标参数：货物时效上下文
            priority: Flt64? = null,
            priorityCategory: Flt64? = null,
            // 业务目标参数：余度上下文
            experimentalLongitudinalBalance: Flt64? = null,
            redundancyRange: Flt64? = null,
            // 业务目标参数：重量推荐上下文
            weightRecommendationBalance: Flt64? = null,
            weightRecommendationPayload: Flt64? = null
        ): Parameter {
            return Parameter(
                macRangeC = macRangeC ?: Flt64.one,
                longitudinalBalance = longitudinalBalance ?: Flt64.one,
                B737LongitudinalBalance = B737LongitudinalBalance ?: Flt64.one,
                lateralBalance = lateralBalance ?: Flt64.one,
                horizontalStabilizerWarn = horizontalStabilizerWarn ?: Flt64.one,
                ballastWeight = ballastWeight ?: Flt64.one,
                emptyHated = emptyHated ?: Flt64.one,
                besideDoorMainPosition = besideDoorMainPosition ?: Flt64.one,
                dividedEmpty = dividedEmpty ?: Flt64.one,
                adviceLoadAmount = adviceLoadAmount ?: Flt64.one,
                adviceLoadWeight = adviceLoadWeight ?: Flt64.one,
                sameFlowTransferIn = sameFlowTransferIn ?: Flt64.one,
                sameFlowTransferOut = sameFlowTransferOut ?: Flt64.one,
                itemOrder = itemOrder ?: Flt64.one,
                trailerChange = trailerChange ?: Flt64.one,
                trailerCircling = trailerCircling ?: Flt64.one,
                priority = priority ?: Flt64.one,
                priorityCategory = priorityCategory ?: Flt64.one,
                experimentalLongitudinalBalance = experimentalLongitudinalBalance ?: Flt64.one,
                redundancyRange = redundancyRange ?: Flt64.one,
                weightRecommendationBalance = weightRecommendationBalance ?: Flt64.one,
                weightRecommendationPayload = weightRecommendationPayload ?: Flt64.one
            )
        }
    }
}
