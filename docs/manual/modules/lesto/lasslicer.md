# LasSlicer

LasSlicer cuts a point cloud into horizontal slices, at regular elevations, or at regular heights over the ground with an elevation model, and writes each slice as a raster of the cells with its points, or as a chart of them. A slice at a given height over the ground shows what is there: the stems near the ground, the crowns higher up.

## Details

```{include} ../generated/LasSlicer_details.md
```

## Parameters

```{include} ../generated/LasSlicer.md
```

## Example

Slices of 1 m every 5 m of height over the ground of the sample survey, as rasters of 0.5 m, in the folder `las_slices`:

```{literalinclude} ../examples/hm/lasslicer.groovy
:language: groovy
```

:::{figure} ../../images/modules/LasSlicer_slice_15.png
:alt: The slice between 14.5 and 15.5 m over the ground, in red over the hillshade of the elevation model: rings and patches of a few meters, the crowns of the trees taller than 15 m, mostly in the north of the plot.

The slice between 14.5 and 15.5 m over the ground, in red over the hillshade of the elevation model: rings and patches of a few meters, the crowns of the trees taller than 15 m, mostly in the north of the plot.
:::

The slices are `slice_5.tif`, `slice_10.tif`, `slice_15.tif` and `slice_20.tif`.
