# ospf-kotlin-framework-csp2d-domain-material-context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`csp2d-domain-material-context` provides Products, demands, dual-mode materials (coil or fixed sheet), machines, by-products, cutting patterns and cutting plans, with geometry and contribution validation.

## Boundary

Part of the [csp2d](../README.md) framework. The generic layering discipline keeps `FltX` out of the domain modules;
it appears only in the modelling boundary (`produce`) and in `application`. The 2D geometry is shared through
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README.md).

## Parent Module

[OSPF Kotlin Framework CSP2D](../README.md)
