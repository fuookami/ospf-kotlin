/**
 * 质量单位 / Mass units
 *
 * 用于测量物体质量的单位。
 * Units for measuring the mass of objects.
 *
 * 单位常量来源 / Unit constant sources:
 * - Kilogram: SI基本单位 / SI base unit
 * - Gram: 1 g = 10⁻³ kg
 * - Pound: 1 lb = 453.59237 g（精确值，国际磅）/ 1 lb = 453.59237 g (exact, international pound)
 * - Point: 珠宝行业质量单位，1 pt = 2 mg / Point: jewelry industry mass unit, 1 pt = 2 mg
 * - Carat: 珠宝行业质量单位，1 ct = 200 mg / Carat: jewelry industry mass unit, 1 ct = 200 mg
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Mass

/**
 * 千克（基本单位）/ Kilogram (base unit)
 *
 * 质量的SI基本单位，定义为普朗克常数为6.62607015×10⁻³⁴ J·s时的质量单位。
 * The SI base unit of mass, defined by fixing the Planck constant to 6.62607015×10⁻³⁴ J·s.
 *
 * 符号 / Symbol: kg
 * 换算关系 / Conversion: 1 kg = 1 kg（基本单位）/ 1 kg = 1 kg (base unit)
 */
object Kilogram : PhysicalUnit() {
    /** 单位名称：kilogram / Unit name: kilogram */
    override val name: String = "kilogram"
    /** 单位符号：kg / Unit symbol: kg */
    override val symbol: String = "kg"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
    /** 比例因子：1（基本单位）/ Scale factor: 1 (base unit) */
    override val scale = Scale()
}

/**
 * 微克 / Microgram
 *
 * 百万分之一克。
 * One millionth of a gram.
 *
 * 符号 / Symbol: µg
 * 换算关系 / Conversion: 1 µg = 10⁻⁶ g = 10⁻⁹ kg
 */
object Microgram : DerivedPhysicalUnit(Gram * Scale.micro) {
    /** 单位名称：microgram / Unit name: microgram */
    override val name: String = "microgram"
    /** 单位符号：µg / Unit symbol: µg */
    override val symbol: String = "µg"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 毫克 / Milligram
 *
 * 千分之一克。
 * One thousandth of a gram.
 *
 * 符号 / Symbol: mg
 * 换算关系 / Conversion: 1 mg = 10⁻³ g = 10⁻⁶ kg
 */
object Milligram : DerivedPhysicalUnit(Gram * Scale.milli) {
    /** 单位名称：milligram / Unit name: milligram */
    override val name: String = "milligram"
    /** 单位符号：mg / Unit symbol: mg */
    override val symbol: String = "mg"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 点 / Point
 *
 * 珠宝行业使用的质量单位。
 * Mass unit used in the jewelry industry.
 *
 * 符号 / Symbol: pt
 * 换算关系 / Conversion: 1 pt = 2 mg
 */
object Point : DerivedPhysicalUnit(Milligram * 2) {
    /** 单位名称：point / Unit name: point */
    override val name: String = "point"
    /** 单位符号：pt / Unit symbol: pt */
    override val symbol: String = "pt"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 克拉 / Carat
 *
 * 珠宝行业使用的质量单位，主要用于称量宝石。
 * Mass unit used in the jewelry industry, primarily for weighing gemstones.
 *
 * 符号 / Symbol: ct
 * 换算关系 / Conversion: 1 ct = 200 mg = 0.2 g
 */
object Carat : DerivedPhysicalUnit(Point * Scale(10, 2)) {
    /** 单位名称：carat / Unit name: carat */
    override val name: String = "carat"
    /** 单位符号：ct / Unit symbol: ct */
    override val symbol: String = "ct"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 克 / Gram
 *
 * 千分之一千克。
 * One thousandth of a kilogram.
 *
 * 符号 / Symbol: g
 * 换算关系 / Conversion: 1 g = 10⁻³ kg
 */
object Gram : DerivedPhysicalUnit(Kilogram / Scale.kilo) {
    /** 单位名称：gram / Unit name: gram */
    override val name: String = "gram"
    /** 单位符号：g / Unit symbol: g */
    override val symbol: String = "g"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 公担 / Kintal
 *
 * 一百千克。
 * One hundred kilograms.
 *
 * 符号 / Symbol: q
 * 换算关系 / Conversion: 1 q = 100 kg
 */
object Kintal : DerivedPhysicalUnit(Kilogram * Scale.hecto) {
    /** 单位名称：kintal / Unit name: kintal */
    override val name: String = "kintal"
    /** 单位符号：q / Unit symbol: q */
    override val symbol: String = "q"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 吨 / Ton
 *
 * 一千千克，也称为公吨。
 * One thousand kilograms, also known as metric ton.
 *
 * 符号 / Symbol: t
 * 换算关系 / Conversion: 1 t = 1000 kg
 */
object Ton : DerivedPhysicalUnit(Kilogram * Scale.kilo) {
    /** 单位名称：ton / Unit name: ton */
    override val name: String = "ton"
    /** 单位符号：t / Unit symbol: t */
    override val symbol: String = "t"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 磅 / Pound
 *
 * 英制质量单位。
 * Imperial mass unit.
 *
 * 符号 / Symbol: lb
 * 换算关系 / Conversion: 1 lb = 453.59237 g（精确值）/ 1 lb = 453.59237 g (exact)
 */
object Pound : DerivedPhysicalUnit(Gram * 453.59237) {
    /** 单位名称：pound / Unit name: pound */
    override val name: String = "pound"
    /** 单位符号：lb / Unit symbol: lb */
    override val symbol: String = "lb"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 格令 / Grain
 *
 * 英制质量单位，最古老的质量单位之一。
 * Imperial mass unit, one of the oldest mass units.
 *
 * 符号 / Symbol: gr
 * 换算关系 / Conversion: 1 gr = 1/7000 lb ≈ 64.79891 mg
 */
object Gran : DerivedPhysicalUnit(Pound * Scale(7000, -1)) {
    /** 单位名称：gran / Unit name: gran */
    override val name: String = "gran"
    /** 单位符号：gr / Unit symbol: gr */
    override val symbol: String = "gr"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 长吨 / Long Ton
 *
 * 英制质量单位，主要用于英国。
 * Imperial mass unit, primarily used in the UK.
 *
 * 符号 / Symbol: lt
 * 换算关系 / Conversion: 1 lt = 2240 lb ≈ 1016.047 kg
 */
object LongTon : DerivedPhysicalUnit(Pound * 2240) {
    /** 单位名称：long ton / Unit name: long ton */
    override val name: String = "long ton"
    /** 单位符号：lt / Unit symbol: lt */
    override val symbol: String = "lt"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 短吨 / Short Ton
 *
 * 英制质量单位，主要用于美国。
 * Imperial mass unit, primarily used in the US.
 *
 * 符号 / Symbol: st
 * 换算关系 / Conversion: 1 st = 2000 lb ≈ 907.185 kg
 */
object ShortTon : DerivedPhysicalUnit(Pound * 2000) {
    /** 单位名称：short ton / Unit name: short ton */
    override val name: String = "short ton"
    /** 单位符号：st / Unit symbol: st */
    override val symbol: String = "st"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 英石 / Stone
 *
 * 英制质量单位，主要用于英国。
 * Imperial mass unit, primarily used in the UK.
 *
 * 符号 / Symbol: st
 * 换算关系 / Conversion: 1 st = 14 lb ≈ 6.350 kg
 */
object Stone : DerivedPhysicalUnit(Pound * 14) {
    /** 单位名称：stone / Unit name: stone */
    override val name: String = "stone"
    /** 单位符号：st / Unit symbol: st */
    override val symbol: String = "st"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 盎司 / Ounce
 *
 * 英制质量单位。
 * Imperial mass unit.
 *
 * 符号 / Symbol: oz
 * 换算关系 / Conversion: 1 oz = 1/16 lb ≈ 28.35 g
 */
object Ounce : DerivedPhysicalUnit(Pound * Scale(2, -4)) {
    /** 单位名称：ounce / Unit name: ounce */
    override val name: String = "ounce"
    /** 单位符号：oz / Unit symbol: oz */
    override val symbol: String = "oz"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 金衡盎司 / Troy Ounce
 *
 * 珠宝行业使用的质量单位，用于称量贵金属。
 * Mass unit used in the jewelry industry for weighing precious metals.
 *
 * 符号 / Symbol: oz.tr
 * 换算关系 / Conversion: 1 oz.tr = 1/12 lb ≈ 37.80 g
 */
object TroyOunce : DerivedPhysicalUnit(Pound * Scale(12, -1)) {
    /** 单位名称：troy ounce / Unit name: troy ounce */
    override val name: String = "ounce"
    /** 单位符号：oz.tr / Unit symbol: oz.tr */
    override val symbol: String = "oz.tr"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}

/**
 * 打兰 / Dram
 *
 * 英制质量单位。
 * Imperial mass unit.
 *
 * 符号 / Symbol: dr
 * 换算关系 / Conversion: 1 dr = 1/16 oz ≈ 1.77 g
 */
object Dram : DerivedPhysicalUnit(Ounce * Scale(2, -4)) {
    /** 单位名称：dram / Unit name: dram */
    override val name: String = "dram"
    /** 单位符号：dr / Unit symbol: dr */
    override val symbol: String = "dr"

    /** 对应物理量：质量 / Corresponding quantity: Mass */
    override val quantity = Mass
}
