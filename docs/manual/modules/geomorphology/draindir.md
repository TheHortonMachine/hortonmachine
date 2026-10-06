# DrainDir

Corrects the D8 drainage directions with path-based methods, which choose among the possible directions the one that keeps the flow path closest to the direction of steepest descent, accumulating the deviations along the path. It also computes the total contributing area of each cell: the number of cells draining into it, itself included, the input of [ExtractNetwork](../network/extractnetwork.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/draindir.groovy
:language: groovy
```

:::{figure} ../../images/modules/DrainDir_drain.png
:alt: The drainage directions of the sample area, corrected by DrainDir.

The drainage directions of the sample area, corrected by DrainDir: compared with the ones of [FlowDirections](flowdirections.md), the long straight bands of the same direction are broken into the directions that follow the steepest descent along the paths.
:::

:::{figure} ../../images/modules/DrainDir_tca.png
:alt: The total contributing area of the sample area, in cells.

The total contributing area of the sample area, in cells: the network emerges as the cells that drain the largest areas.
:::

## Reference

```{include} ../generated/DrainDir.md
```
