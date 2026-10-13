# RasterMaximaFinder

RasterMaximaFinder finds the tops of the trees in a canopy height model, as its local maxima: the cells higher than all the others of a window around them. The window can be of a fixed size, or as wide as the crown expected for a tree of the height of the cell, so that a tall tree is not split into several tops and two small ones are not merged into one. [GeomorphonMaximaFinder](geomorphonmaximafinder.md) finds them from the shape of the surface, [PointCloudMaximaFinder](pointcloudmaximafinder.md) from the points themselves, and [WatershedAlgorithm](watershedalgorithm.md) draws the crowns around them.

## Example

The tops of the trees of the canopy height model of [Las2RasterInterpolator](las2rasterinterpolator.md), higher than 5 m, with the windows of mixed forests:

```{literalinclude} ../examples/hm/rastermaximafinder.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterMaximaFinder_tops_raster.png
:alt: The 191 tops, red dots, each in the blue circle of its window, over the canopy height model in green: a top in almost every crown, the larger circles on the taller trees, a few crowns with two tops.

The 191 tops, red dots, each in the blue circle of its window, over the canopy height model in green: a top in almost every crown, the larger circles on the taller trees, a few crowns with two tops.
:::

## Reference

```{include} ../generated/RasterMaximaFinder.md
```
