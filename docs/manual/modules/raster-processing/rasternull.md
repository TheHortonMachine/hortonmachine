# RasterNull

Sets the cells of one value of a raster to no-data, or, with the inverse, all the cells of the other values. It cleans the rasters whose missing data are stored as a value, and extracts the cells of one class of a classified raster, as a mask for other modules.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/rasternull.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterNull_belt_middle.png
:alt: The cells of the middle elevation belt of RangeLookup, from 1300 to 1700 m.

The cells of the middle elevation belt of [RangeLookup](rangelookup.md), from 1300 to 1700 m: all the other cells are no-data.
:::

## Reference

```{include} ../generated/RasterNull.md
```
