# OSPF Kotlin Multiarray

:us: English | :cn: [简体中文](README_ch.md)

A high-performance multi-dimensional array library for Kotlin, supporting various storage orders, views, block-based sparse arrays, and tabular data structures.

### Features

- **Multi-dimensional Arrays**: Type-safe 1D to N-dimensional arrays with compile-time shape checking
- **Storage Orders**: Row-major (C-style) and Column-major (Fortran-style) storage orders
- **Shape System**: Compile-time shapes (Shape1-4) and dynamic shapes (DynShape)
- **Views**: Slicing, indexing, and dimension-mapping views without data copying
- **Block Arrays**: Sparse array support with block-based storage
- **DataFrame**: 2D tabular data structure with named columns
- **Extension Operators**: Multi-dimensional access operators for List and Map types
- **Iterator Protocol**: Access order-aware iteration with proper snapshot semantics

### Installation

Add dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-multiarray</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Quick Start

```kotlin
import fuookami.ospf.kotlin.multiarray.*

// Create a 2x3 array filled with zeros
val array = MutableMultiArray.newWith(Shape2(2, 3), 0)

// Set and get values
array[0, 1] = 10
array[1, 2] = 20
println(array[0, 1])  // 10

// Create with generator
val matrix = MultiArray.newBy(Shape3(2, 3, 4)) { i, vec -> i * 10 }

// Iterate with (linearIndex, vectorIndex, value)
for ((linearIdx, vectorIdx, value) in matrix.enumerate()) {
    println("[$linearIdx] ${vectorIdx.toList()} = $value")
}

// Slice using dummy indices
val view = array[_a, 1]  // All rows, column 1

// Convert storage order
val columnMajor = matrix.toStorageOrder(StorageOrder.ColumnMajor)
```

### Core Components

#### Shape System

| Type | Description | Use Case |
|------|-------------|----------|
| `Shape1` | 1-dimensional shape | Vectors |
| `Shape2` | 2-dimensional shape | Matrices |
| `Shape3` | 3-dimensional shape | Tensors |
| `Shape4` | 4-dimensional shape | Batch tensors |
| `DynShape` | Dynamic dimension shape | Runtime-determined shapes |

```kotlin
// Create shapes with specific storage order
val rowMajor = Shape3(2, 3, 4)
val columnMajor = Shape3.withOrder(2, 3, 4, StorageOrder.ColumnMajor)

// Shape operations
val shape = Shape2(3, 4)
println(shape.dimension)   // 2
println(shape.size)         // 12
val vec = shape.vector(5)   // [1, 1] (linear index to vector)
val idx = shape.index(intArrayOf(1, 1))  // 5 (vector to linear index)
```

#### MultiArray and MutableMultiArray

`MultiArray` is immutable; `MutableMultiArray` allows element mutation.

```kotlin
// Factory methods
val arr1 = MultiArray.new(Shape2(3, 3))                    // Default values
val arr2 = MultiArray.newWith(Shape2(3, 3), 42)            // Fill with value
val arr3 = MultiArray.newBy(Shape2(3, 3)) { i, v -> i }    // Generator
val arr4 = MultiArray.fromList(Shape2(2, 2), listOf(1, 2, 3, 4))  // From list

// Mutable array
val mutable = MutableMultiArray.newWith(Shape2(2, 2), 0)
mutable[0, 0] = 1
mutable[1, 1] = 2
val immutable = mutable.toImmutable()

// Convenience functions
val v = multiArrayOf(5, 0)           // 1D array of 5 zeros
val m = multiArrayOf(3, 3, 0)        // 3x3 matrix of zeros
```

#### Views and Slicing

Views provide zero-copy access to sub-arrays.

```kotlin
val array = MultiArray.newBy(Shape3(2, 3, 4)) { i, _ -> i }

// Slice view using dummy indices
val slice = array[_a, 1, _a]      // All rows, column 1, all depths
val range = array[0..1, _a, 0]    // Rows 0-1, all columns, depth 0

// Mapped view (transpose)
val transposed = MappedMultiArrayView(array, listOf(
    MapIndex.Map(2),  // dimension 2 -> position 0
    MapIndex.Map(0),  // dimension 0 -> position 1
    MapIndex.Map(1)   // dimension 1 -> position 2
))
```

#### DummyIndex and MapIndex

`DummyIndex` types for slicing:

| Type | Example | Description |
|------|---------|-------------|
| `DummyIndex.All` | `_a` | All elements in dimension |
| `DummyIndex.Index(n)` | `1` | Single index |
| `DummyIndex.Range(r)` | `0..2` | Continuous range |
| `DummyIndex.IndexArray(list)` | `[0, 2, 4]` | Discrete indices |

`MapIndex` types for dimension remapping:

| Type | Example | Description |
|------|---------|-------------|
| `MapIndex.Map(i)` | `MapIndex.Map(0)` | Map dimension to position |
| `MapIndex.Dummy(d)` | `MapIndex.Dummy(_a)` | Keep as dummy index |

#### BlockMultiArray (Sparse)

Sparse array that only stores non-default values.

```kotlin
// Create sparse array
val sparse = BlockMultiArray.empty<Int, Shape3>(Shape3(100, 100, 100))

// Set only non-zero values
sparse[intArrayOf(0, 0, 0)] = 1
sparse[intArrayOf(50, 50, 50)] = 2

// Convert to dense array
val dense = sparse.toMultiArray(defaultValue = 0)

// Create from existing array (with filter)
val sparseFromDense = BlockMultiArray.fromMultiArray(dense) { it != 0 }
```

#### DataFrame

2D tabular data structure with named columns, similar to pandas DataFrame.

```kotlin
// Create from column data
val df = dataFrameOf(
    "name" to listOf("Alice", "Bob", "Charlie"),
    "age" to listOf(25, 30, 35),
    "city" to listOf("NYC", "LA", "Chicago")
)

// Access data
println(df.getByName(0, "name"))  // Alice
println(df.getColumnByName("age"))  // [25, 30, 35]

// Filter and select
val filtered = df.filter { row -> (row[1] as Int) > 25 }
val selected = df.select("name", "city")

// Builder pattern
val df2 = DataFrame.build<String>("x", "y") {
    row("a", "b")
    row("c", "d")
}

// Convert to MultiArray
val multiArray = df.toNullableMultiArray()
```

#### List Extensions

Multi-dimensional access operators for nested List types.

```kotlin
val matrix: List2<Int> = listOf(
    listOf(1, 2, 3),
    listOf(4, 5, 6)
)

// Get column from all rows
val col = matrix[_a, 1]  // [2, 5]

// Get all columns from row
val row = matrix[0, _a]  // [1, 2, 3]

// 3D list access
val cube: List3<Int> = listOf(
    listOf(listOf(1, 2), listOf(3, 4)),
    listOf(listOf(5, 6), listOf(7, 8))
)
val val = cube[0, 1, 0]  // 3
val allFirst = cube[_a, 0, 0]  // [1, 5]
```

#### Map Extensions

Multi-dimensional access operators for Map types containing MultiArray values.

```kotlin
val map = mapOf(
    "a" to MultiArray.newWith(Shape2(2, 2), 1),
    "b" to MultiArray.newWith(Shape2(2, 2), 2)
)

// Access by key and index
val v = map["a", 0, 1]       // Get element from MultiArray value
val all = map[_a]             // Get all values (Iterable)
map["b", 0, 1] = 42          // Set element in MutableMultiArray value

// Works with MultiMap2, MultiMap3, MultiMap4 as well
```

#### Access Order and Iteration

```kotlin
// Row-major iteration (default, C-style)
for (idx in shape.indices(AccessOrder.RowMajor)) {
    println(idx.toList())
}

// Column-major iteration (Fortran-style)
for (idx in shape.indices(AccessOrder.ColumnMajor)) {
    println(idx.toList())
}

// Flatten with specific order
val rowFlat = array.flatten(AccessOrder.RowMajor)
val colFlat = array.flatten(AccessOrder.ColumnMajor)

// Enumerate with order
for ((i, vec, value) in array.enumerateWithOrder(AccessOrder.ColumnMajor)) {
    // ...
}
```

### API Reference

#### Factory Methods

- `MultiArray.new(shape)` - Create with default values
- `MultiArray.newWith(shape, value)` - Create with specific fill value
- `MultiArray.newBy(shape, generator)` - Create with generator function `(linearIndex, vectorIndex) -> T`
- `MultiArray.fromList(shape, list, accessOrder)` - Create from flat list
- `MutableMultiArray.new(...)` / `newWith(...)` / `newBy(...)` / `fromList(...)` - Mutable variants

#### Access Methods

- `array[i]` - Linear index access
- `array[i, j, k]` - Vector index access
- `array[vec]` - IntArray vector access
- `array[_a, 1]` - Slice/view access (returns `MultiArrayView`)

#### Iteration

- `array.iterate(order)` - Iterate with specified access order
- `array.enumerate()` - Iterate as `(linearIndex, vectorIndex, value)` triples
- `array.enumerateWithOrder(order)` - Enumerate with specific access order
- `array.flatten(order)` - Convert to flat list

### Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ShapeColumnMajorInverseTest
```

### License

MIT License
