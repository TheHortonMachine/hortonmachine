# CannyEdgeDetector

Finds the edges of a raster: the thin lines along which its values change most rapidly. It is the classic method of Canny (1986) for images, applied to the values of any single band raster, so the edges are lines of rapid change of whatever the raster holds:

- on a slope map, the **breaks of slope**: the edges of the terraces and of the flat valley floors, the scarps, the crowns of the landslides;
- on an elevation model, the lines of steepest slope, which are rarely what is wanted: run it on the slope instead;
- on an image or an index, as a vegetation index, the borders of the fields, of the forest, of the water.

The result has 1 on the edge cells and no-data elsewhere, ready to be drawn over other maps or turned into lines.

The two thresholds are fractions of the largest gradient of the map, the strongest change it contains, so the same values work on rasters of any unit. This also means that the edges found depend on the whole map: where a few areas change much more than the rest, as the rock faces of a mountain area on a slope map, they set the largest gradient, and the ordinary breaks elsewhere fall below the thresholds. Lower thresholds find them, and more, weaker, edges; the gaussian smoothing, `pRadiusgauss`, sets how small the changes can be: larger values ignore the details and the noise, and keep only the main edges. The examples below show both.

Where three edges meet, as where a weaker edge reaches a stronger one, the weaker one can stop a few cells before the junction: it is a known limit of the method.

## Details

```{include} ../generated/CannyEdgeDetector_details.md
```

## Parameters

```{include} ../generated/CannyEdgeDetector.md
```

## Examples

### Terraces

The first example builds a synthetic terrain, to show what the module finds: with [RasterGenerator](rastergenerator.md) and [Mapcalc](mapcalc.md) a slope of 1.5 x 1.2 km, rising towards north, with four terraces. The treads are gentle, with a slope of 5 %, the risers are steep and 40 m wide, of 40, 40, 15 and 6 m from south to north, so steeper the higher they are. [Gradient](../geomorphology/gradient.md) computes the slope, and the module finds its breaks twice, with the default thresholds and with lower ones:

```{literalinclude} ../examples/hm/cannyedgedetector_terraces.groovy
:language: groovy
```

:::{figure} ../../images/modules/CannyEdgeDetector_terraces_gradient.png
:alt: The gradient of the synthetic terraces: 0.05 on the treads, up to 1.44 on the risers of 40 m in the south, 0.57 on the one of 15 m and 0.26 on the one of 6 m in the north.

The gradient of the synthetic terraces: 0.05 on the treads, up to 1.44 on the risers of 40 m in the south, 0.57 on the one of 15 m and 0.26 on the one of 6 m in the north.
:::

Each riser has two breaks of slope, where the tread ends and the riser starts, and at its foot. The breaks of the risers of 40 m, where the gradient changes from 0.05 to 1.44, set the largest gradient of the gradient map; the ones of the riser of 15 m are about 0.37 of it, above the high threshold of 0.2, the ones of the riser of 6 m about 0.15, below it, so they are left out:

:::{figure} ../../images/modules/CannyEdgeDetector_terraces_breaks.png
:alt: The breaks of slope with the default thresholds, low 0.1 and high 0.2, in red: a line at the top and one at the foot of the three lower risers. The riser of 6 m, in the north, has no line.

The breaks of slope with the default thresholds, low 0.1 and high 0.2, in red: a line at the top and one at the foot of the three lower risers. The riser of 6 m, in the north, has no line.
:::

With thresholds of 0.03 and 0.06 the breaks of the riser of 6 m are found too, while the noise of a few centimeters on the treads stays below them:

:::{figure} ../../images/modules/CannyEdgeDetector_terraces_breaks_low.png
:alt: The breaks of slope with the thresholds 0.03 and 0.06: the two lines of every riser, also the smallest one in the north.

The breaks of slope with the thresholds 0.03 and 0.06: the two lines of every riser, also the smallest one in the north.
:::

### Breaks of slope of the sample area

The second example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones, and finds the breaks of slope on the gradient map of [Gradient](../geomorphology/gradient.md). The rock faces in the east change slope much more than the rest of the area: with the default thresholds only them and the rims of the deepest gorges are found. Lower thresholds find the breaks of the open slopes too, and a stronger smoothing, of 3 cells, leaves out the many small irregularities that the lower thresholds would bring in:

```{literalinclude} ../examples/hm/cannyedgedetector.groovy
:language: groovy
```

:::{figure} ../../images/modules/CannyEdgeDetector_slope_breaks.png
:alt: The breaks of slope of the sample area, in red: the rims of the gorges of the main valleys, the banks of the channels cut in the slopes, the edges of the rock faces in the east and the breaks of the open slopes.

The breaks of slope of the sample area, in red: the rims of the gorges of the main valleys, the banks of the channels cut in the slopes, the edges of the rock faces in the east and the breaks of the open slopes.
:::
