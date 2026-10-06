# H2cA

H2cA gives each hillslope cell the value that a map has on the network cell into which the hillslope drains. It connects the hillslopes to the properties of their channels: the number of the link, the contributing area, the elevation of the channel.

With the elevation, the difference between the elevation of each cell and that of its channel is the height above the nearest drainage (HAND): low values are the areas that a rise of the channel can flood, high values the areas well above it. All the paths must end in a channel or in an outlet marked with the value 10, for example with [MarkOutlets](../dem-manipulation/markoutlets.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/h2ca.groovy
:language: groovy
```

:::{figure} ../../images/modules/H2cA_hand.png
:alt: The height above the nearest drainage of the basin of ExtractBasin, in meters: the elevation minus the elevation of the channel each cell drains into, computed with the Mapcalc module. The colors span from the 2nd to the 98th percentile of the values.

The height above the nearest drainage of the basin of ExtractBasin, in meters: the elevation minus the elevation of the channel each cell drains into, computed with the Mapcalc module. The colors span from the 2nd to the 98th percentile of the values.
:::

## Reference

```{include} ../generated/H2cA.md
```
