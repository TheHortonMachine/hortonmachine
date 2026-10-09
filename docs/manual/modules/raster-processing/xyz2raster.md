# Xyz2Raster

Converts a text file of XYZ triplets on a regular grid into a raster. Many elevation models, especially the lidar ones of the public administrations, are distributed this way: a line for each cell, with the coordinates of its center and its elevation.

The points must be on a regular grid, whose resolution has to be given, together with the coordinate reference system of the coordinates, since the text file doesn't have them. Each point becomes the value of the cell centered on it, and the cells without a point are novalue: it is a conversion, not an interpolation. To make a raster from scattered points, as the ones of a survey, use [SurfaceInterpolator](surfaceinterpolator.md).

The values can be separated by spaces, tabs or by a given separator; the lines that are not three numbers, as a header, are skipped.

## Example

The example reads back into a raster the triplets of the sample elevation model of the manual written by [Raster2Xyz](raster2xyz.md):

```{literalinclude} ../examples/hm/raster2xyz.groovy
:language: groovy
```

## Reference

```{include} ../generated/Xyz2Raster.md
```
