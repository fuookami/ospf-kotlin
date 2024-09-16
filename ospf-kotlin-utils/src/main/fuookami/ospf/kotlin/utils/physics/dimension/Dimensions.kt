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
// T
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
val WaveNumber = DerivedQuantity(listOf(L * -1), "wave number", "k")
// a = v / t
val Acceleration = DerivedQuantity(Velocity / Time, "acceleration", "a")
// α = ω / t
val AngularAcceleration = DerivedQuantity(AngularVelocity / Time, "angular acceleration", "α")
// P = m * v
val Momentum = DerivedQuantity(Mass * Velocity, "momentum", "M")
// L = m * ω
val AngularMomentum = DerivedQuantity(Mass * AngularVelocity, "angular momentum", "L")
// l = m * L^2
val MomentOfInertia = DerivedQuantity(Mass * (L * 2), "moment of inertia", "I")
// F = m * a
val Force = DerivedQuantity(Mass * Acceleration, "force", "F")
val Gravity = DerivedQuantity(Force, "gravity", "g")
// p = F / S
val Pressure = DerivedQuantity(Force / Area, "pressure", "h")
val Stress = DerivedQuantity(Pressure, "stress", "σ")
// I = F * t
val Impulse = DerivedQuantity(Force * Time, "impulse", "I")
// t = F * L
val Torque = DerivedQuantity(Force * Length, "torque", "T")
// ρ = M / V
val MassDensity = DerivedQuantity(Momentum / Volume, "mass density", "ρ")
// v = V / m
val SpecificVolume = DerivedQuantity(Volume / Mass, "specific volume", "v")
// E = FL
val Energy = DerivedQuantity(Force * Length, "energy", "E")
val Work = DerivedQuantity(Energy, "work", "W")
val Heat = DerivedQuantity(Energy, "heat", "Q")

// L^3 t^-1
val FlowVelocity = DerivedQuantity(listOf(L * 3, T * -1), "flow velocity")

// L^2 m t^-3
val Power = DerivedQuantity(listOf(L * 2, M * 1, T * -3), "power")

// m / L
val LinearDensity = DerivedQuantity(Mass / Length, "linear density")
// m / S
val SurfaceDensity = DerivedQuantity(Mass / Area, "surface density")

// Electromagnetism

// Optics

// Radiology


// Other
