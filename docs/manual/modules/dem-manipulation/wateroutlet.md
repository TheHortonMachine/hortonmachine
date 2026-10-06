# Wateroutlet

Extracts the basin of an outlet: the cells that drain into it along the drainage directions. It is the port of the r.water.outlet module of GRASS GIS.

The outlet must be on the cell where the water really passes, usually a cell of the network: a point next to the channel gives the tiny basin of a hillslope. [ExtractBasin](extractbasin.md) does the same, and can also snap the outlet to a network, smooth the basin and write it as a vector.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/wateroutlet.groovy
:language: groovy
```

:::{figure} ../../images/modules/Wateroutlet_outlet_basin.png
:alt: The basin of an outlet in the southeast of the sample area: 1.4 km².

The basin of an outlet in the southeast of the sample area: 1.4 km².
:::

## Reference

```{include} ../generated/Wateroutlet.md
```
