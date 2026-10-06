# PitfillerBarnes

PitfillerBarnes fills the depressions of the elevation model, as [Pitfiller](pitfiller.md), with the Priority-Flood algorithm of Barnes (2016), which processes the map in tiles in parallel. It is much faster on large elevation models, and with the large file mode it works file to file, for maps that don't fit in memory.

In the same run it can also compute the D8 drainage directions of the filled map, flats included, their total contributing areas and the network above a threshold: the first steps of a hydrological analysis, on maps too large for the single modules.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/pitfillerbarnes.groovy
:language: groovy
```

:::{figure} ../../images/modules/PitfillerBarnes_pit_barnes.png
:alt: The depitted elevation of the sample area, computed by PitfillerBarnes.

The depitted elevation of the sample area, computed by PitfillerBarnes.
:::

:::{figure} ../../images/modules/PitfillerBarnes_net_barnes.png
:alt: The network extracted in the same run, with a threshold of 100 cells, on the D8 directions of the filled map.

The network extracted in the same run, with a threshold of 100 cells, on the D8 directions of the filled map.
:::

## Reference

```{include} ../generated/PitfillerBarnes.md
```
