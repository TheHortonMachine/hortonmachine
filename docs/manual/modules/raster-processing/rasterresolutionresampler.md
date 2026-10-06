# RasterResolutionResampler

Resamples a raster to a new cell size, on the same extent and in the same coordinate reference system. Larger cells make the maps lighter and the computations faster, at the price of detail, as for a first analysis of a large area; smaller cells don't add detail, they only split the existing one. As for the reprojection, nearest neighbour keeps the original values, bilinear and bicubic smooth them.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/rasterresolutionresampler.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterResolutionResampler_dtm_50m.png
:alt: The elevation of the sample area resampled from 10 m to 50 m cells.

The elevation of the sample area resampled from 10 m to 50 m cells.
:::

## Reference

```{include} ../generated/RasterResolutionResampler.md
```
