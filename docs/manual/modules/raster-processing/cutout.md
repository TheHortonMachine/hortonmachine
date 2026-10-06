# CutOut

Keeps only the cells of a raster inside a mask, or within a range of values, setting all the others to no-data. With `doInverse` the mask is inverted: the cells inside it are removed.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/cutout.groovy
:language: groovy
```

:::{figure} ../../images/modules/CutOut_drain_basin.png
:alt: The drainage directions of DrainDir, kept only inside the basin of ExtractBasin.

The drainage directions of DrainDir, kept only inside the basin of ExtractBasin.
:::

## Reference

```{include} ../generated/CutOut.md
```
