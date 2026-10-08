# NetDiff

Calculates, for each stretch of the river network, the difference of a quantity between its upstream and downstream ends, and assigns it to all the cells of the stretch. With the elevation, it gives the drop of each link or branch, the base of the slope of the channels and of the stream power along the network.

The stretches are defined by a map of the network with a value for each of them, as the links of [NetNumbering](netnumbering.md). The drainage directions must have the outlet marked with the value 10, for example with [MarkOutlets](../dem-manipulation/markoutlets.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/netdiff.groovy
:language: groovy
```

:::{figure} ../../images/modules/NetDiff_netdiff.png
:alt: The elevation drop of each link of the network of the basin of ExtractBasin, in meters: the steep source links drop the most.

The elevation drop of each link of the network of the basin of ExtractBasin, in meters: the steep source links drop the most.
:::

## Reference

```{include} ../generated/NetDiff.md
```
