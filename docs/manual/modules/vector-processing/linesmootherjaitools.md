# LineSmootherJaitools

Smooths lines with cubic Bezier curves through all their vertices: the lines keep their vertices and get round turns between them. It suits lines with few vertices, as the simplified ones, drawn with corners.

## Example

The example smooths the contour lines simplified by [VectorSimplifier](vectorsimplifier.md), with a tolerance of 25 m:

```{literalinclude} ../examples/hm/linesmootherjaitools.groovy
:language: groovy
```

A detail of the smoothed lines in red, over the original contours of [ContourExtractor](contourextractor.md) in blue: the curves through the few vertices of the simplified lines follow the general course of the contours, without their small turns:

```{image} ../../images/modules/LineSmootherJaitools_contours_bezier.png
:alt: A detail of the simplified contour lines smoothed by the Bezier curves, with the original ones
```

The [LineSmootherMcMaster](linesmoothermcmaster.md) smooths instead by averaging the vertices.

## Reference

```{include} ../generated/LineSmootherJaitools.md
```
