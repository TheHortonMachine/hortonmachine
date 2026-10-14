# LinesPolygonizer

Builds polygons from lines, as the parcels of a drawing whose borders were traced as single lines. In the default mode, faces, every area closed by the lines becomes a polygon, also where the lines cross without a shared vertex. In the join mode the lines of each group are chained into a single polygon, which closes small gaps between them. The id of the polygons can be taken from points that lie in them, as the labels of a drawing.

## Example

The example builds the elevation belts of the basin of [ExtractBasin](../dem-manipulation/extractbasin.md). It takes the contour lines cut to the basin by [VectorClipper](vectorclipper.md) and adds the outline of the basin, made into a line by [VectorReshaper](vectorreshaper.md) and merged with [VectorMerger](vectormerger.md):

```{literalinclude} ../examples/hm/linespolygonizer.groovy
:language: groovy
```

The 10 contour lines and the divide close 11 belts. Their areas sum up to the 1.89 km² of the basin. The lines are drawn in blue:

```{image} ../../images/modules/LinesPolygonizer_basin_belts_belt.png
:alt: The elevation belts of the basin, each with its own color, between the contour lines
```

The belts are numbered in the order the polygonizer finds them, not by elevation. Without the divide, the contours would close no belt, since they end on the border of the basin.

## Reference

```{include} ../generated/LinesPolygonizer.md
```
