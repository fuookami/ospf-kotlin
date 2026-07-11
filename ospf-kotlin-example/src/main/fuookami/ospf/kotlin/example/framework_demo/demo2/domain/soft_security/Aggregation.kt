package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Aggregates soft security data including divide-empty-loading model for weight distribution optimization.
 * 聚合软安全数据（包括空载分割模型）用于重量分布优化。
 *
 * @property aircraftModel The aircraft model reference / 飞机模型引用
 * @property mainDeck The main deck configuration / 主甲板配置
 * @property items The list of cargo items / 货物项目列表
 * @property positions The list of stowage positions / 装载位置列表
 * @property stowage The stowage assignment matrix / 装载分配矩阵
 * @property load The load distribution data / 载荷分布数据
 * @property ballast The optional ballast data / 可选压舱物数据
 * @property divideEmptyLoading The divide-empty-loading model derived from positions and load / 从位置和载荷派生的空载分割模型
*/
class Aggregation(
    internal val aircraftModel: AircraftModel,
    internal val mainDeck: Deck,
    internal val items: List<Item>,
    internal val positions: List<Position>,
    internal val stowage: Stowage,
    internal val load: Load,
    internal val ballast: Ballast?
) {
    val divideEmptyLoading = DivideEmptyLoading(
        positions = positions,
        load = load
    )

    /**
     * Registers divide-empty-loading constraints into the optimization model.
     * 将空载分割约束注册到优化模型中。
     *
     * @param stowageMode The stowage mode for the optimization / 优化的装载模式
     * @param model The linear meta model to register into / 要注册到的线性元模型
     * @return Success or failure result / 成功或失败结果
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = divideEmptyLoading.register(model)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }

    /**
     * Registers soft security constraints for the Benders master problem.
     * 为 Benders 主问题注册软安全约束。
     *
     * @param model The linear meta model for the master problem / 主问题的线性元模型
     * @return Success or failure result / 成功或失败结果
    */
    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        // Soft security constraints go into the master problem.
        return register(stowageMode = StowageMode.FullLoad, model = model)
    }

    /**
     * Registers soft security constraints for the Benders sub-problem.
     * 为 Benders 子问题注册软安全约束。
     *
     * @param model The linear meta model for the sub-problem / 子问题的线性元模型
     * @param solution The solution values from the sub-problem / 子问题的解值
     * @return Success or failure result / 成功或失败结果
    */
    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        // Soft security does not contribute to the sub problem.
        return ok
    }

    /**
     * Flushes the Benders sub-problem solution into the soft security context.
     * 将 Benders 子问题解刷新到软安全上下文中。
     *
     * @param model The linear meta model for the sub-problem / 子问题的线性元模型
     * @param solution The solution values from the sub-problem / 子问题的解值
     * @return Success or failure result / 成功或失败结果
    */
    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}

