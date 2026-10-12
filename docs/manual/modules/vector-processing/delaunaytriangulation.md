# DelaunayTriangulation

Creates the Delaunay triangulation of a set of points: the triangles that join them without crossing, as close to equilateral as possible, the base of a TIN surface from surveyed points. The triangles get the lowest and highest elevation of their vertices.

## Example

The example triangulates the centroids of the sub-basins of [BasinShape](../basin/basinshape.md), made by [VectorReshaper](vectorreshaper.md), with their mean elevation:

```{literalinclude} ../examples/hm/delaunaytriangulation.groovy
:language: groovy
```

The 99 centroids give 184 triangles, which cover their convex hull. The triangles colored by the highest elevation of their vertices: the long ones on the borders join the outer centroids of the convex hull:

```{image} ../../images/modules/DelaunayTriangulation_triangles_maxelev.png
:alt: The Delaunay triangles between the centroids of the sub-basins, colored by their highest elevation
```

## Reference

```{include} ../generated/DelaunayTriangulation.md
```
