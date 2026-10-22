package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkArcId

/** 弧变量和输入/输出线性表达式的模型组件。 / Model component for arc variables and input/output expressions.
 *
 * @param V 领域数值类型 / Domain numeric type
 * @property prepared 已校验并归一化的弧数据 / Validated and normalized arc data
 * @property name 变量命名空间 / Variable namespace
 */
internal class GeneralizedFlowArcComponent<V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>>(
    private val prepared: List<PreparedGeneralizedFlowArc<V>>,
    private val name: String
) {
    val variables = mutableMapOf<NetworkArcId, URealVar>()
    val inputs = mutableMapOf<NetworkArcId, LinearPolynomial<Flt64>>()
    val outputs = mutableMapOf<NetworkArcId, LinearPolynomial<Flt64>>()

    /** 注册弧投入变量并构建广义产出表达式。 / Register arc-input variables and build generalized output expressions.
     *
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        for (item in prepared) {
            val arcId = item.arc.networkArc.id
            val variable = URealVar("${name}_input_${arcId.value}")
            when (val result = model.add(variable)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val input = LinearPolynomial(
                listOf(LinearMonomial(Flt64.one, variable)),
                Flt64.zero
            )
            variables[arcId] = variable
            inputs[arcId] = input
            outputs[arcId] = input * item.outputCoefficient
        }
        return ok
    }
}

/** 已在 solver 数值边界完成单位、有限性和系数校验的弧配置。 / Arc configuration validated at the solver numeric boundary. */
internal data class PreparedGeneralizedFlowArc<V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>>(
    val arc: GeneralizedFlowArc<V>,
    val lowerInput: Flt64,
    val upperInput: Flt64,
    val unitConversionCoefficient: Flt64,
    val outputCoefficient: Flt64,
    val costCoefficient: Flt64
)
