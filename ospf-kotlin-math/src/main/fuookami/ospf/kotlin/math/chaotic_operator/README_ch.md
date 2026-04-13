# ospf-kotlin-math/chaotic_operator

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的混沌系统吸引子和迭代映射。提供 27+ 著名混沌系统的实现，用于仿真、可视化和研究。

## 可用系统

### 3D 连续系统（微分方程）

| 系统 | 参数 | 描述 |
|------|------|------|
| `LorenzSystem` | a, b, c, h | 经典洛伦兹吸引子（蝴蝶效应），1963 |
| `ChenSystem` | a, b, c, h | 陈氏吸引子，洛伦兹的对偶 |
| `RosslerAttractor` | a, b, c, h | 罗斯勒吸引子，比洛伦兹更简单 |
| `ChenCelikovskyAttractor` | a, b, c, d, h | 广义陈氏系统 |
| `ChenLeeAttractor` | a, b, c, h | 陈-李变体 |
| `CoulletAttractor` | a, b, c, h | 库莱特系统 |
| `BurkeShawAttractor` | a, b, c, h | 伯克-肖系统 |
| `BoualiAttractor` | a, b, c, d, h | 布阿里系统 |
| `AizawaAttractor` | a, b, c, d, e, f, h | 相泽吸引子 |
| `AnishchenkoAstakhovAttractor` | a, b, c, d, h | 阿尼先科-阿斯塔霍夫系统 |
| `ArneodoAttractor` | a, b, c, d, h | 阿内奥多吸引子 |
| `ChuaAttractor` | a, b, c, d, e, h | 蔡氏电路吸引子 |
| `Brusselator` | a, b, h | 布鲁塞尔子化学模型 |

### 2D/3D 离散映射

| 系统 | 参数 | 描述 |
|------|------|------|
| `ArnoldsCatMap` | h | 阿诺德猫映射 |
| `BakersMap` | - | 面包师映射 |
| `BogdanovMap` | a, b, c, d | 波格丹诺夫映射 |
| `CircleMap` | K, Omega | 圆映射 |
| `ArnoldTongue` | - | 阿诺德舌可视化 |
| `ChebyshevMap` | n | 切比雪夫多项式映射 |
| `GaussMap` | a, q | 高斯映射 |
| `ComplexQuadraticPolynomial` | c | 复二次映射（曼德博/朱利亚） |
| `ComplexSquaringMap` | - | 复平方映射 |

### 物理和生物模型

| 系统 | 参数 | 描述 |
|------|------|------|
| `BiologyChaoticModel` | a, b, c, d, h | 生物混沌模型 |
| `CapacitanceEquation` | a, b, c, d, h | 电容混沌方程 |
| `CircuitChaotic` | a, b, c, h | 电子电路混沌 |
| `ChuaCircuit` | alpha, beta, m0, m1, h | 蔡氏电路模型 |
| `DoublePendulumSystem` | g, l1, l2, m1, m2, h | 双摆系统 |
| `CoupledLorenzAttractor` | a, b, c, k, h | 耦合洛伦兹系统 |

## 使用示例

```kotlin
import fuookami.ospf.kotlin.math.chaotic_operator.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point3
import fuookami.ospf.kotlin.math.geometry.point3

// 洛伦兹吸引子
val lorenz = LorenzSystem(
    a = Flt64(10.0),
    b = Flt64(28.0),
    c = Flt64(8.0 / 3.0),
    h = Flt64(0.01)
)

// 生成轨迹
var state = point3(Flt64(1.0), Flt64(1.0), Flt64(1.0))
val trajectory = mutableListOf(state)
repeat(10000) {
    state = lorenz(state)
    trajectory.add(state)
}

// 使用 Generator 惰性迭代
val generator = LorenzSystem.Generator(
    a = Flt64(10.0),
    b = Flt64(28.0),
    c = Flt64(8.0 / 3.0),
    h = Flt64(0.01),
    initial = point3(Flt64(1.0), Flt64(1.0), Flt64(1.0))
)
val first100 = generator.asSequence().take(100).toList()
```

## 相关链接

- [主 README](../../README.md)
- [Fractal Operator 模块](../fractal_operator/README_ch.md)
- [Geometry 模块](../geometry/README_ch.md)
