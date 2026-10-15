# RasterDiff

Subtracts a raster from another, cell by cell. The typical use is the canopy height model, the height of the vegetation over the ground, from the surface model of a lidar survey minus its terrain model. The surface often dips a bit under the terrain because of the noise of the two models, so the negative differences can be set to 0, and the low vegetation and the ground can be left out with a threshold.

It is a shortcut for the most common expression of [Mapcalc](mapcalc.md), which it runs inside: Mapcalc is the tool for any other combination of rasters.

## Details

```{include} ../generated/RasterDiff_details.md
```

## Parameters

```{include} ../generated/RasterDiff.md
```

## Example

The example runs on the sample data of the manual, in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. It takes the surface of the lidar plot interpolated by [SurfaceInterpolator](surfaceinterpolator.md) minus the terrain model of the plot:

```{literalinclude} ../examples/hm/rasterdiff.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterDiff_chm_777.png
:alt: The canopy height model of the lidar plot: the crowns of the trees above 2 m, the rest novalue.

The canopy height model of the lidar plot: the crowns of the trees above 2 m, the ground and the low vegetation left out.
:::
