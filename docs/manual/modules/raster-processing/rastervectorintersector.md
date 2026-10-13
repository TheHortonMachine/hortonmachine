# RasterVectorIntersector

Cuts a raster with polygons: it keeps the values of the cells inside the polygons, or with the inverse switch the ones outside, and sets the others to novalue, on the same grid. It keeps, for example, the elevation of a basin from its polygon, or blanks the lakes of an elevation model.

It does in one step what [ScanLineRasterizer](scanlinerasterizer.md), with the raster as grid, and [CutOut](cutout.md) do in two. To also shrink the grid to the polygons, use [RasterResizer](rasterresizer.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. It cuts the elevation with the high cells of the [VectorFilter](../vector-processing/vectorfilter.md) example, and with its inverse:

```{literalinclude} ../examples/hm/rastervectorintersector.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterVectorIntersector_dtm_high_cells.png
:alt: The elevation inside the high cells of the VectorFilter example.

The elevation inside the high cells.
:::

:::{figure} ../../images/modules/RasterVectorIntersector_dtm_not_high_cells.png
:alt: The elevation outside of the high cells, with the inverse switch.

The elevation outside of them, with the inverse switch.
:::

## Reference

```{include} ../generated/RasterVectorIntersector.md
```
