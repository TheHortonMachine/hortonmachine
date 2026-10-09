# Las2RasterInterpolator

Las2RasterInterpolator interpolates the points of a lidar survey over the ground into a raster, with inverse distance weighting, on the grid of an elevation model. It interpolates the height of the points over the ground, for a model of the height of the canopy, or their elevation, for a model of its surface. Unlike [LasOnRasterMapper](lasonrastermapper.md), it leaves no holes.

## Example

The height of the canopy of the sample plot, from the first returns:

```{literalinclude} ../examples/hm/las2rasterinterpolator.groovy
:language: groovy
```

:::{figure} ../../images/modules/Las2RasterInterpolator_chm_idw.png
:alt: The height of the canopy of the plot, up to about 20 m: the round crowns of the trees, smoother than the highest points of each cell, and the road at the north-west corner.

The height of the canopy of the plot, up to about 20 m: the round crowns of the trees, smoother than the highest points of each cell, and the road at the north-west corner.
:::

## Reference

```{include} ../generated/Las2RasterInterpolator.md
```
