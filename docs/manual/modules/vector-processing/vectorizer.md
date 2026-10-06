# Vectorizer

Converts a raster into a vector of polygons, one for each area of cells with the same value, with the value in a field. It is the way to bring the results of the raster analyses, as basins, classes or hazard zones, into the vector world, to measure them, combine them with other vectors or edit them. A threshold on the number of cells discards the smallest polygons, and the holes can be removed. The no-data cells are not converted.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/vectorizer.groovy
:language: groovy
```

:::{figure} ../../images/modules/Vectorizer_outlet_basin.png
:alt: The outline of the polygon of the basin of Wateroutlet.

The outline of the polygon of the basin of Wateroutlet.
:::

## Reference

```{include} ../generated/Vectorizer.md
```
