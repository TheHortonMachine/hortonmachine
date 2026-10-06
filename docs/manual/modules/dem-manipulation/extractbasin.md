# ExtractBasin

Extracts the basin upstream of an outlet point, following the drainage directions upstream from it. The outlet must lie on the network, as drawn by the drainage directions: when a network vector is supplied, the outlet is first snapped to it, within the snapping distance.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/extractbasin.groovy
:language: groovy
```

:::{figure} ../../images/modules/ExtractBasin_basin.png
:alt: The basin of an outlet in the middle of the sample area: 1.9 km².

The basin of an outlet in the middle of the sample area: 1.9 km².
:::

## Reference

```{include} ../generated/ExtractBasin.md
```
