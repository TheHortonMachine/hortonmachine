# Insolation

Insolation computes the direct solar radiation that reaches the terrain on a period of days, with a clear sky. For each half hour of each day, the position of the sun gives the angle of the rays on each cell, from its slope and aspect, and the shadows cast by the surrounding terrain; the radiation crossing the atmosphere is reduced by standard transmittances of the air, the ozone, the water vapour and the aerosols, with the vectorial algebra of Corripio (2003).

The values in kW/m² of each half hour are summed: half of the sum is the energy in kWh/m². The diffuse radiation is not considered, so the slopes in the shadow get nothing. Only the day of the year of the dates is used, so the period can't cross the end of a year.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/insolation.groovy
:language: groovy
```

:::{figure} ../../images/modules/Insolation_insolation.png
:alt: The direct radiation of the summer solstice on the sample area, as the sum of the half hours in kW/m²: about 8.5 kWh/m² on the slopes facing the sun, much less in the narrow valleys and on the slopes facing north.

The direct radiation of the summer solstice on the sample area, as the sum of the half hours in kW/m²: about 8.5 kWh/m² on the slopes facing the sun, much less in the narrow valleys and on the slopes facing north.
:::

## Reference

```{include} ../generated/Insolation.md
```
