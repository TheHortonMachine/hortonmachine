# LeastCostFlowDirections

LeastCostFlowDirections computes the drainage directions on the elevation model as it is, without filling the depressions. It starts from the cells at the border of the map, which become the outlets, and grows the directions upstream, always from the lowest cell reached so far: the least cost search of Metz et al. (2011), used by the r.watershed module of GRASS GIS. When the search reaches a depression, it crosses it along the lowest path, instead of filling it.

The elevation model is not changed, which matters where the depressions are real or where filling them would flatten large areas, and a single module gives the drainage directions, the contributing areas, the slope and the aspect.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/leastcostflowdirections.groovy
:language: groovy
```

:::{figure} ../../images/modules/LeastCostFlowDirections_tca_leastcost.png
:alt: The total contributing area of the least cost drainage directions of the sample area, in cells, computed on the elevation not depitted.

The total contributing area of the least cost drainage directions of the sample area, in cells, computed on the elevation not depitted.
:::

## Reference

```{include} ../generated/LeastCostFlowDirections.md
```
