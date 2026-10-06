# Gradient

The gradient is the steepest slope of the terrain in each cell, in any direction, computed from the 3x3 window of elevations around the cell. Three formulas are available:

- **Finite Differences**: from the four neighbours along the rows and the columns;
- **Horn**: from all the eight neighbours, weighting more the closest ones;
- **Evans**: from all the eight neighbours, with the same weight.

Horn and Evans smooth the noise of the elevation better than the finite differences. The gradient differs from the [Slope](slope.md), which is measured along the drainage direction.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/gradient.groovy
:language: groovy
```

:::{figure} ../../images/modules/Gradient_gradient.png
:alt: The gradient of the sample area, with the method of Horn, as the tangent of the slope angle.

The gradient of the sample area, with the method of Horn, as the tangent of the slope angle.
:::

## Reference

```{include} ../generated/Gradient.md
```
