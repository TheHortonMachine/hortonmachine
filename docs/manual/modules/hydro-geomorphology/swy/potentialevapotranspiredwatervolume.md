# PotentialEvapotranspiredWaterVolume

The second step of the [Seasonal Water Yield](index.md) workflow: the potential evapotranspiration of each month, the crop coefficient of the land use of each cell times the reference evapotranspiration.

The reference evapotranspiration of the sample data is given as maps. Where only temperatures and precipitation are available, the module calculates it with the modified Hargreaves equation, from the maximum, minimum and mean temperature, the extraterrestrial radiation and the precipitation of the month, with `pDaysInTimestep` the days of the month (30 if not set).

## Details

```{include} ../../generated/PotentialEvapotranspiredWaterVolume_details.md
```

## Parameters

```{include} ../../generated/PotentialEvapotranspiredWaterVolume.md
```

## Example

```{literalinclude} ../../examples/swy/02_pet.groovy
:language: groovy
```

:::{figure} ../../../images/modules/PotentialEvapotranspiredWaterVolume_pet_1.png
:alt: The potential evapotranspiration of January, in mm.

The potential evapotranspiration of January, in mm: the lines and patches of lower values are the roads and the urban areas, with a crop coefficient of 0.4.
:::

## The values

The potential evapotranspiration is a depth of water in the month, in mm: what the cell would evapotranspire with all the water it needs. The actual evapotranspiration of [SWYRechargeRouting](swyrechargerouting.md) is at most this much, and less where the water is not enough.

On the sample data it goes from about 23 to 148 mm in a month, around 1200 mm in the year:

- the **reference evapotranspiration** is between 65 and 140 mm in a month, the lowest in the cloudy months of the long rains, April and May, and the highest in the dry, sunny months from January to March. In the year it goes from about 1350 mm in the lower east of the basin to 1090 mm on the highest cells. It comes from the modified Hargreaves method; at the Nyeri station the Penman-Monteith method gives about 1490 mm in the year (see [how the values compare with measures](index.md#how-the-values-compare-with-measures));
- the **crop coefficients** of the sample are between 0.3 and 1.15. Those of the forest (1.0), the tea (0.95) and the coffee (1.05) are near the values of FAO for the same covers (Allen et al., 1998): 1.0 for large forests of conifers, 1.0 for tea without shade and 1.15 with shade, 0.95 for coffee on bare ground and 1.1 with weeds. The coefficient of the general agriculture changes in the year, from 0.3 when the fields are bare to 1.15 in July and August. The urban areas and the roads have 0.4.

Where the land use is novalue, the potential evapotranspiration is novalue too.

**References**

- Allen, R. G., Pereira, L. S., Raes, D., Smith, M. (1998). Crop evapotranspiration: guidelines for computing crop water requirements. FAO Irrigation and Drainage Paper 56, Table 12. https://www.fao.org/4/x0490e/x0490e0b.htm
