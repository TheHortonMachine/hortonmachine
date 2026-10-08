# VectorSimplifier

Simplifies the geometries of a vector, dropping the vertices that do not change their shape beyond a tolerance, or rounds their coordinates to a given precision. The lines extracted from rasters, as the contours of [ContourExtractor](contourextractor.md) or the polygons of [Vectorizer](vectorizer.md), have a vertex for each cell crossed: simplified, they are lighter to store and draw, and lose the staircase of the cells.

## Example

The example runs on the contour lines of ContourExtractor, extracted from the sample elevation model of the manual:

```{literalinclude} ../examples/hm/vectorsimplifier.groovy
:language: groovy
```

:::{figure} ../../images/modules/VectorSimplifier_contours_simple.png
:alt: The contour lines of ContourExtractor, simplified with a tolerance of 25 m.

The contour lines of ContourExtractor, simplified with a tolerance of 25 m.
:::

## Reference

```{include} ../generated/VectorSimplifier.md
```
