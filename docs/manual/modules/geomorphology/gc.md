# Gc

Classifies the cells of a basin by their geomorphological role, combining three maps: the slope, the river network and the topographic classes of [Tc](../hillslope/tc.md). The steep sites, as cliffs and rock walls, and the channels are taken out first; the remaining cells keep their topographic class, which tells hillslopes, valleys and planar sites apart by their curvatures.

The input maps come from [Slope](slope.md), [ExtractNetwork](../network/extractnetwork.md) and the 9 classes of Tc. The steepness threshold is a tangent of the slope, as in the output of Slope: 1 is 45 degrees.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/gc.groovy
:language: groovy
```

:::{figure} ../../images/modules/Gc_gc_aggregated.png
:alt: The aggregated geomorphological classes of the sample elevation model: mostly planar sites, crossed by hillslopes, valleys and channels.

The aggregated geomorphological classes of the sample elevation model: mostly planar sites, crossed by hillslopes, valleys and channels.
:::

## Reference

```{include} ../generated/Gc.md
```
