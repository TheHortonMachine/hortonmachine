# RasterReprojector

Reprojects a raster into another coordinate reference system, given by its code, as EPSG:32632 for WGS 84 / UTM zone 32N. The cells of the new grid are computed from the original ones with the chosen interpolation: nearest neighbour keeps the original values, as needed for maps of classes or codes, bilinear and bicubic smooth them, better for continuous values as the elevation. The cell size of the output can be set; if not, it is estimated from the original one.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/rasterreprojector.groovy
:language: groovy
```

## Reference

```{include} ../generated/RasterReprojector.md
```
