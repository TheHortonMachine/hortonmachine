# Pitfiller

Real terrain drains to the sea, but in a digital elevation model there are always cells lower than all their neighbours: errors of the data, effects of the resolution, or real closed depressions. From such cells the drainage directions can't be defined, so the depressions are filled first. The depitted elevation model is the input of the drainage directions modules, like [FlowDirections](../geomorphology/flowdirections.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/pitfiller.groovy
:language: groovy
```

:::{figure} ../../images/modules/Pitfiller_pit.png
:alt: The depitted elevation of the sample area: the filled depressions are the flat patches.

The depitted elevation of the sample area: the filled depressions are the flat patches.
:::

## Reference

```{include} ../generated/Pitfiller.md
```
