# ospf-kotlin-framework-csp2d-domain-produce-context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`csp2d-domain-produce-context` provides The master problem: pattern usage variables, aggregate area/height expressions, demand coverage, material availability, machine capacity and configurable usage objectives. This is the modelling boundary where FltX appears.

## Boundary

Part of the [csp2d](../README.md) framework. The generic layering discipline keeps `FltX` out of the domain modules;
it appears only in the modelling boundary (`produce`) and in `application`. The 2D geometry is shared through
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README.md).

## Parent Module

[OSPF Kotlin Framework CSP2D](../README.md)
