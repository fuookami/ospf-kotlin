/*

 * Copyright (c) 2024-2025 Fuookami. All rights reserved.

 */



package fuookami.ospf.kotlin.framework.bpp3d.infrastructure



import fuookami.ospf.kotlin.utils.math.*

import fuookami.ospf.kotlin.utils.math.ordinary.*

import fuookami.ospf.kotlin.utils.math.geometry.projectivePlane.*



/** 射影平面上的几何映射 / Geometry mapping on projective plane */

internal object ProjectivePlaneGeometryMapping {

    /** 射影平面上的点 / Point on projective plane */

    internal data class Point(

        /** 在射影平面上的坐标 / Coordinates on projective plane */

        val coordinate: ProjectivePlane

    ) {

        internal companion object {

            /** 将原始坐标映射到射影平面 / Map original coordinates to projective plane */

            internal fun map(original: GeometryMapping.Point): Point {

                return Point(

                    coordinate = ProjectivePlane(

                        x = original.coordinate.x / original.coordinate.z,

                        y = original.coordinate.y / original.coordinate.z

                    )

                )

            }

        }

    }



    /** 射影平面上的边 / Edge on projective plane */

    internal data class Edge(

        /** 边的起点 / Start point of the edge */

        val start: Point,

        /** 边的终点 / End point of the edge */

        val end: Point

    ) {

        internal companion object {

            /** 将原始边映射到射影平面 / Map original edge to projective plane */

            internal fun map(original: GeometryMapping.Edge): Edge {

                return Edge(

                    start = Point.map(original.start),

                    end = Point.map(original.end)

                )

            }

        }

    }



    /** 射影平面上的面 / Face on projective plane */

    internal data class Face(

        /** 面的边列表 / List of edges of the face */

        val edges: List<Edge>

    ) {

        internal companion object {

            /** 将原始面映射到射影平面 / Map original face to projective plane */

            internal fun map(original: GeometryMapping.Face): Face {

                return Face(

                    edges = original.edges.map { Edge.map(it) }

                )

            }

        }

    }



    /** 射影平面上的多边形 / Polygon on projective plane */

    internal data class Polygon(

        /** 多边形的面列表 / List of faces of the polygon */

        val faces: List<Face>

    ) {

        internal companion object {

            /** 将原始多边形映射到射影平面 / Map original polygon to projective plane */

            internal fun map(original: GeometryMapping.Polygon): Polygon {

                return Polygon(

                    faces = original.faces.map { Face.map(it) }

                )

            }

        }

    }



    /** 将原始几何映射转换为射影平面几何映射 / Convert original geometry mapping to projective plane geometry mapping */

    internal fun map(original: GeometryMapping): List<List<Polygon>> {

        return original.map { row ->

            row.map { Polygon.map(it) }

        }

    }

}

