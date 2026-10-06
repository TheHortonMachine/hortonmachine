# Tca3d

The total contributing area usually counts the cells draining into each cell, so it measures the plan area. On steep terrain the real surface of a cell is larger than its plan area: on a slope of 45 degrees, about 1.4 times. Tca3d sums the real, three-dimensional surface of the cells, computed from the triangles between each cell and its neighbours, so that the areas grow more on the steep slopes.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/tca3d.groovy
:language: groovy
```

:::{figure} ../../images/modules/Tca3d_tca3d.png
:alt: The three-dimensional total contributing area of the sample area, in m².

The three-dimensional total contributing area of the sample area, in m².
:::

## Reference

```{include} ../generated/Tca3d.md
```
