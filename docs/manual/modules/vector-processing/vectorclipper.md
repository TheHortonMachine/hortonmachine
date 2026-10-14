# VectorClipper

Cuts the features of a vector, points, lines or polygons, to the area covered by the polygons of another vector, as the roads of a region to a municipality. The features keep their attributes. To also get the attributes of the polygons, use [VectorIntersector](vectorintersector.md).

## Example

The example cuts the contour lines of [ContourExtractor](contourextractor.md) to the basin of [ExtractBasin](../dem-manipulation/extractbasin.md):

```{literalinclude} ../examples/hm/vectorclipper.groovy
:language: groovy
```

Of the 38 contour lines, 10 cross the basin, outlined in blue, and are kept with the part inside it and their elevation:

```{image} ../../images/modules/VectorClipper_contours_basin_value.png
:alt: The contour lines cut to the basin
```

## Reference

```{include} ../generated/VectorClipper.md
```
