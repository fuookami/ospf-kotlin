package fuookami.ospf.kotlin.utils.physics.dimension

// L^2
val Area = DerivedQuantity(listOf(L * 2), "area")

// L t^-2
val Acceleration = DerivedQuantity(listOf(L * 1, T * -2), "acceleration")

// L^2 m t^-2
val Energy = DerivedQuantity(listOf(L * 1, M * 2, T * -2), "energy")

// L m t^-2
val Force = DerivedQuantity(listOf(L * 1, M * 1, T * -2), "force")

// L^3 t^-1
val FlowVelocity = DerivedQuantity(listOf(L * 3, T * -1), "flow velocity")

// L
val Length = DerivedQuantity(L, "length")

// m
val Mass = DerivedQuantity(M, "mass")

// L^-3 m
val MassDensity = DerivedQuantity(listOf(L * -3, M * 1), "mass density")

// L^2 m t^-3
val Power = DerivedQuantity(listOf(L * 2, M * 1, T * -3), "power")

// L^-2 m
val SurfaceDensity = DerivedQuantity(listOf(L * -2, M * 1), "surface density")

// t
val Time = DerivedQuantity(T, "time")

// L^1 t^-1
val Velocity = DerivedQuantity(listOf(L * 1, T * -1), "velocity")

// L^3
val Volume = DerivedQuantity(listOf(L * 3), "volume")
