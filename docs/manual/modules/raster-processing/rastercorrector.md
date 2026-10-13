# RasterCorrector

Sets single cells of a raster to new values, or to novalue. It is meant for a few manual fixes, as a spike of an elevation model, a pit that a pit filling should not fill, or the outlet cell that a network extraction needs to be a channel. The cells are given by their column and row, as shown by the tools that inspect a raster, or by the coordinates of a point read off a map.

To change many cells, use [Mapcalc](mapcalc.md) or [RasterNull](rasternull.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`:

```{literalinclude} ../examples/hm/rastercorrector.groovy
:language: groovy
```

## Reference

```{include} ../generated/RasterCorrector.md
```
