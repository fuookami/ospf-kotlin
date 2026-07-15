package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Types of fuel tanks available on an aircraft.
 * 飞机上可用的油箱类型。
*/
enum class FuelTankType {
    /** Main fuel tank / 主油箱 */
    Main,
    /** Center fuel tank / 中央油箱 */
    Center,
    /** Outer main fuel tank (e.g. B747 Main Tank 1 & 4) / 外侧主油箱（如 B747 主油箱 1 和 4） */
    OuterMain,
    /** Inner main fuel tank (e.g. B747 Main Tank 2 & 3) / 内侧主油箱（如 B747 主油箱 2 和 3） */
    InnerMain,
    /** Reserve fuel tank (e.g. B747 Reserve Tank) / 备用油箱（如 B747 备用油箱） */
    Reserve
}

/**
 * Interpolated balanced arm lookup table for a fuel tank across different volumes and flight phases.
 * 油箱跨不同体积和飞行阶段的插值平衡臂查找表。
 *
 * @property points The list of interpolation points for balanced arm lookup. / 平衡臂查找的插值点列表
*/
data class FuelTankBalancedArm(
    val points: List<Point>
) {

    /**
     * A single interpolation point mapping a fuel volume to balanced arms at different flight phases.
     * 将燃油体积映射到不同飞行阶段平衡臂的单个插值点。
     *
     * @property volume The fuel volume at this point. / 该点的燃油体积
     * @property takeOffBalancedArm The balanced arm at takeoff phase. / 起飞阶段的平衡臂
     * @property landingBalancedArm The balanced arm at landing phase, or null if same as takeoff. / 着陆阶段的平衡臂，若与起飞相同则为 null
    */
    data class Point(
        val volume: Quantity<Flt64>,
        val takeOffBalancedArm: Quantity<Flt64>,
        val landingBalancedArm: Quantity<Flt64>?
    ) {
        operator fun get(phase: FlightPhase): Quantity<Flt64> {
            return when (phase) {
                FlightPhase.TakeOff, FlightPhase.ZeroFuel -> takeOffBalancedArm
                FlightPhase.Landing -> landingBalancedArm ?: takeOffBalancedArm
            }
        }
    }

    /**
     * Look up the balanced arm for a given flight phase and fuel volume via interpolation.
     * 通过插值查找给定飞行阶段和燃油体积的平衡臂。
     *
     * @param phase The flight phase. / 飞行阶段
     * @param volume The fuel volume. / 燃油体积
     * @return The interpolated balanced arm. / 插值后的平衡臂
    */
    operator fun invoke(phase: FlightPhase, volume: Quantity<Flt64>): Quantity<Flt64> {
        assert(points.size >= 2)
        return if (volume.partialOrd(points.first().volume) is Order.Less) {
            points.first()[phase]
        } else if (volume.partialOrd(points.last().volume) is Order.Greater) {
            points.last()[phase]
        } else {
            val i = (1 until points.size).first {
                points[it - 1].volume.partialOrd(volume) is Order.Less
                    && volume.partialOrd(points[it].volume) is Order.Less
            }
            val dy = (points[i][phase] - points[i - 1][phase])!!
            val dx = (points[i].volume - points[i - 1].volume)!!
            val slope = (dy / dx)!!
            (slope * (volume - points[i - 1].volume)!!)!!
        }
    }
}

/**
 * A fuel tank with its type, capacity, and balanced arm lookup data.
 * 具有类型、容量和平衡臂查找数据的油箱。
 *
 * @property type The type of the fuel tank. / 油箱类型
 * @property name The name identifier of the fuel tank. / 油箱名称标识
 * @property maxVolume The maximum fuel volume the tank can hold. / 油箱可容纳的最大燃油体积
 * @property balancedArm The balanced arm lookup table for this tank. / 该油箱的平衡臂查找表
*/
data class FuelTank(
    val type: FuelTankType,
    val name: String,
    val maxVolume: Quantity<Flt64>,
    val balancedArm: FuelTankBalancedArm
)

/**
 * A snapshot view of a fuel tank at a specific volume with computed weight and balanced arm.
 * 特定体积的油箱快照视图（具有计算的重量和平衡臂）。
 *
 * @property tank The underlying fuel tank. / 底层油箱
 * @property volume The current fuel volume. / 当前燃油体积
 * @property weight The computed fuel weight. / 计算的燃油重量
 * @property balancedArm The balanced arm at the current volume. / 当前体积下的平衡臂
*/
data class FuelTankView(
    val tank: FuelTank,
    val volume: Quantity<Flt64>,
    val weight: Quantity<Flt64>,
    val balancedArm: Quantity<Flt64>
) {
    val type by tank::type
    val name by tank::name
}

/**
 * Precomputed fuel constants (density, weight, index) for a given flight phase.
 * 给定飞行阶段的预计算燃油常数（密度、重量、指数）。
 *
 * @property density The fuel density. / 燃油密度
 * @property weight The total fuel weight. / 总燃油重量
 * @property index The total fuel index. / 总燃油指数
*/
data class FuelConstant(
    val density: Quantity<Flt64>,
    val weight: Quantity<Flt64>,
    val index: Quantity<Flt64>
) {
    companion object {
        /**
         * Create a FuelConstant by computing the index from tank views and formula.
         * 通过从油箱视图和公式计算指数来创建 FuelConstant。
         *
         * @param density The fuel density. / 燃油密度
         * @param weight The total fuel weight. / 总燃油重量
         * @param tanks The list of fuel tank views. / 油箱视图列表
         * @param aircraftModel The aircraft model for unit definitions. / 用于单位定义的飞机型号
         * @param formula The aerodynamic formula for index computation. / 用于指数计算的气动公式
         * @return The computed FuelConstant. / 计算的燃油常数
        */
        operator fun invoke(
            density: Quantity<Flt64>,
            weight: Quantity<Flt64>,
            tanks: List<FuelTankView>,
            aircraftModel: AircraftModel,
            formula: Formula
        ): FuelConstant {
            var index = Quantity(Flt64.zero, aircraftModel.torqueUnit)
            for (tank in tanks) {
                index = (index + formula.index(tank.weight, tank.balancedArm))!!
            }
            return FuelConstant(
                density = density,
                weight = weight,
                index = index
            )
        }
    }
}
