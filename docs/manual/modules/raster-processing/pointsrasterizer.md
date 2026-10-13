# PointsRasterizer

Converts points into a raster: the cell that contains each point gets its value, a constant or the one of a numeric field, the others are novalue. It places measures, as the rain gauges or the survey points of a field campaign, on the grid of a model, and gives the presences that [KernelDensity](kerneldensity.md) turns into a density.

The grid is given by its bounds, cell size and reference system, or it is taken from a raster, which must have the coordinate reference system of the points. To interpolate the values of the points on all the cells, use [SurfaceInterpolator](surfaceinterpolator.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. It places the centroids of the subbasins of the [DelaunayTriangulation](../vector-processing/delaunaytriangulation.md) example, with their mean elevation, on the cells of 100 m of [RasterSubsampler](rastersubsampler.md):

```{literalinclude} ../examples/hm/pointsrasterizer.groovy
:language: groovy
```

:::{figure} ../../images/modules/PointsRasterizer_centroids_avgelev.png
:alt: The cells of 100 m that contain the centroids of the subbasins, colored by their mean elevation.

The cells of 100 m that contain the centroids of the subbasins, with the mean elevation of each subbasin.
:::

## Reference

```{include} ../generated/PointsRasterizer.md
```
