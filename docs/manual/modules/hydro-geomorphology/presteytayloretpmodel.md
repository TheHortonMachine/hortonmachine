# PresteyTaylorEtpModel

Computes the potential evapotranspiration with the Priestley-Taylor method: the evapotranspiration of a surface with no shortage of water, driven by the available energy, the net radiation minus the heat going into the soil. It needs neither wind nor humidity, only the net radiation, the air temperature and the atmospheric pressure, which makes it fit for areas with few meteo stations.

The Priestley-Taylor coefficient α scales the equilibrium evapotranspiration: 1.26 is the value of well-watered surfaces. With hourly time steps, the soil heat flux is a fraction of the net radiation, different for the day and the night; with daily time steps it is neglected.

The time series are CSV files in the time series format of the HortonMachine, with one column per station or sub-basin, as described for the [ERM station data](../geoframe/erm/ermstationdataimporter.md). The result is in mm per time step: mm/h with hourly time steps, mm/day with daily ones.

```{include} ../generated/PresteyTaylorEtpModel.md
```
