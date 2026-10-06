# DebrisVandre

DebrisVandre traces the runout path of a debris flow from each trigger point, following the drainage directions, with the criteria of Burton and Bathurst (1998):

- where the slope is at least 10 degrees, the debris moves on;
- where the slope is under 4 degrees, the debris stops;
- in between, the debris slows down and travels at most the distance of the Vandre (1985) equation, W = 0.4 · ΔH, with ΔH the drop of the path above.

The modified criteria move on from 8 degrees and have no lower limit. The paths end at the outlets, which must be marked with the value 10 in the drainage directions, for example with [MarkOutlets](../dem-manipulation/markoutlets.md), or at the optional obstacles. With a map of the soil depth, the soil mobilized along the paths can also be cumulated.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/debrisvandre.groovy
:language: groovy
```

:::{figure} ../../images/modules/DebrisVandre_debris_paths.png
:alt: The runout paths from the trigger points of DebrisTriggerCnr in the sample area.

The runout paths from the trigger points of DebrisTriggerCnr in the sample area.
:::

## Reference

```{include} ../generated/DebrisVandre.md
```
