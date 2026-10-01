# Gradient

The gradient is the steepest slope of the terrain in each cell, in any direction, computed from the 3x3 window of elevations around the cell. Three formulas are available:

- **Finite Differences**: from the four neighbours along the rows and the columns;
- **Horn**: from all the eight neighbours, weighting more the closest ones;
- **Evans**: from all the eight neighbours, with the same weight.

Horn and Evans smooth the noise of the elevation better than the finite differences. The gradient differs from the [Slope](slope.md), which is measured along the drainage direction.

```{include} ../generated/Gradient.md
```
