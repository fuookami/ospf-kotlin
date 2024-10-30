package fuookami.ospf.kotlin.utils.physics.dimension

// Fundamental Quantity
// L
val Length = DerivedQuantity(L, "length", "L")
// m
val Mass = DerivedQuantity(M, "mass", "m")
// t
val Time = DerivedQuantity(T, "time", "t")
// I
val Current = DerivedQuantity(I, "current", "I")
// θ
val Temperature = DerivedQuantity(Theta, "temperature", "T")
// N
val AmountOfSubstance = DerivedQuantity(N, "amount of substance", "N")
// l
val LuminousIntensity = DerivedQuantity(J, "luminous intensity", "l")
// rad
val PlaneAngle = DerivedQuantity(rad, "plane angle", "rad")
// sr
val SolidAngle = DerivedQuantity(sr, "solid angle", "sr")
// i
val Information = DerivedQuantity(B, "information", "i")

// Basic Derived Quantities
// S = L^2
val Area = DerivedQuantity(listOf(L * 2), "area", "S")
// V = L^3
val Volume = DerivedQuantity(listOf(L * 3), "volume", "V")
// f = t^-1
val Frequency = DerivedQuantity(listOf(T * -1), "frequency", "f")

// Mechanics
// v = L / t
val Velocity = DerivedQuantity(Length / Time, "velocity", "v")
// ω = rad / t
val AngularVelocity = DerivedQuantity(PlaneAngle / Time, "angular velocity", "w")
// k = L^-1
val WaveNumber = DerivedQuantity(Length * -1, "wave number", "k")
// a = v / t
val Acceleration = DerivedQuantity(Velocity / Time, "acceleration", "a")
// α = ω / t
val AngularAcceleration = DerivedQuantity(AngularVelocity / Time, "angular acceleration", "α")
// P = mv
val Momentum = DerivedQuantity(Mass * Velocity, "momentum", "M")
// L = mω
val AngularMomentum = DerivedQuantity(Mass * AngularVelocity, "angular momentum", "L")
// l = mL^2
val MomentOfInertia = DerivedQuantity(Mass * (Length * 2), "moment of inertia", "I")
// F = ma
val Force = DerivedQuantity(Mass * Acceleration, "force", "F")
val Gravity = DerivedQuantity(Force, "gravity", "g")
// p = F / S
val Pressure = DerivedQuantity(Force / Area, "pressure", "h")
val Stress = DerivedQuantity(Pressure, "stress", "σ")
// I = Ft
val Impulse = DerivedQuantity(Force * Time, "impulse", "I")
// t = FL
val Torque = DerivedQuantity(Force * Length, "torque", "T")
// ρ = M / V
val MassDensity = DerivedQuantity(Momentum / Volume, "mass density", "ρ")
// v = V / m
val SpecificVolume = DerivedQuantity(Volume / Mass, "specific volume", "v")
// E = FL
val Energy = DerivedQuantity(Force * Length, "energy", "E")
val Work = DerivedQuantity(Energy, "work", "W")
val Heat = DerivedQuantity(Energy, "heat", "Q")
// c = N / V
val Molarity = DerivedQuantity(AmountOfSubstance / Volume, "molarity", "c")
// Vm = V / N
val MolarVolume = DerivedQuantity(Volume / AmountOfSubstance, "molar volume", "Vm")
// S = E / T
val Entropy = DerivedQuantity(Energy / Temperature, "entropy", "S")
// Cm = S / N
val MolarEntropy = DerivedQuantity(Entropy / AmountOfSubstance, "molar entropy", "Cm")
val MolarHeatCapacity = DerivedQuantity(MolarEntropy, "molar heat capacity", "Cm")
// c = S / m
val SpecificEntropy = DerivedQuantity(Entropy / Mass, "specific entropy", "c")
val SpecificHeatCapacity = DerivedQuantity(SpecificEntropy, "specific heat capacity", "c")
// Em = E / N
val MolarEnergy = DerivedQuantity(Energy / AmountOfSubstance, "molar energy", "Em")
// h = E / m
val SpecificEnergy = DerivedQuantity(Energy / Mass, "specific energy", "h")
// U = E / V
val EnergyDensity = DerivedQuantity(Energy / Volume, "energy density", "U")
// Cm = E / θ
val HeatCapacity = DerivedQuantity(Energy / Temperature, "heat capacity", "Cm")
// σ = F / L
val SurfaceTension = DerivedQuantity(Force / Length, "surface tension", "σ")
// P = F * v
val Power = DerivedQuantity(Force * Velocity, "power", "P")
// E = P / S
val PowerDensity = DerivedQuantity(Power / Area, "power density", "E")
val Irradiance = DerivedQuantity(PowerDensity, "irradiance", "E")
val HeatFluxDensity = DerivedQuantity(PowerDensity, "heat flux density", "E")
// λ = P / Lθ
val ThermalConductivity = DerivedQuantity(Power / (Length * Temperature), "thermal conductivity", "λ")
// μ = pt
val DynamicViscosity = DerivedQuantity(Pressure * Time, "dynamic viscosity", "μ")
// γ = μ / ρ
val KinematicViscosity = DerivedQuantity(DynamicViscosity / MassDensity, "kinematic viscosity", "γ")
// M = m / N
val MolarMass = DerivedQuantity(Mass / AmountOfSubstance, "molar mass", "M")
// λm = m / L
val LinearDensity = DerivedQuantity(Mass / Length, "linear density", "λm")
// ρA = m / S
val SurfaceDensity = DerivedQuantity(Mass / Area, "surface density", "ρA")
// S = E * t
val Action = DerivedQuantity(Energy * Time, "action", "S")
// Q = S * v
val FlowRate = DerivedQuantity(Area * Velocity, "flow rate", "Q")

// Electromagnetism
// Q = It
val ElectricCharge = DerivedQuantity(Current * Time, "electric charge", "Q")
// ρq = Q / V
val ElectricChargeDensity = DerivedQuantity(ElectricCharge / Volume, "electric charge density", "ρq")
// J = I / A
val ElectricCurrentDensity = DerivedQuantity(Current / Area, "electric current density", "J")
// U = P / I
val ElectricPotential = DerivedQuantity(Power / Current, "electric potential", "U")
val ElectromotiveForce = DerivedQuantity(ElectricPotential, "electromotive force", "U")
val Voltage = DerivedQuantity(ElectricPotential, "voltage", "U")
// R = U / I
val Resistance = DerivedQuantity(Voltage / Current, "resistance", "R")
val Impedance = DerivedQuantity(Resistance, "impedance", "R")
// G = R^-1
val Conductance = DerivedQuantity(-Resistance, "conductance", "R")
// κ = G / L
val Conductivity = DerivedQuantity(Conductance / Length, "conductivity", "κ")
// κm = κ * Vm
val MolarConductivity = DerivedQuantity(Conductivity * MolarVolume, "molar conductivity", "κm")
// F = Q / U
val Capacitance = DerivedQuantity(ElectricCharge / Voltage, "capacitance", "F")
// ε = F / L
val Permittivity = DerivedQuantity(Capacitance / Length, "permittivity", "ε")
// E = U / L
val ElectricFieldIntensity = DerivedQuantity(Voltage / Length, "electromotive field intensity", "E")
val ElectricFieldStrength = DerivedQuantity(ElectricFieldIntensity, "electric field strength", "E")
// L = Ut / I
val Inductance = DerivedQuantity(Voltage / Current * Time, "inductance", "L")
// B = F / LI
val MagneticFieldDensity = DerivedQuantity(Force / (Length * Current), "magnetic field density", "B")
// H = I / L
val MagneticFieldIntensity = DerivedQuantity(Current / Length, "magnetic field intensity", "H")
// φ = BS
val MagneticFlux = DerivedQuantity(MagneticFieldIntensity * Area, "magnetic flux", "φ")
// μ = L / L
val MagneticPermeability = DerivedQuantity(Inductance / Length, "magnetic permeability", "μ")
// λ = μ^-1
val MagneticReluctance = DerivedQuantity(-MagneticPermeability, "magnetic reluctance", "λ")
// λq = Q / L
val ElectricChargeLinearDensity = DerivedQuantity(ElectricCharge / Length, "electric charge linear density", "λq")
// σq = Q / S
val ElectricChargeSurfaceDensity = DerivedQuantity(ElectricCharge / Area, "electric charge surface density", "σq")

// Optics
// φ = l * sr
val LuminousFlux = DerivedQuantity(LuminousIntensity * SolidAngle,"luminous flux", "φ")
// Ev = φ / S
val Illuminance = DerivedQuantity(LuminousFlux / Area, "illuminance", "Ev")
// L = l / S
val Luminance = DerivedQuantity(LuminousIntensity / Area, "luminance", "L")

// Radiology
// A = t^-1
val Activity = DerivedQuantity(-Time, "activity", "A")
// D = E / m
val AbsorbedDose = DerivedQuantity(Energy / Mass, "absorbed dose", "D")
val DoseEquivalent = DerivedQuantity(AbsorbedDose, "dose equivalent", "D")
// d = D / t
val DosingRate = DerivedQuantity(DoseEquivalent / Time, "dosing rate", "d")

// Other
// N / t
val CatalyticActivity = DerivedQuantity(AmountOfSubstance / Time, "catalytic activity")
// B = i / t
val Bandwidth = DerivedQuantity(Information / Time, "bandwidth", "B")
