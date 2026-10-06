# Curvatures

The curvatures describe the shape of the terrain around each cell, and how it steers the flow of the water:

- the **profile curvature**, along the steepest slope, tells whether the flow accelerates or slows down: positive where the slope decreases downhill (concave), as at the foot of the slopes, negative where it increases (convex), as at the edge of the terraces;
- the **planar curvature**, of the contour lines, tells whether the flow converges or diverges: positive in the hollows and the valleys, where the water concentrates, negative on the noses and the ridges, where it spreads;
- the **tangential curvature** is the planar curvature multiplied by the sine of the slope angle: it tells the same, but weighs less the gentle slopes, where the convergence matters less.

The curvatures are computed from the first and second derivatives of the elevation, estimated by finite differences on the 3x3 window around each cell. They are sensitive to the noise of the elevation: on detailed elevation models, smoothing the elevation first, or using a coarser resolution, gives more readable maps.

The planar curvature is the input of [Ab](ab.md), which estimates from it the contour length of each cell.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/curvatures.groovy
:language: groovy
```

:::{figure} ../../images/modules/Curvatures_plan.png
:alt: The planar curvature of the sample area: positive, in red, in the channels and the hollows where the flow converges, negative on the ridges. The colors span from the 2nd to the 98th percentile of the values.

The planar curvature of the sample area: positive, in red, in the channels and the hollows where the flow converges, negative on the ridges. The colors span from the 2nd to the 98th percentile of the values.
:::

:::{figure} ../../images/modules/Curvatures_prof.png
:alt: The profile curvature of the sample area: positive where the slope decreases downhill, negative where it increases. The colors span from the 2nd to the 98th percentile of the values.

The profile curvature of the sample area: positive where the slope decreases downhill, negative where it increases. The colors span from the 2nd to the 98th percentile of the values.
:::

:::{figure} ../../images/modules/Curvatures_tang.png
:alt: The tangential curvature of the sample area: the planar curvature weighted by the sine of the slope angle, with smaller values than the planar one, about half, on these steep slopes. The colors span from the 2nd to the 98th percentile of the values.

The tangential curvature of the sample area: the planar curvature weighted by the sine of the slope angle, with smaller values than the planar one, about half, on these steep slopes. The colors span from the 2nd to the 98th percentile of the values.
:::

## Reference

```{include} ../generated/Curvatures.md
```
