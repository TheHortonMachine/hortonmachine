# Tca

The total contributing area of a cell is the number of cells that drain into it, itself included: multiplied by the area of a cell, it is the area of the basin closed at that cell. It grows along the paths of the water, and the cells with the largest values draw the network.

Tca computes it from any map of drainage directions. [DrainDir](draindir.md) already gives the contributing areas of the directions it corrects: Tca serves the other directions, as the D8 ones of [FlowDirections](flowdirections.md) or those edited by hand. With D8 directions on smooth slopes the water follows parallel straight lines, so the contributing areas grow in long straight stripes, unlike the ones of DrainDir.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/tca.groovy
:language: groovy
```

:::{figure} ../../images/modules/Tca_tca_d8.png
:alt: The total contributing area of the D8 directions of FlowDirections, in cells: on the smooth slopes the water flows in parallel straight lines.

The total contributing area of the D8 directions of FlowDirections, in cells: on the smooth slopes the water flows in parallel straight lines.
:::

## Reference

```{include} ../generated/Tca.md
```
