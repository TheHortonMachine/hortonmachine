# LasHeightDistribution

LasHeightDistribution maps the vertical structure of a forest. In each tile of a grid it looks at how the heights of the points over the ground are distributed: each peak of the distribution is a layer of the vegetation. Each cell of the elevation model is then classified as no forest, single layer, single layer of variable height, as trees of different heights side by side, or double layer, as trees over a lower layer.

## Details

```{include} ../generated/LasHeightDistribution_details.md
```

## Parameters

```{include} ../generated/LasHeightDistribution.md
```

## Example

The sample plot divided in 3 x 3 tiles of 20 m by GridsGenerator, with the categories mapped on the elevation model resampled to 2 m by [RasterResolutionResampler](../raster-processing/rasterresolutionresampler.md), so that each cell contains enough points:

```{literalinclude} ../examples/hm/lasheightdistribution.groovy
:language: groovy
```

:::{figure} ../../images/modules/LasHeightDistribution_forest_layers.png
:alt: The structure of the forest of the plot: mostly a single layer, no forest along the road at the north-west corner and in some gaps, a double layer in a few cells at the north-west and south-west corners.

The structure of the forest of the plot: mostly a single layer, no forest along the road at the north-west corner and in some gaps, a double layer in a few cells at the north-west and south-west corners.
:::
