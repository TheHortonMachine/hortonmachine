# TopIndex

The topographic index, ln(a/tan β), expresses how much a cell tends to saturate: cells with a large contributing area a and a gentle slope β, typically the concave areas near the network, saturate first. It depends only on the morphology, and is the base of the TOPMODEL family of models, where the cells with the same index behave the same way.

Here the contributing area is the total contributing area in cells, from [DrainDir](../geomorphology/draindir.md), and the slope is the one along the drainage directions, from [Slope](../geomorphology/slope.md).

Where the slope is 0, the index is not defined and the cell is left as no-data. These cells, with a very gentle slope, are the most prone to saturate: in models like [Peakflow](../hydro-geomorphology/peakflow.md) they are usually set to the highest value of the map, with the [Map Calculator](../../apps/mapcalc.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/topindex.groovy
:language: groovy
```

:::{figure} ../../images/modules/TopIndex_topindex.png
:alt: The topographic index of the sample area: the highest values are along the network, where the soil saturates first.

The topographic index of the sample area: the highest values are along the network, where the soil saturates first.
:::

## Reference

```{include} ../generated/TopIndex.md
```
