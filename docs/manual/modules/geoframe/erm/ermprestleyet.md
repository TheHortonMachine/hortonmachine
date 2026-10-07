# ErmPrestleyEt

Computes the potential evapotranspiration of each sub-basin with the Priestley-Taylor method of the [PresteyTaylorEtpModel](../../hydro-geomorphology/presteytayloretpmodel.md) module, where the method and its references are described.

In the workflow, the inputs come from the GeoFrame database: the net radiation of [ErmRadiation](ermradiation.md) and the air temperature interpolated by [ErmKriging](ermkriging.md), at the centroid of each sub-basin. The parameters of the method are fixed:

| Parameter | Value |
|---|---|
| Priestley-Taylor coefficient α | 1.26, of well-watered surfaces |
| soil heat flux during the day, as a fraction of the net radiation | 0.35 |
| soil heat flux during the night, as a fraction of the net radiation | 0.75 |
| atmospheric pressure | 100 kPa |

The soil heat flux is used only with hourly time steps.

```{literalinclude} ../../examples/erm/05_evapotranspiration.groovy
:language: groovy
```

With this step the input data of the sub-basins are complete, and they can be checked in the [Database Viewer](../../../apps/dbviewer.md): right-click on the `basin_data` table of the GeoFrame database and choose **Open Basin Data Chart**, then pick the sub-basin in the combo box at the top.

:::{figure} ../../../images/modules/Geoframe_erm_basindata_chart.png
:alt: The basin data chart of the Database Viewer, with the evapotranspiration, precipitation, radiation and temperature of a sub-basin.

The input data of a sub-basin: the potential evapotranspiration of this step, the net radiation of [ErmRadiation](ermradiation.md), the precipitation and temperature of [ErmKriging](ermkriging.md).
:::

```{include} ../../generated/ErmPrestleyEt.md
```
