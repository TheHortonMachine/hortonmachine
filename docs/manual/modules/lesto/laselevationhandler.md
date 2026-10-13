# LasElevationHandler

LasElevationHandler normalizes a LAS file over an elevation model: the elevation of the ground under each point is subtracted from it, and the points get their height over the ground instead of their elevation. With `doAdd` it does the opposite, bringing the heights of a normalized file back to elevations.

## Example

The heights over the ground of the points of the sample survey, converted to a vector by [LasConverter](../vector-processing/lasconverter.md) to see them on a map:

```{literalinclude} ../examples/hm/laselevationhandler.groovy
:language: groovy
```

:::{figure} ../../images/modules/LasElevationHandler_las_heights_elev.png
:alt: The normalized points colored by their height over the ground, from -0.3 m, the ground points just under the elevation model, in white to 21.6 m in dark green: the crowns of the trees stand out over the ground, which is white everywhere, also along the slope of the plot.

The normalized points colored by their height over the ground, from -0.3 m, the ground points just under the elevation model, in white to 21.6 m in dark green: the crowns of the trees stand out over the ground, which is white everywhere, also along the slope of the plot.
:::

The 8 points just outside of the elevation model are left out.

## Reference

```{include} ../generated/LasElevationHandler.md
```
