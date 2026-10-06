# SumDownStream

Sums the values of a map downstream, along the drainage directions: each cell gets the sum of the values of all the cells upstream of it, itself included. With a map of ones it gives the contributing area; with other maps, how much of something the water collects on its way: the unstable areas, the sources of sediment, the volume of water of each cell.

The cells with values outside the thresholds are not summed, and stop the paths that reach them. The paths end at the outlets marked with the value 10 in the drainage directions, for example with [MarkOutlets](../dem-manipulation/markoutlets.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/sumdownstream.groovy
:language: groovy
```

:::{figure} ../../images/modules/SumDownStream_unstable_upstream.png
:alt: The number of cells upstream of each cell of the basin of ExtractBasin that are unstable in the map of Shalstab, made of ones and zeros with the Mapcalc module.

The number of cells upstream of each cell of the basin of ExtractBasin that are unstable in the map of Shalstab, made of ones and zeros with the Mapcalc module.
:::

## Reference

```{include} ../generated/SumDownStream.md
```
