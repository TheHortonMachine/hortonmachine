# ERM (Embedded Reservoir Model)

The ERM modules form a workflow that goes from a digital elevation model and the measurements of meteo stations and stream gauges, to a calibrated simulation of the discharge of a basin. The seven steps run in order, each one adding its results to a single GeoFrame database (a GeoPackage), which is the input of the next step:

| Step | Module | Adds to the database |
|---|---|---|
| 1 | [ErmDataPreparator](ermdatapreparator.md) | the sub-basins (hydrological response units), the network and their topology, the stream gauges |
| 2 | [ErmStationDataImporter](ermstationdataimporter.md) | the measured temperature, precipitation and discharge |
| 3 | [ErmKriging](ermkriging.md) | the temperature and precipitation of each sub-basin |
| 4 | [ErmRadiation](ermradiation.md) | the net radiation of each sub-basin |
| 5 | [ErmPrestleyEt](ermprestleyet.md) | the potential evapotranspiration of each sub-basin |
| 6 | [ErmCalibration](ermcalibration.md) | nothing: the best model parameters are reported in the log |
| 7 | [ErmSimulation](ermsimulation.md) | nothing: the simulated discharge is shown against the observed one |

The modules can be run from the [Spatial Toolbox](../../../apps/spatialtoolbox.md), in the GeoFrame folder, or with scripts in the [Geoscript Console](../../../apps/geoscript.md). The page of each module has the script of its step: together they are a complete run on a basin in Japan, and only the `workspace` folder at the top of each script needs to be changed to run them on other data.

:::{admonition} TODO
:class: warning

References for the ERM model and the GEOframe environment, and maps of the results of each step, are still to be added.
:::

```{toctree}
:maxdepth: 1

ermdatapreparator
ermstationdataimporter
ermkriging
ermradiation
ermprestleyet
ermcalibration
ermsimulation
```
