# LinesRasterizer

Converts lines into a raster: the cells crossed by a line get its value, a constant or the one of a numeric field, the others are novalue. The lines become chains of cells one cell wide and connected also by the corners, as the paths of the flow directions, so a vector network, as a mapped river network, can take the place of the network extracted from an elevation model, or burn the channels into it with [Carver](carver.md).

The grid is given by its bounds and its number of rows and columns, or it is taken from a raster, which must have the coordinate reference system of the lines. For polygons, use [ScanLineRasterizer](scanlinerasterizer.md); for points, [PointsRasterizer](pointsrasterizer.md).

## Details

```{include} ../generated/LinesRasterizer_details.md
```

## Parameters

```{include} ../generated/LinesRasterizer.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. It rasterizes the network of [NetworkAttributesBuilder](../network/networkattributesbuilder.md) with the Strahler order of its reaches:

```{literalinclude} ../examples/hm/linesrasterizer.groovy
:language: groovy
```

:::{figure} ../../images/modules/LinesRasterizer_net_strahler.png
:alt: The network rasterized with the Strahler order of each reach.

The network rasterized with the Strahler order of its reaches: at the confluences the cell keeps the higher order.
:::
