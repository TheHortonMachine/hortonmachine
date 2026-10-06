# HackLength

The Hack length of a cell is the length of the main stream upstream of it, from its source: going upstream, at each confluence the main stream is the branch that drains the largest area. Hack (1957) found that in a basin the length of the main stream grows as a power of the contributing area, L ∝ A<sup>h</sup>, with h about 0.6, a law that holds in basins of any size.

The lengths are measured along the drainage directions, also in 3D with the elevation. The outlet must be marked with the value 10, for example with [MarkOutlets](../dem-manipulation/markoutlets.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/hacklength.groovy
:language: groovy
```

:::{figure} ../../images/modules/HackLength_hacklength.png
:alt: The Hack length of the basin of ExtractBasin, in meters: it grows along the main streams, up to the length of the longest path at the outlet.

The Hack length of the basin of ExtractBasin, in meters: it grows along the main streams, up to the length of the longest path at the outlet.
:::

## Reference

```{include} ../generated/HackLength.md
```
