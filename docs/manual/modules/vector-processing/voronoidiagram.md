# VoronoiDiagram

Creates the Voronoi diagram, or the Thiessen polygons, of a set of points: around each point, the cell of the places closer to it than to any other point, with the attributes of the point. It gives the area of influence of each point, as the area of each rain gauge in the Thiessen method for the mean rainfall of a basin.

## Details

```{include} ../generated/VoronoiDiagram_details.md
```

## Parameters

```{include} ../generated/VoronoiDiagram.md
```

## Example

The example makes the cells of the centroids of the sub-basins of the [DelaunayTriangulation](delaunaytriangulation.md) example:

```{literalinclude} ../examples/hm/voronoidiagram.groovy
:language: groovy
```

The 99 cells, colored by the mean elevation of their sub-basin, with the centroids in blue. The outer cells reach the cut rectangle of the diagram, beyond the map:

```{image} ../../images/modules/VoronoiDiagram_voronoi_avgelev.png
:alt: The Voronoi cells of the centroids of the sub-basins, colored by their mean elevation
```
