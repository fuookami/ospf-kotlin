package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

/**
 * 单位制接口
 * 定义不同单位制（如 SI、CGS、英制等）的标准单位和比例
 */
interface UnitSystem {
    val name: String

    /**
     * 基础量纲的比例因子
     */
    val scales: Map<FundamentalQuantityDimension, Scale>

    /**
     * 导出量纲的标准单位
     */
    val standardUnits: Map<DerivedQuantity, PhysicalUnit>

    /**
     * 获取指定量纲的标准单位
     */
    fun standardUnitForDimension(quantity: DerivedQuantity): PhysicalUnit? {
        return standardUnits[quantity]
    }

    /**
     * 获取指定量纲的比例因子
     */
    fun getScaleFactor(quantity: DerivedQuantity): Scale {
        // 计算导出量纲的比例因子（基于基础量纲的比例）
        if (quantity.isNone()) {
            return Scale()
        }
        
        var result = Scale()
        for (fq in quantity.quantities) {
            val baseScale = scales[fq.dimension] ?: Scale()
            // 幂次运算：scale^power
            val powerScale = if (fq.index > 0) {
                var s = Scale()
                for (i in 1..fq.index) s *= baseScale
                s
            } else {
                var s = Scale()
                for (i in 1..-fq.index) s /= baseScale
                s
            }
            result = result * powerScale
        }
        return result
    }

    /**
     * 获取指定量纲的单位
     */
    fun getUnitForQuantity(quantity: DerivedQuantity): PhysicalUnit? {
        return standardUnits[quantity]
    }

    /**
     * 检查是否支持指定量纲
     */
    fun supportsQuantity(quantity: DerivedQuantity): Boolean {
        return standardUnits.containsKey(quantity)
    }
}

/**
 * 单位制构建器
 */
class UnitSystemBuilder(private val name: String) {
    private val scales = mutableMapOf<FundamentalQuantityDimension, Scale>()
    private val standardUnits = mutableMapOf<DerivedQuantity, PhysicalUnit>()

    /**
     * 设置基础量纲的比例
     */
    fun withScale(dimension: FundamentalQuantityDimension, scale: Scale): UnitSystemBuilder {
        scales[dimension] = scale
        return this
    }

    /**
     * 设置标准单位
     */
    fun withStandardUnit(quantity: DerivedQuantity, unit: PhysicalUnit): UnitSystemBuilder {
        standardUnits[quantity] = unit
        return this
    }

    /**
     * 构建单位制
     */
    fun build(): UnitSystem {
        return ConcreteUnitSystem(name, scales.toMap(), standardUnits.toMap())
    }
}

/**
 * 具体单位制实现
 */
data class ConcreteUnitSystem(
    override val name: String,
    override val scales: Map<FundamentalQuantityDimension, Scale>,
    override val standardUnits: Map<DerivedQuantity, PhysicalUnit>
) : UnitSystem

/**
 * SI 单位制（国际单位制）
 */
data object SI : UnitSystem {
    override val name: String = "SI"

    override val scales: Map<FundamentalQuantityDimension, Scale> by lazy {
        StandardFundamentalQuantityDimension.entries.associateWith<FundamentalQuantityDimension, Scale> { Scale() }
    }

    override val standardUnits: Map<DerivedQuantity, PhysicalUnit> by lazy {
        buildMap<DerivedQuantity, PhysicalUnit> {
            // 基础量纲标准单位 - 只使用现有文件中定义的单位
            put(DerivedQuantity(L, "length", "L"), Meter)
            put(DerivedQuantity(M, "mass", "m"), Kilogram)
            put(DerivedQuantity(T, "time", "t"), Second)
            put(DerivedQuantity(I, "current", "I"), Ampere)
            put(DerivedQuantity(N, "amount of substance", "N"), Mole)
            put(DerivedQuantity(rad, "plane angle", "rad"), Radian)
            
            // 导出量纲标准单位 - 只使用现有文件中定义的单位
            put(Area, SquareMeter)
            put(Volume, CubicMeter)
            put(Frequency, Hertz)
            put(Force, Newton)
            put(Pressure, Pascal)
            put(Energy, Joule)
            put(Voltage, Volt)
            put(ElectricCharge, Coulomb)
            put(Capacitance, Farad)
            put(Resistance, Ohm)
        }
    }

    override fun standardUnitForDimension(quantity: DerivedQuantity): PhysicalUnit? {
        return standardUnits[quantity]
    }

    override fun toString(): String {
        return name
    }
}
