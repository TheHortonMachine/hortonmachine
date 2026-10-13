# WatershedAlgorithm

WatershedAlgorithm divides a raster into basins, flooding it from its minima as water filling a landscape. Turned upside down, a canopy height model floods from the tops of the trees, and each basin is then a crown: its area and shape, around the tops found by [RasterMaximaFinder](rastermaximafinder.md). The output is the number of the basin of each cell, or the lines between the basins.

## Example

The crowns of the canopy height model of [Las2RasterInterpolator](las2rasterinterpolator.md): first without the ground and the low vegetation, set to no value with [Mapcalc](../raster-processing/mapcalc.md), then smoothed with [KernelDensity](../raster-processing/kerneldensity.md), so that the small bumps of the canopy do not make crowns of their own:

```{literalinclude} ../examples/hm/watershedalgorithm.groovy
:language: groovy
```

:::{figure} ../../images/modules/WatershedAlgorithm_crowns.png
:alt: The 107 crowns, each in its own color, over the hillshade of the elevation model: round patches of a few meters where the trees are, divided by thin lines without value, and the open ground left out.

The 107 crowns, each in its own color, over the hillshade of the elevation model: round patches of a few meters where the trees are, divided by thin lines without value, and the open ground left out.
:::

On the canopy model as it is, noisy from cell to cell, the same flood gives more than 400 small pieces.

## Reference

```{include} ../generated/WatershedAlgorithm.md
```
