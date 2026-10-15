# Las2BivariateRasterMosaic

Las2BivariateRasterMosaic converts a lidar survey, as large as a region, into a mosaic of raster tiles. The value of each cell comes from a quadratic surface fitted to the points around it: a smooth surface model, which keeps the slopes and the shape of the crowns, or a map of the intensity. The tiles can be processed in parallel, and the mosaic opens in a GIS as a single raster.

## Details

```{include} ../generated/Las2BivariateRasterMosaic_details.md
```

## Parameters

```{include} ../generated/Las2BivariateRasterMosaic.md
```

## Example

The surface of the first returns of the sample plot, in tiles of 100 m: the plot fits in the first one, `tile_0_0.tiff`:

```{literalinclude} ../examples/hm/las2bivariaterastermosaic.groovy
:language: groovy
```

:::{figure} ../../images/modules/Las2BivariateRasterMosaic_tile_0_0.png
:alt: The surface of the first returns of the plot, from 1899 to 1949 m: the crowns of the trees over the ground, smooth and without holes.

The surface of the first returns of the plot, from 1899 to 1949 m: the crowns of the trees over the ground, smooth and without holes.
:::
