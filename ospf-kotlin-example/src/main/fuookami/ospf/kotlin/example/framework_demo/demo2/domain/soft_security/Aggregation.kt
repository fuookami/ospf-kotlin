package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security

import fuookami.ospf.kotlin.utils.error.*
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
 * @property aircraftModel 飞机模型引用 / The aircraft model reference
 * @property mainDeck 主甲板配置 / The main deck configuration
 * @property items 货物项目列表 / The list of cargo items
 * @property positions 装载位置列表 / The list of stowage positions
 * @property stowage 装载分配矩阵 / The stowage assignment matrix
 * @property load 载荷分布数据 / The load distribution data
 * @property ballast 可选压舱物数据 / The optional ballast data
 * @property divideEmptyLoading 从位置和载荷派生的空载分割模型 / The divide-empty-loading model derived from positions and load
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
     * @param stowageMode 优化的装载模式 / The stowage mode for the optimization
     * @param model 要注册到的线性元模型 / The linear meta model to register into
     * @return 成功或失败结果 / Success or failure result
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = divideEmptyLoading.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }

    /**
     * Registers soft security constraints for the Benders master problem.
     * 为 Benders 主问题注册软安全约束。
     *
     * @param stowageMode 优化的装载模式 / The stowage mode for the optimization
     * @param model 主问题的线性元模型 / The linear meta model for the master problem
     * @return 成功或失败结果 / Success or failure result
    */
    fun registerForBendersMP(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        // Soft security constraints go into the master problem.
        return register(stowageMode = stowageMode, model = model)
    }

    /**
     * Registers soft security constraints for the Benders sub-problem.
     * 为 Benders 子问题注册软安全约束。
     *
     * @param model 子问题的线性元模型 / The linear meta model for the sub-problem
     * @param solution 子问题的解值 / The solution values from the sub-problem
     * @return 成功或失败结果 / Success or failure result
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
     * @param model 子问题的线性元模型 / The linear meta model for the sub-problem
     * @param solution 子问题的解值 / The solution values from the sub-problem
     * @return 成功或失败结果 / Success or failure result
    */
    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
