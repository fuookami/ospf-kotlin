/**
 * 预定义物理量纲
 * Predefined Physical Dimensions
 *
 * 定义常用的物理量纲，包括基本物理量、导出物理量、力学量、电磁学量、光学量和放射学量。
 * Defines commonly used physical dimensions, including base quantities, derived quantities, mechanical quantities, electromagnetic quantities, optical quantities, and radiological quantities.
 */
package fuookami.ospf.kotlin.quantities.dimension

// ============================================================================
// 基本物理量 / Fundamental Quantities
// ============================================================================

/** 长度 / Length */
val Length = DerivedQuantity(
    dimension = L,
    name = "length",
    symbol = "L"
)

/** 质量 / Mass */
val Mass = DerivedQuantity(
    dimension = M,
    name = "mass",
    symbol = "m"
)

/** 时间 / Time */
val Time = DerivedQuantity(
    dimension = T,
    name = "time",
    symbol = "t"
)

/** 电流 / Current */
val Current = DerivedQuantity(
    dimension = I,
    name = "current",
    symbol = "I"
)

/** 温度 / Temperature */
val Temperature = DerivedQuantity(
    dimension = Theta,
    name = "temperature",
    symbol = "T"
)

/** 物质的量 / Amount of substance */
val AmountOfSubstance = DerivedQuantity(
    dimension = N,
    name = "amount of substance",
    symbol = "N"
)

/** 发光强度 / Luminous intensity */
val LuminousIntensity = DerivedQuantity(
    dimension = J,
    name = "luminous intensity",
    symbol = "l"
)

/** 平面角 / Plane angle */
val PlaneAngle = DerivedQuantity(
    dimension = rad,
    name = "plane angle",
    symbol = "rad"
)

/** 立体角 / Solid angle */
val SolidAngle = DerivedQuantity(
    dimension = sr,
    name = "solid angle",
    symbol = "sr"
)

/** 信息 / Information */
val Information = DerivedQuantity(
    dimension = B,
    name = "information",
    symbol = "i",
    domain = QuantityDomain.Discrete
)

// ============================================================================
// 基本导出量 / Basic Derived Quantities
// ============================================================================

/** 面积 / Area (S = L²) */
val Area = DerivedQuantity(
    quantity = Length * Length,
    name = "area",
    symbol = "S"
)

/** 体积 / Volume (V = L³) */
val Volume = DerivedQuantity(
    quantity = Area * Length,
    name = "volume",
    symbol = "V"
)

/** 频率 / Frequency (f = t⁻¹) */
val Frequency = DerivedQuantity(
    quantity = Time.unaryMinus(),
    name = "frequency",
    symbol = "f"
)

// ============================================================================
// 力学量 / Mechanics
// ============================================================================

/** 速度 / Velocity (v = L / t) */
val Velocity = DerivedQuantity(
    quantity = Length / Time,
    name = "velocity",
    symbol = "v"
)

/** 角速度 / Angular velocity (ω = rad / t) */
val AngularVelocity = DerivedQuantity(
    quantity = PlaneAngle / Time,
    name = "angular velocity",
    symbol = "w"
)

/** 波数 / Wave number (k = L⁻¹) */
val WaveNumber = DerivedQuantity(
    quantity = Length * -1,
    name = "wave number",
    symbol = "k"
)

/** 加速度 / Acceleration (a = v / t) */
val Acceleration = DerivedQuantity(
    quantity = Velocity / Time,
    name = "acceleration",
    symbol = "a"
)

/** 角加速度 / Angular acceleration (α = ω / t) */
val AngularAcceleration = DerivedQuantity(
    quantity = AngularVelocity / Time,
    name = "angular acceleration",
    symbol = "α"
)

/** 动量 / Momentum (P = mv) */
val Momentum = DerivedQuantity(
    quantity = Mass * Velocity,
    name = "momentum",
    symbol = "M"
)

/** 角动量 / Angular momentum (L = mω) */
val AngularMomentum = DerivedQuantity(
    quantity = Mass * AngularVelocity,
    name = "angular momentum",
    symbol = "L"
)

/** 转动惯量 / Moment of inertia (I = mL²) */
val MomentOfInertia = DerivedQuantity(
    quantity = Mass * (Length * 2),
    name = "moment of inertia",
    symbol = "I"
)

/** 力 / Force (F = ma) */
val Force = DerivedQuantity(
    quantity = Mass * Acceleration,
    name = "force",
    symbol = "F"
)

/** 重力 / Gravity */
val Gravity = DerivedQuantity(
    quantity = Force,
    name = "gravity",
    symbol = "g"
)

/** 压强 / Pressure (p = F / S) */
val Pressure = DerivedQuantity(
    quantity = Force / Area,
    name = "pressure",
    symbol = "h"
)

/** 应力 / Stress */
val Stress = DerivedQuantity(
    quantity = Pressure,
    name = "stress",
    symbol = "σ"
)

/** 冲量 / Impulse (I = Ft) */
val Impulse = DerivedQuantity(
    quantity = Force * Time,
    name = "impulse",
    symbol = "I"
)

/** 力矩 / Torque (τ = FL) */
val Torque = DerivedQuantity(
    quantity = Force * Length,
    name = "torque",
    symbol = "T"
)

/** 质量密度 / Mass density (ρ = m / V) */
val MassDensity = DerivedQuantity(
    quantity = Mass / Volume,
    name = "mass density",
    symbol = "ρ"
)

/** 比体积 / Specific volume (v = V / m) */
val SpecificVolume = DerivedQuantity(
    quantity = Volume / Mass,
    name = "specific volume",
    symbol = "v"
)

/** 能量 / Energy (E = FL) */
val Energy = DerivedQuantity(
    quantity = Force * Length,
    name = "energy",
    symbol = "E"
)

/** 功 / Work */
val Work = DerivedQuantity(
    quantity = Energy,
    name = "work",
    symbol = "W"
)

/** 热量 / Heat */
val Heat = DerivedQuantity(
    quantity = Energy,
    name = "heat",
    symbol = "Q"
)

/** 摩尔浓度 / Molarity (c = N / V) */
val Molarity = DerivedQuantity(
    quantity = AmountOfSubstance / Volume,
    name = "molarity",
    symbol = "c"
)

/** 摩尔体积 / Molar volume (Vm = V / N) */
val MolarVolume = DerivedQuantity(
    quantity = Volume / AmountOfSubstance,
    name = "molar volume",
    symbol = "Vm"
)

/** 熵 / Entropy (S = E / θ) */
val Entropy = DerivedQuantity(
    quantity = Energy / Temperature,
    name = "entropy",
    symbol = "S"
)

/** 摩尔熵 / Molar entropy (Cm = S / N) */
val MolarEntropy = DerivedQuantity(
    quantity = Entropy / AmountOfSubstance,
    name = "molar entropy",
    symbol = "Cm"
)

/** 摩尔热容 / Molar heat capacity */
val MolarHeatCapacity = DerivedQuantity(
    quantity = MolarEntropy,
    name = "molar heat capacity",
    symbol = "Cm"
)

/** 比熵 / Specific entropy (c = S / m) */
val SpecificEntropy = DerivedQuantity(
    quantity = Entropy / Mass,
    name = "specific entropy",
    symbol = "c"
)

/** 比热容 / Specific heat capacity */
val SpecificHeatCapacity = DerivedQuantity(
    quantity = SpecificEntropy,
    name = "specific heat capacity",
    symbol = "c"
)

/** 摩尔能量 / Molar energy (Em = E / N) */
val MolarEnergy = DerivedQuantity(
    quantity = Energy / AmountOfSubstance,
    name = "molar energy",
    symbol = "Em"
)

/** 比能 / Specific energy (h = E / m) */
val SpecificEnergy = DerivedQuantity(
    quantity = Energy / Mass,
    name = "specific energy",
    symbol = "h"
)

/** 能量密度 / Energy density (U = E / V) */
val EnergyDensity = DerivedQuantity(
    quantity = Energy / Volume,
    name = "energy density",
    symbol = "U"
)

/** 热容 / Heat capacity (Cm = E / θ) */
val HeatCapacity = DerivedQuantity(
    quantity = Energy / Temperature,
    name = "heat capacity",
    symbol = "Cm"
)

/** 表面张力 / Surface tension (σ = F / L) */
val SurfaceTension = DerivedQuantity(
    quantity = Force / Length,
    name = "surface tension",
    symbol = "σ"
)

/** 功率 / Power (P = Fv) */
val Power = DerivedQuantity(
    quantity = Force * Velocity,
    name = "power",
    symbol = "P"
)

/** 功率密度 / Power density (E = P / S) */
val PowerDensity = DerivedQuantity(
    quantity = Power / Area,
    name = "power density",
    symbol = "E"
)

/** 辐照度 / Irradiance */
val Irradiance = DerivedQuantity(
    quantity = PowerDensity,
    name = "irradiance",
    symbol = "E"
)

/** 热流密度 / Heat flux density */
val HeatFluxDensity = DerivedQuantity(
    quantity = PowerDensity,
    name = "heat flux density",
    symbol = "E"
)

/** 热导率 / Thermal conductivity (λ = P / Lθ) */
val ThermalConductivity = DerivedQuantity(
    quantity = Power / (Length * Temperature),
    name = "thermal conductivity",
    symbol = "λ"
)

/** 动力粘度 / Dynamic viscosity (μ = pt) */
val DynamicViscosity = DerivedQuantity(
    quantity = Pressure * Time,
    name = "dynamic viscosity",
    symbol = "μ"
)

/** 运动粘度 / Kinematic viscosity (γ = μ / ρ) */
val KinematicViscosity = DerivedQuantity(
    quantity = DynamicViscosity / MassDensity,
    name = "kinematic viscosity",
    symbol = "γ"
)

/** 摩尔质量 / Molar mass (M = m / N) */
val MolarMass = DerivedQuantity(
    quantity = Mass / AmountOfSubstance,
    name = "molar mass",
    symbol = "M"
)

/** 线密度 / Linear density (λm = m / L) */
val LinearDensity = DerivedQuantity(
    quantity = Mass / Length,
    name = "linear density",
    symbol = "λm"
)

/** 面密度 / Surface density (ρA = m / S) */
val SurfaceDensity = DerivedQuantity(
    quantity = Mass / Area,
    name = "surface density",
    symbol = "ρA"
)

/** 作用量 / Action (S = Et) */
val Action = DerivedQuantity(
    quantity = Energy * Time,
    name = "action",
    symbol = "S"
)

/** 流量 / Flow rate (Q = Sv) */
val FlowRate = DerivedQuantity(
    quantity = Area * Velocity,
    name = "flow rate",
    symbol = "Q"
)

// ============================================================================
// 电磁学量 / Electromagnetism
// ============================================================================

/** 电荷 / Electric charge (Q = It) */
val ElectricCharge = DerivedQuantity(
    quantity = Current * Time,
    name = "electric charge",
    symbol = "Q"
)

/** 电荷密度 / Electric charge density (ρq = Q / V) */
val ElectricChargeDensity = DerivedQuantity(
    quantity = ElectricCharge / Volume,
    name = "electric charge density",
    symbol = "ρq"
)

/** 电流密度 / Electric current density (J = I / A) */
val ElectricCurrentDensity = DerivedQuantity(
    quantity = Current / Area,
    name = "electric current density",
    symbol = "J"
)

/** 电势 / Electric potential (U = P / I) */
val ElectricPotential = DerivedQuantity(
    quantity = Power / Current,
    name = "electric potential",
    symbol = "U"
)

/** 电动势 / Electromotive force */
val ElectromotiveForce = DerivedQuantity(
    quantity = ElectricPotential,
    name = "electromotive force",
    symbol = "U"
)

/** 电压 / Voltage */
val Voltage = DerivedQuantity(
    quantity = ElectricPotential,
    name = "voltage",
    symbol = "U"
)

/** 电阻 / Resistance (R = U / I) */
val Resistance = DerivedQuantity(
    quantity = Voltage / Current,
    name = "resistance",
    symbol = "R"
)

/** 阻抗 / Impedance */
val Impedance = DerivedQuantity(
    quantity = Resistance,
    name = "impedance",
    symbol = "R"
)

/** 电导 / Conductance (G = R⁻¹) */
val Conductance = DerivedQuantity(
    quantity = Resistance.reciprocal(),
    name = "conductance",
    symbol = "G"
)

/** 电导率 / Conductivity (κ = G / L) */
val Conductivity = DerivedQuantity(
    quantity = Conductance / Length,
    name = "conductivity",
    symbol = "κ"
)

/** 摩尔电导率 / Molar conductivity (κm = κ × Vm) */
val MolarConductivity = DerivedQuantity(
    quantity = Conductivity * MolarVolume,
    name = "molar conductivity",
    symbol = "κm"
)

/** 电容 / Capacitance (F = Q / U) */
val Capacitance = DerivedQuantity(
    quantity = ElectricCharge / Voltage,
    name = "capacitance",
    symbol = "F"
)

/** 介电常数 / Permittivity (ε = F / L) */
val Permittivity = DerivedQuantity(
    quantity = Capacitance / Length,
    name = "permittivity",
    symbol = "ε"
)

/** 电场强度 / Electric field intensity (E = U / L) */
val ElectricFieldIntensity = DerivedQuantity(
    quantity = Voltage / Length,
    name = "electromotive field intensity",
    symbol = "E"
)

/** 电场强度 / Electric field strength */
val ElectricFieldStrength = DerivedQuantity(
    quantity = ElectricFieldIntensity,
    name = "electric field strength",
    symbol = "E"
)

/** 电感 / Inductance (L = Ut / I) */
val Inductance = DerivedQuantity(
    quantity = Voltage / Current * Time,
    name = "inductance",
    symbol = "L"
)

/** 磁感应强度 / Magnetic field density (B = F / LI) */
val MagneticFieldDensity = DerivedQuantity(
    quantity = Force / (Length * Current),
    name = "magnetic field density",
    symbol = "B"
)

/** 磁场强度 / Magnetic field intensity (H = I / L) */
val MagneticFieldIntensity = DerivedQuantity(
    quantity = Current / Length,
    name = "magnetic field intensity",
    symbol = "H"
)

/** 磁通量 / Magnetic flux (φ = BS) */
val MagneticFlux = DerivedQuantity(
    quantity = MagneticFieldDensity * Area,
    name = "magnetic flux",
    symbol = "φ"
)

/** 磁导率 / Magnetic permeability (μ = L / L) */
val MagneticPermeability = DerivedQuantity(
    quantity = Inductance / Length,
    name = "magnetic permeability",
    symbol = "μ"
)

/** 磁阻 / Magnetic reluctance (λ = μ⁻¹) */
val MagneticReluctance = DerivedQuantity(
    quantity = MagneticPermeability.reciprocal(),
    name = "magnetic reluctance",
    symbol = "λ"
)

/** 电荷线密度 / Electric charge linear density (λq = Q / L) */
val ElectricChargeLinearDensity = DerivedQuantity(
    quantity = ElectricCharge / Length,
    name = "electric charge linear density",
    symbol = "λq"
)

/** 电荷面密度 / Electric charge surface density (σq = Q / S) */
val ElectricChargeSurfaceDensity = DerivedQuantity(
    quantity = ElectricCharge / Area,
    name = "electric charge surface density",
    symbol = "σq"
)

// ============================================================================
// 光学量 / Optics
// ============================================================================

/** 光通量 / Luminous flux (φ = l × sr) */
val LuminousFlux = DerivedQuantity(
    quantity = LuminousIntensity * SolidAngle,
    name = "luminous flux",
    symbol = "φ"
)

/** 照度 / Illuminance (Ev = φ / S) */
val Illuminance = DerivedQuantity(
    quantity = LuminousFlux / Area,
    name = "illuminance",
    symbol = "Ev"
)

/** 亮度 / Luminance (L = l / S) */
val Luminance = DerivedQuantity(
    quantity = LuminousIntensity / Area,
    name = "luminance",
    symbol = "L"
)

// ============================================================================
// 放射学量 / Radiology
// ============================================================================

/** 放射性活度 / Activity (A = t⁻¹) */
val Activity = DerivedQuantity(
    quantity = Time.reciprocal(),
    name = "activity",
    symbol = "A"
)

/** 吸收剂量 / Absorbed dose (D = E / m) */
val AbsorbedDose = DerivedQuantity(
    quantity = Energy / Mass,
    name = "absorbed dose",
    symbol = "D"
)

/** 剂量当量 / Dose equivalent */
val DoseEquivalent = DerivedQuantity(
    quantity = AbsorbedDose,
    name = "dose equivalent",
    symbol = "D"
)

/** 剂量率 / Dosing rate (d = D / t) */
val DosingRate = DerivedQuantity(
    quantity = DoseEquivalent / Time,
    name = "dosing rate",
    symbol = "d"
)

// ============================================================================
// 其他 / Other
// ============================================================================

/** 催化活性 / Catalytic activity (N / t) */
val CatalyticActivity = DerivedQuantity(AmountOfSubstance / Time, "catalytic activity")

/** 带宽 / Bandwidth (B = i / t) */
val Bandwidth = DerivedQuantity(
    quantity = Information / Time,
    name = "bandwidth",
    symbol = "B"
)
