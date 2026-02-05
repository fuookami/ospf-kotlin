package fuookami.ospf.kotlin.utils.physics.dimension

// Fundamental Quantity
// L
val Length = DerivedQuantity(
    dimension = L,
    name = "length",
    symbol = "L"
)
// m
val Mass = DerivedQuantity(
    dimension = M,
    name = "mass",
    symbol = "m"
)
// t
val Time = DerivedQuantity(
    dimension = T,
    name = "time",
    symbol = "t"
)
// I
val Current = DerivedQuantity(
    dimension = I,
    name = "current",
    symbol = "I"
)
// θ
val Temperature = DerivedQuantity(
    dimension = Theta,
    name = "temperature",
    symbol = "T"
)
// N
val AmountOfSubstance = DerivedQuantity(
    dimension = N,
    name = "amount of substance",
    symbol = "N"
)
// l
val LuminousIntensity = DerivedQuantity(
    dimension = J,
    name = "luminous intensity",
    symbol = "l"
)
// rad
val PlaneAngle = DerivedQuantity(
    dimension = rad,
    name = "plane angle",
    symbol = "rad"
)
// sr
val SolidAngle = DerivedQuantity(
    dimension = sr,
    name = "solid angle",
    symbol = "sr"
)
// i
val Information = DerivedQuantity(
    dimension = B,
    name = "information",
    symbol = "i"
)

// Basic Derived Quantities
// S = L^2
val Area = DerivedQuantity(
    quantity = Length * Length,
    name = "area",
    symbol = "S"
)
// V = L^3
val Volume = DerivedQuantity(
    quantity = Area * Length,
    name = "volume",
    symbol = "V"
)
// f = t^-1
val Frequency = DerivedQuantity(
    quantity = Length.unaryMinus(),
    name = "frequency",
    symbol = "f"
)

// Mechanics
// v = L / t
val Velocity = DerivedQuantity(
    quantity = Length / Time,
    name = "velocity",
    symbol = "v"
)
// ω = rad / t
val AngularVelocity = DerivedQuantity(
    quantity = PlaneAngle / Time,
    name = "angular velocity",
    symbol = "w"
)
// k = L^-1
val WaveNumber = DerivedQuantity(
    quantity = Length * -1,
    name = "wave number",
    symbol = "k"
)
// a = v / t
val Acceleration = DerivedQuantity(
    quantity = Velocity / Time,
    name = "acceleration",
    symbol = "a"
)
// α = ω / t
val AngularAcceleration = DerivedQuantity(
    quantity = AngularVelocity / Time,
    name = "angular acceleration",
    symbol = "α"
)
// P = mv
val Momentum = DerivedQuantity(
    quantity = Mass * Velocity,
    name = "momentum",
    symbol = "M"
)
// L = mω
val AngularMomentum = DerivedQuantity(
    quantity = Mass * AngularVelocity,
    name = "angular momentum",
    symbol = "L"
)
// l = mL^2
val MomentOfInertia = DerivedQuantity(
    quantity = Mass * (Length * 2),
    name = "moment of inertia",
    symbol = "I"
)
// F = ma
val Force = DerivedQuantity(
    quantity = Mass * Acceleration,
    name = "force",
    symbol = "F"
)
val Gravity = DerivedQuantity(
    quantity = Force,
    name = "gravity",
    symbol = "g"
)
// p = F / S
val Pressure = DerivedQuantity(
    quantity = Force / Area,
    name = "pressure",
    symbol = "h"
)
val Stress = DerivedQuantity(
    quantity = Pressure,
    name = "stress",
    symbol = "σ"
)
// I = Ft
val Impulse = DerivedQuantity(
    quantity = Force * Time,
    name = "impulse",
    symbol = "I"
)
// t = FL
val Torque = DerivedQuantity(
    quantity = Force * Length,
    name = "torque",
    symbol = "T"
)
// ρ = M / V
val MassDensity = DerivedQuantity(
    quantity = Momentum / Volume,
    name = "mass density",
    symbol = "ρ"
)
// v = V / m
val SpecificVolume = DerivedQuantity(
    quantity = Volume / Mass,
    name = "specific volume",
    symbol = "v"
)
// E = FL
val Energy = DerivedQuantity(
    quantity = Force * Length,
    name = "energy",
    symbol = "E"
)
val Work = DerivedQuantity(
    quantity = Energy,
    name = "work",
    symbol = "W"
)
val Heat = DerivedQuantity(
    quantity = Energy,
    name = "heat",
    symbol = "Q"
)
// c = N / V
val Molarity = DerivedQuantity(
    quantity = AmountOfSubstance / Volume,
    name = "molarity",
    symbol = "c"
)
// Vm = V / N
val MolarVolume = DerivedQuantity(
    quantity = Volume / AmountOfSubstance,
    name = "molar volume",
    symbol = "Vm"
)
// S = E / T
val Entropy = DerivedQuantity(
    quantity = Energy / Temperature,
    name = "entropy",
    symbol = "S"
)
// Cm = S / N
val MolarEntropy = DerivedQuantity(
    quantity = Entropy / AmountOfSubstance,
    name = "molar entropy",
    symbol = "Cm"
)
val MolarHeatCapacity = DerivedQuantity(
    quantity = MolarEntropy,
    name = "molar heat capacity",
    symbol = "Cm"
)
// c = S / m
val SpecificEntropy = DerivedQuantity(
    quantity = Entropy / Mass,
    name = "specific entropy",
    symbol = "c"
)
val SpecificHeatCapacity = DerivedQuantity(
    quantity = SpecificEntropy,
    name = "specific heat capacity",
    symbol = "c"
)
// Em = E / N
val MolarEnergy = DerivedQuantity(
    quantity = Energy / AmountOfSubstance,
    name = "molar energy",
    symbol = "Em"
)
// h = E / m
val SpecificEnergy = DerivedQuantity(
    quantity = Energy / Mass,
    name = "specific energy",
    symbol = "h"
)
// U = E / V
val EnergyDensity = DerivedQuantity(
    quantity = Energy / Volume,
    name = "energy density",
    symbol = "U"
)
// Cm = E / θ
val HeatCapacity = DerivedQuantity(
    quantity = Energy / Temperature,
    name = "heat capacity",
    symbol = "Cm"
)
// σ = F / L
val SurfaceTension = DerivedQuantity(
    quantity = Force / Length,
    name = "surface tension",
    symbol = "σ"
)
// P = F * v
val Power = DerivedQuantity(
    quantity = Force * Velocity,
    name = "power",
    symbol = "P"
)
// E = P / S
val PowerDensity = DerivedQuantity(
    quantity = Power / Area,
    name = "power density",
    symbol = "E"
)
val Irradiance = DerivedQuantity(
    quantity = PowerDensity,
    name = "irradiance",
    symbol = "E"
)
val HeatFluxDensity = DerivedQuantity(
    quantity = PowerDensity,
    name = "heat flux density",
    symbol = "E"
)
// λ = P / Lθ
val ThermalConductivity = DerivedQuantity(
    quantity = Power / (Length * Temperature),
    name = "thermal conductivity",
    symbol = "λ"
)
// μ = pt
val DynamicViscosity = DerivedQuantity(
    quantity = Pressure * Time,
    name = "dynamic viscosity",
    symbol = "μ"
)
// γ = μ / ρ
val KinematicViscosity = DerivedQuantity(
    quantity = DynamicViscosity / MassDensity,
    name = "kinematic viscosity",
    symbol = "γ"
)
// M = m / N
val MolarMass = DerivedQuantity(
    quantity = Mass / AmountOfSubstance,
    name = "molar mass",
    symbol = "M"
)
// λm = m / L
val LinearDensity = DerivedQuantity(
    quantity = Mass / Length,
    name = "linear density",
    symbol = "λm"
)
// ρA = m / S
val SurfaceDensity = DerivedQuantity(
    quantity = Mass / Area,
    name = "surface density",
    symbol = "ρA"
)
// S = E * t
val Action = DerivedQuantity(
    quantity = Energy * Time,
    name = "action",
    symbol = "S"
)
// Q = S * v
val FlowRate = DerivedQuantity(
    quantity = Area * Velocity,
    name = "flow rate",
    symbol = "Q"
)

// Electromagnetism
// Q = It
val ElectricCharge = DerivedQuantity(
    quantity = Current * Time,
    name = "electric charge",
    symbol = "Q"
)
// ρq = Q / V
val ElectricChargeDensity = DerivedQuantity(
    quantity = ElectricCharge / Volume,
    name = "electric charge density",
    symbol = "ρq"
)
// J = I / A
val ElectricCurrentDensity = DerivedQuantity(
    quantity = Current / Area,
    name = "electric current density",
    symbol = "J"
)
// U = P / I
val ElectricPotential = DerivedQuantity(
    quantity = Power / Current,
    name = "electric potential",
    symbol = "U"
)
val ElectromotiveForce = DerivedQuantity(
    quantity = ElectricPotential,
    name = "electromotive force",
    symbol = "U"
)
val Voltage = DerivedQuantity(
    quantity = ElectricPotential,
    name = "voltage",
    symbol = "U"
)
// R = U / I
val Resistance = DerivedQuantity(
    quantity = Voltage / Current,
    name = "resistance",
    symbol = "R"
)
val Impedance = DerivedQuantity(
    quantity = Resistance,
    name = "impedance",
    symbol = "R"
)
// G = R^-1
val Conductance = DerivedQuantity(
    quantity = -Resistance,
    name = "conductance",
    symbol = "R"
)
// κ = G / L
val Conductivity = DerivedQuantity(
    quantity = Conductance / Length,
    name = "conductivity",
    symbol = "κ"
)
// κm = κ * Vm
val MolarConductivity = DerivedQuantity(
    quantity = Conductivity * MolarVolume,
    name = "molar conductivity",
    symbol = "κm"
)
// F = Q / U
val Capacitance = DerivedQuantity(
    quantity = ElectricCharge / Voltage,
    name = "capacitance",
    symbol = "F"
)
// ε = F / L
val Permittivity = DerivedQuantity(
    quantity = Capacitance / Length,
    name = "permittivity",
    symbol = "ε"
)
// E = U / L
val ElectricFieldIntensity = DerivedQuantity(
    quantity = Voltage / Length,
    name = "electromotive field intensity",
    symbol = "E"
)
val ElectricFieldStrength = DerivedQuantity(
    quantity = ElectricFieldIntensity,
    name = "electric field strength",
    symbol = "E"
)
// L = Ut / I
val Inductance = DerivedQuantity(
    quantity = Voltage / Current * Time,
    name = "inductance",
    symbol = "L"
)
// B = F / LI
val MagneticFieldDensity = DerivedQuantity(
    quantity = Force / (Length * Current),
    name = "magnetic field density",
    symbol = "B"
)
// H = I / L
val MagneticFieldIntensity = DerivedQuantity(
    quantity = Current / Length,
    name = "magnetic field intensity",
    symbol = "H"
)
// φ = BS
val MagneticFlux = DerivedQuantity(
    quantity = MagneticFieldIntensity * Area,
    name = "magnetic flux",
    symbol = "φ"
)
// μ = L / L
val MagneticPermeability = DerivedQuantity(
    quantity = Inductance / Length,
    name = "magnetic permeability",
    symbol = "μ"
)
// λ = μ^-1
val MagneticReluctance = DerivedQuantity(
    quantity = -MagneticPermeability,
    name = "magnetic reluctance",
    symbol = "λ"
)
// λq = Q / L
val ElectricChargeLinearDensity = DerivedQuantity(
    quantity = ElectricCharge / Length,
    name = "electric charge linear density",
    symbol = "λq"
)
// σq = Q / S
val ElectricChargeSurfaceDensity = DerivedQuantity(
    quantity = ElectricCharge / Area,
    name = "electric charge surface density",
    symbol = "σq"
)

// Optics
// φ = l * sr
val LuminousFlux = DerivedQuantity(
    quantity = LuminousIntensity * SolidAngle,
    name = "luminous flux",
    symbol = "φ"
)
// Ev = φ / S
val Illuminance = DerivedQuantity(
    quantity = LuminousFlux / Area,
    name = "illuminance",
    symbol = "Ev"
)
// L = l / S
val Luminance = DerivedQuantity(
    quantity = LuminousIntensity / Area,
    name = "luminance",
    symbol = "L"
)

// Radiology
// A = t^-1
val Activity = DerivedQuantity(
    quantity = -Time,
    name = "activity",
    symbol = "A"
)
// D = E / m
val AbsorbedDose = DerivedQuantity(
    quantity = Energy / Mass,
    name = "absorbed dose",
    symbol = "D"
)
val DoseEquivalent = DerivedQuantity(
    quantity = AbsorbedDose,
    name = "dose equivalent",
    symbol = "D"
)
// d = D / t
val DosingRate = DerivedQuantity(
    quantity = DoseEquivalent / Time,
    name = "dosing rate",
    symbol = "d"
)

// Other
// N / t
val CatalyticActivity = DerivedQuantity(AmountOfSubstance / Time, "catalytic activity")
// B = i / t
val Bandwidth = DerivedQuantity(
    quantity = Information / Time,
    name = "bandwidth",
    symbol = "B"
)
