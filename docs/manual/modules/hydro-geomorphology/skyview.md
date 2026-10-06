# Skyview

In mountainous terrain, the surrounding slopes hide part of the sky: the sky view factor is the fraction still visible, 1 on a flat open plain and lower at the bottom of narrow valleys. It reduces the diffuse radiation reaching the ground, as in the radiation of the [ERM workflow](../geoframe/erm/ermradiation.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/skyview.groovy
:language: groovy
```

:::{figure} ../../images/modules/Skyview_skyview.png
:alt: The sky view factor of the sample area: the lowest values are at the bottom of the narrow valleys.

The sky view factor of the sample area: the lowest values are at the bottom of the narrow valleys.
:::

## Reference

```{include} ../generated/Skyview.md
```
