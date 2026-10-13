# Lesto

The modules of LESTO, the Lidar Empowered Sciences Toolbox Opensource, for the point clouds of lidar surveys in LAS files: to look at their values and filter them, and to turn them into rasters, as the elevation model of the ground, the surface of the canopy and its height.

The examples run on the sample lidar survey of the manual, `uni_bz_plot777.las` in `docs/manual/modules/maps/data`: a forest plot of 60 x 60 m with about 33,000 points, 9 per square meter, and its elevation model `dtm_777.tif` of 0.5 m. [LasConverter](../vector-processing/lasconverter.md) shows the points on a map.

```{toctree}
:maxdepth: 1
:caption: Filter

lasheightdistribution
lashistogram
lasmerger
lassplitter
lasthresholder
```

```{toctree}
:maxdepth: 1
:caption: Raster

adaptivetinfilter
las2bivariaterastermosaic
las2rasterinterpolator
lasonrastermapper
lastriangulation2dsm
```
