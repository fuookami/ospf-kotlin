package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * Aggregated business objective parameters for the stowage optimization model.
 * 配载优化模型的聚合业务目标参数。
 *
 * @property macRangeC Weight coefficient for MAC range optimization. / 重心范围优化权重系数
 * @property longitudinalBalance Weight coefficient for longitudinal balance. / 纵向平衡权重系数
 * @property B737LongitudinalBalance Weight coefficient for B737-specific longitudinal balance. / B737 专用纵向平衡权重系数
 * @property lateralBalance Weight coefficient for lateral balance. / 横向平衡权重系数
 * @property horizontalStabilizerWarn Weight coefficient for horizontal stabilizer warning. / 水平安定面警告权重系数
 * @property ballastWeight Weight coefficient for ballast weight penalty. / 压舱重量惩罚权重系数
 * @property emptyHated Weight coefficient for empty position penalty. / 空舱位惩罚权重系数
 * @property besideDoorMainPosition Weight coefficient for beside-door main position preference. / 门旁主舱位偏好权重系数
 * @property dividedEmpty Weight coefficient for divided empty position penalty. / 分隔空位惩罚权重系数
 * @property adviceLoadAmount Weight coefficient for advised load amount. / 建议装载量权重系数
 * @property adviceLoadWeight Weight coefficient for advised load weight. / 建议装载重量权重系数
 * @property sameFlowTransferIn Weight coefficient for same-flow transfer-in preference. / 同流向转入偏好权重系数
 * @property sameFlowTransferOut Weight coefficient for same-flow transfer-out preference. / 同流向转出偏好权重系数
 * @property itemOrder Weight coefficient for item ordering preference. / 货物排序偏好权重系数
 * @property trailerChange Weight coefficient for trailer change penalty. / 拖车更换惩罚权重系数
 * @property trailerCircling Weight coefficient for trailer circling penalty. / 拖车绕行惩罚权重系数
 * @property priority Weight coefficient for cargo priority. / 货物优先级权重系数
 * @property priorityCategory Weight coefficient for priority category. / 优先级类别权重系数
 * @property experimentalLongitudinalBalance Weight coefficient for experimental longitudinal balance in redundancy. / 余度中实验性纵向平衡权重系数
 * @property redundancyRange Weight coefficient for redundancy range. / 余度范围权重系数
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
        ): Parameter {
            TODO("not implemented yet")
        }
    }
}
