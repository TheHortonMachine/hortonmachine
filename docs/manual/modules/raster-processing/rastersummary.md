# RasterSummary

Calculates the basic statistics of the values of a raster: minimum, maximum, mean, standard deviation, range and sum, and optionally their histogram, with the number of cells for each interval of values. The no-data cells are not considered.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/rastersummary.groovy
:language: groovy
```

For the sample elevation model, the minimum is 846 m, the maximum 2150 m, the mean 1575 m and the standard deviation 278 m.

## Reference

```{include} ../generated/RasterSummary.md
```
