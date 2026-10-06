# Magnitudo

The magnitude of a cell is the number of sources upstream of it, the sources being the cells into which nothing drains. On the drainage directions of the network alone, the sources are the channel heads, and the magnitude is the Shreve (1966) magnitude: 1 for the first links, the sum of the two magnitudes at each confluence. It measures the size of the network upstream, as the contributing area measures the size of the basin.

On the drainage directions of the whole terrain, the sources are the cells on the divides, and the magnitude grows with the contributing area.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/magnitudo.groovy
:language: groovy
```

:::{figure} ../../images/modules/Magnitudo_magnitudo.png
:alt: The Shreve magnitude of the network of the basin of ExtractBasin, from 1 at the channel heads to 24 at the outlet.

The Shreve magnitude of the network of the basin of ExtractBasin, from 1 at the channel heads to 24 at the outlet.
:::

## Reference

```{include} ../generated/Magnitudo.md
```
