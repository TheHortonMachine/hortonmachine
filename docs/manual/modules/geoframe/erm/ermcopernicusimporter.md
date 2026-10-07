# ErmCopernicusImporter

An alternative to the steps 3 to 5 of the workflow ([ErmKriging](ermkriging.md), [ErmRadiation](ermradiation.md) and [ErmPrestleyEt](ermprestleyet.md)), for basins without enough meteo stations: the input data of the sub-basins are taken from the daily **AgERA5** dataset of the Copernicus Climate Data Store, a global grid of about 0.1° (some 10 km) derived from the ERA5 reanalysis. For each day of the period, the module stores in the GeoFrame database the mean over each sub-basin of:

| Variable | AgERA5 | Stored as |
|---|---|---|
| temperature | 2 m temperature, 24 hour mean | °C |
| precipitation | precipitation flux | mm/day |
| evapotranspiration | reference evapotranspiration (FAO-56 Penman-Monteith) | mm/day |

The model needs only these three variables, so the net radiation of ErmRadiation is not needed. [ErmStationDataImporter](ermstationdataimporter.md) is still needed, for the discharge measured at the stream gauges, which the calibration compares with the simulated one.

A few things to consider:

- AgERA5 data are daily, so the calibration and the simulation have to run with a daily time step (`pTimeStepMinutes = 60*24`).
- Each daily value is stored at the time of the day of `pStartTimestamp`: it has to be the same as the one of the station data, or the simulated and observed discharges are not aligned.
- The reference evapotranspiration is the one of a grass reference surface, not the Priestley-Taylor one of ErmPrestleyEt: the parameters calibrated with one of the two are not valid for the other.
- The AgERA5 cells are larger than the sub-basins, and the precipitation varies even more smoothly than its grid: in small basins all the sub-basins can get the same precipitation.

The data are downloaded with the Copernicus API token and kept in the download folder set in the [Settings](../../../apps/settings.md#copernicus): the data of each day and variable are downloaded only once, as a global file, and then read from the folder. The first run on a long period has to download three files per day, each one a job of the Climate Data Store, and can take long.

```{literalinclude} ../../examples/erm/03_copernicus_importer.groovy
:language: groovy
```

```{include} ../../generated/ErmCopernicusImporter.md
```
