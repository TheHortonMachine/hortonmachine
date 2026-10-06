# Mosaic12

Patches up to 12 rasters into one, which covers all of them, as the tiles of an elevation model delivered in pieces. The output takes the cell size of the first raster. Where the rasters overlap, the merge mode decides the value: the first one (INSERT_ON_NOVALUE), the last one (SUBSTITUTE), or their average or sum.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/mosaic12.groovy
:language: groovy
```

:::{figure} ../../images/modules/Mosaic12_dtm_mosaic.png
:alt: The elevation cut to the extents of the basins of ExtractBasin and of Wateroutlet, patched into one raster that covers both: the cells outside the two pieces are no-data.

The elevation cut to the extents of the basins of ExtractBasin and of Wateroutlet, patched into one raster that covers both: the cells outside the two pieces are no-data.
:::

## Reference

```{include} ../generated/Mosaic12.md
```
