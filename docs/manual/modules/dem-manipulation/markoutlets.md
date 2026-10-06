# MarkOutlets

Marks the outlets on a map of drainage directions with the conventional value 10: the cells that drain out of the map, or into no-data cells, for example after the map was cut to a basin with [CutOut](../raster-processing/cutout.md). Several modules recognize the outlet by this value.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/markoutlets.groovy
:language: groovy
```

:::{figure} ../../images/modules/Markoutlets_drain_marked.png
:alt: The drainage directions of the basin of ExtractBasin, with the outlet marked with 10: the single red cell at the southwest end of the basin.

The drainage directions of the basin of ExtractBasin, with the outlet marked with 10: the single red cell at the southwest end of the basin.
:::

## Reference

```{include} ../generated/Markoutlets.md
```
