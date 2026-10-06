# NetNumbering

Gives a number to each link of the network, the stretch of channel between two confluences, and extracts the sub-basin draining into each link. With a desired area, the links are aggregated into sub-basins of about that size: these are the hydrological response units of the semi-distributed models, like the ones of the [ERM workflow](../geoframe/erm/index.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/netnumbering.groovy
:language: groovy
```

:::{figure} ../../images/modules/NetNumbering_subbasins.png
:alt: The sub-basins of the basin of ExtractBasin, one for each link of its network.

The sub-basins of the basin of ExtractBasin, one for each link of its network.
:::

## Reference

```{include} ../generated/NetNumbering.md
```
