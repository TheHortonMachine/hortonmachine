# TopIndex

The topographic index, ln(a/tan β), expresses how much a cell tends to saturate: cells with a large contributing area a and a gentle slope β, typically the concave areas near the network, saturate first. It depends only on the morphology, and is the base of the TOPMODEL family of models, where the cells with the same index behave the same way.

Here the contributing area is the total contributing area in cells, from [DrainDir](../geomorphology/draindir.md), and the slope is the one along the drainage directions, from [Slope](../geomorphology/slope.md).

Where the slope is 0, the index is not defined and the cell is left as no-data. These cells, with a very gentle slope, are the most prone to saturate: in models like [Peakflow](../hydro-geomorphology/peakflow.md) they are usually set to the highest value of the map, with the [Map Calculator](../../apps/mapcalc.md).

```{include} ../generated/TopIndex.md
```
