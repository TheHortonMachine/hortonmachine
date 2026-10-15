# RasterMissingValuesFiller

Fills the holes of a raster, its no-data cells, by interpolation of the valid cells around them. It repairs the gaps of elevation models from lidar or photogrammetry, under water surfaces or dense vegetation, and the cells lost by a processing, and it can fill rasters of classes without inventing new classes.

## Details

```{include} ../generated/RasterMissingValuesFiller_details.md
```

## Parameters

```{include} ../generated/RasterMissingValuesFiller.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. It first makes holes of 60 by 60 m in the elevation with [Mapcalc](mapcalc.md), then fills them:

```{literalinclude} ../examples/hm/rastermissingvaluesfiller.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterMissingValuesFiller_dtm_holes.png
:alt: The sample elevation model with a hole of 60 by 60 m every 400 m.

The sample elevation model with a hole of 60 by 60 m every 400 m.
:::

:::{figure} ../../images/modules/RasterMissingValuesFiller_dtm_filled.png
:alt: The holes filled with a thin plate spline through their rims.

The holes filled with a thin plate spline through their rims: the mask leaves the frame of the model as it is.
:::
