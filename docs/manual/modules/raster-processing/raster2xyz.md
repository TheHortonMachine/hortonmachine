# Raster2Xyz

Converts a raster into a text file of XYZ triplets, one line for each cell, with the easting and the northing of its center and its value, separated by tabs:

```text
1637215.0	5114435.0	1207.594970703125
1637225.0	5114435.0	1217.385986328125
...
```

These are the first lines for the sample elevation model of the example below: the cells are written row by row, from the north west corner, and the novalues are left out, as the first cells of this raster. The values of rasters of floating point numbers are written with all the digits of their conversion. It is the plain format to pass a raster to programs that don't read the raster formats, or to look at its values. [Xyz2Raster](xyz2raster.md) reads it back.

## Example

The example converts the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, into triplets and back into a raster:

```{literalinclude} ../examples/hm/raster2xyz.groovy
:language: groovy
```

## Reference

```{include} ../generated/Raster2Xyz.md
```
