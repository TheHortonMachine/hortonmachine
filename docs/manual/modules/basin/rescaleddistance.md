# RescaledDistance

The distance of each cell from the outlet, along the drainage directions, gives the width function of the basin: how many cells are at each distance. Divided by a velocity, it tells when the water of each cell reaches the outlet.

The water is however much faster in the channels than on the hillslopes. The rescaled distance multiplies the hillslope part of each path by the ratio r between the channel and the hillslope velocities, so that the whole distance becomes proportional to the travel time at the channel velocity. Low ratios (5 to 20) describe the fast surface runoff, high ratios (50 to 200) the slow subsurface flow: [Peakflow](../hydro-geomorphology/peakflow.md) uses one of each.

The outlets are found from the maps: they are the network cells that drain out of the map, or into no-data cells. With an elevation map, the distances are measured in 3D.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/rescaleddistance.groovy
:language: groovy
```

:::{figure} ../../images/modules/RescaledDistance_rescaled.png
:alt: The rescaled distance from the outlet of the basin, in m, with a ratio of 10 between the channel and the hillslope velocities: the hillslopes far from the network get the largest values.

The rescaled distance from the outlet of the basin, in m, with a ratio of 10 between the channel and the hillslope velocities: the hillslopes far from the network get the largest values.
:::

## Reference

```{include} ../generated/RescaledDistance.md
```
