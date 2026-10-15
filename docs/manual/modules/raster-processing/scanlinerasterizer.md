# ScanLineRasterizer

Converts polygons into a raster: the cells whose center is inside a polygon get a value, the others are novalue. The value can be:

- a constant, to make a mask of the polygons;
- the value of a numeric field of each polygon, as a class of land use or a parameter of a soil map, to turn a vector map into the input raster of a model;
- the value of a raster, when neither of the others is given: the raster is then cut with the polygons.

The grid of the raster is given by its bounds and its number of rows and columns, or it is taken from a raster, which must have the coordinate reference system of the polygons. Only polygons are supported. For lines, use [LinesRasterizer](linesrasterizer.md); for points, [PointsRasterizer](pointsrasterizer.md).

The module crosses the polygons with a line through the centers of each row of cells, the scanline, and fills the cells between the crossings, in parallel for the polygons. On geometries where this fails, the point in polygon mode checks the center of each cell instead: it is slower, needs a raster for the grid and uses only a constant value.

## Details

```{include} ../generated/ScanLineRasterizer_details.md
```

## Parameters

```{include} ../generated/ScanLineRasterizer.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. It rasterizes the subbasins of [BasinShape](../basin/basinshape.md) with their mean elevation:

```{literalinclude} ../examples/hm/scanlinerasterizer.groovy
:language: groovy
```

:::{figure} ../../images/modules/ScanLineRasterizer_subbasins_avgelev.png
:alt: The subbasins rasterized with their mean elevation: each subbasin is a single value, from the low ones near the outlet to the high ones near the ridges.

The subbasins rasterized with their mean elevation: each subbasin is a single value, from the low ones near the outlet to the high ones near the ridges.
:::
