# CurvaturesBivariate

Computes the profile and planar curvatures, the slope and the aspect of the terrain, like [Curvatures](curvatures.md), [Slope](slope.md) and [Aspect](aspect.md), but at a scale you choose. Around each cell a quadratic surface is fitted to a window of `pCells` x `pCells` cells, and its derivatives give the four maps.

The window sets the size of the forms that the maps show:

- with 3 x 3 cells the results are close to the ones of Curvatures, and they show every small form and the noise of the elevation;
- with larger windows the small forms and the noise are smoothed out, and the curvatures describe the shape of larger parts of the terrain: on a 10 m model, a window of 9 cells shows the valleys and ridges of about 90 m.

Running the module with a few window sizes is a simple multiscale analysis of the terrain: the forms that remain at the larger scales are the main ones of the landscape.

The cells closer to the border of the map, or to no-data, than half the window are left as no-data, so a larger window leaves a wider band without values.

## Details

```{include} ../generated/CurvaturesBivariate_details.md
```

## Parameters

```{include} ../generated/CurvaturesBivariate.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. It uses a window of 9 x 9 cells, 90 m, on the elevation filled by [Pitfiller](../dem-manipulation/pitfiller.md):

```{literalinclude} ../examples/hm/curvaturesbivariate.groovy
:language: groovy
```

:::{figure} ../../images/modules/CurvaturesBivariate_plan_bivariate.png
:alt: The planar curvature with a window of 90 m: positive, in red, along the valleys and the gullies, where the flow converges, negative, in yellow, on the ridges and on the rims beside the main valleys. Compared to the planar curvature of Curvatures, the cell by cell texture is gone and the forms are continuous bands, with values about three times smaller. The colors span from the 2nd to the 98th percentile of the values.

The planar curvature with a window of 90 m: positive, in red, along the valleys and the gullies, where the flow converges, negative, in yellow, on the ridges and on the rims beside the main valleys. Compared to the planar curvature of [Curvatures](curvatures.md), the cell by cell texture is gone and the forms are continuous bands, with values about three times smaller. The colors span from the 2nd to the 98th percentile of the values.
:::

:::{figure} ../../images/modules/CurvaturesBivariate_prof_bivariate.png
:alt: The profile curvature with a window of 90 m: positive, in red, where the slope decreases downhill, as along the floors of the main valleys at the foot of their slopes, negative, in yellow, where it increases, as on the rims above the steep valley sides. The colors span from the 2nd to the 98th percentile of the values.

The profile curvature with a window of 90 m: positive, in red, where the slope decreases downhill, as along the floors of the main valleys at the foot of their slopes, negative, in yellow, where it increases, as on the rims above the steep valley sides. The colors span from the 2nd to the 98th percentile of the values.
:::
