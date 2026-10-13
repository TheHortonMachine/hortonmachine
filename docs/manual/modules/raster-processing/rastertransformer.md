# RasterTransformer

Flips, rotates, scales and translates a raster. The flips and the rotation move the values on the grid; the scale and the translation change only where the raster lies, its georeference. The typical uses are a raster with a wrong georeference, as a scanned map placed by hand or a grid with the rows upside down, and the rotation of a model to align it with a direction, as the axis of a valley or of a flume.

The rotation needs new values for the cells of the rotated grid: the nearest cell suits the classes, the bilinear interpolation the continuous values, as the elevation. To change the reference system of a raster, use [RasterReprojector](rasterreprojector.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`. It rotates the elevation model by 30 degrees counterclockwise:

```{literalinclude} ../examples/hm/rastertransformer.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterTransformer_dtm_rotated.png
:alt: The elevation model rotated by 30 degrees, on a larger grid with novalue corners.

The elevation model rotated by 30 degrees: the grid covers the whole rotated raster, the corners outside of it are novalue.
:::

## Reference

```{include} ../generated/RasterTransformer.md
```
