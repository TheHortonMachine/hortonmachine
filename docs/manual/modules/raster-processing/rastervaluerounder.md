# RasterValueRounder

Rounds the values of a raster to a number of decimals, given by a pattern. It removes the meaningless decimals of the results of interpolations and conversions, for the readability of the values and the size of the compressed files.

## Details

```{include} ../generated/RasterValueRounder_details.md
```

## Parameters

```{include} ../generated/RasterValueRounder.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/rastervaluerounder.groovy
:language: groovy
```

The rounded elevation is compared with the original one in the example of [RasterCompare](rastercompare.md).
