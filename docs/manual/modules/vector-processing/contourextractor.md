# ContourExtractor

Extracts the contour lines of a raster, at regular intervals of its values between a minimum and a maximum, as a vector of lines with their value. With an elevation model they are the contours of the maps; with other rasters, the lines of equal value, as of equal precipitation or temperature.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/contourextractor.groovy
:language: groovy
```

:::{figure} ../../images/modules/ContourExtractor_contours.png
:alt: The contour lines of the sample area, every 100 m.

The contour lines of the sample area, every 100 m.
:::

## Reference

```{include} ../generated/ContourExtractor.md
```
