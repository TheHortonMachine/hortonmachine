# DistanceToOutlet

Calculates the distance of each cell from the outlet, along the path the water follows: in meters, also in 3D with the elevation, or as the number of cells crossed. The distances give the width function of the basin, how many cells are at each distance from the outlet, and with a velocity the time each cell needs to reach it.

The outlet must be marked with the value 10 in the drainage directions, for example with [MarkOutlets](../dem-manipulation/markoutlets.md): the paths that leave the map elsewhere get 0. [RescaledDistance](../basin/rescaleddistance.md) computes the same distance, with the hillslopes slowed down.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/distancetooutlet.groovy
:language: groovy
```

:::{figure} ../../images/modules/DistanceToOutlet_distance.png
:alt: The distance from the outlet of the basin of ExtractBasin, in meters, measured in 3D.

The distance from the outlet of the basin of ExtractBasin, in meters, measured in 3D.
:::

## Reference

```{include} ../generated/DistanceToOutlet.md
```
