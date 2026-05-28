# ospf-kotlin-math

[Chinese Documentation (README_ch.md)](./README_ch.md)

A comprehensive mathematical algebra, symbol, and geometry system for OSPF Kotlin. Provides foundational mathematical types, algebraic structures, symbolic expressions, geometric primitives, and numerical operations with a focus on type safety, precision, and performance.

## Overview

`ospf-kotlin-math` is designed to provide a robust mathematical foundation for scientific computing, optimization problems, and symbolic computation. Key design principles include:

- **Type Safety**: Strong typing with algebraic structure constraints
- **Precision Control**: Support for arbitrary precision arithmetic (IntX, FltX)
- **Immutability**: All value types are immutable by design
- **Extensibility**: Interfaces allow custom implementations

## Module Structure

| Package | Description | Key Types |
|---------|-------------|-----------|
| `algebra.concept` | Algebraic structure interfaces | `Group`, `Ring`, `Field`, `Monoid`, `Semigroup` |
| `algebra.law` | Algebraic law validation | `Associativity`, `Commutativity`, `Distributivity` |
| `algebra.number` | Number type implementations | `Int8`-`IntX`, `UInt8`-`UIntX`, `Flt32`, `Flt64`, `FltX`, `Rtn8`-`RtnX` |
| `algebra.value_range` | Typed value ranges | `ValueRange`, `TypedValueRange`, `Bound`, `Interval` |
| `chaotic_operator` | Chaotic system attractors | `Lorenz`, `Chen`, `Rossler`, `LogisticMap`, 30+ implementations |
| `combinatorics` | Combinatorial algorithms | `permutations`, `combinations`, `cross` |
| `fractal_operator` | Fractal generation | `MandelbrotSet` |
| `functional` | Collection extensions | `usize`, `uIndices` |
| `geometry` | Dimension-generic geometric primitives | `Point`, `Vector`, `Edge`, `Triangle`, `Circle`, `Rectangle`, `Quadrilateral`, `Box2`/`Box3`, `Cuboid3`, `Cylinder3`, `Placement2`/`Placement3` |
| `multiarray` | Multi-dimensional arrays | `MultiArray`, `Einsum`, `TensorExpr` |
| `operator` | Mathematical operators | `Plus`, `Minus`, `Times`, `Div`, `Pow`, `Trigonometry` |
| `ordinary` | Common math operations | `gcd`, `lcm`, `Prime`, `Factorization` |
| `parallel` | Parallel computation | `parallelFold`, `chunked` |
| `symbol` | Symbolic expression system | `Symbol`, `LinearPolynomial`, `CanonicalPolynomial`, `Inequality` |
| `symbol.expression` | Runtime expression AST | `ScalarExpression`, `BooleanExpression`, `PropertyPath` |
| `symbol.parse` | Direct polynomial/inequality parser | `parseLinear`, `parseQuadratic`, `parseCanonical`, `ParseResult` |
| `symbol.serde` | Polynomial/inequality JSON serde and symbol identity | `linearPolynomialFromJson`, `SymbolIdentityExpr` |

## Expression Entry Points

- `symbol.expression.*` is the preferred stack for runtime boolean/scalar expressions and property-path evaluation.
- `symbol.parse` provides direct polynomial and inequality parsing (linear, quadratic, canonical).
- `symbol.serde` provides JSON serialization/deserialization for polynomials and inequalities.

## Architecture Design

### Algebraic Hierarchy

```
Semigroup (associative +)
    └── Monoid (identity)
        └── Group (inverse)
            └── AbelianGroup (commutative)

MultiplicativeSemigroup (associative *)
    └── MultiplicativeMonoid (identity)
        └── MultiplicativeGroup (inverse)

Ring = AbelianGroup (+) + MultiplicativeSemigroup (*)
    └── CommutativeRing (commutative *)
        └── Field (multiplicative inverse)
```

### Number Type Hierarchy

```
Integer (signed integers)
├── Int8, Int16, Int32, Int64 (fixed precision)
├── IntX (arbitrary precision)
└── NumericInteger (Int8..Int64 with numeric operations)

UInteger (unsigned integers)
├── UInt8, UInt16, UInt32, UInt64 (fixed precision)
├── UIntX (arbitrary precision)
└── NumericUInteger (UInt8..UInt64 with numeric operations)

Floating (floating-point)
├── Flt32, Flt64 (IEEE 754)
└── FltX (arbitrary precision via BigDecimal)

Rational (rational numbers)
├── Rtn8..RtnX (signed rational)
└── URtn8..URtnX (unsigned rational)
```

### Geometry Type Hierarchy

All geometric types are dimension-generic, parameterized by `<D : Dimension, V : FloatingNumber<V>>`:

```
Dimension
├── Dim1 (1D)
├── Dim2 (2D)
└── Dim3 (3D)

Point<D, V>       -- position in D-dimensional space
Vector<D, V>      -- direction and magnitude in D-dimensional space
Edge<P, D, V>     -- line segment connecting two points
Triangle<P, D, V> -- three vertices
Quadrilateral<P, D, V> -- four vertices
Rectangle<P, D, V> -- four right-angle vertices (axis-aligned factory available)
Circle<P, Vec, D, V>  -- center + direction + radius (general circle/sphere)

Projection2<V> (sealed interface, aka Shape2<V>)
├── Rectangle2<V> -- width + height
└── Circle2<V>    -- radius

Shape3<V> (interface)
├── Cuboid3<V>    -- width + height + depth
└── Cylinder3<V>  -- radius + height + axis

Box2<V>           -- 2D bounding box (position + Shape2)
Box3<V>           -- 3D bounding box (position + Cuboid3)
Placement2<V>     -- 2D placement (position + Projection2)
Placement3<V>     -- 3D placement (position + Shape3)

Axis2, Axis3      -- axis enums (X, Y / X, Y, Z)
AxisPermutation2/3 -- axis permutation
AxisPlane3        -- principal plane enum (XY, XZ, YZ)
PlaneFrame3       -- coordinate frame for plane projection
```

## Core Features

### Algebraic Structures

All number types implement appropriate algebraic structures:

```kotlin
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*

// Flt64 implements Field (supports +, -, *, /)
val a = Flt64(3.14)
val b = Flt64(2.0)

val sum = a + b      // Addition via Group
val product = a * b  // Multiplication via Ring
val quotient = a / b // Division via Field

// Type-safe operations
val c: Flt64 = Flt64.one + Flt64.two  // Compile-time type checking
```

### Number Types

```kotlin
import fuookami.ospf.kotlin.math.algebra.number.*

// Fixed precision integers
val i8 = Int8(127)
val i64 = Int64(9223372036854775807L)

// Arbitrary precision integers
val bigInt = IntX("123456789012345678901234567890")

// Floating-point numbers
val f32 = Flt32(3.14159f)
val f64 = Flt64(3.141592653589793)

// Arbitrary precision floating-point
val bigFloat = FltX("3.141592653589793238462643383279")

// Rational numbers
val rational = RtnX(IntX(1), IntX(3))  // 1/3

// Type conversions
val toFlt64: Flt64 = i64.toFlt64()
val toIntX: IntX = f64.toIntX()
```

### Value Ranges

Typed value ranges with compile-time and runtime validation:

```kotlin
import fuookami.ospf.kotlin.math.algebra.value_range.*

// Create value range
val range = ValueRange(
    lower = Bound(Flt64(0.0), Interval.Closed),
    upper = Bound(Flt64(100.0), Interval.Closed)
)

// Typed value range for compile-time safety
val percentage: ClosedTypedValueRange<Flt64> = TypedValueRange.closed(
    Flt64(0.0),
    Flt64(100.0)
)

// Clamp values
val clamped = range.clamp(Flt64(150.0))  // Returns 100.0

// Arithmetic operations preserve ranges
val doubled = percentage * Flt64(2.0)  // Range: [0, 200]
```

### Symbolic Expressions

Comprehensive symbolic expression system supporting polynomials, inequalities, and operations:

```kotlin
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*

val x = symbolOf("x")
val y = symbolOf("y")

// Linear polynomial: 1 + 2x + 3y
val linear = LinearPolynomial(
    monomials = listOf(
        LinearMonomial(Flt64(2.0), x),
        LinearMonomial(Flt64(3.0), y)
    ),
    constant = Flt64(1.0)
)

// Quadratic polynomial: x^2 + 2xy + y^2
val quadratic = QuadraticPolynomial(
    monomials = listOf(
        QuadraticMonomial(Flt64(1.0), x, x),
        QuadraticMonomial(Flt64(2.0), x, y),
        QuadraticMonomial(Flt64(1.0), y, y)
    ),
    constant = Flt64.zero
)

// Canonical polynomial (general form)
val canonical = CanonicalPolynomial(
    monomials = listOf(
        CanonicalMonomial(Flt64(1.0), mapOf(x to 2, y to 1))
    ),
    constant = Flt64.zero
)

// Evaluate at specific values
val result = linear.evaluate(mapOf(x to Flt64(2.0), y to Flt64(3.0)))
// Result: 1 + 2*2 + 3*3 = 14

// Differentiation
val derivative = linear.differentiate(x)  // d/dx(1 + 2x + 3y) = 2

// Compile to optimized form
val compiled = linear.compile()

// Convert to LaTeX
val latex = linear.toLatex()  // "1 + 2x + 3y"

// Factorization
val factored = factorize(linear)  // Attempts algebraic factorization

// Interval extremum evaluation
val extremum = linear.evaluateIntervalExtremum(
    mapOf(x to closedRange(0.0, 10.0), y to closedRange(-5.0, 5.0))
)
// Returns the min/max bounds of the polynomial over the given intervals
```

### Inequality System

```kotlin
import fuookami.ospf.kotlin.math.symbol.inequality.*

// Linear inequality: ax + by + c <= 0
val inequality = LinearInequality(
    polynomial = linearPolynomial,
    constraint = Constraint.LessEqual
)

// Check feasibility
val feasible = inequality.isFeasible()

// Combine inequalities
val system = listOf(inequality1, inequality2, inequality3)
val result = solveInequalitySystem(system)
```

### Satisfiability Checking

Check whether a given assignment of values satisfies an inequality:

```kotlin
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

val x = symbolOf("x")
val y = symbolOf("y")

// Create a linear inequality: 2x + y <= 5
val inequality = Flt64LinearInequality(
    lhs = LinearPolynomial(
        monomials = listOf(
            LinearMonomial(Flt64(2.0), x),
            LinearMonomial(Flt64.one, y)
        ),
        constant = Flt64.zero
    ),
    rhs = LinearPolynomial(constant = Flt64(5.0)),
    comparison = Comparison.LE
)

// Check satisfiability with a map-based assignment
val satisfied = inequality.isSatisfied(mapOf(x to Flt64.one, y to Flt64(2.0)))
// true: 2*1 + 2 = 4 <= 5

// Check with ordered assignment (avoids Map overhead)
val order = listOf(x, y)
val satisfiedOrdered = inequality.isSatisfiedOrdered(order, listOf(Flt64.one, Flt64(2.0)))
// true

// Works for QuadraticInequality and CanonicalInequality too
```

### Non-Throwing Parse Functions

Parse expressions and inequalities without catching exceptions, using `Ret<T>` for structured error handling:

```kotlin
import fuookami.ospf.kotlin.math.symbol.parser.*
import fuookami.ospf.kotlin.utils.functional.*

// Parse with structured error handling
val result = parseLinear("2*x + 3*y")
when (result) {
    is Ok -> println("Parsed: ${result.value}")
    is Failed -> println("Error: ${result.error}")
    is Fatal -> println("Fatal: ${result.errors}")
}

// Parse with error classification
val linearResult = parseLinearInequality("2*x + 3*y - 5 <= 0")
if (linearResult is Failed) {
    val issue = linearResult.error.context as? ParseIssue
    println("Issue type: ${issue?.type}") // Lexical, Syntax, Conversion, Semantic, Unknown
    println("Position: ${issue?.position}")
}

// Parse specific polynomial types
val canonicalResult = parseCanonical("x^2 + 2*x*y + y^2")
val quadraticResult = parseQuadratic("x^2 + 2*x*y + y^2")
val inequalityResult = parseLinearInequality("2*x + y <= 5")
```

### Expression DSL

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.dsl.*

// Build boolean expressions with DSL
val expr = path("a").gt(5) and path("b").isNotNull()
```

### Geometric Primitives

All geometric types are dimension-generic. Use factory functions `point2`/`point3` and `vector2`/`vector3` to create Flt64-typed points and vectors:

```kotlin
import fuookami.ospf.kotlin.math.geometry.*

// Points and vectors (dimension-generic, Flt64 convenience factories)
val p1 = point2(Flt64(0.0), Flt64(0.0))    // Point<Dim2, Flt64>
val p2 = point2(Flt64(3.0), Flt64(4.0))
val p3 = point2(Flt64(6.0), Flt64(0.0))
val v = vector2(Flt64(3.0), Flt64(4.0))     // Vector<Dim2, Flt64>

// Access coordinates via extension properties
val x: Flt64 = p1.x
val y: Flt64 = p1.y

// 3D points and vectors
val q1 = point3(Flt64(1.0), Flt64(2.0), Flt64(3.0))  // Point<Dim3, Flt64>
val w = vector3(Flt64(0.0), Flt64(0.0), Flt64(1.0))   // Vector<Dim3, Flt64>
val z: Flt64 = q1.z

// Distance calculations
val dist: Flt64 = p1 distance p2  // 5.0 (Euclidean by default)

// Edges
val edge = Edge(p1, p2)
val length: Flt64 = edge.length
val mid: Point<Dim2, Flt64> = edge.midpoint()

// Triangles
val triangle = Triangle(p1, p2, p3)
val area: Flt64 = triangle.area        // Heron's formula
val perimeter: Flt64 = triangle.perimeter
val centroid: Point<Dim2, Flt64> = triangle.centroid

// 2D triangle-specific operations
val area2d: Flt64 = triangle.area2D()           // Cross-product method
val cc: Circle<...> = triangle.circumcircle()
val incenterPt: Point<Dim2, Flt64> = triangle.incenter()
val containsPt: Boolean = triangle containsPoint p1

// Circles (dimension-generic: center + direction + radius)
val circle = Circle(center = p1, radiusVec = vector2(Flt64(5.0), Flt64.zero))
val circleArea: Flt64 = circle.area
val circleCirc: Flt64 = circle.circumference
val inside: Boolean = circle containsPoint p2

// Rectangles (four vertices, not necessarily axis-aligned)
val rect = Rectangle(p1, p2, p3, p4)  // general rectangle from 4 vertices
val rectArea: Flt64 = rect.area
val rectContains: Boolean = rect.contains(point2(Flt64(1.0), Flt64(1.0)))

// Axis-aligned rectangle from corners
val aaRect = Rectangle(point2(Flt64(0.0), Flt64(5.0)), point2(Flt64(10.0), Flt64(0.0)))

// Quadrilaterals
val quad = Quadrilateral(p1, p2, p3, p4)
val quadArea: Flt64 = quad.area          // Shoelace formula (2D)
val isConvex: Boolean = quad.isConvex()
val quadPerimeter: Flt64 = quad.perimeter

// Triangulation (Delaunay)
val points = listOf(p1, p2, p3, p4, p5)
val triangles: List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>> = triangulate(points)
val result: DelaunayTriangulation2 = delaunayTriangulate(points)  // full result with edges
```

### 3D Shapes

```kotlin
import fuookami.ospf.kotlin.math.geometry.*

// Cuboid (width x height x depth)
val cuboid = Cuboid3(width = Flt64(10.0), height = Flt64(5.0), depth = Flt64(3.0))
val volume: Flt64 = cuboid.volume
val box: Box3<Flt64> = cuboid.atOrigin()

// Cylinder (radius, height, axis)
val cylinder = Cylinder3(radius = Flt64(2.5), height = Flt64(10.0), axis = Axis3.Z)
val cylVolume: Flt64 = cylinder.volume(Flt64.pi)
val cylBase: Flt64 = cylinder.baseArea(Flt64.pi)
val boundingCuboid: Cuboid3<Flt64> = cylinder.boundingCuboid

// Bounding boxes
val box2 = Box2(x = Flt64.zero, y = Flt64.zero, shape = Rectangle2(Flt64(10.0), Flt64(5.0)))
val box3 = Box3(x = Flt64.zero, y = Flt64.zero, z = Flt64.zero, cuboid = cuboid)
val overlaps: Boolean = box3.overlapped(otherBox)
val intersection: Box3<Flt64>? = box3.intersect(otherBox)
val inside: Boolean = box3.contains(Flt64(1.0), Flt64(1.0), Flt64(1.0))

// Placements (shape + position)
val placement2 = Placement2(x = Flt64(1.0), y = Flt64(2.0), shape = Rectangle2(Flt64(5.0), Flt64(3.0)))
val placement3 = Placement3(x = Flt64(1.0), y = Flt64(2.0), z = Flt64(0.0), shape = cuboid)
val pOverlaps: Boolean = placement3.overlapped(otherPlacement)

// Axis permutations (rotate/flip shapes)
val perm = AxisPermutation3.YXZ
val permutedCuboid: Cuboid3<Flt64> = perm.apply(cuboid)
val permutedCylinder: Cylinder3<Flt64> = perm.apply(cylinder)

// Plane frames and projections
val frame = PlaneFrame3.XY
val footprint: Rectangle2<Flt64> = frame.footprint(cuboid)
val dist: Flt64 = frame.distance(PlanePoint3(Flt64(1.0), Flt64(2.0), Flt64(5.0)))

// Cylinder projection on a plane
val proj: Projection2<Flt64> = cylinder.projectionOn(AxisPlane3.XY)
```

### Projection Shapes (Shape2)

```kotlin
import fuookami.ospf.kotlin.math.geometry.*

// 2D projection shapes (sealed interface Projection2<V> aka Shape2<V>)
val rect2 = Rectangle2(width = Flt64(10.0), height = Flt64(5.0))
val circ2 = Circle2(radius = Flt64(2.5))

val rectArea: Flt64 = rect2.area
val rectAlongX: Flt64 = rect2.along(Axis2.X)
val permuted: Rectangle2<Flt64> = rect2.permute(AxisPermutation2.YX)

val circArea: Flt64 = circ2.area(Flt64.pi)
val diameter: Flt64 = circ2.diameter
```

### Distance Metrics

```kotlin
import fuookami.ospf.kotlin.math.geometry.*

// Distance metric strategies (sealed interface)
val p1 = point2(Flt64(0.0), Flt64(0.0))
val p2 = point2(Flt64(3.0), Flt64(4.0))

// Default: Euclidean (L2)
val dist: Flt64 = p1 distance p2  // 5.0

// Explicit metric selection
val euclidean: Flt64 = Distance.Euclidean(p1, p2)    // 5.0
val manhattan: Flt64 = Distance.Manhattan(p1, p2)     // 7.0
val minkowski: Flt64 = Distance.Minkowski(p = 3)(p1, p2)
val chebyshev: Flt64 = Distance.Chebyshev(p1, p2)    // 4.0

// Custom metric
val d: Flt64 = p1.distanceBetween(p2, Distance.Manhattan)
```

### Einstein Summation

Powerful tensor operations using Einstein notation:

```kotlin
import fuookami.ospf.kotlin.math.multiarray.einsum.*

// Create arrays
val a = MultiArray.newWith(Shape2(2, 3), 1.0)
val b = MultiArray.newWith(Shape2(3, 4), 2.0)

// Matrix multiplication
val c1 = matmul(a, b, 0.0)

// Using string notation
val c2 = einsumDouble(a, b, "ij,jk->ik")

// Tensor contraction
val tensor = MultiArray.newWith(Shape3(2, 3, 4), 1.0)
val contracted = einsumDouble(tensor, "ijk->ij")  // Sum over k

// Outer product
val v1 = MultiArray.newWith(Shape1(3), 1.0)
val v2 = MultiArray.newWith(Shape1(4), 1.0)
val outer = einsumDouble(v1, v2, "i,j->ij")

// Batch matrix multiplication
val batchA = MultiArray.newWith(Shape3(10, 2, 3), 1.0)
val batchB = MultiArray.newWith(Shape3(10, 3, 4), 1.0)
val batchC = einsumDouble(batchA, batchB, "bij,bjk->bik")
```

### Chaos and Fractals

Support for chaotic systems and fractal generation:

```kotlin
import fuookami.ospf.kotlin.math.chaotic.*

// Lorenz attractor
val lorenz = Lorenz(
    sigma = Flt64(10.0),
    rho = Flt64(28.0),
    beta = Flt64(8.0 / 3.0)
)

// Generate trajectory
val initial = vector3(Flt64(1.0), Flt64(1.0), Flt64(1.0))
val trajectory = lorenz.iterate(initial, steps = 10000, dt = Flt64(0.01))

// Chen attractor
val chen = Chen(a = Flt64(35.0), b = Flt64(3.0), c = Flt64(28.0))

// Logistic map
val logistic = LogisticMap(r = Flt64(3.9))
val series = logistic.iterate(Flt64(0.5), steps = 1000)

// Available chaotic systems:
// - Lorenz, Chen, Rossler, Lu, Liu
// - Arneodo, Halvorsen, Thomas
// - Aizawa, Anishchenko, Rabinovich
// - And many more (30+ implementations)
```

### Fractal Generation

```kotlin
import fuookami.ospf.kotlin.math.fractal.*

// Mandelbrot set
val mandelbrot = MandelbrotSet(
    maxIterations = 1000,
    escapeRadius = Flt64(2.0)
)

// Check if point is in set
val inSet = mandelbrot.contains(Flt64(-0.5), Flt64(0.0))

// Get iteration count
val iterations = mandelbrot.iterations(Flt64(-0.75), Flt64(0.1))
```

### Ordinary Mathematics

```kotlin
import fuookami.ospf.kotlin.math.ordinary.*

// GCD - supports multiple arguments
val gcd1 = gcd(UInt64(12), UInt64(8))              // 4
val gcd2 = gcd(UInt64(12), UInt64(8), UInt64(4))   // 4
val gcd3 = gcd(listOf(UInt64(0), UInt64(0)))       // 0 (edge case)

// LCM - handles zero correctly
val lcm1 = lcm(UInt64(4), UInt64(6))    // 12
val lcm2 = lcm(UInt64(4), UInt64(0))    // 0 (zero short-circuit)

// Prime number utilities
val primes = Prime.getPrimesUpTo(UInt64(100))  // [2, 3, 5, 7, 11, ...]
val isPrime = Prime.isPrime(UInt64(97))        // true

// Prime factorization
val factors = factorize(UInt64(60))  // [(2, 2), (3, 1), (5, 1)]

// Divisors
val divisors = getDivisors(UInt64(12))  // [1, 2, 3, 4, 6, 12]
```

### Combinatorics

```kotlin
import fuookami.ospf.kotlin.math.combinatorics.*

// Permutations
val perms = permutations(listOf(1, 2, 3))
// [[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]

// Combinations
val combs = combinations(listOf(1, 2, 3, 4), k = 2)
// [[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]]

// Cartesian product
val product = cross(listOf(1, 2), listOf('a', 'b'))
// [[1, 'a'], [1, 'b'], [2, 'a'], [2, 'b']]
```

### Parallel Operations

```kotlin
import fuookami.ospf.kotlin.math.parallel.fold

// Parallel fold with chunking to limit coroutine count
val sum = parallelFold(
    collection = largeList,
    initial = Flt64.zero,
    chunkSize = 100  // Limit concurrent coroutines
) { acc, item -> acc + item }

// Parallel reduction
val product = parallelReduce(
    collection = numbers,
    initial = Flt64.one,
    chunkSize = 50
) { acc, item -> acc * item }
```

### Comparison Operators

```kotlin
import fuookami.ospf.kotlin.math.ComparisonOperator

// Create operator with precision
val op = ComparisonOperator<Flt64, Flt64>(Flt64(1e-10))

// Use with tolerance
with(op) {
    val a = Flt64(1.0)
    val b = Flt64(1.0 + 1e-15)

    a eq b    // true (within tolerance)
    a neq b   // false
    a ls b    // false
    a leq b   // true
    a gr b    // false
    a geq b   // true
}
```

### Trivalent Logic

```kotlin
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.BalancedTrivalent

// Three-valued logic (True, False, Unknown)
val t = Trivalent.True
val f = Trivalent.False
val u = Trivalent.Unknown

// Conversion from nullable Boolean
val fromNullable: Trivalent = Trivalent(null)  // Unknown

// Numeric representation
val value: URtn8 = t.value  // 1
val isTrue: Boolean? = u.isTrue  // null

// Balanced trivalent (+1, -1, 0)
val balanced = BalancedTrivalent.True  // value = +1
```

### Scale and SI Prefixes

```kotlin
import fuookami.ospf.kotlin.math.Scale

// SI prefixes
val k = Scale.kilo      // 10^3
val M = Scale.mega      // 10^6
val m = Scale.milli     // 10^-3
val u = Scale.micro     // 10^-6

// Custom scales
val custom = Scale(2, 10)  // 2^10 = 1024

// Scale arithmetic
val km = Scale.kilo * Scale.milli  // Identity
val result = Scale.mega / Scale.kilo  // 10^3
```

## Performance Optimizations

| Feature | Optimization | Notes |
|---------|-------------|-------|
| GCD multi-arg | Iterator-based fold | Avoids % 0 vulnerability |
| LCM | Zero short-circuit | Prevents entering vulnerable GCD path |
| Prime cache | Sieve of Eratosthenes | O(n log log n) initialization |
| Factorization | O(sqrt(n)) upper bound | Uses sqrt(n) instead of n |
| Parallel fold | Chunk-based limiting | Controls coroutine count |
| Contract operation | Stride pre-computation | Output-driven iteration |
| Polynomial evaluation | Compile optimization | Reduces overhead for repeated evaluation |
| Interval extremum | Coefficient sign analysis | Linear scan over monomials |
| IntX/FltX | Lazy evaluation | BigDecimal operations only when needed |

## Testing

```bash
# Run all tests
mvn -pl ospf-kotlin-math test

# Run specific test classes
mvn -pl ospf-kotlin-math -Dtest=GCDTest,LCMTest test

# Run with verbose output
mvn -pl ospf-kotlin-math test -Dsurefire.useFile=false

# Run integration tests
mvn -pl ospf-kotlin-math verify
```

Test coverage includes:
- Algebraic structure laws (associativity, commutativity, distributivity)
- Number type conversions and arithmetic
- Symbolic expression parsing, evaluation, and differentiation
- Geometric primitive operations (point, vector, edge, triangle, circle, rectangle, quadrilateral)
- Delaunay triangulation
- 3D shapes (cuboid, cylinder, bounding box, placement)
- Chaos and fractal iterations
- Edge cases and boundary conditions

## Dependencies

| Module | Purpose |
|--------|---------|
| `ospf-kotlin-utils` | Utility functions, functional types, Either |
| `ospf-kotlin-multiarray` | Multi-dimensional array foundation |

## Related Modules

- [symbol/README.md](src/main/fuookami/ospf/kotlin/math/symbol/README.md) - Symbolic expression documentation
- [geometry/README.md](src/main/fuookami/ospf/kotlin/math/geometry/README.md) - Geometry module documentation
- [algebra/value_range/README.md](src/main/fuookami/ospf/kotlin/math/algebra/value_range/README.md) - Value range documentation

## License

This module is part of the OSPF Kotlin project and is licensed under the Apache License 2.0.
