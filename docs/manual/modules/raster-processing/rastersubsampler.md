# RasterSubsampler

Aggregates a raster to a coarser one, whose cells cover a block of input cells each, with the average, the minimum, the maximum, the sum or the most frequent value of the block. Unlike the resampling of [RasterResolutionResampler](rasterresolutionresampler.md), which reads the input at the center of the new cells, every input cell weighs on the result: a land cover keeps the class that covers most of each new cell, a map of counts keeps its total.

## Details

```{include} ../generated/RasterSubsampler_details.md
```

## Parameters

```{include} ../generated/RasterSubsampler.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/rastersubsampler.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterSubsampler_belts_100m.png
:alt: The elevation belts of RangeLookup in cells of 100 m, with the most frequent belt of each.

The elevation belts of [RangeLookup](rangelookup.md) in cells of 100 m, with the belt that covers most of each cell.
:::
