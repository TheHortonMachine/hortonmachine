# DebrisFlow

DebrisFlow estimates the area flooded by a debris flow of a given volume, and the thickness of its deposit, with a Monte Carlo simulation. Many random paths start from a point, usually the apex of a fan, and move downslope: each step goes to a lower neighbour, chosen with a probability proportional to its slope. The paths are repeated until the cells crossed cover the area expected for the volume,

A = mobility coefficient · V<sup>2/3</sup>

or the maximum number of paths is reached. The probability of each cell is the fraction of the paths that crossed it. The deposit has an average thickness of deposit coefficient · V<sup>1/3</sup>, thicker where the probability is higher.

The paths stop at the first cell without lower neighbours, so the elevation should be depitted, for example with [Pitfiller](../dem-manipulation/pitfiller.md). The method is meant for fans, where the paths spread: in a narrow valley they all follow the channel.

:::{warning}
The module is experimental. The random paths change at every run, so the results differ slightly each time, and the probability can slightly exceed 1.
:::

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/debrisflow.groovy
:language: groovy
```

:::{figure} ../../images/modules/DebrisFlow_debris_probability.png
:alt: The probability of a debris flow of 50000 m³ starting where a channel opens into the main valley of the sample area: in the narrow valley all the paths follow the channel down to the border of the map.

The probability of a debris flow of 50000 m³ starting where a channel opens into the main valley of the sample area: in the narrow valley all the paths follow the channel down to the border of the map.
:::

:::{figure} ../../images/modules/DebrisFlow_debris_deposit.png
:alt: The deposit thickness of the same debris flow, in meters.

The deposit thickness of the same debris flow, in meters.
:::

## Reference

```{include} ../generated/DebrisFlow.md
```
