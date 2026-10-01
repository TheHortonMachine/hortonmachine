# ErmDataPreparator

The data preparation runs, on the digital elevation model, the chain of hydro-geomorphological modules that defines the basin and its sub-basins:

1. [Pitfiller](../../dem-manipulation/pitfiller.md) removes the depressions of the DTM;
2. [FlowDirections](../../geomorphology/flowdirections.md) and [DrainDir](../../geomorphology/draindir.md) compute the drainage directions and the total contributing areas;
3. [ExtractNetwork](../../network/extractnetwork.md) extracts the river network, with the threshold `pDrainThreshold`;
4. [Skyview](../../hydro-geomorphology/skyview.md) computes the sky view factor, used later for the radiation;
5. [ExtractBasin](../../dem-manipulation/extractbasin.md) extracts the basin of the outlet, and all the rasters are cut to it with [RasterResizer](../../raster-processing/rasterresizer.md) and [CutOut](../../raster-processing/cutout.md);
6. [NetNumbering](../../network/netnumbering.md) splits the basin into sub-basins of about `pDesiredArea`;
7. [GeoframeInputsBuilder](../../hydro-geomorphology/geoframeinputsbuilder.md) writes the sub-basins, the network, their topology and the stream gauges into the GeoFrame database.

All the rasters are written to an `outputs` folder next to the DTM, each with a QGIS style, so they can be checked in QGIS. Rasters already present are reused, unless `doOverwrite` is set.

```{literalinclude} ../../examples/erm/01_data_preparation.groovy
:language: groovy
```

```{include} ../../generated/ErmDataPreparator.md
```
