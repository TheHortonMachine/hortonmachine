# FlowDirections

Computes where the water of each cell flows. The directions are coded with the numbers of the HortonMachine, counterclockwise starting from east:

| | | |
|---|---|---|
| 4 | 3 | 2 |
| 5 | cell | 1 |
| 6 | 7 | 8 |

The D8 method allows only these eight directions, which on smooth slopes leads to straight parallel flow paths that deviate from the real direction of steepest descent: [DrainDir](draindir.md) corrects them.

```{include} ../generated/FlowDirections.md
```
