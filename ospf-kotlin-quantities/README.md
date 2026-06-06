# ospf-kotlin-quantities

:us: English | :cn: [简体中文](README_ch.md)

A comprehensive physical quantities and units library for OSPF Kotlin. Provides strongly-typed physical quantities, unit conversions, and dimensional analysis with a focus on type safety, correctness, and ease of use.

## Overview

`ospf-kotlin-quantities` is designed to provide a robust foundation for scientific computing, engineering applications, and any domain requiring physical quantity calculations. Key design principles include:

- **Type Safety**: Strong typing with dimensional analysis prevents invalid operations
- **Correctness**: Accurate unit conversions based on authoritative sources (NIST, BIPM)
- **Extensibility**: Easy to add new units and quantities
- **Performance**: Optimized for both compile-time and runtime efficiency

## Module Structure

| Package | Description | Key Types |
|---------|-------------|-----------|
| `dimension` | Physical dimensions and quantities | `FundamentalQuantity`, `DerivedQuantity`, `Dimensions`, `QuantityDomain` |
| `quantity` | Quantity types and operations | `Quantity<V>`, `DurationExtensions`, `MinMax`, `ValueRange` |
| `math/symbol` | Quantity-aware symbol helpers | `SymbolQuantity`, `DimensionedSymbol`, `SymbolDimensionRegistry`, `SymbolQuantityOps` |
| `math/geometry` | Geometry primitives with quantity support | `Axis2`, `Axis3`, `Box2`, `Box3`, `Cuboid3`, `Cylinder3`, `Placement3`, `Shape3` |
| `unit` | Physical units definitions | `PhysicalUnit`, `UnitSystem`, 300+ predefined units |

## Architecture Design

### Dimension Hierarchy

```
FundamentalQuantity (SI base quantities + supplementary)
├── Length, Mass, Time, Current
├── Temperature, SubstanceAmount, LuminousIntensity
├── PlaneAngle, SolidAngle
└── Information

DerivedQuantity (derived from fundamentals)
├── Area, Volume, Velocity, Acceleration
├── Force, Energy, Power, Pressure
├── Frequency, Momentum, Torque
├── Voltage, Resistance, ElectricCharge, Capacitance
├── Bandwidth, FlowRate, MassDensity, SurfaceDensity
└── Many more (50+ predefined in Dimensions.kt)...
```

### Unit System

```
PhysicalUnit (abstract class)
├── Base units (Meter, Kilogram, Second, ...)
├── Derived units (Newton, Joule, Watt, ...)
├── SI prefixed units (Kilometer, Megahertz, ...)
└── Non-SI units (Foot, Pound, Horsepower, ...)

UnitSystem (interface)
├── SI  - International System of Units (7 base + 3 supplementary)
├── MKS - Meter-Kilogram-Second (mechanical subset)
└── CGS - Centimeter-Gram-Second (mechanical subset)
```

## Core Features

### Quantity Creation and Operations

```kotlin
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.math.algebra.number.*

// Create quantities with units
val length = Flt64(5.0) * Meter
val time = Flt64(2.0) * Second
val velocity = length / time  // 2.5 m/s

// Unit conversion
val distanceInFeet = length.to(Foot)  // Converts to feet
val timeInMinutes = time.to(Minute)   // Converts to minutes

// Arithmetic operations
val totalLength = length + Flt64(3.0) * Meter  // 8.0 m
val doubledLength = length * Flt64(2.0)        // 10.0 m
```

### Dimensional Safety

```kotlin
// Compile-time and runtime dimensional checking
val mass = Flt64(10.0) * Kilogram
val length = Flt64(5.0) * Meter

// This compiles and works (Force = Mass × Acceleration)
val acceleration = Flt64(9.81) * MeterPerSecondSquared
val force = mass * acceleration  // 98.1 N

// This would fail at runtime (dimension mismatch)
// val invalid = mass + length  // Cannot add mass to length
```

### Unit Conversion

```kotlin
// Length conversions
val meters = Flt64(1000.0) * Meter
val kilometers = meters.to(Kilometer)  // 1.0 km
val feet = meters.to(Foot)             // 3280.84 ft
val inches = meters.to(Inch)           // 39370.1 in

// Pressure conversions
val pascals = Flt64(101325.0) * Pascal
val atmospheres = pascals.to(Atmosphere)  // 1.0 atm
val bars = pascals.to(Bar)               // 1.01325 bar

// Integer quantity conversion (returns null for non-integer factors)
val intMeters = Int64(1000) * Meter
val intKilometers = intMeters.to(Kilometer)  // 1 km (Ok)
val intFeet = intMeters.to(Foot)             // null (non-integer factor)
```

### Duration Interoperability

```kotlin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Convert between Quantity and kotlin.time.Duration
val time = Flt64(5.0) * Second
val result = time.toDuration()
when (result) {
    is Ok -> {
        val duration: Duration = result.value  // 5 seconds
        val quantity = duration.toQuantity<Flt64>(Minute)
        // 0.0833... minutes
    }
    is Failed -> { /* handle error */ }
    is Fatal -> { /* handle fatal error */ }
}

// Convenience methods
val duration = 90.seconds
val hours = duration.toQuantityHoursFlt64()     // Ok(0.025 h)
val bestFit = duration.toQuantityBestFit<Flt64>()  // Auto-selects best unit
```

### Symbol Quantity Support

```kotlin
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*

// Quantities with symbolic values
val x = Symbol("x")
val quantity: Quantity<LinearPolynomial<Flt64>> =
    (LinearMonomial(Flt64(2.0), x) + Flt64(3.0)) * Meter

// Evaluate at specific value
val result = quantity.evaluate(mapOf(x to Flt64(5.0)))
// (2*5 + 3) m = 13 m
```

### Supported Quantities and Units

#### Base Quantities (10)
| Quantity | SI Unit | Common Units |
|----------|---------|--------------|
| Length | Meter (m) | Kilometer, Foot, Inch, Mile, NauticalMile, AU, LightYear, Parsec |
| Mass | Kilogram (kg) | Gram, Ton, Pound, Ounce |
| Time | Second (s) | Minute, Hour, Day, Year, Millisecond, Nanosecond |
| Electric Current | Ampere (A) | Milliampere, Kiloampere |
| Temperature | Kelvin (K) | Celsius, Fahrenheit |
| Amount of Substance | Mole (mol) | Millimole, Kilomole |
| Luminous Intensity | Candela (cd) | Millicandela |
| Plane Angle | Radian (rad) | Degree, ArcMinute, ArcSecond |
| Solid Angle | Steradian (sr) | - |
| Information | Bit (bit) | Byte, Kilobit, Megabit, Gigabit |

#### Derived Quantities (50+)
| Quantity | SI Unit | Examples |
|----------|---------|----------|
| Area | m² | Hectare, Acre, SquareFoot |
| Volume | m³ | Liter, Gallon, CubicYard |
| Velocity | m/s | Km/h, Mph, Knot |
| Acceleration | m/s² | Gravity, Ft/s² |
| Force | Newton (N) | Dyne, PoundForce |
| Energy | Joule (J) | Calorie, BTU, ElectronVolt |
| Power | Watt (W) | Horsepower, Megawatt |
| Pressure | Pascal (Pa) | Bar, Atmosphere, PSI |
| Frequency | Hertz (Hz) | Kilohertz, Megahertz |
| Momentum | N·s | kg·m/s |
| Torque | N·m | ft·lbf |
| Angular Velocity | rad/s | deg/s |
| Angular Acceleration | rad/s² | deg/s² |
| Catalytic Activity | Katal (kat) | Enzyme Unit |
| Voltage | Volt (V) | Millivolt, Kilovolt |
| Resistance | Ohm | Milliohm, Kiloohm |
| Electric Charge | Coulomb (C) | Ampere-hour |
| Capacitance | Farad (F) | Microfarad, Picofarad |
| Bandwidth | bit/s | Kbit/s, Mbit/s, Gbit/s |
| Flow Rate | m³/s | Liter/s, Gallon/min |
| Mass Density | kg/m³ | g/cm³ |
| Surface Density | kg/m² | - |
| Stress | Pascal (Pa) | Megapascal, Gigapascal |
| Wavenumber | m⁻¹ | cm⁻¹ |

### Error Handling

All conversion operations return `Ret<T>` for safe error handling:

```kotlin
import fuookami.ospf.kotlin.utils.functional.*

val result = quantity.toDuration()
when (result) {
    is Ok -> {
        val duration = result.value
        // Use duration
    }
    is Failed -> {
        println("Error: ${result.code} - ${result.message}")
    }
    is Fatal -> {
        println("Fatal error")
        result.errors.forEach { println(it.message) }
    }
}
```

## Performance Optimizations

| Feature | Optimization | Notes |
|---------|-------------|-------|
| Unit conversion | Cached conversion factors | Reduces repeated calculations |
| Dimension analysis | Lazy evaluation | Only computed when needed |
| Integer conversion | Early null return | Fast path for non-integer factors |
| Unit system | ConcurrentHashMap | Thread-safe singleton |

## Testing

```powershell
# Run all tests
mvn -pl ospf-kotlin-quantities test

# Run specific test classes
mvn -pl ospf-kotlin-quantities -Dtest=DurationExtensionsTest test

# Run with verbose output
mvn -pl ospf-kotlin-quantities test -Dsurefire.useFile=false
```

Test coverage includes:
- Unit constant correctness (NIST/BIPM values)
- Dimension analysis
- Unit conversion accuracy
- Integer type handling
- Duration interoperability
- Symbol quantity operations
- Thread safety

## Dependencies

| Module | Purpose |
|--------|---------|
| `ospf-kotlin-math` | Mathematical types (Flt64, Int64, Symbol, etc.) |
| `ospf-kotlin-utils` | Error handling (Ret, Error), Functional types |

## Unit Constants Sources

All unit conversion constants are derived from authoritative sources:
- **SI Units**: BIPM (Bureau International des Poids et Mesures)
- **Non-SI Units**: NIST Special Publication 811
- **Astronomical Units**: IAU (International Astronomical Union)

See individual unit files for specific source citations.

## Related Modules

- [ospf-kotlin-math](../ospf-kotlin-math) - Mathematical algebra and symbol system
- [ospf-kotlin-utils](../ospf-kotlin-utils) - Utility functions and error handling

## License

This module is part of the OSPF Kotlin project and is licensed under the Apache License 2.0.
