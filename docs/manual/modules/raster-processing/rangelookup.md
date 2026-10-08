# RangeLookup

Reclassifies a raster by ranges of values: each cell gets the class of the range its value falls in. It turns continuous maps into classes, as elevation belts, slope classes for a hazard map or the bins of a legend, and the classes can then be combined with [Mapcalc](mapcalc.md) or converted into polygons with [Vectorizer](../vector-processing/vectorizer.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/rangelookup.groovy
:language: groovy
```

:::{figure} ../../images/modules/RangeLookup_elevation_belts.png
:alt: The three elevation belts of the sample elevation model.

The three elevation belts of the sample elevation model.
:::

## Reference

```{include} ../generated/RangeLookup.md
```
