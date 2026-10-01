# DrainDir

Corrects the D8 drainage directions with path-based methods, which choose among the possible directions the one that keeps the flow path closest to the direction of steepest descent, accumulating the deviations along the path. It also computes the total contributing area of each cell: the number of cells draining into it, itself included, the input of [ExtractNetwork](../network/extractnetwork.md).

```{include} ../generated/DrainDir.md
```
