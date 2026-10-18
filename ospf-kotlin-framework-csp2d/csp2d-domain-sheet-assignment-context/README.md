# ospf-kotlin-framework-csp2d-domain-sheet-assignment-context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`csp2d-domain-sheet-assignment-context` provides Consumed segment height for coils (strip packing) and sheet count for fixed sheets, plus a pure helper computing consumed length, over-length and sheet count from usage counts.

## Boundary

Part of the [csp2d](../README.md) framework. The generic layering discipline keeps `FltX` out of the domain modules;
it appears only in the modelling boundary (`produce`) and in `application`. The 2D geometry is shared through
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README.md).

## Parent Module

[OSPF Kotlin Framework CSP2D](../README.md)
