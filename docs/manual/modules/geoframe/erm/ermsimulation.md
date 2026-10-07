# ErmSimulation

Runs the water budget model over a period with a fixed set of parameters, usually the ones found by the calibration, and shows a chart of the simulated against the observed discharge, with their KGE. Running it on a period different from the calibration one validates the calibrated parameters.

```{literalinclude} ../../examples/erm/07_simulation.groovy
:language: groovy
```

:::{figure} ../../../images/modules/Geoframe_erm_simulation.png
:alt: The simulation script in the Geoscript Console, with the chart of the simulated against the observed discharge.

The simulation run in the [Geoscript Console](../../../apps/geoscript.md), with the parameters of the calibration: at the end, the chart of the simulated against the observed discharge, and their KGE.
:::

## The results in the database

Each simulation writes the simulated discharge of every sub-basin to a new table of the GeoFrame database, named after the time of the run: `sim<date>_<time>_water_budget_simulation_discharge`. Runs with different parameters or periods can so be compared later. With `doWriteState`, the state of the model at each time step is written too, in a `sim<date>_<time>_water_budget_state` table.

The results can be explored in the [Database Viewer](../../../apps/dbviewer.md). When it opens the database, the GeoFrame icon in front of its name shows that it has been recognized as a GeoFrame database, which enables the GeoFrame actions in the context menus of its tables. Right-click on a simulation table and choose **Open ERM Simulation Chart**: for a sub-basin, the chart panel shows its precipitation and temperature, and below them its simulated discharge against the observed one. The map on the right shows the sub-basins, numbered by their id, with the network and the shown sub-basin highlighted: activate the arrow tool and click on another sub-basin to chart it.

:::{figure} ../../../images/modules/Geoframe_erm_dbviewer.png
:alt: The GeoFrame database in the Database Viewer, with the ERM simulation chart of a sub-basin and the map of the sub-basins.

The GeoFrame database in the Database Viewer, recognized by the GeoFrame icon, and the ERM simulation chart opened on a simulation table: precipitation and temperature, simulated and observed discharge of the selected sub-basin, and the map to select the sub-basin.
:::

In the same way, **Open Station Data Chart** on the `station_data` table and **Open Basin Data Chart** on the `basin_data` table chart the measured data of the stations and the input data of the sub-basins: see [ErmStationDataImporter](ermstationdataimporter.md) and [ErmPrestleyEt](ermprestleyet.md).

```{include} ../../generated/ErmSimulation.md
```
