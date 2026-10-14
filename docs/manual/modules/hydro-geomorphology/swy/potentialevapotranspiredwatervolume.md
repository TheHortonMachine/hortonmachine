# PotentialEvapotranspiredWaterVolume

The second step of the [Seasonal Water Yield](index.md) workflow: the potential evapotranspiration of each month, the crop coefficient of the land use of each cell times the reference evapotranspiration.

The reference evapotranspiration of the sample data is given as maps. Where only temperatures and precipitation are available, the module calculates it with the modified Hargreaves equation, from the maximum, minimum and mean temperature, the extraterrestrial radiation and the precipitation of the month, with `pDaysInTimestep` the days of the month (30 if not set).

```{literalinclude} ../../examples/swy/02_pet.groovy
:language: groovy
```

:::{figure} ../../../images/modules/PotentialEvapotranspiredWaterVolume_pet_1.png
:alt: The potential evapotranspiration of January, in mm.

The potential evapotranspiration of January, in mm: the lines and patches of lower values are the roads and the urban areas, with a crop coefficient of 0.4.
:::

```{include} ../../generated/PotentialEvapotranspiredWaterVolume.md
```
