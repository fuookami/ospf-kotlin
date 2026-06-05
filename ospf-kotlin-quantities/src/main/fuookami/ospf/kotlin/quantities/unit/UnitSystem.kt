/**
 * 单位制
 * Unit system
 *
 * 定义不同单位制（如 SI、CGS、英制等）的标准单位和比例。
 * Defines standard units and scales for different unit systems (SI, CGS, Imperial, etc).
 *
 * 核心概念 / Core concepts:
 * - [UnitSystem]: 单位制接口 / Unit system interface
 * - [ConcreteUnitSystem]: 具体单位制实现 / Concrete unit system implementation
 * - [UnitSystemBuilder]: 单位制构建器 / Unit system builder
 * - [SI]: 国际单位制 / International System of Units
 * - [MKS]: 米-千克-秒单位制 / Meter-Kilogram-Second system
 * - [CGS]: 厘米-克-秒单位制 / Centimeter-Gram-Second system
 *
 * 单位制定义了基本单位，导出单位通过懒加载自动推导。
 * Unit systems define base units, derived units are lazily computed.
 *
 * 预定义单位制 / Predefined unit systems:
 * - SI: 国际单位制（米、千克、秒、安培、开尔文、摩尔、坎德拉）
 * - SI: International System of Units (meter, kilogram, second, ampere, kelvin, mole, candela)
 * - MKS: 米-千克-秒单位制 / Meter-Kilogram-Second system
 * - CGS: 厘米-克-秒单位制 / Centimeter-Gram-Second system
 */
package fuookami.ospf.kotlin.quantities.unit

import java.util.concurrent.ConcurrentHashMap
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.*

/**
 * 单位制接口
 * Unit system interface
 *
 * 定义不同单位制（如 SI、CGS、英制等）的标准单位和比例。
 * Defines standard units and scales for different unit systems (SI, CGS, Imperial, etc).
 *
 * 单位制定义了基本单位，导出单位通过懒加载自动推导。
 * Unit systems define base units, derived units are lazily computed.
 */
interface UnitSystem {
    /**
     * 单位制名称
     * Unit system name
     */
    val name: String

    /**
     * 基本单位映射
     * Base units mapping
     *
     * 将基本量纲映射到对应的基本单位。
     * Maps fundamental dimensions to their corresponding base units.
     */
    val baseUnits: Map<FundamentalQuantityDimension, PhysicalUnit>

    /**
     * 用户指定的标准单位
     * User-specified standard units
     *
     * 允许用户为特定量纲指定非默认的标准单位。
     * Allows users to specify non-default standard units for specific dimensions.
     */
    val standardUnits: MutableMap<DerivedQuantity, PhysicalUnit>

    /**
     * 导出单位缓存
     * Derived units cache
     *
     * 缓存已推导的导出单位，避免重复计算。
     * Caches derived units to avoid repeated computation.
     */
    val derivedCache: MutableMap<DerivedQuantity, PhysicalUnit>

    /**
     * 获取指定量纲的标准单位
     * Get standard unit for dimension
     *
     * 如果用户指定了标准单位，返回用户指定的；否则返回推导的默认单位。
     * Returns user-specified standard unit if set, otherwise returns derived default unit.
     *
     * @param quantity 量纲 / Dimension
     * @return 标准单位，如果不支持该量纲返回 null / Standard unit, or null if dimension is not supported
     */
    fun standardUnitForDimension(quantity: DerivedQuantity): PhysicalUnit? {
        // 1. 首先检查用户是否指定了标准单位
        // First check if user has specified a standard unit
        if (standardUnits.containsKey(quantity)) {
            return standardUnits[quantity]
        }

        // 2. 否则使用推导的默认单位
        // Otherwise use derived default unit
        return unitForDimension(quantity)
    }

    /**
     * 设置指定量纲的标准单位
     * Set standard unit for dimension
     *
     * 允许在单位制创建后动态修改标准单位。
     * Allows dynamic modification of standard units after unit system creation.
     *
     * @param quantity 量纲 / Dimension
     * @param unit 标准单位 / Standard unit
     */
    fun setStandardUnit(quantity: DerivedQuantity, unit: PhysicalUnit) {
        standardUnits[quantity] = unit
    }

    /**
     * 移除指定量纲的标准单位（恢复使用默认推导单位）
     * Remove standard unit for dimension (revert to default derived unit)
     *
     * @param quantity 量纲 / Dimension
     * @return 如果成功移除返回 true，如果不存在返回 false / Returns true if removed, false if not found
     */
    fun removeStandardUnit(quantity: DerivedQuantity): Boolean {
        return standardUnits.remove(quantity) != null
    }

    /**
     * 获取指定量纲的单位（懒加载推导）
     * Get unit for dimension (lazy derivation)
     *
     * @param quantity 量纲 / Dimension
     * @return 单位，如果不支持该量纲返回 null / Unit, or null if dimension is not supported
     */
    fun unitForDimension(quantity: DerivedQuantity): PhysicalUnit? {
        // 1. 检查是否是基本量纲
        // Check if it's a fundamental dimension
        if (quantity.quantities.size == 1) {
            val fq = quantity.quantities.first()
            if (fq.index == 1) {
                // 尝试从基本单位获取
                // Try to get from base units
                return baseUnits[fq.dimension]
            }
        }

        // 2. 检查缓存
        // Check cache
        if (derivedCache.containsKey(quantity)) {
            return derivedCache[quantity]
        }

        // 3. 推导单位
        // Derive unit
        val unit = deriveUnit(quantity) ?: return null

        // 4. 存入缓存
        // Store in cache
        derivedCache[quantity] = unit

        return unit
    }

    /**
     * 推导指定量纲的单位
     * Derive unit for dimension
     *
     * 根据基本单位的组合推导出导出单位。
     * Derives a unit from combinations of base units.
     *
     * @param quantity 量纲 / Dimension
     * @return 推导的单位，如果不支持返回 null / Derived unit, or null if not supported
     */
    fun deriveUnit(quantity: DerivedQuantity): PhysicalUnit? {
        var resultScale = Scale()
        val symbolParts = mutableListOf<String>()

        for (fq in quantity.quantities) {
            val baseUnit = baseUnits[fq.dimension] ?: return null
            val power = fq.index

            if (power != 0) {
                // 累积比例
                // Accumulate scale
                val unitScale = baseUnit.scale
                resultScale = when {
                    power == 1 -> resultScale * unitScale
                    power == -1 -> resultScale / unitScale
                    else -> {
                        // 处理非整数幂 / Handle non-integer powers
                        var powScale = Scale()
                        val absPower = if (power > 0) power else -power
                        for (i in 1..absPower) {
                            powScale *= unitScale
                        }
                        if (power > 0) resultScale * powScale else resultScale / powScale
                    }
                }

                // 构建符号
                // Build symbol
                val unitSym = baseUnit.symbol ?: ""
                when {
                    power == 1 -> symbolParts.add(unitSym)
                    power == -1 -> symbolParts.add("$unitSym/")
                    else -> symbolParts.add("${unitSym}^$power")
                }
            }
        }

        val symbol = symbolParts.joinToString("·")
        val name = "derived_${quantity.dimensionSymbol()}"

        return AnonymousPhysicalUnit(
            quantity = quantity,
            conversionRule = UnitConversionRule.Linear(resultScale),
            name = name,
            symbol = symbol
        )
    }

    /**
     * 获取指定量纲相对于标准单位制的比例
     * Get conversion scale to standard system for dimension
     *
     * @param quantity 量纲 / Dimension
     * @return 转换比例，如果不支持返回 null / Conversion scale, or null if not supported
     */
    fun conversionToStandard(quantity: DerivedQuantity): Scale? {
        val unit = unitForDimension(quantity) ?: return null
        return unit.scale
    }

    /**
     * 检查是否支持指定量纲
     * Check if quantity is supported
     *
     * @param quantity 量纲 / Dimension
     * @return 如果支持返回 true，否则返回 false / Returns true if supported, false otherwise
     */
    fun supportsQuantity(quantity: DerivedQuantity): Boolean {
        return unitForDimension(quantity) != null
    }
}

/**
 * 具体单位制实现
 * Concrete unit system implementation
 *
 * @param name 单位制名称 / Unit system name
 * @param baseUnits 基本单位映射 / Base units mapping
 * @param standardUnits 用户指定的标准单位 / User-specified standard units
 * @param derivedCache 导出单位缓存 / Derived units cache
 */
data class ConcreteUnitSystem(
    override val name: String,
    override val baseUnits: Map<FundamentalQuantityDimension, PhysicalUnit>,
    override val standardUnits: MutableMap<DerivedQuantity, PhysicalUnit> = ConcurrentHashMap(),
    override val derivedCache: MutableMap<DerivedQuantity, PhysicalUnit> = ConcurrentHashMap()
) : UnitSystem

/**
 * 单位制构建器
 * Unit system builder
 *
 * 使用 UnitSystemBuilder 可以创建自定义单位制。
 * Use UnitSystemBuilder to create custom unit systems.
 *
 * 使用方法 / Usage:
 * - new(): 创建空单位制 / Create empty unit system
 * - fromPrototype(): 从现有单位制继承 / Inherit from existing unit system
 * - withBaseUnit(): 添加基本单位 / Add base unit
 * - withDerivedUnit(): 添加导出单位 / Add derived unit
 * - withStandardUnit(): 设置标准单位 / Set standard unit
 * - build(): 构建单位制 / Build unit system
 */
class UnitSystemBuilder {
    private val name: String
    private var prototype: UnitSystem? = null
    private val baseUnits: MutableMap<FundamentalQuantityDimension, PhysicalUnit>
    private val derivedUnits: MutableMap<DerivedQuantity, PhysicalUnit>
    private val standardUnits: MutableMap<DerivedQuantity, PhysicalUnit>

    /**
     * 创建新的单位制构建器
     * Create new unit system builder
     *
     * @param name 单位制名称 / Unit system name
     */
    constructor(name: String) {
        this.name = name
        this.prototype = null
        this.baseUnits = mutableMapOf()
        this.derivedUnits = mutableMapOf()
        this.standardUnits = mutableMapOf()
    }

    /**
     * 从原型单位制创建（继承其所有单位）
     * Create from prototype unit system (inherit all units)
     *
     * @param name 单位制名称 / Unit system name
     * @param prototype 原型单位制 / Prototype unit system
     */
    constructor(name: String, prototype: UnitSystem) {
        this.name = name
        this.prototype = prototype
        this.baseUnits = prototype.baseUnits.toMutableMap()
        this.derivedUnits = mutableMapOf()
        this.standardUnits = prototype.standardUnits.toMutableMap()
    }

    /**
     * 添加/替换基本单位
     * Add/replace base unit
     *
     * @param dimension 基本量纲 / Fundamental dimension
     * @param unit 基本单位 / Base unit
     * @return 构建器实例 / Builder instance
     */
    fun withBaseUnit(dimension: FundamentalQuantityDimension, unit: PhysicalUnit): UnitSystemBuilder {
        baseUnits[dimension] = unit
        return this
    }

    /**
     * 添加/替换导出单位
     * Add/replace derived unit
     *
     * @param quantity 量纲 / Dimension
     * @param unit 导出单位 / Derived unit
     * @return 构建器实例 / Builder instance
     */
    fun withDerivedUnit(quantity: DerivedQuantity, unit: PhysicalUnit): UnitSystemBuilder {
        derivedUnits[quantity] = unit
        return this
    }

    /**
     * 设置指定量纲的标准单位
     * Set standard unit for dimension
     *
     * 标准单位用于将物理量转换为该量纲的标准表示。
     * Standard units are used to convert quantities to standard representation for that dimension.
     *
     * @param quantity 量纲 / Dimension
     * @param unit 标准单位 / Standard unit
     * @return 构建器实例 / Builder instance
     */
    fun withStandardUnit(quantity: DerivedQuantity, unit: PhysicalUnit): UnitSystemBuilder {
        standardUnits[quantity] = unit
        return this
    }

    /**
     * 构建单位制
     * Build unit system
     *
     * @return 构建的单位制实例 / Built unit system instance
     */
    fun build(): UnitSystem {
        val system = ConcreteUnitSystem(
            name = name,
            baseUnits = baseUnits.toMap(),
            standardUnits = ConcurrentHashMap(standardUnits),
            derivedCache = ConcurrentHashMap(derivedUnits)
        )
        return system
    }
}

// ============================================================================
// SI 单位制 / SI unit system
// ============================================================================

/**
 * SI 单位制（国际单位制）
 * SI unit system (International System of Units)
 *
 * 基本单位 / Base units:
 * - 长度: 米 (m) / Length: meter (m)
 * - 质量: 千克 (kg) / Mass: kilogram (kg)
 * - 时间: 秒 (s) / Time: second (s)
 * - 电流: 安培 (A) / Electric current: ampere (A)
 * - 温度: 开尔文 (K) / Temperature: kelvin (K)
 * - 物质的量: 摩尔 (mol) / Amount of substance: mole (mol)
 * - 发光强度: 坎德拉 (cd) / Luminous intensity: candela (cd)
 * - 信息: 比特 (bit) / Information: bit (bit)
 * - 平面角: 弧度 (rad) / Plane angle: radian (rad)
 * - 立体角: 球面度 (sr) / Solid angle: steradian (sr)
 */
object SI : UnitSystem {
    override val name: String = "SI"

    override val baseUnits: Map<FundamentalQuantityDimension, PhysicalUnit> by lazy {
        mapOf(
            StandardFundamentalQuantityDimension.Length to Meter,
            StandardFundamentalQuantityDimension.Mass to Kilogram,
            StandardFundamentalQuantityDimension.Time to Second,
            StandardFundamentalQuantityDimension.Current to Ampere,
            StandardFundamentalQuantityDimension.Temperature to Kelvin,
            StandardFundamentalQuantityDimension.SubstanceAmount to Mole,
            StandardFundamentalQuantityDimension.LuminousIntensity to Candela,
            StandardFundamentalQuantityDimension.Information to Bit,
            StandardFundamentalQuantityDimension.PlaneAngle to Radian,
            StandardFundamentalQuantityDimension.SolidAngle to Steradian
        )
    }

    override val standardUnits: MutableMap<DerivedQuantity, PhysicalUnit> by lazy {
        ConcurrentHashMap(
            mapOf(
                // 基础量纲标准单位 / Base dimension standard units
                DerivedQuantity(L, "length", "L") to Meter,
                DerivedQuantity(M, "mass", "m") to Kilogram,
                DerivedQuantity(T, "time", "t") to Second,
                DerivedQuantity(I, "current", "I") to Ampere,
                DerivedQuantity(N, "amount of substance", "N") to Mole,
                DerivedQuantity(rad, "plane angle", "rad") to Radian,

                // 导出量纲标准单位 / Derived dimension standard units
                Area to SquareMeter,
                Volume to CubicMeter,
                Frequency to Hertz,
                Force to Newton,
                Pressure to Pascal,
                Energy to Joule,
                Voltage to Volt,
                ElectricCharge to Coulomb,
                Capacitance to Farad,
                Resistance to Ohm
            )
        )
    }

    override val derivedCache: MutableMap<DerivedQuantity, PhysicalUnit> = ConcurrentHashMap()

    override fun toString(): String = name
}

// ============================================================================
// MKS 单位制 / MKS unit system
// ============================================================================

/**
 * MKS 单位制（米-千克-秒）
 * MKS unit system (meter-kilogram-second)
 *
 * 这是 SI 单位制的子集，只包含力学量纲。
 * This is a subset of SI, containing only mechanical dimensions.
 *
 * 基本单位 / Base units:
 * - 长度: 米 (m) / Length: meter (m)
 * - 质量: 千克 (kg) / Mass: kilogram (kg)
 * - 时间: 秒 (s) / Time: second (s)
 */
object MKS : UnitSystem {
    override val name: String = "MKS"

    override val baseUnits: Map<FundamentalQuantityDimension, PhysicalUnit> by lazy {
        mapOf(
            StandardFundamentalQuantityDimension.Length to Meter,
            StandardFundamentalQuantityDimension.Mass to Kilogram,
            StandardFundamentalQuantityDimension.Time to Second
        )
    }

    override val standardUnits: MutableMap<DerivedQuantity, PhysicalUnit> = ConcurrentHashMap<DerivedQuantity, PhysicalUnit>()

    override val derivedCache: MutableMap<DerivedQuantity, PhysicalUnit> = ConcurrentHashMap<DerivedQuantity, PhysicalUnit>()

    override fun toString(): String = name
}

// ============================================================================
// CGS 单位制 / CGS unit system
// ============================================================================

/**
 * CGS 单位制（厘米-克-秒）
 * CGS unit system (centimeter-gram-second)
 *
 * 这是早期物理学常用的单位制，主要用于力学量纲。
 * This is a unit system commonly used in early physics, mainly for mechanical dimensions.
 *
 * 基本单位 / Base units:
 * - 长度: 厘米 (cm) / Length: centimeter (cm)
 * - 质量: 克 (g) / Mass: gram (g)
 * - 时间: 秒 (s) / Time: second (s)
 */
object CGS : UnitSystem {
    override val name: String = "CGS"

    override val baseUnits: Map<FundamentalQuantityDimension, PhysicalUnit> by lazy {
        mapOf(
            StandardFundamentalQuantityDimension.Length to Centimeter,
            StandardFundamentalQuantityDimension.Mass to Gram,
            StandardFundamentalQuantityDimension.Time to Second
        )
    }

    override val standardUnits: MutableMap<DerivedQuantity, PhysicalUnit> = ConcurrentHashMap<DerivedQuantity, PhysicalUnit>()

    override val derivedCache: MutableMap<DerivedQuantity, PhysicalUnit> = ConcurrentHashMap<DerivedQuantity, PhysicalUnit>()

    override fun toString(): String = name
}
