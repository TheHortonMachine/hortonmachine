# KernelDensity

Estimates the density of the values of a raster: for each cell, it sums the values of the cells within a radius, each weighted by a kernel that decreases with the distance from the cell. Applied to a map of presences, with 1 where something is and 0 elsewhere, it gives how concentrated they are around each cell: the density of the channels, of the landslides, of the trees found by a lidar survey.

The kernels are the ones of the Jaitools project, and the Epanechnikov one is the default. The weights are not normalized: the result is a weighted count, whose scale depends on the kernel and on the radius, so the maps are good to compare areas with each other, computed with the same settings.

The cells that have no value in the input have no value in the output, as the ones closer to the border than the radius. For a density everywhere, the cells without presence must be 0, not novalue, as in the example.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. [Mapcalc](mapcalc.md) turns the network of [ExtractNetwork](../network/extractnetwork.md) into presences, then the module sums them within 100 m:

```{literalinclude} ../examples/hm/kerneldensity.groovy
:language: groovy
```

:::{figure} ../../images/modules/KernelDensity_channels_density.png
:alt: The density of the channels within 100 m: highest where many channels converge, as on the fan in the north east. The cells with no channel within 100 m are zero and not colored; the band of 100 m along the border has no value.

The density of the channels within 100 m: highest where many channels converge, as on the fan in the north east. The cells with no channel within 100 m are zero and not colored; the band of 100 m along the border has no value.
:::

## Reference

```{include} ../generated/KernelDensity.md
```
