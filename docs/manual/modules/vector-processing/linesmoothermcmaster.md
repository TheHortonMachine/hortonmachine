# LineSmootherMcMaster

Smooths lines with the sliding averaging of McMaster: each vertex moves towards the mean position of the vertices around it. It removes the small wiggles of the lines, as those of the contours or of the borders extracted from a raster, keeping their general course and their ends.

## Details

```{include} ../generated/LineSmootherMcMaster_details.md
```

## Parameters

```{include} ../generated/LineSmootherMcMaster.md
```

## Example

The example smooths the contour lines of [ContourExtractor](contourextractor.md), extracted from the sample elevation model, on a window of about 80 m:

```{literalinclude} ../examples/hm/linesmoothermcmaster.groovy
:language: groovy
```

A detail of the smoothed lines in red, under the original ones in blue: the small turns of the contours, that follow the cells of the elevation model, are gone:

```{image} ../../images/modules/LineSmootherMcMaster_contours_mcmaster.png
:alt: A detail of the contour lines smoothed by the sliding averaging, under the original ones
```

The [LineSmootherJaitools](linesmootherjaitools.md) smooths instead with curves through the vertices.
