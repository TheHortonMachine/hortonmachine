# RasterGenerator

Creates a new raster from nothing, on a grid given by its bounds, cell size and reference system, with a constant value or with random values. Its use is to make a grid where there is no data yet, for the modules that take the grid from a raster: [Mapcalc](mapcalc.md), [SurfaceInterpolator](surfaceinterpolator.md) and the rasterizers, as [ScanLineRasterizer](scanlinerasterizer.md). The random values, with a seed to get the same ones at every run, give test data or the noise for a simulation.

## Details

```{include} ../generated/RasterGenerator_details.md
```

## Parameters

```{include} ../generated/RasterGenerator.md
```

## Example

The example makes a grid of 10 m on an area without data, and fills it with a synthetic terrain through Mapcalc, with a shorter version of the fractal terrain of the [Mapcalc](mapcalc.md) page:

```{literalinclude} ../examples/hm/rastergenerator.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterGenerator_synthetic_terrain.png
:alt: A synthetic terrain of hills, computed by Mapcalc on the grid of RasterGenerator.

The synthetic terrain computed by Mapcalc on the new grid.
:::
