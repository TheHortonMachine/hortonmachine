# RasterResizer

Cuts a raster to a smaller area, without resampling it: the cells keep their size and position.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/rasterresizer.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterResizer_dtm_basin.png
:alt: The elevation of the sample area, cut to the extent of the basin of ExtractBasin.

The elevation of the sample area, cut to the extent of the basin of ExtractBasin.
:::

## Reference

```{include} ../generated/RasterResizer.md
```
