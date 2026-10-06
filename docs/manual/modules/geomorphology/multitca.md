# MultiTca

With a single drainage direction per cell, as with [DrainDir](draindir.md), all the water of a cell goes to one neighbour: correct in the channels and the hollows, where the water converges, but not on the ridges and the divergent slopes, where it spreads. MultiTca uses the [topographic classes](../hillslope/tc.md) of each cell to choose:

- on the divergent cells, classes 10 to 60, the contributing area is split among all the lower neighbours, in proportion to their drop;
- on the convergent cells, classes 70 to 90, it follows the single drainage direction.

The contributing areas spread smoothly on the slopes, and concentrate in the channels as with a single direction.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/multitca.groovy
:language: groovy
```

:::{figure} ../../images/modules/MultiTca_multitca.png
:alt: The total contributing area of the sample area, in cells, with multiple directions on the divergent cells: on the slopes the areas spread, in the channels they concentrate as with DrainDir.

The total contributing area of the sample area, in cells, with multiple directions on the divergent cells: on the slopes the areas spread, in the channels they concentrate as with DrainDir.
:::

## Reference

```{include} ../generated/MultiTca.md
```
