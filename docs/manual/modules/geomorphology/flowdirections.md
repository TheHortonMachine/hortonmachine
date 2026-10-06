# FlowDirections

Computes where the water of each cell flows. The directions are coded with the numbers of the HortonMachine, counterclockwise starting from east:

| | | |
|---|---|---|
| 4 | 3 | 2 |
| 5 | cell | 1 |
| 6 | 7 | 8 |

The D8 method allows only these eight directions, which on smooth slopes leads to straight parallel flow paths that deviate from the real direction of steepest descent: [DrainDir](draindir.md) corrects them.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/flowdirections.groovy
:language: groovy
```

:::{figure} ../../images/modules/FlowDirections_flow.png
:alt: The D8 drainage directions of the sample area, with the codes of the HortonMachine.

The D8 drainage directions of the sample area, with the codes of the HortonMachine.
:::

## Reference

```{include} ../generated/FlowDirections.md
```
