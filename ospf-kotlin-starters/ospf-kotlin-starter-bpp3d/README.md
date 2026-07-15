# ospf-kotlin-starter-bpp3d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starter-bpp3d` is the starter artifact for the BPP3D framework stack.

## Included Modules

This starter depends on the general [ospf-kotlin-starter](../ospf-kotlin-starter/README.md) and the BPP3D framework modules:

| Module | Purpose |
| --- | --- |
| `ospf-kotlin-framework-bpp3d-infrastructure` | Shared geometry, DTO, and domain primitives |
| `ospf-kotlin-framework-bpp3d-domain-item-context` | Item, material, bin, layer, pattern, and shadow-price models |
| `ospf-kotlin-framework-bpp3d-domain-packing-context` | Packing, validation, renderer output, and solution assembly |
| `ospf-kotlin-framework-bpp3d-domain-bla-context` | Bottom-Up Left-Justified placement context |
| `ospf-kotlin-framework-bpp3d-domain-block-loading-context` | Block generation and search contexts |
| `ospf-kotlin-framework-bpp3d-domain-layer-generation-context` | Layer candidate generation |
| `ospf-kotlin-framework-bpp3d-domain-layer-assignment-context` | Layer assignment model and pipelines |
| `ospf-kotlin-framework-bpp3d-application` | Application-level column-generation orchestration |

## Usage

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp3d</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp3d -am -DskipTests package
```
