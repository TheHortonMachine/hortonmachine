# Tc

The topographic classes combine the two curvatures of the [Curvatures](../geomorphology/curvatures.md) module, each split into three classes by a threshold:

- the **tangential curvature**, of the contour lines: divergent (negative), planar or convergent (positive);
- the **profile curvature**, along the slope: convex (negative), planar or concave (positive).

The 9 classes are their combinations, and the 3 classes group them by the effect on the water: concave, where the water collects, planar, and convex, where it spreads.

| Code | Tangential | Profile | 3 classes |
|---|---|---|---|
| 10 | planar | planar | 25 planar |
| 20 | planar | convex | 35 convex |
| 30 | planar | concave | 15 concave |
| 40 | divergent | planar | 35 convex |
| 50 | divergent | convex | 35 convex |
| 60 | divergent | concave | 35 convex |
| 70 | convergent | planar | 15 concave |
| 80 | convergent | convex | 35 convex |
| 90 | convergent | concave | 15 concave |

The thresholds decide how much of the terrain is planar: with 0, almost nothing. The curvatures are noisy on detailed elevation models, so the classes are noisy too. The 9 classes are the input of [MultiTca](../geomorphology/multitca.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/tc.groovy
:language: groovy
```

:::{figure} ../../images/modules/Tc_tc9.png
:alt: The 9 topographic classes of the sample area, with thresholds of 0.01 1/m for both curvatures.

The 9 topographic classes of the sample area, with thresholds of 0.01 1/m for both curvatures.
:::

:::{figure} ../../images/modules/Tc_tc3.png
:alt: The 3 topographic classes of the sample area: the channels and the hollows are concave.

The 3 topographic classes of the sample area: the channels and the hollows are concave.
:::

## Reference

```{include} ../generated/Tc.md
```
