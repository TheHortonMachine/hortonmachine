# DebrisTriggerCnr

Debris flows start in the steep channels, where enough water collects on enough slope. DebrisTriggerCnr finds these points along the network with the method of the CNR: a network cell is a trigger if its gradient is steeper than

S = 0.32 · A<sup>-0.2</sup>

with A the contributing area in km², so that the channels draining larger areas trigger with gentler slopes. The channels steeper than the gradient threshold (38 degrees) are considered rock, without material to mobilize, and those draining more than the area threshold (10 km²) are rivers rather than torrents.

The triggers are the input of the runout models, as [DebrisVandre](debrisvandre.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/debristriggercnr.groovy
:language: groovy
```

:::{figure} ../../images/modules/DebrisTriggerCnr_triggers.png
:alt: The trigger points of the sample area, with the value of their threshold gradient. The cells at the border of the map, where the gradient is not reliable, also appear as triggers.

The trigger points of the sample area, with the value of their threshold gradient. The cells at the border of the map, where the gradient is not reliable, also appear as triggers.
:::

## Reference

```{include} ../generated/DebrisTriggerCnr.md
```
